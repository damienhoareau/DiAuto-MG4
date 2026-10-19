package com.andrerinas.openheadunit.hud

import com.andrerinas.openheadunit.aap.protocol.proto.NavigationStatus.NavigationLane
import com.andrerinas.openheadunit.aap.protocol.proto.NavigationStatus.NavigationLane.LaneDirection.Shape
import org.junit.Assert.assertEquals
import org.junit.Test

class SaicLanesTest {

    /** A lane from shapes; a shape followed by a star in [taken] is the one to take. */
    private fun lane(vararg directions: Pair<Shape, Boolean>): NavigationLane {
        val builder = NavigationLane.newBuilder()
        for ((shape, highlighted) in directions) {
            builder.addLaneDirections(
                NavigationLane.LaneDirection.newBuilder().setShape(shape).setIsHighlighted(highlighted)
            )
        }
        return builder.build()
    }

    private fun pattern(vararg directions: Pair<Shape, Boolean>) = SaicLanes.pattern(lane(*directions))

    @Test
    fun aSingleArrowIsGrayedOrHighlighted() {
        assertEquals(1, pattern(Shape.STRAIGHT to false))
        assertEquals(2, pattern(Shape.STRAIGHT to true))
        assertEquals(3, pattern(Shape.NORMAL_LEFT to false))
        assertEquals(4, pattern(Shape.NORMAL_LEFT to true))
        assertEquals(5, pattern(Shape.NORMAL_RIGHT to false))
        assertEquals(6, pattern(Shape.NORMAL_RIGHT to true))
        assertEquals(7, pattern(Shape.U_TURN_LEFT to false))
        assertEquals(8, pattern(Shape.U_TURN_LEFT to true))
        assertEquals(9, pattern(Shape.U_TURN_RIGHT to false))
        assertEquals(10, pattern(Shape.U_TURN_RIGHT to true))
    }

    @Test
    fun slightAndSharpShapesDrawAsTheirSide() {
        assertEquals(4, pattern(Shape.SLIGHT_LEFT to true))
        assertEquals(4, pattern(Shape.SHARP_LEFT to true))
        assertEquals(6, pattern(Shape.SLIGHT_RIGHT to true))
        assertEquals(6, pattern(Shape.SHARP_RIGHT to true))
    }

    @Test
    fun twoArrowsTakeTheCombinedPatternWithTheOneToTake() {
        assertEquals(11, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to false))
        assertEquals(12, pattern(Shape.STRAIGHT to true, Shape.NORMAL_LEFT to false))
        assertEquals(13, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to true))
        assertEquals(14, pattern(Shape.STRAIGHT to false, Shape.NORMAL_RIGHT to false))
        assertEquals(15, pattern(Shape.STRAIGHT to true, Shape.NORMAL_RIGHT to false))
        assertEquals(16, pattern(Shape.STRAIGHT to false, Shape.NORMAL_RIGHT to true))
        assertEquals(17, pattern(Shape.NORMAL_LEFT to false, Shape.U_TURN_LEFT to false))
        assertEquals(18, pattern(Shape.NORMAL_LEFT to true, Shape.U_TURN_LEFT to false))
        assertEquals(19, pattern(Shape.NORMAL_LEFT to false, Shape.U_TURN_LEFT to true))
        assertEquals(20, pattern(Shape.NORMAL_RIGHT to false, Shape.U_TURN_RIGHT to false))
        assertEquals(21, pattern(Shape.NORMAL_RIGHT to true, Shape.U_TURN_RIGHT to false))
        assertEquals(22, pattern(Shape.NORMAL_RIGHT to false, Shape.U_TURN_RIGHT to true))
        assertEquals(31, pattern(Shape.NORMAL_LEFT to false, Shape.NORMAL_RIGHT to false))
        assertEquals(32, pattern(Shape.NORMAL_LEFT to true, Shape.NORMAL_RIGHT to false))
        assertEquals(33, pattern(Shape.NORMAL_LEFT to false, Shape.NORMAL_RIGHT to true))
    }

    @Test
    fun theOrderOfTheDirectionsDoesNotChangeThePattern() {
        assertEquals(13, pattern(Shape.NORMAL_LEFT to true, Shape.STRAIGHT to false))
        assertEquals(33, pattern(Shape.NORMAL_RIGHT to true, Shape.NORMAL_LEFT to false))
    }

    @Test
    fun threeArrowsTakeTheWidePatterns() {
        assertEquals(27, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to false, Shape.NORMAL_RIGHT to false))
        assertEquals(28, pattern(Shape.STRAIGHT to true, Shape.NORMAL_LEFT to false, Shape.NORMAL_RIGHT to false))
        assertEquals(29, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to true, Shape.NORMAL_RIGHT to false))
        assertEquals(30, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to false, Shape.NORMAL_RIGHT to true))
        assertEquals(23, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to false, Shape.U_TURN_LEFT to false))
        assertEquals(24, pattern(Shape.STRAIGHT to true, Shape.NORMAL_LEFT to false, Shape.U_TURN_LEFT to false))
        assertEquals(25, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to true, Shape.U_TURN_LEFT to false))
        assertEquals(26, pattern(Shape.STRAIGHT to false, Shape.NORMAL_LEFT to false, Shape.U_TURN_LEFT to true))
    }

    @Test
    fun aLaneWithSeveralShapesOnTheSameSideCountsOnce() {
        assertEquals(13, pattern(Shape.STRAIGHT to false, Shape.SLIGHT_LEFT to true, Shape.SHARP_LEFT to false))
    }

    @Test
    fun aCombinationTheClusterLacksKeepsTheWidestPatternHoldingTheArrowToTake() {
        // Straight, right and a right U-turn has no pattern of its own.
        assertEquals(16, pattern(Shape.STRAIGHT to false, Shape.NORMAL_RIGHT to true, Shape.U_TURN_RIGHT to false))
        assertEquals(22, pattern(Shape.STRAIGHT to false, Shape.NORMAL_RIGHT to false, Shape.U_TURN_RIGHT to true))
    }

    @Test
    fun withNoPatternHoldingTheArrowsTheArrowToTakeIsShownAlone() {
        // Straight and a right U-turn: no pattern has both.
        assertEquals(10, pattern(Shape.STRAIGHT to false, Shape.U_TURN_RIGHT to true))
        assertEquals(1, pattern(Shape.STRAIGHT to false, Shape.U_TURN_RIGHT to false))
    }

    @Test
    fun aCombinationTheClusterLacksWithNothingToTakeKeepsAWidePattern() {
        // Straight, right and a right U-turn, none to take: the widest pattern made of its arrows.
        assertEquals(14, pattern(Shape.STRAIGHT to false, Shape.NORMAL_RIGHT to false, Shape.U_TURN_RIGHT to false))
    }

    @Test
    fun aLaneWithoutAKnownDirectionIsAGrayedStraightArrow() {
        assertEquals(1, SaicLanes.pattern(NavigationLane.newBuilder().build()))
        assertEquals(1, pattern(Shape.UNKNOWN to false))
    }

    @Test
    fun theLanesAreEncodedInOrderAndSeparatedByCommas() {
        val lanes = listOf(
            lane(Shape.NORMAL_LEFT to false),
            lane(Shape.STRAIGHT to true),
            lane(Shape.STRAIGHT to true, Shape.NORMAL_RIGHT to false),
        )
        assertEquals("3,2,15", SaicLanes.encode(lanes))
        assertEquals("", SaicLanes.encode(emptyList()))
    }

    @Test
    fun everyPatternIsInTheRangeTheClusterDraws() {
        val shapes = Shape.values().toList()
        for (a in shapes) for (b in shapes) for (c in shapes) for (flags in 0..7) {
            val id = pattern(a to (flags and 1 != 0), b to (flags and 2 != 0), c to (flags and 4 != 0))
            assert(id in 1..33) { "$a $b $c $flags -> $id" }
        }
    }
}
