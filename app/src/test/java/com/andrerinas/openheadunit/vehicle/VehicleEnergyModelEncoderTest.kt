package com.andrerinas.openheadunit.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleEnergyModelEncoderTest {
    @Test
    fun encode_producesNonEmptyProtobuf() {
        val bytes = VehicleEnergyModelEncoder.encode(
            VehicleEnergyModelEncoder.Snapshot(
                capacityWh = 61700,
                currentWh = 30850,
                rangeMeters = 200_000,
                batteryPercent = 50.0,
            ),
        )
        assertTrue(bytes.isNotEmpty())
        // Nested battery message starts at field 1 (tag 0x0a).
        assertTrue(bytes[0] == 0x0a.toByte())
    }

    @Test(expected = IllegalArgumentException::class)
    fun encode_rejectsZeroCapacity() {
        VehicleEnergyModelEncoder.encode(
            VehicleEnergyModelEncoder.Snapshot(0, 1, 1000),
        )
    }

    @Test
    fun fullPackShortRange_doesNotClaimInsaneWhPerKm() {
        // 61.7 kWh / 70 km would be ~881 Wh/km before clamp — must not pass through.
        val snap = VehicleEnergyModelEncoder.Snapshot(
            capacityWh = 61700,
            currentWh = 61700,
            rangeMeters = 70_000,
            batteryPercent = 100.0,
        )
        val whPerKm = VehicleEnergyModelEncoder.whPerKmForTest(snap)
        assertEquals(220f, whPerKm, 0.1f)
        // ~53 km at 220 Wh/km ≈ 11.7 kWh ≈ 19% of pack → arrival ~81%, not ~14%.
        val usedWh = whPerKm * 53f
        val arrivalPct = 100f * (1f - usedWh / 61700f)
        assertTrue("arrivalPct=$arrivalPct", arrivalPct > 70f)
    }

    @Test
    fun healthyRange_yieldsMg4LikeConsumption() {
        val snap = VehicleEnergyModelEncoder.Snapshot(
            capacityWh = 61700,
            currentWh = 61700,
            rangeMeters = 400_000,
            batteryPercent = 100.0,
        )
        val whPerKm = VehicleEnergyModelEncoder.whPerKmForTest(snap)
        assertEquals(154.25f, whPerKm, 1f)
        val force = VehicleEnergyModelEncoder.constantForceNForTest(snap)
        assertTrue("force=$force", force in 200f..900f)
    }
}
