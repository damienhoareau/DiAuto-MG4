package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors

/**
 * Finds out which sensors the phone asks a head unit for.
 *
 * The phone only asks for the sensors it is told about, so to learn which of the ones this head unit does not
 * feed it would use, the head unit announces them too and the requests are logged. Nothing is sent for them: a
 * request is answered with success, as every request is. Data follows only for the ones in [FED].
 *
 * On unless `setprop debug.diauto.sensorprobe 0`.
 */
object SensorProbe {
    private const val PROPERTY = "debug.diauto.sensorprobe"

    /** Sensors announced only to see whether the phone asks for them. */
    val PROBED: List<Sensors.SensorType> = listOf(
        Sensors.SensorType.RPM,
        Sensors.SensorType.ODOMETER,
        Sensors.SensorType.FUEL_LEVEL,
        Sensors.SensorType.DIAGNOSTICS,
        Sensors.SensorType.HVAC,
        Sensors.SensorType.DEAD_RECONING,
        Sensors.SensorType.PASSENGER,
        Sensors.SensorType.DOOR,
        Sensors.SensorType.LIGHT,
        Sensors.SensorType.TIRE,
        Sensors.SensorType.GPS,
    )

    /** The ones of them this head unit does feed, from what the car publishes. The others get no data. */
    val FED: Set<Sensors.SensorType> = setOf(
        Sensors.SensorType.RPM,
        Sensors.SensorType.DOOR,
        Sensors.SensorType.ODOMETER,
        Sensors.SensorType.FUEL_LEVEL,
        Sensors.SensorType.HVAC,
        Sensors.SensorType.GPS,
    )

    fun enabled(): Boolean = try {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java, String::class.java)
            .invoke(null, PROPERTY, "") != "0"
    } catch (e: Throwable) {
        true
    }

    private val announced = LinkedHashSet<String>()
    private val requested = LinkedHashSet<String>()

    /** A new service discovery: what is announced now is what the phone can ask for. */
    @Synchronized
    fun beginSession() {
        announced.clear()
    }

    @Synchronized
    fun noteAnnounced(type: Sensors.SensorType) {
        announced.add(type.name)
    }

    @Synchronized
    fun noteRequested(type: Sensors.SensorType) {
        requested.add(type.name)
    }

    /** For the diagnostic report. */
    @Synchronized
    fun summary(): String {
        val probedNames = PROBED.map { it.name }.toSet()
        val probedAnnounced = announced.filter { it in probedNames }
        val probedRequested = requested.filter { it in probedNames }
        return "announced=${announced.joinToString(",").ifEmpty { "-" }}; " +
            "requested since start=${requested.joinToString(",").ifEmpty { "-" }}; " +
            "probe: announced=${probedAnnounced.size}, requested=${probedRequested.joinToString(",").ifEmpty { "none" }}"
    }
}
