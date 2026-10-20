package com.allgo.app.androidauto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSwitchPolicyTest {

    private val a = "AA:BB:CC:DD:EE:01"
    private val b = "AA:BB:CC:DD:EE:02"

    @Test
    fun anotherPhoneReplacesTheSessionInProgress() {
        assertTrue(DeviceSwitchPolicy.shouldSwitch(b, a, sessionUp = true))
    }

    @Test
    fun thePhoneAlreadyProjectingIsLeftAlone() {
        assertFalse(DeviceSwitchPolicy.shouldSwitch(a, a, sessionUp = true))
    }

    @Test
    fun theCaseAndSpacesOfAnAddressDoNotMakeAnotherPhone() {
        assertFalse(DeviceSwitchPolicy.shouldSwitch(" aa:bb:cc:dd:ee:01 ", a, sessionUp = true))
    }

    @Test
    fun withoutASessionNothingIsReplaced() {
        assertFalse(DeviceSwitchPolicy.shouldSwitch(b, a, sessionUp = false))
    }

    @Test
    fun aRequestThatNamesNoPhoneNeverEndsASession() {
        assertFalse(DeviceSwitchPolicy.shouldSwitch(null, a, sessionUp = true))
        assertFalse(DeviceSwitchPolicy.shouldSwitch("", a, sessionUp = true))
        assertFalse(DeviceSwitchPolicy.shouldSwitch(DeviceSwitchPolicy.UNKNOWN_ADDRESS, a, sessionUp = true))
    }

    @Test
    fun anUnknownActivePhoneCannotBeTold() {
        assertFalse(DeviceSwitchPolicy.shouldSwitch(b, null, sessionUp = true))
        assertFalse(DeviceSwitchPolicy.shouldSwitch(b, DeviceSwitchPolicy.UNKNOWN_ADDRESS, sessionUp = true))
    }
}
