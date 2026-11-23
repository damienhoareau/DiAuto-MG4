package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.proto.Wireless
import com.google.protobuf.ByteString
import com.google.protobuf.UnknownFieldSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WppPhoneIdentityTest {

    private fun field(bytes: ByteString) = UnknownFieldSet.Field.newBuilder().addLengthDelimited(bytes).build()
    private fun text(value: String) = field(ByteString.copyFromUtf8(value))

    /** A version response as a phone sends it, with PhoneInfo in field 6. */
    private fun response(serial: String? = null, instanceId: String? = null, btName: String? = null, btAddress: String? = null): ByteArray {
        val identity = UnknownFieldSet.newBuilder().apply {
            instanceId?.let { addField(1, text(it)) }
            btName?.let { addField(3, text(it)) }
            btAddress?.let { addField(4, text(it)) }
        }.build()
        return Wireless.WifiVersionResponse.newBuilder()
            .setMajor(4).setMinor(2).setStatus(0)
            .apply { serial?.let { setDeviceSerial(it) } }
            .setUnknownFields(UnknownFieldSet.newBuilder().addField(6, field(identity.toByteString())).build())
            .build()
            .toByteArray()
    }

    @Test
    fun `every field of the identity is read`() {
        val id = WppPhoneIdentity.parse(response("R5CT1234ABCD", "0f1e2d3c-uuid", "Pixel 9", "AA:BB:CC:DD:EE:FF"))!!

        assertEquals("R5CT1234ABCD", id.deviceSerial)
        assertEquals("0f1e2d3c-uuid", id.instanceId)
        assertEquals("Pixel 9", id.bluetoothName)
        assertEquals("AA:BB:CC:DD:EE:FF", id.bluetoothAddress)
        assertFalse(id.isEmpty())
    }

    @Test
    fun `the serial is shortened in the log line`() {
        val line = WppPhoneIdentity.parse(response("R5CT1234ABCD", "uuid"))!!.describe()

        assertTrue(line, line.contains("serial=…ABCD"))
        assertFalse(line, line.contains("R5CT1234ABCD"))
        assertTrue(line, line.contains("instanceId=uuid"))
    }

    @Test
    fun `a response without identity is empty, not missing`() {
        val payload = Wireless.WifiVersionResponse.newBuilder().setMajor(4).setMinor(2).setStatus(-8).build().toByteArray()
        val id = WppPhoneIdentity.parse(payload)!!

        assertTrue(id.isEmpty())
        assertNull(id.instanceId)
    }

    @Test
    fun `an empty serial counts as none`() {
        assertNull(WppPhoneIdentity.parse(response(serial = "", instanceId = "x"))!!.deviceSerial)
    }

    @Test
    fun `garbage is not a version response`() {
        assertNull(WppPhoneIdentity.parse(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())))
    }
}
