package com.allgo.mirroring.deviceservice;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes4.dex */
public interface IAndroidAutoSessionCallback extends IInterface {
    void onAAPNavigationStatus(String str, int i, Bitmap bitmap) throws RemoteException;

    void onBatteryStatus(int i, int i2) throws RemoteException;

    void onFRX(String str) throws RemoteException;

    void onLastModeChanged(int i, int i2, int i3) throws RemoteException;

    void onNativeUILaunch() throws RemoteException;

    void onPhoneStateUpdate(int i) throws RemoteException;

    void onPlayBackUpdate(int i) throws RemoteException;

    void onScreenStarted() throws RemoteException;

    void onScreenStopped() throws RemoteException;

    void onSessionBTAuthResultStatus(MirroringDevice mirroringDevice, int i) throws RemoteException;

    void onSessionBTPairingRequest(String str) throws RemoteException;

    void onSessionBTPairingRequestTimeout() throws RemoteException;

    void onSessionClosed(MirroringDevice mirroringDevice) throws RemoteException;

    void onSessionDisconnected(MirroringDevice mirroringDevice, int i) throws RemoteException;

    void onSessionFailed(MirroringDevice mirroringDevice, int i) throws RemoteException;

    void onSessionStarted(MirroringDevice mirroringDevice, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IAndroidAutoSessionCallback {
        private static final String DESCRIPTOR = "com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback";
        static final int TRANSACTION_onAAPNavigationStatus = 16;
        static final int TRANSACTION_onBatteryStatus = 12;
        static final int TRANSACTION_onFRX = 11;
        static final int TRANSACTION_onLastModeChanged = 15;
        static final int TRANSACTION_onNativeUILaunch = 7;
        static final int TRANSACTION_onPhoneStateUpdate = 5;
        static final int TRANSACTION_onPlayBackUpdate = 6;
        static final int TRANSACTION_onScreenStarted = 8;
        static final int TRANSACTION_onScreenStopped = 9;
        static final int TRANSACTION_onSessionBTAuthResultStatus = 13;
        static final int TRANSACTION_onSessionBTPairingRequest = 10;
        static final int TRANSACTION_onSessionBTPairingRequestTimeout = 14;
        static final int TRANSACTION_onSessionClosed = 2;
        static final int TRANSACTION_onSessionDisconnected = 3;
        static final int TRANSACTION_onSessionFailed = 4;
        static final int TRANSACTION_onSessionStarted = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAndroidAutoSessionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAndroidAutoSessionCallback)) {
                return (IAndroidAutoSessionCallback) iin;
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
                    onSessionStarted(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSessionClosed(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSessionDisconnected(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSessionFailed(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPhoneStateUpdate(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPlayBackUpdate(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNativeUILaunch();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    onScreenStarted();
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    onScreenStopped();
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSessionBTPairingRequest(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFRX(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    onBatteryStatus(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSessionBTAuthResultStatus(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSessionBTPairingRequestTimeout();
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    onLastModeChanged(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    onAAPNavigationStatus(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? (Bitmap) Bitmap.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IAndroidAutoSessionCallback {
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

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionStarted(MirroringDevice dev, int launchStatus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dev != null) {
                        _data.writeInt(1);
                        dev.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(launchStatus);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionClosed(MirroringDevice dev) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dev != null) {
                        _data.writeInt(1);
                        dev.writeToParcel(_data, 0);
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

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionDisconnected(MirroringDevice dev, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dev != null) {
                        _data.writeInt(1);
                        dev.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(reason);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionFailed(MirroringDevice dev, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dev != null) {
                        _data.writeInt(1);
                        dev.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(reason);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onPhoneStateUpdate(int CallState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(CallState);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onPlayBackUpdate(int PlayBackState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(PlayBackState);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onNativeUILaunch() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onScreenStarted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onScreenStopped() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionBTPairingRequest(String btMacAddress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(btMacAddress);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onFRX(String state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(state);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onBatteryStatus(int currentBatteryLevel, int timeRemainingOnBattery) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(currentBatteryLevel);
                    _data.writeInt(timeRemainingOnBattery);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionBTAuthResultStatus(MirroringDevice dev, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dev != null) {
                        _data.writeInt(1);
                        dev.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(status);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onSessionBTPairingRequestTimeout() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onLastModeChanged(int lastScreenMode, int lastAudioMode, int changeReason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(lastScreenMode);
                    _data.writeInt(lastAudioMode);
                    _data.writeInt(changeReason);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback
            public void onAAPNavigationStatus(String navigationMessage, int navigationStatus, Bitmap icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(navigationMessage);
                    _data.writeInt(navigationStatus);
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
