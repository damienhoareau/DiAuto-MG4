package com.andrerinas.openheadunit.launcher

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings as AppSettings

/**
 * Enables/disables [LauncherAndroidAutoClickService] via Secure settings.
 * Works on platform-signed MG4 builds (system UID / WRITE_SECURE_SETTINGS).
 */
object LauncherAaClickHelper {

    private const val TAG = "LauncherAaClick"

    fun syncFromSettings(context: Context) {
        val enabled = try {
            AppSettings(context).openOnLauncherAndroidAutoClick
        } catch (e: Exception) {
            AppLog.w("$TAG: settings unavailable (${e.message})")
            return
        }
        if (enabled) {
            enableService(context)
        } else {
            disableService(context)
        }
    }

    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, LauncherAndroidAutoClickService::class.java)
            .flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun enableService(context: Context): Boolean {
        return try {
            val cn = ComponentName(context, LauncherAndroidAutoClickService::class.java)
            val flat = cn.flattenToString()
            val resolver = context.contentResolver
            val current = Settings.Secure.getString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (current.isNullOrEmpty()) {
                Settings.Secure.putString(
                    resolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    flat
                )
            } else if (!isServiceEnabled(context)) {
                Settings.Secure.putString(
                    resolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    "$current:$flat"
                )
            }
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            AppLog.i("$TAG: accessibility service enabled ($flat)")
            true
        } catch (e: SecurityException) {
            AppLog.w("$TAG: cannot enable accessibility service (need WRITE_SECURE_SETTINGS): ${e.message}")
            false
        } catch (e: Exception) {
            AppLog.e("$TAG: enable failed", e)
            false
        }
    }

    fun disableService(context: Context): Boolean {
        return try {
            val cn = ComponentName(context, LauncherAndroidAutoClickService::class.java)
            val flat = cn.flattenToString()
            val resolver = context.contentResolver
            val current = Settings.Secure.getString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return true
            val remaining = current.split(':')
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals(flat, ignoreCase = true) }
            Settings.Secure.putString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                remaining.joinToString(":")
            )
            if (remaining.isEmpty()) {
                Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
            }
            AppLog.i("$TAG: accessibility service disabled")
            true
        } catch (e: SecurityException) {
            AppLog.w("$TAG: cannot disable accessibility service: ${e.message}")
            false
        } catch (e: Exception) {
            AppLog.e("$TAG: disable failed", e)
            false
        }
    }
}
