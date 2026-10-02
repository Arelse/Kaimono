package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Everforest Dark / Light
 *
 * https://github.com/sainnhe/everforest
 */
internal object EverforestColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFA7C080),
        onPrimary = Color(0xFF1C3410),
        primaryContainer = Color(0xFF324A22),
        onPrimaryContainer = Color(0xFFC3E3A0),
        secondary = Color(0xFF7FBBB3),
        onSecondary = Color(0xFF00332F),
        secondaryContainer = Color(0xFF1B4A45),
        onSecondaryContainer = Color(0xFF9FD8CF),
        tertiary = Color(0xFFD699B6),
        onTertiary = Color(0xFF3D1F2C),
        tertiaryContainer = Color(0xFF573542),
        onTertiaryContainer = Color(0xFFF5B9D6),
        error = Color(0xFFE67E80),
        onError = Color(0xFF3D0E0F),
        errorContainer = Color(0xFF652224),
        onErrorContainer = Color(0xFFFFB3B2),
        background = Color(0xFF2D353B),
        onBackground = Color(0xFFD3C6AA),
        surface = Color(0xFF2D353B),
        onSurface = Color(0xFFD3C6AA),
        surfaceVariant = Color(0xFF3D484D),
        onSurfaceVariant = Color(0xFF9DA9A0),
        outline = Color(0xFF859289),
        outlineVariant = Color(0xFF475258),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFD3C6AA),
        inverseOnSurface = Color(0xFF2D353B),
        inversePrimary = Color(0xFF4F6B36),
        surfaceContainerLowest = Color(0xFF242B30),
        surfaceDim = Color(0xFF2D353B),
        surfaceContainerLow = Color(0xFF323A40),
        surfaceContainer = Color(0xFF363F45),
        surfaceContainerHigh = Color(0xFF414A50),
        surfaceContainerHighest = Color(0xFF4C555B),
        surfaceBright = Color(0xFF515B61),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF8DA101),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5EFB0),
        onPrimaryContainer = Color(0xFF263300),
        inversePrimary = Color(0xFFC3DA7E),
        secondary = Color(0xFF35A77C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFB6F0D4),
        onSecondaryContainer = Color(0xFF00210F),
        tertiary = Color(0xFFDF69BA),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8EC),
        onTertiaryContainer = Color(0xFF390828),
        background = Color(0xFFFFFBEF),
        onBackground = Color(0xFF5C6A72),
        surface = Color(0xFFFFFBEF),
        onSurface = Color(0xFF5C6A72),
        surfaceVariant = Color(0xFFF0E7CE),
        onSurfaceVariant = Color(0xFF5C6A72),
        surfaceTint = Color(0xFF8DA101),
        inverseSurface = Color(0xFF5C6A72),
        inverseOnSurface = Color(0xFFFFFBEF),
        outline = Color(0xFF939F8E),
        outlineVariant = Color(0xFFD7CFB5),
        error = Color(0xFFF85552),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD5),
        onErrorContainer = Color(0xFF410001),
        surfaceDim = Color(0xFFE7E0C9),
        surfaceBright = Color(0xFFFFFBEF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFAF4DD),
        surfaceContainer = Color(0xFFF4EED7),
        surfaceContainerHigh = Color(0xFFEEE8D1),
        surfaceContainerHighest = Color(0xFFE8E2CB),
    )
}

