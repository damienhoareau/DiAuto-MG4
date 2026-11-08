package com.andrerinas.openheadunit.aap

import android.os.SystemClock

/**
 * Field 4 of the Type 1 `WifiStartRequest`: why the head unit is asking the phone to project.
 *
 * Read from Android Auto (Gearhead) by the open-android-auto project; the phone logs a request
 * "by HU due to user request" apart from one "due to auto launch". A head unit that sends nothing
 * is read as [USER_REQUEST], which is what DiAuto did until this field existed.
 *
 * [AUTO_LAUNCH] is what lets the phone's own "Start Android Auto automatically" setting decide,
 * when that setting is "Default (set by the vehicle)" or "If used on the last drive".
 */
enum class WppStartReason(val wire: Int) {
    USER_REQUEST(1),
    AUTO_LAUNCH(2),
    AUTOMATIC_RESTART(3);
}

/**
 * Which [WppStartReason] to send. Pure, so the choice can be tested without a phone.
 *
 * - [LAUNCH_ALWAYS]: always [WppStartReason.USER_REQUEST], as if the user had asked. The phone
 *   projects whatever its own setting says. This is how DiAuto behaved before, and the default.
 * - [LAUNCH_PHONE_PREFERENCE]: tell the phone the truth and let it decide — a tap on Connect is a
 *   user request, a reconnection after the link was lost mid-session an automatic restart, and
 *   anything else (ignition, Bluetooth coming back) an automatic launch.
 */
object WppStartReasonPolicy {
    const val LAUNCH_ALWAYS = 0
    const val LAUNCH_PHONE_PREFERENCE = 1

    /** How long after a tap on Connect a handshake still counts as asked for by the user. */
    const val USER_REQUEST_WINDOW_MS = 60_000L

    /** How long after an unexpected disconnect a handshake still counts as a restart. */
    const val RESTART_WINDOW_MS = 120_000L

    /**
     * @param msSinceUserRequest time since the user last tapped Connect, or null if not since the
     *   last session started.
     * @param msSinceSessionLost time since a session last ended without the user asking, or null.
     */
    fun choose(launchMode: Int, msSinceUserRequest: Long?, msSinceSessionLost: Long?): WppStartReason {
        if (launchMode != LAUNCH_PHONE_PREFERENCE) return WppStartReason.USER_REQUEST
        if (msSinceUserRequest != null && msSinceUserRequest in 0 until USER_REQUEST_WINDOW_MS) {
            return WppStartReason.USER_REQUEST
        }
        if (msSinceSessionLost != null && msSinceSessionLost in 0 until RESTART_WINDOW_MS) {
            return WppStartReason.AUTOMATIC_RESTART
        }
        return WppStartReason.AUTO_LAUNCH
    }
}

/**
 * What [WppStartReasonPolicy] needs to know, kept for the life of the process: the handshake
 * manager is stopped and rebuilt around every disconnect, so it cannot hold this itself. A car
 * that is switched off kills the process, which is right: the next start is an automatic launch.
 */
object WppStartReasonTracker {
    @Volatile private var userRequestAt = 0L
    @Volatile private var sessionLostAt = 0L

    /** The user tapped Connect for a phone. */
    fun noteUserRequest() { userRequestAt = SystemClock.elapsedRealtime() }

    /** A wireless session ended without the user asking for it. */
    fun noteSessionLost() { sessionLostAt = SystemClock.elapsedRealtime() }

    /** A session came up, or the user ended one: whatever came before is settled. */
    fun reset() {
        userRequestAt = 0L
        sessionLostAt = 0L
    }

    fun current(launchMode: Int): WppStartReason {
        val now = SystemClock.elapsedRealtime()
        return WppStartReasonPolicy.choose(
            launchMode,
            userRequestAt.takeIf { it != 0L }?.let { now - it },
            sessionLostAt.takeIf { it != 0L }?.let { now - it },
        )
    }
}
