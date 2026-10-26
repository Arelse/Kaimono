package eu.kanade.tachiyomi.ui.player

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.VideoSize
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** One audio or subtitle track as the player reports it. */
data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean,
)

/**
 * Plays episodes from an installed anime source. Episodes come from the source's episode list,
 * streams from its video list, and playback runs in ExoPlayer.
 */
class AnimePlayerViewModel(
    private val sourceId: Long,
    animeUrl: String,
    title: String,
    private val initialEpisodeUrl: String,
) : ViewModel() {

    private val app: Application = Injekt.get()

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
        /** Codec of the stream's video track, or "no video track". Shown when the picture is black. */
        val videoInfo: String = "",
        /** Size the decoder reports, e.g. "1280x720"; "0x0" means no picture is being decoded. */
        val videoSize: String = "",
    )

    private val _state = MutableStateFlow(State(title = title))
    val state: StateFlow<State> = _state.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(app).build()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val video = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }?.getTrackFormat(0)
                val info = video?.let { it.sampleMimeType ?: "unknown codec" } ?: "no video track"
                _state.update { it.copy(videoInfo = info) }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _state.update { it.copy(videoSize = "${videoSize.width}x${videoSize.height}") }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Autoplay the next episode when this one ends.
                if (playbackState == Player.STATE_ENDED) {
                    val next = _state.value.episodeIndex + 1
                    if (next < _state.value.episodes.size) playEpisode(next)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update { it.copy(loading = false, error = "Playback failed (${error.errorCodeName})") }
            }
        })

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

        // Keeps the position and duration in state so the controls can show them.
        viewModelScope.launch {
            while (isActive) {
                _state.update {
                    it.copy(
                        position = player.currentPosition.coerceAtLeast(0L),
                        duration = player.duration.takeIf { d -> d > 0L } ?: 0L,
                    )
                }
                delay(500)
            }
        }
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
            prepare(videos[preferred], keepPosition = 0L)
            _state.update { it.copy(loading = false) }
        }
    }

    /** Switches to another stream of the same episode, keeping the current position. */
    fun selectVideo(index: Int) {
        val video = _state.value.videos.getOrNull(index) ?: return
        val keep = player.currentPosition
        _state.update { it.copy(videoIndex = index, error = null) }
        prepare(video, keepPosition = keep)
    }

    fun playNeighbour(delta: Int) = playEpisode(_state.value.episodeIndex + delta)

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it > 0L }
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (duration != null) target.coerceAtMost(duration) else target)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    /** Audio or subtitle tracks the player currently has, for [type] (a C.TRACK_TYPE_* value). */
    fun trackOptions(type: Int): List<TrackOption> {
        val tracks: Tracks = player.currentTracks
        return tracks.groups.flatMapIndexed { groupIndex, group ->
            if (group.type != type) {
                emptyList()
            } else {
                (0 until group.length).map { trackIndex ->
                    TrackOption(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        label = labelFor(group.getTrackFormat(trackIndex)),
                        selected = group.isTrackSelected(trackIndex),
                    )
                }
            }
        }
    }

    fun selectTrack(option: TrackOption, type: Int) {
        val group = player.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }

    fun disableTrack(type: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, true)
            .build()
    }

    private fun prepare(video: Video, keepPosition: Long) {
        val headers: Map<String, String> = video.headers?.let { h ->
            (0 until h.size).associate { h.name(it) to h.value(it) }
        }.orEmpty()
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
        val mediaSource = DefaultMediaSourceFactory(httpFactory)
            .createMediaSource(MediaItem.fromUri(video.videoUrl))
        player.setMediaSource(mediaSource)
        player.prepare()
        if (keepPosition > 0L) player.seekTo(keepPosition)
        player.playWhenReady = true
        player.setPlaybackSpeed(_state.value.speed)
    }

    private fun labelFor(format: Format): String {
        format.label?.takeIf { it.isNotBlank() }?.let { return it }
        format.language?.takeIf { it.isNotBlank() }?.let { return it.uppercase() }
        return "Track"
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
