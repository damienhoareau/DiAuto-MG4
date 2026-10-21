package com.andrerinas.openheadunit.aap

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.andrerinas.openheadunit.aap.protocol.AudioConfigs
import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.aap.protocol.messages.DrivingStatusEvent
import com.andrerinas.openheadunit.aap.protocol.messages.LocationUpdateEvent
import com.andrerinas.openheadunit.aap.protocol.messages.ServiceDiscoveryResponse
import com.andrerinas.openheadunit.aap.protocol.messages.VideoFocusEvent
import com.andrerinas.openheadunit.aap.protocol.proto.Common
import com.andrerinas.openheadunit.aap.protocol.proto.Control
import com.andrerinas.openheadunit.aap.protocol.proto.Input
import com.andrerinas.openheadunit.aap.protocol.proto.Media
import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.andrerinas.openheadunit.decoder.MicRecorder
import com.andrerinas.openheadunit.decoder.VideoDecoder
import com.andrerinas.openheadunit.location.LocationHolder
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings

interface AapControl {
    fun execute(message: AapMessage): Int
}

internal class AapControlMedia(
    private val aapTransport: AapTransport,
    private val micRecorder: MicRecorder,
    private val aapAudio: AapAudio): AapControl {

    private var lastNativeFocusRequestTime = 0L
    private var nativeFocusRequestCount = 0

    override fun execute(message: AapMessage): Int {

        when (message.type) {
            Media.MsgType.MEDIA_MESSAGE_SETUP_VALUE -> {
                val setupRequest = message.parse(Media.MediaSetupRequest.newBuilder()).build()
                return mediaSinkSetupRequest(setupRequest, message.channel)
            }
            Media.MsgType.MEDIA_MESSAGE_START_VALUE -> {
                val startRequest = message.parse(Media.Start.newBuilder()).build()
                return mediaStartRequest(startRequest, message.channel)
            }
            Media.MsgType.MEDIA_MESSAGE_STOP_VALUE -> return mediaSinkStopRequest(message.channel)
            Media.MsgType.MEDIA_MESSAGE_VIDEO_FOCUS_REQUEST_VALUE -> {
                val focusRequest = message.parse(Media.VideoFocusRequestNotification.newBuilder()).build()
                AppLog.i("RX: Video Focus Request - mode: %s, reason: %s", focusRequest.mode, focusRequest.reason)

                if (focusRequest.mode == Media.VideoFocusMode.VIDEO_FOCUS_NATIVE) {
                    AppLog.i("Video Focus NATIVE received. User likely clicked Exit. Stopping transport.")
                    aapTransport.wasUserExit = true
                    aapTransport.stop()
                }
                return 0
            }
            Media.MsgType.MEDIA_MESSAGE_MICROPHONE_REQUEST_VALUE -> {
                val micRequest = message.parse(Media.MicrophoneRequest.newBuilder()).build()
                return micRequest(micRequest)
            }
            Media.MsgType.MEDIA_MESSAGE_UPDATE_UI_CONFIG_REPLY_VALUE -> {
                AppLog.i("RX: Update UI Config Reply received. Acknowledging UI Config change.")
                aapTransport.onUpdateUiConfigReplyReceived?.invoke()
                return 0
            }
            Media.MsgType.MEDIA_MESSAGE_ACK_VALUE -> return 0
            else -> AppLog.e("Unsupported Media message type: ${message.type}")
        }
        return 0
    }

    private fun mediaStartRequest(request: Media.Start, channel: Int): Int {
        AppLog.i("Media Start Request %s: session=%d, config_index=%d", Channel.name(channel), request.sessionId, request.configurationIndex)

        aapTransport.setSessionId(channel, request.sessionId)
        return 0
    }

    private fun mediaSinkSetupRequest(request: Media.MediaSetupRequest, channel: Int): Int {

        AppLog.i("Media Sink Setup Request: %d on channel %s", request.type, Channel.name(channel))

        val maxUnacked = maxUnackedFor(channel)
        val configResponse = Media.Config.newBuilder().apply {
            status = Media.Config.ConfigStatus.HEADUNIT
            this.maxUnacked = maxUnacked

            addConfigurationIndices(0)
        }.build()
        AppLog.i("Config response: %s (maxUnacked=%d)", configResponse, maxUnacked)
        val msg = AapMessage(channel, Media.MsgType.MEDIA_MESSAGE_CONFIG_VALUE, configResponse)
        aapTransport.send(msg)

        if (channel == Channel.ID_VID) {
            aapTransport.gainVideoFocus()
        }

        // Pushing AudioFocusNotification
        if (Channel.isAudio(channel)) {
            val focusNotification = Control.AudioFocusNotification.newBuilder()
                .setFocusState(Control.AudioFocusNotification.AudioFocusStateType.STATE_GAIN)
                .setUnsolicited(true)
                .build()
            aapTransport.send(AapMessage(Channel.ID_CTR, Control.ControlMsgType.MESSAGE_AUDIO_FOCUS_NOTIFICATION_VALUE, focusNotification))
        }

        return 0
    }

    private fun maxUnackedFor(channel: Int): Int {
        if (channel == Channel.ID_VID) {
            val softwareHevc =
                aapTransport.settings.videoCodec == VideoDecoder.CodecType.H265.settingsValue &&
                        aapTransport.settings.forceSoftwareDecoding &&
                        aapTransport.settings.softwareVideoDecoder == Settings.SoftwareVideoDecoder.BUNDLED_FFMPEG
            if (softwareHevc) {
                // Keep the phone closer to decoder pace. A large wireless window lets video
                // backlog turn into visible input lag when 2K HEVC is decoded in software.
                return if (aapTransport.isWireless) 6 else 8
            }
            // Left wide for hardware decode, deliberately. The window is counted in messages, not
            // frames, and a keyframe fragments into a dozen or more of them, so narrowing it makes
            // the phone stall mid-keyframe and caps throughput at window/RTT, worst on exactly the
            // congested links where the backlog it would be trying to bound shows up. The backlog
            // is bounded where it costs nothing instead: the decoder discards decoded frames it is
            // behind on rather than having the phone send fewer.
            return if (aapTransport.isWireless) 12 else 16
        }

        // Audio still benefits from a wider jitter window, especially on wireless.
        return if (aapTransport.isWireless) 30 else 16
    }

    private fun mediaSinkStopRequest(channel: Int): Int {
        AppLog.i("Media Sink Stop Request: " + Channel.name(channel))
        if (Channel.isAudio(channel)) {
            aapAudio.stopAudio(channel)
        } else if (channel == Channel.ID_VID) {
            if (aapTransport.ignoreNextStopRequest) {
                AppLog.i("Video Sink Stopped -> Ignored (Forced Keyframe Request)")
                aapTransport.ignoreNextStopRequest = false
                return 0
            }
            AppLog.i("Video Sink Stopped -> Normal background/transition behavior")
        }
        return 0
    }

    private fun micRequest(micRequest: Media.MicrophoneRequest): Int {
        AppLog.i("Mic request: open=${micRequest.open}")

        val status = if (micRequest.open) {
            val result = micRecorder.start()
            AppLog.i("MicRecorder.start result=$result")
            if (result == 0) {
                Common.MessageStatus.STATUS_SUCCESS_VALUE
            } else {
                Common.MessageStatus.STATUS_INTERNAL_ERROR_VALUE
            }
        } else {
            micRecorder.stop()
            Common.MessageStatus.STATUS_SUCCESS_VALUE
        }

        val response = Media.MicrophoneResponse.newBuilder()
            .setStatus(status)
            .setSessionId(0)
            .build()
        aapTransport.send(
            AapMessage(
                Channel.ID_MIC,
                Media.MsgType.MEDIA_MESSAGE_MICROPHONE_RESPONSE_VALUE,
                response,
            ),
        )
        return 0
    }

    companion object {
        /** Time window for counting consecutive VIDEO_FOCUS_NATIVE requests. */
        private const val NATIVE_FOCUS_DEBOUNCE_MS = 5000L
        /** Number of NATIVE focus requests within the debounce window before stopping. */
        private const val MAX_NATIVE_FOCUS_RETRIES = 3
    }
}

internal class AapControlTouch(private val aapTransport: AapTransport): AapControl {

    override fun execute(message: AapMessage): Int {

        when (message.type) {
            Input.MsgType.BINDINGREQUEST_VALUE -> {
                val request = message.parse(Input.KeyBindingRequest.newBuilder()).build()
                return inputBinding(request, message.channel)
            }
            else -> AppLog.e("Unsupported Input message type: ${message.type}")
        }
        return 0
    }

    private fun inputBinding(request: Input.KeyBindingRequest, channel: Int): Int {
        aapTransport.send(AapMessage(channel, Input.MsgType.BINDINGRESPONSE_VALUE, Input.BindingResponse.newBuilder()
                .setStatus(Common.MessageStatus.STATUS_SUCCESS)
                .build()))
        return 0
    }

}

internal class AapControlSensor(
        private val aapTransport: AapTransport,
        private val context: Context,
        private val settings: Settings): AapControl {

    override fun execute(message: AapMessage): Int {
        when (message.type) {
            Sensors.SensorsMsgType.SENSOR_STARTREQUEST_VALUE -> {
                val request = message.parse(Sensors.SensorRequest.newBuilder()).build()
                return sensorStartRequest(request, message.channel)
            }
            else -> AppLog.e("Unsupported Sensor message type: ${message.type}")
        }
        return 0
    }

    private fun sensorStartRequest(request: Sensors.SensorRequest, channel: Int): Int {
        AppLog.i("Sensor Start Request sensor: %s, minUpdatePeriod: %d", request.type.name, request.minUpdatePeriod)

        val msg = AapMessage(channel, Sensors.SensorsMsgType.SENSOR_STARTRESPONSE_VALUE, Sensors.SensorResponse.newBuilder()
                .setStatus(Common.MessageStatus.STATUS_SUCCESS)
                .build())
        AppLog.i(msg.toString())

        aapTransport.send(msg)
        aapTransport.startSensor(request.type.number)
        com.andrerinas.openheadunit.vehicle.Mg4EnergyReporter.onSensorStart(request.type.number)
        com.andrerinas.openheadunit.vehicle.Mg4DrivingSensorReporter.onSensorStart(request.type.number)

        if (request.type == Sensors.SensorType.NIGHT) {
            AppLog.i("Night sensor requested. Triggering immediate update.")
            val intent = Intent(AapService.ACTION_REQUEST_NIGHT_MODE_UPDATE)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }

        if (request.type == Sensors.SensorType.LOCATION && settings.useGpsForNavigation) {
            // The phone only accepts LOCATION events once this request has been answered, which
            // can land well after TransportStarted (CommManager's own post-handshake flush can
            // race this and lose). This is the actual earliest point sending can succeed.
            //
            // currentGpsFix, not currentLocation: only this head unit's own GPS may be sent as the
            // car's position. A unit with no antenna attached passes every gate here and never
            // locks, and the broader accessor would hand it a network fix to send instead.
            val fix = LocationHolder.currentGpsFix(context)
            if (fix != null) {
                val sentOnWire = aapTransport.send(LocationUpdateEvent(fix))
                AppLog.i("LOCATION sensor requested. Sending current fix immediately. sentOnWire=$sentOnWire")
            } else {
                AppLog.i("LOCATION sensor requested. No recent GPS fix to prime with.")
            }
        }
        return 0
    }
}

internal class AapControlService(
        private val aapTransport: AapTransport,
        private val aapAudio: AapAudio,
        private val settings: Settings,
        private val context: Context): AapControl {

    override fun execute(message: AapMessage): Int {

        when (message.type) {
            Control.ControlMsgType.MESSAGE_SERVICE_DISCOVERY_REQUEST_VALUE -> {
                val request = message.parse(Control.ServiceDiscoveryRequest.newBuilder()).build()
                return serviceDiscoveryRequest(request)
            }
            Control.ControlMsgType.MESSAGE_PING_REQUEST_VALUE -> {
                val pingRequest = message.parse(Control.PingRequest.newBuilder()).build()
                return pingRequest(pingRequest, message.channel)
            }
            Control.ControlMsgType.MESSAGE_NAV_FOCUS_REQUEST_VALUE -> {
                val navigationFocusRequest = message.parse(Control.NavFocusRequestNotification.newBuilder()).build()
                return navigationFocusRequest(navigationFocusRequest, message.channel)
            }
            Control.ControlMsgType.MESSAGE_BATTERY_STATUS_NOTIFICATION_VALUE -> {
                val battery = message.parse(Control.BatteryStatusNotification.newBuilder()).build()
                AppLog.i("RX: Battery status %d%% (critical=%s)", battery.batteryLevel, battery.criticalBattery)
                if (battery.criticalBattery) {
                    AaStatusBus.battery.tryEmit(AaStatusBus.Battery(battery.batteryLevel, battery.timeRemainingS))
                }
                return 0
            }
            Control.ControlMsgType.MESSAGE_BYEBYE_REQUEST_VALUE -> {
                val shutdownRequest = message.parse(Control.ByeByeRequest.newBuilder()).build()
                return byebyeRequest(shutdownRequest, message.channel)
            }
            Control.ControlMsgType.MESSAGE_BYEBYE_RESPONSE_VALUE -> {
                AppLog.i("Byebye Response received")
                return -1
            }
            Control.ControlMsgType.MESSAGE_VOICE_SESSION_NOTIFICATION_VALUE -> {
                val voiceRequest = message.parse(Control.VoiceSessionNotification.newBuilder()).build()
                return voiceSessionNotification(voiceRequest)
            }
            Control.ControlMsgType.MESSAGE_AUDIO_FOCUS_REQUEST_VALUE -> {
                val audioFocusRequest = message.parse(Control.AudioFocusRequestNotification.newBuilder()).build()
                return audioFocusRequest(audioFocusRequest, message.channel)
            }
            Control.ControlMsgType.MESSAGE_CHANNEL_CLOSE_NOTIFICATION_VALUE -> {
                AppLog.i("RX: Channel Close Notification on chan ${message.channel}")
                return 0
            }
            else -> AppLog.e("Unsupported Control message type: ${message.type}")
        }
        return 0
    }


    private fun serviceDiscoveryRequest(request: Control.ServiceDiscoveryRequest): Int {
        AppLog.i("Service Discovery Request: %s", request.phoneName)

        val msg = ServiceDiscoveryResponse(context)
        aapTransport.send(msg)
        return 0
    }

    private fun pingRequest(request: Control.PingRequest, channel: Int): Int {
        val response = Control.PingResponse.newBuilder()
                .setTimestamp(System.nanoTime())
                .build()

        val msg = AapMessage(channel, Control.ControlMsgType.MESSAGE_PING_RESPONSE_VALUE, response)
        aapTransport.send(msg)
        return 0
    }

    private fun navigationFocusRequest(request: Control.NavFocusRequestNotification, channel: Int): Int {
        AppLog.i("Navigation Focus Request: %s", request.focusType)

        // The navigation focus goes through the car, as in the stock app: granted when the car gives it,
        // native when it refuses. Giving it up is answered as before.
        val projected = request.focusType != Control.NavFocusType.NAV_FOCUS_1
        val granted = if (projected) aapTransport.appFocus.requestNavigation() else {
            aapTransport.appFocus.releaseNavigation()
            true
        }
        val response = Control.NavFocusNotification.newBuilder()
                .setFocusType(if (granted) Control.NavFocusType.NAV_FOCUS_2 else Control.NavFocusType.NAV_FOCUS_1)
                .build()

        val msg = AapMessage(channel, Control.ControlMsgType.MESSAGE_NAV_FOCUS_NOTIFICATION_VALUE, response)
        AppLog.i(msg.toString())

        aapTransport.send(msg)
        return 0
    }

    private fun byebyeRequest(request: Control.ByeByeRequest, channel: Int): Int {
        AppLog.i("!!! RECEIVED BYEBYE REQUEST FROM PHONE !!! Reason: ${request.reason}")

        val msg = AapMessage(channel, Control.ControlMsgType.MESSAGE_BYEBYE_RESPONSE_VALUE, Control.ByeByeResponse.newBuilder().build())
        AppLog.i("Sending BYEYERESPONSE")
        aapTransport.send(msg)
        Utils.ms_sleep(500)
        // The user ended the session on the phone: that is an exit, not a loss, so nothing restarts
        // the connection behind their back.
        if (request.reason == Control.ByeByeReason.USER_SELECTION) aapTransport.wasUserExit = true
        AppLog.i("Calling aapTransport.quit(clean=true) (userExit=${aapTransport.wasUserExit})")
        aapTransport.quit(clean = true)
        return -1
    }

    private fun voiceSessionNotification(request: Control.VoiceSessionNotification): Int {
        if (request.status == Control.VoiceSessionNotification.VoiceSessionStatus.VOICE_STATUS_START) {
            AppLog.i("Voice Session Notification: START")
            aapTransport.isAssistantActive = true
            aapTransport.appFocus.requestVoice()
            OemSignals.screenOn(context, "voice session")
        } else if (request.status == Control.VoiceSessionNotification.VoiceSessionStatus.VOICE_STATUS_STOP) {
            AppLog.i("Voice Session Notification: STOP")
            aapTransport.isAssistantActive = false
            aapTransport.appFocus.releaseVoice()
        }
        return 0
    }

    private fun audioFocusRequest(notification: Control.AudioFocusRequestNotification, channel: Int): Int {
        AppLog.i("Audio Focus Request: ${notification.request}")

        val mappedState = focusResponse[notification.request]

        // Like the stock app, the system focus is asked for first and the phone is answered with the
        // outcome: the state it asked for when the head unit got the focus (or made no request, see
        // below), a loss when the system refused. The phone then keeps its audio when the car
        // really could not play it, instead of streaming into a source nobody hears.
        var systemResult: Int? = null
        if (settings.enableAudioSink) {
            val separate = settings.separateAudioStreams
            systemResult = aapAudio.requestFocusChange(
                AudioConfigs.stream(channel, separate),
                notification.request.number,
                AudioManager.OnAudioFocusChangeListener {
                    AppLog.i("System audio focus changed: $it ${systemFocusName[it]}")
                },
            )
        } else {
            AppLog.i("Audio Sink disabled - skipping system audio focus request for channel ${Channel.name(channel)}")
        }

        if (mappedState != null) {
            val refused = systemResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED
            val state = if (refused) Control.AudioFocusNotification.AudioFocusStateType.STATE_LOSS else mappedState
            val response = Control.AudioFocusNotification.newBuilder()
                .setFocusState(state)
                .build()
            AppLog.i("Sending AudioFocusNotification: $state" +
                (if (refused) " (the system refused the focus)" else if (systemResult == null) " (no system request made)" else ""))
            aapTransport.send(AapMessage(channel, Control.ControlMsgType.MESSAGE_AUDIO_FOCUS_NOTIFICATION_VALUE, response))

            // Sync MediaSession
            val isGain = state == Control.AudioFocusNotification.AudioFocusStateType.STATE_GAIN
            aapTransport.onAudioFocusStateChanged?.invoke(isGain)
        }

        return 0
    }

    companion object {
        private val systemFocusName = mapOf(
                AudioManager.AUDIOFOCUS_GAIN to "AUDIOFOCUS_GAIN",
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT to "AUDIOFOCUS_GAIN_TRANSIENT",
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE to "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE",
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK to "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK",
                AudioManager.AUDIOFOCUS_LOSS to "AUDIOFOCUS_LOSS",
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT to "AUDIOFOCUS_LOSS_TRANSIENT",
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK to "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK",
                AudioManager.AUDIOFOCUS_NONE to "AUDIOFOCUS_NONE"
        )

        private val focusResponse = mapOf(
            Control.AudioFocusRequestNotification.AudioFocusRequestType.RELEASE to Control.AudioFocusNotification.AudioFocusStateType.STATE_LOSS,
            Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN to Control.AudioFocusNotification.AudioFocusStateType.STATE_GAIN,
            Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_TRANSIENT to Control.AudioFocusNotification.AudioFocusStateType.STATE_GAIN_TRANSIENT,
            Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_TRANSIENT_MAY_DUCK to Control.AudioFocusNotification.AudioFocusStateType.STATE_GAIN_TRANSIENT_GUIDANCE_ONLY
        )
    }
}

internal class AapControlGateway(
        private val aapTransport: AapTransport,
        private val serviceControl: AapControl,
        private val mediaControl: AapControl,
        private val touchControl: AapControl,
        private val sensorControl: AapControl): AapControl {

    constructor(aapTransport: AapTransport,
                micRecorder: MicRecorder,
                aapAudio: AapAudio,
                settings: Settings,
                context: Context) : this(
            aapTransport,
            AapControlService(aapTransport, aapAudio, settings, context),
            AapControlMedia(aapTransport, micRecorder, aapAudio),
            AapControlTouch(aapTransport),
            AapControlSensor(aapTransport, context, settings))

    override fun execute(message: AapMessage): Int {
        if (message.type == 7) {
            val request = message.parse(Control.ChannelOpenRequest.newBuilder()).build()
            return channelOpenRequest(request, message.channel)
        }

        when (message.channel) {
            Channel.ID_CTR -> return serviceControl.execute(message)
            Channel.ID_INP -> return touchControl.execute(message)
            Channel.ID_SEN -> return sensorControl.execute(message)
            Channel.ID_VID, Channel.ID_AUD, Channel.ID_AU1, Channel.ID_AU2, Channel.ID_MIC -> return mediaControl.execute(message)
        }
        return 0
    }

    private fun channelOpenRequest(request: Control.ChannelOpenRequest, channel: Int): Int {
        val msg = AapMessage(channel, Control.ControlMsgType.MESSAGE_CHANNEL_OPEN_RESPONSE_VALUE, Control.ChannelOpenResponse.newBuilder()
                .setStatus(Common.MessageStatus.STATUS_SUCCESS)
                .build())
        aapTransport.send(msg)

        if (channel == Channel.ID_SEN) {
            aapTransport.send(DrivingStatusEvent(Sensors.SensorBatch.DrivingStatusData.Status.UNRESTRICTED))
        }
        return 0
    }
}
