package com.andrerinas.openheadunit.aap

/**
 * How long a Bluetooth handshake waits for the car's Wi-Fi client before giving up on the shared
 * network and sending the phone to the car hotspot.
 *
 * At ignition the phone's Bluetooth comes back in a second or two, while the car's Wi-Fi client is
 * still scanning, associating or waiting for a DHCP lease. Choosing the network at that instant
 * picks the hotspot every time, and the hotspot branch then switches the car's client off, so the
 * shared network cannot win for the rest of the trip. A short wait while the client is visibly on
 * its way fixes that, and costs nothing when it is off or idle.
 *
 * The budget is bounded by the phone's own first-message timer (~12 s from the moment it opens the
 * RFCOMM channel): the version request has to go out well before it.
 */
object SharedWifiJoinPolicy {

    enum class Phase {
        /** The client is off: nothing to wait for. */
        OFF,
        /** On but not trying any network (disconnected, inactive, dormant...): nothing to wait for. */
        IDLE,
        /** Being switched on, or scanning for a saved network. */
        SCANNING,
        /** Authenticating, associating, handshaking, or connected without an address yet. */
        JOINING,
        /** Connected with an address: the shared network can be read now. */
        READY,
    }

    const val SCANNING_WAIT_MS = 3_000L
    const val JOINING_WAIT_MS = 6_000L
    const val POLL_MS = 250L

    private val JOINING_STATES = setOf(
        "AUTHENTICATING", "ASSOCIATING", "ASSOCIATED", "FOUR_WAY_HANDSHAKE", "GROUP_HANDSHAKE",
    )

    /**
     * @param wifiEnabling the client is being switched on (`WIFI_STATE_ENABLING`).
     * @param supplicantState the name of the `SupplicantState`, or null when unknown.
     */
    fun phaseOf(wifiEnabled: Boolean, wifiEnabling: Boolean, supplicantState: String?, hasIp: Boolean): Phase = when {
        wifiEnabling -> Phase.SCANNING
        !wifiEnabled -> Phase.OFF
        supplicantState == "COMPLETED" -> if (hasIp) Phase.READY else Phase.JOINING
        supplicantState in JOINING_STATES -> Phase.JOINING
        supplicantState == "SCANNING" -> Phase.SCANNING
        else -> Phase.IDLE
    }

    /** How long, from the start of the wait, the handshake may keep waiting in [phase]. */
    fun maxWaitMs(phase: Phase): Long = when (phase) {
        Phase.SCANNING -> SCANNING_WAIT_MS
        Phase.JOINING -> JOINING_WAIT_MS
        Phase.OFF, Phase.IDLE, Phase.READY -> 0L
    }

    fun shouldKeepWaiting(phase: Phase, waitedMs: Long): Boolean = waitedMs < maxWaitMs(phase)
}
