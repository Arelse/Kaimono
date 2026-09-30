package eu.kanade.presentation.more.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    val backgroundPath by uiPreferences.settingsBackgroundPath.collectAsState()
    val backgroundBlur by uiPreferences.settingsBackgroundBlur.collectAsState()
    val backgroundLight by uiPreferences.settingsBackgroundLight.collectAsState()
    val hasBackground = backgroundPath.isNotBlank()

    Box {
        if (hasBackground) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backgroundPath)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(backgroundBlur.dp),
            )
            Image(
                painter = androidx.compose.ui.graphics.painter.ColorPainter(
                    Color.Black.copy(alpha = (100 - backgroundLight).coerceIn(0, 100) / 100f * 0.85f),
                ),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )
        }
        Scaffold(
            containerColor = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                AppBar(
                    title = stringResource(titleRes),
                    backgroundColor = if (hasBackground) Color.Transparent else null,
                    navigateUp = onBackPressed,
                    actions = actions,
                    scrollBehavior = it,
                )
            },
            content = { contentPadding ->
                PreferenceScreen(
                    items = itemsProvider(),
                    contentPadding = contentPadding,
                )
            },
        )
    }
}
