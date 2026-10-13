package com.allgo.app.androidauto

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Placeholder for the Allgo `AndroidAutoService` component. Nothing on the car binds to it with an
 * AIDL contract (RemoteUI only uses AndroidAutoMainService); it exists so the component name the
 * boot receiver and any component lookup expect is still there.
 */
class AndroidAutoService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
}
