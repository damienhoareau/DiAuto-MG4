package com.andrerinas.openheadunit.vehicle

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
}
