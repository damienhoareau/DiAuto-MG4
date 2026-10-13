package com.andrerinas.openheadunit.aap

/**
 * What the car asks of the projection through RemoteUI (IAndroidAutoMainService.disableTouchInput /
 * disableVideoFocus), for instance while the reversing camera or a native screen is up.
 */
object ProjectionGate {
    @Volatile var touchDisabled = false
    @Volatile var videoFocusDisabled = false

    fun reset() {
        touchDisabled = false
        videoFocusDisabled = false
    }
}
