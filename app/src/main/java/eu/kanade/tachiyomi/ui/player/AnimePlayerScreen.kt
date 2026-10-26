@file:OptIn(ExperimentalMaterial3Api::class)

package eu.kanade.tachiyomi.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val THEME_PREF_KEY = "anime_player_theme"

private enum class PlayerSheet { EPISODES, QUALITY, AUDIO, SUBTITLES, SPEED, OPTIONS }

private val playerSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private val resizeModes = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT,
    AspectRatioFrameLayout.RESIZE_MODE_FILL,
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
)
private val resizeLabels = listOf("Fit", "Fill", "Zoom")

/**
 * In-app anime player. Plays one episode at a time through ExoPlayer and shows the same set of
 * controls in one of three themes, each with its own button names and placement.
 */
class AnimePlayerScreen(
    val sourceId: Long,
    val animeUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val initialEpisodeUrl: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val activity = context as? Activity
        val lifecycleOwner = LocalLifecycleOwner.current

        val vm = viewModel<AnimePlayerViewModel>(
            key = "anime-player-$sourceId-$animeUrl-$initialEpisodeUrl",
            factory = viewModelFactory {
                initializer { AnimePlayerViewModel(sourceId, animeUrl, title, initialEpisodeUrl) }
            },
        )
        val state by vm.state.collectAsState()

        // Shows the video track info as a toast whenever it changes. Toasts are not covered by the
        // player's overlays, so this is readable even when the on-screen debug line is hidden.
        LaunchedEffect(state.videoInfo, state.videoSize) {
            if (state.videoInfo.isNotEmpty() || state.videoSize.isNotEmpty()) {
                toast(context, "Video: ${state.videoInfo.ifEmpty { "unknown" }}, decoded ${state.videoSize.ifEmpty { "none" }}")
            }
        }

        val prefs = remember { Injekt.get<PreferenceStore>() }
        val themePref = remember { prefs.getString(THEME_PREF_KEY, PlayerTheme.CINEMA.name) }
        var theme by remember { mutableStateOf(PlayerTheme.fromName(themePref.get())) }

        var controlsVisible by remember { mutableStateOf(true) }
        var interactionTick by remember { mutableIntStateOf(0) }
        var locked by remember { mutableStateOf(false) }
        var resizeIndex by remember { mutableIntStateOf(0) }
        var fullscreen by remember { mutableStateOf(false) }
        var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
        var scrub by remember { mutableStateOf<Float?>(null) }

        fun poke() {
            controlsVisible = true
            interactionTick++
        }

        // Controls fade out after a few seconds of playback with no touches.
        LaunchedEffect(interactionTick, state.isPlaying, sheet, locked) {
            if (controlsVisible && state.isPlaying && sheet == null && !locked) {
                kotlinx.coroutines.delay(3500)
                controlsVisible = false
            }
        }

        // Pause when the app goes to the background, so audio doesn't keep playing.
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) vm.player.pause()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Immersive: hide the system bars while the player is open, restore them on exit.
        DisposableEffect(activity) {
            val window = activity?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {
                if (window != null) {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        LaunchedEffect(fullscreen) {
            activity?.requestedOrientation = if (fullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        DisposableEffect(Unit) {
            onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }

        val onAction: (PlayerAction) -> Unit = { action ->
            poke()
            when (action) {
                PlayerAction.BACK -> navigator.pop()
                PlayerAction.LOCK -> locked = true
                PlayerAction.OPTIONS -> sheet = PlayerSheet.OPTIONS
                PlayerAction.PIP -> {
                    runCatching {
                        activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    }.onFailure {
                        toast(context, "Picture in picture isn't enabled for this app")
                    }
                }
                PlayerAction.EXTERNAL -> openExternal(context, state.videos.getOrNull(state.videoIndex)?.videoUrl)
                PlayerAction.PREV -> vm.playNeighbour(-1)
                PlayerAction.REPLAY -> vm.seekBy(-30_000L)
                PlayerAction.PLAY_PAUSE -> vm.togglePlay()
                PlayerAction.FORWARD -> vm.seekBy(30_000L)
                PlayerAction.NEXT -> vm.playNeighbour(1)
                PlayerAction.EPISODES -> sheet = PlayerSheet.EPISODES
                PlayerAction.QUALITY -> sheet = PlayerSheet.QUALITY
                PlayerAction.AUDIO -> sheet = PlayerSheet.AUDIO
                PlayerAction.SUBTITLES -> sheet = PlayerSheet.SUBTITLES
                PlayerAction.SPEED -> sheet = PlayerSheet.SPEED
                PlayerAction.ASPECT -> resizeIndex = (resizeIndex + 1) % resizeModes.size
                PlayerAction.FULLSCREEN -> fullscreen = !fullscreen
                PlayerAction.CAST, PlayerAction.SHADERS, PlayerAction.SYNC_SUBS -> {
                    toast(context, "Not available in this build yet")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (locked) {
                                poke()
                            } else if (controlsVisible) {
                                controlsVisible = false
                            } else {
                                poke()
                            }
                        },
                    )
                },
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = vm.player
                    }
                },
                update = { view -> view.resizeMode = resizeModes[resizeIndex] },
            )

            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            // Debug line, always shown while playing: what the stream's video track reports and the
            // decoded frame size. Used to tell a missing picture apart from a drawing problem.
            if (state.error == null && !state.loading) {
                Text(
                    text = "Video: ${state.videoInfo.ifEmpty { "unknown" }}, decoded ${state.videoSize.ifEmpty { "none" }}",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 96.dp),
                )
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }

            if (locked) {
                // Locked: only the unlock button is on screen, so stray touches can't do anything.
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .background(Color(0x99000000), CircleShape)
                            .clickable {
                                locked = false
                                poke()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Unlock controls",
                            tint = Color.White,
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PlayerOverlay(
                        theme = theme,
                        layout = theme.layout(),
                        title = state.title,
                        episodeLabel = state.episodes.getOrNull(state.episodeIndex)
                            ?.let { "Episode ${episodeNumberLabel(it.episode_number)}" }
                            .orEmpty(),
                        isPlaying = state.isPlaying,
                        fullscreen = fullscreen,
                        canPrev = state.episodeIndex > 0,
                        canNext = state.episodeIndex < state.episodes.size - 1,
                        position = state.position,
                        duration = state.duration,
                        scrub = scrub,
                        onScrub = { scrub = it },
                        onSeekFraction = { fraction ->
                            vm.seekTo((fraction * state.duration).toLong())
                            scrub = null
                            poke()
                        },
                        onAction = onAction,
                    )
                }
            }
        }

        sheet?.let { current ->
            ModalBottomSheet(onDismissRequest = { sheet = null }) {
                when (current) {
                    PlayerSheet.EPISODES -> OptionSheet(
                        title = "Episodes",
                        options = state.episodes.mapIndexed { index, episode ->
                            "Episode ${episodeNumberLabel(episode.episode_number)}" to (index == state.episodeIndex)
                        },
                        onPick = { index ->
                            vm.playEpisode(index)
                            sheet = null
                        },
                    )
                    PlayerSheet.QUALITY -> OptionSheet(
                        title = "Quality",
                        options = state.videos.mapIndexed { index, video ->
                            video.videoTitle.ifBlank { "Source ${index + 1}" } to (index == state.videoIndex)
                        },
                        onPick = { index ->
                            vm.selectVideo(index)
                            sheet = null
                        },
                    )
                    PlayerSheet.AUDIO -> {
                        val options = vm.trackOptions(C.TRACK_TYPE_AUDIO)
                        OptionSheet(
                            title = "Audio",
                            options = options.map { it.label to it.selected },
                            onPick = { index ->
                                vm.selectTrack(options[index], C.TRACK_TYPE_AUDIO)
                                sheet = null
                            },
                        )
                    }
                    PlayerSheet.SUBTITLES -> {
                        val options = vm.trackOptions(C.TRACK_TYPE_TEXT)
                        // The first row turns subtitles off, so the real tracks start at index 1.
                        OptionSheet(
                            title = "Subtitles",
                            options = listOf("Off" to options.none { it.selected }) +
                                options.map { it.label to it.selected },
                            onPick = { index ->
                                if (index == 0) {
                                    vm.disableTrack(C.TRACK_TYPE_TEXT)
                                } else {
                                    vm.selectTrack(options[index - 1], C.TRACK_TYPE_TEXT)
                                }
                                sheet = null
                            },
                        )
                    }
                    PlayerSheet.SPEED -> OptionSheet(
                        title = "Playback speed",
                        options = playerSpeeds.map { "${it}×" to (it == state.speed) },
                        onPick = { index ->
                            vm.setSpeed(playerSpeeds[index])
                            sheet = null
                        },
                    )
                    PlayerSheet.OPTIONS -> {
                        val themes = PlayerTheme.entries
                        OptionSheet(
                            title = "Player settings",
                            options = themes.map { "${it.label} — ${it.description}" to (it == theme) } +
                                listOf(
                                    "Open in external player" to false,
                                    "Aspect: ${resizeLabels[resizeIndex]}" to false,
                                ),
                            onPick = { index ->
                                when {
                                    index < themes.size -> {
                                        theme = themes[index]
                                        themePref.set(theme.name)
                                        sheet = null
                                    }
                                    index == themes.size -> {
                                        openExternal(context, state.videos.getOrNull(state.videoIndex)?.videoUrl)
                                        sheet = null
                                    }
                                    else -> resizeIndex = (resizeIndex + 1) % resizeModes.size
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOverlay(
    theme: PlayerTheme,
    layout: PlayerLayout,
    title: String,
    episodeLabel: String,
    isPlaying: Boolean,
    fullscreen: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    position: Long,
    duration: Long,
    scrub: Float?,
    onScrub: (Float?) -> Unit,
    onSeekFraction: (Float) -> Unit,
    onAction: (PlayerAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f),
                    ),
                ),
            ),
    ) {
        // Top bar: left group, title in the middle, right group.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionGroup(theme, layout.topLeft, isPlaying, fullscreen, canPrev, canNext, onAction, labelled = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episodeLabel.isNotBlank()) {
                    Text(
                        text = episodeLabel,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            ActionGroup(theme, layout.topRight, isPlaying, fullscreen, canPrev, canNext, onAction, labelled = false)
        }

        // Centre transport.
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                when (theme) {
                    PlayerTheme.CINEMA -> 28.dp
                    PlayerTheme.FROSTED -> 20.dp
                    PlayerTheme.CLASSIC -> 16.dp
                },
            ),
        ) {
            layout.centre.forEach { action ->
                PlayerButton(
                    theme = theme,
                    action = action,
                    isPlaying = isPlaying,
                    fullscreen = fullscreen,
                    enabled = isEnabled(action, canPrev, canNext),
                    labelled = false,
                    big = action == PlayerAction.PLAY_PAUSE,
                    onClick = { onAction(action) },
                )
            }
        }

        // Bottom: seek bar, then the tool strip.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SeekRow(
                position = position,
                duration = duration,
                scrub = scrub,
                onScrub = onScrub,
                onSeekFraction = onSeekFraction,
                glass = theme == PlayerTheme.FROSTED,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionGroup(
                    theme = theme,
                    actions = layout.bottomTools,
                    isPlaying = isPlaying,
                    fullscreen = fullscreen,
                    canPrev = canPrev,
                    canNext = canNext,
                    onAction = onAction,
                    labelled = true,
                )
            }
        }
    }
}

@Composable
private fun ActionGroup(
    theme: PlayerTheme,
    actions: List<PlayerAction>,
    isPlaying: Boolean,
    fullscreen: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    onAction: (PlayerAction) -> Unit,
    labelled: Boolean,
) {
    Row(
        modifier = if (theme == PlayerTheme.FROSTED) {
            Modifier
                .playerGlass(RoundedCornerShape(26.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        } else {
            Modifier
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            PlayerButton(
                theme = theme,
                action = action,
                isPlaying = isPlaying,
                fullscreen = fullscreen,
                enabled = isEnabled(action, canPrev, canNext),
                labelled = labelled,
                big = false,
                onClick = { onAction(action) },
            )
        }
    }
}

@Composable
private fun PlayerButton(
    theme: PlayerTheme,
    action: PlayerAction,
    isPlaying: Boolean,
    fullscreen: Boolean,
    enabled: Boolean,
    labelled: Boolean,
    big: Boolean,
    onClick: () -> Unit,
) {
    val size = when {
        big -> 64.dp
        theme == PlayerTheme.CLASSIC -> 40.dp
        else -> 44.dp
    }
    val shape = when (theme) {
        PlayerTheme.CLASSIC -> RoundedCornerShape(10.dp)
        else -> CircleShape
    }
    val fill = when (theme) {
        PlayerTheme.CINEMA -> Color.Transparent
        PlayerTheme.FROSTED -> Color(0x66181818)
        PlayerTheme.CLASSIC -> Color(0xAA000000)
    }
    val alpha = if (enabled) 0.95f else 0.35f

    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(fill, shape)
                .then(
                    if (theme == PlayerTheme.FROSTED) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(isPlaying, fullscreen),
                contentDescription = theme.label(action),
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(if (big) 34.dp else 22.dp),
            )
        }
        if (labelled) {
            Text(
                text = theme.label(action),
                color = Color.White.copy(alpha = if (enabled) 0.85f else 0.35f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SeekRow(
    position: Long,
    duration: Long,
    scrub: Float?,
    onScrub: (Float?) -> Unit,
    onSeekFraction: (Float) -> Unit,
    glass: Boolean,
) {
    val progress = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shown = scrub ?: progress
    val shownMs = if (scrub != null) (scrub * duration).toLong() else position

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (glass) {
                    Modifier
                        .playerGlass(RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp)
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatTime(shownMs),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = shown,
            onValueChange = { onScrub(it) },
            onValueChangeFinished = {
                scrub?.let { onSeekFraction(it) }
                onScrub(null)
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Text(
            text = formatTime(duration),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun OptionSheet(
    title: String,
    options: List<Pair<String, Boolean>>,
    onPick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (options.isEmpty()) {
            Text(
                text = "Nothing here yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            itemsIndexed(options) { index, option ->
                val (label, selected) = option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(index) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.playerGlass(shape: androidx.compose.ui.graphics.Shape): Modifier = this
    .background(Color(0x66181818), shape)
    .border(1.dp, Color.White.copy(alpha = 0.2f), shape)

private fun isEnabled(action: PlayerAction, canPrev: Boolean, canNext: Boolean): Boolean = when (action) {
    PlayerAction.PREV -> canPrev
    PlayerAction.NEXT -> canNext
    else -> true
}

private fun episodeNumberLabel(number: Float): String =
    if (number % 1f == 0f) number.toInt().toString() else number.toString()

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun openExternal(context: Context, url: String?) {
    if (url == null) {
        toast(context, "No stream to open")
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/*")
    runCatching { context.startActivity(Intent.createChooser(intent, "Open episode in")) }
        .onFailure { toast(context, "No video app installed") }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
