package com.andrerinas.openheadunit.utils

import android.media.AudioAttributes
import android.media.AudioManager
import com.evsuite.hardware.saic.SaicCarAudioSource
import org.junit.Assert.assertEquals
import org.junit.Test

class CarAudioAttributesTest {

    @Test
    fun `media stream keeps the media usage`() {
        assertEquals(AudioAttributes.USAGE_MEDIA, CarAudioAttributes.usageForStream(AudioManager.STREAM_MUSIC))
    }

    @Test
    fun `notification stream is navigation guidance`() {
        assertEquals(
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            CarAudioAttributes.usageForStream(AudioManager.STREAM_NOTIFICATION)
        )
    }

    @Test
    fun `voice call stream is the assistant`() {
        assertEquals(AudioAttributes.USAGE_ASSISTANT, CarAudioAttributes.usageForStream(AudioManager.STREAM_VOICE_CALL))
    }

    @Test
    fun `any other stream falls back to media`() {
        assertEquals(AudioAttributes.USAGE_MEDIA, CarAudioAttributes.usageForStream(AudioManager.STREAM_RING))
    }

    @Test
    fun `the Android Auto source type matches the stock Allgo app`() {
        assertEquals("key_car_source_type", SaicCarAudioSource.KEY_CAR_SOURCE_TYPE)
        assertEquals(70, SaicCarAudioSource.SRC_ANDROID_AUTO)
    }
}
