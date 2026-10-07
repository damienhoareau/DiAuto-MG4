package com.andrerinas.openheadunit.aap

/**
 * Where the Native AA TCP server should bind once SoftAP / P2P credentials are known.
 *
 * DiPlay binds its control listener to the SoftAP IPv4, not `0.0.0.0`. Prefer the advertised
 * host IP whenever we have one; fall back to the wildcard only before credentials exist.
 */
object WirelessServerBindPolicy {

    /** SoftAP / P2P host to bind and advertise, or null for the wildcard listener. */
    fun bindHostForAdvertisedIp(advertisedIp: String?): String? =
        advertisedIp?.trim()?.takeIf { it.isNotEmpty() }

    /** Same as [bindHostForAdvertisedIp] — the IP Type 1 should carry. */
    fun advertisedHost(advertisedIp: String?): String? =
        bindHostForAdvertisedIp(advertisedIp)

    /**
     * Whether an already-listening server is on a different address than the one we will
     * advertise to the phone. A wildcard listener with a known SoftAP IP needs a rebind.
     */
    fun needsRebind(currentBoundHost: String?, desiredHost: String?): Boolean {
        val desired = bindHostForAdvertisedIp(desiredHost) ?: return false
        return currentBoundHost != desired
    }
}
