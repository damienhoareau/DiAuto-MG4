package com.andrerinas.openheadunit.aap

import android.media.AudioManager

/**
 * What to tell the phone when another app on the head unit takes audio focus from us, or gives it
 * back.
 *
 * The phone is told it always has focus when it asks (see AapControl), which keeps Android Auto
 * routing audio to us. It must still hear when something else takes over: when the car radio is
 * selected the phone otherwise goes on "playing" into a source nobody listens to, and pressing
 * play again changes nothing, because it never asks for focus again.
 *
 * A short loss is passed on as a transient loss, so the phone pauses and resumes on its own; a
 * permanent one as a loss, so it waits for the user. A loss that only asks to lower the volume is
 * left to the car's mixer.
 */
object SystemFocusForwardPolicy {

    enum class Forward { NONE, LOSS, LOSS_TRANSIENT, GAIN }

    /**
     * A loss arriving this soon after one of our own focus requests is the system dropping our
     * other request (the app holds more than one), not another app taking over. Passing it on
     * would pause the phone because of ourselves.
     */
    const val SELF_INFLICTED_WINDOW_MS = 1_000L

    fun forwardFor(change: Int, lostToOtherApp: Boolean, sinceOwnRequestMs: Long): Forward = when (change) {
        AudioManager.AUDIOFOCUS_LOSS ->
            if (sinceOwnRequestMs < SELF_INFLICTED_WINDOW_MS) Forward.NONE else Forward.LOSS
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
            if (sinceOwnRequestMs < SELF_INFLICTED_WINDOW_MS) Forward.NONE else Forward.LOSS_TRANSIENT
        // Only worth saying if the phone was told it had lost it.
        AudioManager.AUDIOFOCUS_GAIN -> if (lostToOtherApp) Forward.GAIN else Forward.NONE
        else -> Forward.NONE
    }
}
