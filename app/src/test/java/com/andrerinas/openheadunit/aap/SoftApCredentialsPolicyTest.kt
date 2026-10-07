package com.andrerinas.openheadunit.aap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftApCredentialsPolicyTest {

    private fun decide(
        manualSsid: String = "",
        manualPassphrase: String = "",
        systemConfig: SoftApCredentials? = null,
        ip: String? = "192.168.43.1"
    ) = SoftApCredentialsPolicy.decide(manualSsid, manualPassphrase, systemConfig, ip)

    @Test
    fun `an interface with no address of its own is not an access point yet`() {
        assertEquals(SoftApCredentialsAttempt.NO_AP_YET, decide(ip = null))
        assertEquals(SoftApCredentialsAttempt.NO_AP_YET, decide(ip = ""))
    }

    @Test
    fun `the device naming its own access point is enough`() {
        assertEquals(
            SoftApCredentialsAttempt.PUBLISHED,
            decide(systemConfig = SoftApCredentials("AndroidAP", "swordfish"))
        )
    }

    @Test
    fun `a device that will not name its access point is a dead end, not a wait`() {
        // The distinction this policy exists for. There is an access point — the interface has an
        // address — and the read that would name it has failed. It will fail again next second, so
        // returning NO_AP_YET here is what made the resolve loop spend its whole budget polling and
        // switch on a hotspot that was already up.
        assertEquals(SoftApCredentialsAttempt.CONFIG_UNREADABLE, decide(systemConfig = null))
    }

    @Test
    fun `the manual override is the way through on a device that hides its configuration`() {
        // What the on-screen instruction tells the user to do, so it has to be the case that does
        // it: no system configuration available, and the route runs anyway.
        assertEquals(
            SoftApCredentialsAttempt.PUBLISHED,
            decide(manualSsid = "OHU-TEST", manualPassphrase = "testtest1234", systemConfig = null)
        )
        assertEquals(
            SoftApCredentials("OHU-TEST", "testtest1234"),
            SoftApCredentialsPolicy.resolve("OHU-TEST", "testtest1234", null)
        )
    }

    @Test
    fun `a named network with no password goes out open when the live SoftAP is unreadable`() {
        // Locked-down unit path from CONNECTION_SETUP: manual SSID, blank password, no system
        // config. The empty passphrase is the honest answer; SoftApCredentialsProvider logs it.
        assertEquals(
            SoftApCredentialsAttempt.PUBLISHED,
            decide(manualSsid = "OHU-TEST", manualPassphrase = "", systemConfig = null)
        )
        assertEquals(
            SoftApCredentials("OHU-TEST", ""),
            SoftApCredentialsPolicy.resolve("OHU-TEST", "", null)
        )
    }

    @Test
    fun `live SoftAP name and passphrase win over stale settings`() {
        // Vehicle UI can rename/re-key the hotspot while settings keep a stale copy. Platform-
        // signed MG4 can read the live SoftAP — advertise that, not the override.
        assertEquals(
            SoftApCredentials("AndroidAP", "fromTheDevice"),
            SoftApCredentialsPolicy.resolve(
                "OHU-TEST",
                "staleSavedPassword",
                SoftApCredentials("AndroidAP", "fromTheDevice")
            )
        )
    }

    @Test
    fun `manual passphrase fills in when the live SoftAP has none`() {
        assertEquals(
            SoftApCredentials("AndroidAP", "typedByHand"),
            SoftApCredentialsPolicy.resolve("", "typedByHand", SoftApCredentials("AndroidAP", ""))
        )
        assertEquals(
            SoftApCredentials("OHU-TEST", "typedByHand"),
            SoftApCredentialsPolicy.resolve("OHU-TEST", "typedByHand", null)
        )
    }

    @Test
    fun `manual SSID is only used when the live SoftAP name is missing`() {
        assertEquals(
            SoftApCredentials("OHU-TEST", "fromTheDevice"),
            SoftApCredentialsPolicy.resolve("OHU-TEST", "", SoftApCredentials("", "fromTheDevice"))
        )
    }

    @Test
    fun `live SoftAP SSID conflicting with the saved name is detected but not a refuse`() {
        // resolve() prefers live; callers may warn about the stale override.
        assertTrue(
            SoftApCredentialsPolicy.liveSsidConflicts(
                "SavedName",
                SoftApCredentials("LiveCarHotspot", "x")
            )
        )
        assertEquals(
            SoftApCredentials("LiveCarHotspot", "x"),
            SoftApCredentialsPolicy.resolve(
                "SavedName",
                "stale",
                SoftApCredentials("LiveCarHotspot", "x")
            )
        )
        assertFalse(
            SoftApCredentialsPolicy.liveSsidConflicts(
                "LiveCarHotspot",
                SoftApCredentials("LiveCarHotspot", "x")
            )
        )
        assertFalse(
            SoftApCredentialsPolicy.liveSsidConflicts("", SoftApCredentials("LiveCarHotspot", "x"))
        )
        assertFalse(SoftApCredentialsPolicy.liveSsidConflicts("SavedName", null))
    }

    @Test
    fun `a system configuration with an empty name is no configuration at all`() {
        // getSoftApConfiguration() can return an object whose SSID is empty rather than refusing
        // outright; that is the same dead end and must read as one.
        assertEquals(
            SoftApCredentialsAttempt.CONFIG_UNREADABLE,
            decide(systemConfig = SoftApCredentials("", ""))
        )
    }

    @Test
    fun `the address is checked before the name, so a missing one is never reported as unreadable`() {
        // Ordering matters for the message the user sees: "this device will not let apps read its
        // hotspot name" is wrong and unactionable when the truth is that no access point is up.
        assertEquals(SoftApCredentialsAttempt.NO_AP_YET, decide(systemConfig = null, ip = null))
    }
}
