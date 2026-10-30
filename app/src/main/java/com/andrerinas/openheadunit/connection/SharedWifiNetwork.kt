package com.andrerinas.openheadunit.connection

import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.andrerinas.openheadunit.utils.AppLog

/**
 * The Wi-Fi network the head unit is itself a client of, described the way the phone is told about
 * the head unit's own hotspot: name, passphrase, address and BSSID.
 *
 * Probe only. A phone that is on this same network does not need the head unit's access point, and
 * whether the phone accepts being sent here (above all when it is the one hosting the network) is
 * what this is meant to find out. On unless `setprop debug.diauto.sharednet 0`.
 *
 * Without a way to know from the car that the phone is on the same network, the network is tried and
 * remembered: a network that was tried and led to no session is left alone for a week, and the car's
 * own hotspot is used for it, so a phone that cannot join (its own hotspot, another place) costs one
 * failed attempt instead of one every trip.
 */
internal object SharedWifiNetwork {
    private const val TAG = "SharedWifi"
    private const val PROPERTY = "debug.diauto.sharednet"

    data class Network(val ssid: String, val psk: String, val ip: String, val bssid: String)

    private const val PREFS = "shared_wifi"
    private const val FAILED_PREFIX = "failed2:"
    private const val FAILED_FOR_MS = 7L * 24 * 60 * 60 * 1000
    private const val MIN_LASTING_MS = 20_000L

    // The network last sent to a phone, until a session shows it worked. Only touched under the lock.
    private var pendingSsid: String? = null

    // The network a running session came up over, and when.
    private var liveSsid: String? = null
    private var liveSince = 0L

    fun probeEnabled(): Boolean = try {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java, String::class.java)
            .invoke(null, PROPERTY, "") != "0"
    } catch (e: Throwable) {
        true
    }

    /**
     * Whether to send the phone to [network]. A network sent earlier that produced no session counts
     * as failed, and is skipped until the week is up.
     */
    @Synchronized
    fun shouldTry(context: Context, network: Network): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pendingSsid?.let { earlier ->
            prefs.edit().putLong(FAILED_PREFIX + earlier, System.currentTimeMillis()).apply()
            AppLog.i("$TAG: '$earlier' was sent to a phone and no session followed, leaving it alone for a week.")
            pendingSsid = null
        }
        val failedAt = prefs.getLong(FAILED_PREFIX + network.ssid, 0L)
        if (failedAt != 0L && System.currentTimeMillis() - failedAt < FAILED_FOR_MS) {
            AppLog.i("$TAG: '${network.ssid}' did not work for a phone before, using the car hotspot.")
            return false
        }
        pendingSsid = network.ssid
        return true
    }

    /** The attempt broke on the car's side or on the link before the phone could answer: it says nothing about the network. */
    @Synchronized
    fun noteNotTried() {
        pendingSsid = null
    }

    /** A session came up over the network sent last, if any. It only counts as working if it lasts. */
    @Synchronized
    fun noteSessionStarted() {
        val ssid = pendingSsid ?: return
        pendingSsid = null
        liveSsid = ssid
        liveSince = SystemClock.elapsedRealtime()
        AppLog.i("$TAG: a session came up over '$ssid'.")
    }

    /** The session ended: one that did not last is the phone refusing the network, so it is left alone for a week. */
    @Synchronized
    fun noteSessionEnded(context: Context) {
        val ssid = liveSsid ?: return
        liveSsid = null
        val lasted = SystemClock.elapsedRealtime() - liveSince
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (lasted < MIN_LASTING_MS) {
            prefs.edit().putLong(FAILED_PREFIX + ssid, System.currentTimeMillis()).apply()
            AppLog.i("$TAG: the session over '$ssid' ended after ${lasted}ms, leaving it alone for a week.")
        } else {
            prefs.edit().remove(FAILED_PREFIX + ssid).apply()
            AppLog.i("$TAG: the session over '$ssid' lasted ${lasted / 1000}s, the network works.")
        }
    }

    @Suppress("DEPRECATION")
    fun current(context: Context): Network? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wm == null || !wm.isWifiEnabled) {
            AppLog.i("$TAG: the car's Wi-Fi client is off, no shared network.")
            return null
        }
        val info = wm.connectionInfo
        if (info == null || info.supplicantState != SupplicantState.COMPLETED || info.networkId < 0) {
            AppLog.i("$TAG: the car's Wi-Fi client is not connected (${info?.supplicantState}), no shared network.")
            return null
        }
        val ssid = info.ssid?.trim('"').orEmpty()
        val bssid = info.bssid.orEmpty().uppercase()
        val ip = formatAddress(info.ipAddress)
        if (ssid.isEmpty() || ssid == "<unknown ssid>" || ip == null) {
            AppLog.i("$TAG: connected, but ssid='$ssid' ip=$ip is not usable.")
            return null
        }
        val psk = passphraseOf(wm, info.networkId)
        if (psk == null) {
            AppLog.w("$TAG: no passphrase readable for '$ssid', no shared network.")
            return null
        }
        AppLog.i("$TAG: the car is on '$ssid' ($ip, BSSID $bssid, passphrase length ${psk.length}).")
        return Network(ssid, psk, ip, bssid)
    }

    /** The saved passphrase, through the system-only call; null when it is open or cannot be read. */
    @Suppress("UNCHECKED_CAST")
    private fun passphraseOf(wm: WifiManager, networkId: Int): String? = try {
        val configs = wm.javaClass.getMethod("getPrivilegedConfiguredNetworks").invoke(wm) as? List<WifiConfiguration>
        configs?.firstOrNull { it.networkId == networkId }?.preSharedKey?.trim('"')?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        AppLog.w("$TAG: reading the saved networks failed: ${e.cause?.message ?: e.message}")
        null
    }

    private fun formatAddress(le: Int): String? =
        if (le == 0) null else "${le and 0xff}.${(le shr 8) and 0xff}.${(le shr 16) and 0xff}.${(le shr 24) and 0xff}"
}
