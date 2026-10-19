package com.andrerinas.openheadunit.hud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterIconSetTest {

    @Test
    fun theSettingSelectsTheSet() {
        assertEquals(ClusterIconSet.TELENAV, ClusterIconSet.fromSetting("com.telenav.app.arp"))
        assertEquals(ClusterIconSet.NNG, ClusterIconSet.fromSetting("com.nng.igo.primong"))
        assertEquals(ClusterIconSet.ISA, ClusterIconSet.fromSetting("com.saicmotor.navigation"))
    }

    @Test
    fun anAbsentOrUnknownSettingIsTeleNav() {
        assertEquals(ClusterIconSet.TELENAV, ClusterIconSet.fromSetting(null))
        assertEquals(ClusterIconSet.TELENAV, ClusterIconSet.fromSetting(""))
        assertEquals(ClusterIconSet.TELENAV, ClusterIconSet.fromSetting("com.example.nav"))
    }

    @Test
    fun teleNavKeepsItsOwnNumbers() {
        for (icon in ClusterIdTelenav.values()) assertEquals(icon.id, ClusterIconSet.TELENAV.idFor(icon))
    }

    @Test
    fun everyIconHasANumberInTheRangeOfEachSet() {
        // The sets hold 0..39 (TeleNav), 0..52 (NNG) and 0..49 (ISA) drawables.
        for (icon in ClusterIdTelenav.values()) {
            assertTrue(icon.name, ClusterIconSet.NNG.idFor(icon) in 0..52)
            assertTrue(icon.name, ClusterIconSet.ISA.idFor(icon) in 0..49)
        }
    }

    @Test
    fun nngNumbersTheTurnsAndRoundaboutsLikeTeleNav() {
        val same = listOf(
            ClusterIdTelenav.CONTINUE, ClusterIdTelenav.TURN_LEFT, ClusterIdTelenav.TURN_RIGHT,
            ClusterIdTelenav.TURN_SLIGHT_LEFT, ClusterIdTelenav.TURN_SLIGHT_RIGHT,
            ClusterIdTelenav.LEFT_U_TURN, ClusterIdTelenav.RIGHT_U_TURN,
            ClusterIdTelenav.TURN_HARD_RIGHT, ClusterIdTelenav.TURN_HARD_LEFT,
            ClusterIdTelenav.ROUND_ABOUT_3, ClusterIdTelenav.ROUND_ABOUT_5, ClusterIdTelenav.ROUND_ABOUT_1,
            ClusterIdTelenav.ROUND_ABOUT_7, ClusterIdTelenav.ROUND_ABOUT_2, ClusterIdTelenav.ROUND_ABOUT_0,
            ClusterIdTelenav.ROUND_ABOUT_6, ClusterIdTelenav.ROUND_ABOUT_4,
            ClusterIdTelenav.STAY_LEFT, ClusterIdTelenav.STAY_RIGHT, ClusterIdTelenav.FERRY,
            ClusterIdTelenav.LOCATION_AHEAD, ClusterIdTelenav.LOCATION_LEFT, ClusterIdTelenav.LOCATION_RIGHT,
        )
        for (icon in same) assertEquals(icon.name, icon.id, ClusterIconSet.NNG.idFor(icon))
    }

    @Test
    fun nngHasItsOwnNumbersForTheRampsTollAndHookTurn() {
        assertEquals(32, ClusterIconSet.NNG.idFor(ClusterIdTelenav.EXIT_LEFT))
        assertEquals(33, ClusterIconSet.NNG.idFor(ClusterIdTelenav.EXIT_RIGHT))
        assertEquals(34, ClusterIconSet.NNG.idFor(ClusterIdTelenav.PASS_TOLL_BOOTH))
        assertEquals(35, ClusterIconSet.NNG.idFor(ClusterIdTelenav.HOOK_TURN_RIGHT))
    }

    @Test
    fun isaNumbersTheTurnsFromTheRightAroundToTheLeft() {
        assertEquals(0, ClusterIconSet.ISA.idFor(ClusterIdTelenav.CONTINUE))
        assertEquals(1, ClusterIconSet.ISA.idFor(ClusterIdTelenav.TURN_SLIGHT_RIGHT))
        assertEquals(2, ClusterIconSet.ISA.idFor(ClusterIdTelenav.TURN_RIGHT))
        assertEquals(3, ClusterIconSet.ISA.idFor(ClusterIdTelenav.TURN_HARD_RIGHT))
        assertEquals(4, ClusterIconSet.ISA.idFor(ClusterIdTelenav.RIGHT_U_TURN))
        assertEquals(5, ClusterIconSet.ISA.idFor(ClusterIdTelenav.LEFT_U_TURN))
        assertEquals(6, ClusterIconSet.ISA.idFor(ClusterIdTelenav.TURN_HARD_LEFT))
        assertEquals(7, ClusterIconSet.ISA.idFor(ClusterIdTelenav.TURN_LEFT))
        assertEquals(8, ClusterIconSet.ISA.idFor(ClusterIdTelenav.TURN_SLIGHT_LEFT))
    }

    @Test
    fun isaKeepsAndRampsAndRoundabouts() {
        assertEquals(9, ClusterIconSet.ISA.idFor(ClusterIdTelenav.STAY_RIGHT))
        assertEquals(11, ClusterIconSet.ISA.idFor(ClusterIdTelenav.STAY_LEFT))
        assertEquals(44, ClusterIconSet.ISA.idFor(ClusterIdTelenav.EXIT_RIGHT))
        assertEquals(46, ClusterIconSet.ISA.idFor(ClusterIdTelenav.EXIT_LEFT))
        assertEquals(36, ClusterIconSet.ISA.idFor(ClusterIdTelenav.ROUND_ABOUT_3))
        assertEquals(37, ClusterIconSet.ISA.idFor(ClusterIdTelenav.ROUND_ABOUT_5))
        assertEquals(38, ClusterIconSet.ISA.idFor(ClusterIdTelenav.ROUND_ABOUT_1))
        assertEquals(39, ClusterIconSet.ISA.idFor(ClusterIdTelenav.ROUND_ABOUT_7))
        assertEquals(43, ClusterIconSet.ISA.idFor(ClusterIdTelenav.ROUND_ABOUT_4))
    }

    @Test
    fun isaShowsTheArrivalAsOneFlagWhateverTheSide() {
        assertEquals(18, ClusterIconSet.ISA.idFor(ClusterIdTelenav.LOCATION_AHEAD))
        assertEquals(18, ClusterIconSet.ISA.idFor(ClusterIdTelenav.LOCATION_LEFT))
        assertEquals(18, ClusterIconSet.ISA.idFor(ClusterIdTelenav.LOCATION_RIGHT))
    }

    @Test
    fun theRoundaboutsOfEverySetAreDistinct() {
        val roundabouts = listOf(
            ClusterIdTelenav.ROUND_ABOUT_0, ClusterIdTelenav.ROUND_ABOUT_1, ClusterIdTelenav.ROUND_ABOUT_2,
            ClusterIdTelenav.ROUND_ABOUT_3, ClusterIdTelenav.ROUND_ABOUT_4, ClusterIdTelenav.ROUND_ABOUT_5,
            ClusterIdTelenav.ROUND_ABOUT_6, ClusterIdTelenav.ROUND_ABOUT_7,
        )
        for (set in ClusterIconSet.values()) {
            assertEquals(set.name, 8, roundabouts.map { set.idFor(it) }.toSet().size)
        }
    }

    @Test
    fun aSetChangeMakesThePlanSendTheIconAgain() {
        val before = SaicGuidance(ClusterIdTelenav.TURN_LEFT, 300, "Rue", iconSet = ClusterIconSet.TELENAV)
        val after = before.copy(iconSet = ClusterIconSet.ISA)
        val calls = SaicCallPlan.plan(before, after)
        assertEquals(listOf(SaicCall.GuideInfos(ClusterIdTelenav.TURN_LEFT, 300, "Rue", ClusterIconSet.ISA)), calls)
        assertTrue(SaicCallPlan.plan(after, after).isEmpty())
    }
}
