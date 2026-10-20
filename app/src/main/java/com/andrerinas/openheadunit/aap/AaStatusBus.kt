package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.proto.Control
import com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * What the phone says about playback and calls, in the vocabulary the head unit's RemoteUI expects
 * (see IAndroidAutoSessionCallback.onPlayBackUpdate / onPhoneStateUpdate). [UNKNOWN] means nothing
 * has been reported since the session started.
 */
object AaStatusBus {
    const val UNKNOWN = -1

    /** 0 = stopped, 1 = playing, 2 = paused. */
    val playback = MutableStateFlow(UNKNOWN)

    /** 0 = no call, 1 = incoming, 2 = in a call, 3 = on hold. */
    val call = MutableStateFlow(UNKNOWN)

    /** What the phone is playing, for the car's "now playing" card. The cover is empty when there is none. */
    class Track(
        val song: String,
        val album: String,
        val artist: String,
        val durationSeconds: Int,
        val albumArt: ByteArray,
    )

    /** [state] is the phone's own numbering: 1 stopped, 2 playing, 3 paused. */
    data class PlaybackInfo(
        val state: Int,
        val source: String,
        val positionSeconds: Int,
        val shuffle: Boolean,
        val repeat: Boolean,
        val repeatOne: Boolean,
    )

    val track = MutableStateFlow<Track?>(null)
    val playbackInfo = MutableStateFlow<PlaybackInfo?>(null)

    /**
     * Guidance on/off for the car. The launcher only acts on the exact messages "Navigation Status:Active"
     * (status 1) and "Navigation Status:In-active" (status 0); it ignores any other text.
     */
    data class Navigation(val message: String, val status: Int)

    /** Critical phone battery warning: level in percent, estimated seconds left. */
    data class Battery(val level: Int, val secondsRemaining: Int)

    val navigation = MutableSharedFlow<Navigation>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val battery = MutableSharedFlow<Battery>(extraBufferCapacity = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun reset() {
        playback.value = UNKNOWN
        call.value = UNKNOWN
        track.value = null
        playbackInfo.value = null
    }

    fun playbackCode(state: MediaPlayback.MediaPlaybackStatus.State): Int = when (state) {
        MediaPlayback.MediaPlaybackStatus.State.STOPPED -> 0
        MediaPlayback.MediaPlaybackStatus.State.PLAYING -> 1
        MediaPlayback.MediaPlaybackStatus.State.PAUSED -> 2
    }

    /** An incoming call wins over a call in progress, which wins over one on hold. */
    fun callCode(states: List<Control.Service.PhoneStatus_State>): Int = when {
        states.any { it == Control.Service.PhoneStatus_State.Incoming } -> 1
        states.any { it == Control.Service.PhoneStatus_State.InCall || it == Control.Service.PhoneStatus_State.Muted || it == Control.Service.PhoneStatus_State.Conferenced } -> 2
        states.any { it == Control.Service.PhoneStatus_State.OnHold } -> 3
        else -> 0
    }
}
