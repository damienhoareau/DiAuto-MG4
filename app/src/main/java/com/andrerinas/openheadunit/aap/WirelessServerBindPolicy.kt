package com.andrerinas.openheadunit.aap

/**
 * Where the Native AA TCP server should bind once SoftAP / P2P credentials are known.
 *
 * DiPlay (CarPlay on the same SAIC SoftAP) binds its AirPlay control listener to the
 * access-point IPv4, not `0.0.0.0`. Binding the wildcard leaves the phone able to join
 * Wi‑Fi (`WifiConnectStatus=0`) while never reaching the projection socket — measured on
 * MG4 SoftAP when the advertised host was `ap0` but the listener sat on every interface.
 * Prefer the advertised host IP whenever we have one; fall back to the wildcard only
 * before credentials exist (helper / head-unit-server modes).
 */
object WirelessServerBindPolicy {

    /** SoftAP / P2P host to bind, or null to keep the wildcard listener. */
    fun bindHostForAdvertisedIp(advertisedIp: String?): String? =
        advertisedIp?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Whether an already-listening server is on a different address than the one we will
     * advertise to the phone. A wildcard listener with a known SoftAP IP counts as needing
     * a rebind — that is the DiPlay gap.
     */
    fun needsRebind(currentBoundHost: String?, desiredHost: String?): Boolean {
        val desired = bindHostForAdvertisedIp(desiredHost) ?: return false
        return currentBoundHost != desired
    }
}
