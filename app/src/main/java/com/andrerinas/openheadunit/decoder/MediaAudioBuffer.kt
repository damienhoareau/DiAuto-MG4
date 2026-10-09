package com.andrerinas.openheadunit.decoder

/**
 * Jitter buffer sizing for the AA MEDIA channel when music rides Wi‑Fi with video.
 * MG4/SWI69 wireless often delivers media in bursts separated by 400–600 ms; music needs a
 * start buffer that outlasts those gaps. Speech / navigation keep the low-latency path.
 */
object MediaAudioBuffer {
    const val DEFAULT_MILLIS = 750
    val presets = listOf(500, DEFAULT_MILLIS, 1000)

    private const val HEADROOM_MILLIS = 200
    private const val MIN_TRACK_BUFFER_BYTES = 16 * 1024
    private const val MIN_START_BUFFER_BYTES = 4 * 1024

    fun sanitize(millis: Int): Int = millis.takeIf { it in presets } ?: DEFAULT_MILLIS

    data class Plan(val trackBufferBytes: Int, val startBytes: Int)

    fun plan(
        isMedia: Boolean,
        sampleRate: Int,
        channels: Int,
        minBufferBytes: Int,
        mediaMillis: Int,
    ): Plan {
        val lowLatency = Plan(
            trackBufferBytes = maxOf(minBufferBytes * 4, MIN_TRACK_BUFFER_BYTES),
            startBytes = maxOf(minBufferBytes, MIN_START_BUFFER_BYTES),
        )
        if (!isMedia || mediaMillis <= 0) return lowLatency
        val bytesPerSecond = sampleRate.toLong() * channels.coerceIn(1, 2) * 2
        val start = (bytesPerSecond * sanitize(mediaMillis) / 1000).toInt()
        val capacity = (bytesPerSecond * (sanitize(mediaMillis) + HEADROOM_MILLIS) / 1000).toInt()
        return Plan(
            trackBufferBytes = maxOf(capacity, lowLatency.trackBufferBytes),
            startBytes = maxOf(start, lowLatency.startBytes),
        )
    }

    fun startBytesFor(plannedStartBytes: Int, actualCapacityBytes: Int, writeChunkBytes: Int): Int {
        if (actualCapacityBytes <= 0) return plannedStartBytes
        return minOf(plannedStartBytes, actualCapacityBytes - writeChunkBytes)
            .coerceAtLeast(writeChunkBytes)
    }
}
