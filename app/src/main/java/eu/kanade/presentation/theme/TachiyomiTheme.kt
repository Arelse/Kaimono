package eu.kanade.presentation.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.getValue
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import eu.kanade.presentation.theme.colorscheme.DraculaColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.GruvboxColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.EverforestColorScheme
import eu.kanade.presentation.theme.colorscheme.OneDarkColorScheme
import eu.kanade.presentation.theme.colorscheme.RosePineColorScheme
import eu.kanade.presentation.theme.colorscheme.SolarizedColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.TokyoNightColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetCompatColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@androidx.annotation.Keep
@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    seedColor: Int? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    val prefAppTheme by uiPreferences.appTheme.collectAsState()
    val prefIsAmoled by uiPreferences.themeDarkAmoled.collectAsState()
    val prefCoverStyle by uiPreferences.themeCoverBasedStyle.collectAsState()
    BaseTachiyomiTheme(
        appTheme = appTheme ?: prefAppTheme,
        isAmoled = amoled ?: prefIsAmoled,
        seedColor = seedColor,
        coverStyle = prefCoverStyle,
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(appTheme, isAmoled, content = content)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    seedColor: Int? = null,
    coverStyle: com.materialkolor.PaletteStyle = com.materialkolor.PaletteStyle.Vibrant,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val customThemeColor by Injekt.get<UiPreferences>().customThemeColor.collectAsState()
    val appFont by Injekt.get<UiPreferences>().appFont.collectAsState()
    val uiMode by Injekt.get<UiPreferences>().uiMode.collectAsState()
    val useUiSwitcherTheme by Injekt.get<UiPreferences>().useUiSwitcherTheme.collectAsState()
    val typography = remember(appFont) { typographyFor(appFont) }
    // Rounded shape theme: any Card/Surface/Button using default MaterialTheme.shapes tokens picks
    // up rounder corners automatically, same cascading trick as colorScheme below but for shape.
    // Only active for the modes listed below; every other mode (and the legacy AppTheme picker)
    // keeps Material's normal default shapes untouched.
    val shapes = remember(uiMode, useUiSwitcherTheme) {
        // Neumorphism and Liquid Glass use the same app-wide corner treatment: their cards and
        // surfaces are the shape-bearing components, so this cascades to every Card/Surface app-wide.
        val roundedModes = setOf(
            eu.kanade.domain.ui.model.UiMode.NEUMORPHISM,
            eu.kanade.domain.ui.model.UiMode.LIQUID_GLASS,
        )
        if (useUiSwitcherTheme && uiMode in roundedModes) {
            androidx.compose.material3.Shapes(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
            )
        } else {
            androidx.compose.material3.Shapes()
        }
    }
    MaterialExpressiveTheme(
        typography = typography,
        shapes = shapes,
        colorScheme = remember(appTheme, isDark, isAmoled, seedColor, coverStyle, customThemeColor, uiMode, useUiSwitcherTheme) {
            // The UI Switcher (More screen) takes priority over the legacy AppTheme picker below,
            // unless the user has explicitly turned it off. All three UiMode values share this
            // palette, differing in translucency/treatment rather than color - see
            // IosGlassColorScheme and SettingsGroup/SettingItem.
            if (useUiSwitcherTheme) {
                val base = eu.kanade.presentation.theme.colorscheme.IosGlassColorScheme.getColorScheme(
                    isDark = true,
                    isAmoled = isAmoled,
                    overrideDarkSurfaceContainers = true,
                )
                if (uiMode == eu.kanade.domain.ui.model.UiMode.GLASSMORPHISM) {
                    // The single app-wide lever: every surface token below is translucent, not
                    // opaque. Any screen using standard Material3 Surface/Card with default
                    // colors (the vast majority of this app's screens) reads these same tokens,
                    // so this cascades everywhere automatically - not just the screens Claude
                    // has hand-styled (More screen's SettingsGroup, etc). No new dependency, no
                    // true backdrop blur (would need the Haze library) - this is alpha-based
                    // "frosted" translucency, same honest limitation as Translucent mode.
                    base.copy(
                        surface = base.surface.copy(alpha = 0.45f),
                        surfaceVariant = base.surfaceVariant.copy(alpha = 0.45f),
                        surfaceContainerLowest = base.surfaceContainerLowest.copy(alpha = 0.35f),
                        surfaceContainerLow = base.surfaceContainerLow.copy(alpha = 0.40f),
                        surfaceContainer = base.surfaceContainer.copy(alpha = 0.45f),
                        surfaceContainerHigh = base.surfaceContainerHigh.copy(alpha = 0.55f),
                        surfaceContainerHighest = base.surfaceContainerHighest.copy(alpha = 0.65f),
                        primaryContainer = base.primaryContainer.copy(alpha = 0.55f),
                        secondaryContainer = base.secondaryContainer.copy(alpha = 0.55f),
                        tertiaryContainer = base.tertiaryContainer.copy(alpha = 0.45f),
                    )
                } else if (uiMode == eu.kanade.domain.ui.model.UiMode.NEUMORPHISM) {
                    // Opaque, not translucent - that's the point. Neumorphism's identity is
                    // depth from shadow alone, against a surface that barely differs in tone
                    // from the background it sits on (see the near-black surface values below,
                    // close to base.background rather than base.surfaceContainerHighest). The
                    // elevation Claude added on the More screen does the rest of the work here.
                    val neuSurface = Color(0xFF0A0A0A)
                    base.copy(
                        surface = neuSurface,
                        surfaceVariant = neuSurface,
                        surfaceContainerLowest = Color(0xFF060606),
                        surfaceContainerLow = Color(0xFF080808),
                        surfaceContainer = neuSurface,
                        surfaceContainerHigh = Color(0xFF0D0D0D),
                        surfaceContainerHighest = Color(0xFF101010),
                    )
                } else if (uiMode == eu.kanade.domain.ui.model.UiMode.LIQUID_GLASS) {
                    // Translucent like Glassmorphism, but tinted toward the accent blue rather
                    // than neutral gray - approximates the "tinted refraction" look of Apple's
                    // Liquid Glass / the linked liquid-glass-js project. This is an honest
                    // Compose-native interpretation, not a port: that library runs its effect in
                    // a browser via WebGL + html2canvas sampling the live DOM, neither of which
                    // exists in a native Android view hierarchy, so there's nothing to literally
                    // "use" from it here. The animated specular sweep on the More screen is the
                    // other half of this mode's identity.
                    val tint = Color(0xFF0A84FF)
                    base.copy(
                        surface = androidx.compose.ui.graphics.lerp(base.surface, tint, 0.12f).copy(alpha = 0.40f),
                        surfaceVariant = androidx.compose.ui.graphics.lerp(base.surfaceVariant, tint, 0.12f).copy(alpha = 0.40f),
                        surfaceContainerLowest = androidx.compose.ui.graphics.lerp(base.surfaceContainerLowest, tint, 0.10f).copy(alpha = 0.30f),
                        surfaceContainerLow = androidx.compose.ui.graphics.lerp(base.surfaceContainerLow, tint, 0.12f).copy(alpha = 0.35f),
                        surfaceContainer = androidx.compose.ui.graphics.lerp(base.surfaceContainer, tint, 0.14f).copy(alpha = 0.40f),
                        surfaceContainerHigh = androidx.compose.ui.graphics.lerp(base.surfaceContainerHigh, tint, 0.16f).copy(alpha = 0.50f),
                        surfaceContainerHighest = androidx.compose.ui.graphics.lerp(base.surfaceContainerHighest, tint, 0.18f).copy(alpha = 0.60f),
                        primaryContainer = base.primaryContainer.copy(alpha = 0.55f),
                        secondaryContainer = base.secondaryContainer.copy(alpha = 0.55f),
                        tertiaryContainer = base.tertiaryContainer.copy(alpha = 0.40f),
                    )
                } else {
                    base
                }
            } else if (seedColor != null) {
                val scheme = MonetCompatColorScheme(Color(seedColor), coverStyle)
                scheme.getColorScheme(isDark, isAmoled, overrideDarkSurfaceContainers = false)
            } else if (appTheme == AppTheme.CUSTOM) {
                val scheme = MonetCompatColorScheme(
                    Color(customThemeColor),
                    com.materialkolor.PaletteStyle.TonalSpot,
                )
                scheme.getColorScheme(isDark, isAmoled, overrideDarkSurfaceContainers = false)
            } else {
                getThemeColorScheme(
                    context = context,
                    appTheme = appTheme,
                    isDark = isDark,
                    isAmoled = isAmoled,
                )
            }
        },
        content = content,
    )
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
    AppTheme.DRACULA to DraculaColorScheme,
    AppTheme.GRUVBOX to GruvboxColorScheme,
    AppTheme.ROSEPINE to RosePineColorScheme,
    AppTheme.ONEDARK to OneDarkColorScheme,
    AppTheme.SOLARIZED to SolarizedColorScheme,
    AppTheme.EVERFOREST to EverforestColorScheme,
)

private fun typographyFor(font: eu.kanade.domain.ui.model.AppFont): androidx.compose.material3.Typography {
    val base = androidx.compose.material3.Typography()
    val family = when (font) {
        eu.kanade.domain.ui.model.AppFont.DEFAULT -> return base
        eu.kanade.domain.ui.model.AppFont.SANS_SERIF -> androidx.compose.ui.text.font.FontFamily.SansSerif
        eu.kanade.domain.ui.model.AppFont.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
        eu.kanade.domain.ui.model.AppFont.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
        eu.kanade.domain.ui.model.AppFont.CURSIVE -> androidx.compose.ui.text.font.FontFamily.Cursive
    }
    return androidx.compose.material3.Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
