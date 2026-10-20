package com.allgo.app.androidauto

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.RemoteCallbackList
import android.view.KeyEvent
import com.allgo.aapsdk.AapAttributes
import com.allgo.aapsdk.vehicle.CompassData
import com.allgo.aapsdk.vehicle.DrivingStatusData
import com.allgo.aapsdk.vehicle.FuelData
import com.allgo.aapsdk.vehicle.NightModeData
import com.allgo.aapsdk.vehicle.SpeedData
import com.allgo.mirroring.deviceservice.IAndroidAutoDeviceCallback
import com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
import com.allgo.mirroring.deviceservice.MirroringDevice
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AaStatusBus
import com.andrerinas.openheadunit.aap.AapProjectionActivity
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.aap.OemSignals
import com.andrerinas.openheadunit.aap.ProjectionGate
import com.andrerinas.openheadunit.aap.protocol.messages.VideoFocusEvent
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The service the car's RemoteUI binds to (`com.allgo.app.androidauto.AndroidAutoMainService`),
 * answering the same AIDL contract as the Allgo Android Auto app this package replaces.
 *
 * DiAuto keeps doing the actual work (USB, Bluetooth handshake, hotspot, projection). This service
 * is the adapter: it turns RemoteUI's requests (start/stop/resume a session, launch an app) into
 * DiAuto actions, and turns DiAuto's connection state into the device and session callbacks
 * RemoteUI uses to keep the car's UI (launcher tile, mirroring state) in step.
 */
class AndroidAutoMainService : Service() {

    private val deviceCallbacks = RemoteCallbackList<IAndroidAutoDeviceCallback>()
    private val sessionCallbacks = RemoteCallbackList<IAndroidAutoSessionCallback>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var activeDevice: MirroringDevice? = null
    private var deviceAnnounced = false
    private var sessionAnnounced = false

    // A session is being ended so that another phone's can start: the car is not sent back to its
    // native screen in between, and a second request for the same phone is not run twice.
    @Volatile private var switching = false
    private var switchJob: Job? = null
    private var switchTarget: String? = null

    private companion object {
        /** The original reports a reason code with onSessionFailed; DiAuto has no finer cause to give. */
        const val SESSION_FAILED_GENERIC = -1
        const val UNKNOWN_ADDRESS = DeviceSwitchPolicy.UNKNOWN_ADDRESS
    }

    private val commManager get() = App.provide(this).commManager

    // The surround-view camera takes the screen like RemoteUI's disableVideoFocus does; the stock app
    // listens to both, so a car that only announces the camera is followed too. Held only when
    // RemoteUI has not already taken the screen away, and given back only what was taken here.
    private var aroundViewHold = false
    private var projectionBeforeAroundView = false

    private val aroundViewReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                OemSignals.ACTION_AROUND_VIEW_START -> onAroundView(true)
                OemSignals.ACTION_AROUND_VIEW_STOP -> onAroundView(false)
            }
        }
    }

    private fun onAroundView(active: Boolean) {
        AppLog.i("AndroidAutoMainService: surround view ${if (active) "started" else "stopped"}")
        if (active) {
            if (ProjectionGate.videoFocusDisabled || aroundViewHold) return
            aroundViewHold = true
            projectionBeforeAroundView = AapProjectionActivity.isForeground
            ProjectionGate.videoFocusDisabled = true
            if (commManager.isConnected) commManager.send(VideoFocusEvent(gain = false, unsolicited = true))
        } else {
            if (!aroundViewHold) return
            aroundViewHold = false
            ProjectionGate.videoFocusDisabled = false
            if (!commManager.isConnected) return
            // Like the stock app: back to the projection only if it was on screen when the camera came.
            if (projectionBeforeAroundView) {
                showProjection()
                commManager.send(VideoFocusEvent(gain = true, unsolicited = true))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.i("AndroidAutoMainService: created")
        val aroundView = IntentFilter().apply {
            addAction(OemSignals.ACTION_AROUND_VIEW_START)
            addAction(OemSignals.ACTION_AROUND_VIEW_STOP)
        }
        try {
            registerReceiver(aroundViewReceiver, aroundView)
        } catch (e: Exception) {
            AppLog.w("AndroidAutoMainService: could not follow the surround view: ${e.message}")
        }
        scope.launch {
            commManager.connectionState.collect { state -> onConnectionState(state) }
        }
        scope.launch {
            AaStatusBus.playback.collect { code -> if (code != AaStatusBus.UNKNOWN) sendPlayback(code) }
        }
        scope.launch {
            AaStatusBus.navigation.collect { nav ->
                if (sessionAnnounced) fanOutSession { it.onAAPNavigationStatus(nav.message, nav.status, null) }
            }
        }
        scope.launch {
            AaStatusBus.battery.collect { battery ->
                if (sessionAnnounced) fanOutSession { it.onBatteryStatus(battery.level, battery.secondsRemaining) }
            }
        }
        scope.launch {
            AaStatusBus.call.collect { code -> if (code != AaStatusBus.UNKNOWN) sendCall(code) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder {
        AppLog.i("AndroidAutoMainService: bound by ${intent?.component ?: intent?.action}")
        return binder
    }

    override fun onDestroy() {
        try { unregisterReceiver(aroundViewReceiver) } catch (_: Exception) {}
        scope.cancel()
        deviceCallbacks.kill()
        sessionCallbacks.kill()
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // DiAuto -> RemoteUI
    // -------------------------------------------------------------------------

    /**
     * RemoteUI keeps every announced device and, at its next start, asks Bluetooth for each stored
     * address: an empty one crashes the service in a loop. So the device always carries a well-formed
     * address, the connected phone's when there is one.
     */
    private fun phoneAddress(): String {
        val adapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        val bonded = try { adapter?.bondedDevices } catch (_: SecurityException) { null }
        val connected = bonded?.firstOrNull { dev ->
            try { dev.javaClass.getMethod("isConnected").invoke(dev) as? Boolean == true } catch (_: Exception) { false }
        }
        return (connected ?: bonded?.firstOrNull())?.address ?: UNKNOWN_ADDRESS
    }

    private fun describeDevice(sessionStatus: Int): MirroringDevice = MirroringDevice().apply {
        val address = phoneAddress()
        type = MirroringDevice.ANDROID_AUTO
        transport = if (commManager.isWirelessSession) MirroringDevice.TRANSPORT_TYPE_WIFI else MirroringDevice.TRANSPORT_TYPE_USB
        name = "Android Auto"
        serialId = address
        BTMacAddress = address
        this.sessionStatus = sessionStatus
    }

    /**
     * RemoteUI resolves the device of a session callback with getReferenceFor(): only type 1 (matched on
     * the Bluetooth address) or type 2 over USB are found. A wireless Android Auto device is neither, so
     * session events are sent as type 1; it is matched against the entry the device callback created.
     */
    private fun sessionView(device: MirroringDevice, status: Int = device.sessionStatus): MirroringDevice =
        MirroringDevice(device).also {
            it.type = MirroringDevice.CARPLAY
            it.sessionStatus = status
            it.mRemoteDevice.sessionStatus = status
        }

    // Playback and call state go to RemoteUI only while a session is announced; the current values are
    // replayed when it starts, since the phone usually reports them before that.
    private fun sendPlayback(code: Int) {
        if (!sessionAnnounced) return
        fanOutSession { it.onPlayBackUpdate(code) }
    }

    private fun sendCall(code: Int) {
        if (!sessionAnnounced) return
        // The stock app wakes the screen on any phone status: a call must be visible.
        if (code != 0) OemSignals.screenOn(this, "phone call state $code")
        fanOutSession { it.onPhoneStateUpdate(code) }
    }

    private fun onConnectionState(state: CommManager.ConnectionState) {
        when (state) {
            is CommManager.ConnectionState.Connected -> {
                val device = describeDevice(MirroringDevice.SESSION_STARTING).also { activeDevice = it }
                AppLog.i("AndroidAutoMainService: Connected, device=${device.name} transport=${device.transport} mac=${device.BTMacAddress} announced=$deviceAnnounced")
                if (!deviceAnnounced) {
                    deviceAnnounced = true
                    fanOutDevice { it.onDeviceConnected(device) }
                }
            }
            is CommManager.ConnectionState.TransportStarted -> {
                switching = false
                val device = describeDevice(MirroringDevice.SESSION_STARTED).also { activeDevice = it }
                AppLog.i("AndroidAutoMainService: TransportStarted, mac=${device.BTMacAddress} announced=$sessionAnnounced")
                // This service can come up after Connected went by (it is restarted with the app), so
                // RemoteUI must still hear about the device before it hears about its session.
                if (!deviceAnnounced) {
                    deviceAnnounced = true
                    val announced = describeDevice(MirroringDevice.SESSION_STARTING)
                    AppLog.i("AndroidAutoMainService: announcing the device ahead of its session")
                    fanOutDevice { it.onDeviceConnected(announced) }
                }
                if (!sessionAnnounced) {
                    sessionAnnounced = true
                    AppLog.i("AndroidAutoMainService: announcing session started")
                    fanOutSession {
                        it.onSessionStarted(sessionView(device), 0)
                        it.onScreenStarted()
                    }
                    AaStatusBus.playback.value.let { if (it != AaStatusBus.UNKNOWN) sendPlayback(it) }
                    AaStatusBus.call.value.let { if (it != AaStatusBus.UNKNOWN) sendCall(it) }
                }
            }
            is CommManager.ConnectionState.Disconnected -> {
                val device = activeDevice
                if (sessionAnnounced) OemSignals.screenOn(this, "phone disconnected")
                // Ending the session to start another phone's: the car stays on the projection.
                val toNativeScreen = state.isUserExit && !switching
                switching = false
                if (sessionAnnounced && device != null) {
                    val closed = sessionView(device, MirroringDevice.SESSION_NOT_STARTED)
                    fanOutSession {
                        it.onScreenStopped()
                        // The user left through the phone's own Exit: the car goes back to its native screen.
                        if (toNativeScreen) it.onNativeUILaunch()
                        if (state.isUserExit) it.onSessionClosed(closed) else it.onSessionDisconnected(closed, 0)
                    }
                }
                if (deviceAnnounced && device != null) {
                    val gone = MirroringDevice(device).also { it.sessionStatus = MirroringDevice.SESSION_NOT_STARTED }
                    fanOutDevice { it.onDeviceDisconnected(gone) }
                }
                activeDevice = null
                deviceAnnounced = false
                sessionAnnounced = false
                AaStatusBus.reset()
                ProjectionGate.reset()
                aroundViewHold = false
            }
            is CommManager.ConnectionState.Error -> {
                val device = activeDevice
                // Only once RemoteUI knows the device: a failure before that has nothing to attach to.
                if (deviceAnnounced && device != null) {
                    AppLog.w("AndroidAutoMainService: session failed: ${state.message}")
                    fanOutSession { it.onSessionFailed(sessionView(device, MirroringDevice.SESSION_NOT_STARTED), SESSION_FAILED_GENERIC) }
                }
            }
            else -> {}
        }
    }

    private inline fun fanOutDevice(call: (IAndroidAutoDeviceCallback) -> Unit) {
        val n = deviceCallbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                try { call(deviceCallbacks.getBroadcastItem(i)) } catch (e: Exception) { AppLog.w("AndroidAutoMainService: device callback failed: ${e.message}") }
            }
        } finally {
            deviceCallbacks.finishBroadcast()
        }
    }

    private inline fun fanOutSession(call: (IAndroidAutoSessionCallback) -> Unit) {
        val n = sessionCallbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                try { call(sessionCallbacks.getBroadcastItem(i)) } catch (e: Exception) { AppLog.w("AndroidAutoMainService: session callback failed: ${e.message}") }
            }
        } finally {
            sessionCallbacks.finishBroadcast()
        }
    }

    // -------------------------------------------------------------------------
    // RemoteUI -> DiAuto
    // -------------------------------------------------------------------------

    private fun startAapService(action: String, mac: String? = null) {
        try {
            val intent = Intent(this, AapService::class.java).setAction(action)
            if (mac != null) intent.putExtra(AapService.EXTRA_MAC, mac)
            startForegroundService(intent)
        } catch (e: Exception) {
            AppLog.e("AndroidAutoMainService: could not reach AapService ($action): ${e.message}")
        }
    }

    private fun showProjection() {
        try {
            startActivity(AapProjectionActivity.intent(this))
        } catch (e: Exception) {
            AppLog.w("AndroidAutoMainService: could not bring the projection forward: ${e.message}")
        }
    }

    /** Starts a session with [device] when none is running: wireless by its address, otherwise over USB. */
    private fun startDevice(device: MirroringDevice?) {
        val mac = device?.BTMacAddress
        if (device?.transport == MirroringDevice.TRANSPORT_TYPE_WIFI && !mac.isNullOrEmpty()) {
            startAapService(AapService.ACTION_NATIVE_AA_POKE, mac)
        } else {
            startAapService(AapService.ACTION_CHECK_USB)
        }
    }

    /**
     * Ends the session in progress, waits for it to be over (as long as the original does), then starts
     * the one for [device]. If the first does not end in time the request fails, and the session in
     * progress is left to go on.
     */
    private fun switchTo(device: MirroringDevice) {
        val target = device.BTMacAddress?.uppercase()
        if (switchJob?.isActive == true && switchTarget == target) {
            AppLog.i("AndroidAutoMainService: already switching to ${device.name}")
            return
        }
        switchJob?.cancel()
        switchTarget = target
        switching = true
        AppLog.i("AndroidAutoMainService: switching to ${device.name} ($target): ending the current session first")
        switchJob = scope.launch {
            startAapService(AapService.ACTION_DISCONNECT)
            val ended = withTimeoutOrNull(DeviceSwitchPolicy.ENDING_WAIT_MS) {
                commManager.connectionState.first { it is CommManager.ConnectionState.Disconnected }
            }
            if (ended == null) {
                AppLog.w("AndroidAutoMainService: the current session did not end in ${DeviceSwitchPolicy.ENDING_WAIT_MS} ms; not switching")
                switching = false
                if (deviceAnnounced) {
                    fanOutSession { it.onSessionFailed(sessionView(device, MirroringDevice.SESSION_NOT_STARTED), SESSION_FAILED_GENERIC) }
                }
                return@launch
            }
            AppLog.i("AndroidAutoMainService: the current session ended, starting ${device.name}")
            startDevice(device)
        }
    }

    private val binder = object : IAndroidAutoMainService.Stub() {
        override fun register(device: IAndroidAutoDeviceCallback?, session: IAndroidAutoSessionCallback?) {
            AppLog.i("AndroidAutoMainService: register(device=${device != null}, session=${session != null})")
            if (device != null) deviceCallbacks.register(device)
            if (session != null) sessionCallbacks.register(session)
            // A client that arrives while a phone is already connected must learn about it.
            // Not "activeDevice" alone: inside the Stub that name is its own getActiveDevice().
            val active = this@AndroidAutoMainService.activeDevice
            if (active != null && device != null && deviceAnnounced) {
                try { device.onDeviceConnected(MirroringDevice(active)) } catch (_: Exception) {}
            }
            // Same for the session: RemoteUI restarts on its own, and the launcher tile only follows events.
            if (active != null && session != null && sessionAnnounced) {
                AppLog.i("AndroidAutoMainService: replaying session started to a late client")
                try {
                    session.onSessionStarted(sessionView(active), 0)
                    session.onScreenStarted()
                } catch (_: Exception) {}
            }
        }

        override fun unregister(device: IAndroidAutoDeviceCallback?, session: IAndroidAutoSessionCallback?) {
            if (device != null) deviceCallbacks.unregister(device)
            if (session != null) sessionCallbacks.unregister(session)
        }

        override fun registerCallback(binder: IBinder?) {
            // The original hands the app's own helper callbacks here; nothing in DiAuto consumes them.
        }

        override fun startSession(device: MirroringDevice?, screenMode: Int, audioMode: Int): Int {
            AppLog.i("AndroidAutoMainService: startSession(${device?.name}, transport=${device?.transport}, screen=$screenMode, audio=$audioMode)")
            if (commManager.isConnected || commManager.isBusy) {
                // Like the original: another phone than the one projecting replaces it, the same one
                // is only brought back to the screen.
                if (device != null && DeviceSwitchPolicy.shouldSwitch(
                        device.BTMacAddress, this@AndroidAutoMainService.activeDevice?.BTMacAddress, commManager.isConnected)
                ) {
                    switchTo(device)
                } else {
                    showProjection()
                }
                return 0
            }
            startDevice(device)
            return 0
        }

        override fun stopSession(device: MirroringDevice?): Int {
            AppLog.i("AndroidAutoMainService: stopSession")
            startAapService(AapService.ACTION_DISCONNECT)
            return 0
        }

        override fun pauseSession(device: MirroringDevice?): Int = 0

        override fun resumeSession(device: MirroringDevice?): Int {
            showProjection()
            return 0
        }

        override fun isSessionActive(): Boolean = commManager.isConnected

        override fun getActiveDevice(): MirroringDevice? = this@AndroidAutoMainService.activeDevice

        override fun getPluginVersion(): String = "DiAuto"

        override fun getVRStatus(): Boolean = false

        override fun launchApp(app: Int): Int {
            // Whatever the app, the first thing the car wants is the projection on screen.
            showProjection()
            return 0
        }

        // The original presses the SEARCH key for both opening and closing the microphone; the phone toggles.
        override fun launchGoogleAssistant(start: Boolean): Int {
            AppLog.i("AndroidAutoMainService: voice assistant ${if (start) "start" else "stop"}")
            commManager.sendKey(KeyEvent.KEYCODE_SEARCH, true, null, "remoteui-voice")
            commManager.sendKey(KeyEvent.KEYCODE_SEARCH, false, null, "remoteui-voice")
            return 0
        }

        // Steering-wheel and screen media keys arrive as RemoteUI codes; the original forwards them to the
        // phone as AAP key presses. Codes it ignores (pressed-only events, unused slots) report -1.
        override fun sendMediaPlayControl(key: Int): Int {
            val keyCode = when (key) {
                1 -> KeyEvent.KEYCODE_MEDIA_PLAY
                2 -> KeyEvent.KEYCODE_MEDIA_PAUSE
                3, 11 -> KeyEvent.KEYCODE_MEDIA_NEXT
                4, 12 -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                19, 21 -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                else -> return -1
            }
            AppLog.i("AndroidAutoMainService: media key $key -> keycode $keyCode")
            commManager.sendKey(keyCode, true, null, "remoteui-media")
            commManager.sendKey(keyCode, false, null, "remoteui-media")
            return 0
        }

        override fun disableAAP(disable: Boolean) {
            AppLog.i("AndroidAutoMainService: disableAAP($disable)")
        }

        override fun disableTouchInput(disable: Boolean) {
            AppLog.i("AndroidAutoMainService: disableTouchInput($disable)")
            ProjectionGate.touchDisabled = disable
        }

        // Disabled: hand the screen back to the car (native focus). Enabled: take the projection again.
        override fun disableVideoFocus(disable: Boolean) {
            AppLog.i("AndroidAutoMainService: disableVideoFocus($disable)")
            ProjectionGate.videoFocusDisabled = disable
            if (!commManager.isConnected) return
            if (disable) {
                commManager.send(VideoFocusEvent(gain = false, unsolicited = true))
            } else {
                showProjection()
                commManager.send(VideoFocusEvent(gain = true, unsolicited = true))
            }
        }

        override fun surfaceChanged() {}

        override fun surfaceDestroyed() {}

        override fun setAttributes(attributes: AapAttributes?): Int = 0

        // Vehicle data: DiAuto reads the car itself, so the values RemoteUI pushes are accepted and dropped.
        override fun sendCompassData(sessionId: Int, data: CompassData?): Int = 0
        override fun sendDrivingStatus(sessionId: Int, data: DrivingStatusData?): Int = 0
        override fun sendFuelData(sessionId: Int, data: FuelData?): Int = 0
        override fun sendNightModeData(sessionId: Int, data: NightModeData?): Int = 0
        override fun sendSpeedData(sessionId: Int, data: SpeedData?): Int = 0
    }
}
