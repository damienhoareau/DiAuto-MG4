package com.andrerinas.openheadunit.vehicle

import android.os.SystemClock
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.aap.AapTransport
import com.andrerinas.openheadunit.aap.protocol.messages.CarSpeedEvent
import com.andrerinas.openheadunit.aap.protocol.messages.GearEvent
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

    private val TYPES = setOf(
        Sensors.SensorType.CAR_SPEED_VALUE,
        Sensors.SensorType.GEAR_VALUE,
        Sensors.SensorType.PARKING_BRAKE_VALUE,
        Sensors.SensorType.ENVIRONMENT_VALUE,
    )

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
        App.instance?.let { Mg4EnergyProvider.start(it) }
        if (tickFuture == null) {
            tickFuture = executor.scheduleWithFixedDelay(::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS)
        }
    }

    @Synchronized
    fun unbind() {
        transportRef.set(null)
        requested.clear()
        tickFuture?.cancel(false)
        tickFuture = null
        synchronized(lastSent) {
            lastSent.clear()
            lastSentAt.clear()
        }
    }

    fun onSensorStart(type: Int) {
        if (type !in TYPES) return
        requested.add(type)
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
                // Metres per second times a million, as the phone expects; the sign is dropped.
                val kmh = abs(EVHardware.getVehicleSpeedKmh() ?: return)
                value = (kmh / 3.6 * 1_000_000).roundToInt()
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
