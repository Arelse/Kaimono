package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for One Dark / One Light (Atom)
 *
 * https://github.com/atom/atom/tree/master/packages/one-dark-ui
 */
internal object OneDarkColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF61AFEF),
        onPrimary = Color(0xFF06243E),
        primaryContainer = Color(0xFF1B3A5C),
        onPrimaryContainer = Color(0xFFB8DDFB),
        secondary = Color(0xFFC678DD),
        onSecondary = Color(0xFF2E0F3D),
        secondaryContainer = Color(0xFF45215A),
        onSecondaryContainer = Color(0xFFEDC9FA),
        tertiary = Color(0xFF98C379),
        onTertiary = Color(0xFF17300B),
        tertiaryContainer = Color(0xFF2C4A1C),
        onTertiaryContainer = Color(0xFFCDE9B9),
        error = Color(0xFFE06C75),
        onError = Color(0xFF3D0007),
        errorContainer = Color(0xFF6B121B),
        onErrorContainer = Color(0xFFFFB4AC),
        background = Color(0xFF282C34),
        onBackground = Color(0xFFABB2BF),
        surface = Color(0xFF282C34),
        onSurface = Color(0xFFABB2BF),
        surfaceVariant = Color(0xFF3B4048),
        onSurfaceVariant = Color(0xFF9DA5B4),
        outline = Color(0xFF5C6370),
        outlineVariant = Color(0xFF3E4451),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFABB2BF),
        inverseOnSurface = Color(0xFF282C34),
        inversePrimary = Color(0xFF1A6FB0),
        surfaceContainerLowest = Color(0xFF20232A),
        surfaceDim = Color(0xFF282C34),
        surfaceContainerLow = Color(0xFF2C3038),
        surfaceContainer = Color(0xFF31353E),
        surfaceContainerHigh = Color(0xFF3B4048),
        surfaceContainerHighest = Color(0xFF454B55),
        surfaceBright = Color(0xFF4A505C),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF4078F2),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDBE4FF),
        onPrimaryContainer = Color(0xFF001947),
        inversePrimary = Color(0xFFB2C7FF),
        secondary = Color(0xFFA626A4),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD6FA),
        onSecondaryContainer = Color(0xFF390037),
        tertiary = Color(0xFF50A14F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC8F5C1),
        onTertiaryContainer = Color(0xFF002204),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF383A42),
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF383A42),
        surfaceVariant = Color(0xFFE5E5E6),
        onSurfaceVariant = Color(0xFF383A42),
        surfaceTint = Color(0xFF4078F2),
        inverseSurface = Color(0xFF383A42),
        inverseOnSurface = Color(0xFFFAFAFA),
        outline = Color(0xFF9A9A9B),
        outlineVariant = Color(0xFFCACACB),
        error = Color(0xFFE45649),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD4),
        onErrorContainer = Color(0xFF410001),
        surfaceDim = Color(0xFFDAD9DA),
        surfaceBright = Color(0xFFFAFAFA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF4F3F3),
        surfaceContainer = Color(0xFFEEEDED),
        surfaceContainerHigh = Color(0xFFE8E7E8),
        surfaceContainerHighest = Color(0xFFE2E1E2),
    )
}
