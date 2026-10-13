package com.allgo.app.androidauto;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class AndroidAutoIdentifiers implements Parcelable {
    public static final int AAP_BYEBYE_REASON_AAP_DEVICE_SWITCH = 2;
    public static final int AAP_BYEBYE_REASON_AAP_SUPPORTED_DEV = 5;
    public static final int AAP_BYEBYE_REASON_AAP_UNSUPPORTED_DEV = 3;
    public static final int AAP_BYEBYE_REASON_NOT_CURRENTLY_SUPPORTED = 4;
    public static final int AAP_BYEBYE_REASON_USER_INITIATED_EXIT = 1;
    public static final int AAP_ERR_FRAMING = 275;
    public static final int AAP_ERR_MD_STOPPED = 271;
    public static final int APP_HOME = 0;
    public static final int APP_MAPS = 2;
    public static final int APP_MUSIC = 1;
    public static final int APP_PHONE = 3;
    public static final Parcelable.Creator<AndroidAutoIdentifiers> CREATOR = new Parcelable.Creator<AndroidAutoIdentifiers>() { // from class: com.allgo.app.androidauto.AndroidAutoIdentifiers.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AndroidAutoIdentifiers createFromParcel(Parcel in) {
            return new AndroidAutoIdentifiers(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AndroidAutoIdentifiers[] newArray(int size) {
            return new AndroidAutoIdentifiers[size];
        }
    };
    public static final int MEDIA_KEY_NEXT = 3;
    public static final int MEDIA_KEY_NEXT_PRESSED = 7;
    public static final int MEDIA_KEY_NEXT_RELEASED = 11;
    public static final int MEDIA_KEY_PAUSE = 2;
    public static final int MEDIA_KEY_PLAY = 1;
    public static final int MEDIA_KEY_PLAYPAUSE_PRESSED = 20;
    public static final int MEDIA_KEY_PLAYPAUSE_RELEASED = 21;
    public static final int MEDIA_KEY_PREVIOUS = 4;
    public static final int MEDIA_KEY_PREVIOUS_PRESSED = 8;
    public static final int MEDIA_KEY_PREVIOUS_RELEASED = 12;
    public static final int MEDIA_PLAY_PAUSE = 19;
    public static final int PHONE_CALL_INCOMING = 2;
    public static final int PHONE_CALL_ON_HOLD = 3;
    public static final int PHONE_CALL_STARTED = 1;
    public static final int PHONE_CALL_STOPPED = 0;
    public static final int PLAYBACK_PAUSED = 2;
    public static final int PLAYBACK_STARTED = 1;
    public static final int PLAYBACK_STOPPED = 0;
    public static final String START_FRX = "START_FRX";
    public static final String STOP_FRX = "STOP_FRX";

    public AndroidAutoIdentifiers() {
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    private AndroidAutoIdentifiers(Parcel in) {
    }
}
