package com.andrerinas.openheadunit.aap.protocol.messages

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.google.protobuf.Message

/** Car speed, in metres per second times a million (always positive). */
class CarSpeedEvent(speedE6: Int) : SensorEvent(
    Sensors.SensorType.CAR_SPEED_VALUE,
    batch { it.addSpeedData(Sensors.SensorBatch.SpeedData.newBuilder().setSpeedE6(speedE6)) },
)

class GearEvent(inPark: Boolean) : SensorEvent(
    Sensors.SensorType.GEAR_VALUE,
    batch {
        it.addGearData(
            Sensors.SensorBatch.GearData.newBuilder().setGear(
                if (inPark) Sensors.SensorBatch.GearData.GEAR.PARK else Sensors.SensorBatch.GearData.GEAR.DRIVE
            )
        )
    },
)

class ParkingBrakeEvent(engaged: Boolean) : SensorEvent(
    Sensors.SensorType.PARKING_BRAKE_VALUE,
    batch { it.addParkingbrakeData(Sensors.SensorBatch.ParkingBrakeData.newBuilder().setIsEngaged(engaged)) },
)

/** Outside temperature, in thousandths of a degree Celsius. */
class OutsideTemperatureEvent(temperatureE3: Int) : SensorEvent(
    Sensors.SensorType.ENVIRONMENT_VALUE,
    batch { it.addEnviormentData(Sensors.SensorBatch.EnvironmentData.newBuilder().setTemperatureE3(temperatureE3)) },
)

private fun batch(fill: (Sensors.SensorBatch.Builder) -> Unit): Message =
    Sensors.SensorBatch.newBuilder().also(fill).build()
