package com.andrerinas.openheadunit.utils

/**
 * When the head unit's own Wi‑Fi client has to be switched off for a wireless Android Auto session,
 * and when it may be switched back on.
 *
 * On the MG4 the phone cannot join the car's hotspot while the head unit is itself connected to a
 * Wi‑Fi network as a client: it reports a join failure (status -11) and no station ever associates,
 * even though the access point is up and its configuration is the same as when it works. With the
 * client off the phone joins within seconds.
 *
 * The client is only ever switched back on by whoever switched it off: a Wi‑Fi connection the user
 * turned off, or turned back on, is theirs.
 */
object CarWifiClientPolicy {

    /** No session started this long after the client was switched off: give the Wi‑Fi back. */
    const val IDLE_RESTORE_MS = 3 * 60_000L

    /**
     * A session just ended: wait before giving the Wi-Fi back, because the phone comes back on its
     * own. Measured on an MG4: restarting Android Auto on the phone and getting a session again took
     * well over a minute, and a Wi-Fi client switched back on in between has to be switched off again
     * (with the hotspot restarted) when the phone returns.
     */
    const val RECONNECT_GRACE_MS = 3 * 60_000L

    /** How long to wait for the radio to report that the client is really off. */
    const val SETTLE_TIMEOUT_MS = 4_000L

    /**
     * Switch the client off only on the platform-signed car install (where an app may do it), and
     * only if it is on.
     */
    fun shouldSuspend(isSystemInstall: Boolean, wifiEnabled: Boolean): Boolean =
        isSystemInstall && wifiEnabled

    /**
     * An access point started while the client was on is kept by the radio on the client's channel,
     * and stays there when the client goes away: the phone then finds it on 2.4 GHz, where wireless
     * Android Auto drops within seconds, or not at all. One started after the client is off comes up
     * on its own channel. So when the client had to be switched off with the access point already up,
     * the access point is restarted.
     */
    fun shouldRestartAccessPoint(clientWasOn: Boolean, accessPointUp: Boolean): Boolean =
        clientWasOn && accessPointUp

    /** Switch it back on only if we switched it off and nobody has turned it on again since. */
    fun shouldRestore(suspendedByUs: Boolean, wifiEnabled: Boolean): Boolean =
        suspendedByUs && !wifiEnabled
}
