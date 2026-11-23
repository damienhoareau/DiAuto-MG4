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
 *   ...                                           // field 3: not used here
 *   repeated int32 supported_channel_frequencies_mhz = 4;   // the Wi-Fi the phone will be sent to
 *   ...                                           // field 5: not used here
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

    /**
     * The version announced in every version request: 6.0, as LIVI announces it. It clears every gate known
     * on the phone's side: 4.2 for taking the WPP-over-TCP endpoint without an allow list (the 4.2 the
     * Urban head unit sends, and that this used to send), 4.5 for the setup info, 1.6 for the phone to
     * describe itself (PhoneInfo, see WppPhoneIdentity).
     */
    const val TCP_MAJOR = 6
    const val TCP_MINOR = 0

    /** Field numbers of the messages above. */
    private const val FIELD_MAJOR = 1
    private const val FIELD_MINOR = 2
    private const val FIELD_FREQUENCIES = 4
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
     * @param frequenciesMhz the Wi-Fi frequencies the phone will be sent to, in MHz (field 4, repeated,
     *   one varint each, as LIVI sends it: `supported_channel_frequencies_mhz`). A phone given none answers
     *   the version request with NO_SUPPORTED_CHANNELS (-8). Empty leaves the field out; values that are
     *   not positive are dropped.
     */
    fun encodeVersionRequest(
        major: Int,
        minor: Int,
        ip: String?,
        port: Int,
        frequenciesMhz: List<Int> = emptyList(),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        writeVarintField(out, FIELD_MAJOR, major)
        writeVarintField(out, FIELD_MINOR, minor)
        for (mhz in frequenciesMhz) if (mhz > 0) writeVarintField(out, FIELD_FREQUENCIES, mhz)
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

    /** The setup info is only taken from this version on (Android Auto 17.9: 4.5). */
    const val SETUP_MAJOR = 4
    const val SETUP_MINOR = 5

    /**
     * The payload of a Type 11 `WifiSetupInfo`, from the same reading of Android Auto 17.9:
     *
     * ```
     * WifiSetupInfo { int32 major = 1; int32 minor = 2; bytes token = 3;
     *                 WifiProjectionProtocolInfo tcp = 4; AccessPointInfo ap = 5; }
     * AccessPointInfo { string ssid = 1; string key = 2; string bssid = 3; SecurityMode security = 4; }
     * ```
     *
     * Over Bluetooth only. The phone keeps the network and the endpoint together, per head unit, and on a later
     * visit to that network dials the endpoint without Bluetooth; see [encodeVersionRequest].
     */
    fun encodeSetupInfo(
        token: ByteArray, ip: String, port: Int, ssid: String, key: String, bssid: String, securityMode: Int,
        channelsMhz: List<Int> = emptyList(),
    ): ByteArray {
        val tcp = ByteArrayOutputStream()
        val address = ip.toByteArray(Charsets.UTF_8)
        writeTag(tcp, FIELD_TCP_IP, WIRE_LENGTH_DELIMITED)
        writeVarint(tcp, address.size)
        tcp.write(address)
        writeVarintField(tcp, FIELD_TCP_PORT, port)

        val ap = ByteArrayOutputStream()
        writeStringField(ap, 1, ssid)
        writeStringField(ap, 2, key)
        writeStringField(ap, 3, bssid)
        writeVarintField(ap, 4, securityMode)
        for (channel in channelsMhz) writeVarintField(ap, 6, channel)

        val out = ByteArrayOutputStream()
        writeVarintField(out, FIELD_MAJOR, SETUP_MAJOR)
        writeVarintField(out, FIELD_MINOR, SETUP_MINOR)
        writeTag(out, 3, WIRE_LENGTH_DELIMITED)
        writeVarint(out, token.size)
        out.write(token)
        writeTag(out, 4, WIRE_LENGTH_DELIMITED)
        writeVarint(out, tcp.size())
        out.write(tcp.toByteArray())
        writeTag(out, 5, WIRE_LENGTH_DELIMITED)
        writeVarint(out, ap.size())
        out.write(ap.toByteArray())
        return out.toByteArray()
    }

    /**
     * The payload of a head unit's Type 7 `WifiStartResponse`, the answer to a start request the phone
     * sent itself (prototux/AARE, docs/research/gearhead-notes.md §3.2):
     *
     * ```
     * WifiStartResponse { string ip_address = 1; int32 port = 2; int32 status = 3; }
     * ```
     *
     * Built by hand because `wireless.proto` models the phone's acknowledgement, which has no port.
     *
     * @param ip the address the phone is to dial, or null to leave it out (with a non-success status).
     * @param port the TCP port the phone is to dial, or null to leave it out.
     * @param status a [WppStatus]; negative values are encoded as protobuf int32 (ten bytes).
     */
    fun encodeStartResponse(ip: String?, port: Int?, status: Int): ByteArray {
        val out = ByteArrayOutputStream()
        if (ip != null) writeStringField(out, 1, ip)
        if (port != null) writeVarintField(out, 2, port)
        writeTag(out, 3, WIRE_VARINT)
        writeInt32(out, status)
        return out.toByteArray()
    }

    private fun writeStringField(out: ByteArrayOutputStream, field: Int, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeTag(out, field, WIRE_LENGTH_DELIMITED)
        writeVarint(out, bytes.size)
        out.write(bytes)
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

    /** An int32 of either sign as protobuf encodes it: a negative one is sign-extended to 64 bits. */
    private fun writeInt32(out: ByteArrayOutputStream, value: Int) {
        if (value >= 0) return writeVarint(out, value)
        var v = value.toLong()
        while (v and 0x7FL.inv() != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }
}
