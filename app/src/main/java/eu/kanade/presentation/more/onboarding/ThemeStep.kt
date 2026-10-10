package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import tachiyomi.i18n.novel.TDMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ThemeStep : OnboardingStep {

    override val isComplete: Boolean = true
    override val icon: ImageVector = Icons.Default.PhoneAndroid
    override val title: String = "Welcome!"

    private val uiPreferences: UiPreferences = Injekt.get()

    @Composable
    override fun Content() {
        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val hideMangaUiPref = basePreferences.hideMangaUi
        val hideMangaUi by hideMangaUiPref.collectAsState()

        // Palette choices shown in onboarding: the three "named" palettes closest to the
        // reference design's Lavender / Dynamic / Catppuccin trio. Dynamic (Material You /
        // wallpaper-derived color) is MONET under the hood, and is skipped on devices that
        // don't support it - same guard AppThemePreferenceWidget already uses in Settings.
        val paletteChoices = remember {
            listOfNotNull(
                AppTheme.LAVENDER,
                AppTheme.MONET.takeIf { DeviceUtil.isDynamicColorAvailable },
                AppTheme.CATPPUCCIN,
            )
        }

        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemeModeCard(
                    label = "System",
                    selected = themeMode == ThemeMode.SYSTEM,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        themeModePref.set(ThemeMode.SYSTEM)
                        setAppCompatDelegateThemeMode(ThemeMode.SYSTEM)
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE4E2E6)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF141318)))
                    }
                }
                ThemeModeCard(
                    label = "Light",
                    selected = themeMode == ThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        themeModePref.set(ThemeMode.LIGHT)
                        setAppCompatDelegateThemeMode(ThemeMode.LIGHT)
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE4E2E6)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFF4B5C92))
                    }
                }
                ThemeModeCard(
                    label = "Dark",
                    selected = themeMode == ThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        themeModePref.set(ThemeMode.DARK)
                        setAppCompatDelegateThemeMode(ThemeMode.DARK)
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141318)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFFB6C4FF))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "COLOR STYLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                paletteChoices.forEach { palette ->
                    PaletteCard(
                        palette = palette,
                        amoled = amoled,
                        selected = appTheme == palette,
                        modifier = Modifier.weight(1f),
                        onClick = { appThemePref.set(palette) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hideMangaUiPref.set(!hideMangaUi) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = hideMangaUi,
                    onCheckedChange = { hideMangaUiPref.set(it) },
                )
                Text(
                    text = stringResource(TDMR.strings.pref_hide_manga_ui),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        preview()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun PaletteCard(
    palette: AppTheme,
    amoled: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Wrapping the preview in this palette's own TachiyomiTheme means every color below is
        // that palette's *real* scheme, not an invented approximation - the same mechanism
        // Settings' own theme picker (AppThemePreferenceWidget) already uses.
        TachiyomiTheme(appTheme = palette, amoled = amoled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(6.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = palette.titleRes?.let { stringResource(it) } ?: palette.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
