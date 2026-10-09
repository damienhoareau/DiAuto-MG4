package com.andrerinas.openheadunit.decoder

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAudioBufferTest {
    @Test
    fun sanitizeFallsBackToDefault() {
        assertEquals(MediaAudioBuffer.DEFAULT_MILLIS, MediaAudioBuffer.sanitize(250))
        assertEquals(500, MediaAudioBuffer.sanitize(500))
        assertEquals(750, MediaAudioBuffer.sanitize(750))
        assertEquals(1000, MediaAudioBuffer.sanitize(1000))
    }

    @Test
    fun mediaPlanMatchesWifiBurstWindow() {
        val plan = MediaAudioBuffer.plan(
            isMedia = true,
            sampleRate = 48_000,
            channels = 2,
            minBufferBytes = 7_680,
            mediaMillis = 500,
        )
        // 48000 * 2 * 2 * 0.5s = 96000 start; capacity adds 200ms headroom → 134400
        assertEquals(96_000, plan.startBytes)
        assertEquals(134_400, plan.trackBufferBytes)
    }

    @Test
    fun nonMediaKeepsLowLatency() {
        val plan = MediaAudioBuffer.plan(
            isMedia = false,
            sampleRate = 16_000,
            channels = 1,
            minBufferBytes = 1_280,
            mediaMillis = 1000,
        )
        assertEquals(maxOf(1_280 * 4, 16 * 1024), plan.trackBufferBytes)
        assertEquals(maxOf(1_280, 4 * 1024), plan.startBytes)
    }

    @Test
    fun startBytesRespectsActualTrackCapacity() {
        assertEquals(98_000, MediaAudioBuffer.startBytesFor(192_000, 100_000, 2_048 - 48))
        assertEquals(57_600, MediaAudioBuffer.startBytesFor(57_600, 134_400, 2_048))
        assertEquals(57_600, MediaAudioBuffer.startBytesFor(57_600, 0, 2_048))
    }
}
