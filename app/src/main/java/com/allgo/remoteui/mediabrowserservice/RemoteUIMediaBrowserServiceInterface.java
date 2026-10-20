package com.allgo.remoteui.mediabrowserservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes4.dex */
public interface RemoteUIMediaBrowserServiceInterface extends IInterface {
    void onMetaDataCb(RemoteUIMediaPlaybackMetadata remoteUIMediaPlaybackMetadata) throws RemoteException;

    void onPlayBackDataCb(RemoteUIMediaPlaybackInfo remoteUIMediaPlaybackInfo) throws RemoteException;

    void onSetSessionStatusCb(boolean z) throws RemoteException;

    public static abstract class Stub extends Binder implements RemoteUIMediaBrowserServiceInterface {
        private static final String DESCRIPTOR = "com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserServiceInterface";
        static final int TRANSACTION_onMetaDataCb = 1;
        static final int TRANSACTION_onPlayBackDataCb = 2;
        static final int TRANSACTION_onSetSessionStatusCb = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static RemoteUIMediaBrowserServiceInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof RemoteUIMediaBrowserServiceInterface)) {
                return (RemoteUIMediaBrowserServiceInterface) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    onMetaDataCb(parcel.readInt() != 0 ? RemoteUIMediaPlaybackMetadata.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPlayBackDataCb(parcel.readInt() != 0 ? RemoteUIMediaPlaybackInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSetSessionStatusCb(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements RemoteUIMediaBrowserServiceInterface {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserServiceInterface
            public void onMetaDataCb(RemoteUIMediaPlaybackMetadata songMetaData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (songMetaData != null) {
                        _data.writeInt(1);
                        songMetaData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserServiceInterface
            public void onPlayBackDataCb(RemoteUIMediaPlaybackInfo mediaPlayBackInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (mediaPlayBackInfo != null) {
                        _data.writeInt(1);
                        mediaPlayBackInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.remoteui.mediabrowserservice.RemoteUIMediaBrowserServiceInterface
            public void onSetSessionStatusCb(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
