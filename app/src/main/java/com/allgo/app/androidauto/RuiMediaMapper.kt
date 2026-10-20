package com.allgo.app.androidauto

import com.allgo.remoteui.mediabrowserservice.RemoteUIMediaPlaybackInfo
import com.allgo.remoteui.mediabrowserservice.RemoteUIMediaPlaybackMetadata
import com.andrerinas.openheadunit.aap.AaStatusBus

/**
 * What the car's media service wants about the phone's playback, in the form the stock Allgo app gives it:
 * times in milliseconds, the state as the phone numbers it (1 stopped, 2 playing, 3 paused), the cover as a
 * byte array that is never null (the parcel reads its length).
 */
internal object RuiMediaMapper {

    fun metadata(track: AaStatusBus.Track): RemoteUIMediaPlaybackMetadata = RemoteUIMediaPlaybackMetadata().apply {
        song = track.song
        album = track.album
        artist = track.artist
        albumArt = track.albumArt
        albumArtLength = track.albumArt.size
        playlist = ""
        duration = track.durationSeconds * 1000
        rating = 0
    }

    fun playback(info: AaStatusBus.PlaybackInfo): RemoteUIMediaPlaybackInfo = RemoteUIMediaPlaybackInfo().apply {
        state = info.state
        mediaSource = info.source
        playbackSeconds = info.positionSeconds * 1000
        shuffle = if (info.shuffle) 1 else 0
        repeat = if (info.repeat) 1 else 0
        repeatOne = if (info.repeatOne) 1 else 0
        elapsedTimeAvailable = 1
    }
}
