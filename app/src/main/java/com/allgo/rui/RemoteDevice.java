package com.allgo.rui;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class RemoteDevice implements Parcelable {
    public static final int ANDROID_AUTO = 2;
    public static final int CARLIFE = 3;
    public static final int CARPLAY = 1;
    public static final Parcelable.Creator<RemoteDevice> CREATOR = new Parcelable.Creator<RemoteDevice>() { // from class: com.allgo.rui.RemoteDevice.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteDevice createFromParcel(Parcel in) {
            return new RemoteDevice(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteDevice[] newArray(int size) {
            return new RemoteDevice[size];
        }
    };
    public static final int RUI_USER_DISABLED = -1;
    public static final int RUI_USER_ENABLED = 1;
    public static final int SESSION_AA_READY = 3;
    public static final int SESSION_CP_READY = 3;
    public static final int SESSION_NOT_STARTED = 0;
    public static final int SESSION_STARTED = 2;
    public static final int SESSION_STARTING = 1;
    public static final int SESSION_UNAVAILABLE = 4;
    public static final int TRANSPORT_TYPE_USB = 1;
    public static final int TRANSPORT_TYPE_WIFI = 2;
    public static final int UNKNOWN = 0;
    public String UUID;
    public String btMacAddress;
    public int enabledFlag;
    public boolean isKnownDevice;
    public String name;
    public int sessionStatus;
    public int transport;
    public int type;

    public String getName() {
        return this.name;
    }

    public String getUUID() {
        return this.UUID;
    }

    public int getSessionStatus() {
        return this.sessionStatus;
    }

    public String getBluetoothMac() {
        return this.btMacAddress;
    }

    public int getTransport() {
        return this.transport;
    }

    public int getType() {
        return this.type;
    }

    public void setUserChoice(int userChoice) {
        this.enabledFlag = userChoice;
    }

    public RemoteDevice() {
        this.sessionStatus = 0;
        this.name = null;
        this.UUID = null;
        this.btMacAddress = null;
        this.isKnownDevice = false;
        this.type = 0;
        this.enabledFlag = 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.sessionStatus);
        dest.writeString(this.name);
        dest.writeString(this.UUID);
        dest.writeString(this.btMacAddress);
        dest.writeInt(this.type);
        dest.writeInt(this.transport);
        dest.writeBoolean(this.isKnownDevice);
        dest.writeInt(this.enabledFlag);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    private RemoteDevice(Parcel in) {
        this.sessionStatus = 0;
        this.name = null;
        this.UUID = null;
        this.btMacAddress = null;
        this.isKnownDevice = false;
        this.type = 0;
        this.enabledFlag = 0;
        this.sessionStatus = in.readInt();
        this.name = in.readString();
        this.UUID = in.readString();
        this.btMacAddress = in.readString();
        this.type = in.readInt();
        this.transport = in.readInt();
        this.isKnownDevice = in.readBoolean();
        this.enabledFlag = in.readInt();
    }
}
