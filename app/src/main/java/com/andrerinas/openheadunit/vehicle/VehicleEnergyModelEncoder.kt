package com.andrerinas.openheadunit.vehicle

import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream

/**
 * Minimal wire encoder for Android Auto VehicleEnergyModel (sensor type 23/25).
 * Field numbers follow OpenAutoLink's reconstructed schema used by Google Maps.
 *
 * Maps SoC = min_usable_capacity.Wh / max_capacity.Wh
 * (min_usable_capacity carries the *current* energy, not a static floor).
 */
object VehicleEnergyModelEncoder {

    data class Snapshot(
        val capacityWh: Int,
        val currentWh: Int,
        val rangeMeters: Int,
        val batteryPercent: Double = 0.0,
        val maxChargePowerW: Int = 150_000,
        val maxDischargePowerW: Int = 150_000,
    )

    fun encode(snapshot: Snapshot): ByteArray {
        require(snapshot.capacityWh > 0)
        require(snapshot.currentWh > 0)
        require(snapshot.rangeMeters > 0)

        val percent = if (snapshot.batteryPercent > 0) {
            snapshot.batteryPercent.toFloat()
        } else {
            (100f * snapshot.currentWh / snapshot.capacityWh).coerceIn(1f, 100f)
        }
        val whPerKm = snapshot.currentWh.toFloat() / snapshot.rangeMeters.toFloat() * 1000f
        val battery = encodeBattery(snapshot, percent)
        val consumption = encodeConsumption(whPerKm)
        val specs = encodeVehicleSpecs()
        val prefs = encodeChargingPrefs()

        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeByteArray(1, battery) // battery
        cos.writeByteArray(2, consumption) // consumption
        cos.writeByteArray(4, specs) // vehicle specs (ELECTRIC)
        cos.writeByteArray(12, prefs) // charging_prefs
        cos.flush()
        return out.toByteArray()
    }

    private fun encodeBattery(snapshot: Snapshot, percent: Float): ByteArray {
        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeInt64(1, 1L) // config_id
        // Maps treats min_usable_capacity as *current* Wh.
        cos.writeByteArray(3, encodeEnergyValue(snapshot.currentWh, percent))
        cos.writeByteArray(4, encodeEnergyValue(snapshot.capacityWh, 100f))
        cos.writeFloat(6, 0.92f) // charge_efficiency
        cos.writeFloat(7, 0.95f) // discharge_efficiency
        cos.writeByteArray(8, encodeEnergyValue((snapshot.capacityWh * 0.05).toInt().coerceAtLeast(1), 5f))
        cos.writeInt32(9, snapshot.maxChargePowerW)
        cos.writeInt32(10, snapshot.maxDischargePowerW)
        cos.writeBool(11, true) // regen_braking_capable
        cos.flush()
        return out.toByteArray()
    }

    private fun encodeConsumption(whPerKm: Float): ByteArray {
        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeByteArray(1, encodeEnergyRate(whPerKm.coerceIn(80f, 250f))) // driving
        cos.writeByteArray(2, encodeEnergyRate(2.0f)) // auxiliary
        cos.writeByteArray(3, encodeEnergyRate(0.36f)) // aerodynamic
        cos.flush()
        return out.toByteArray()
    }

    private fun encodeVehicleSpecs(): ByteArray {
        // VehicleSpecs.fuel_types[0] ≈ FuelTypeEntry { type = ELECTRIC(10) }
        // Exact nested tags vary; varint field 1 = 10 matches SDR fuel-type encoding.
        val fuelEntry = ByteArrayOutputStream().also { raw ->
            val cos = CodedOutputStream.newInstance(raw)
            cos.writeInt32(1, 10) // ELECTRIC
            cos.flush()
        }.toByteArray()
        val connectorEntry = ByteArrayOutputStream().also { raw ->
            val cos = CodedOutputStream.newInstance(raw)
            cos.writeInt32(1, 2) // MENNEKES / Type 2
            cos.flush()
        }.toByteArray()
        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeByteArray(1, fuelEntry)
        cos.writeByteArray(2, connectorEntry)
        cos.flush()
        return out.toByteArray()
    }

    private fun encodeChargingPrefs(): ByteArray {
        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeInt32(3, 1) // mode = standard
        cos.flush()
        return out.toByteArray()
    }

    private fun encodeEnergyValue(wattHours: Int, displayValue: Float): ByteArray {
        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeInt32(1, wattHours)
        cos.writeFloat(2, displayValue)
        cos.flush()
        return out.toByteArray()
    }

    private fun encodeEnergyRate(rate: Float): ByteArray {
        val out = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(out)
        cos.writeFloat(1, rate)
        cos.flush()
        return out.toByteArray()
    }
}
