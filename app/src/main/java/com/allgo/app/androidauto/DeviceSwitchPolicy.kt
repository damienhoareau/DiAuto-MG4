package com.allgo.app.androidauto

/**
 * When the car's RemoteUI asks for a session with a phone while another one is projecting.
 *
 * The Allgo Android Auto app it was written for ends the session in progress, waits for it to be
 * over, then starts the one asked for; asked for the phone already projecting, it does nothing. This
 * is the part of that decision that can be told from the phones' Bluetooth addresses alone.
 */
internal object DeviceSwitchPolicy {

    /** What the service reports when it does not know the phone's address. */
    const val UNKNOWN_ADDRESS = "00:00:00:00:00:00"

    /** How long the session in progress is given to end, as the original gives it. */
    const val ENDING_WAIT_MS = 8_000L

    /**
     * Whether the session in progress has to end for the one asked for to start.
     *
     * Only a session that is up is replaced, and only by a phone that is known to be another one:
     * a request that names no phone, or an address that is the unknown one, cannot be told from
     * the phone already projecting, so it never ends a session.
     */
    fun shouldSwitch(requestedAddress: String?, activeAddress: String?, sessionUp: Boolean): Boolean {
        if (!sessionUp) return false
        val wanted = normalize(requestedAddress) ?: return false
        val current = normalize(activeAddress) ?: return false
        return wanted != current
    }

    private fun normalize(address: String?): String? =
        address?.trim()?.uppercase()?.takeIf { it.isNotEmpty() && it != UNKNOWN_ADDRESS }
}
