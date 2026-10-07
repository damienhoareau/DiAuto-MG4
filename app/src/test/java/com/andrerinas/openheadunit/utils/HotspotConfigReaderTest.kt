package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class HotspotConfigReaderTest {

    @Test
    fun `Android 9 SoftAP quotes are stripped from SSID and passphrase`() {
        // DiPlay ManualHotspotManager.unquote — same rule for both fields.
        assertEquals("AndroidAP", HotspotConfigReader.unquote("\"AndroidAP\""))
        assertEquals("swordfish", HotspotConfigReader.unquote("\"swordfish\""))
    }

    @Test
    fun `unquoted values pass through`() {
        assertEquals("AndroidAP", HotspotConfigReader.unquote("AndroidAP"))
        assertEquals("swordfish", HotspotConfigReader.unquote("swordfish"))
    }

    @Test
    fun `null and empty become empty`() {
        assertEquals("", HotspotConfigReader.unquote(null))
        assertEquals("", HotspotConfigReader.unquote(""))
    }

    @Test
    fun `a single quote is not treated as a SoftAP wrapper`() {
        assertEquals("\"", HotspotConfigReader.unquote("\""))
        assertEquals("\"x", HotspotConfigReader.unquote("\"x"))
    }
}
