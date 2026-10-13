package com.allgo.aapsdk.vehicle;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class FuelData extends AapVehicleData implements Parcelable {
    public static final Parcelable.Creator<FuelData> CREATOR = new Parcelable.Creator<FuelData>() { // from class: com.allgo.aapsdk.vehicle.FuelData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public FuelData createFromParcel(Parcel source) {
            return new FuelData(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public FuelData[] newArray(int size) {
            return new FuelData[size];
        }
    };
    public int fuelRemainingPercent;
    public boolean hasFuelRemainingPercent;
    public boolean hasLowFuelWarning;
    public boolean hasRangeKm;
    public boolean lowFuelWarning;
    public int rangeKm;

    public FuelData() {
    }

    public FuelData(Parcel source) {
        this.hasFuelRemainingPercent = source.readInt() == 1;
        this.fuelRemainingPercent = source.readInt();
        this.hasRangeKm = source.readInt() == 1;
        this.rangeKm = source.readInt();
        this.hasLowFuelWarning = source.readInt() == 1;
        this.lowFuelWarning = source.readInt() == 1;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // com.allgo.aapsdk.vehicle.AapVehicleData, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.hasFuelRemainingPercent ? 1 : 0);
        parcel.writeInt(this.fuelRemainingPercent);
        parcel.writeInt(this.hasRangeKm ? 1 : 0);
        parcel.writeInt(this.rangeKm);
        parcel.writeInt(this.hasLowFuelWarning ? 1 : 0);
        parcel.writeInt(this.lowFuelWarning ? 1 : 0);
    }
}
