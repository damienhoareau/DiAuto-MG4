package com.andrerinas.openheadunit.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.andrerinas.openheadunit.aap.AapService
import com.andrerinas.openheadunit.utils.AppLog
import android.os.UserManager
import android.os.Build

class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in BOOT_ACTIONS) return

        AppLog.i("Boot auto-start: received action=$action")

        val isLocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && 
                      !(context.getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked

        if (isLocked) {
            AppLog.w("BootCompleteReceiver: Device is locked. Cannot start AapService yet. Waiting for user unlock.")
            return
        }

        AppLog.i("Boot auto-start: starting AapService with BOOT_START (trigger=$action)")
        val serviceIntent = Intent(context, AapService::class.java).apply {
            putExtra(EXTRA_BOOT_START, true)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val EXTRA_BOOT_START = "com.andrerinas.openheadunit.EXTRA_BOOT_START"

        private val BOOT_ACTIONS = setOf(
            // Standard Android boot
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            // Generic / OEM quick boot
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            // MediaTek IPO (Instant Power On)
            "com.mediatek.intent.action.QUICKBOOT_POWERON",
            "com.mediatek.intent.action.BOOT_IPO",
            // FYT / GLSX head units (ACC ignition wake)
            "com.fyt.boot.ACCON",
            "com.glsx.boot.ACCON",
            "android.intent.action.ACTION_MT_COMMAND_SLEEP_OUT",
            // Microntek / MTCD / PX3 head units (ACC wake)
            "com.cayboy.action.ACC_ON",
            "com.carboy.action.ACC_ON"
        )
    }
}
