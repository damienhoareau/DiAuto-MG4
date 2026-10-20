package com.allgo.remoteui.mediabrowserservice;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class RemoteUIMediaPlaybackMetadata implements Parcelable {
    public static final Parcelable.Creator<RemoteUIMediaPlaybackMetadata> CREATOR = new Parcelable.Creator<RemoteUIMediaPlaybackMetadata>() { // from class: com.allgo.remoteui.mediabrowserservice.RemoteUIMediaPlaybackMetadata.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteUIMediaPlaybackMetadata createFromParcel(Parcel source) {
            return new RemoteUIMediaPlaybackMetadata(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteUIMediaPlaybackMetadata[] newArray(int size) {
            return new RemoteUIMediaPlaybackMetadata[size];
        }
    };
    public String album;
    public byte[] albumArt;
    public int albumArtLength;
    public String artist;
    public int duration;
    public String playlist;
    public int rating;
    public String song;

    public RemoteUIMediaPlaybackMetadata(String song, String album, String artist, byte[] albumArt, String playlist, int duration, int rating) {
        this.song = song;
        this.album = album;
        this.artist = artist;
        this.albumArt = albumArt;
        this.playlist = playlist;
        this.duration = duration;
        this.rating = rating;
    }

    public RemoteUIMediaPlaybackMetadata() {
    }

    public RemoteUIMediaPlaybackMetadata(Parcel source) {
        this.song = source.readString();
        this.album = source.readString();
        this.artist = source.readString();
        this.albumArtLength = source.readInt();
        this.albumArt = new byte[source.readInt()];
        source.readByteArray(this.albumArt);
        this.playlist = source.readString();
        this.duration = source.readInt();
        this.rating = source.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.song);
        dest.writeString(this.album);
        dest.writeString(this.artist);
        dest.writeInt(this.albumArtLength);
        dest.writeInt(this.albumArt.length);
        dest.writeByteArray(this.albumArt);
        dest.writeString(this.playlist);
        dest.writeInt(this.duration);
        dest.writeInt(this.rating);
    }
}
