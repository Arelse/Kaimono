package eu.kanade.domain.ui

import com.materialkolor.PaletteStyle
import eu.kanade.domain.ui.model.AccentColor
import eu.kanade.domain.ui.model.AppFont
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.HistoryCardStyle
import eu.kanade.domain.ui.model.HomeScreenStyle
import eu.kanade.domain.ui.model.LibraryCardStyle
import eu.kanade.domain.ui.model.NavBarStyle
import eu.kanade.domain.ui.model.ParticleEffect
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    preferenceStore: PreferenceStore,
) {

    val themeMode: Preference<ThemeMode> = preferenceStore.getEnum("pref_theme_mode_key", ThemeMode.DARK)

    val appTheme: Preference<AppTheme> = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) {
            AppTheme.MONET
        } else {
            AppTheme.DEFAULT
        },
    )

    val themeDarkAmoled: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)

    val fabSizeDp: Preference<Int> = preferenceStore.getInt("pref_fab_size_dp", 68)

    val relativeTime: Preference<Boolean> = preferenceStore.getBoolean("relative_time_v2", true)

    val dateFormat: Preference<String> = preferenceStore.getString("app_date_format", "")

    val tabletUiMode: Preference<TabletUiMode> = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    val imagesInDescription: Preference<Boolean> = preferenceStore.getBoolean("pref_render_images_description", true)

    val themeCoverBased: Preference<Boolean> = preferenceStore.getBoolean("pref_theme_cover_based_key", true)

    val themeCoverBasedStyle: Preference<PaletteStyle> = preferenceStore.getEnum("pref_theme_cover_based_style_key", PaletteStyle.Vibrant)

    val usePanoramaCoverMangaInfo: Preference<Boolean> = preferenceStore.getBoolean("pref_panorama_cover_manga_info", false)

    val topAlignCover: Preference<Boolean> = preferenceStore.getBoolean("pref_top_align_cover", false)

    val customThemeColor: Preference<Int> = preferenceStore.getInt("pref_custom_theme_color", 0xFF7F77DD.toInt())

    val libraryCardStyle: Preference<LibraryCardStyle> = preferenceStore.getEnum("pref_library_card_style", LibraryCardStyle.DEFAULT)

    val historyCardStyle: Preference<HistoryCardStyle> = preferenceStore.getEnum("pref_history_card_style", HistoryCardStyle.REGULAR)

    val translucentNav: Preference<Boolean> = preferenceStore.getBoolean("pref_translucent_nav", true)

    val immersiveMode: Preference<Boolean> = preferenceStore.getBoolean("pref_immersive_mode", false)

    val navBarMargin: Preference<Int> = preferenceStore.getInt("pref_nav_bar_margin", 16)

    // Percentages, 100 = default look.
    val cardRoundness: Preference<Int> = preferenceStore.getInt("pref_card_roundness", 100)

    val glowMultiplier: Preference<Int> = preferenceStore.getInt("pref_glow_multiplier", 100)

    val cardAnimationMs: Preference<Int> = preferenceStore.getInt("pref_card_animation_ms", 200)

    val appFont: Preference<AppFont> = preferenceStore.getEnum("pref_app_font", AppFont.DEFAULT)

    val navBarStyle: Preference<NavBarStyle> = preferenceStore.getEnum("pref_nav_bar_style", NavBarStyle.CLASSIC)

    // Home Screen Switcher: re-skins the Home tab + floating nav bar. CLASSIC = app theme, unchanged.
    val homeScreenStyle: Preference<HomeScreenStyle> =
        preferenceStore.getEnum("pref_home_screen_style", HomeScreenStyle.CLASSIC)

    // Comma-separated tab keys in display order, e.g. "home,library,history,browse,more".
    // Empty means default order.
    val navTabOrder: Preference<String> = preferenceStore.getString("pref_nav_tab_order", "")

    val navHiddenTabs: Preference<Set<String>> = preferenceStore.getStringSet("pref_nav_hidden_tabs", emptySet())

    val accentGradientEnabled: Preference<Boolean> = preferenceStore.getBoolean("pref_accent_gradient_enabled", false)

    val accentColor: Preference<AccentColor> = preferenceStore.getEnum("pref_accent_color", AccentColor.EMBER)

    val particleEffect: Preference<ParticleEffect> = preferenceStore.getEnum("pref_particle_effect", ParticleEffect.NONE)

    val bloomEnabled: Preference<Boolean> = preferenceStore.getBoolean("pref_bloom_enabled", true)

    val grainOverlayEnabled: Preference<Boolean> = preferenceStore.getBoolean("pref_grain_overlay_enabled", false)

    val settingsLiquidMode: Preference<Boolean> = preferenceStore.getBoolean("pref_settings_liquid_mode", true)

    val readerUseLiquidBackground: Preference<Boolean> = preferenceStore.getBoolean("pref_reader_use_liquid_background", false)

    val readerBackgroundBlur: Preference<Int> = preferenceStore.getInt("pref_reader_background_blur", 20)

    val readerBackgroundLight: Preference<Int> = preferenceStore.getInt("pref_reader_background_light", 50)

    val settingsBackgroundRetainOriginalColor: Preference<Boolean> = preferenceStore.getBoolean("pref_settings_background_retain_color", true)

    val settingsBackgroundPath: Preference<String> = preferenceStore.getString("pref_settings_background_path", "")

    val settingsBackgroundBlur: Preference<Int> = preferenceStore.getInt("pref_settings_background_blur", 12)

    val settingsBackgroundLight: Preference<Int> = preferenceStore.getInt("pref_settings_background_light", 65)

    val profileShowRecentlyRead: Preference<Boolean> = preferenceStore.getBoolean("pref_profile_show_recently_read", true)

    val profileShowGenres: Preference<Boolean> = preferenceStore.getBoolean("pref_profile_show_genres", true)

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}

