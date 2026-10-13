package com.allgo.mirroring.deviceservice;

import android.os.Parcel;
import android.os.Parcelable;

import com.allgo.rui.RemoteDevice;

/**
 * Binary-compatible copy of the Allgo RemoteUI MirroringDevice (Android Auto part only).
 *
 * The parcel layout is what the car's RemoteUI service reads and writes, so field order and types
 * must not change. The CarPlay reference the original optionally appends (type 1) is not supported.
 */
public class MirroringDevice implements Parcelable {
    public static final int ANDROID_AUTO = 2;
    public static final int CARPLAY = 1;
    public static final Parcelable.Creator<MirroringDevice> CREATOR = new Parcelable.Creator<MirroringDevice>() {
        @Override
        public MirroringDevice createFromParcel(Parcel in) {
            return new MirroringDevice(in);
        }

        @Override
        public MirroringDevice[] newArray(int size) {
            return new MirroringDevice[size];
        }
    };
    public static final int SESSION_CP_READY = 3;
    public static final int SESSION_NOT_STARTED = 0;
    public static final int SESSION_STARTED = 2;
    public static final int SESSION_STARTING = 1;
    public static final int TRANSPORT_TYPE_USB = 1;
    public static final int TRANSPORT_TYPE_WIFI = 2;
    public static final int UNKOWN = 0;

    public String BTMacAddress;
    public RemoteDevice mRemoteDevice;
    public String name;
    public Object reference;
    public String serialId;
    public int sessionStatus;
    public int transport;
    public int type;

    public MirroringDevice() {
        this.sessionStatus = 0;
        this.name = null;
        this.serialId = null;
        this.BTMacAddress = null;
        this.mRemoteDevice = null;
        this.type = 0;
    }

    public MirroringDevice(MirroringDevice device) {
        this();
        this.mRemoteDevice = new RemoteDevice();
        this.mRemoteDevice.name = device.getName();
        if (device.getTransport() == 2) {
            this.mRemoteDevice.UUID = device.getBluetoothMac();
        } else {
            this.mRemoteDevice.UUID = device.getSerialId();
        }
        this.mRemoteDevice.btMacAddress = device.getBluetoothMac();
        this.mRemoteDevice.sessionStatus = device.getSessionStatus();
        this.mRemoteDevice.transport = device.getTransport();
        this.name = device.getName();
        this.transport = device.getTransport();
        this.serialId = device.getSerialId();
        this.BTMacAddress = device.getBluetoothMac();
    }

    public int getTransport() {
        return this.transport;
    }

    public String getName() {
        return this.name;
    }

    public String getBluetoothMac() {
        return this.BTMacAddress;
    }

    public String getSerialId() {
        return this.serialId;
    }

    public int getSessionStatus() {
        return this.sessionStatus;
    }

    public int getType() {
        return this.type;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.sessionStatus);
        dest.writeString(this.name);
        dest.writeString(this.serialId);
        dest.writeString(this.BTMacAddress);
        dest.writeInt(this.type);
        dest.writeInt(this.transport);
        // The original appends a CarPlayDevice here only for type == CARPLAY.
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private MirroringDevice(Parcel in) {
        this();
        this.sessionStatus = in.readInt();
        this.name = in.readString();
        this.serialId = in.readString();
        this.BTMacAddress = in.readString();
        this.type = in.readInt();
        this.transport = in.readInt();
    }
}
