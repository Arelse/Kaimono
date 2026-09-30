package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Dracula
 *
 * https://draculatheme.com/contribute
 */
internal object DraculaColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFBD93F9),
        onPrimary = Color(0xFF1E1F29),
        primaryContainer = Color(0xFF44475A),
        onPrimaryContainer = Color(0xFFD6B8FF),
        secondary = Color(0xFF8BE9FD),
        onSecondary = Color(0xFF0F2B30),
        secondaryContainer = Color(0xFF1B4750),
        onSecondaryContainer = Color(0xFFC2F4FF),
        tertiary = Color(0xFFFF79C6),
        onTertiary = Color(0xFF3A0A28),
        tertiaryContainer = Color(0xFF5C1240),
        onTertiaryContainer = Color(0xFFFFB3E6),
        error = Color(0xFFFF5555),
        onError = Color(0xFF3A0000),
        errorContainer = Color(0xFF8B0000),
        onErrorContainer = Color(0xFFFFB4B4),
        background = Color(0xFF282A36),
        onBackground = Color(0xFFF8F8F2),
        surface = Color(0xFF282A36),
        onSurface = Color(0xFFF8F8F2),
        surfaceVariant = Color(0xFF44475A),
        onSurfaceVariant = Color(0xFFBFC2D6),
        outline = Color(0xFF6272A4),
        outlineVariant = Color(0xFF44475A),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF8F8F2),
        inverseOnSurface = Color(0xFF282A36),
        inversePrimary = Color(0xFF7A57C9),
        surfaceContainerLowest = Color(0xFF20222C),
        surfaceDim = Color(0xFF282A36),
        surfaceContainerLow = Color(0xFF2C2E3B),
        surfaceContainer = Color(0xFF313342),
        surfaceContainerHigh = Color(0xFF3B3D4E),
        surfaceContainerHighest = Color(0xFF454759),
        surfaceBright = Color(0xFF4A4D62),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF7A57C9),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE4D7FF),
        onPrimaryContainer = Color(0xFF3A1F6E),
        inversePrimary = Color(0xFFBD93F9),
        secondary = Color(0xFF0E7C91),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC2F4FF),
        onSecondaryContainer = Color(0xFF00363F),
        tertiary = Color(0xFFC23B85),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8EE),
        onTertiaryContainer = Color(0xFF5C1240),
        background = Color(0xFFF8F8F2),
        onBackground = Color(0xFF282A36),
        surface = Color(0xFFF8F8F2),
        onSurface = Color(0xFF282A36),
        surfaceVariant = Color(0xFFE3E1EC),
        onSurfaceVariant = Color(0xFF44475A),
        surfaceTint = Color(0xFF7A57C9),
        inverseSurface = Color(0xFF282A36),
        inverseOnSurface = Color(0xFFF8F8F2),
        outline = Color(0xFF6272A4),
        outlineVariant = Color(0xFFC7C6D0),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        surfaceDim = Color(0xFFD9D7E0),
        surfaceBright = Color(0xFFF8F8F2),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF2F0FA),
        surfaceContainer = Color(0xFFECEAF4),
        surfaceContainerHigh = Color(0xFFE6E4EE),
        surfaceContainerHighest = Color(0xFFE0DEE8),
    )
}
