package com.andrerinas.openheadunit.aap

import java.security.SecureRandom

/**
 * What the head unit's own hotspot has to look like for wireless Android Auto, decided the way the
 * OEM stack decides it (Yanfeng `CarAdapterWifiManager` / `CarWifiMgr`): 5 GHz, WPA2-PSK, on the
 * channel named by `persist.sys.connectivity.defaultwifichannel` when the firmware sets one.
 *
 * Without that property the channel is left to the driver (auto). The OEM stack's own fallback of
 * 149 is not applied: on the MG4 (mt2712) asking for 149 made the 5 GHz access point fail to come
 * up at all, which pushed the hotspot down to 2.4 GHz.
 *
 * The user never types an SSID or a password: the hotspot's existing name and passphrase are read
 * back and handed to the phone, and are only generated here when the unit has none.
 */
object CarHotspotPolicy {

    /** `WifiConfiguration.KeyMgmt.WPA2_PSK`. */
    const val KEY_MGMT_WPA2_PSK = 4

    /** `WifiConfiguration.apBand` value for 5 GHz. */
    const val AP_BAND_5GHZ = 1

    /** Channels the OEM stack accepts for the 5 GHz access point. */
    val ALLOWED_CHANNELS = listOf(149, 153, 157, 161, 165, 48)

    const val DEFAULT_SSID = "DiAuto-AA"

    private const val MIN_PASSPHRASE = 8
    private const val GENERATED_PASSPHRASE_LENGTH = 12
    private const val PASSPHRASE_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** The channel named by the system property, or null (leave it to the driver) if absent or not allowed. */
    fun channelFromProperty(value: String?): Int? =
        value?.trim()?.toIntOrNull()?.takeIf { it in ALLOWED_CHANNELS }

    /** The access point as the framework currently has it configured. */
    data class Snapshot(
        val ssid: String,
        val passphrase: String,
        val apBand: Int,
        val apChannel: Int,
        val wpa2: Boolean
    )

    /**
     * Why a configuration cannot be used as it stands, or null if it can. Mirrors `CarWifiMgr`,
     * which accepts only band 5 GHz with WPA2-PSK. The channel is checked only when the firmware
     * names one ([channel] non-null).
     */
    fun problem(config: Snapshot?, channel: Int?): String? = when {
        config == null -> "unreadable"
        config.ssid.isBlank() -> "no name"
        config.passphrase.length < MIN_PASSPHRASE -> "no usable passphrase"
        !config.wpa2 -> "not WPA2-PSK"
        config.apBand != AP_BAND_5GHZ -> "not on 5 GHz"
        channel != null && config.apChannel != channel -> "channel ${config.apChannel} instead of $channel"
        else -> null
    }

    /** The name to use: the existing one, or [DEFAULT_SSID] when the unit has none. */
    fun ssidOrDefault(existing: String?): String = existing?.takeIf { it.isNotBlank() } ?: DEFAULT_SSID

    /** The passphrase to use: the existing one if it is valid for WPA2, otherwise a generated one. */
    fun passphraseOrGenerate(existing: String?, random: SecureRandom = SecureRandom()): String {
        if (existing != null && existing.length in MIN_PASSPHRASE..63) return existing
        return buildString {
            repeat(GENERATED_PASSPHRASE_LENGTH) {
                append(PASSPHRASE_ALPHABET[random.nextInt(PASSPHRASE_ALPHABET.length)])
            }
        }
    }
}
