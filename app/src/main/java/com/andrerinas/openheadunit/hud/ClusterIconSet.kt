package com.andrerinas.openheadunit.hud

import android.content.ContentResolver
import android.provider.Settings

/**
 * The three sets of turn icons the cluster (com.saicmotor.hmi.clusterprojection) can draw, each with
 * its own numbering of the tbtType sent with guideInfosChange. The cluster picks the set from the
 * system setting `navigation_type` (the navigation app of the car), and TeleNav when it is absent.
 *
 * The mapper works in the vocabulary of the TeleNav set, the one checked on a vehicle; the other two
 * are reached from it, so a car that uses another navigation app is sent the numbers its cluster
 * expects. The tables follow the adapter's ClusterIdNng and ClusterIdSOIMnav, checked against the
 * drawables of the cluster.
 */
internal enum class ClusterIconSet(
    /** The value of `navigation_type` that selects the set. */
    val navigationType: String,
    val label: String,
) {
    TELENAV("com.telenav.app.arp", "TeleNav"),
    NNG("com.nng.igo.primong", "NNG"),
    ISA("com.saicmotor.navigation", "ISA");

    /** The icon number this set uses for what [icon] shows in the TeleNav set. */
    fun idFor(icon: ClusterIdTelenav): Int = when (this) {
        TELENAV -> icon.id
        NNG -> nng(icon)
        ISA -> isa(icon)
    }

    companion object {
        const val SETTING = "navigation_type"

        /** The set a `navigation_type` value selects; TeleNav when it is empty or not one of the three. */
        fun fromSetting(value: String?): ClusterIconSet = values().firstOrNull { it.navigationType == value } ?: TELENAV

        fun current(resolver: ContentResolver): ClusterIconSet =
            fromSetting(runCatching { Settings.Global.getString(resolver, SETTING) }.getOrNull())

        // The NNG set numbers the turns, roundabouts and destinations like TeleNav, up to 31.
        private fun nng(icon: ClusterIdTelenav): Int = when (icon) {
            ClusterIdTelenav.ENTER_RIGHT,
            ClusterIdTelenav.MERGE_RIGHT -> 45 // SLIGHT_RIGHT_ALT
            ClusterIdTelenav.ENTER_LEFT,
            ClusterIdTelenav.MERGE_LEFT -> 46 // SLIGHT_LEFT_ALT
            ClusterIdTelenav.EXIT_LEFT -> 32 // ExitLeft
            ClusterIdTelenav.EXIT_RIGHT -> 33 // ExitRight
            ClusterIdTelenav.PASS_TOLL_BOOTH -> 34
            ClusterIdTelenav.HOOK_TURN_RIGHT -> 35
            else -> icon.id
        }

        private fun isa(icon: ClusterIdTelenav): Int = when (icon) {
            ClusterIdTelenav.CONTINUE -> 0
            ClusterIdTelenav.TURN_SLIGHT_RIGHT -> 1
            ClusterIdTelenav.TURN_RIGHT -> 2
            ClusterIdTelenav.TURN_HARD_RIGHT -> 3
            ClusterIdTelenav.RIGHT_U_TURN -> 4
            ClusterIdTelenav.LEFT_U_TURN -> 5
            ClusterIdTelenav.TURN_HARD_LEFT -> 6
            ClusterIdTelenav.TURN_LEFT -> 7
            ClusterIdTelenav.TURN_SLIGHT_LEFT -> 8
            ClusterIdTelenav.STAY_RIGHT -> 9 // KeepRight
            ClusterIdTelenav.STAY_LEFT -> 11 // KeepLeft

            // Roundabouts: the same eight exits, in a different order.
            ClusterIdTelenav.ROUND_ABOUT_3 -> 36 // 12 o'clock
            ClusterIdTelenav.ROUND_ABOUT_5 -> 37 // 9 o'clock
            ClusterIdTelenav.ROUND_ABOUT_1 -> 38 // 3 o'clock
            ClusterIdTelenav.ROUND_ABOUT_7 -> 39 // 6 o'clock
            ClusterIdTelenav.ROUND_ABOUT_2 -> 40 // 1 o'clock
            ClusterIdTelenav.ROUND_ABOUT_0 -> 41 // 5 o'clock
            ClusterIdTelenav.ROUND_ABOUT_6 -> 42 // 7 o'clock
            ClusterIdTelenav.ROUND_ABOUT_4 -> 43 // 11 o'clock

            ClusterIdTelenav.FERRY -> 35
            ClusterIdTelenav.PASS_TOLL_BOOTH -> 34

            // The arrival has one flag, with no side; the numbered waypoints come as ahead, left, right.
            ClusterIdTelenav.LOCATION_AHEAD,
            ClusterIdTelenav.LOCATION_LEFT,
            ClusterIdTelenav.LOCATION_RIGHT -> 18
            ClusterIdTelenav.LOCATION_AHEAD_1 -> 19
            ClusterIdTelenav.LOCATION_LEFT_1 -> 20
            ClusterIdTelenav.LOCATION_RIGHT_1 -> 21
            ClusterIdTelenav.LOCATION_AHEAD_2 -> 22
            ClusterIdTelenav.LOCATION_LEFT_2 -> 23
            ClusterIdTelenav.LOCATION_RIGHT_2 -> 24
            ClusterIdTelenav.LOCATION_AHEAD_3 -> 25
            ClusterIdTelenav.LOCATION_LEFT_3 -> 26
            ClusterIdTelenav.LOCATION_RIGHT_3 -> 27

            // Joining and leaving a motorway.
            ClusterIdTelenav.ENTER_RIGHT,
            ClusterIdTelenav.MERGE_RIGHT -> 45 // HighwayEnterRight
            ClusterIdTelenav.ENTER_LEFT,
            ClusterIdTelenav.MERGE_LEFT -> 47 // HighwayEnterLeft
            ClusterIdTelenav.EXIT_RIGHT -> 44 // HighwayExitRight
            ClusterIdTelenav.EXIT_LEFT -> 46 // HighwayExitLeft

            // The set has no hook turn: an ordinary right turn is the nearest.
            ClusterIdTelenav.HOOK_TURN_RIGHT -> 2
        }
    }
}
