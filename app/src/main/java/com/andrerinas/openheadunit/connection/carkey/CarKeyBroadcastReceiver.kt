package com.andrerinas.openheadunit.connection.carkey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.utils.AppLog
import com.evsuite.hardware.PhysicalButtonEventDecoder

/**
 * The MG4's steering-wheel keys, taken from the car's hardkey broadcast the way the stock Allgo app
 * takes them: next, previous and mute (87, 88, 164) go to the phone on a short press, the raw wheel
 * codes (297 to 301) do nothing, and the voice key toggles the assistant. The broadcast is not aborted.
 */
class CarKeyBroadcastReceiver : BroadcastReceiver(), CarKeyReceiver {

    companion object {
        private const val ACTION_HARDKEY = "com.saic.keyevent.hardkey.report"
        private const val EXTRA_KEYCODE = "android.intent.extra.hardkey.keycode"
        private const val EXTRA_DOWN = "android.intent.extra.hardkey.down"
        private const val EXTRA_LONGPRESS = "android.intent.extra.hardkey.longpress"
        private const val KEYCODE_MUTE = 164
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

        if (isDown && !isLong) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> handleClick(context, keyCode)
                KEYCODE_MUTE -> {
                    handleClick(context, if (muteSent) KeyEvent.KEYCODE_MEDIA_PLAY else KeyEvent.KEYCODE_MEDIA_PAUSE)
                    muteSent = !muteSent
                }
            }
        }

        val event = decoder.accept(keyCode, isDown, isLong) ?: return
        if (event.button == PhysicalButtonEventDecoder.Button.ASSISTANT &&
            event.press != PhysicalButtonEventDecoder.Press.LONG) {
            toggleVoiceAssistant(context)
        }
    }
}
