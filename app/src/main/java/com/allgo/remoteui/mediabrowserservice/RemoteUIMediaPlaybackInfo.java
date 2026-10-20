package com.allgo.remoteui.mediabrowserservice;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class RemoteUIMediaPlaybackInfo implements Parcelable {
    public static final Parcelable.Creator<RemoteUIMediaPlaybackInfo> CREATOR = new Parcelable.Creator<RemoteUIMediaPlaybackInfo>() { // from class: com.allgo.remoteui.mediabrowserservice.RemoteUIMediaPlaybackInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteUIMediaPlaybackInfo createFromParcel(Parcel source) {
            return new RemoteUIMediaPlaybackInfo(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteUIMediaPlaybackInfo[] newArray(int size) {
            return new RemoteUIMediaPlaybackInfo[size];
        }
    };
    public int elapsedTimeAvailable;
    public String mediaSource;
    public int playbackSeconds;
    public int repeat;
    public int repeatOne;
    public int shuffle;
    public int state;

    public RemoteUIMediaPlaybackInfo(int state, String mediaSource, int playbackSeconds, int shuffle, int repeat, int repeatOne, int elapsedTimeAvailable) {
        this.state = state;
        this.mediaSource = mediaSource;
        this.playbackSeconds = playbackSeconds;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.repeatOne = repeatOne;
        this.elapsedTimeAvailable = elapsedTimeAvailable;
    }

    public RemoteUIMediaPlaybackInfo() {
    }

    public RemoteUIMediaPlaybackInfo(Parcel source) {
        this.state = source.readInt();
        this.mediaSource = source.readString();
        this.playbackSeconds = source.readInt();
        this.shuffle = source.readInt();
        this.repeat = source.readInt();
        this.repeatOne = source.readInt();
        this.elapsedTimeAvailable = source.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.state);
        dest.writeString(this.mediaSource);
        dest.writeInt(this.playbackSeconds);
        dest.writeInt(this.shuffle);
        dest.writeInt(this.repeat);
        dest.writeInt(this.repeatOne);
        dest.writeInt(this.elapsedTimeAvailable);
    }
}
