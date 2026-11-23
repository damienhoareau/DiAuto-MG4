package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WppProjectionInfoTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun `without an endpoint the request is only the version`() {
        assertArrayEquals(bytes(0x08, 1, 0x10, 1), WppProjectionInfo.encodeVersionRequest(1, 1, null, 30517))
    }

    @Test
    fun `the endpoint goes in field 6 as a nested message`() {
        val payload = WppProjectionInfo.encodeVersionRequest(4, 2, "10.0.0.5", 30517)
        // 08 04 | 10 02 | 32 len | 0a 08 "10.0.0.5" | 10 <30517 as a varint>
        val expected = bytes(0x08, 4, 0x10, 2, 0x32, 14, 0x0a, 8) +
            "10.0.0.5".toByteArray() + bytes(0x10, 0xB5, 0xEE, 0x01)
        assertArrayEquals(expected, payload)
    }

    @Test
    fun `a port above 127 takes more than one varint byte`() {
        val payload = WppProjectionInfo.encodeVersionRequest(4, 2, "a", 300)
        // 300 = 0xAC 0x02
        assertEquals(listOf(0x10, 0xAC, 0x02), payload.takeLast(3).map { it.toInt() and 0xFF })
    }

    @Test
    fun `the length of the nested message counts exactly its bytes`() {
        val ip = "192.168.1.32"
        val payload = WppProjectionInfo.encodeVersionRequest(4, 2, ip, 30517)
        val declared = payload[5].toInt() and 0xFF
        assertEquals(payload.size - 6, declared)
    }

    // --- the frequencies the phone will be sent to (field 4) ----------------------------------

    @Test
    fun `a frequency goes in field 4 as a varint, as LIVI sends it`() {
        // LIVI's reference bytes for 6.0 on channel 36: 08 06 10 00 20 bc 28
        assertArrayEquals(
            bytes(0x08, 6, 0x10, 0, 0x20, 0xBC, 0x28),
            WppProjectionInfo.encodeVersionRequest(6, 0, null, 30517, listOf(5180)),
        )
    }

    @Test
    fun `the frequency comes before the endpoint, fields in order`() {
        val payload = WppProjectionInfo.encodeVersionRequest(4, 2, "10.0.0.5", 30517, listOf(2437))
        // 08 04 | 10 02 | 20 <2437 = 85 13> | 32 ...
        assertEquals(listOf(0x08, 4, 0x10, 2, 0x20, 0x85, 0x13, 0x32), payload.take(8).map { it.toInt() and 0xFF })
    }

    @Test
    fun `no frequency or a non-positive one leaves the request as it was`() {
        val before = WppProjectionInfo.encodeVersionRequest(4, 2, "10.0.0.5", 30517)
        assertArrayEquals(before, WppProjectionInfo.encodeVersionRequest(4, 2, "10.0.0.5", 30517, emptyList()))
        assertArrayEquals(before, WppProjectionInfo.encodeVersionRequest(4, 2, "10.0.0.5", 30517, listOf(0)))
    }

    @Test
    fun `the frequency reads back as field 4`() {
        val payload = WppProjectionInfo.encodeVersionRequest(4, 2, "10.0.0.5", 30517, listOf(5745))
        val parsed = com.andrerinas.openheadunit.aap.protocol.proto.Wireless.WifiVersionRequest.parseFrom(payload)
        assertEquals(4, parsed.major)
        assertEquals(listOf(5745L), parsed.unknownFields.getField(4).varintList)
    }

    // --- the head unit's answer to a start request the phone sent itself ---------------------

    @Test
    fun `a successful start response carries the endpoint and a zero status`() {
        val payload = WppProjectionInfo.encodeStartResponse("10.0.0.5", 30518, WppStatus.SUCCESS)
        // 0a 08 "10.0.0.5" | 10 <30518> | 18 00
        val expected = bytes(0x0a, 8) + "10.0.0.5".toByteArray() + bytes(0x10, 0xB6, 0xEE, 0x01, 0x18, 0)
        assertArrayEquals(expected, payload)
    }

    @Test
    fun `a negative status is a ten-byte int32 and nothing else goes with it`() {
        val payload = WppProjectionInfo.encodeStartResponse(null, null, WppStatus.NOT_YET_STARTED)
        // -6 as int32: fa ff ff ff ff ff ff ff ff 01
        val expected = bytes(0x18, 0xFA, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01)
        assertArrayEquals(expected, payload)
    }

    @Test
    fun `the start response reads back with protobuf`() {
        val payload = WppProjectionInfo.encodeStartResponse("192.168.43.1", 30518, WppStatus.SUCCESS)
        val parsed = com.andrerinas.openheadunit.aap.protocol.proto.Wireless.WifiStartResponse.parseFrom(payload)
        assertEquals("192.168.43.1", parsed.ipAddress)
        assertEquals(0, parsed.status)
        // The port is not in our schema (it models the phone's acknowledgement), so it reads as an unknown field.
        assertEquals(listOf(30518L), parsed.unknownFields.getField(2).varintList)

        val notYet = com.andrerinas.openheadunit.aap.protocol.proto.Wireless.WifiStartResponse
            .parseFrom(WppProjectionInfo.encodeStartResponse(null, null, WppStatus.NOT_YET_STARTED))
        assertEquals(-6, notYet.status)
    }
}
