package com.allgo.aapsdk.vehicle;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class SpeedData extends AapVehicleData implements Parcelable {
    public static final Parcelable.Creator<SpeedData> CREATOR = new Parcelable.Creator<SpeedData>() { // from class: com.allgo.aapsdk.vehicle.SpeedData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpeedData createFromParcel(Parcel source) {
            return new SpeedData(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SpeedData[] newArray(int size) {
            return new SpeedData[size];
        }
    };
    public int carSetSpeed;
    public boolean isCarEngaged;
    public boolean isCarEngagedValid;
    public boolean isCarSetSpeedValid;
    public int speedE3;

    public SpeedData() {
    }

    public SpeedData(Parcel source) {
        this.speedE3 = source.readInt();
        this.isCarEngagedValid = source.readInt() == 1;
        this.isCarEngaged = source.readInt() == 1;
        this.isCarSetSpeedValid = source.readInt() == 1;
        this.carSetSpeed = source.readInt();
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.speedE3);
        parcel.writeInt(this.isCarEngagedValid ? 1 : 0);
        parcel.writeInt(this.isCarEngaged ? 1 : 0);
        parcel.writeInt(this.isCarSetSpeedValid ? 1 : 0);
        parcel.writeInt(this.carSetSpeed);
    }
}
