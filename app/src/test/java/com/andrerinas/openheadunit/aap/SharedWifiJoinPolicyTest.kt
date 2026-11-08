package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.SharedWifiJoinPolicy.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedWifiJoinPolicyTest {

    @Test
    fun `a client being switched on counts as scanning`() {
        assertEquals(Phase.SCANNING, SharedWifiJoinPolicy.phaseOf(false, true, null, false))
    }

    @Test
    fun `a client that is off is not waited for`() {
        val phase = SharedWifiJoinPolicy.phaseOf(false, false, "COMPLETED", true)
        assertEquals(Phase.OFF, phase)
        assertFalse(SharedWifiJoinPolicy.shouldKeepWaiting(phase, 0))
    }

    @Test
    fun `connected with an address is ready, without one it is still joining`() {
        assertEquals(Phase.READY, SharedWifiJoinPolicy.phaseOf(true, false, "COMPLETED", true))
        assertEquals(Phase.JOINING, SharedWifiJoinPolicy.phaseOf(true, false, "COMPLETED", false))
    }

    @Test
    fun `every association step counts as joining`() {
        for (state in listOf("AUTHENTICATING", "ASSOCIATING", "ASSOCIATED", "FOUR_WAY_HANDSHAKE", "GROUP_HANDSHAKE")) {
            assertEquals(state, Phase.JOINING, SharedWifiJoinPolicy.phaseOf(true, false, state, false))
        }
    }

    @Test
    fun `an idle client is not waited for`() {
        for (state in listOf("DISCONNECTED", "INACTIVE", "DORMANT", "INTERFACE_DISABLED", "UNINITIALIZED", "INVALID", null)) {
            val phase = SharedWifiJoinPolicy.phaseOf(true, false, state, false)
            assertEquals(state.toString(), Phase.IDLE, phase)
            assertFalse(SharedWifiJoinPolicy.shouldKeepWaiting(phase, 0))
        }
    }

    @Test
    fun `scanning is waited for less long than joining`() {
        assertTrue(SharedWifiJoinPolicy.shouldKeepWaiting(Phase.SCANNING, 2_999))
        assertFalse(SharedWifiJoinPolicy.shouldKeepWaiting(Phase.SCANNING, 3_000))
        assertTrue(SharedWifiJoinPolicy.shouldKeepWaiting(Phase.JOINING, 5_999))
        assertFalse(SharedWifiJoinPolicy.shouldKeepWaiting(Phase.JOINING, 6_000))
    }

    @Test
    fun `the wait leaves the phone's first-message timer room`() {
        // 300 ms RFCOMM settle + the longest wait must stay well under the phone's ~12 s.
        assertTrue(300 + SharedWifiJoinPolicy.JOINING_WAIT_MS <= 8_000)
    }
}
