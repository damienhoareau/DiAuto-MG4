package com.allgo.app.androidauto;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.allgo.aapsdk.AapAttributes;
import com.allgo.aapsdk.vehicle.CompassData;
import com.allgo.aapsdk.vehicle.DrivingStatusData;
import com.allgo.aapsdk.vehicle.FuelData;
import com.allgo.aapsdk.vehicle.NightModeData;
import com.allgo.aapsdk.vehicle.SpeedData;
import com.allgo.mirroring.deviceservice.IAndroidAutoDeviceCallback;
import com.allgo.mirroring.deviceservice.IAndroidAutoSessionCallback;
import com.allgo.mirroring.deviceservice.MirroringDevice;

/* JADX INFO: loaded from: classes4.dex */
public interface IAndroidAutoMainService extends IInterface {
    void disableAAP(boolean z) throws RemoteException;

    void disableTouchInput(boolean z) throws RemoteException;

    void disableVideoFocus(boolean z) throws RemoteException;

    MirroringDevice getActiveDevice() throws RemoteException;

    String getPluginVersion() throws RemoteException;

    boolean getVRStatus() throws RemoteException;

    boolean isSessionActive() throws RemoteException;

    int launchApp(int i) throws RemoteException;

    int launchGoogleAssistant(boolean z) throws RemoteException;

    int pauseSession(MirroringDevice mirroringDevice) throws RemoteException;

    void register(IAndroidAutoDeviceCallback iAndroidAutoDeviceCallback, IAndroidAutoSessionCallback iAndroidAutoSessionCallback) throws RemoteException;

    void registerCallback(IBinder iBinder) throws RemoteException;

    int resumeSession(MirroringDevice mirroringDevice) throws RemoteException;

    int sendCompassData(int i, CompassData compassData) throws RemoteException;

    int sendDrivingStatus(int i, DrivingStatusData drivingStatusData) throws RemoteException;

    int sendFuelData(int i, FuelData fuelData) throws RemoteException;

    int sendMediaPlayControl(int i) throws RemoteException;

    int sendNightModeData(int i, NightModeData nightModeData) throws RemoteException;

    int sendSpeedData(int i, SpeedData speedData) throws RemoteException;

    int setAttributes(AapAttributes aapAttributes) throws RemoteException;

    int startSession(MirroringDevice mirroringDevice, int i, int i2) throws RemoteException;

    int stopSession(MirroringDevice mirroringDevice) throws RemoteException;

    void surfaceChanged() throws RemoteException;

    void surfaceDestroyed() throws RemoteException;

    void unregister(IAndroidAutoDeviceCallback iAndroidAutoDeviceCallback, IAndroidAutoSessionCallback iAndroidAutoSessionCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IAndroidAutoMainService {
        private static final String DESCRIPTOR = "com.allgo.app.androidauto.IAndroidAutoMainService";
        static final int TRANSACTION_disableAAP = 20;
        static final int TRANSACTION_disableTouchInput = 9;
        static final int TRANSACTION_disableVideoFocus = 19;
        static final int TRANSACTION_getActiveDevice = 6;
        static final int TRANSACTION_getPluginVersion = 12;
        static final int TRANSACTION_getVRStatus = 24;
        static final int TRANSACTION_isSessionActive = 7;
        static final int TRANSACTION_launchApp = 11;
        static final int TRANSACTION_launchGoogleAssistant = 10;
        static final int TRANSACTION_pauseSession = 4;
        static final int TRANSACTION_register = 1;
        static final int TRANSACTION_registerCallback = 21;
        static final int TRANSACTION_resumeSession = 3;
        static final int TRANSACTION_sendCompassData = 16;
        static final int TRANSACTION_sendDrivingStatus = 15;
        static final int TRANSACTION_sendFuelData = 13;
        static final int TRANSACTION_sendMediaPlayControl = 25;
        static final int TRANSACTION_sendNightModeData = 14;
        static final int TRANSACTION_sendSpeedData = 17;
        static final int TRANSACTION_setAttributes = 18;
        static final int TRANSACTION_startSession = 2;
        static final int TRANSACTION_stopSession = 5;
        static final int TRANSACTION_surfaceChanged = 23;
        static final int TRANSACTION_surfaceDestroyed = 22;
        static final int TRANSACTION_unregister = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAndroidAutoMainService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAndroidAutoMainService)) {
                return (IAndroidAutoMainService) iin;
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
                    register(IAndroidAutoDeviceCallback.Stub.asInterface(parcel.readStrongBinder()), IAndroidAutoSessionCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStartSession = startSession(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iStartSession);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iResumeSession = resumeSession(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iResumeSession);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iPauseSession = pauseSession(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iPauseSession);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStopSession = stopSession(parcel.readInt() != 0 ? MirroringDevice.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iStopSession);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    MirroringDevice activeDevice = getActiveDevice();
                    parcel2.writeNoException();
                    if (activeDevice != null) {
                        parcel2.writeInt(1);
                        activeDevice.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSessionActive = isSessionActive();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSessionActive ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregister(IAndroidAutoDeviceCallback.Stub.asInterface(parcel.readStrongBinder()), IAndroidAutoSessionCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableTouchInput(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iLaunchGoogleAssistant = launchGoogleAssistant(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(iLaunchGoogleAssistant);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iLaunchApp = launchApp(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iLaunchApp);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    String pluginVersion = getPluginVersion();
                    parcel2.writeNoException();
                    parcel2.writeString(pluginVersion);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendFuelData = sendFuelData(parcel.readInt(), parcel.readInt() != 0 ? FuelData.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendFuelData);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendNightModeData = sendNightModeData(parcel.readInt(), parcel.readInt() != 0 ? NightModeData.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendNightModeData);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendDrivingStatus = sendDrivingStatus(parcel.readInt(), parcel.readInt() != 0 ? DrivingStatusData.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendDrivingStatus);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendCompassData = sendCompassData(parcel.readInt(), parcel.readInt() != 0 ? CompassData.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendCompassData);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendSpeedData = sendSpeedData(parcel.readInt(), parcel.readInt() != 0 ? SpeedData.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendSpeedData);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    int attributes = setAttributes(parcel.readInt() != 0 ? AapAttributes.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(attributes);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableVideoFocus(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableAAP(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerCallback(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    surfaceDestroyed();
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    surfaceChanged();
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean vRStatus = getVRStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(vRStatus ? 1 : 0);
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendMediaPlayControl = sendMediaPlayControl(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendMediaPlayControl);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IAndroidAutoMainService {
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

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void register(IAndroidAutoDeviceCallback deviceCallback, IAndroidAutoSessionCallback sessionCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(deviceCallback != null ? deviceCallback.asBinder() : null);
                    _data.writeStrongBinder(sessionCallback != null ? sessionCallback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int startSession(MirroringDevice mirroringDevice, int lastScreenMode, int lastAudioMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (mirroringDevice != null) {
                        _data.writeInt(1);
                        mirroringDevice.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(lastScreenMode);
                    _data.writeInt(lastAudioMode);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int resumeSession(MirroringDevice mirroringDevice) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (mirroringDevice != null) {
                        _data.writeInt(1);
                        mirroringDevice.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int pauseSession(MirroringDevice mirroringDevice) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (mirroringDevice != null) {
                        _data.writeInt(1);
                        mirroringDevice.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int stopSession(MirroringDevice mirroringDevice) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (mirroringDevice != null) {
                        _data.writeInt(1);
                        mirroringDevice.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public MirroringDevice getActiveDevice() throws RemoteException {
                MirroringDevice _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = MirroringDevice.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public boolean isSessionActive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void unregister(IAndroidAutoDeviceCallback deviceCb, IAndroidAutoSessionCallback sessionCb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(deviceCb != null ? deviceCb.asBinder() : null);
                    _data.writeStrongBinder(sessionCb != null ? sessionCb.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void disableTouchInput(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int launchGoogleAssistant(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int launchApp(int app) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(app);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public String getPluginVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int sendFuelData(int vehicleSensorType, FuelData fuelData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vehicleSensorType);
                    if (fuelData != null) {
                        _data.writeInt(1);
                        fuelData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int sendNightModeData(int vehicleSensorType, NightModeData nightModeData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vehicleSensorType);
                    if (nightModeData != null) {
                        _data.writeInt(1);
                        nightModeData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int sendDrivingStatus(int vehicleSensorType, DrivingStatusData drivingStatusData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vehicleSensorType);
                    if (drivingStatusData != null) {
                        _data.writeInt(1);
                        drivingStatusData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int sendCompassData(int vehicleSensorType, CompassData compassData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vehicleSensorType);
                    if (compassData != null) {
                        _data.writeInt(1);
                        compassData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int sendSpeedData(int vehicleSensorType, SpeedData speedData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vehicleSensorType);
                    if (speedData != null) {
                        _data.writeInt(1);
                        speedData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int setAttributes(AapAttributes aapAtributes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (aapAtributes != null) {
                        _data.writeInt(1);
                        aapAtributes.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void disableVideoFocus(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void disableAAP(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void registerCallback(IBinder iBinder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(iBinder);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void surfaceDestroyed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public void surfaceChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public boolean getVRStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.allgo.app.androidauto.IAndroidAutoMainService
            public int sendMediaPlayControl(int mediaKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mediaKey);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
