package com.allgo.aapsdk.vec;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.Arrays;

/* JADX INFO: loaded from: classes4.dex */
public class AapVecInfo implements Parcelable {
    public static final int AAP_MAX_NUM_VEC_APPS = 5;
    public static final int AAP_MAX_VEC_SERVICES = 1;
    public int numVecServices;
    public AapVecService[] vecServices;
    private static String TAG = "AapVecInfo";
    public static final Parcelable.Creator<AapVecInfo> CREATOR = new Parcelable.Creator<AapVecInfo>() { // from class: com.allgo.aapsdk.vec.AapVecInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapVecInfo createFromParcel(Parcel source) {
            return new AapVecInfo(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AapVecInfo[] newArray(int size) {
            return new AapVecInfo[size];
        }
    };

    public AapVecInfo() {
    }

    public AapVecInfo(Parcel source) {
        this.numVecServices = source.readInt();
        Parcelable[] parcelableArray = source.readParcelableArray(AapVecService.class.getClassLoader());
        if (parcelableArray != null) {
            this.vecServices = (AapVecService[]) Arrays.copyOf(parcelableArray, parcelableArray.length, AapVecService[].class);
        } else {
            Log.e(TAG, "Failed to allocate memory for parcelableArray");
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.numVecServices);
        Parcelable[] parcelableArr = new Parcelable[this.numVecServices];
        Parcelable[] parcelableArray = (Parcelable[]) Arrays.copyOf(this.vecServices, this.vecServices.length, Parcelable[].class);
        dest.writeParcelableArray(parcelableArray, flags);
    }
}
