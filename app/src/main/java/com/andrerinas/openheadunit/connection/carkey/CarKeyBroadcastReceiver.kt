package com.andrerinas.openheadunit.connection.carkey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.connection.CommManager
import com.andrerinas.openheadunit.utils.AppLog
import com.evsuite.hardware.PhysicalButtonEventDecoder

/**
 * The MG4's steering-wheel keys, taken from the car's hardkey broadcast the way the stock Allgo app
 * takes them: next, previous and mute (87, 88, 164) go to the phone on a short press, and the voice key
 * toggles the assistant. The wheel's own left, right and OK codes (299 to 301) are sent as previous, next
 * and play/pause. The broadcast is not aborted.
 */
class CarKeyBroadcastReceiver : BroadcastReceiver(), CarKeyReceiver {

    companion object {
        private const val ACTION_HARDKEY = "com.saic.keyevent.hardkey.report"
        private const val EXTRA_KEYCODE = "android.intent.extra.hardkey.keycode"
        private const val EXTRA_DOWN = "android.intent.extra.hardkey.down"
        private const val EXTRA_LONGPRESS = "android.intent.extra.hardkey.longpress"
        private const val KEYCODE_MUTE = 164

        // What the MG4 wheel really sends (seen with adb): the car's own media layer is the only one that
        // acts on these, and not while Android Auto is in front, so they go to the phone here.
        private const val WHEEL_LEFT = 299
        private const val WHEEL_RIGHT = 300
        private const val WHEEL_OK = 301
    }

    private var context: Context? = null
    private val decoder = PhysicalButtonEventDecoder()

    /** Whether the last mute press paused the phone, so that the next one resumes it. */
    private var muteSent = false

    override val isSupported = true
    override val isSUNeeded = false

    override fun register(context: Context) {
        this.context = context
        ContextCompat.registerReceiver(
            context,
            this,
            IntentFilter(ACTION_HARDKEY).apply { priority = 1000 },
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun unregister() {
        this.context?.unregisterReceiver(this)
        this.context = null
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_HARDKEY) return
        val keyCode = intent.getIntExtra(EXTRA_KEYCODE, -1)
        if (keyCode == -1) return
        val isDown = intent.getBooleanExtra(EXTRA_DOWN, false)
        val isLong = intent.getBooleanExtra(EXTRA_LONGPRESS, false)
        AppLog.i("CarKeyReceiver: hardkey code=$keyCode down=$isDown long=$isLong")

        // Like the stock app, these three go to the phone only while the head unit does not hold the audio
        // focus (its isAAPhasGain): while it plays, the car's own media layer carries next, previous and
        // mute to the phone, and both would skip twice.
        if (isDown && !isLong && (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == KEYCODE_MUTE)) {
            val commManager = App.provide(context).commManager
            if (!commManager.isConnected) muteSent = false
            if (commManager.phoneHasAudioGain()) {
                AppLog.i("CarKeyReceiver: key $keyCode left to the car, the phone holds the audio focus")
            } else if (keyCode == KEYCODE_MUTE) {
                onMute(context, commManager)
            } else {
                handleClick(context, keyCode)
            }
        }

        if (isDown && !isLong) {
            when (keyCode) {
                WHEEL_LEFT -> handleClick(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                WHEEL_RIGHT -> handleClick(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                WHEEL_OK -> handleClick(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
        }

        val event = decoder.accept(keyCode, isDown, isLong) ?: return
        if (event.button == PhysicalButtonEventDecoder.Button.ASSISTANT &&
            event.press != PhysicalButtonEventDecoder.Press.LONG) {
            toggleVoiceAssistant(context)
        }
    }

    /**
     * Mute pauses the phone, and the next press resumes it. A voice session that is running is
     * closed first, then the phone plays again.
     */
    private fun onMute(context: Context, commManager: CommManager) {
        if (!muteSent) {
            handleClick(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
            muteSent = true
            return
        }
        if (commManager.isAssistantActive()) {
            commManager.sendToggleVoiceAssistant()
            Thread.sleep(100)
        }
        handleClick(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        muteSent = false
    }
}
