package com.andrerinas.openheadunit.connection

import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.andrerinas.openheadunit.utils.AppLog

/**
 * The Wi-Fi network the head unit is itself a client of, described the way the phone is told about
 * the head unit's own hotspot: name, passphrase, address and BSSID.
 *
 * Probe only. A phone that is on this same network does not need the head unit's access point, and
 * whether the phone accepts being sent here (above all when it is the one hosting the network) is
 * what this is meant to find out. Switched on with `setprop debug.diauto.sharednet 1`.
 */
internal object SharedWifiNetwork {
    private const val TAG = "SharedWifi"
    private const val PROPERTY = "debug.diauto.sharednet"

    data class Network(val ssid: String, val psk: String, val ip: String, val bssid: String)

    fun probeEnabled(): Boolean = try {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java, String::class.java)
            .invoke(null, PROPERTY, "") == "1"
    } catch (e: Throwable) {
        false
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
