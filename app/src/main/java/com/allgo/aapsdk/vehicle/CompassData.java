package com.allgo.aapsdk.vehicle;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class CompassData extends AapVehicleData implements Parcelable {
    public static final Parcelable.Creator<CompassData> CREATOR = new Parcelable.Creator<CompassData>() { // from class: com.allgo.aapsdk.vehicle.CompassData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CompassData createFromParcel(Parcel source) {
            return new CompassData(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CompassData[] newArray(int size) {
            return new CompassData[size];
        }
    };
    public int bearingE6;
    public boolean has3D;
    public boolean hasPitch;
    public boolean hasRoll;
    public int pitchE6;
    public int rollE6;

    public CompassData() {
    }

    public CompassData(Parcel source) {
        this.has3D = source.readInt() == 1;
        this.bearingE6 = source.readInt();
        this.hasPitch = source.readInt() == 1;
        this.pitchE6 = source.readInt();
        this.hasRoll = source.readInt() == 1;
        this.rollE6 = source.readInt();
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.has3D ? 1 : 0);
        parcel.writeInt(this.bearingE6);
        parcel.writeInt(this.hasPitch ? 1 : 0);
        parcel.writeInt(this.pitchE6);
        parcel.writeInt(this.hasRoll ? 1 : 0);
        parcel.writeInt(this.rollE6);
    }
}
