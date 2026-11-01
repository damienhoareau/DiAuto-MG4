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
}
