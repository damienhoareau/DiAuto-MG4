package com.allgo.aapsdk.vec;

import android.os.Parcel;
import android.os.Parcelable;

/* JADX INFO: loaded from: classes4.dex */
public class AapVecService implements Parcelable {
    public static final Parcelable.Creator<AapVecService> CREATOR = new Parcelable.Creator<AapVecService>() { // from class: com.allgo.aapsdk.vec.AapVecService.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapVecService createFromParcel(Parcel source) {
            return new AapVecService(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapVecService[] newArray(int size) {
            return new AapVecService[size];
        }
    };
    public String[] appsName;
    public int numApps;
    public String serviceName;

    public AapVecService() {
    }

    public AapVecService(Parcel source) {
        this.serviceName = source.readString();
        this.numApps = source.readInt();
        this.appsName = new String[source.readInt()];
        source.readStringArray(this.appsName);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serviceName);
        dest.writeInt(this.numApps);
        dest.writeInt(this.appsName.length);
        dest.writeStringArray(this.appsName);
    }
}
