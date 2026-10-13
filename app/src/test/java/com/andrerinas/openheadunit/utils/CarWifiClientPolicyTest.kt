package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarWifiClientPolicyTest {

    @Test
    fun `the client is switched off on the car install when it is on`() {
        assertTrue(CarWifiClientPolicy.shouldSuspend(isSystemInstall = true, wifiEnabled = true))
    }

    @Test
    fun `the client is left alone outside the car install`() {
        assertFalse(CarWifiClientPolicy.shouldSuspend(isSystemInstall = false, wifiEnabled = true))
    }

    @Test
    fun `there is nothing to switch off when the client is already off`() {
        assertFalse(CarWifiClientPolicy.shouldSuspend(isSystemInstall = true, wifiEnabled = false))
    }

    @Test
    fun `the access point is restarted when it was up while the client was on`() {
        assertTrue(CarWifiClientPolicy.shouldRestartAccessPoint(clientWasOn = true, accessPointUp = true))
    }

    @Test
    fun `an access point that is down is not restarted`() {
        assertFalse(CarWifiClientPolicy.shouldRestartAccessPoint(clientWasOn = true, accessPointUp = false))
    }

    @Test
    fun `an access point started after the client was already off is left alone`() {
        assertFalse(CarWifiClientPolicy.shouldRestartAccessPoint(clientWasOn = false, accessPointUp = true))
    }

    @Test
    fun `the client is switched back on only if we switched it off and it is still off`() {
        assertTrue(CarWifiClientPolicy.shouldRestore(suspendedByUs = true, wifiEnabled = false))
    }

    @Test
    fun `a client the user turned off is never switched back on`() {
        assertFalse(CarWifiClientPolicy.shouldRestore(suspendedByUs = false, wifiEnabled = false))
    }

    @Test
    fun `a client the user turned back on is left as it is`() {
        assertFalse(CarWifiClientPolicy.shouldRestore(suspendedByUs = true, wifiEnabled = true))
    }

    @Test
    fun `the Wi-Fi only comes back after the phone has had time to reconnect`() {
        assertEquals(3 * 60_000L, CarWifiClientPolicy.RECONNECT_GRACE_MS)
        assertTrue(CarWifiClientPolicy.RECONNECT_GRACE_MS >= CarWifiClientPolicy.IDLE_RESTORE_MS)
    }
}
