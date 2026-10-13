package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarHotspotPolicyTest {

    private fun good() = CarHotspotPolicy.Snapshot("MG", "secret123", 1, 149, true)

    @Test
    fun `a 5 GHz WPA2 hotspot with a name and passphrase is accepted as it stands`() {
        assertNull(CarHotspotPolicy.problem(good(), channel = 149))
    }

    @Test
    fun `an unreadable configuration is a problem`() {
        assertNotNull(CarHotspotPolicy.problem(null, channel = null))
    }

    @Test
    fun `2_4 GHz or open or unnamed hotspots are rejected`() {
        assertNotNull(CarHotspotPolicy.problem(good().copy(apBand = 0), null))
        assertNotNull(CarHotspotPolicy.problem(good().copy(wpa2 = false), null))
        assertNotNull(CarHotspotPolicy.problem(good().copy(ssid = " "), null))
        assertNotNull(CarHotspotPolicy.problem(good().copy(passphrase = "short"), null))
    }

    @Test
    fun `the channel only matters when the firmware names one`() {
        val other = good().copy(apChannel = 36)
        assertNull(CarHotspotPolicy.problem(other, channel = null))
        assertNotNull(CarHotspotPolicy.problem(other, channel = 149))
    }

    @Test
    fun `the channel property is honoured only for allowed channels`() {
        assertEquals(157, CarHotspotPolicy.channelFromProperty("157"))
        assertEquals(48, CarHotspotPolicy.channelFromProperty(" 48 "))
        assertNull(CarHotspotPolicy.channelFromProperty("36"))
        assertNull(CarHotspotPolicy.channelFromProperty(""))
        assertNull(CarHotspotPolicy.channelFromProperty(null))
        assertNull(CarHotspotPolicy.channelFromProperty("abc"))
    }

    @Test
    fun `an existing name and valid passphrase are kept`() {
        assertEquals("MG", CarHotspotPolicy.ssidOrDefault("MG"))
        assertEquals("secret123", CarHotspotPolicy.passphraseOrGenerate("secret123"))
    }

    @Test
    fun `a missing name or passphrase is replaced`() {
        assertEquals(CarHotspotPolicy.DEFAULT_SSID, CarHotspotPolicy.ssidOrDefault(null))
        assertEquals(CarHotspotPolicy.DEFAULT_SSID, CarHotspotPolicy.ssidOrDefault("  "))
        val generated = CarHotspotPolicy.passphraseOrGenerate("abc")
        assertEquals(12, generated.length)
        assertTrue(generated.all { it.isLetterOrDigit() })
    }
}
