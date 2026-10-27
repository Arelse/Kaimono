package eu.kanade.tachiyomi.ui.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.browse.source.isUsableAnimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** One audio or subtitle track the player offers. [id] is the track id the player selects by. */
data class TrackOption(
    val id: Int,
    val label: String,
    val selected: Boolean,
)

/**
 * Plays episodes from an installed anime source. Episodes come from the source's episode list,
 * streams from its video list, and playback runs in mpv through [MpvEngine].
 */
class AnimePlayerViewModel(
    private val sourceId: Long,
    animeUrl: String,
    title: String,
    private val initialEpisodeUrl: String,
) : ViewModel(), MpvEngine.Listener {

    private val app: Application = Injekt.get()

    private val engine = MpvEngine(app)

    // The stream currently loaded, used for the diagnostic readout.
    private var currentVideo: Video? = null

    private val source: AnimeCatalogueSource? = AnimeExtensionManager.installedSources()
        .filterIsInstance<AnimeCatalogueSource>()
        .firstOrNull { isUsableAnimeSource(it) && it.id == sourceId }

    private val seed: SAnime = SAnime.create().apply {
        url = animeUrl
        this.title = title
    }

    data class State(
        val title: String = "",
        val episodes: List<SEpisode> = emptyList(),
        val episodeIndex: Int = -1,
        val videos: List<Video> = emptyList(),
        val videoIndex: Int = 0,
        val loading: Boolean = true,
        val error: String? = null,
        val isPlaying: Boolean = false,
        val position: Long = 0L,
        val duration: Long = 0L,
        val speed: Float = 1f,
        /** Codec and stream details of the current video, or "no video track". Shown when the picture is black. */
        val videoInfo: String = "",
        /** Size the decoder reports, e.g. "1280x720"; empty means no picture is being decoded. */
        val videoSize: String = "",
    )

    private val _state = MutableStateFlow(State(title = title))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        engine.listener = this
        engine.start()

        viewModelScope.launch {
            val src = source
            if (src == null) {
                _state.update { it.copy(loading = false, error = "This source is no longer installed") }
                return@launch
            }
            val episodes = withContext(Dispatchers.IO) {
                runCatching { src.getEpisodeList(seed) }.getOrDefault(emptyList())
            }
            _state.update { it.copy(episodes = episodes) }
            if (episodes.isEmpty()) {
                _state.update { it.copy(loading = false, error = "No episodes found for this anime") }
                return@launch
            }
            playEpisode(episodes.indexOfFirst { it.url == initialEpisodeUrl }.coerceAtLeast(0))
        }

        // Keeps the playback state, position, and diagnostics in state so the controls can show them.
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(500)
            }
        }
    }

    private fun refresh() {
        if (!engine.isStarted) return
        val codec = engine.videoCodec()
        val host = currentVideo?.videoUrl?.toHttpUrlOrNull()?.host ?: "?"
        val file = currentVideo?.videoUrl?.substringAfterLast('/')?.substringBefore('?')?.take(40) ?: "?"
        val info = if (codec == null) "no video track" else "codec=$codec, host=$host, file=$file"
        _state.update {
            it.copy(
                isPlaying = !engine.isPaused() && !engine.isEndOfFile(),
                position = engine.positionMs(),
                duration = engine.durationMs(),
                videoInfo = info,
                videoSize = engine.videoSize(),
            )
        }
    }

    // MpvEngine.Listener

    override fun onFileLoaded() {
        _state.update { it.copy(loading = false, error = null) }
    }

    override fun onEndOfFile() {
        // Autoplay the next episode when this one ends.
        val next = _state.value.episodeIndex + 1
        if (next < _state.value.episodes.size) playEpisode(next)
    }

    override fun onLoadFailed() {
        _state.update { it.copy(loading = false, error = "Playback failed: the player could not open this stream") }
    }

    /**
     * Some extensions implement getVideoList(episode); others only implement the hoster API
     * (getHosterList, then getVideoList per hoster). Try the first, and fall back to the second when
     * the extension says it isn't implemented.
     */
    private suspend fun streamsFor(src: AnimeCatalogueSource, episode: SEpisode): List<Video> =
        try {
            src.getVideoList(episode)
        } catch (e: UnsupportedOperationException) {
            src.getHosterList(episode).flatMap { hoster -> src.getVideoList(hoster) }
        }

    fun playEpisode(index: Int) {
        val src = source ?: return
        val episode = _state.value.episodes.getOrNull(index) ?: return
        _state.update { it.copy(episodeIndex = index, loading = true, error = null, videos = emptyList()) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { streamsFor(src, episode) } }
            val videos = result.getOrDefault(emptyList()).filter { it.videoUrl.isNotBlank() }

            if (videos.isEmpty()) {
                // Show the extension's real error when it threw, instead of hiding it.
                val reason = result.exceptionOrNull()
                    ?.let { "Could not load streams: ${it::class.java.simpleName}: ${it.message ?: "no message"}" }
                    ?: "No stream found for this episode"
                _state.update { it.copy(loading = false, error = reason) }
                return@launch
            }
            val preferred = videos.indexOfFirst { it.preferred }.takeIf { it >= 0 } ?: 0
            _state.update { it.copy(videos = videos, videoIndex = preferred) }
            prepare(videos[preferred], keepPositionMs = 0L)
        }
    }

    /** Switches to another stream of the same episode, keeping the current position. */
    fun selectVideo(index: Int) {
        val video = _state.value.videos.getOrNull(index) ?: return
        val keep = engine.positionMs()
        _state.update { it.copy(videoIndex = index, error = null) }
        prepare(video, keepPositionMs = keep)
    }

    fun playNeighbour(delta: Int) = playEpisode(_state.value.episodeIndex + delta)

    fun togglePlay() {
        if (engine.isPaused()) {
            if (engine.isEndOfFile()) engine.seekAbsoluteMs(0L)
            engine.setPaused(false)
        } else {
            engine.setPaused(true)
        }
    }

    /** Pauses playback, for when the app goes to the background. */
    fun pausePlayback() {
        engine.setPaused(true)
    }

    fun seekBy(deltaMs: Long) = engine.seekRelativeMs(deltaMs)

    fun seekTo(positionMs: Long) = engine.seekAbsoluteMs(positionMs)

    fun setSpeed(speed: Float) {
        engine.setSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    /** 0 fits the picture, 1 fills the view by cropping, and values between zoom in part way. */
    fun setPanscan(value: Double) = engine.setPanscan(value)

    /** The view the picture is drawn into. Its surface is attached to the engine as it appears. */
    fun createSurface(context: Context): MpvSurfaceView = MpvSurfaceView(context, engine)

    /** Audio or subtitle tracks the current file has, for [type] (a C.TRACK_TYPE_* value). */
    fun trackOptions(type: Int): List<TrackOption> =
        engine.tracks(mpvType(type)).map { TrackOption(it.id, it.label, it.selected) }

    fun selectTrack(option: TrackOption, type: Int) = engine.selectTrack(mpvType(type), option.id)

    fun disableTrack(type: Int) = engine.disableTrack(mpvType(type))

    private fun mpvType(type: Int): String = if (type == C.TRACK_TYPE_AUDIO) "audio" else "sub"

    private fun prepare(video: Video, keepPositionMs: Long) {
        currentVideo = video
        val headers: Map<String, String> = video.headers?.let { h ->
            (0 until h.size).associate { h.name(it) to h.value(it) }
        }.orEmpty()
        // Subtitle files shipped with the stream are added once the file has loaded.
        val subtitles = video.subtitleTracks
            .filter { it.url.isNotBlank() }
            .map { it.url to it.lang }
        engine.loadFile(video.videoUrl, headers, subtitles, keepPositionMs)
        engine.setSpeed(_state.value.speed)
    }

    override fun onCleared() {
        engine.listener = null
        engine.stop()
        super.onCleared()
    }
}
