package com.allgo.app.androidauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andrerinas.openheadunit.utils.AppLog

/** Same boot hook as the Allgo app: brings the (lightweight) AndroidAutoService component up. */
class AndroidAutoStarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLog.i("AndroidAutoStarter: BOOT_COMPLETED, starting AndroidAutoService")
        context.startService(Intent(context, AndroidAutoService::class.java))
    }
}
