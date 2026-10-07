package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WirelessServerBindPolicyTest {

    @Test
    fun `empty advertised IP keeps the wildcard`() {
        assertNull(WirelessServerBindPolicy.bindHostForAdvertisedIp(null))
        assertNull(WirelessServerBindPolicy.bindHostForAdvertisedIp(""))
        assertNull(WirelessServerBindPolicy.bindHostForAdvertisedIp("   "))
    }

    @Test
    fun `a SoftAP IPv4 becomes the bind host`() {
        assertEquals(
            "192.168.43.1",
            WirelessServerBindPolicy.bindHostForAdvertisedIp("192.168.43.1")
        )
        assertEquals(
            "192.168.43.1",
            WirelessServerBindPolicy.bindHostForAdvertisedIp(" 192.168.43.1 ")
        )
    }

    @Test
    fun `wildcard listening needs a rebind once SoftAP IP is known`() {
        // DiPlay's success path: control socket on SoftAP IPv4, not 0.0.0.0.
        assertTrue(WirelessServerBindPolicy.needsRebind(null, "192.168.43.1"))
        assertTrue(WirelessServerBindPolicy.needsRebind("", "192.168.43.1"))
    }

    @Test
    fun `already on the SoftAP IP does not rebind`() {
        assertFalse(WirelessServerBindPolicy.needsRebind("192.168.43.1", "192.168.43.1"))
    }

    @Test
    fun `wrong SoftAP IP needs a rebind`() {
        assertTrue(WirelessServerBindPolicy.needsRebind("192.168.42.1", "192.168.43.1"))
    }

    @Test
    fun `no advertised IP never forces a rebind`() {
        assertFalse(WirelessServerBindPolicy.needsRebind(null, null))
        assertFalse(WirelessServerBindPolicy.needsRebind("192.168.43.1", null))
        assertFalse(WirelessServerBindPolicy.needsRebind(null, ""))
    }
}
