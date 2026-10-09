package com.andrerinas.openheadunit.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarNetworkQuirksTest {

    private val saicScript = """
        # commentaire
        #retry_set_iptables_rule "iptables -t filter -I" "INPUT -p tcp --dport 9999 -j ACCEPT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p icmp --icmp-type echo-request -j REJECT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p udp --dport 58420 -j ACCEPT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p tcp --dport 30517 -j ACCEPT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p tcp --dport 30518 -j ACCEPT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p tcp --dport 6010 -j ACCEPT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p tcp --dport 8080 -j ACCEPT"
        retry_set_iptables_rule "iptables -t filter -I" "INPUT -p udp --dport 67 -j ACCEPT"
        retry_set_iptables_rule "iptables -A" "INPUT -j REJECT"
    """.trimIndent()

    @Test
    fun `parses the SAIC whitelist and the default reject`() {
        val fw = CarNetworkQuirks.parseFirewall(saicScript, "/system/bin/arp_update.sh")
        assertNotNull(fw)
        assertTrue(fw!!.rejectsByDefault)
        assertEquals(listOf(30517, 30518, 6010, 8080), fw.allowedTcp)
    }

    @Test
    fun `commented rules and udp or icmp rules are ignored`() {
        val fw = CarNetworkQuirks.parseFirewall(saicScript, "x")!!
        assertFalse(9999 in fw.allowedTcp)
        assertFalse(58420 in fw.allowedTcp)
        assertFalse(67 in fw.allowedTcp)
    }

    @Test
    fun `no firewall rules gives null`() {
        assertNull(CarNetworkQuirks.parseFirewall("echo hello\nip link set eth0 up", "x"))
    }

    @Test
    fun `restrictive firewall picks 30518 first`() {
        val fw = CarNetworkQuirks.parseFirewall(saicScript, "x")
        val (port, _) = CarNetworkQuirks.pickPort(fw, isSaic = true) { true }
        assertEquals(30518, port)
    }

    @Test
    fun `restrictive firewall skips a port that cannot be bound`() {
        val fw = CarNetworkQuirks.parseFirewall(saicScript, "x")
        val (port, _) = CarNetworkQuirks.pickPort(fw, isSaic = true) { it != 30518 }
        assertEquals(30517, port)
    }

    @Test
    fun `restrictive firewall without a preferred port uses an allowed one`() {
        val fw = CarNetworkQuirks.Firewall("x", true, listOf(4000, 5000))
        val (port, _) = CarNetworkQuirks.pickPort(fw, isSaic = true) { true }
        assertEquals(4000, port)
    }

    @Test
    fun `all candidates busy still returns an allowed port`() {
        val fw = CarNetworkQuirks.parseFirewall(saicScript, "x")
        val (port, _) = CarNetworkQuirks.pickPort(fw, isSaic = true) { false }
        assertEquals(30518, port)
    }

    @Test
    fun `unreadable script on a SAIC device falls back to 30518`() {
        val (port, _) = CarNetworkQuirks.pickPort(null, isSaic = true) { true }
        assertEquals(CarNetworkQuirks.SAIC_FALLBACK_PORT, port)
    }

    @Test
    fun `other devices keep the standard port`() {
        val (port, _) = CarNetworkQuirks.pickPort(null, isSaic = false) { true }
        assertEquals(CarNetworkQuirks.STANDARD_PORT, port)
    }

    @Test
    fun `a firewall without default reject keeps the standard port`() {
        val fw = CarNetworkQuirks.Firewall("x", false, listOf(80))
        val (port, _) = CarNetworkQuirks.pickPort(fw, isSaic = true) { true }
        assertEquals(CarNetworkQuirks.STANDARD_PORT, port)
    }
}
