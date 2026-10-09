package com.andrerinas.openheadunit.utils

import android.content.Context
import android.content.Intent
import com.andrerinas.openheadunit.aap.DummyVpnService

object VpnControl {
    fun stopVpn(context: Context) {
        AppLog.i("VpnControl: Stopping DummyVpnService")
        try {
            context.startService(Intent(context, DummyVpnService::class.java).apply {
                action = DummyVpnService.ACTION_STOP_VPN
            })
        } catch (e: Exception) {
            AppLog.e("VpnControl: Failed to stop VPN", e)
        }
    }
}
