package com.andrerinas.openheadunit.connection

import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress

/**
 * MG4 : le firmware SAIC fixe `net.ipv4.conf.{all,default}.arp_ignore = 8` (script `arp_update.sh`,
 * section "Disable arp", prevu pour l'Ethernet interne). Le noyau de la voiture ne repond donc JAMAIS aux
 * requetes ARP, y compris celles du telephone sur le hotspot (ap0) : le telephone ne peut pas resoudre
 * l'adresse materielle de 192.168.x.1 et sa connexion TCP vers le port 30518 echoue en
 * `EHOSTUNREACH` (observe dans le logcat du Pixel : CAR.SETUP.WIFI, "No route to host",
 * IpReachabilityMonitor NUD_FAILED), sans qu'aucun paquet n'arrive jamais jusqu'a notre ServerSocket.
 *
 * Sans root on ne peut pas changer ce sysctl. Contournement : un hote Linux met a jour (ou debloque) une
 * entree de voisinage EXISTANTE (y compris INCOMPLETE/FAILED) quand il recoit une requete ARP diffusee dont
 * l'expediteur est cette adresse IP -- meme si la requete ne le concerne pas. On fait donc emettre au noyau de
 * la voiture des requetes ARP diffusees (sender = adresse du hotspot), en envoyant des datagrammes UDP vers des
 * adresses inutilisees du sous-reseau. Le telephone apprend ainsi l'adresse materielle de la voiture.
 *
 * N'envoie aucun contenu utile : un octet UDP vers le port 9 (discard), jamais vers un appareil reel.
 */
object ArpNudger {
    private const val TAG = "ArpNudger"
    private const val INTERVAL_MS = 150L

    @Volatile
    private var job: Job? = null

    @Volatile
    private var runningFor: String? = null

    /** Demarre (ou prolonge) le reveil ARP pour le hotspot [hostIp] pendant [durationMs]. */
    @Synchronized
    fun start(scope: CoroutineScope, hostIp: String, durationMs: Long = 60_000L) {
        if (job?.isActive == true && runningFor == hostIp) {
            // deja en cours pour ce hotspot : on repart pour une duree complete
            stop()
        } else {
            stop()
        }
        val host = try {
            InetAddress.getByName(hostIp) as? Inet4Address
        } catch (e: Exception) {
            null
        }
        if (host == null) {
            AppLog.w("$TAG: adresse hotspot invalide ($hostIp), pas de reveil ARP.")
            return
        }
        val raw = host.address
        runningFor = hostIp
        job = scope.launch(Dispatchers.IO) {
            AppLog.i("$TAG: reveil ARP actif pour ${host.hostAddress} (${durationMs / 1000}s) -- le noyau de la voiture a arp_ignore=8.")
            val deadline = System.currentTimeMillis() + durationMs
            var sent = 0
            var errors = 0
            try {
                // source = adresse du hotspot, pour que le sender-IP de l'ARP soit bien celle que le telephone cherche
                DatagramSocket(0, host).use { socket ->
                    var i = 0
                    while (isActive && System.currentTimeMillis() < deadline) {
                        // adresses .200 a .249 : jamais attribuees par le DHCP (plage basse), jamais la voiture
                        val lastOctet = 200 + (i % 50)
                        i++
                        val target = byteArrayOf(raw[0], raw[1], raw[2], lastOctet.toByte())
                        try {
                            socket.send(
                                DatagramPacket(byteArrayOf(0), 1, InetAddress.getByAddress(target), 9)
                            )
                            sent++
                        } catch (e: Exception) {
                            if (errors++ < 3) AppLog.w("$TAG: envoi UDP impossible : ${e.javaClass.simpleName}: ${e.message}")
                        }
                        delay(INTERVAL_MS)
                    }
                }
            } catch (e: Exception) {
                AppLog.w("$TAG: arret sur erreur ${e.javaClass.simpleName}: ${e.message}")
            }
            AppLog.i("$TAG: reveil ARP termine ($sent datagrammes, $errors erreurs).")
        }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        runningFor = null
    }
}
