package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The black + #0A84FF (iOS system blue) palette shared by both reference mockups
 * (Mihon_iOS_26_Monochromatic_Refined.html and Mihon_iOS_26_VisionOS_Reader.html).
 * Used by all three UiMode values - they differ in surface translucency/treatment, not color.
 */
internal object IosGlassColorScheme : BaseColorScheme() {

    // rgba(28, 28, 30, 0.65) card background from the mockups, as an opaque approximation -
    // actual translucency is layered on top via alpha in SettingsGroup/SettingItem based on
    // the active UiMode, since ColorScheme itself can't carry per-mode alpha.
    private val cardSurface = Color(0xFF1C1C1E)
    private val iosBlue = Color(0xFF0A84FF)
    private val iosGreen = Color(0xFF30D158)

    override val darkScheme = darkColorScheme(
        primary = iosBlue,
        onPrimary = Color.White,
        primaryContainer = iosBlue,
        onPrimaryContainer = Color.White,
        secondary = iosGreen,
        onSecondary = Color.Black,
        secondaryContainer = iosGreen,
        onSecondaryContainer = Color.Black,
        tertiary = iosBlue,
        onTertiary = Color.White,
        tertiaryContainer = cardSurface,
        onTertiaryContainer = Color.White,
        error = Color(0xFFFF453A),
        onError = Color.White,
        errorContainer = Color(0xFFFF453A),
        onErrorContainer = Color.White,
        background = Color.Black,
        onBackground = Color.White,
        surface = cardSurface,
        onSurface = Color.White,
        surfaceVariant = Color.Black,
        onSurfaceVariant = Color(0xFFA3A3A3), // neutral-400 from the mockups' secondary text
        outline = Color(0xFF3A3A3C),
        outlineVariant = Color(0xFF2C2C2E),
        scrim = Color.Black,
        inverseSurface = Color.White,
        inverseOnSurface = Color.Black,
        inversePrimary = iosBlue,
        surfaceDim = Color.Black,
        surfaceBright = cardSurface,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0C0C0C),
        surfaceContainer = Color(0xFF131313),
        surfaceContainerHigh = Color(0xFF1B1B1B),
        surfaceContainerHighest = cardSurface,
    )

    // No light-mode equivalent in either mockup; this is a straightforward light inversion
    // of the same blue accent rather than a designed counterpart.
    override val lightScheme = lightColorScheme(
        primary = iosBlue,
        onPrimary = Color.White,
        primaryContainer = iosBlue,
        onPrimaryContainer = Color.White,
        secondary = iosGreen,
        onSecondary = Color.Black,
        secondaryContainer = iosGreen,
        onSecondaryContainer = Color.Black,
        tertiary = iosBlue,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF2F2F7),
        onTertiaryContainer = Color.Black,
        error = Color(0xFFFF3B30),
        onError = Color.White,
        errorContainer = Color(0xFFFF3B30),
        onErrorContainer = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color(0xFFF2F2F7),
        onSurface = Color.Black,
        surfaceVariant = Color.White,
        onSurfaceVariant = Color(0xFF6E6E73),
        outline = Color(0xFFD1D1D6),
        outlineVariant = Color(0xFFE5E5EA),
        scrim = Color.Black,
        inverseSurface = Color.Black,
        inverseOnSurface = Color.White,
        inversePrimary = iosBlue,
        surfaceDim = Color(0xFFF2F2F7),
        surfaceBright = Color.White,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF2F2F7),
        surfaceContainer = Color(0xFFECECEE),
        surfaceContainerHigh = Color(0xFFE5E5EA),
        surfaceContainerHighest = Color(0xFFDADADC),
    )
}
