package com.andrerinas.openheadunit.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdasPropertiesTest {

    private val all = AdasProperties.groups.flatMap { it.properties }

    @Test
    fun everyGroupHasProperties() {
        for (group in AdasProperties.groups) assertTrue(group.title, group.properties.isNotEmpty())
    }

    @Test
    fun noPropertyIsListedTwice() {
        assertEquals(all.size, all.map { it.second }.toSet().size)
        assertEquals(all.size, all.map { it.first }.toSet().size)
    }

    @Test
    fun everyIdIsOfATypeTheReaderHandles() {
        val handled = setOf(AdasProperties.TYPE_BOOLEAN, AdasProperties.TYPE_INT, AdasProperties.TYPE_FLOAT)
        for ((name, id) in all) assertTrue(name, (id and AdasProperties.TYPE_MASK) in handled)
    }

    @Test
    fun theSpeedLimitPropertiesTheAdapterWritesAreThere() {
        val ids = all.map { it.second }.toSet()
        // Value, status, unit, type and road type: what isa* calls end up in.
        for (id in listOf(559980933, 557883779, 557883780, 557883844, 557883778)) assertTrue("$id", id in ids)
    }
}
