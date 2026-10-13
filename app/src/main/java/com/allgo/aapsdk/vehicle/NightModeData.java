package com.allgo.aapsdk.vehicle;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class NightModeData extends AapVehicleData implements Parcelable {
    public static final Parcelable.Creator<NightModeData> CREATOR = new Parcelable.Creator<NightModeData>() { // from class: com.allgo.aapsdk.vehicle.NightModeData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NightModeData createFromParcel(Parcel source) {
            return new NightModeData(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NightModeData[] newArray(int size) {
            return new NightModeData[size];
        }
    };
    public boolean nightMode;

    public NightModeData() {
    }

    public NightModeData(Parcel source) {
        this.nightMode = source.readInt() == 1;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.nightMode ? 1 : 0);
    }
}
