package com.andrerinas.openheadunit.aap

import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.aap.protocol.proto.Control
import com.andrerinas.openheadunit.utils.AppLog

/** Call state sent by the phone on the PHONE_STATUS channel. Payload messages only (type above 31). */
class AapPhoneStatus {
    fun process(message: AapMessage): Boolean {
        if (message.channel != Channel.ID_PHONE) return false
        when (message.type) {
            MSG_PHONE_STATUS -> try {
                val status = message.parse(Control.Service.PhoneStatusService.newBuilder()).build()
                val states = status.callsList.filter { it.hasState() }.map { it.state }
                val code = AaStatusBus.callCode(states)
                if (AppLog.LOG_DEBUG) AppLog.d("AapPhoneStatus: ${status.callsCount} call(s), states=$states -> $code")
                AaStatusBus.call.value = code
            } catch (e: Exception) {
                AppLog.w("AapPhoneStatus: could not parse the phone status: ${e.message}")
            }
            MSG_PHONE_STATUS_INPUT -> Unit
            else -> return false
        }
        return true
    }

    companion object {
        const val MSG_PHONE_STATUS = 32769
        const val MSG_PHONE_STATUS_INPUT = 32770
    }
}
