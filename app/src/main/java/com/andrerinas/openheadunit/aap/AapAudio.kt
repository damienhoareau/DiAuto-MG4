package com.andrerinas.openheadunit.aap

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.evsuite.hardware.saic.SaicCarAudioSource
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.andrerinas.openheadunit.aap.protocol.AudioConfigs
import com.andrerinas.openheadunit.aap.protocol.Channel
import com.andrerinas.openheadunit.aap.protocol.proto.Control
import com.andrerinas.openheadunit.decoder.AudioDecoder
import com.andrerinas.openheadunit.decoder.MediaAudioBuffer
import com.andrerinas.openheadunit.utils.AppLog
import com.andrerinas.openheadunit.utils.BluetoothHelper
import com.andrerinas.openheadunit.utils.CarAudioAttributes
import com.andrerinas.openheadunit.utils.Settings

internal class AapAudio(
        private val audioDecoder: AudioDecoder,
        private val audioManager: AudioManager,
        private val settings: Settings,
        private val context: Context) {

    /**
     * MEDIA focus must be attributed to AllGo AA on eh32 so SystemUI's media card
     * leaves radio; notification/voice keep the process [audioManager].
     */
    private val mediaAudioManager: AudioManager =
        SaicCarAudioSource.audioManager(context) ?: audioManager

    private val staticAudioFocus = settings.staticAudioFocus
    private val separateAudioStreams = settings.separateAudioStreams
    private val mediaVolumeOffset = settings.mediaVolumeOffset
    private val assistantVolumeOffset = settings.assistantVolumeOffset
    private val navigationVolumeOffset = settings.navigationVolumeOffset
    private val audioLatencyMultiplier = settings.audioLatencyMultiplier
    private val useAacAudio = settings.useAacAudio
    private val audioQueueCapacity = settings.audioQueueCapacity
    private val enableAudioSink = settings.enableAudioSink
    private val attachHwDspEqualizer = settings.attachHwDspEqualizer
    private val mediaBufferMillis = settings.mediaAudioBufferMillis
    private fun playbackFocusMode() = settings.effectivePlaybackFocusMode(context)

    private var legacyFocusListener: AudioManager.OnAudioFocusChangeListener? = null

    // The four system focus requests the stock Allgo app holds, one per kind of phone request, so a
    // new request never replaces (and strands) another: media (GAIN), transient media
    // (GAIN_TRANSIENT), the assistant (GAIN_TRANSIENT while a voice session runs) and navigation
    // guidance (GAIN_TRANSIENT_MAY_DUCK). Guarded by [focusLock].
    private val focusLock = Any()
    private var mediaFocus: AudioFocusRequest? = null
    private var transMediaFocus: AudioFocusRequest? = null
    private var assistantFocus: AudioFocusRequest? = null
    private var navigationFocus: AudioFocusRequest? = null

    /** Tells the phone the system moved the media focus (set by [AapTransport]). */
    @Volatile
    var focusNotifier: ((Control.AudioFocusNotification.AudioFocusStateType) -> Unit)? = null

    /** Whether a voice session is running: the stock app leaves the navigation request alone then. */
    @Volatile
    var isAssistantActive: () -> Boolean = { false }

    /** Whether the phone currently believes the head unit holds the media focus. */
    @Volatile
    private var phoneHasGain = false

    @Volatile
    private var playbackFocusRequest: AudioFocusRequest? = null
    @Volatile
    private var legacyPlaybackFocusListener: AudioManager.OnAudioFocusChangeListener? = null

    // Dynamic-mode playback focus: some phones never send an AudioFocusRequestNotification(GAIN)
    // when media starts (the always-grant handshake makes them assume the head unit always holds
    // focus), so relying on the protocol notification alone leaves the car radio playing alongside
    // AA audio. Instead we hold system audio focus for as long as any AA audio channel is actively
    // playing and release it when the last one stops. Static mode manages focus permanently and is
    // left untouched.
    //
    // Whether we take it at all is PlaybackFocusPolicy's call: on a head unit that is also the
    // phone's Bluetooth A2DP sink, the focus grab makes the sink service AVRCP-pause that same
    // phone, silencing the stream we are playing.
    private val activeAudioChannels = mutableSetOf<Int>()
    private val playbackFocusListener = AudioManager.OnAudioFocusChangeListener {
        AppLog.i("AapAudio: playback audio focus changed: $it")
    }

    @Volatile
    private var holdingPlaybackFocus = false
    @Volatile
    private var focusAcquiredAtMs = 0L
    @Volatile
    private var selfDefeatingStops = 0
    @Volatile
    private var selfDefeatingLatched = false

    @Volatile
    private var isDucked = false
    private val handler = Handler(Looper.getMainLooper())
    private val unduckRunnable = Runnable {
        unduckMedia()
    }

    private fun duckMedia() {
        if (!isDucked) {
            val mediaTrack = audioDecoder.getTrack(Channel.ID_AUD)
            if (mediaTrack != null) {
                val duckedVolume = getMediaGain() * DUCK_VOLUME_FACTOR
                AppLog.i("Static Audio Focus: Ducking media volume to $duckedVolume")
                mediaTrack.setVolume(duckedVolume)
                isDucked = true
            }
        }
    }

    private fun unduckMedia() {
        if (isDucked) {
            val mediaTrack = audioDecoder.getTrack(Channel.ID_AUD)
            if (mediaTrack != null) {
                val originalVolume = getMediaGain()
                AppLog.i("Static Audio Focus: Restoring media volume to $originalVolume")
                mediaTrack.setVolume(originalVolume)
            }
            isDucked = false
        }
    }

    private fun getMediaGain(): Float {
        return (1.0f + (mediaVolumeOffset / 100.0f)).coerceIn(0.0f, 2.0f)
    }

    /**
     * Whether a focus request the phone asked for over the protocol should reach the system.
     *
     * [AapControl] answers every AudioFocusRequestNotification with an always-grant reply, which is
     * what keeps AA routing audio to us; separately it asks the system for the focus the phone
     * wanted, so other apps on the head unit duck. That second half is the same act that makes an
     * A2DP sink AVRCP-pause the phone we project, and it honours GAIN as a *permanent* grab, so it
     * needs the same rule as the playback path.
     *
     * RELEASE is never gated: abandoning focus must always work, or a grab from before the link
     * came up would be stranded for the rest of the session.
     *
     * The latch does not extend here — it is armed in [onAudioPlaybackStopped], which only runs
     * while the playback path holds focus. On a unit whose Bluetooth probe reads nothing, this path
     * has no automatic backstop and needs the mode set to NEVER by hand.
     */
    fun shouldHonourProtocolFocusRequest(isRelease: Boolean): Boolean {
        if (isRelease) return true

        val btMediaLinkActive = BluetoothHelper.isA2dpMediaLinkActive(context)
        val mode = playbackFocusMode()
        val honour = PlaybackFocusPolicy.shouldAcquire(
                mode = mode,
                staticAudioFocus = staticAudioFocus,
                audioSinkEnabled = enableAudioSink,
                isAudioChannel = true,
                btMediaLinkActive = btMediaLinkActive,
                selfDefeatingLatched = selfDefeatingLatched)

        if (!honour) {
            AppLog.i("AapAudio: phone asked for audio focus - leaving system audio focus alone " +
                    "(mode=$mode, bluetoothMedia=$btMediaLinkActive, latched=$selfDefeatingLatched)")
        }
        return honour
    }

    fun requestFocusChange(stream: Int, focusRequest: Int, callback: AudioManager.OnAudioFocusChangeListener): Int {
        val effectiveFocus = focusRequest
        AppLog.i("Audio Focus Request: stream=$stream, type=$effectiveFocus")

        var result = AudioManager.AUDIOFOCUS_REQUEST_FAILED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26+
            result = requestStockFocus(effectiveFocus)
        } else { // API < 26
            @Suppress("DEPRECATION")
            result = when (effectiveFocus) {
                Control.AudioFocusRequestNotification.AudioFocusRequestType.RELEASE_VALUE -> {
                    audioManager.abandonAudioFocus(callback)
                    legacyFocusListener?.let { audioManager.abandonAudioFocus(it) }
                    legacyFocusListener = null
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                }
                Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_VALUE -> {
                    legacyFocusListener = callback
                    mediaAudioManager.requestAudioFocus(callback, stream, AudioManager.AUDIOFOCUS_GAIN)
                }
                Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_TRANSIENT_VALUE -> {
                    legacyFocusListener = callback
                    mediaAudioManager.requestAudioFocus(callback, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                }
                Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_TRANSIENT_MAY_DUCK_VALUE -> {
                    legacyFocusListener = callback
                    mediaAudioManager.requestAudioFocus(callback, stream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                }
                else -> AudioManager.AUDIOFOCUS_REQUEST_FAILED
            }
            AppLog.i("Audio focus request result (legacy): ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "FAILED"}")
        }
        return result
    }

    /** The media attributes of the stock app: USAGE_MEDIA with the Android Auto car source, nothing else. */
    private fun stockMediaAttributes(): AudioAttributes =
        SaicCarAudioSource.attachSourceType(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA),
            SaicCarAudioSource.SRC_ANDROID_AUTO
        ).build()

    private fun stockAttributes(usage: Int): AudioAttributes = AudioAttributes.Builder().setUsage(usage).build()

    private fun abandon(request: AudioFocusRequest?, what: String) {
        request ?: return
        val abandoned = runCatching { mediaAudioManager.abandonAudioFocusRequest(request) }
            .getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        if (abandoned != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) AppLog.w("Audio focus: failed to abandon $what")
    }

    private fun stockRequest(type: Int, attributes: AudioAttributes, listener: AudioManager.OnAudioFocusChangeListener) =
        AudioFocusRequest.Builder(type)
            .setAudioAttributes(attributes)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(listener)
            .build()

    private fun grab(request: AudioFocusRequest, what: String): Int {
        val result = mediaAudioManager.requestAudioFocus(request)
        AppLog.i("Audio focus $what: ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "FAILED ($result)"}")
        return result
    }

    private fun loggingListener(name: String) =
        AudioManager.OnAudioFocusChangeListener { AppLog.i("Audio focus $name changed: $it") }

    private val transMediaListener = loggingListener("transient media")
    private val assistantListener = loggingListener("assistant")
    private val navigationListener = loggingListener("navigation")

    /**
     * What the stock app does with a focus request from the phone, each kind with its own request
     * and attributes. Returns the system result of the request that was made, GRANTED when none had
     * to be (a voice session running, a request already held).
     */
    private fun requestStockFocus(phoneRequest: Int): Int = synchronized(focusLock) {
        val voice = isAssistantActive()
        when (phoneRequest) {
            Control.AudioFocusRequestNotification.AudioFocusRequestType.RELEASE_VALUE -> {
                AppLog.i("Releasing audio focus")
                abandon(mediaFocus, "media"); mediaFocus = null
                abandon(navigationFocus, "navigation"); navigationFocus = null
                if (!voice) {
                    abandon(assistantFocus, "assistant"); assistantFocus = null
                    abandon(transMediaFocus, "transient media"); transMediaFocus = null
                }
                phoneHasGain = false
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
            Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_VALUE -> {
                // A media gain ends the assistant request and, when no guidance is playing, the navigation one.
                abandon(assistantFocus, "assistant"); assistantFocus = null
                val guidancePlaying = synchronized(activeAudioChannels) { Channel.ID_AU2 in activeAudioChannels }
                if (!guidancePlaying) { abandon(navigationFocus, "navigation"); navigationFocus = null }
                val request = stockRequest(AudioManager.AUDIOFOCUS_GAIN, stockMediaAttributes(), mediaFocusListener)
                mediaFocus = request
                grab(request, "media").also { if (it == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) phoneHasGain = true }
            }
            Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_TRANSIENT_VALUE -> when {
                voice && assistantFocus != null -> AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                voice -> {
                    val request = stockRequest(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, stockAttributes(AudioAttributes.USAGE_ASSISTANT), assistantListener)
                    assistantFocus = request
                    grab(request, "assistant")
                }
                transMediaFocus != null -> AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                else -> {
                    val request = stockRequest(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, stockMediaAttributes(), transMediaListener)
                    transMediaFocus = request
                    grab(request, "transient media")
                }
            }
            Control.AudioFocusRequestNotification.AudioFocusRequestType.GAIN_TRANSIENT_MAY_DUCK_VALUE -> when {
                navigationFocus != null -> AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                voice -> {
                    AppLog.i("Audio focus navigation: a voice session is running, request ignored")
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                }
                else -> {
                    val request = stockRequest(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                        stockAttributes(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE), navigationListener)
                    navigationFocus = request
                    grab(request, "navigation")
                }
            }
            else -> AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }
    }

    /**
     * What the system tells the media request: passed on to the phone like the stock app does, so it
     * pauses when something else takes the focus and resumes when it comes back.
     */
    private val mediaFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        AppLog.i("Audio focus media changed: $change")
        val notify = focusNotifier
        if (notify != null) {
            val state = Control.AudioFocusNotification.AudioFocusStateType.STATE_GAIN
            val mediaPlaying: Boolean
            val guidancePlaying: Boolean
            synchronized(activeAudioChannels) {
                mediaPlaying = Channel.ID_AUD in activeAudioChannels
                guidancePlaying = Channel.ID_AU2 in activeAudioChannels
            }
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    if (mediaPlaying && !guidancePlaying) {
                        notify(Control.AudioFocusNotification.AudioFocusStateType.STATE_LOSS_TRANSIENT_CAN_DUCK)
                    }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                    if (!isAssistantActive()) {
                        notify(Control.AudioFocusNotification.AudioFocusStateType.STATE_LOSS_TRANSIENT)
                        phoneHasGain = false
                    }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    notify(Control.AudioFocusNotification.AudioFocusStateType.STATE_LOSS)
                    phoneHasGain = false
                }
                AudioManager.AUDIOFOCUS_GAIN ->
                    if (!phoneHasGain) {
                        notify(state)
                        phoneHasGain = true
                    }
            }
        }
    }

    private fun requestPlaybackFocus(channel: Int) {
        val speech = channel == Channel.ID_AU1 || channel == Channel.ID_AU2
        // Speech/notification must MAY_DUCK so hybrid A2DP music is not AVRCP-paused.
        val focusGain = if (speech) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        } else {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = if (speech) {
                val usage = if (channel == Channel.ID_AU1) {
                    AudioAttributes.USAGE_VOICE_COMMUNICATION
                } else {
                    AudioAttributes.USAGE_NOTIFICATION
                }
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            } else {
                SaicCarAudioSource.mediaAttributes()
            }
            val focusAm = if (speech) audioManager else mediaAudioManager
            val request = AudioFocusRequest.Builder(focusGain)
                    .setAudioAttributes(attrs)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(playbackFocusListener)
                    .build()
            playbackFocusRequest = request

            val result = focusAm.requestAudioFocus(request)
            AppLog.i(
                "AapAudio: Playback focus (${Channel.name(channel)}, " +
                    "${if (speech) "MAY_DUCK" else "TRANSIENT"}) result: " +
                    "${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "FAILED ($result)"}",
            )
        } else {
            @Suppress("DEPRECATION")
            legacyPlaybackFocusListener = playbackFocusListener
            val stream = AudioConfigs.stream(channel, separateAudioStreams)
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(playbackFocusListener, stream, focusGain)
            AppLog.i(
                "AapAudio: Playback focus legacy (${Channel.name(channel)}) result: " +
                    "${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "FAILED"}",
            )
        }
    }

    private fun releasePlaybackFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackFocusRequest?.let {
                AppLog.i("AapAudio: Releasing playback transient audio focus")
                // Speech may have been requested on process AM; media on SAIC AM.
                runCatching { mediaAudioManager.abandonAudioFocusRequest(it) }
                runCatching { audioManager.abandonAudioFocusRequest(it) }
            }
            playbackFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            legacyPlaybackFocusListener?.let {
                AppLog.i("AapAudio: Releasing playback transient audio focus (legacy)")
                runCatching { mediaAudioManager.abandonAudioFocus(it) }
                runCatching { audioManager.abandonAudioFocus(it) }
            }
            legacyPlaybackFocusListener = null
        }
    }

    /**
     * Native AA keeps A2DP up through bootstrap, so the first media channel often starts while
     * [PlaybackFocusPolicy.Mode.AUTO] correctly skips focus (to avoid AVRCP-pausing the phone).
     * After the Wi‑Fi media path drops A2DP, call this so the car radio does not keep the amp
     * for the rest of the session.
     */
    fun reclaimPlaybackFocusIfNeeded() {
        if (staticAudioFocus || !enableAudioSink) return
        if (holdingPlaybackFocus) return
        val channel = synchronized(activeAudioChannels) {
            when {
                activeAudioChannels.isEmpty() -> return
                Channel.ID_AUD in activeAudioChannels -> Channel.ID_AUD
                else -> activeAudioChannels.first()
            }
        }
        val btMediaLinkActive = BluetoothHelper.isA2dpMediaLinkActive(context)
        val mode = playbackFocusMode()
        val acquire = PlaybackFocusPolicy.shouldAcquire(
            mode = mode,
            staticAudioFocus = staticAudioFocus,
            audioSinkEnabled = enableAudioSink,
            isAudioChannel = true,
            btMediaLinkActive = btMediaLinkActive,
            selfDefeatingLatched = selfDefeatingLatched,
        )
        if (!acquire) {
            AppLog.i(
                "AapAudio: reclaim skipped after BT media release " +
                    "(mode=$mode, bluetoothMedia=$btMediaLinkActive, latched=$selfDefeatingLatched)",
            )
            return
        }
        AppLog.i(
            "AapAudio: reclaiming transient system audio focus after BT media release " +
                "(${Channel.name(channel)}, mode=$mode)",
        )
        focusAcquiredAtMs = SystemClock.elapsedRealtime()
        holdingPlaybackFocus = true
        requestPlaybackFocus(channel)
    }

    fun releaseAllFocus() {
        AppLog.i("AapAudio: Releasing all audio focus.")
        synchronized(activeAudioChannels) { activeAudioChannels.clear() }
        // The latch is a property of one connection, not of the head unit: re-arm it so a session
        // that reconnects with Bluetooth off gets the car-radio behaviour back.
        holdingPlaybackFocus = false
        focusAcquiredAtMs = 0L
        selfDefeatingStops = 0
        selfDefeatingLatched = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            synchronized(focusLock) {
                abandon(mediaFocus, "media"); mediaFocus = null
                abandon(transMediaFocus, "transient media"); transMediaFocus = null
                abandon(assistantFocus, "assistant"); assistantFocus = null
                abandon(navigationFocus, "navigation"); navigationFocus = null
            }
            phoneHasGain = false
            playbackFocusRequest?.let { mediaAudioManager.abandonAudioFocusRequest(it) }
            playbackFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            legacyFocusListener?.let { mediaAudioManager.abandonAudioFocus(it) }
            legacyFocusListener = null
            @Suppress("DEPRECATION")
            legacyPlaybackFocusListener?.let { mediaAudioManager.abandonAudioFocus(it) }
            legacyPlaybackFocusListener = null
        }
    }

    /**
     * Processes a message as an audio stream packet.
     * Returns true if the packet was identified and processed as audio data, false otherwise.
     */
    fun process(message: AapMessage): Boolean {
        // Media stream packets have msgType 0 or 1.
        // Control packets on audio channels (Setup, Start, Stop) have types > 32767.
        if (message.type == 0 || message.type == 1) {
            if (message.size >= 10) {
                decode(message.channel, 10, message.data, message.size - 10)
            }
            return true
        }
        return false
    }

    private fun startAudioTrack(channel: Int) {
        if (audioDecoder.getTrack(channel) != null) return

        val config = AudioConfigs.get(channel)
        val stream = AudioConfigs.stream(channel, separateAudioStreams)
        // AAC only on MEDIA — matches Service Discovery (SPEECH/SYSTEM are PCM).
        val channelUsesAac = useAacAudio && channel == Channel.ID_AUD

        val offset = when (channel) {
            Channel.ID_AUD -> mediaVolumeOffset
            Channel.ID_AU1 -> assistantVolumeOffset
            Channel.ID_AU2 -> navigationVolumeOffset
            else -> 0
        }
        val gain = (1.0f + (offset / 100.0f)).coerceIn(0.0f, 2.0f)

        // Voice and Navigation benefit from lower latency. Cap the multiplier for those channels.
        // MEDIA over Wi‑Fi also gets a DiPlay-style jitter start buffer (nav/speech stay immediate).
        val effectiveMultiplier = if (channel == Channel.ID_AUD) {
            audioLatencyMultiplier
        } else {
            audioLatencyMultiplier.coerceAtMost(4)
        }
        val jitterMs = if (channel == Channel.ID_AUD) {
            MediaAudioBuffer.sanitize(mediaBufferMillis)
        } else {
            0
        }

        AppLog.i(
            "AudioDecoder.start: channel=$channel, stream=$stream, gain=$gain, " +
                "sampleRate=${config.sampleRate}, numberOfBits=${config.numberOfBits}, " +
                "numberOfChannels=${config.numberOfChannels}, isAac=$channelUsesAac, " +
                "latencyMultiplier=$effectiveMultiplier, queueCapacity=$audioQueueCapacity, " +
                "mediaJitterMs=$jitterMs, attachHwDspEqualizer=$attachHwDspEqualizer",
        )
        audioDecoder.start(
            channel,
            stream,
            config.sampleRate,
            config.numberOfBits,
            config.numberOfChannels,
            channelUsesAac,
            gain,
            effectiveMultiplier,
            audioQueueCapacity,
            staticAudioFocus,
            attachHwDspEqualizer,
            mediaBufferMillis = jitterMs,
        )
        onAudioPlaybackStarted(channel)
    }

    private fun onAudioPlaybackStarted(channel: Int) {
        if (staticAudioFocus || !enableAudioSink || !Channel.isAudio(channel)) return

        // Only the set mutation belongs under the lock. requestPlaybackFocus() is a binder
        // round-trip to AudioService and the Bluetooth probe below is another, and this runs on
        // the transport thread for every track change.
        val wasEmpty = synchronized(activeAudioChannels) {
            val empty = activeAudioChannels.isEmpty()
            activeAudioChannels.add(channel)
            empty
        }
        if (!wasEmpty) return

        val btMediaLinkActive = BluetoothHelper.isA2dpMediaLinkActive(context)
        val mode = playbackFocusMode()
        val acquire = PlaybackFocusPolicy.shouldAcquire(
                mode = mode,
                staticAudioFocus = staticAudioFocus,
                audioSinkEnabled = enableAudioSink,
                isAudioChannel = true,
                btMediaLinkActive = btMediaLinkActive,
                selfDefeatingLatched = selfDefeatingLatched)

        if (!acquire) {
            AppLog.i("AapAudio: AA audio started (${Channel.name(channel)}) - leaving system audio focus alone " +
                    "(mode=$mode, bluetoothMedia=$btMediaLinkActive, latched=$selfDefeatingLatched)")
            return
        }

        // Use GAIN_TRANSIENT, not GAIN: a permanent GAIN sends other players (e.g. the car
        // radio) a permanent AUDIOFOCUS_LOSS, so they stop and do NOT resume when we later
        // abandon focus. TRANSIENT sends AUDIOFOCUS_LOSS_TRANSIENT so they pause and resume
        // once AA audio stops and we release focus.
        AppLog.i("AapAudio: AA audio started (${Channel.name(channel)}) - acquiring transient system audio focus " +
                "(mode=$mode, bluetoothMedia=$btMediaLinkActive)")
        focusAcquiredAtMs = SystemClock.elapsedRealtime()
        holdingPlaybackFocus = true
        requestPlaybackFocus(channel)
    }

    /**
     * Releases system audio focus once the last active AA audio channel stops (dynamic mode),
     * letting other local sources (e.g. the car radio) resume.
     */
    private fun onAudioPlaybackStopped(channel: Int) {
        if (staticAudioFocus || !enableAudioSink || !Channel.isAudio(channel)) return

        val nowEmpty = synchronized(activeAudioChannels) {
            activeAudioChannels.remove(channel)
            activeAudioChannels.isEmpty()
        }
        if (!nowEmpty || !holdingPlaybackFocus) return

        noteStopWhileHoldingFocus(channel)

        AppLog.i("AapAudio: last AA audio channel stopped - releasing transient system audio focus")
        holdingPlaybackFocus = false
        releasePlaybackFocus()
    }

    /**
     * Watches for the pathology the Bluetooth probe is meant to pre-empt, for the units where it
     * cannot see the link: the media channel closing again almost as soon as we took focus, because
     * the phone stopped its own playback in response. Two of those in a row and we stop asking for
     * focus, so the session settles instead of cycling every few seconds.
     */
    private fun noteStopWhileHoldingFocus(channel: Int) {
        if (selfDefeatingLatched || focusAcquiredAtMs == 0L) return

        val elapsedMs = SystemClock.elapsedRealtime() - focusAcquiredAtMs
        if (!PlaybackFocusPolicy.countsAsSelfDefeating(channel == Channel.ID_AUD, elapsedMs)) {
            selfDefeatingStops = 0
            return
        }

        selfDefeatingStops++
        if (AppLog.LOG_DEBUG) {
            AppLog.d("AapAudio: media stopped ${elapsedMs}ms after taking audio focus " +
                    "($selfDefeatingStops/${PlaybackFocusPolicy.SELF_DEFEATING_LIMIT})")
        }
        if (selfDefeatingStops >= PlaybackFocusPolicy.SELF_DEFEATING_LIMIT) {
            selfDefeatingLatched = true
            AppLog.w("AapAudio: taking system audio focus is stopping the phone's own playback " +
                    "(the head unit is most likely its Bluetooth audio sink) - not acquiring it again this session")
        }
    }

    fun precreateAudioTrack(channel: Int) {
        if (!staticAudioFocus) return
        if (channel != Channel.ID_AU2) return
        startAudioTrack(channel)
    }

    private fun decode(channel: Int, start: Int, buf: ByteArray, len: Int) {
        var length = len
        if (length > AUDIO_BUFS_SIZE) {
            AppLog.e("Error audio len: %d  aud_buf_BUFS_SIZE: %d", length, AUDIO_BUFS_SIZE)
            length = AUDIO_BUFS_SIZE
        }

        if (audioDecoder.getTrack(channel) == null) {
            startAudioTrack(channel)
        }

        audioDecoder.decode(channel, buf, start, length)

        if ((channel == Channel.ID_AU1 || channel == Channel.ID_AU2) && staticAudioFocus) {
            duckMedia()
            handler.removeCallbacks(unduckRunnable)
            handler.postDelayed(unduckRunnable, UNDUCK_DELAY_MS)
        }
    }

    fun updateGains() {
        val mediaGain = (1.0f + (settings.mediaVolumeOffset / 100.0f)).coerceIn(0.0f, 2.0f)
        val assistantGain = (1.0f + (settings.assistantVolumeOffset / 100.0f)).coerceIn(0.0f, 2.0f)
        val navGain = (1.0f + (settings.navigationVolumeOffset / 100.0f)).coerceIn(0.0f, 2.0f)

        audioDecoder.setGain(Channel.ID_AUD, mediaGain)
        audioDecoder.setGain(Channel.ID_AU1, assistantGain)
        audioDecoder.setGain(Channel.ID_AU2, navGain)
    }

    fun restartAudio() {
        AppLog.i("AapAudio: Restarting all audio tracks")
        audioDecoder.stop()
    }

    fun stopAudio(channel: Int) {
        AppLog.i("Audio Stop: " + Channel.name(channel))
        if ((channel == Channel.ID_AU1 || channel == Channel.ID_AU2) && staticAudioFocus) {
            // Keep the speech wrappers alive to prevent recreate overhead and keep state consistent.
            // Just restore media volume.
            handler.removeCallbacks(unduckRunnable)
            unduckMedia()
        } else {
            audioDecoder.stop(channel)
            onAudioPlaybackStopped(channel)
        }
    }

    companion object {
        private const val AUDIO_BUFS_SIZE = 65536 * 4  // Up to 256 Kbytes
        private const val DUCK_VOLUME_FACTOR = 0.4f
        private const val UNDUCK_DELAY_MS = 1500L
    }
}
