package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Rosé Pine / Rosé Pine Dawn
 *
 * https://rosepinetheme.com/
 */
internal object RosePineColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFC4A7E7),
        onPrimary = Color(0xFF2A1B3D),
        primaryContainer = Color(0xFF3E2E56),
        onPrimaryContainer = Color(0xFFE5D4FA),
        secondary = Color(0xFF9CCFD8),
        onSecondary = Color(0xFF0A2E33),
        secondaryContainer = Color(0xFF1B454C),
        onSecondaryContainer = Color(0xFFC2ECF2),
        tertiary = Color(0xFFEBBCBA),
        onTertiary = Color(0xFF3D1F1E),
        tertiaryContainer = Color(0xFF573433),
        onTertiaryContainer = Color(0xFFFFDAD8),
        error = Color(0xFFEB6F92),
        onError = Color(0xFF3D0016),
        errorContainer = Color(0xFF5C1230),
        onErrorContainer = Color(0xFFFFD9E1),
        background = Color(0xFF191724),
        onBackground = Color(0xFFE0DEF4),
        surface = Color(0xFF191724),
        onSurface = Color(0xFFE0DEF4),
        surfaceVariant = Color(0xFF26233A),
        onSurfaceVariant = Color(0xFF908CAA),
        outline = Color(0xFF6E6A86),
        outlineVariant = Color(0xFF403D52),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE0DEF4),
        inverseOnSurface = Color(0xFF191724),
        inversePrimary = Color(0xFF7A5CA3),
        surfaceContainerLowest = Color(0xFF141220),
        surfaceDim = Color(0xFF191724),
        surfaceContainerLow = Color(0xFF1C1A29),
        surfaceContainer = Color(0xFF1F1D2E),
        surfaceContainerHigh = Color(0xFF26233A),
        surfaceContainerHighest = Color(0xFF2E2A44),
        surfaceBright = Color(0xFF332F4D),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF907AA9),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEBE0F5),
        onPrimaryContainer = Color(0xFF332448),
        inversePrimary = Color(0xFFC4A7E7),
        secondary = Color(0xFF286983),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD0E9F0),
        onSecondaryContainer = Color(0xFF072B36),
        tertiary = Color(0xFFD7827E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFBDEDC),
        onTertiaryContainer = Color(0xFF3D1615),
        background = Color(0xFFFAF4ED),
        onBackground = Color(0xFF575279),
        surface = Color(0xFFFAF4ED),
        onSurface = Color(0xFF575279),
        surfaceVariant = Color(0xFFF2E9E1),
        onSurfaceVariant = Color(0xFF797593),
        surfaceTint = Color(0xFF907AA9),
        inverseSurface = Color(0xFF575279),
        inverseOnSurface = Color(0xFFFAF4ED),
        outline = Color(0xFF9893A5),
        outlineVariant = Color(0xFFDFDAD9),
        error = Color(0xFFB4637A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDCE1),
        onErrorContainer = Color(0xFF3D0016),
        surfaceDim = Color(0xFFE7DCD0),
        surfaceBright = Color(0xFFFAF4ED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6EFE7),
        surfaceContainer = Color(0xFFF2E9E1),
        surfaceContainerHigh = Color(0xFFEDE3D9),
        surfaceContainerHighest = Color(0xFFE7DCD0),
    )
}
