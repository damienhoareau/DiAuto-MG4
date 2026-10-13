package com.andrerinas.openheadunit.aap

/** What a user-initiated disconnect should do to this head unit's own access point. */
enum class HotspotExitAction {
    /** Not a hotspot route — this decision belongs to someone else. */
    NONE,

    /** Take the access point down, so the phone is actually put off the network. */
    DISABLE,

    /** Leave it alone and say what that costs: it is not ours to switch off. */
    WARN_LEFT_UP
}

/**
 * Whether ending a session by hand should also take down the access point the phone is sitting on.
 *
 * Closing the AAP socket and sending a `ByeByeRequest` does not make the phone leave the network:
 * it stays associated and Android Auto spins on its wireless-setup restart until it throttles
 * itself, because from its side the car walked away mid-session. The only lever this app has is
 * making the network go away. The access point is the app's own (it starts and configures it, as
 * the OEM stack does), so it is restarted on a user exit unless this device has proven unable to
 * bring one back up.
 */
object UserExitHotspotPolicy {

    /**
     * @param mode [com.andrerinas.openheadunit.utils.Settings.wifiConnectionMode]
     * @param strategy [com.andrerinas.openheadunit.utils.Settings.helperConnectionStrategy]
     * @param teardownProvenUnsafe whether this device has already failed to bring its access point
     *   back after the app took it down. See the note on the [HotspotExitAction.WARN_LEFT_UP]
     *   branch below.
     */
    fun onUserExit(
        mode: Int,
        strategy: Int,
        teardownProvenUnsafe: Boolean = false
    ): HotspotExitAction = when {
        !usesHeadUnitHotspot(mode, strategy) -> HotspotExitAction.NONE
        // Permission is not the only question; capability is the other one, and unlike permission
        // it can only be learned by trying. Some radios will not host an access point again once it
        // has been taken down — measured on a UNISOC unit where hostapd's channel scan raced the
        // driver re-creating the interface and lost, on the teardown and on every attempt after it,
        // including the auto-enable that was supposed to be the way back. On such a device the
        // choice is between one stranded hotspot and one per session, and the app only finds out
        // which kind it is by stranding the first.
        teardownProvenUnsafe -> HotspotExitAction.WARN_LEFT_UP
        else -> HotspotExitAction.DISABLE
    }

    /** Whether this combination puts the phone on an access point this device is hosting. */
    fun usesHeadUnitHotspot(mode: Int, strategy: Int): Boolean =
        mode == 3 || (mode == 2 && strategy == 4)
}
