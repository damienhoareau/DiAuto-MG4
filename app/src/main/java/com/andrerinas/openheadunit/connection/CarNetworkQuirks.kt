package com.andrerinas.openheadunit.connection

import android.os.Build
import com.andrerinas.openheadunit.utils.AppLog
import java.io.File
import java.net.ServerSocket

/**
 * Particularites reseau du firmware MG4/SAIC, detectees sur la voiture elle-meme.
 *
 * Le script SAIC `arp_update.sh` (section "Disable arp" + pare-feu) :
 *  - fixe `arp_ignore=8` : la voiture ne repond a aucune requete ARP, meme sur le hotspot ;
 *  - pose `iptables INPUT ... REJECT` apres une liste blanche de ports (TCP 6010, 8080, 30517, 30518...).
 *
 * Plutot qu'une table de versions logicielles (le firmware n'expose aucun numero « SWI » lisible),
 * on lit les regles reelles : le port TCP annonce au telephone est choisi parmi les ports autorises.
 */
object CarNetworkQuirks {
    private const val TAG = "CarNetworkQuirks"

    /** Port habituel d'Android Auto sans fil, utilise quand aucun pare-feu restrictif n'est detecte. */
    const val STANDARD_PORT = 5288

    /** Port autorise par tous les firmwares SAIC connus ; repli si le script n'est pas lisible. */
    const val SAIC_FALLBACK_PORT = 30518

    private val SCRIPT_PATHS = listOf("/system/bin/arp_update.sh", "/vendor/bin/arp_update.sh")

    /** Ordre de preference parmi les ports autorises : les moins susceptibles d'etre deja pris d'abord. */
    private val PREFERRED = listOf(30518, 30517, 8080, 6010)

    private val ACCEPT_TCP = Regex("""INPUT\s+-p\s+tcp\s+--dport\s+(\d+)\s+-j\s+ACCEPT""")
    private val DEFAULT_REJECT = Regex("""INPUT\s+-j\s+(REJECT|DROP)\b""")

    data class Firewall(val source: String, val rejectsByDefault: Boolean, val allowedTcp: List<Int>)

    /** Extrait la liste blanche TCP et la presence d'un rejet par defaut. Ignore les lignes commentees. */
    fun parseFirewall(text: String, source: String): Firewall? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
        val allowed = lines.flatMap { ACCEPT_TCP.findAll(it).map { m -> m.groupValues[1].toInt() }.toList() }.distinct()
        val rejects = lines.any { DEFAULT_REJECT.containsMatchIn(it) }
        if (allowed.isEmpty() && !rejects) return null
        return Firewall(source, rejects, allowed)
    }

    /** Choisit le port a annoncer. [isFree] teste si un port peut etre lie ; renvoie (port, raison). */
    fun pickPort(firewall: Firewall?, isSaic: Boolean, isFree: (Int) -> Boolean): Pair<Int, String> {
        if (firewall != null && firewall.rejectsByDefault && firewall.allowedTcp.isNotEmpty()) {
            val candidates = PREFERRED.filter { it in firewall.allowedTcp } +
                firewall.allowedTcp.filter { it !in PREFERRED }
            val chosen = candidates.firstOrNull { isFree(it) } ?: candidates.first()
            return chosen to "pare-feu restrictif lu dans ${firewall.source} (TCP autorises: ${firewall.allowedTcp.joinToString(",")})"
        }
        if (firewall != null) {
            return STANDARD_PORT to "pare-feu lu dans ${firewall.source} sans rejet par defaut"
        }
        if (isSaic) {
            return SAIC_FALLBACK_PORT to "appareil SAIC, script de pare-feu illisible : repli sur un port autorise sur tous les firmwares connus"
        }
        return STANDARD_PORT to "pas de pare-feu SAIC detecte : port standard"
    }

    @Volatile private var cachedPort: Int? = null
    @Volatile private var cachedWhy: String = ""
    @Volatile private var cachedArpNudge: Boolean? = null

    private fun isSaicDevice(): Boolean =
        Build.MODEL.equals("SAIC", ignoreCase = true) ||
            Build.DEVICE.contains("saic", ignoreCase = true) ||
            Build.MANUFACTURER.equals("AUTUS", ignoreCase = true)

    private fun readFirewall(): Firewall? {
        for (path in SCRIPT_PATHS) {
            try {
                val f = File(path)
                if (f.isFile && f.canRead()) {
                    parseFirewall(f.readText(), path)?.let { return it }
                }
            } catch (e: Exception) {
                AppLog.d("$TAG: lecture de $path impossible (${e.javaClass.simpleName}: ${e.message})")
            }
        }
        return null
    }

    private fun canBind(port: Int): Boolean = try {
        ServerSocket().use { s ->
            s.reuseAddress = true
            s.bind(java.net.InetSocketAddress(port))
        }
        true
    } catch (e: Exception) {
        false
    }

    /** Port TCP sans fil choisi automatiquement (calcule une fois, puis mis en cache pour la duree du processus). */
    fun autoWirelessPort(): Int {
        cachedPort?.let { return it }
        synchronized(this) {
            cachedPort?.let { return it }
            val (port, why) = pickPort(readFirewall(), isSaicDevice(), ::canBind)
            cachedWhy = why
            cachedPort = port
            AppLog.i("$TAG: port sans fil automatique = $port ($why).")
            return port
        }
    }

    /** Explication du dernier choix automatique (pour les diagnostics). */
    fun autoPortReason(): String {
        autoWirelessPort()
        return cachedWhy
    }

    /**
     * Le reveil ARP n'est utile que si le noyau ne repond pas aux requetes ARP (arp_ignore >= 3) ;
     * valeur illisible : on l'active sur les appareils SAIC (inoffensif).
     */
    fun needsArpNudge(): Boolean {
        cachedArpNudge?.let { return it }
        val value = listOf("all", "default").mapNotNull { scope ->
            try {
                File("/proc/sys/net/ipv4/conf/$scope/arp_ignore").readText().trim().toIntOrNull()
            } catch (e: Exception) {
                null
            }
        }.maxOrNull()
        val needed = if (value != null) value >= 3 else isSaicDevice()
        cachedArpNudge = needed
        AppLog.i("$TAG: arp_ignore=${value ?: "illisible"} -> reveil ARP ${if (needed) "necessaire" else "inutile"}.")
        return needed
    }
}
