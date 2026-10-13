package com.allgo.aapsdk;

import android.os.Parcel;
import android.os.Parcelable;
import com.allgo.aapsdk.vec.AapVecInfo;

/* JADX INFO: loaded from: classes4.dex */
public class AapAttributes implements Parcelable {
    public static final int AAP_ACCELEROMETER_FUSION = 4;
    public static final int AAP_CAR_SPEED_FUSION = 64;
    public static final int AAP_COMPASS_FUSION = 8;
    public static final int AAP_GYROSCOPE_FUSION = 2;
    public static final int AAP_MAX_NUM_CONNECTOR_TYPES = 11;
    public static final int AAP_MAX_NUM_FUEL_TYPES = 13;
    public static final int AAP_MAX_NUM_SENSORS = 21;
    public static final int AAP_PRIOR_LOCATIONS = 1;
    public static final int AAP_RAW_GPS_ONLY = 256;
    public static final int AAP_STEERING_ANGLE_FUSION = 32;
    public static final int AAP_WHEEL_SPEED_FUSION = 16;
    public static final Parcelable.Creator<AapAttributes> CREATOR = new Parcelable.Creator<AapAttributes>() { // from class: com.allgo.aapsdk.AapAttributes.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapAttributes createFromParcel(Parcel source) {
            return new AapAttributes(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapAttributes[] newArray(int size) {
            return new AapAttributes[size];
        }
    };
    public String HUSWBuild;
    public String HUSWVersion;
    public boolean bIsValidVecInfo;
    public String bTMacId;
    public int[] bTPairMethod;
    public int[] connectorTypes;
    public String displayName;
    public int driverPosition;
    public int[] fuelTypes;
    public int locCharacterization;
    public String modelYear;
    public int numConnectorTypes;
    public int numFuelTypes;
    public int numSensors;
    public int[] sensors;
    public int sessionConfig;
    public AapVecInfo vecInfo;
    public String vehicleId;
    public String vehicleMake;
    public String vehicleModel;

    public AapAttributes() {
    }

    public AapAttributes(Parcel source) {
        this.vehicleMake = source.readString();
        this.vehicleModel = source.readString();
        this.modelYear = source.readString();
        this.displayName = source.readString();
        this.HUSWBuild = source.readString();
        this.HUSWVersion = source.readString();
        this.vehicleId = source.readString();
        this.driverPosition = source.readInt();
        this.bTMacId = source.readString();
        this.bTPairMethod = new int[source.readInt()];
        source.readIntArray(this.bTPairMethod);
        this.numSensors = source.readInt();
        this.sensors = new int[source.readInt()];
        source.readIntArray(this.sensors);
        this.locCharacterization = source.readInt();
        this.numFuelTypes = source.readInt();
        this.fuelTypes = new int[source.readInt()];
        source.readIntArray(this.fuelTypes);
        this.numConnectorTypes = source.readInt();
        this.connectorTypes = new int[source.readInt()];
        source.readIntArray(this.connectorTypes);
        this.bIsValidVecInfo = source.readInt() == 1;
        this.vecInfo = (AapVecInfo) source.readParcelable(AapVecInfo.class.getClassLoader());
        this.sessionConfig = source.readInt();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.vehicleMake);
        parcel.writeString(this.vehicleModel);
        parcel.writeString(this.modelYear);
        parcel.writeString(this.displayName);
        parcel.writeString(this.HUSWBuild);
        parcel.writeString(this.HUSWVersion);
        parcel.writeString(this.vehicleId);
        parcel.writeInt(this.driverPosition);
        parcel.writeString(this.bTMacId);
        parcel.writeInt(this.bTPairMethod.length);
        parcel.writeIntArray(this.bTPairMethod);
        parcel.writeInt(this.numSensors);
        parcel.writeInt(this.sensors.length);
        parcel.writeIntArray(this.sensors);
        parcel.writeInt(this.locCharacterization);
        parcel.writeInt(this.numFuelTypes);
        parcel.writeInt(this.fuelTypes.length);
        parcel.writeIntArray(this.fuelTypes);
        parcel.writeInt(this.numConnectorTypes);
        parcel.writeInt(this.connectorTypes.length);
        parcel.writeIntArray(this.connectorTypes);
        parcel.writeInt(this.bIsValidVecInfo ? 1 : 0);
        parcel.writeParcelable(this.vecInfo, i);
        parcel.writeInt(this.sessionConfig);
    }
}
