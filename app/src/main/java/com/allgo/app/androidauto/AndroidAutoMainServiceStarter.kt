package com.allgo.app.androidauto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andrerinas.openheadunit.utils.AppLog

/**
 * Same boot hook as the Allgo app: starts AndroidAutoMainService, which RemoteUI binds to. No
 * activity is launched; the app stays in the background until a phone connects.
 */
class AndroidAutoMainServiceStarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLog.i("AndroidAutoMainServiceStarter: BOOT_COMPLETED, starting AndroidAutoMainService")
        context.startService(Intent(context, AndroidAutoMainService::class.java))
    }
}
