package eu.kanade.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AnimeLook
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Colours and shape for the anime screens. One set per [AnimeLook]. */
data class AnimeLookTokens(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val cornerRadius: Dp,
    /** True for light looks, so scrims and badges can pick the right contrast. */
    val isLight: Boolean,
)

fun AnimeLook.tokens(): AnimeLookTokens = when (this) {
    AnimeLook.CINEMATIC -> AnimeLookTokens(
        background = Color(0xFF0A0A0C),
        surface = Color(0xFF16161A),
        surfaceHigh = Color(0xFF22222A),
        accent = Color(0xFFE8A74A),
        onAccent = Color(0xFF0A0A0C),
        textPrimary = Color(0xFFF4F1EA),
        textMuted = Color(0xFF9A9AA6),
        cornerRadius = 20.dp,
        isLight = false,
    )
    AnimeLook.FROSTED -> AnimeLookTokens(
        background = Color(0xFF0B1220),
        surface = Color(0xFF18223A),
        surfaceHigh = Color(0xFF1F2C48),
        accent = Color(0xFF7FB8FF),
        onAccent = Color(0xFF06121F),
        textPrimary = Color(0xFFEAF2FF),
        textMuted = Color(0xFF8FA3C2),
        cornerRadius = 22.dp,
        isLight = false,
    )
    AnimeLook.EDITORIAL -> AnimeLookTokens(
        background = Color(0xFFF5F1EA),
        surface = Color(0xFFFFFFFF),
        surfaceHigh = Color(0xFFEDE7DC),
        accent = Color(0xFFC8102E),
        onAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF1B1B1B),
        textMuted = Color(0xFF6B6B6B),
        cornerRadius = 6.dp,
        isLight = true,
    )
    AnimeLook.EMBER -> AnimeLookTokens(
        background = Color(0xFF140A08),
        surface = Color(0xFF24140F),
        surfaceHigh = Color(0xFF331C15),
        accent = Color(0xFFFF5A3C),
        onAccent = Color(0xFF140A08),
        textPrimary = Color(0xFFFFEDE6),
        textMuted = Color(0xFFC4A096),
        cornerRadius = 28.dp,
        isLight = false,
    )
}

/** The anime look currently chosen in Settings. Updates live when the setting changes. */
@Composable
fun rememberAnimeLook(): AnimeLookTokens {
    val look by Injekt.get<UiPreferences>().animeLook.collectAsState()
    return remember(look) { look.tokens() }
}

