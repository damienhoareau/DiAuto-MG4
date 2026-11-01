package com.andrerinas.openheadunit.aap.protocol.messages

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.google.protobuf.Message

/**
 * Car speed, in metres per second times a thousand (always positive). The proto names the field
 * speed_e6, but the stock app sends speedE3 there: a million-fold value reads as thousands of m/s.
 */
class CarSpeedEvent(speedE3: Int) : SensorEvent(
    Sensors.SensorType.CAR_SPEED_VALUE,
    batch { it.addSpeedData(Sensors.SensorBatch.SpeedData.newBuilder().setSpeedE6(speedE3)) },
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

/** Compass heading, pitch and roll, in degrees times a million. */
class CompassEvent(bearingE6: Int, pitchE6: Int, rollE6: Int) : SensorEvent(
    Sensors.SensorType.COMPASS_VALUE,
    batch {
        it.addCompassData(
            Sensors.SensorBatch.CompassData.newBuilder()
                .setBearingE6(bearingE6).setPitchE6(pitchE6).setRollE6(rollE6)
        )
    },
)

/** Acceleration along the three axes, in thousandths of a metre per second squared. */
class AccelerometerEvent(xE3: Int, yE3: Int, zE3: Int) : SensorEvent(
    Sensors.SensorType.ACCEL_VALUE,
    batch {
        it.addAccelData(
            Sensors.SensorBatch.AccelerometerData.newBuilder()
                .setAccelerationXE3(xE3).setAccelerationYE3(yE3).setAccelerationZE3(zE3)
        )
    },
)

/** Rotation speed around the three axes, in thousandths of a radian per second. */
class GyroscopeEvent(xE3: Int, yE3: Int, zE3: Int) : SensorEvent(
    Sensors.SensorType.GYRO_VALUE,
    batch {
        it.addGyroData(
            Sensors.SensorBatch.GyroscopeData.newBuilder()
                .setRotationSpeedXE3(xE3).setRotationSpeedYE3(yE3).setRotationSpeedZE3(zE3)
        )
    },
)

/**
 * Odometer, in tenths of a kilometre. The proto names the fields kms_el, which reads as the `_e1` of its
 * siblings (`_e3`, `_e6`); the unit is not confirmed against the phone.
 */
class OdometerEvent(kmE1: Int) : SensorEvent(
    Sensors.SensorType.ODOMETER_VALUE,
    batch { it.addOdometerData(Sensors.SensorBatch.OdometerData.newBuilder().setKmsEl(kmE1)) },
)

/**
 * Energy level of the car: the battery's percentage as the fuel level, and the range. The unit of the range
 * (metres, as the old car API had it) is not confirmed against the phone.
 */
class FuelLevelEvent(percent: Int, rangeMetres: Int?, low: Boolean) : SensorEvent(
    Sensors.SensorType.FUEL_LEVEL_VALUE,
    batch {
        it.addFuelData(
            Sensors.SensorBatch.FuelData.newBuilder().setFuellevel(percent).setLowfuel(low).also { fuel ->
                if (rangeMetres != null) fuel.setRange(rangeMetres)
            }
        )
    },
)

/** Cabin temperature and the temperature the climate control is set to, in thousandths of a degree Celsius. */
class HvacEvent(targetE3: Int?, currentE3: Int?) : SensorEvent(
    Sensors.SensorType.HVAC_VALUE,
    batch {
        it.addHvacData(
            Sensors.SensorBatch.HvacData.newBuilder().also { hvac ->
                if (targetE3 != null) hvac.setTargetTemperatureE3(targetE3)
                if (currentE3 != null) hvac.setCurrentTemperatureE3(currentE3)
            }
        )
    },
)

/** One GPS satellite as the head unit's receiver sees it. Angles in thousandths of a degree, signal in thousandths of dB-Hz. */
class GpsSatelliteInfo(val prn: Int, val snrE3: Int, val usedInFix: Boolean, val azimuthE3: Int, val elevationE3: Int)

class GpsSatelliteEvent(satellites: List<GpsSatelliteInfo>) : SensorEvent(
    Sensors.SensorType.GPS_VALUE,
    batch {
        it.addGpsSatelliteData(
            Sensors.SensorBatch.GpsSatelliteData.newBuilder()
                .setNumberInUse(satellites.count { s -> s.usedInFix })
                .setNumberInView(satellites.size)
                .also { data ->
                    satellites.forEach { s ->
                        data.addSatellites(
                            Sensors.SensorBatch.GpsSatelliteData.GpsSatellite.newBuilder()
                                .setPrn(s.prn).setSnrE3(s.snrE3).setUsedInFix(s.usedInFix)
                                .setAzimuthE3(s.azimuthE3).setElevationE3(s.elevationE3)
                        )
                    }
                }
        )
    },
)

private fun batch(fill: (Sensors.SensorBatch.Builder) -> Unit): Message =
    Sensors.SensorBatch.newBuilder().also(fill).build()
