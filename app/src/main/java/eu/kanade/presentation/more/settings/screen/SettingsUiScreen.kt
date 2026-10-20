package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.NavBarStyle
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.NavTabReorderDialog
import eu.kanade.tachiyomi.ui.home.NavTabKeys
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Card styles, nav bar behavior, fonts, and animation tuning - split out from Settings >
 * Appearance into its own top-level category (was previously the "UI settings" group nested in
 * SettingsAppearanceScreen; moved here as-is, group title included, so this reads the same to
 * someone who used it before).
 */
object SettingsUiScreen : SearchableSettings {

    override val supportsReset: Boolean get() = true

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_ui_settings

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val libraryPreferences = remember { Injekt.get<tachiyomi.domain.library.service.LibraryPreferences>() }

        return listOf(
            getUiSettingsGroup(uiPreferences = uiPreferences, libraryPreferences = libraryPreferences),
        )
    }

    @Composable
    private fun getUiSettingsGroup(
        uiPreferences: UiPreferences,
        libraryPreferences: tachiyomi.domain.library.service.LibraryPreferences,
    ): Preference.PreferenceGroup {
        val basePreferences = remember { Injekt.get<eu.kanade.domain.base.BasePreferences>() }
        val isJoined by libraryPreferences.joinedLibrary.collectAsState()
        val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
        val joined = isJoined || hideMangaUi

        val tabOrderPref = uiPreferences.navTabOrder
        val tabOrder by tabOrderPref.collectAsState()
        val hiddenTabsPref = uiPreferences.navHiddenTabs
        val hiddenTabs by hiddenTabsPref.collectAsState()
        var showReorder by remember { mutableStateOf(false) }
        val navMarginPref = uiPreferences.navBarMargin
        val navMargin by navMarginPref.collectAsState()
        val roundnessPref = uiPreferences.cardRoundness
        val roundness by roundnessPref.collectAsState()
        val glowPref = uiPreferences.glowMultiplier
        val glow by glowPref.collectAsState()
        val animPref = uiPreferences.cardAnimationMs
        val anim by animPref.collectAsState()

        if (showReorder) {
            NavTabReorderDialog(
                initialOrder = NavTabKeys.ordered(NavTabKeys.defaultKeys(joined), tabOrder),
                initialHidden = hiddenTabs,
                lockedKey = NavTabKeys.MORE,
                labelFor = { NavTabKeys.label(it, joined) },
                onDismiss = { showReorder = false },
                onSave = { order, hidden ->
                    tabOrderPref.set(order.joinToString(","))
                    hiddenTabsPref.set(hidden)
                    showReorder = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = "UI settings",
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.libraryCardStyle,
                    entries = mapOf(
                        eu.kanade.domain.ui.model.LibraryCardStyle.DEFAULT to "Default",
                        eu.kanade.domain.ui.model.LibraryCardStyle.SAIKOU to "Saikou",
                        eu.kanade.domain.ui.model.LibraryCardStyle.EXOTIC to "Exotic",
                        eu.kanade.domain.ui.model.LibraryCardStyle.MINIMAL_EXOTIC to "Minimal exotic",
                    ),
                    title = "Library card style",
                    subtitle = "%s (comfortable grid only)",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.historyCardStyle,
                    entries = mapOf(
                        eu.kanade.domain.ui.model.HistoryCardStyle.REGULAR to "Regular",
                        eu.kanade.domain.ui.model.HistoryCardStyle.FROSTED_GLASS to "Frosted glass",
                        eu.kanade.domain.ui.model.HistoryCardStyle.BOOTIFUL to "Bootiful",
                    ),
                    title = "History card style",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.navBarStyle,
                    entries = mapOf(
                        NavBarStyle.CLASSIC to "Classic",
                        NavBarStyle.DYNAMIC_PILL to "Dynamic pill",
                    ),
                    title = "Nav bar style",
                    subtitle = "Overridden by the \"Navigation bar\" toggle in More > UI Switcher",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.homeLayout,
                    entries = mapOf(
                        eu.kanade.domain.ui.model.HomeLayout.DEFAULT to "Default",
                        eu.kanade.domain.ui.model.HomeLayout.MEDIA_HUB to "Featured",
                    ),
                    title = "Home screen layout",
                    subtitle = "Default keeps the classic Home. Featured adds search, filters, and the palette, sync, trophy and account sheets",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.homeScreenStyle,
                    entries = mapOf(
                        eu.kanade.domain.ui.model.HomeScreenStyle.CLASSIC to "Classic",
                        eu.kanade.domain.ui.model.HomeScreenStyle.AURORA_VIOLET to "Aurora Violet",
                        eu.kanade.domain.ui.model.HomeScreenStyle.NEBULA_GLASS to "Nebula Glass",
                        eu.kanade.domain.ui.model.HomeScreenStyle.PRISM_AURORA to "Prism Aurora",
                        eu.kanade.domain.ui.model.HomeScreenStyle.MIDNIGHT_INDIGO to "Midnight Indigo",
                        eu.kanade.domain.ui.model.HomeScreenStyle.ECLIPSE_NOIR to "Eclipse Noir",
                    ),
                    title = "Home screen style",
                    subtitle = "Re-skins the Home tab",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.homeStyleNavBar,
                    entries = mapOf(
                        eu.kanade.domain.ui.model.HomeScreenStyle.CLASSIC to "Classic",
                        eu.kanade.domain.ui.model.HomeScreenStyle.AURORA_VIOLET to "Aurora Violet",
                        eu.kanade.domain.ui.model.HomeScreenStyle.NEBULA_GLASS to "Nebula Glass",
                        eu.kanade.domain.ui.model.HomeScreenStyle.PRISM_AURORA to "Prism Aurora",
                        eu.kanade.domain.ui.model.HomeScreenStyle.MIDNIGHT_INDIGO to "Midnight Indigo",
                        eu.kanade.domain.ui.model.HomeScreenStyle.ECLIPSE_NOIR to "Eclipse Noir",
                    ),
                    title = "Home Style Nav Bar",
                    subtitle = "Re-skins the floating nav bar",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.translucentNav,
                    title = "Translucent nav",
                    subtitle = "Let the background show through the navigation bar",
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.immersiveMode,
                    title = "Immersive mode",
                    subtitle = "Hide system status and navigation bars",
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.appFont,
                    entries = mapOf(
                        eu.kanade.domain.ui.model.AppFont.DEFAULT to "Default",
                        eu.kanade.domain.ui.model.AppFont.SANS_SERIF to "Sans serif",
                        eu.kanade.domain.ui.model.AppFont.SERIF to "Serif",
                        eu.kanade.domain.ui.model.AppFont.MONOSPACE to "Monospace",
                        eu.kanade.domain.ui.model.AppFont.CURSIVE to "Cursive",
                    ),
                    title = "Font family",
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = navMargin,
                    valueRange = 0..48,
                    title = "Bottom nav bar margin",
                    subtitle = "Horizontal space around the navigation bar",
                    onValueChanged = { navMarginPref.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = roundness,
                    valueRange = 0..200,
                    title = "Card roundness",
                    subtitle = "Corner roundness of library and history cards, in %",
                    onValueChanged = { roundnessPref.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = glow,
                    valueRange = 0..300,
                    title = "Glow multiplier",
                    subtitle = "Glow strength of Exotic library cards, in %",
                    onValueChanged = { glowPref.set(it) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = anim,
                    valueRange = 50..500,
                    title = "Card animation duration",
                    subtitle = "Tap animation speed in milliseconds",
                    onValueChanged = { animPref.set(it) },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = "Reorder navigation tabs",
                    subtitle = "Change tab order and hide tabs you don't use",
                    onClick = { showReorder = true },
                ),
            ),
        )
    }
}
