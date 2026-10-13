package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkLossTeardownPolicyTest {

    @Test
    fun `a device shutdown takes every route down, so every route closes first`() {
        for (mode in 1..3) {
            for (strategy in 0..4) {
                assertTrue(
                    "mode=$mode strategy=$strategy",
                    LinkLossTeardownPolicy.shouldTearDown(LinkLossTrigger.DEVICE_SHUTDOWN, mode, strategy)
                )
            }
        }
    }

    @Test
    fun `station wifi going down closes the routes that ride it`() {
        // Mode 1 (NSD) and mode 2 strategies 0 and 3 all reach the phone over station WiFi.
        assertTrue(LinkLossTeardownPolicy.shouldTearDown(LinkLossTrigger.WIFI_STATION_DISABLING, 1, 0))
        assertTrue(LinkLossTeardownPolicy.shouldTearDown(LinkLossTrigger.WIFI_STATION_DISABLING, 2, 0))
        assertTrue(LinkLossTeardownPolicy.shouldTearDown(LinkLossTrigger.WIFI_STATION_DISABLING, 2, 3))
    }

    @Test
    fun `station wifi going down leaves a session on our own access point alone`() {
        // The soft AP is a separate interface and survives the station toggle on several chipsets.
        // Tearing this down would cost a 45-90s reconnect to prevent nothing.
        assertFalse(LinkLossTeardownPolicy.shouldTearDown(LinkLossTrigger.WIFI_STATION_DISABLING, 3, 0))
        // Mode 2 strategy 4 is the head unit hotspot: same reasoning, different route.
        assertFalse(LinkLossTeardownPolicy.shouldTearDown(LinkLossTrigger.WIFI_STATION_DISABLING, 2, 4))
    }

    @Test
    fun `a usb session is left alone by a wifi toggle, whatever wireless mode is stored`() {
        // The wireless mode is a setting, not a description of the session. A USB drive with mode 1
        // stored would otherwise be ended by the user switching WiFi off.
        for (mode in 1..3) {
            for (strategy in 0..4) {
                assertFalse(
                    "mode=$mode strategy=$strategy",
                    LinkLossTeardownPolicy.shouldTearDown(
                        LinkLossTrigger.WIFI_STATION_DISABLING, mode, strategy,
                        sessionIsWireless = false
                    )
                )
            }
        }
    }

    @Test
    fun `a device shutdown still closes a usb session`() {
        // Nothing survives the shutdown, and the phone's head unit server is wedged just as hard by
        // a USB session that vanishes as by a wireless one.
        assertTrue(
            LinkLossTeardownPolicy.shouldTearDown(
                LinkLossTrigger.DEVICE_SHUTDOWN, 1, 0, sessionIsWireless = false
            )
        )
    }

    @Test
    fun `the wifi answer is the exact complement of the routes that own their own network`() {
        for (mode in 1..3) {
            for (strategy in 0..4) {
                val tearsDown = LinkLossTeardownPolicy.shouldTearDown(
                    LinkLossTrigger.WIFI_STATION_DISABLING, mode, strategy, sessionIsWireless = true
                )
                assertTrue(
                    "mode=$mode strategy=$strategy",
                    tearsDown != UserExitHotspotPolicy.usesHeadUnitHotspot(mode, strategy)
                )
            }
        }
    }
}
