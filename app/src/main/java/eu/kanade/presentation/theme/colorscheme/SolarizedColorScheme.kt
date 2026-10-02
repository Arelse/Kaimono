package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Solarized Dark / Light
 *
 * https://ethanschoonover.com/solarized/
 */
internal object SolarizedColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF268BD2),
        onPrimary = Color(0xFF00304D),
        primaryContainer = Color(0xFF004A73),
        onPrimaryContainer = Color(0xFFCDE5FF),
        secondary = Color(0xFF2AA198),
        onSecondary = Color(0xFF003733),
        secondaryContainer = Color(0xFF00504A),
        onSecondaryContainer = Color(0xFFA6F2E9),
        tertiary = Color(0xFFB58900),
        onTertiary = Color(0xFF3E2D00),
        tertiaryContainer = Color(0xFF594100),
        onTertiaryContainer = Color(0xFFFFDF9E),
        error = Color(0xFFDC322F),
        onError = Color(0xFF3D0003),
        errorContainer = Color(0xFF6B0F0C),
        onErrorContainer = Color(0xFFFFB4AA),
        background = Color(0xFF002B36),
        onBackground = Color(0xFF93A1A1),
        surface = Color(0xFF002B36),
        onSurface = Color(0xFF93A1A1),
        surfaceVariant = Color(0xFF073642),
        onSurfaceVariant = Color(0xFF839496),
        outline = Color(0xFF586E75),
        outlineVariant = Color(0xFF073642),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF93A1A1),
        inverseOnSurface = Color(0xFF002B36),
        inversePrimary = Color(0xFF0969A2),
        surfaceContainerLowest = Color(0xFF00212A),
        surfaceDim = Color(0xFF002B36),
        surfaceContainerLow = Color(0xFF06303B),
        surfaceContainer = Color(0xFF0A3540),
        surfaceContainerHigh = Color(0xFF153F4A),
        surfaceContainerHighest = Color(0xFF1F4954),
        surfaceBright = Color(0xFF254F5A),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF268BD2),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCDE5FF),
        onPrimaryContainer = Color(0xFF001E31),
        inversePrimary = Color(0xFF9ACBFF),
        secondary = Color(0xFF2AA198),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFA6F2E9),
        onSecondaryContainer = Color(0xFF00201D),
        tertiary = Color(0xFFB58900),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDF9E),
        onTertiaryContainer = Color(0xFF271A00),
        background = Color(0xFFFDF6E3),
        onBackground = Color(0xFF586E75),
        surface = Color(0xFFFDF6E3),
        onSurface = Color(0xFF586E75),
        surfaceVariant = Color(0xFFEEE8D5),
        onSurfaceVariant = Color(0xFF586E75),
        surfaceTint = Color(0xFF268BD2),
        inverseSurface = Color(0xFF586E75),
        inverseOnSurface = Color(0xFFFDF6E3),
        outline = Color(0xFF839496),
        outlineVariant = Color(0xFFD4CEBB),
        error = Color(0xFFDC322F),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD5),
        onErrorContainer = Color(0xFF410002),
        surfaceDim = Color(0xFFDDD7C5),
        surfaceBright = Color(0xFFFDF6E3),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F1DE),
        surfaceContainer = Color(0xFFF1EBD8),
        surfaceContainerHigh = Color(0xFFEBE5D3),
        surfaceContainerHighest = Color(0xFFE6DFCD),
    )
}
