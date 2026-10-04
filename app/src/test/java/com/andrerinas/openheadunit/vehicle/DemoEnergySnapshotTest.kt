package com.andrerinas.openheadunit.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Pure mapping used by demo mode (Settings → Wh / meters). Kept separate from
 * [Mg4EnergyProvider] so it runs on the JVM without Android / EVHardware.
 */
class DemoEnergySnapshotTest {
    @Test
    fun demoValues_mapToVemFields() {
        val percent = 72.0
        val rangeKm = 280
        val capacityKwh = 61.7
        val capacityWh = (capacityKwh * 1000).roundToInt()
        val currentWh = (capacityKwh * (percent / 100.0) * 1000).roundToInt()

        val bytes = VehicleEnergyModelEncoder.encode(
            VehicleEnergyModelEncoder.Snapshot(
                capacityWh = capacityWh,
                currentWh = currentWh,
                rangeMeters = rangeKm * 1000,
                batteryPercent = percent,
            ),
        )
        assertEquals(61700, capacityWh)
        assertEquals(44424, currentWh)
        assertTrue(bytes.isNotEmpty())
    }
}
