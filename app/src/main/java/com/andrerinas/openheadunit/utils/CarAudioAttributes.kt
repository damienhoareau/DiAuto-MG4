package com.andrerinas.openheadunit.utils

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Process

/**
 * How the MG4's own Android Auto app (Allgo) describes its audio to the car's audio policy.
 *
 * Allgo asks for media focus with `USAGE_MEDIA`, for navigation with
 * `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` and for voice with `USAGE_ASSISTANT`. The car source type
 * (`key_car_source_type = 70`) that goes with the media request is set by SaicCarAudioSource.
 *
 * Only applied on the platform-signed car install.
 */
object CarAudioAttributes {
    /** True on the platform-signed MG4 install (system UID). */
    val isCarBuild: Boolean by lazy { Process.myUid() == Process.SYSTEM_UID }

    /** The usage Allgo gives each Android Auto stream: media, navigation guidance, assistant. */
    fun usageForStream(stream: Int): Int = when (stream) {
        AudioManager.STREAM_NOTIFICATION -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        AudioManager.STREAM_VOICE_CALL -> AudioAttributes.USAGE_ASSISTANT
        else -> AudioAttributes.USAGE_MEDIA
    }
}
