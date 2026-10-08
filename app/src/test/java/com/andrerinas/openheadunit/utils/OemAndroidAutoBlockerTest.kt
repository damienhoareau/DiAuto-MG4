package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class OemAndroidAutoBlockerTest {
    @Test
    fun `targets AllGo Android Auto and SAIC bridge not CarPlay or shared RUI`() {
        assertTrue(OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.allgo.app.androidauto"))
        assertTrue(OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.saicmotor.caradapter"))
        assertTrue(!OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.allgo.rui"))
        assertTrue(!OemAndroidAutoBlocker.TARGET_PACKAGES.contains("com.allgo.carplay.service"))
    }
}
