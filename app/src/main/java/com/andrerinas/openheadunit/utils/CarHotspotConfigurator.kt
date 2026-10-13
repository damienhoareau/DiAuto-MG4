package com.andrerinas.openheadunit.utils

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.andrerinas.openheadunit.aap.ApBand
import com.andrerinas.openheadunit.aap.CarHotspotPolicy

/**
 * Brings the head unit's own hotspot into the shape wireless Android Auto needs, the way the OEM
 * stack does it (`CarAdapterWifiManager.a(ApConfig)`): read the configuration with
 * `getWifiApConfiguration`, set 5 GHz, the channel and WPA2-PSK on it, and write it back with
 * `setWifiApConfiguration`. Pre-Android-11 only; later versions use `SoftApConfigCompat`.
 *
 * Nothing here asks the user for anything. The existing name and passphrase are kept, and only
 * generated when the unit has none. Needs the platform-signed (`android.uid.system`) install.
 */
object CarHotspotConfigurator {

    private const val CHANNEL_PROPERTY = "persist.sys.connectivity.defaultwifichannel"

    private fun wifiManager(context: Context) =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /** The channel the firmware asks for, or null to leave it to the driver. */
    private fun targetChannel(): Int? = try {
        val sp = Class.forName("android.os.SystemProperties")
        CarHotspotPolicy.channelFromProperty(sp.getMethod("get", String::class.java).invoke(null, CHANNEL_PROPERTY) as? String)
    } catch (e: Exception) {
        null
    }

    private fun readConfig(wm: WifiManager): WifiConfiguration? = try {
        wm.javaClass.getMethod("getWifiApConfiguration").invoke(wm) as? WifiConfiguration
    } catch (e: Exception) {
        AppLog.d("CarHotspotConfigurator: getWifiApConfiguration unavailable: ${e.message}")
        null
    }

    private fun intField(config: WifiConfiguration, name: String): Int = try {
        config.javaClass.getField(name).getInt(config)
    } catch (e: Exception) {
        -1
    }

    private fun snapshot(config: WifiConfiguration) = CarHotspotPolicy.Snapshot(
        ssid = HotspotConfigReader.unquote(config.SSID),
        passphrase = HotspotConfigReader.unquote(config.preSharedKey),
        apBand = intField(config, "apBand"),
        apChannel = intField(config, "apChannel"),
        wpa2 = config.allowedKeyManagement.get(CarHotspotPolicy.KEY_MGMT_WPA2_PSK)
    )

    /** Whether the hotspot already is what the phone needs; logs why not otherwise. */
    fun isValid(context: Context): Boolean {
        val config = readConfig(wifiManager(context))
        val problem = CarHotspotPolicy.problem(config?.let(::snapshot), targetChannel())
        if (problem != null) AppLog.i("CarHotspotConfigurator: hotspot configuration needs fixing ($problem).")
        return problem == null
    }

    /**
     * Writes the configuration for [band], keeping the existing name and passphrase. Returns true
     * if the framework accepted it. 2.4 GHz is only the fallback for a radio that will not host 5 GHz.
     */
    fun apply(context: Context, band: ApBand): Boolean = try {
        val wm = wifiManager(context)
        val config = readConfig(wm) ?: WifiConfiguration()
        val before = snapshot(config)
        val ssid = CarHotspotPolicy.ssidOrDefault(before.ssid)
        val psk = CarHotspotPolicy.passphraseOrGenerate(before.passphrase)

        config.SSID = ssid
        config.preSharedKey = psk
        config.allowedKeyManagement.clear()
        config.allowedKeyManagement.set(CarHotspotPolicy.KEY_MGMT_WPA2_PSK)
        val fiveGhz = band == ApBand.BAND_5GHZ
        config.javaClass.getField("apBand").setInt(config, if (fiveGhz) CarHotspotPolicy.AP_BAND_5GHZ else 0)
        val channel = if (fiveGhz) targetChannel() else null
        config.javaClass.getField("apChannel").setInt(config, channel ?: 0)

        val applied = wm.javaClass.getMethod("setWifiApConfiguration", WifiConfiguration::class.java)
            .invoke(wm, config) as? Boolean ?: false
        AppLog.i(
            "CarHotspotConfigurator: hotspot set to ${if (fiveGhz) "5 GHz, channel ${channel ?: "auto"}" else "2.4 GHz"}, " +
                "WPA2-PSK, name ${if (ssid == before.ssid) "kept" else "created"}, " +
                "passphrase ${if (psk == before.passphrase) "kept" else "generated"}. Success=$applied"
        )
        applied
    } catch (e: Exception) {
        AppLog.w("CarHotspotConfigurator: could not write the hotspot configuration: ${e.message}")
        false
    }
}
