package eu.kanade.domain.ui

import com.materialkolor.PaletteStyle
import eu.kanade.domain.ui.model.AppTheme
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

    val bloomEnabled: Preference<Boolean> = preferenceStore.getBoolean("pref_bloom_enabled", true)

    val grainOverlayEnabled: Preference<Boolean> = preferenceStore.getBoolean("pref_grain_overlay_enabled", false)

    val settingsLiquidMode: Preference<Boolean> = preferenceStore.getBoolean("pref_settings_liquid_mode", true)

    val readerUseLiquidBackground: Preference<Boolean> = preferenceStore.getBoolean("pref_reader_use_liquid_background", false)

    val settingsBackgroundRetainOriginalColor: Preference<Boolean> = preferenceStore.getBoolean("pref_settings_background_retain_color", true)

    val settingsBackgroundPath: Preference<String> = preferenceStore.getString("pref_settings_background_path", "")

    val settingsBackgroundBlur: Preference<Int> = preferenceStore.getInt("pref_settings_background_blur", 12)

    val settingsBackgroundLight: Preference<Int> = preferenceStore.getInt("pref_settings_background_light", 65)

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}

