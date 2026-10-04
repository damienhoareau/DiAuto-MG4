package com.andrerinas.openheadunit.aap.protocol.messages

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.andrerinas.openheadunit.vehicle.VehicleEnergyModelEncoder
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.UnknownFieldSet

/**
 * Android Auto sensor type 23 — VehicleEnergyModel for Google Maps EV battery display.
 * Wire: SensorBatch field 23 length-delimited = serialized VehicleEnergyModel.
 */
class VehicleEnergyModelEvent(snapshot: VehicleEnergyModelEncoder.Snapshot) :
    SensorEvent(Sensors.SensorType.VEHICLE_ENERGY_MODEL_VALUE, makeProto(23, snapshot))

/**
 * Sensor type 25 — same VEM payload as raw bytes (some AA / Maps paths request 25, not 23).
 */
class RawVehicleEnergyModelEvent(snapshot: VehicleEnergyModelEncoder.Snapshot) :
    SensorEvent(Sensors.SensorType.RAW_VEHICLE_ENERGY_MODEL_VALUE, makeProto(25, snapshot))

private fun makeProto(fieldNumber: Int, snapshot: VehicleEnergyModelEncoder.Snapshot): Message {
    val vemBytes = VehicleEnergyModelEncoder.encode(snapshot)
    val unknown = UnknownFieldSet.newBuilder()
        .addField(
            fieldNumber,
            UnknownFieldSet.Field.newBuilder()
                .addLengthDelimited(ByteString.copyFrom(vemBytes))
                .build(),
        )
        .build()
    return Sensors.SensorBatch.newBuilder()
        .setUnknownFields(unknown)
        .build()
}
