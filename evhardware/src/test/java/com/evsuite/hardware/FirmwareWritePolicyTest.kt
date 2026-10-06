package com.evsuite.hardware

import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareWritePolicyTest {

    @Test
    fun `known generations are recognized`() {
        FirmwareInfo.Gen.values()
            .filterNot { it == FirmwareInfo.Gen.UNKNOWN }
            .forEach { generation ->
                assertEquals(generation, FirmwareInfo.generationOf("${generation.name}-build"))
            }
    }

    @Test
    fun `missing and unrecognized firmware stay unknown`() {
        assertEquals(FirmwareInfo.Gen.UNKNOWN, FirmwareInfo.generationOf(null))
        assertEquals(FirmwareInfo.Gen.UNKNOWN, FirmwareInfo.generationOf(""))
        assertEquals(FirmwareInfo.Gen.UNKNOWN, FirmwareInfo.generationOf("SWI999-test"))
        assertEquals(FirmwareInfo.Gen.UNKNOWN, FirmwareInfo.generationOf("DBI999-test"))
    }

    @Test
    fun `regional DBI prefixes map onto the matching SWI generation`() {
        assertEquals(FirmwareInfo.Gen.SWI68, FirmwareInfo.generationOf("DBI68-12345-xxx"))
        assertEquals(FirmwareInfo.Gen.SWI69, FirmwareInfo.generationOf("DBI69-99999"))
        assertEquals(FirmwareInfo.Gen.SWI131, FirmwareInfo.generationOf("dbi131-build"))
        assertEquals(FirmwareInfo.Gen.SWI132, FirmwareInfo.generationOf("DBI132-build"))
        assertEquals(FirmwareInfo.Gen.SWI133, FirmwareInfo.generationOf("DBI133-build"))
        assertEquals(FirmwareInfo.Gen.SWI165, FirmwareInfo.generationOf("DBI165-build"))
    }
}
