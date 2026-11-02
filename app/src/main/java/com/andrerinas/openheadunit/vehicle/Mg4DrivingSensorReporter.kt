package com.andrerinas.openheadunit.vehicle

import android.os.SystemClock
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AapTransport
import com.andrerinas.openheadunit.aap.protocol.messages.AccelerometerEvent
import com.andrerinas.openheadunit.aap.protocol.messages.CarSpeedEvent
import com.andrerinas.openheadunit.aap.protocol.messages.GyroscopeEvent
import com.andrerinas.openheadunit.aap.protocol.messages.CompassEvent
import com.andrerinas.openheadunit.aap.protocol.messages.DoorEvent
import com.andrerinas.openheadunit.aap.protocol.messages.FuelLevelEvent
import com.andrerinas.openheadunit.aap.protocol.messages.GearEvent
import com.andrerinas.openheadunit.aap.protocol.messages.GpsSatelliteEvent
import com.andrerinas.openheadunit.aap.protocol.messages.GpsSatelliteInfo
import com.andrerinas.openheadunit.aap.protocol.messages.HvacEvent
import com.andrerinas.openheadunit.aap.protocol.messages.OdometerEvent
import com.andrerinas.openheadunit.aap.protocol.messages.RpmEvent
import com.andrerinas.openheadunit.aap.protocol.messages.OutsideTemperatureEvent
import com.andrerinas.openheadunit.aap.protocol.messages.ParkingBrakeEvent
import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.andrerinas.openheadunit.utils.AppLog
import com.evsuite.hardware.EVHardware
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sends the car's speed, gear, parking brake and outside temperature to the phone while it has
 * started the matching sensors. A value the car cannot read is not sent: nothing is invented.
 */
object Mg4DrivingSensorReporter {
    private const val TAG = "DiAuto-MG4"
    private const val TICK_MS = 1_000L
    private const val SPEED_REPEAT_MS = 5_000L
    private const val STATE_REPEAT_MS = 10_000L
    private const val TEMPERATURE_REPEAT_MS = 60_000L

    /** The firmware's number for park; the other gears are not told apart yet. */
    private const val PARK = 1

    /** The battery percentage at which the fuel-level event says the energy is low. */
    private const val LOW_BATTERY_PERCENT = 10

    private val TYPES = setOf(
        Sensors.SensorType.CAR_SPEED_VALUE,
        Sensors.SensorType.GEAR_VALUE,
        Sensors.SensorType.PARKING_BRAKE_VALUE,
        Sensors.SensorType.ENVIRONMENT_VALUE,
        Sensors.SensorType.COMPASS_VALUE,
        Sensors.SensorType.ACCEL_VALUE,
        Sensors.SensorType.GYRO_VALUE,
        // Announced to see whether the phone uses them (see SensorProbe); fed from what the car publishes.
        Sensors.SensorType.ODOMETER_VALUE,
        Sensors.SensorType.FUEL_LEVEL_VALUE,
        Sensors.SensorType.HVAC_VALUE,
        Sensors.SensorType.GPS_VALUE,
        Sensors.SensorType.RPM_VALUE,
        Sensors.SensorType.DOOR_VALUE,
    )

    private const val RPM_REPEAT_MS = 5_000L

    // AOSP door areas: the two front doors are confirmed on this firmware, the others are tried.
    private val DOOR_AREAS = intArrayOf(0x1, 0x4, 0x10, 0x40)
    private const val HOOD_AREA = 0x10000000
    private const val BOOT_AREA = 0x20000000

    /** The door event: the hood, the tailgate and the four doors, where an area that does not answer reads as shut. */
    private data class DoorValue(val hood: Boolean, val boot: Boolean, val doors: List<Boolean>)

    private const val SLOW_REPEAT_MS = 60_000L
    private const val GNSS_MIN_INTERVAL_MS = 1_000L

    /** The fuel level event: the battery percentage, the range in metres when known, and a low-battery flag. */
    private data class FuelValue(val percent: Int, val rangeMetres: Int?, val low: Boolean)

    /** The climate event: set and cabin temperatures in thousandths of a degree. */
    private data class HvacValue(val targetE3: Int?, val currentE3: Int?)

    private var gnssCallback: android.location.GnssStatus.Callback? = null
    @Volatile private var lastGnssMs = 0L

    private const val MOTION_MIN_INTERVAL_MS = 100L
    @Volatile private var lastAccelMs = 0L
    @Volatile private var lastGyroMs = 0L
    @Volatile private var motionLogMs = 0L
    private var motionListener: android.hardware.SensorEventListener? = null

    private const val COMPASS_MIN_INTERVAL_MS = 200L
    private const val PUSH_LOG_EVERY_MS = 30_000L

    @Volatile private var lastCompassMs = 0L
    @Volatile private var lastCompassLogMs = 0L
    @Volatile private var lastSpeedLogMs = 0L
    @Volatile private var compassPushes = 0
    @Volatile private var compassSent = 0

    private val requested = ConcurrentHashMap.newKeySet<Int>()
    private val transportRef = AtomicReference<AapTransport?>(null)
    private var tickFuture: ScheduledFuture<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "diauto-mg4-driving").apply { isDaemon = true }
    }

    /** Last value sent per sensor type, and when. */
    private val lastSent = HashMap<Int, Any>()
    private val lastSentAt = HashMap<Int, Long>()

    @Synchronized
    fun bind(transport: AapTransport) {
        transportRef.set(transport)
        // The car properties are read through EVHardware, which the energy provider initialises.
        App.instance?.let {
            Mg4EnergyProvider.start(it)
            executor.execute { logAndroidSensors(it) }
        }
        if (tickFuture == null) {
            tickFuture = executor.scheduleWithFixedDelay(::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS)
        }
    }

    @Synchronized
    fun unbind() {
        transportRef.set(null)
        requested.clear()
        stopMotion()
        stopGnss()
        tickFuture?.cancel(false)
        tickFuture = null
        synchronized(lastSent) {
            lastSent.clear()
            lastSentAt.clear()
        }
    }

    /**
     * The heading the car's own service pushes through RemoteUI (degrees times a million). Sent on
     * only while the phone has started the compass, at most five times a second.
     */
    fun onCompass(bearingE6: Int, pitchE6: Int, rollE6: Int) {
        val now = SystemClock.elapsedRealtime()
        compassPushes++
        if (now - lastCompassLogMs >= PUSH_LOG_EVERY_MS) {
            lastCompassLogMs = now
            AppLog.i(
                "$TAG RemoteUI compass: bearing=${bearingE6 / 1e6} pitch=${pitchE6 / 1e6} roll=${rollE6 / 1e6} " +
                    "(pushes=$compassPushes, sent=$compassSent, requested=${Sensors.SensorType.COMPASS_VALUE in requested})"
            )
        }
        if (Sensors.SensorType.COMPASS_VALUE !in requested || now - lastCompassMs < COMPASS_MIN_INTERVAL_MS) return
        val transport = transportRef.get() ?: return
        lastCompassMs = now
        if (transport.send(CompassEvent(bearingE6, pitchE6, rollE6))) compassSent++
    }

    /** What RemoteUI pushes as the speed, logged now and then to compare with the car property. */
    fun onRemoteSpeed(speedE3: Int, engaged: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSpeedLogMs < PUSH_LOG_EVERY_MS) return
        lastSpeedLogMs = now
        AppLog.i("$TAG RemoteUI speed: speedE3=$speedE3 cruiseEngaged=$engaged (car property: ${EVHardware.getVehicleSpeedKmh()} km/h)")
    }

    /** Which motion sensors this head unit offers to Android itself, to see what could feed the phone. */
    private fun logAndroidSensors(app: android.content.Context) {
        try {
            val manager = app.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
            val all = manager.getSensorList(android.hardware.Sensor.TYPE_ALL)
            AppLog.i("$TAG android sensors (${all.size}): " + all.joinToString { "${it.name}[type=${it.type}]" })
        } catch (error: Throwable) {
            AppLog.w("$TAG android sensor list failed: ${error.message}")
        }
    }

    /** Listens to the head unit's accelerometer and gyroscope while the phone has started either. */
    @Synchronized
    private fun startMotion() {
        if (motionListener != null) return
        val app = App.instance ?: return
        try {
            val manager = app.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
            val listener = object : android.hardware.SensorEventListener {
                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
                override fun onSensorChanged(event: android.hardware.SensorEvent) = onMotion(event)
            }
            var any = false
            for (kind in intArrayOf(android.hardware.Sensor.TYPE_ACCELEROMETER, android.hardware.Sensor.TYPE_GYROSCOPE)) {
                val sensor = manager.getDefaultSensor(kind) ?: continue
                any = manager.registerListener(listener, sensor, 100_000) || any
            }
            if (any) motionListener = listener
            AppLog.i("$TAG motion sensors listening=$any")
        } catch (error: Throwable) {
            AppLog.w("$TAG motion sensors failed: ${error.message}")
        }
    }

    @Synchronized
    private fun stopMotion() {
        val listener = motionListener ?: return
        motionListener = null
        try {
            val app = App.instance ?: return
            (app.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager)
                .unregisterListener(listener)
        } catch (_: Throwable) {
        }
    }

    private fun onMotion(event: android.hardware.SensorEvent) {
        val now = SystemClock.elapsedRealtime()
        val v = event.values
        if (v.size < 3) return
        val x = (v[0] * 1000).roundToInt()
        val y = (v[1] * 1000).roundToInt()
        val z = (v[2] * 1000).roundToInt()
        val transport = transportRef.get() ?: return
        if (event.sensor.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
            if (Sensors.SensorType.ACCEL_VALUE !in requested || now - lastAccelMs < MOTION_MIN_INTERVAL_MS) return
            lastAccelMs = now
            transport.send(AccelerometerEvent(x, y, z))
        } else {
            if (Sensors.SensorType.GYRO_VALUE !in requested || now - lastGyroMs < MOTION_MIN_INTERVAL_MS) return
            lastGyroMs = now
            transport.send(GyroscopeEvent(x, y, z))
        }
        if (now - motionLogMs >= PUSH_LOG_EVERY_MS) {
            motionLogMs = now
            AppLog.i("$TAG motion sensor tx type=${event.sensor.type} x=$x y=$y z=$z")
        }
    }

    /** Listens to the head unit's GPS receiver while the phone has started the satellite sensor. */
    @Synchronized
    private fun startGnss() {
        if (gnssCallback != null) return
        val app = App.instance ?: return
        try {
            val manager = app.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val callback = object : android.location.GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: android.location.GnssStatus) = onGnss(status)
            }
            val ok = manager.registerGnssStatusCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))
            if (ok) gnssCallback = callback
            AppLog.i("$TAG gnss status listening=$ok")
        } catch (error: Throwable) {
            AppLog.w("$TAG gnss status failed: ${error.message}")
        }
    }

    @Synchronized
    private fun stopGnss() {
        val callback = gnssCallback ?: return
        gnssCallback = null
        try {
            val app = App.instance ?: return
            (app.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager)
                .unregisterGnssStatusCallback(callback)
        } catch (_: Throwable) {
        }
    }

    private fun onGnss(status: android.location.GnssStatus) {
        val now = SystemClock.elapsedRealtime()
        if (Sensors.SensorType.GPS_VALUE !in requested || now - lastGnssMs < GNSS_MIN_INTERVAL_MS) return
        val transport = transportRef.get() ?: return
        lastGnssMs = now
        // Only the GPS constellation: the phone reads the number as a GPS PRN.
        val satellites = (0 until status.satelliteCount)
            .filter { status.getConstellationType(it) == android.location.GnssStatus.CONSTELLATION_GPS }
            .map {
                GpsSatelliteInfo(
                    prn = status.getSvid(it),
                    snrE3 = (status.getCn0DbHz(it) * 1000).roundToInt(),
                    usedInFix = status.usedInFix(it),
                    azimuthE3 = (status.getAzimuthDegrees(it) * 1000).roundToInt(),
                    elevationE3 = (status.getElevationDegrees(it) * 1000).roundToInt(),
                )
            }
        transport.send(GpsSatelliteEvent(satellites))
        if (now - motionLogMs >= PUSH_LOG_EVERY_MS) {
            motionLogMs = now
            AppLog.i("$TAG gnss tx: ${satellites.count { it.usedInFix }} used / ${satellites.size} GPS satellites in view")
        }
    }

    fun onSensorStart(type: Int) {
        if (type !in TYPES) return
        requested.add(type)
        if (type == Sensors.SensorType.GPS_VALUE) {
            executor.execute { startGnss() }
            return
        }
        if (type == Sensors.SensorType.ACCEL_VALUE || type == Sensors.SensorType.GYRO_VALUE) {
            executor.execute { startMotion() }
            return
        }
        AppLog.i("$TAG phone requested driving sensor type=$type")
        executor.execute { sendIfDue(type, force = true) }
    }

    private fun tick() {
        try {
            for (type in requested) sendIfDue(type, force = false)
        } catch (error: Throwable) {
            AppLog.e("$TAG driving sensors failed: ${error.message}")
        }
    }

    private fun sendIfDue(type: Int, force: Boolean) {
        val transport = transportRef.get() ?: return
        val value: Any
        val repeatMs: Long
        when (type) {
            Sensors.SensorType.CAR_SPEED_VALUE -> {
                // Metres per second times a thousand, as the stock app sends it; the sign is dropped.
                val kmh = abs(EVHardware.getVehicleSpeedKmh() ?: return)
                value = (kmh / 3.6 * 1_000).roundToInt()
                repeatMs = SPEED_REPEAT_MS
            }
            Sensors.SensorType.GEAR_VALUE -> {
                // Any gear but park is sent as drive: reverse and neutral are not told apart yet.
                value = (EVHardware.getGearRaw() ?: return) == PARK
                repeatMs = STATE_REPEAT_MS
            }
            Sensors.SensorType.PARKING_BRAKE_VALUE -> {
                // The car property when the firmware has it, else the brake sets itself in park.
                value = EVHardware.isParkingBrakeOn() ?: EVHardware.isVehicleInPark() ?: return
                repeatMs = STATE_REPEAT_MS
            }
            Sensors.SensorType.ENVIRONMENT_VALUE -> {
                value = ((EVHardware.getOutsideTempCelsius() ?: return) * 1000).roundToInt()
                repeatMs = TEMPERATURE_REPEAT_MS
            }
            Sensors.SensorType.ODOMETER_VALUE -> {
                // Tenths of a kilometre.
                value = ((EVHardware.getOdometerKm() ?: return) * 10).roundToInt()
                repeatMs = SLOW_REPEAT_MS
            }
            Sensors.SensorType.FUEL_LEVEL_VALUE -> {
                val percent = (EVHardware.getBatterySocPercent() ?: return).roundToInt().coerceIn(0, 100)
                value = FuelValue(percent, EVHardware.getRemainingRangeKm()?.let { it * 1000 }, percent <= LOW_BATTERY_PERCENT)
                repeatMs = SLOW_REPEAT_MS
            }
            Sensors.SensorType.HVAC_VALUE -> {
                val target = EVHardware.getTemperatureSetCelsius()?.let { (it * 1000).roundToInt() }
                val current = EVHardware.getCabinTemperatureCelsius()?.let { (it * 1000).roundToInt() }
                if (target == null && current == null) return
                value = HvacValue(target, current)
                repeatMs = SLOW_REPEAT_MS
            }
            Sensors.SensorType.RPM_VALUE -> {
                value = ((EVHardware.getEngineRpm() ?: return) * 1000).roundToInt()
                repeatMs = RPM_REPEAT_MS
            }
            Sensors.SensorType.DOOR_VALUE -> {
                // Without the front left door answering there is nothing to say; the other areas may stay silent.
                val front = EVHardware.doorOpenOrNull(DOOR_AREAS[0]) ?: return
                value = DoorValue(
                    hood = EVHardware.doorOpenOrNull(HOOD_AREA) ?: false,
                    boot = EVHardware.doorOpenOrNull(BOOT_AREA) ?: false,
                    doors = listOf(front) + DOOR_AREAS.drop(1).map { EVHardware.doorOpenOrNull(it) ?: false },
                )
                repeatMs = SLOW_REPEAT_MS
            }
            else -> return
        }
        val now = SystemClock.elapsedRealtime()
        val due = synchronized(lastSent) {
            force || lastSent[type] != value || now - (lastSentAt[type] ?: 0L) >= repeatMs
        }
        if (!due) return
        val sent = when (type) {
            Sensors.SensorType.CAR_SPEED_VALUE -> transport.send(CarSpeedEvent(value as Int))
            Sensors.SensorType.GEAR_VALUE -> transport.send(GearEvent(value as Boolean))
            Sensors.SensorType.PARKING_BRAKE_VALUE -> transport.send(ParkingBrakeEvent(value as Boolean))
            Sensors.SensorType.ODOMETER_VALUE -> transport.send(OdometerEvent(value as Int))
            Sensors.SensorType.FUEL_LEVEL_VALUE -> (value as FuelValue).let { transport.send(FuelLevelEvent(it.percent, it.rangeMetres, it.low)) }
            Sensors.SensorType.HVAC_VALUE -> (value as HvacValue).let { transport.send(HvacEvent(it.targetE3, it.currentE3)) }
            Sensors.SensorType.RPM_VALUE -> transport.send(RpmEvent(value as Int))
            Sensors.SensorType.DOOR_VALUE -> (value as DoorValue).let { transport.send(DoorEvent(it.hood, it.boot, it.doors)) }
            else -> transport.send(OutsideTemperatureEvent(value as Int))
        }
        if (!sent) return
        val changed = synchronized(lastSent) {
            val before = lastSent[type]
            lastSent[type] = value
            lastSentAt[type] = now
            before != value
        }
        // The speed changes all the time; the others are worth a line when they change.
        if (changed && type != Sensors.SensorType.CAR_SPEED_VALUE) {
            AppLog.i("$TAG driving sensor tx type=$type value=$value")
        }
    }
}
