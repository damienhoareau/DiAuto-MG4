package com.allgo.aapsdk.vehicle;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class DrivingStatusData extends AapVehicleData implements Parcelable {
    public static final int AAP_DRIVE_STATUS_LIMIT_MESSAGE_LEN = 16;
    public static final int AAP_DRIVE_STATUS_NO_CONFIG = 8;
    public static final int AAP_DRIVE_STATUS_NO_KEYBOARD_INPUT = 2;
    public static final int AAP_DRIVE_STATUS_NO_VIDEO = 1;
    public static final int AAP_DRIVE_STATUS_NO_VOICE_INPUT = 4;
    public static final int AAP_DRIVE_STATUS_UNRESTRICTED = 0;
    public static final Parcelable.Creator<DrivingStatusData> CREATOR = new Parcelable.Creator<DrivingStatusData>() { // from class: com.allgo.aapsdk.vehicle.DrivingStatusData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DrivingStatusData createFromParcel(Parcel source) {
            return new DrivingStatusData(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DrivingStatusData[] newArray(int size) {
            return new DrivingStatusData[size];
        }
    };
    public int drivingStatusType;

    public DrivingStatusData() {
    }

    public DrivingStatusData(Parcel source) {
        this.drivingStatusType = source.readInt();
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.drivingStatusType);
    }
}
