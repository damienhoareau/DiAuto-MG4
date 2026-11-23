package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.proto.Wireless
import com.google.protobuf.UnknownFieldSet

/**
 * Who the phone says it is, from its Type 5 `WifiVersionResponse`:
 *
 * ```
 * WifiVersionResponse {
 *   string device_serial = 3;                     // the serial it also shows over USB
 *   MobileDeviceIdentity identity = 6 {           // "PhoneInfo"
 *     string instance_id = 1;                     // random, kept by Android Auto across sessions
 *     string bluetooth_name = 3;
 *     string bluetooth_address = 4;
 *   }
 * }
 * ```
 *
 * Field names from LIVI (livi-aa-proto `wireless_setup.proto`) and prototux/AARE
 * (docs/research/gearhead-notes.md §2.5 and §3.2). The phone fills field 6 only for a head unit
 * announcing version 1.6 or later, which ours does.
 *
 * Field 6 is not in `wireless.proto`, so it is read from the unknown fields rather than by
 * regenerating the Java classes. Diagnostic only: nothing branches on it yet.
 */
data class WppPhoneIdentity(
    val deviceSerial: String?,
    val instanceId: String?,
    val bluetoothName: String?,
    val bluetoothAddress: String?,
) {
    /** True when the phone said nothing about itself. */
    fun isEmpty(): Boolean =
        deviceSerial == null && instanceId == null && bluetoothName == null && bluetoothAddress == null

    /**
     * One line for the log. The serial is shortened to its last four characters: enough to tell
     * two phones apart in a report a user shares, without publishing the whole of it.
     */
    fun describe(): String = listOf(
        "instanceId=${instanceId ?: "-"}",
        "serial=${deviceSerial?.let(::masked) ?: "-"}",
        "btName=${bluetoothName ?: "-"}",
        "btAddress=${bluetoothAddress ?: "-"}",
    ).joinToString(" ")

    companion object {
        private const val FIELD_IDENTITY = 6
        private const val FIELD_INSTANCE_ID = 1
        private const val FIELD_BT_NAME = 3
        private const val FIELD_BT_ADDRESS = 4

        /** Null when [payload] is not a version response at all; empty fields when it carries none. */
        fun parse(payload: ByteArray): WppPhoneIdentity? {
            val response = try {
                Wireless.WifiVersionResponse.parseFrom(payload)
            } catch (e: Exception) {
                return null
            }
            val identity = response.unknownFields.getField(FIELD_IDENTITY).lengthDelimitedList.firstOrNull()
                ?.let { bytes -> runCatching { UnknownFieldSet.parseFrom(bytes) }.getOrNull() }
            return WppPhoneIdentity(
                deviceSerial = response.deviceSerial.takeIf { response.hasDeviceSerial() && it.isNotEmpty() },
                instanceId = identity?.string(FIELD_INSTANCE_ID),
                bluetoothName = identity?.string(FIELD_BT_NAME),
                bluetoothAddress = identity?.string(FIELD_BT_ADDRESS),
            )
        }

        private fun UnknownFieldSet.string(field: Int): String? =
            getField(field).lengthDelimitedList.firstOrNull()?.toStringUtf8()?.takeIf { it.isNotEmpty() }

        private fun masked(serial: String): String =
            if (serial.length <= 4) "****" else "…" + serial.takeLast(4)
    }
}
