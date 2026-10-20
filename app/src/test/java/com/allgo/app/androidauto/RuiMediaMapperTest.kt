package com.allgo.app.androidauto

import com.andrerinas.openheadunit.aap.AaStatusBus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RuiMediaMapperTest {

    @Test
    fun theTrackIsGivenInMillisecondsWithItsCoverLength() {
        val art = byteArrayOf(1, 2, 3)
        val m = RuiMediaMapper.metadata(AaStatusBus.Track("America", "Bookends", "Simon & Garfunkel", 215, art))
        assertEquals("America", m.song)
        assertEquals("Bookends", m.album)
        assertEquals("Simon & Garfunkel", m.artist)
        assertEquals(215_000, m.duration)
        assertEquals(3, m.albumArtLength)
        assertEquals(art, m.albumArt)
    }

    @Test
    fun aTrackWithoutCoverStillCarriesAnArrayTheParcelCanMeasure() {
        val m = RuiMediaMapper.metadata(AaStatusBus.Track("a", "", "", 0, ByteArray(0)))
        assertNotNull(m.albumArt)
        assertEquals(0, m.albumArtLength)
        assertEquals("", m.playlist)
    }

    @Test
    fun theStateIsKeptAsThePhoneNumbersItAndThePositionIsInMilliseconds() {
        val p = RuiMediaMapper.playback(AaStatusBus.PlaybackInfo(2, "YT Music", 85, shuffle = true, repeat = false, repeatOne = true))
        assertEquals(2, p.state)
        assertEquals("YT Music", p.mediaSource)
        assertEquals(85_000, p.playbackSeconds)
        assertEquals(1, p.shuffle)
        assertEquals(0, p.repeat)
        assertEquals(1, p.repeatOne)
        assertEquals(1, p.elapsedTimeAvailable)
    }
}
