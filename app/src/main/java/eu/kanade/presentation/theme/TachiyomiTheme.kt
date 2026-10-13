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
    // Spatial UI's lever is the shape theme rather than color: any Card/Surface/Button using
    // default MaterialTheme.shapes tokens (most of the app) picks up rounder, more "floating
    // panel" corners automatically - same cascading trick as colorScheme below, just for shape
    // instead of color. Only active when Spatial is actually selected; every other mode (and the
    // legacy AppTheme picker) keeps Material's normal default shapes untouched.
    val shapes = remember(uiMode, useUiSwitcherTheme) {
        if (useUiSwitcherTheme && uiMode == eu.kanade.domain.ui.model.UiMode.SPATIAL) {
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
                } else if (uiMode == eu.kanade.domain.ui.model.UiMode.SPATIAL) {
                    // More opaque than Glassmorphism on purpose - Spatial's identity is depth
                    // and rounded floating panels (see the shapes override above and the
                    // elevation Claude added on the More screen), not see-through-ness. A light
                    // translucency here still lets it read as "layered" rather than flat/solid.
                    base.copy(
                        surface = base.surface.copy(alpha = 0.72f),
                        surfaceVariant = base.surfaceVariant.copy(alpha = 0.72f),
                        surfaceContainerLowest = base.surfaceContainerLowest.copy(alpha = 0.60f),
                        surfaceContainerLow = base.surfaceContainerLow.copy(alpha = 0.66f),
                        surfaceContainer = base.surfaceContainer.copy(alpha = 0.72f),
                        surfaceContainerHigh = base.surfaceContainerHigh.copy(alpha = 0.80f),
                        surfaceContainerHighest = base.surfaceContainerHighest.copy(alpha = 0.88f),
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
