package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Gruvbox Dark/Light
 *
 * https://github.com/morhetz/gruvbox
 */
internal object GruvboxColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFABD2F),
        onPrimary = Color(0xFF3A2E00),
        primaryContainer = Color(0xFF5C4A00),
        onPrimaryContainer = Color(0xFFFFE082),
        secondary = Color(0xFF8EC07C),
        onSecondary = Color(0xFF1C2E17),
        secondaryContainer = Color(0xFF33472A),
        onSecondaryContainer = Color(0xFFC3E8B4),
        tertiary = Color(0xFFFE8019),
        onTertiary = Color(0xFF3D1B00),
        tertiaryContainer = Color(0xFF5C2900),
        onTertiaryContainer = Color(0xFFFFCBA3),
        error = Color(0xFFFB4934),
        onError = Color(0xFF3D0000),
        errorContainer = Color(0xFF6B0000),
        onErrorContainer = Color(0xFFFFB4A9),
        background = Color(0xFF282828),
        onBackground = Color(0xFFEBDBB2),
        surface = Color(0xFF282828),
        onSurface = Color(0xFFEBDBB2),
        surfaceVariant = Color(0xFF3C3836),
        onSurfaceVariant = Color(0xFFD5C4A1),
        outline = Color(0xFFA89984),
        outlineVariant = Color(0xFF504945),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFEBDBB2),
        inverseOnSurface = Color(0xFF282828),
        inversePrimary = Color(0xFFB57614),
        surfaceContainerLowest = Color(0xFF1D2021),
        surfaceDim = Color(0xFF282828),
        surfaceContainerLow = Color(0xFF2C2A28),
        surfaceContainer = Color(0xFF32302E),
        surfaceContainerHigh = Color(0xFF3C3836),
        surfaceContainerHighest = Color(0xFF464038),
        surfaceBright = Color(0xFF504945),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFB57614),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE082),
        onPrimaryContainer = Color(0xFF3A2E00),
        inversePrimary = Color(0xFFFABD2F),
        secondary = Color(0xFF427B58),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC3E8B4),
        onSecondaryContainer = Color(0xFF1C2E17),
        tertiary = Color(0xFFAF3A03),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFCBA3),
        onTertiaryContainer = Color(0xFF3D1B00),
        background = Color(0xFFFBF1C7),
        onBackground = Color(0xFF3C3836),
        surface = Color(0xFFFBF1C7),
        onSurface = Color(0xFF3C3836),
        surfaceVariant = Color(0xFFEBDBB2),
        onSurfaceVariant = Color(0xFF3C3836),
        surfaceTint = Color(0xFFB57614),
        inverseSurface = Color(0xFF3C3836),
        inverseOnSurface = Color(0xFFFBF1C7),
        outline = Color(0xFF7C6F64),
        outlineVariant = Color(0xFFD5C4A1),
        error = Color(0xFF9D0006),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410000),
        surfaceDim = Color(0xFFE6D5A3),
        surfaceBright = Color(0xFFFBF1C7),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5EAC1),
        surfaceContainer = Color(0xFFF0E4B6),
        surfaceContainerHigh = Color(0xFFEBDBB2),
        surfaceContainerHighest = Color(0xFFE6D5A3),
    )
}
