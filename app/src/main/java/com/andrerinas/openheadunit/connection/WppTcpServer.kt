package com.andrerinas.openheadunit.connection

import android.content.Context
import android.os.SystemClock
import com.andrerinas.openheadunit.aap.WppAction
import com.andrerinas.openheadunit.aap.WppEvent
import com.andrerinas.openheadunit.aap.WppFraming
import com.andrerinas.openheadunit.aap.WppHandshakeSession
import com.andrerinas.openheadunit.aap.WppMessageType
import com.andrerinas.openheadunit.aap.WppProjectionInfo
import com.andrerinas.openheadunit.aap.protocol.proto.Wireless
import com.andrerinas.openheadunit.ssl.ConscryptInitializer
import com.andrerinas.openheadunit.ssl.NoCheckTrustManager
import com.andrerinas.openheadunit.ssl.SingleKeyKeyManager
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * The head unit's side of "WPP over TCP": the phone dials us over the Wi-Fi it is already on, and the
 * same exchange that normally runs over Bluetooth runs over a TLS tunnel instead.
 *
 * Read from Android Auto 17.9, not from a specification (see [WppProjectionInfo]). On a phone that has
 * been told our address and port, the phone:
 *  - opens a TCP connection to us (we are the server);
 *  - wraps it in TLS as the **server**, asking for a client certificate, so this side is the TLS client
 *    and presents the head unit certificate every Android Auto session already uses;
 *  - then speaks the Wi-Fi projection protocol over it, with the framing used over Bluetooth.
 *
 * The exchange reuses [WppHandshakeSession]: version request, start request pointing the phone at the
 * projection port, and an answer to a credentials request if the phone still asks. The projection
 * session itself arrives on the ordinary wireless port, exactly as after a Bluetooth handshake.
 *
 * Anything that goes wrong here costs nothing but a log line: the phone falls back to Bluetooth, which is
 * why the Bluetooth path is left as it was.
 */
class WppTcpServer(
    private val context: Context,
    private val commManager: CommManager,
    val port: Int,
    val bindHost: String?,
    private val mainPort: () -> Int,
    private val credentials: () -> Credentials?,
    /** Gets the projection listener ready on the address the phone dialed, before it is sent there. */
    private val prepareMainServer: suspend (String) -> Unit,
) {
    /** The network credentials to give a phone that still asks for them. */
    data class Credentials(val ssid: String, val psk: String, val bssid: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var acceptJob: Job? = null
    @Volatile private var serverSocket: ServerSocket? = null
    private val busy = AtomicBoolean(false)
    private val held = java.util.concurrent.atomic.AtomicInteger(0)

    @Volatile var isListening = false
        private set

    /** Whether a phone is in the middle of its WPP exchange. Restarting this server then cuts it off. */
    val isExchanging: Boolean get() = busy.get()

    fun start() {
        if (acceptJob != null) return
        acceptJob = scope.launch { acceptLoop() }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        isListening = false
        scope.cancel()
        record("stopped")
    }

    private suspend fun acceptLoop() {
        val server = try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(bindHost?.let { InetAddress.getByName(it) }, port))
            }
        } catch (e: Exception) {
            record("could not listen on ${bindHost ?: "0.0.0.0"}:$port (${e.javaClass.simpleName}: ${e.message})")
            AppLog.w("WppTcp: $lastEvent")
            return
        }
        serverSocket = server
        isListening = true
        record("listening on ${bindHost ?: "0.0.0.0"}:$port")
        AppLog.i("WppTcp: $lastEvent")
        try {
            while (scope.isActive) {
                val socket = server.accept()
                scope.launch { serve(socket) }
            }
        } catch (e: Exception) {
            if (scope.isActive) AppLog.d("WppTcp: accept ended (${e.message})")
        } finally {
            isListening = false
            try { server.close() } catch (_: Exception) {}
        }
    }

    private suspend fun serve(raw: Socket) {
        val peer = raw.inetAddress?.hostAddress ?: "?"
        if (commManager.isBusy) {
            holdIdle(raw, peer)
            return
        }
        if (!busy.compareAndSet(false, true)) {
            AppLog.i("WppTcp: $peer connected while another WPP exchange is running; closing it.")
            try { raw.close() } catch (_: Exception) {}
            return
        }
        var ssl: SSLSocket? = null
        try {
            AppLog.i("WppTcp: $peer connected to $port. Starting the TLS handshake as client.")
            val now = android.os.SystemClock.elapsedRealtime()
            val userAsked = com.andrerinas.openheadunit.aap.AaStatusBus.manualStartAt.let { it != 0L && now - it < 120_000L }
            if (!userAsked && !com.andrerinas.openheadunit.App.provide(context).settings.openOnPhoneStart) {
                AppLog.i("WppTcp: the phone started this session by itself — keeping it off the screen.")
                com.andrerinas.openheadunit.aap.AaStatusBus.behindUntil = now + 90_000L
            }
            record("$peer connected, TLS handshake")
            raw.tcpNoDelay = true
            ssl = (sslContext().socketFactory.createSocket(raw, peer, raw.port, true) as SSLSocket).apply {
                useClientMode = true
                soTimeout = HANDSHAKE_TIMEOUT_MS
                startHandshake()
                soTimeout = 0
            }
            AppLog.i("WppTcp: TLS handshake with $peer done (${ssl.session.protocol}, ${ssl.session.cipherSuite}).")
            record("$peer TLS ok")
            exchange(ssl, raw.localAddress.hostAddress ?: bindHost ?: "")
        } catch (e: Exception) {
            AppLog.w("WppTcp: exchange with $peer failed: ${e.javaClass.simpleName}: ${e.message}")
            record("$peer failed: ${e.javaClass.simpleName}: ${e.message}")
        } finally {
            try { ssl?.close() } catch (_: Exception) {}
            try { raw.close() } catch (_: Exception) {}
            busy.set(false)
        }
    }

    /**
     * A phone dialing while a session is live is Gearhead restarting its Wi-Fi projection setup (it does so
     * when it sees the car's Bluetooth profiles change). Closing the connection at once makes it restart again
     * straight away, some thirty times a second, and a session the phone is churning its network requests under
     * did not survive it. The Urban head unit takes every connection through to a "projection already started"
     * answer instead. Here the connection is just held open and silent, so the phone waits out its own first
     * message timeout and the retries slow to one every few seconds. A few at once at most.
     */
    private fun holdIdle(raw: Socket, peer: String) {
        if (held.incrementAndGet() > MAX_HELD) {
            held.decrementAndGet()
            try { raw.close() } catch (_: Exception) {}
            return
        }
        try {
            AppLog.d("WppTcp: $peer connected while a session is live; holding it idle.")
            raw.soTimeout = HOLD_MS
            // Returns on the phone closing, on the timeout, or on any bytes it sends (ignored).
            while (raw.getInputStream().read() >= 0) { /* ignore */ }
        } catch (_: Exception) {
        } finally {
            held.decrementAndGet()
            try { raw.close() } catch (_: Exception) {}
        }
    }

    private data class Frame(val type: Int, val payload: ByteArray)

    private suspend fun exchange(ssl: SSLSocket, localIp: String) {
        val session = WppHandshakeSession(versionExchangeEnabled = true)
        val input = DataInputStream(ssl.inputStream)
        val output = ssl.outputStream
        val inbound = Channel<Frame>(Channel.UNLIMITED)
        val reader = scope.launch {
            try {
                while (isActive) inbound.send(readFrame(input))
            } catch (e: Exception) {
                AppLog.d("WppTcp: reader ended (${e.javaClass.simpleName}: ${e.message})")
            } finally {
                inbound.close()
            }
        }

        var stageEnteredAt = SystemClock.elapsedRealtime()
        var sessionSeen = false
        var messages = 0

        fun send(type: Int, payload: ByteArray) {
            output.write(WppFraming.encodeFrame(payload, type))
            output.flush()
            AppLog.i("WppTcp: [TX] Type $type (${payload.size} bytes)")
        }

        fun run(action: WppAction, source: Frame?) {
            when (action) {
                WppAction.SendVersionRequest -> {
                    send(
                        WppMessageType.VERSION_REQUEST,
                        WppProjectionInfo.encodeVersionRequest(
                            WppProjectionInfo.TCP_MAJOR, WppProjectionInfo.TCP_MINOR, localIp, port
                        )
                    )
                }
                WppAction.SendStartRequest -> {
                    val request = Wireless.WifiStartRequest.newBuilder()
                        .setIpAddress(localIp)
                        .setPort(mainPort())
                        .setStatus(0)
                        .build()
                    send(WppMessageType.START_REQUEST, request.toByteArray())
                    record("start request sent ($localIp:${mainPort()})")
                }
                WppAction.SendInfoResponse -> {
                    val creds = credentials()
                    if (creds == null) {
                        AppLog.w("WppTcp: the phone asked for credentials and there are none to give.")
                    } else {
                        val response = Wireless.WifiInfoResponse.newBuilder()
                            .setSsid(creds.ssid)
                            .setKey(creds.psk)
                            .setSecurityMode(Wireless.SecurityMode.WPA2_PERSONAL)
                            .setAccessPointType(Wireless.AccessPointType.DYNAMIC)
                            .setBssid(creds.bssid)
                            .build()
                        send(WppMessageType.INFO_RESPONSE, response.toByteArray())
                    }
                }
                WppAction.SendPingResponse -> send(WppMessageType.PING_RESPONSE, source?.payload ?: ByteArray(0))
                WppAction.ExtendSettle -> {}
                WppAction.CompleteSuccess -> {}
                WppAction.ResumePoke -> {}
                is WppAction.Fail -> if (!sessionSeen) {
                    AppLog.w("WppTcp: exchange ended without a session: ${action.reason}")
                    record("no session: ${action.reason}")
                }
            }
        }

        fun feed(event: WppEvent, source: Frame? = null) {
            val before = session.stage
            for (action in session.on(event)) run(action, source)
            if (session.stage != before) {
                stageEnteredAt = SystemClock.elapsedRealtime()
                AppLog.d("WppTcp: stage $before -> ${session.stage}")
            }
        }

        try {
            // The phone is about to be sent to the projection port: it has to be listening on this address,
            // which a Bluetooth handshake would have arranged and this exchange has not.
            try {
                prepareMainServer(localIp)
            } catch (e: Exception) {
                AppLog.w("WppTcp: could not prepare the projection listener on $localIp: ${e.message}")
            }
            feed(WppEvent.SocketReady)
            // The address is the socket's own: there is no credentials step to wait for over TCP.
            feed(WppEvent.CredentialsReady)

            val startedAt = SystemClock.elapsedRealtime()
            var readerClosed = false
            while (!session.isTerminal() || (sessionSeen && commManager.isConnected)) {
                val result = inbound.tryReceive()
                val frame = result.getOrNull()
                if (frame != null) {
                    messages++
                    describe(frame)
                    // A ping is answered whatever the exchange is doing, including after it has ended.
                    if (frame.type == WppMessageType.PING_REQUEST) run(WppAction.SendPingResponse, frame)
                    else feed(WppEvent.MessageReceived(frame.type, statusOf(frame)), frame)
                    continue
                }
                if (result.isClosed) { readerClosed = true; break }
                if (commManager.isConnected) {
                    if (!sessionSeen) {
                        sessionSeen = true
                        AppLog.i("WppTcp: the projection session reached us.")
                        record("session up")
                        feed(WppEvent.TcpSessionUp)
                    }
                } else if (sessionSeen) {
                    break // the session ended
                }
                val timeout = session.currentStageTimeoutMs()
                if (timeout != null && SystemClock.elapsedRealtime() - stageEnteredAt >= timeout) {
                    feed(if (session.stage == com.andrerinas.openheadunit.aap.WppStage.SETTLING) WppEvent.SettleTimeout else WppEvent.StageTimeout)
                }
                if (!sessionSeen && SystemClock.elapsedRealtime() - startedAt > OVERALL_TIMEOUT_MS) {
                    AppLog.w("WppTcp: no projection session within ${OVERALL_TIMEOUT_MS / 1000}s of the connection.")
                    break
                }
                delay(25)
            }
            AppLog.i(
                "WppTcp: exchange over (messages from the phone: $messages, session seen: $sessionSeen, " +
                    "phone closed: $readerClosed)."
            )
            if (!sessionSeen) record("ended without a session ($messages messages from the phone)")
        } finally {
            reader.cancel()
            inbound.close()
        }
    }

    private fun readFrame(input: DataInputStream): Frame {
        val header = ByteArray(WppFraming.HEADER_SIZE)
        input.readFully(header)
        val size = WppFraming.decodePayloadSize(header)
        val type = WppFraming.decodeType(header)
        val payload = ByteArray(size)
        if (size > 0) input.readFully(payload)
        return Frame(type, payload)
    }

    private fun statusOf(frame: Frame): Int? = try {
        when (frame.type) {
            WppMessageType.VERSION_RESPONSE ->
                Wireless.WifiVersionResponse.parseFrom(frame.payload).let { if (it.hasStatus()) it.status else null }
            WppMessageType.CONNECT_STATUS ->
                Wireless.WifiConnectStatus.parseFrom(frame.payload).let { if (it.hasStatus()) it.status else null }
            WppMessageType.START_RESPONSE ->
                Wireless.WifiStartResponse.parseFrom(frame.payload).let { if (it.hasStatus()) it.status else null }
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    private fun describe(frame: Frame) {
        try {
            when (frame.type) {
                WppMessageType.VERSION_RESPONSE -> {
                    val v = Wireless.WifiVersionResponse.parseFrom(frame.payload)
                    AppLog.i("WppTcp: [RX] WifiVersionResponse v${v.major}.${v.minor} status=${if (v.hasStatus()) v.status else "-"}")
                }
                WppMessageType.CONNECT_STATUS -> {
                    val s = Wireless.WifiConnectStatus.parseFrom(frame.payload)
                    AppLog.i("WppTcp: [RX] WifiConnectStatus status=${if (s.hasStatus()) s.status else "-"}")
                }
                WppMessageType.START_RESPONSE -> {
                    val r = Wireless.WifiStartResponse.parseFrom(frame.payload)
                    AppLog.i("WppTcp: [RX] WifiStartResponse ip=${r.ipAddress} status=${if (r.hasStatus()) r.status else "-"}")
                }
                else -> AppLog.i("WppTcp: [RX] Type ${frame.type} (${frame.payload.size} bytes)")
            }
        } catch (e: Exception) {
            AppLog.i("WppTcp: [RX] Type ${frame.type} (${frame.payload.size} bytes, not parsed: ${e.message})")
        }
    }

    private fun sslContext(): SSLContext {
        val provider = ConscryptInitializer.getProviderName()
        val context = try {
            if (provider != null) SSLContext.getInstance("TLS", provider) else SSLContext.getInstance("TLS")
        } catch (e: Exception) {
            SSLContext.getInstance("TLS")
        }
        return context.apply { init(arrayOf(SingleKeyKeyManager(this@WppTcpServer.context)), arrayOf(NoCheckTrustManager()), null) }
    }

    companion object {
        private const val HANDSHAKE_TIMEOUT_MS = 15_000
        private const val HOLD_MS = 20_000
        private const val MAX_HELD = 4

        /** How long a connection may last without a projection session reaching us. */
        private const val OVERALL_TIMEOUT_MS = 60_000L

        /** The last thing that happened, for the diagnostic report. */
        @Volatile var lastEvent: String = "not started"
            private set

        private fun record(text: String) {
            lastEvent = text
        }
    }
}
