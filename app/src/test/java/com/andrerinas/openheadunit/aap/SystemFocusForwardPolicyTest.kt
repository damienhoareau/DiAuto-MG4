package com.andrerinas.openheadunit.aap

import android.media.AudioManager
import com.andrerinas.openheadunit.aap.SystemFocusForwardPolicy.Forward
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemFocusForwardPolicyTest {

    private val long = 60_000L

    @Test
    fun `another app taking focus for good is passed on as a loss`() {
        assertEquals(Forward.LOSS, SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_LOSS, false, long))
    }

    @Test
    fun `a short loss is passed on as a transient loss`() {
        assertEquals(
            Forward.LOSS_TRANSIENT,
            SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, false, long)
        )
    }

    @Test
    fun `a request to lower the volume is left to the car mixer`() {
        assertEquals(
            Forward.NONE,
            SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, false, long)
        )
    }

    @Test
    fun `a loss right after our own request is not another app`() {
        assertEquals(Forward.NONE, SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_LOSS, false, 50L))
        assertEquals(Forward.NONE, SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, false, 999L))
    }

    @Test
    fun `a loss just past the window is passed on`() {
        assertEquals(
            Forward.LOSS,
            SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_LOSS, false, SystemFocusForwardPolicy.SELF_INFLICTED_WINDOW_MS)
        )
    }

    @Test
    fun `focus given back is passed on only after a loss was passed on`() {
        assertEquals(Forward.GAIN, SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_GAIN, true, long))
        assertEquals(Forward.NONE, SystemFocusForwardPolicy.forwardFor(AudioManager.AUDIOFOCUS_GAIN, false, long))
    }
}
