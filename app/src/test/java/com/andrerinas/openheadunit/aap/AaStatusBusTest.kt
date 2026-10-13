package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.proto.Control.Service.PhoneStatus_State as Call
import com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback.MediaPlaybackStatus.State as Playback
import org.junit.Assert.assertEquals
import org.junit.Test

class AaStatusBusTest {

    @Test
    fun playbackStatesMapToRemoteUiCodes() {
        assertEquals(0, AaStatusBus.playbackCode(Playback.STOPPED))
        assertEquals(1, AaStatusBus.playbackCode(Playback.PLAYING))
        assertEquals(2, AaStatusBus.playbackCode(Playback.PAUSED))
    }

    @Test
    fun noCallMeansIdle() {
        assertEquals(0, AaStatusBus.callCode(emptyList()))
        assertEquals(0, AaStatusBus.callCode(listOf(Call.HangingUp)))
    }

    @Test
    fun incomingBeatsCallInProgressBeatsHold() {
        assertEquals(1, AaStatusBus.callCode(listOf(Call.InCall, Call.Incoming)))
        assertEquals(2, AaStatusBus.callCode(listOf(Call.OnHold, Call.InCall)))
        assertEquals(2, AaStatusBus.callCode(listOf(Call.Muted)))
        assertEquals(3, AaStatusBus.callCode(listOf(Call.OnHold)))
    }
}
