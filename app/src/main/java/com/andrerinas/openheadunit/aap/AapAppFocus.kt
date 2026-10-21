package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.aap.protocol.proto.Control
import com.andrerinas.openheadunit.utils.AppLog
import com.evsuite.hardware.CarAppFocus

/**
 * The car's app focus for what the phone projects, as the stock app takes it: navigation while the
 * phone asks for the navigation stream, voice command while a voice session runs. It goes through the
 * car's own service, so the rest of the head unit (its native navigation and voice recognition, and
 * any other app that projects) knows who has what.
 *
 * Without the car's service the requests are granted, as in the stock app.
 */
internal class AapAppFocus(
    private val transport: AapTransport,
    private val audio: AapAudio,
) {
    @Volatile private var voiceHeld = false

    init {
        listenToNativeVoice()
    }

    /** Takes the navigation focus. True when the phone may project navigation. */
    fun requestNavigation(): Boolean {
        listenToNativeVoice()
        if (!CarAppFocus.isAvailable()) {
            AppLog.e("AppFocus: the car's app-focus service is not there, granting navigation")
            return true
        }
        val granted = CarAppFocus.request(CarAppFocus.NAVIGATION, ::onOwnershipLost)
        AppLog.i("AppFocus: navigation ${if (granted) "granted" else "refused"}")
        return granted
    }

    fun releaseNavigation() {
        CarAppFocus.abandon(CarAppFocus.NAVIGATION)
    }

    fun requestVoice() {
        listenToNativeVoice()
        voiceHeld = true
        val granted = CarAppFocus.request(CarAppFocus.VOICE_COMMAND, ::onOwnershipLost)
        AppLog.i("AppFocus: voice command ${if (granted) "granted" else "refused"}")
    }

    fun releaseVoice() {
        voiceHeld = false
        CarAppFocus.abandon(CarAppFocus.VOICE_COMMAND)
    }

    /** At the end of the session, so the car does not keep a focus nobody uses. */
    fun releaseAll() {
        releaseNavigation()
        releaseVoice()
    }

    /** Another app took the focus the phone had: for navigation, the phone is told it is native again. */
    private fun onOwnershipLost(appType: Int) {
        if (appType == CarAppFocus.VOICE_COMMAND) voiceHeld = false
        if (appType != CarAppFocus.NAVIGATION) return
        AppLog.i("AppFocus: another app took the navigation, telling the phone it is native")
        val notification = Control.NavFocusNotification.newBuilder()
            .setFocusType(Control.NavFocusType.NAV_FOCUS_1)
            .build()
        transport.send(AapMessage(Channel.ID_CTR, Control.ControlMsgType.MESSAGE_NAV_FOCUS_NOTIFICATION_VALUE, notification))
    }

    /**
     * The car's own voice recognition starting while the phone's guidance is speaking: the guidance
     * has to stop, so the phone is told the audio focus is lost for now.
     */
    private fun listenToNativeVoice() {
        CarAppFocus.listen(CarAppFocus.VOICE_COMMAND) { type, active ->
            if (type != CarAppFocus.VOICE_COMMAND || !active || voiceHeld) return@listen
            if (audio.isGuidancePlaying()) {
                AppLog.i("AppFocus: the car's voice recognition started during guidance, telling the phone")
                audio.tellPhoneLossTransient()
            }
        }
    }
}
