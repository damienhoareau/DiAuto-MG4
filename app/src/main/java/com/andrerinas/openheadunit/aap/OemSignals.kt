package com.andrerinas.openheadunit.aap

import android.content.Context
import android.content.Intent
import com.andrerinas.openheadunit.utils.AppLog

/**
 * The broadcasts of the head unit's own system that the stock Allgo Android Auto app sends and
 * listens to, so the car keeps behaving around a projection as it does with the original.
 */
object OemSignals {
    /** Wakes the head unit's screen: sent on a phone call, a voice request and when the phone leaves. */
    const val ACTION_SCREEN_ON = "com.saicmotor.screen.on"

    /** The surround-view (360) camera took the screen, or gave it back. */
    const val ACTION_AROUND_VIEW_START = "com.saicmotor.hmi.aroundview.ACTION_START"
    const val ACTION_AROUND_VIEW_STOP = "com.saicmotor.hmi.aroundview.ACTION_STOP"

    fun screenOn(context: Context, reason: String) {
        try {
            context.sendBroadcast(Intent(ACTION_SCREEN_ON))
            AppLog.i("OemSignals: screen on ($reason)")
        } catch (e: Exception) {
            AppLog.w("OemSignals: could not send screen on ($reason): ${e.message}")
        }
    }
}
