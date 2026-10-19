package com.andrerinas.openheadunit.hud

import com.andrerinas.openheadunit.aap.protocol.proto.NavigationStatus.NavigationLane
import com.andrerinas.openheadunit.aap.protocol.proto.NavigationStatus.NavigationLane.LaneDirection.Shape

/**
 * Lane guidance from Android Auto to the cluster's lane patterns.
 *
 * The cluster (com.saicmotor.hmi.clusterprojection) takes vehicleLaneInfoChanged as a comma separated
 * list with one number per lane, from 1 to 33, and draws the image ic_lane_<number> for each. A number
 * is a whole lane: the arrows it offers and which of them is the one to take. Android Auto describes a
 * lane as a list of directions, each flagged when it is the one to take, so a lane becomes the pattern
 * that offers the same arrows.
 *
 * The patterns, as the cluster has them (a grayed arrow is one not to take):
 *  - 1..10: a single arrow, grayed then highlighted: straight 1/2, left 3/4, right 5/6, left U-turn 7/8,
 *    right U-turn 9/10;
 *  - 11..33: two or three arrows, the first number with none highlighted, then one per arrow.
 *
 * The lanes are drawn in the order the phone lists them, which is taken to be left to right.
 */
internal object SaicLanes {

    private enum class Arrow { STRAIGHT, LEFT, RIGHT, LEFT_U_TURN, RIGHT_U_TURN }

    /** Single arrow: [grayed], and the highlighted one is the next number. */
    private val singles = mapOf(
        Arrow.STRAIGHT to 1,
        Arrow.LEFT to 3,
        Arrow.RIGHT to 5,
        Arrow.LEFT_U_TURN to 7,
        Arrow.RIGHT_U_TURN to 9,
    )

    /** A pattern with several arrows: its number with nothing highlighted, and the number per highlighted arrow. */
    private class Combo(val grayed: Int, val highlighted: Map<Arrow, Int>) {
        val arrows: Set<Arrow> get() = highlighted.keys
    }

    // Three arrows first, so that the widest pattern that fits a lane is found first.
    private val combos = listOf(
        Combo(23, mapOf(Arrow.STRAIGHT to 24, Arrow.LEFT to 25, Arrow.LEFT_U_TURN to 26)),
        Combo(27, mapOf(Arrow.STRAIGHT to 28, Arrow.LEFT to 29, Arrow.RIGHT to 30)),
        Combo(11, mapOf(Arrow.STRAIGHT to 12, Arrow.LEFT to 13)),
        Combo(14, mapOf(Arrow.STRAIGHT to 15, Arrow.RIGHT to 16)),
        Combo(17, mapOf(Arrow.LEFT to 18, Arrow.LEFT_U_TURN to 19)),
        Combo(20, mapOf(Arrow.RIGHT to 21, Arrow.RIGHT_U_TURN to 22)),
        Combo(31, mapOf(Arrow.LEFT to 32, Arrow.RIGHT to 33)),
    )

    private fun arrow(shape: Shape): Arrow? = when (shape) {
        Shape.STRAIGHT -> Arrow.STRAIGHT
        Shape.SLIGHT_LEFT, Shape.NORMAL_LEFT, Shape.SHARP_LEFT -> Arrow.LEFT
        Shape.SLIGHT_RIGHT, Shape.NORMAL_RIGHT, Shape.SHARP_RIGHT -> Arrow.RIGHT
        Shape.U_TURN_LEFT -> Arrow.LEFT_U_TURN
        Shape.U_TURN_RIGHT -> Arrow.RIGHT_U_TURN
        Shape.UNKNOWN -> null
    }

    /** The pattern number for one lane. A lane the cluster has no arrow for is shown as a grayed straight arrow. */
    internal fun pattern(lane: NavigationLane): Int {
        val arrows = LinkedHashSet<Arrow>()
        var taken: Arrow? = null
        for (direction in lane.laneDirectionsList) {
            val arrow = (if (direction.hasShape()) arrow(direction.shape) else null) ?: continue
            arrows += arrow
            if (taken == null && direction.hasIsHighlighted() && direction.isHighlighted) taken = arrow
        }
        if (arrows.isEmpty()) return singles.getValue(Arrow.STRAIGHT)
        if (arrows.size == 1) {
            val only = arrows.first()
            return singles.getValue(only) + if (taken != null) 1 else 0
        }
        // The widest pattern made of arrows the lane has, and holding the one to take if there is one.
        val combo = combos.firstOrNull { combo ->
            arrows.containsAll(combo.arrows) && (taken == null || taken in combo.arrows)
        }
        if (combo != null) return if (taken != null) combo.highlighted.getValue(taken) else combo.grayed
        // No pattern has all the arrows: show the one to take alone, or the first arrow grayed.
        return if (taken != null) singles.getValue(taken) + 1 else singles.getValue(arrows.first())
    }

    /** The lane string for the cluster; empty when the phone sent no lanes. */
    fun encode(lanes: List<NavigationLane>): String = lanes.joinToString(",") { pattern(it).toString() }
}
