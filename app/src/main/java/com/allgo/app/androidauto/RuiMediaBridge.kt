package com.allgo.app.androidauto

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserServiceInterface
import com.andrerinas.openheadunit.aap.AaStatusBus
import com.andrerinas.openheadunit.utils.AppLog

/**
 * Tells the car's media service what the phone is playing, as the stock Allgo app does.
 *
 * The launcher's "now playing" card asks the car's media service, which reads the session of
 * `RemoteUIMediaBrowserService`; that session only has what the Android Auto app pushes through this
 * binder. Without it the card says nothing is playing. The last values are kept and sent again when the
 * service connects, so a track that started before the bind is not lost.
 */
internal class RuiMediaBridge(private val context: Context) {

    private var service: RemoteUIMediaBrowserServiceInterface? = null
    private var binding = false
    private var track: AaStatusBus.Track? = null
    private var playback: AaStatusBus.PlaybackInfo? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = RemoteUIMediaBrowserServiceInterface.Stub.asInterface(binder)
            AppLog.i("RuiMediaBridge: connected to the car's media browser service")
            synchronized(this@RuiMediaBridge) {
                track?.let { push { s -> s.onMetaDataCb(RuiMediaMapper.metadata(it)) } }
                playback?.let { push { s -> s.onPlayBackDataCb(RuiMediaMapper.playback(it)) } }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    @Synchronized
    fun start() {
        if (binding) return
        val intent = Intent().setComponent(COMPONENT).setAction(ACTION)
        binding = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            AppLog.w("RuiMediaBridge: could not bind the media browser service: ${e.message}")
            false
        }
        if (!binding) AppLog.w("RuiMediaBridge: the media browser service is not there; the launcher will not show the track")
    }

    @Synchronized
    fun stop() {
        if (!binding) return
        try { context.unbindService(connection) } catch (_: Exception) {}
        binding = false
        service = null
    }

    @Synchronized
    fun onTrack(value: AaStatusBus.Track?) {
        track = value
        if (value == null) return
        push { it.onMetaDataCb(RuiMediaMapper.metadata(value)) }
    }

    @Synchronized
    fun onPlayback(value: AaStatusBus.PlaybackInfo?) {
        playback = value
        if (value == null) return
        push { it.onPlayBackDataCb(RuiMediaMapper.playback(value)) }
    }

    /** The phone went away: like the stock app, tell the service the session is not up. */
    @Synchronized
    fun onSessionEnded() {
        track = null
        playback = null
        push { it.onSetSessionStatusCb(false) }
    }

    private fun push(call: (RemoteUIMediaBrowserServiceInterface) -> Unit) {
        val s = service ?: return
        try {
            call(s)
        } catch (e: Exception) {
            AppLog.w("RuiMediaBridge: the media browser service did not take it: ${e.message}")
        }
    }

    private companion object {
        val COMPONENT = ComponentName(
            "com.allgo.remoteui.mediabrowserservice",
            "com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserService",
        )
        const val ACTION = "com.allgo.action.aapservice"
    }
}
