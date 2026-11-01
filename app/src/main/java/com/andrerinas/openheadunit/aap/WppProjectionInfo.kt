package com.andrerinas.openheadunit.aap

import java.io.ByteArrayOutputStream

/**
 * What a head unit tells the phone, in its Type 4 `WifiVersionRequest`, to let the phone reach it
 * again later over the Wi-Fi it is already on, without Bluetooth ("WPP over TCP").
 *
 * Read from Android Auto 17.9 (Gearhead), not from a specification:
 *
 * ```
 * WifiVersionRequest {
 *   int32 major = 1;                              // protocol version, 4.2 or later for WPP over TCP
 *   int32 minor = 2;
 *   ...                                           // fields 3 to 5: not used here
 *   WifiProjectionProtocolInfo tcp = 6 {
 *     string ip_address = 1;                      // where to dial
 *     int32 tcp_port = 2;
 *   }
 * }
 * ```
 *
 * The phone only looks at field 6 when the version is at least 4.1, and for exactly 4.1 only for head
 * units on its allow list; 4.2 and later are taken as they are. It then keeps the address and port with the
 * Wi-Fi network it was given, and on a later visit to that network dials them first, falling back to
 * Bluetooth when that does not work.
 *
 * The message is built by hand: it is two varints and one nested message, and no protobuf compiler is needed
 * for it.
 */
object WppProjectionInfo {

    /** The first version the phone takes a WPP-over-TCP endpoint from without an allow list. */
    const val TCP_MAJOR = 4
    const val TCP_MINOR = 2

    /** Field numbers of the messages above. */
    private const val FIELD_MAJOR = 1
    private const val FIELD_MINOR = 2
    private const val FIELD_TCP = 6
    private const val FIELD_TCP_IP = 1
    private const val FIELD_TCP_PORT = 2

    private const val WIRE_VARINT = 0
    private const val WIRE_LENGTH_DELIMITED = 2

    /**
     * The payload of a Type 4 message.
     *
     * @param ip the address the phone is to dial, or null to leave the endpoint out (the plain version
     *   request that was sent before this existed).
     * @param port the TCP port the phone is to dial.
     */
    fun encodeVersionRequest(major: Int, minor: Int, ip: String?, port: Int): ByteArray {
        val out = ByteArrayOutputStream()
        writeVarintField(out, FIELD_MAJOR, major)
        writeVarintField(out, FIELD_MINOR, minor)
        if (ip != null) {
            val info = ByteArrayOutputStream()
            val address = ip.toByteArray(Charsets.UTF_8)
            writeTag(info, FIELD_TCP_IP, WIRE_LENGTH_DELIMITED)
            writeVarint(info, address.size)
            info.write(address)
            writeVarintField(info, FIELD_TCP_PORT, port)
            val bytes = info.toByteArray()
            writeTag(out, FIELD_TCP, WIRE_LENGTH_DELIMITED)
            writeVarint(out, bytes.size)
            out.write(bytes)
        }
        return out.toByteArray()
    }

    private fun writeVarintField(out: ByteArrayOutputStream, field: Int, value: Int) {
        writeTag(out, field, WIRE_VARINT)
        writeVarint(out, value)
    }

    private fun writeTag(out: ByteArrayOutputStream, field: Int, wireType: Int) =
        writeVarint(out, (field shl 3) or wireType)

    /** A non-negative int32 as a protobuf varint. */
    private fun writeVarint(out: ByteArrayOutputStream, value: Int) {
        require(value >= 0) { "negative varints are not used here" }
        var v = value
        while (v >= 0x80) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v)
    }
}
