package eu.kanade.tachiyomi.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The three looks for the anime player. Each one shows every [PlayerAction], but with its own
 * button names ([label]) and its own placement ([layout]).
 */
enum class PlayerTheme(val label: String, val description: String) {
    CINEMA("Cinema", "Big centre transport, a tool strip along the bottom, no boxes"),
    FROSTED("Frosted Glass", "Glass capsules on the top and bottom bars"),
    GLASS_PANE("Liquid Pane", "Blurred glass panels: split top controls, a floating bottom panel"),
    CLASSIC("Classic Bar", "Compact edge buttons with one bottom bar"),
    ;

    companion object {
        fun fromName(name: String): PlayerTheme = entries.firstOrNull { it.name == name } ?: CINEMA
    }
}

enum class PlayerAction {
    BACK, LOCK, OPTIONS, PIP, EXTERNAL,
    PREV, REPLAY, PLAY_PAUSE, FORWARD, NEXT,
    EPISODES, QUALITY, AUDIO, SUBTITLES, SPEED, ASPECT, FULLSCREEN,
    CAST, SHADERS, SYNC_SUBS,
}

/** Which action sits where. Every theme lists all 20 actions exactly once. */
data class PlayerLayout(
    val topLeft: List<PlayerAction>,
    val topRight: List<PlayerAction>,
    val centre: List<PlayerAction>,
    val bottomTools: List<PlayerAction>,
)

fun PlayerTheme.layout(): PlayerLayout = when (this) {
    PlayerTheme.CINEMA -> PlayerLayout(
        topLeft = listOf(PlayerAction.BACK),
        topRight = listOf(PlayerAction.LOCK, PlayerAction.OPTIONS),
        centre = listOf(
            PlayerAction.PREV, PlayerAction.REPLAY, PlayerAction.PLAY_PAUSE,
            PlayerAction.FORWARD, PlayerAction.NEXT,
        ),
        bottomTools = listOf(
            PlayerAction.EPISODES, PlayerAction.QUALITY, PlayerAction.AUDIO, PlayerAction.SUBTITLES,
            PlayerAction.SPEED, PlayerAction.ASPECT, PlayerAction.PIP, PlayerAction.EXTERNAL,
            PlayerAction.CAST, PlayerAction.SHADERS, PlayerAction.SYNC_SUBS, PlayerAction.FULLSCREEN,
        ),
    )
    PlayerTheme.FROSTED -> PlayerLayout(
        topLeft = listOf(PlayerAction.BACK, PlayerAction.LOCK, PlayerAction.OPTIONS),
        topRight = listOf(PlayerAction.PIP, PlayerAction.EXTERNAL, PlayerAction.CAST),
        centre = listOf(PlayerAction.REPLAY, PlayerAction.PLAY_PAUSE, PlayerAction.FORWARD),
        bottomTools = listOf(
            PlayerAction.PREV, PlayerAction.NEXT, PlayerAction.EPISODES, PlayerAction.QUALITY,
            PlayerAction.AUDIO, PlayerAction.SUBTITLES, PlayerAction.SPEED, PlayerAction.ASPECT,
            PlayerAction.SHADERS, PlayerAction.SYNC_SUBS, PlayerAction.FULLSCREEN,
        ),
    )
    PlayerTheme.GLASS_PANE -> PlayerLayout(
        topLeft = listOf(PlayerAction.BACK, PlayerAction.LOCK),
        topRight = listOf(PlayerAction.FULLSCREEN, PlayerAction.OPTIONS, PlayerAction.EXTERNAL),
        centre = listOf(
            PlayerAction.PREV, PlayerAction.REPLAY, PlayerAction.PLAY_PAUSE,
            PlayerAction.FORWARD, PlayerAction.NEXT,
        ),
        bottomTools = listOf(
            PlayerAction.EPISODES, PlayerAction.QUALITY, PlayerAction.AUDIO, PlayerAction.SUBTITLES,
            PlayerAction.SPEED, PlayerAction.ASPECT, PlayerAction.PIP, PlayerAction.CAST,
            PlayerAction.SHADERS, PlayerAction.SYNC_SUBS,
        ),
    )
    PlayerTheme.CLASSIC -> PlayerLayout(
        topLeft = listOf(PlayerAction.BACK),
        topRight = listOf(PlayerAction.FULLSCREEN, PlayerAction.ASPECT, PlayerAction.SPEED, PlayerAction.OPTIONS),
        centre = listOf(PlayerAction.PREV, PlayerAction.PLAY_PAUSE, PlayerAction.NEXT),
        bottomTools = listOf(
            PlayerAction.LOCK, PlayerAction.REPLAY, PlayerAction.FORWARD, PlayerAction.EPISODES,
            PlayerAction.QUALITY, PlayerAction.AUDIO, PlayerAction.SUBTITLES, PlayerAction.PIP,
            PlayerAction.EXTERNAL, PlayerAction.CAST, PlayerAction.SHADERS, PlayerAction.SYNC_SUBS,
        ),
    )
}

/** Button names per theme. These are ours, not the names used by AnymeX. */
fun PlayerTheme.label(action: PlayerAction): String = when (this) {
    PlayerTheme.CINEMA -> when (action) {
        PlayerAction.BACK -> "Exit"
        PlayerAction.LOCK -> "Lock"
        PlayerAction.OPTIONS -> "Options"
        PlayerAction.PIP -> "Mini"
        PlayerAction.EXTERNAL -> "Open in"
        PlayerAction.PREV -> "Prev"
        PlayerAction.REPLAY -> "-30"
        PlayerAction.PLAY_PAUSE -> "Play"
        PlayerAction.FORWARD -> "+30"
        PlayerAction.NEXT -> "Next"
        PlayerAction.EPISODES -> "Episodes"
        PlayerAction.QUALITY -> "Quality"
        PlayerAction.AUDIO -> "Audio"
        PlayerAction.SUBTITLES -> "Subs"
        PlayerAction.SPEED -> "Speed"
        PlayerAction.ASPECT -> "Fit"
        PlayerAction.FULLSCREEN -> "Full"
        PlayerAction.CAST -> "Cast"
        PlayerAction.SHADERS -> "Look"
        PlayerAction.SYNC_SUBS -> "Sync"
    }
    PlayerTheme.FROSTED -> when (action) {
        PlayerAction.BACK -> "Close"
        PlayerAction.LOCK -> "Lock"
        PlayerAction.OPTIONS -> "Settings"
        PlayerAction.PIP -> "Float"
        PlayerAction.EXTERNAL -> "External"
        PlayerAction.PREV -> "Previous"
        PlayerAction.REPLAY -> "Back 30"
        PlayerAction.PLAY_PAUSE -> "Play"
        PlayerAction.FORWARD -> "Ahead 30"
        PlayerAction.NEXT -> "Up Next"
        PlayerAction.EPISODES -> "Chapters"
        PlayerAction.QUALITY -> "HD"
        PlayerAction.AUDIO -> "Tracks"
        PlayerAction.SUBTITLES -> "CC"
        PlayerAction.SPEED -> "Rate"
        PlayerAction.ASPECT -> "Frame"
        PlayerAction.FULLSCREEN -> "Expand"
        PlayerAction.CAST -> "Screen"
        PlayerAction.SHADERS -> "Colour"
        PlayerAction.SYNC_SUBS -> "Timing"
    }
    PlayerTheme.GLASS_PANE -> when (action) {
        PlayerAction.BACK -> "Back"
        PlayerAction.LOCK -> "Lock"
        PlayerAction.OPTIONS -> "Tune"
        PlayerAction.PIP -> "Mini"
        PlayerAction.EXTERNAL -> "Open"
        PlayerAction.PREV -> "Prev"
        PlayerAction.REPLAY -> "15"
        PlayerAction.PLAY_PAUSE -> "Play"
        PlayerAction.FORWARD -> "15"
        PlayerAction.NEXT -> "Next"
        PlayerAction.EPISODES -> "Eps"
        PlayerAction.QUALITY -> "Quality"
        PlayerAction.AUDIO -> "Sound"
        PlayerAction.SUBTITLES -> "Text"
        PlayerAction.SPEED -> "Pace"
        PlayerAction.ASPECT -> "Frame"
        PlayerAction.FULLSCREEN -> "Expand"
        PlayerAction.CAST -> "Cast"
        PlayerAction.SHADERS -> "Grade"
        PlayerAction.SYNC_SUBS -> "Delay"
    }
    PlayerTheme.CLASSIC -> when (action) {
        PlayerAction.BACK -> "Back"
        PlayerAction.LOCK -> "Locked"
        PlayerAction.OPTIONS -> "More"
        PlayerAction.PIP -> "PiP"
        PlayerAction.EXTERNAL -> "Ext"
        PlayerAction.PREV -> "Prev"
        PlayerAction.REPLAY -> "-30s"
        PlayerAction.PLAY_PAUSE -> "Play"
        PlayerAction.FORWARD -> "+30s"
        PlayerAction.NEXT -> "Next"
        PlayerAction.EPISODES -> "List"
        PlayerAction.QUALITY -> "Quality"
        PlayerAction.AUDIO -> "Audio"
        PlayerAction.SUBTITLES -> "Subs"
        PlayerAction.SPEED -> "Speed"
        PlayerAction.ASPECT -> "Aspect"
        PlayerAction.FULLSCREEN -> "Fullscreen"
        PlayerAction.CAST -> "Cast"
        PlayerAction.SHADERS -> "Shaders"
        PlayerAction.SYNC_SUBS -> "Sync"
    }
}

fun PlayerAction.icon(isPlaying: Boolean, fullscreen: Boolean): ImageVector = when (this) {
    PlayerAction.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    PlayerAction.LOCK -> Icons.Filled.Lock
    PlayerAction.OPTIONS -> Icons.Filled.Settings
    PlayerAction.PIP -> Icons.Filled.PictureInPicture
    PlayerAction.EXTERNAL -> Icons.Filled.OpenInNew
    PlayerAction.PREV -> Icons.Filled.SkipPrevious
    PlayerAction.REPLAY -> Icons.Filled.Replay30
    PlayerAction.PLAY_PAUSE -> if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
    PlayerAction.FORWARD -> Icons.Filled.Forward30
    PlayerAction.NEXT -> Icons.Filled.SkipNext
    PlayerAction.EPISODES -> Icons.Filled.PlaylistPlay
    PlayerAction.QUALITY -> Icons.Filled.HighQuality
    PlayerAction.AUDIO -> Icons.Filled.Audiotrack
    PlayerAction.SUBTITLES -> Icons.Filled.Subtitles
    PlayerAction.SPEED -> Icons.Filled.Speed
    PlayerAction.ASPECT -> Icons.Filled.AspectRatio
    PlayerAction.FULLSCREEN -> if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen
    PlayerAction.CAST -> Icons.Filled.Cast
    PlayerAction.SHADERS -> Icons.Filled.Palette
    PlayerAction.SYNC_SUBS -> Icons.Filled.Sync
}
