package com.andrerinas.openheadunit.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.andrerinas.openheadunit.main.MainActivity
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.Settings

/**
 * Watches the OEM MG4 launcher Android Auto tile.
 *
 * When the icon is visually inactive (not selected / not connected) and the user
 * taps it, the OEM shows a "connect USB" toast. This service intercepts that tap
 * and opens DiAuto instead of leaving the user with only the toast.
 */
class LauncherAndroidAutoClickService : AccessibilityService() {

    private var lastLaunchElapsedMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED) return
        if (event.packageName?.toString() != LAUNCHER_PACKAGE) return

        val settings = try {
            Settings(this)
        } catch (_: Exception) {
            return
        }
        if (!settings.openOnLauncherAndroidAutoClick) return

        val source = event.source ?: return
        try {
            val viewId = source.viewIdResourceName
            if (viewId.isNullOrEmpty()) {
                // Some builds omit the id on the event source; resolve from window root.
                resolveFromRoot(source)
                return
            }
            if (isAndroidAutoControl(viewId)) {
                maybeLaunch(source)
            }
        } finally {
            source.recycle()
        }
    }

    private fun resolveFromRoot(clicked: AccessibilityNodeInfo) {
        val root = rootInActiveWindow ?: return
        try {
            for (fullId in ANDROID_AUTO_VIEW_IDS) {
                val matches = root.findAccessibilityNodeInfosByViewId(fullId) ?: continue
                try {
                    for (node in matches) {
                        if (sameNode(clicked, node) || overlapsBounds(clicked, node)) {
                            maybeLaunch(node)
                            return
                        }
                    }
                } finally {
                    matches.forEach { it.recycle() }
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun sameNode(a: AccessibilityNodeInfo, b: AccessibilityNodeInfo): Boolean {
        return a == b || (a.windowId == b.windowId && a.hashCode() == b.hashCode())
    }

    private fun overlapsBounds(a: AccessibilityNodeInfo, b: AccessibilityNodeInfo): Boolean {
        val ra = Rect()
        val rb = Rect()
        a.getBoundsInScreen(ra)
        b.getBoundsInScreen(rb)
        return ra.intersect(rb)
    }

    private fun maybeLaunch(node: AccessibilityNodeInfo) {
        // Launcher uses setSelected(connected). Inactive = not selected → OEM USB toast path.
        if (node.isSelected) {
            AppLog.d("$TAG: Android Auto tile is active/connected — ignoring")
            return
        }
        launchDiAuto()
    }

    private fun isAndroidAutoControl(viewId: String): Boolean {
        return viewId.endsWith(":id/iv_android_auto") || viewId.endsWith(":id/tv_android_auto")
    }

    private fun launchDiAuto() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunchElapsedMs < DEBOUNCE_MS) return
        lastLaunchElapsedMs = now

        AppLog.i("$TAG: inactive Android Auto tile clicked — launching DiAuto")
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            putExtra(MainActivity.EXTRA_LAUNCH_SOURCE, "Launcher Android Auto button")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            AppLog.e("$TAG: failed to launch MainActivity", e)
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLog.i("$TAG: service connected")
    }

    companion object {
        private const val TAG = "LauncherAaClick"
        private const val LAUNCHER_PACKAGE = "com.saicmotor.hmi.launcher"
        private const val DEBOUNCE_MS = 800L

        private val ANDROID_AUTO_VIEW_IDS = listOf(
            "$LAUNCHER_PACKAGE:id/iv_android_auto",
            "$LAUNCHER_PACKAGE:id/tv_android_auto"
        )
    }
}
