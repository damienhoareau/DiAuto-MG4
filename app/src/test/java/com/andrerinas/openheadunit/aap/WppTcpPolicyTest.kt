package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WppTcpPolicyTest {

    @Test
    fun `the setting rules when the property is absent or unrelated`() {
        assertTrue(WppTcpPolicy.enabled(setting = true, property = null))
        assertTrue(WppTcpPolicy.enabled(setting = true, property = ""))
        assertFalse(WppTcpPolicy.enabled(setting = false, property = ""))
        assertTrue(WppTcpPolicy.enabled(setting = true, property = "maybe"))
    }

    @Test
    fun `the property overrides the setting both ways`() {
        assertFalse(WppTcpPolicy.enabled(setting = true, property = "0"))
        assertTrue(WppTcpPolicy.enabled(setting = false, property = "1"))
        assertFalse(WppTcpPolicy.enabled(setting = true, property = " 0 "))
    }
}
