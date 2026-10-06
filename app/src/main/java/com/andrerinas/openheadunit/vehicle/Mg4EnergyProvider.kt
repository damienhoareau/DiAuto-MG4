package com.andrerinas.openheadunit.vehicle

import android.content.Context
import android.os.SystemClock
import com.andrerinas.openheadunit.App
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings
import com.evsuite.hardware.EVHardware
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Read-only MG4 SWI69 bridge for Android Auto VehicleEnergyModel.
 * SoC/range from EVHardware (or demo settings); pack Wh from user net capacity × SOH.
 */
object Mg4EnergyProvider {
    private const val TAG = "DiAuto-MG4"
    private const val POLL_MS = 5_000L
    private const val STALE_MS = 3 * 60_000L

    data class EnergySnapshot(
        val capacityWh: Int,
        val currentWh: Int,
        val rangeMeters: Int,
        val batteryPercent: Double,
        val rangeKm: Int,
        val demo: Boolean = false,
    )

    @Volatile private var app: Context? = null
    @Volatile private var latest: EnergySnapshot? = null
    @Volatile private var latestMillis = 0L
    @Volatile private var started = false
    @Volatile private var initialized = false
    private var pollFuture: ScheduledFuture<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "diauto-mg4-energy").apply { isDaemon = true }
    }

    fun available(context: Context): Boolean {
        val settings = App.provide(context).settings
        if (settings.batteryDemoMode) return true
        return carHardwarePresent()
    }

    /** True when car telemetry may be read — independent of demo mode / firmware gate. */
    fun carHardwarePresent(): Boolean = true

    @Synchronized
    fun start(context: Context) {
        app = context.applicationContext
        if (started) {
            executor.execute(::poll)
            return
        }
        started = true
        val settings = App.provide(context.applicationContext).settings
        // Demo: publish immediately on the caller thread so Service Discovery can see a snapshot.
        if (settings.batteryDemoMode) {
            initialized = true
            publishFromPercent(
                settings = settings,
                percent = settings.batteryDemoPercent.toDouble().coerceIn(1.0, 100.0),
                rangeKm = settings.batteryDemoRangeKm.coerceAtLeast(1),
                demo = true,
            )
        }
        executor.execute {
            if (!settings.batteryDemoMode) {
                try {
                    EVHardware.init(context.applicationContext)
                    initialized = true
                    poll()
                } catch (error: Throwable) {
                    AppLog.e("$TAG EVHardware init failed; EV energy disabled: ${error.message}")
                }
            } else {
                poll()
            }
            if (pollFuture == null) {
                pollFuture = executor.scheduleWithFixedDelay(
                    ::poll, POLL_MS, POLL_MS, TimeUnit.MILLISECONDS,
                )
            }
        }
    }

    fun refresh() {
        if (!started) return
        executor.execute(::poll)
    }

    @Synchronized
    fun stop() {
        pollFuture?.cancel(false)
        pollFuture = null
    }

    fun snapshot(): EnergySnapshot? =
        latest?.takeIf { SystemClock.elapsedRealtime() - latestMillis <= STALE_MS }

    /**
     * One-shot car SoC/range for the Charging info screen, ignoring demo mode.
     * Capacity Wh uses [netCapacityKwh] × [sohPercent] (pending UI values OK).
     */
    fun readCarSnapshot(
        context: Context,
        netCapacityKwh: Float,
        sohPercent: Float,
    ): EnergySnapshot? {
        return runCatching {
            if (!initialized) {
                EVHardware.init(context.applicationContext)
                initialized = true
            }
            val percent = EVHardware.getVendorBatterySocPercent()?.toDouble() ?: return null
            val rangeKm = (EVHardware.getVendorRangeKm()
                ?: EVHardware.getStandardRangeKm()?.roundToInt()) ?: return null
            if (rangeKm <= 0) return null
            val capacityKwh = (netCapacityKwh * sohPercent / 100f).toDouble().coerceIn(1.0, 200.0)
            val fraction = (percent / 100.0).coerceIn(0.01, 1.0)
            EnergySnapshot(
                capacityWh = (capacityKwh * 1000).roundToInt().coerceAtLeast(1),
                currentWh = (capacityKwh * fraction * 1000).roundToInt().coerceAtLeast(1),
                rangeMeters = (rangeKm * 1000).coerceAtLeast(1),
                batteryPercent = percent,
                rangeKm = rangeKm,
                demo = false,
            )
        }.getOrNull()
    }

    private fun poll() {
        try {
            pollSafely()
        } catch (error: Throwable) {
            AppLog.e("$TAG telemetry read failed: ${error.message}")
        }
    }

    private fun pollSafely() {
        val ctx = app ?: return
        val settings = App.provide(ctx).settings
        if (!settings.batteryForGoogleMaps) return

        if (settings.batteryDemoMode) {
            publishFromPercent(
                settings = settings,
                percent = settings.batteryDemoPercent.toDouble().coerceIn(1.0, 100.0),
                rangeKm = settings.batteryDemoRangeKm.coerceAtLeast(1),
                demo = true,
            )
            return
        }

        if (!initialized) return

        val percent = EVHardware.getVendorBatterySocPercent()?.toDouble() ?: return
        val rangeKm = (EVHardware.getVendorRangeKm()
            ?: EVHardware.getStandardRangeKm()?.roundToInt()) ?: return
        if (rangeKm <= 0) return

        publishFromPercent(settings, percent, rangeKm, demo = false)
    }

    /**
     * Capacity Wh always comes from user net × SOH (MG4 HAL capacity is usually empty).
     * Live Wh from the car is ignored so Maps SoC stays consistent with the declared pack.
     */
    private fun publishFromPercent(
        settings: Settings,
        percent: Double,
        rangeKm: Int,
        demo: Boolean,
    ) {
        val capacityKwh = settings.batteryEffectiveNetCapacityKwh.toDouble().coerceIn(1.0, 200.0)
        val fraction = (percent / 100.0).coerceIn(0.01, 1.0)
        publish(
            capacityWh = (capacityKwh * 1000).roundToInt().coerceAtLeast(1),
            currentWh = (capacityKwh * fraction * 1000).roundToInt().coerceAtLeast(1),
            rangeKm = rangeKm,
            batteryPercent = percent,
            demo = demo,
        )
    }

    private fun publish(
        capacityWh: Int,
        currentWh: Int,
        rangeKm: Int,
        batteryPercent: Double,
        demo: Boolean,
    ) {
        latest = EnergySnapshot(
            capacityWh = capacityWh,
            currentWh = currentWh,
            rangeMeters = (rangeKm * 1000).coerceAtLeast(1),
            batteryPercent = batteryPercent,
            rangeKm = rangeKm,
            demo = demo,
        )
        latestMillis = SystemClock.elapsedRealtime()
        val mode = if (demo) "demo" else "car"
        AppLog.i(
            "$TAG battery ${batteryPercent.roundToInt()}% range ${rangeKm}km " +
                "netCap=${capacityWh}Wh ($mode)",
        )
    }
}
