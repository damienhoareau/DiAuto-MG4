package com.andrerinas.openheadunit.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Switches the head unit's Wi‑Fi client off while a wireless session is being set up, and back on
 * afterwards. See [CarWifiClientPolicy] for why, and for the rules.
 *
 * The fact that we switched it off is stored, so that a process that died in between does not leave
 * the car's Wi‑Fi off for good: the next service start gives it back.
 */
object CarWifiClient {
    private const val TAG = "CarWifiClient"
    private const val PREFS = "car_wifi_client"
    private const val KEY_SUSPENDED = "suspended-by-us"

    private val main = Handler(Looper.getMainLooper())
    private var pendingRestore: Runnable? = null

    @Volatile
    private var startupChecked = false

    private fun wifiManager(context: Context): WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isSystemInstall(): Boolean = Process.myUid() == Process.SYSTEM_UID

    /**
     * Called just before the phone is handed the hotspot's credentials. Returns once the client is
     * off (or after [CarWifiClientPolicy.SETTLE_TIMEOUT_MS]); does nothing when it need not act.
     */
    @Suppress("DEPRECATION")
    suspend fun suspendForWirelessSession(context: Context) {
        cancelPendingRestore()
        val wm = wifiManager(context) ?: return
        val clientWasOn = wm.isWifiEnabled
        if (!CarWifiClientPolicy.shouldSuspend(isSystemInstall(), clientWasOn)) {
            // Already off because of us, in an earlier attempt: keep the safety timer running.
            if (prefs(context).getBoolean(KEY_SUSPENDED, false) && !wm.isWifiEnabled) {
                scheduleRestore(context, CarWifiClientPolicy.IDLE_RESTORE_MS, "no wireless session started")
            }
            return
        }

        val accessPointUp = HotspotManager.isAccessPointUp(context)

        // Written before acting: if we die half way, the next start still knows to give it back.
        prefs(context).edit().putBoolean(KEY_SUSPENDED, true).apply()
        val asked = try {
            wm.setWifiEnabled(false)
        } catch (e: Exception) {
            AppLog.w("$TAG: could not switch the car's Wi-Fi client off (${e.javaClass.simpleName}: ${e.message})")
            false
        }
        var waited = 0L
        while (waited < CarWifiClientPolicy.SETTLE_TIMEOUT_MS && wm.wifiState != WifiManager.WIFI_STATE_DISABLED) {
            delay(100)
            waited += 100
        }
        if (wm.wifiState != WifiManager.WIFI_STATE_DISABLED) {
            AppLog.w("$TAG: the car's Wi-Fi client is still on after ${waited}ms (asked=$asked); the phone may fail to join the hotspot.")
            prefs(context).edit().putBoolean(KEY_SUSPENDED, false).apply()
            return
        }
        AppLog.i("$TAG: car Wi-Fi client switched off for the wireless session (${waited}ms).")
        if (CarWifiClientPolicy.shouldRestartAccessPoint(clientWasOn, accessPointUp)) {
            AppLog.i("$TAG: the hotspot was started while the Wi-Fi client was on, so the radio kept it on the client's channel; restarting it so that it comes up on its own.")
            withContext(Dispatchers.IO) { HotspotManager.restart(context) }
        }
        scheduleRestore(context, CarWifiClientPolicy.IDLE_RESTORE_MS, "no wireless session started")
    }

    /** A wireless session is up: the safety timer is no longer needed. */
    fun noteSessionStarted() {
        cancelPendingRestore()
    }

    /** The session ended: give the Wi‑Fi back, unless the phone reconnects within the grace period. */
    fun restoreLater(context: Context, reason: String) {
        if (!prefs(context).getBoolean(KEY_SUSPENDED, false)) return
        scheduleRestore(context, CarWifiClientPolicy.RECONNECT_GRACE_MS, reason)
    }

    /** Once per process: a Wi‑Fi switched off by a previous run that never gave it back. */
    fun restoreIfOrphaned(context: Context) {
        if (startupChecked) return
        startupChecked = true
        restore(context, "service start")
    }

    /** Switches the client back on if we switched it off and it is still off. */
    @Suppress("DEPRECATION")
    fun restore(context: Context, reason: String) {
        cancelPendingRestore()
        val p = prefs(context)
        if (!p.getBoolean(KEY_SUSPENDED, false)) return
        val wm = wifiManager(context)
        if (wm == null || !CarWifiClientPolicy.shouldRestore(true, wm.isWifiEnabled)) {
            AppLog.i("$TAG: nothing to give back ($reason): the car's Wi-Fi is on again.")
            p.edit().putBoolean(KEY_SUSPENDED, false).apply()
            return
        }
        val ok = try {
            wm.setWifiEnabled(true)
        } catch (e: Exception) {
            AppLog.w("$TAG: could not switch the car's Wi-Fi client back on (${e.javaClass.simpleName}: ${e.message})")
            false
        }
        AppLog.i("$TAG: car Wi-Fi client switched back on ($reason): $ok")
        // Kept when it failed, so the next start tries again.
        if (ok) p.edit().putBoolean(KEY_SUSPENDED, false).apply()
    }

    @Synchronized
    private fun scheduleRestore(context: Context, delayMs: Long, reason: String) {
        cancelPendingRestore()
        val appContext = context.applicationContext
        val runnable = Runnable { restore(appContext, reason) }
        pendingRestore = runnable
        main.postDelayed(runnable, delayMs)
    }

    @Synchronized
    private fun cancelPendingRestore() {
        pendingRestore?.let { main.removeCallbacks(it) }
        pendingRestore = null
    }
}
