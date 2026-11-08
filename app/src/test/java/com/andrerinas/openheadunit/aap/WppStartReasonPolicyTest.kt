package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.WppStartReasonPolicy.LAUNCH_ALWAYS
import com.andrerinas.openheadunit.aap.WppStartReasonPolicy.LAUNCH_PHONE_PREFERENCE
import com.andrerinas.openheadunit.aap.protocol.proto.Wireless
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WppStartReasonPolicyTest {

    @Test
    fun `always is a user request whatever happened before`() {
        assertEquals(WppStartReason.USER_REQUEST, WppStartReasonPolicy.choose(LAUNCH_ALWAYS, null, null))
        assertEquals(WppStartReason.USER_REQUEST, WppStartReasonPolicy.choose(LAUNCH_ALWAYS, null, 1_000))
    }

    @Test
    fun `an unknown stored mode behaves as always`() {
        assertEquals(WppStartReason.USER_REQUEST, WppStartReasonPolicy.choose(7, null, 1_000))
    }

    @Test
    fun `phone preference with nothing before is an automatic launch`() {
        assertEquals(WppStartReason.AUTO_LAUNCH, WppStartReasonPolicy.choose(LAUNCH_PHONE_PREFERENCE, null, null))
    }

    @Test
    fun `a recent tap on connect is a user request, and wins over a restart`() {
        assertEquals(WppStartReason.USER_REQUEST, WppStartReasonPolicy.choose(LAUNCH_PHONE_PREFERENCE, 5_000, 5_000))
    }

    @Test
    fun `an old tap no longer counts`() {
        assertEquals(WppStartReason.AUTO_LAUNCH,
            WppStartReasonPolicy.choose(LAUNCH_PHONE_PREFERENCE, WppStartReasonPolicy.USER_REQUEST_WINDOW_MS, null))
    }

    @Test
    fun `a reconnection after an unexpected disconnect is a restart, until the window closes`() {
        assertEquals(WppStartReason.AUTOMATIC_RESTART, WppStartReasonPolicy.choose(LAUNCH_PHONE_PREFERENCE, null, 30_000))
        assertEquals(WppStartReason.AUTO_LAUNCH,
            WppStartReasonPolicy.choose(LAUNCH_PHONE_PREFERENCE, null, WppStartReasonPolicy.RESTART_WINDOW_MS))
    }

    @Test
    fun `the wire values match the phone's enum`() {
        assertEquals(1, WppStartReason.USER_REQUEST.wire)
        assertEquals(2, WppStartReason.AUTO_LAUNCH.wire)
        assertEquals(3, WppStartReason.AUTOMATIC_RESTART.wire)
    }

    @Test
    fun `the start request carries the reason as field 4`() {
        val bytes = Wireless.WifiStartRequest.newBuilder()
            .setIpAddress("10.0.0.1").setPort(5288).setStatus(0)
            .setStartReason(WppStartReason.AUTO_LAUNCH.wire)
            .build().toByteArray()
        // Field 4, varint: tag 0x20, value 2, at the end of the message.
        assertEquals(0x20.toByte(), bytes[bytes.size - 2])
        assertEquals(2.toByte(), bytes[bytes.size - 1])
        assertEquals(2, Wireless.WifiStartRequest.parseFrom(bytes).startReason)
    }

    @Test
    fun `a start request without the field still parses, as a user request`() {
        val parsed = Wireless.WifiStartRequest.parseFrom(
            Wireless.WifiStartRequest.newBuilder().setIpAddress("10.0.0.1").setPort(5288).build().toByteArray())
        assertFalse(parsed.hasStartReason())
    }
}
