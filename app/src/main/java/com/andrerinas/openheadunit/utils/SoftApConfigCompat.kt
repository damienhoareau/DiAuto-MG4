package com.andrerinas.openheadunit.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.aap.ApBand
import com.andrerinas.openheadunit.aap.SoftApBandPolicy

/**
 * Hotspot configuration: the SoftApConfiguration API via reflection on Android 11+ (API 30), and
 * [CarHotspotConfigurator] (5 GHz, WPA2-PSK, fixed channel, as the OEM stack does) before that.
 */
object SoftApConfigCompat {
    private const val TAG = "SoftApConfigCompat"

    /**
     * Configures the hotspot for [band], keeping its existing name and passphrase.
     * Returns true if the hotspot was successfully configured, false otherwise.
     *
     * The caller must ensure that Wi‑Fi is disabled beforehand (handled by HotspotManager).
     */
    fun enableHotspot(context: Context, enabled: Boolean, band: ApBand = ApBand.BAND_5GHZ): Boolean {
        if (!enabled) return false // disabling handled by legacy path
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return CarHotspotConfigurator.apply(context, band)
        }

        // Ensure location permission (required for hotspot config on API 30+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasLocation) {
                AppLog.e("SoftApConfigCompat: Missing ACCESS_FINE_LOCATION permission – cannot configure hotspot")
                return false
            }
        }

        // Log entry for debugging
        AppLog.i("SoftApConfigCompat: enableHotspot called (API=${Build.VERSION.SDK_INT})")
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Build SoftApConfiguration via reflection, from the one already on the device where
            // there is one. This access point is usually the user's, and it is the one whose name
            // and passphrase have already gone out to the phone — rewriting either here renames
            // somebody's hotspot behind their back and invalidates credentials that were correct
            // when they were sent. The same reasoning is already spelled out on the pre-R path
            // below; only this branch was inventing values.
            val builderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
            val existing = readSoftApConfiguration(wifiManager)
            if (existing == null) {
                // Nothing to copy: an empty builder would hand the framework a configuration with
                // no SSID at all, which is how a working access point loses its name.
                AppLog.w("SoftApConfigCompat: cannot read this device's access point configuration, so the band cannot be requested without risking the existing one. Starting the hotspot unconfigured.")
                return false
            }
            val builder = builderClass
                .getDeclaredConstructor(Class.forName("android.net.wifi.SoftApConfiguration"))
                .newInstance(existing)

            // Ask for the band the link needs. Not fatal if the platform refuses the call: the
            // caller confirms the access point afterwards and retries on the other band, so a
            // failure here just means this attempt runs on whatever the framework picks.
            try {
                builderClass.getMethod("setBand", Int::class.javaPrimitiveType)
                    .invoke(builder, SoftApBandPolicy.softApConfigurationBand(band))
                AppLog.i("SoftApConfigCompat: requesting ${SoftApBandPolicy.describe(band)} for the access point.")
            } catch (e: Exception) {
                AppLog.w("SoftApConfigCompat: could not request ${SoftApBandPolicy.describe(band)} (${e.message}); the framework will choose the band.")
            }

            // Build the configuration object
            val buildMethod = builderClass.getMethod("build")
            val softApConfig = buildMethod.invoke(builder)

            // Apply the configuration via WifiManager#setSoftApConfiguration
            val setConfigMethod = wifiManager.javaClass.getMethod(
                "setSoftApConfiguration",
                Class.forName("android.net.wifi.SoftApConfiguration")
            )
            val result = setConfigMethod.invoke(wifiManager, softApConfig) as Boolean
            AppLog.i("SoftApConfiguration applied. Success=$result")
            result
        } catch (e: Exception) {
            // Expected on plenty of head units, which refuse setSoftApConfiguration() outright with
            // "App not allowed to read or update stored WiFi Ap config". Not fatal and not worth an
            // error: the caller has other start paths and confirms the access point afterwards.
            AppLog.w("SoftApConfigCompat: could not configure the access point (${e.message}); leaving it as the device has it and starting it anyway.")
            false
        }
    }

    /** The access point this device already has configured, or null if it will not say. */
    private fun readSoftApConfiguration(wifiManager: WifiManager): Any? = try {
        wifiManager.javaClass.getMethod("getSoftApConfiguration").invoke(wifiManager)
    } catch (e: Exception) {
        AppLog.d("SoftApConfigCompat: could not read the current access point configuration: ${e.message}")
        null
    }
}
