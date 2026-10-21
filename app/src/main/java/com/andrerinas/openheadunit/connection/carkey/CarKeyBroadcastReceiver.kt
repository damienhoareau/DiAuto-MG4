package com.andrerinas.openheadunit.connection.carkey

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.andrerinas.openheadunit.utils.AppLog
import com.evsuite.hardware.PhysicalButtonEventDecoder

/**
 * A comprehensive receiver for steering wheel and hardware buttons,
 * covering both standard Android events and proprietary Chinese headunit broadcasts.
 */
class CarKeyBroadcastReceiver : BroadcastReceiver(), CarKeyReceiver {

    companion object {
        /** SAIC / MG4 AllGo hardkey bus (steering wheel). */
        private const val ACTION_SAIC_HARDKEY = "com.saic.keyevent.hardkey.report"
        private const val EXTRA_SAIC_KEYCODE = "android.intent.extra.hardkey.keycode"
        private const val EXTRA_SAIC_DOWN = "android.intent.extra.hardkey.down"
        private const val EXTRA_SAIC_LONGPRESS = "android.intent.extra.hardkey.longpress"
        private const val KEYCODE_MUTE = 164

        val ACTIONS = arrayOf(
            "android.intent.action.MEDIA_BUTTON",
            "hy.intent.action.MEDIA_BUTTON", // Huayu / Hyundai Protocol
            "geely.intent.action.MEDIA_BUTTON", // Geely / Flyme Auto
            "ecarx.intent.action.MEDIA_BUTTON", // ECARX
            "com.ecarx.intent.action.MEDIA_BUTTON",
            "com.geely.intent.action.MEDIA_BUTTON",
            "com.ecarx.media.action.KEY_EVENT",
            "com.geely.car.media.action.KEY_EVENT",
            "com.nwd.action.ACTION_KEY_VALUE", // NWD (NewWell)
            "com.microntek.irkeyUp", // Microntek (MTCE/MTCB)
            "com.microntek.irkeyDown",
            "com.winca.service.Setting.KEY_ACTION", // Winca
            "android.intent.action.C3_HARDKEY", // FlyAudio / C3
            "IKeyClick.KEY_CLICK",
            "com.eryanet.music.prev", // Eryanet (Eonon etc.)
            "com.eryanet.music.next",
            "com.eryanet.media.playorpause",
            "com.eryanet.media.play",
            "com.eryanet.media.pause",
            "com.bz.action.phone.pickup", // BZ (Joying etc.)
            "com.bz.action.phone.hangup",
            "com.tencent.qqmusiccar.action.MEDIA_BUTTON_INNER_ONKEY",
            "cn.kuwo.kwmusicauto.action.MEDIA_BUTTON",
            ACTION_SAIC_HARDKEY,
        )
    }

    private var context: Context? = null
    private val saicDecoder = PhysicalButtonEventDecoder()
    private var muteSent = false

    override val isSupported = true
    override val isSUNeeded = false

    override fun register(context: Context) {
        this.context = context

        val filter = IntentFilter().apply {
            priority = 1000
            ACTIONS.forEach { addAction(it) }
        }

        ContextCompat.registerReceiver(
            context,
            this,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun unregister() {
        this.context?.unregisterReceiver(this)
        this.context = null
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        AppLog.i("CarKeyReceiver: Handling intent action: $action")

        // Broadcast for KeymapFragment debugger (raw intent data)
        context.sendBroadcast(Intent("com.andrerinas.openheadunit.DEBUG_KEY").apply {
            setPackage(context.packageName)
            putExtra("action", action)
            intent.extras?.let { putExtras(it) }
        })

        // The car's steering-wheel bus is left to the car: its media stack turns the keys into track
        // changes for the phone, as for the stock app. Only the voice key is taken here.
        if (action == ACTION_SAIC_HARDKEY) {
            handleSaicHardkey(context, intent)
            return
        }

        // Try to abort broadcast to prevent other apps (like built-in radio) from reacting
        if (isOrderedBroadcast) {
            abortBroadcast()
        }

        // 1. Standard / OEM Media Button extraction (already has KeyEvent with proper DOWN/UP)
        if (action.endsWith("MEDIA_BUTTON") || action.endsWith("KEY_EVENT")
            || action == "com.tencent.qqmusiccar.action.MEDIA_BUTTON_INNER_ONKEY"
            || action == "cn.kuwo.kwmusicauto.action.MEDIA_BUTTON") {
            val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            if (event != null) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    handleKey(context, event.keyCode, true)
                } else if (event.action == KeyEvent.ACTION_UP) {
                    handleKey(context, event.keyCode, false)
                }
            } else {
                val cmd = intent.getStringExtra("command") ?: intent.getStringExtra("cmd")
                val key = intent.getIntExtra("keycode", -1).takeIf { it > 0 }
                    ?: intent.getIntExtra("key_code", -1).takeIf { it > 0 }

                if (cmd != null) {
                    when (cmd.lowercase()) {
                        "next", "skip_next", "skip" -> handleClick(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                        "previous", "skip_previous", "prev" -> handleClick(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                        "play", "start" -> handleClick(context, KeyEvent.KEYCODE_MEDIA_PLAY)
                        "pause" -> handleClick(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
                        "togglepause", "playpause" -> handleClick(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    }
                } else if (key != null) {
                    handleClick(context, key)
                }
            }
            return
        }

        // 2. Proprietary extraction — extract keycode or use virtual IDs for mapping
        when (action) {
            // --- Protocols with proper DOWN/UP separation ---
            "com.microntek.irkeyDown", "com.microntek.irkeyUp" -> {
                val keyCode = intent.getIntExtra("keyCode", -1)
                if (keyCode != -1) handleKey(context, keyCode, action.endsWith("keyDown"))
            }
            "android.intent.action.C3_HARDKEY" -> {
                val keyCode = intent.getIntExtra("android.intent.extra.c3_hardkey_keycode", -1)
                val c3Action = intent.getIntExtra("android.intent.extra.c3_hardkey_action", -1)
                if (keyCode != -1 && c3Action != -1) handleKey(context, keyCode, c3Action == 0)
            }
            ACTION_SAIC_HARDKEY -> handleSaicHardkey(context, intent)

            // --- Protocols that fire once (no DOWN/UP) → use virtual IDs for mapping ---
            "com.nwd.action.ACTION_KEY_VALUE" -> {
                val value = intent.getByteExtra("extra_key_value", 0).toInt()
                if (value != 0) handleClick(context, 1000 + value)
            }
            "com.winca.service.Setting.KEY_ACTION" ->
                intent.getIntExtra("com.winca.service.Setting.KEY_ACTION_EXTRA", -1)
                    .takeIf { it != -1 }?.let { handleClick(context, it) }
            "IKeyClick.KEY_CLICK" ->
                intent.getIntExtra("CLICK_KEY", -1)
                    .takeIf { it != -1 }?.let { handleClick(context, it) }
            "com.eryanet.music.prev" -> handleClick(context, 2001)
            "com.eryanet.music.next" -> handleClick(context, 2002)
            "com.eryanet.media.playorpause" -> handleClick(context, 2003)
            "com.eryanet.media.play" -> handleClick(context, 2004)
            "com.eryanet.media.pause" -> handleClick(context, 2005)
            "com.bz.action.phone.pickup" -> handleClick(context, 3001)
            "com.bz.action.phone.hangup" -> handleClick(context, 3002)
        }
    }

    /**
     * MG4 / SAIC steering hardkeys, as the stock app takes them: next, previous and mute (87, 88, 164)
     * go to the phone, and so does the voice key. The broadcast is not aborted and the raw wheel codes
     * (297 to 301) are left alone.
     */
    private fun handleSaicHardkey(context: Context, intent: Intent) {
        var keyCode = intent.getIntExtra(EXTRA_SAIC_KEYCODE, -1)
        if (keyCode == -1) keyCode = intent.getIntExtra("keycode", -1)
        if (keyCode == -1) keyCode = intent.getIntExtra("keyCode", -1)
        if (keyCode == -1) return

        val isDown = intent.getBooleanExtra(EXTRA_SAIC_DOWN, false)
        val isLong = intent.getBooleanExtra(EXTRA_SAIC_LONGPRESS, false)

        AppLog.i("CarKeyReceiver: hardkey code=$keyCode down=$isDown long=$isLong")

        // Next, previous and mute come from the car's media layer as 87, 88 and 164, and only then:
        // the stock app sends them to the phone the same way and ignores the raw wheel codes.
        if (isDown && !isLong) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> handleClick(context, keyCode)
                KEYCODE_MUTE -> {
                    handleClick(context, if (muteSent) KeyEvent.KEYCODE_MEDIA_PLAY else KeyEvent.KEYCODE_MEDIA_PAUSE)
                    muteSent = !muteSent
                }
            }
        }

        val event = saicDecoder.accept(keyCode, isDown, isLong) ?: return
        if (event.button == PhysicalButtonEventDecoder.Button.ASSISTANT &&
            event.press != PhysicalButtonEventDecoder.Press.LONG) {
            toggleVoiceAssistant(context)
        }
    }
}
