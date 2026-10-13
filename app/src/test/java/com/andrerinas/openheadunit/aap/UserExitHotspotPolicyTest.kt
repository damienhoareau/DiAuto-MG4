package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserExitHotspotPolicyTest {

    private fun exit(mode: Int, strategy: Int = 2, teardownProvenUnsafe: Boolean = false) =
        UserExitHotspotPolicy.onUserExit(mode, strategy, teardownProvenUnsafe)

    @Test
    fun `native AA on this unit's own access point takes it down so the phone leaves`() {
        assertEquals(HotspotExitAction.DISABLE, exit(mode = 3))
    }

    @Test
    fun `the helper's head-unit-hotspot strategy is the same route by another name`() {
        assertEquals(HotspotExitAction.DISABLE, exit(mode = 2, strategy = 4))
    }

    @Test
    fun `the helper's other strategies and the server mode are left alone`() {
        for (strategy in listOf(0, 1, 2, 3)) {
            assertEquals(
                "helper strategy $strategy hosts no access point",
                HotspotExitAction.NONE,
                exit(mode = 2, strategy = strategy)
            )
        }
        assertEquals(HotspotExitAction.NONE, exit(mode = 1, strategy = 0))
    }

    @Test
    fun `a device that could not switch its access point back on is not asked to again`() {
        // Learned the only way it can be — by trying once and watching it fail. From then on the
        // hotspot is left alone, because on such a device taking it down costs a hotspot per
        // session and never buys the phone leaving.
        assertEquals(
            HotspotExitAction.WARN_LEFT_UP,
            exit(mode = 3, teardownProvenUnsafe = true)
        )
    }

    @Test
    fun `the flag does not make this policy claim a disconnect that was never its own`() {
        // It withdraws; it never reaches.
        assertEquals(HotspotExitAction.NONE, exit(mode = 1, teardownProvenUnsafe = true))
    }

    @Test
    fun `which routes own an access point`() {
        assertTrue(UserExitHotspotPolicy.usesHeadUnitHotspot(3, 0))
        assertTrue(UserExitHotspotPolicy.usesHeadUnitHotspot(2, 4))
        assertFalse(UserExitHotspotPolicy.usesHeadUnitHotspot(2, 0))
        assertFalse(UserExitHotspotPolicy.usesHeadUnitHotspot(1, 4))
    }
}
