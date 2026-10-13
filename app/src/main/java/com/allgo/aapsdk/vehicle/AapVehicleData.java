package com.allgo.aapsdk.vehicle;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class AapVehicleData implements Parcelable {
    public static final int AAP_SENSOR_COMPASS = 2;
    public static final int AAP_SENSOR_DOOR_DATA = 14;
    public static final int AAP_SENSOR_DRIVING_STATUS_DATA = 12;
    public static final int AAP_SENSOR_ENVIRONMENT_DATA = 10;
    public static final int AAP_SENSOR_ERROR_DATA = 21;
    public static final int AAP_SENSOR_FUEL = 6;
    public static final int AAP_SENSOR_GEAR = 8;
    public static final int AAP_SENSOR_GPS_SATELLITE_DATA = 19;
    public static final int AAP_SENSOR_HVAC_DATA = 11;
    public static final int AAP_SENSOR_LIGHT_DATA = 15;
    public static final int AAP_SENSOR_LOCATION = 1;
    public static final int AAP_SENSOR_NIGHT_MODE = 9;
    public static final int AAP_SENSOR_ODOMETER = 5;
    public static final int AAP_SENSOR_PARKING_BRAKE = 7;
    public static final int AAP_SENSOR_PASSENGER_DATA = 13;
    public static final int AAP_SENSOR_RPM = 4;
    public static final int AAP_SENSOR_SPEED = 3;
    public static final int AAP_SENSOR_TIRE_PRESSURE_DATA = 16;
    public static final int AAP_SENSOR_TOLL_CARD_DATA = 20;
    public static final Parcelable.Creator<AapVehicleData> CREATOR = new Parcelable.Creator<AapVehicleData>() { // from class: com.allgo.aapsdk.vehicle.AapVehicleData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapVehicleData createFromParcel(Parcel source) {
            return new AapVehicleData(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapVehicleData[] newArray(int size) {
            return new AapVehicleData[size];
        }
    };

    public AapVehicleData() {
    }

    public AapVehicleData(Parcel source) {
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
    }
}
