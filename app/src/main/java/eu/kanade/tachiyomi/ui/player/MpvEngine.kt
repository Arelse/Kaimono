package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import dev.jdtech.mpv.MPVLib

/** One audio or subtitle track as mpv reports it. [id] is the value mpv uses to select it. */
data class MpvTrack(val id: Int, val label: String, val selected: Boolean)

/**
 * Playback engine built on libmpv. Owns the single mpv instance the player uses. Playback calls are
 * made from the main thread; events from mpv come back on its own thread and are posted to the main
 * thread before they reach [Listener].
 */
class MpvEngine(private val appContext: Context) {

    interface Listener {
        fun onFileLoaded()
        fun onEndOfFile()
        fun onLoadFailed()
    }

    var listener: Listener? = null

    private val main = Handler(Looper.getMainLooper())

    // The libmpv instance, created by start() and cleared by stop().
    private var mpv: MPVLib? = null

    // Set while a new file is being opened, so the end-of-file event from the old file is ignored.
    @Volatile private var switching = false

    // Work to do once the next file has loaded: restore the position and add subtitle files.
    private var pendingStartMs = 0L
    private var pendingSubtitles: List<Pair<String, String>> = emptyList()

    val isStarted: Boolean get() = mpv != null

    private val observer = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {}
        override fun eventProperty(property: String, value: Long) {}
        override fun eventProperty(property: String, value: String) {}
        override fun eventProperty(property: String, value: Double) {}

        override fun eventProperty(property: String, value: Boolean) {
            if (property == "eof-reached" && value && !switching) {
                main.post { listener?.onEndOfFile() }
            }
        }

        override fun event(eventId: Int) {
            when (eventId) {
                EVENT_FILE_LOADED -> {
                    switching = false
                    applyPendingWork()
                    main.post { listener?.onFileLoaded() }
                }
                EVENT_END_FILE -> {
                    if (!switching) main.post { listener?.onLoadFailed() }
                }
            }
        }
    }

    /** Creates and starts the mpv instance. Safe to call more than once. */
    fun start() {
        if (mpv != null) return
        val player = MPVLib.create(appContext)
        mpv = player
        player.setOptionString("config", "no")
        val cache = appContext.cacheDir.absolutePath
        player.setOptionString("gpu-shader-cache-dir", cache)
        player.setOptionString("icc-cache-dir", cache)
        player.setOptionString("hwdec", "mediacodec-copy")
        player.setOptionString("ao", "audiotrack,opensles")
        player.setOptionString("vo", "null")
        // Stay on the last frame at the end of an episode instead of closing, so the autoplay check
        // can read eof-reached.
        player.setOptionString("keep-open", "yes")
        player.setOptionString("idle", "once")
        player.setOptionString("force-window", "no")
        player.init()
        player.addObserver(observer)
        player.observeProperty("eof-reached", FORMAT_FLAG)
    }

    /** Stops mpv. Detach the video surface before calling this. */
    fun stop() {
        val player = mpv ?: return
        player.removeObserver(observer)
        player.destroy()
        mpv = null
    }

    /**
     * Opens [url] with the given HTTP [headers]. [startMs] is where to resume, and [subtitles] are
     * (url, language) pairs added once the file has loaded.
     */
    fun loadFile(url: String, headers: Map<String, String>, subtitles: List<Pair<String, String>>, startMs: Long) {
        val player = mpv ?: return
        switching = true
        pendingStartMs = startMs
        pendingSubtitles = subtitles
        // Always set the header list, even when empty, so the previous stream's headers don't carry over.
        player.setPropertyString(
            "http-header-fields",
            headers.entries.joinToString(",") { "${it.key}: ${it.value}" },
        )
        player.command(arrayOf("loadfile", url))
    }

    private fun applyPendingWork() {
        val player = mpv ?: return
        val start = pendingStartMs
        if (start > 0L) player.command(arrayOf("seek", (start / 1000.0).toString(), "absolute+exact"))
        pendingStartMs = 0L
        for ((url, lang) in pendingSubtitles) {
            if (lang.isBlank()) {
                player.command(arrayOf("sub-add", url, "auto"))
            } else {
                player.command(arrayOf("sub-add", url, "auto", lang))
            }
        }
        pendingSubtitles = emptyList()
    }

    // Playback controls

    fun isPaused(): Boolean = mpv?.getPropertyBoolean("pause") ?: true

    fun setPaused(paused: Boolean) {
        mpv?.setPropertyBoolean("pause", paused)
    }

    fun isEndOfFile(): Boolean = mpv?.getPropertyBoolean("eof-reached") ?: false

    fun positionMs(): Long =
        ((mpv?.getPropertyDouble("time-pos") ?: 0.0) * 1000).toLong().coerceAtLeast(0L)

    fun durationMs(): Long =
        ((mpv?.getPropertyDouble("duration") ?: 0.0) * 1000).toLong().coerceAtLeast(0L)

    fun seekAbsoluteMs(ms: Long) {
        mpv?.command(arrayOf("seek", (ms.coerceAtLeast(0L) / 1000.0).toString(), "absolute+exact"))
    }

    fun seekRelativeMs(deltaMs: Long) {
        mpv?.command(arrayOf("seek", (deltaMs / 1000.0).toString(), "relative+exact"))
    }

    fun setSpeed(speed: Float) {
        mpv?.setPropertyDouble("speed", speed.toDouble())
    }

    /** Video panscan: 0 fits the picture, 1 fills the view by cropping, values between zoom in part way. */
    fun setPanscan(value: Double) {
        mpv?.setPropertyDouble("panscan", value)
    }

    /** "WIDTHxHEIGHT" of the decoded picture, or "" when nothing is decoded yet. */
    fun videoSize(): String {
        val player = mpv ?: return ""
        val w = player.getPropertyInt("video-params/w") ?: return ""
        val h = player.getPropertyInt("video-params/h") ?: return ""
        return if (w > 0 && h > 0) "${w}x$h" else ""
    }

    /** Codec of the current video track, or null when the file has no video track. */
    fun videoCodec(): String? = mpv?.getPropertyString("video-format")?.takeIf { it.isNotBlank() }

    /** Tracks of [type] ("audio" or "sub") in the current file. */
    fun tracks(type: String): List<MpvTrack> {
        val player = mpv ?: return emptyList()
        val count = player.getPropertyInt("track-list/count") ?: 0
        return (0 until count).mapNotNull { index ->
            val prefix = "track-list/$index"
            if (player.getPropertyString("$prefix/type") != type) return@mapNotNull null
            val id = player.getPropertyInt("$prefix/id") ?: return@mapNotNull null
            val title = player.getPropertyString("$prefix/title")?.takeIf { it.isNotBlank() }
            val lang = player.getPropertyString("$prefix/lang")?.takeIf { it.isNotBlank() }?.uppercase()
            MpvTrack(
                id = id,
                label = title ?: lang ?: "Track $id",
                selected = player.getPropertyBoolean("$prefix/selected") ?: false,
            )
        }
    }

    fun selectTrack(type: String, id: Int) {
        mpv?.setPropertyInt(if (type == "audio") "aid" else "sid", id)
    }

    fun disableTrack(type: String) {
        mpv?.setPropertyString(if (type == "audio") "aid" else "sid", "no")
    }

    // Surface

    fun attachSurface(holder: SurfaceHolder) {
        val player = mpv ?: return
        player.attachSurface(holder.surface)
        player.setPropertyString("vo", "gpu")
    }

    fun surfaceSizeChanged(width: Int, height: Int) {
        mpv?.setPropertyString("android-surface-size", "${width}x$height")
    }

    fun detachSurface() {
        val player = mpv ?: return
        player.setPropertyString("vo", "null")
        player.detachSurface()
    }

    private companion object {
        // libmpv's event ids and property format for flags, as the native library defines them.
        const val EVENT_END_FILE = 7
        const val EVENT_FILE_LOADED = 8
        const val FORMAT_FLAG = 3
    }
}

/** The view mpv draws the picture into. Its surface is handed to the engine as it appears and disappears. */
class MpvSurfaceView(context: Context, private val engine: MpvEngine) : SurfaceView(context), SurfaceHolder.Callback {

    private val callback = this

    init {
        holder.addCallback(callback)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        engine.attachSurface(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        engine.surfaceSizeChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        engine.detachSurface()
    }

    /** Stops listening for surface changes. Call before the view is removed. */
    fun release() {
        holder.removeCallback(callback)
    }
}
