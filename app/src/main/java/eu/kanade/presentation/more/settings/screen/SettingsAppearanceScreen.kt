package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AccentColor
import eu.kanade.domain.ui.model.ParticleEffect
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.MR
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Clock

object SettingsAppearanceScreen : SearchableSettings {

    override val supportsReset: Boolean get() = true

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val libraryPreferences = remember { Injekt.get<tachiyomi.domain.library.service.LibraryPreferences>() }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
            getMangaInfoGroup(uiPreferences = uiPreferences),
            getFlourishGroup(uiPreferences = uiPreferences),
            getSettingsBackgroundGroup(uiPreferences = uiPreferences),
            getLibraryLayoutGroup(libraryPreferences = libraryPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        val themeCoverBasedPref = uiPreferences.themeCoverBased
        val themeCoverBased by themeCoverBasedPref.collectAsState()

        val bloomPref = uiPreferences.bloomEnabled
        val grainPref = uiPreferences.grainOverlayEnabled

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
                    Column {
                        AppThemeModePreferenceWidget(
                            value = themeMode,
                            onItemClick = {
                                themeModePref.set(it)
                                setAppCompatDelegateThemeMode(it)
                            },
                        )

                        AppThemePreferenceWidget(
                            value = appTheme,
                            amoled = amoled,
                            onItemClick = { appThemePref.set(it) },
                        )
                    }
                },
                Preference.PreferenceItem.SwitchPreference(
                    preference = amoledPref,
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                    enabled = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = themeCoverBasedPref,
                    title = "Theme based on cover",
                    subtitle = "Color the manga details screen using the cover's dominant color",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.themeCoverBasedStyle,
                    entries = com.materialkolor.PaletteStyle.entries
                        .associateWith { it.name },
                    title = "Cover based theme style",
                    enabled = themeCoverBased,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = bloomPref,
                    title = "Bloom",
                    subtitle = "Enables a soft, glowing gradient effect",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = grainPref,
                    title = "Grain texture overlay",
                    subtitle = "Apply a subtle film grain texture over the interface",
                ),
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime() }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val formattedNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat).format(now)
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode,
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat,
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        },
                    title = stringResource(MR.strings.pref_date_format),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.relativeTime,
                    title = stringResource(MR.strings.pref_relative_format),
                    subtitle = stringResource(
                        MR.strings.pref_relative_format_summary,
                        stringResource(MR.strings.relative_time_today),
                        formattedNow,
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription,
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
            ),
        )
    }

    @Composable
    private fun getMangaInfoGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = "Manga info",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.usePanoramaCoverMangaInfo,
                    title = "Panorama Cover",
                    subtitle = "Show cover in landscape mode if it's a wide image",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.topAlignCover,
                    title = "Align Cover to Top",
                    subtitle = "Show the cover aligned to the top alongside manga info",
                ),
            ),
        )
    }

    @Composable
    private fun getSettingsBackgroundGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val liquidModePref = uiPreferences.settingsLiquidMode
        val liquidMode by liquidModePref.collectAsState()

        val backgroundPathPref = uiPreferences.settingsBackgroundPath
        val backgroundPath by backgroundPathPref.collectAsState()
        val hasBackground = backgroundPath.isNotBlank()

        val backgroundBlurPref = uiPreferences.settingsBackgroundBlur
        val backgroundBlur by backgroundBlurPref.collectAsState()

        val backgroundLightPref = uiPreferences.settingsBackgroundLight
        val backgroundLight by backgroundLightPref.collectAsState()

        val retainOriginalColorPref = uiPreferences.settingsBackgroundRetainOriginalColor
        val retainOriginalColor by retainOriginalColorPref.collectAsState()

        // Same preference as "Theme based on cover" in the Theme group above - one
        // switch, shown in both places, so the two stay in sync rather than drifting.
        val usePosterColorPref = uiPreferences.themeCoverBased

        val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    runCatching {
                        val destination = java.io.File(context.filesDir, "settings_background.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destination.outputStream().use { output -> input.copyTo(output) }
                        }
                        destination.absolutePath
                    }.getOrNull()
                }
                if (savedPath != null) {
                    backgroundPathPref.set(savedPath)
                }
            }
        }

        return Preference.PreferenceGroup(
            title = "Liquid mode",
            preferenceItems = buildList {
                add(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = liquidModePref,
                        title = "Liquid mode",
                        subtitle = if (liquidMode) "Settings screens show a blurred background" else "Solid background",
                    ),
                )
                if (liquidMode) {
                    add(
                        Preference.PreferenceItem.TextPreference(
                            title = if (hasBackground) "Change liquid background" else "Liquid background",
                            subtitle = if (hasBackground) "Tap to pick a different image" else "Choose an image to show behind every settings screen",
                            onClick = { pickImage.launch("image/*") },
                        ),
                    )
                    add(
                        Preference.PreferenceItem.SliderPreference(
                            value = backgroundBlur,
                            valueRange = 0..25,
                            title = "Blur intensity",
                            enabled = hasBackground,
                            onValueChanged = { backgroundBlurPref.set(it) },
                        ),
                    )
                    add(
                        Preference.PreferenceItem.SliderPreference(
                            value = backgroundLight,
                            valueRange = 0..100,
                            title = "Light intensity",
                            subtitle = "How bright the background shows through",
                            enabled = hasBackground,
                            onValueChanged = { backgroundLightPref.set(it) },
                        ),
                    )
                    add(
                        Preference.PreferenceItem.SwitchPreference(
                            preference = retainOriginalColorPref,
                            title = "Retain original color",
                            subtitle = "Off tints the background with your app theme's color instead",
                            enabled = hasBackground,
                        ),
                    )
                    add(
                        Preference.PreferenceItem.SwitchPreference(
                            preference = usePosterColorPref,
                            title = "Use poster color",
                            subtitle = "Applies the manga cover's color on the details page",
                        ),
                    )
                    add(
                        Preference.PreferenceItem.TextPreference(
                            title = "Reset to default picture",
                            subtitle = "Remove the liquid background",
                            enabled = hasBackground,
                            onClick = { backgroundPathPref.set("") },
                        ),
                    )
                }
            },
        )
    }

    @Composable
    private fun getFlourishGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val accentGradientEnabledPref = uiPreferences.accentGradientEnabled
        val accentGradientEnabled by accentGradientEnabledPref.collectAsState()

        return Preference.PreferenceGroup(
            title = "Flourish",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = accentGradientEnabledPref,
                    title = "Accent gradient background",
                    subtitle = "A slow flowing color gradient, independent of your app theme",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.accentColor,
                    entries = mapOf(
                        AccentColor.EMBER to "Ember",
                        AccentColor.MINT to "Mint",
                        AccentColor.ROYAL to "Royal",
                        AccentColor.VOID to "Void",
                        AccentColor.WALLPAPER to "Wallpaper",
                    ),
                    title = "Accent color",
                    enabled = accentGradientEnabled,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.particleEffect,
                    entries = mapOf(
                        ParticleEffect.NONE to "None",
                        ParticleEffect.SNOW to "Snow",
                        ParticleEffect.RAIN to "Rain",
                        ParticleEffect.STARS to "Stars",
                        ParticleEffect.SAKURA to "Sakura",
                        ParticleEffect.FIREFLIES to "Fireflies",
                        ParticleEffect.HEARTS to "Hearts",
                        ParticleEffect.EMBERS to "Embers",
                    ),
                    title = "Particle effect",
                    subtitle = "An animated overlay, works on its own or with the gradient above",
                ),
            ),
        )
    }

    @Composable
    private fun getLibraryLayoutGroup(
        libraryPreferences: tachiyomi.domain.library.service.LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val basePreferences = remember { Injekt.get<eu.kanade.domain.base.BasePreferences>() }
        return Preference.PreferenceGroup(
            title = "Library layout",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.joinedLibrary,
                    title = "Combined library",
                    subtitle = "Merge Novels and Manga into a single Library tab",
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
            ),
        )
    }
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)


             
