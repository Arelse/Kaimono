package eu.kanade.presentation.manga.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.atsuGlass
import eu.kanade.presentation.components.liquidGlass
import eu.kanade.presentation.reader.settings.ControlThemeDialog
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ReaderControlTheme
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt

private val chapterRowShape = RoundedCornerShape(16.dp)

/** Whether the "apply control theme to chapter list" option is switched on. */
@Composable
fun rememberChapterListControlThemeEnabled(): Boolean {
    val prefs = remember { Injekt.get<ReaderPreferences>() }
    val enabled by prefs.controlThemeOnChapterList.collectAsState()
    return enabled
}

/**
 * The control theme to draw chapter rows with, or null for the plain row surface. Only applies when
 * the option is on and a non-Default theme is selected.
 */
@Composable
fun rememberChapterListGlassTheme(): ReaderControlTheme? {
    val prefs = remember { Injekt.get<ReaderPreferences>() }
    val theme by prefs.controlTheme.collectAsState()
    val enabled by prefs.controlThemeOnChapterList.collectAsState()
    return if (enabled && theme != ReaderControlTheme.DEFAULT) theme else null
}

/** Control Theme picker, shown from the manga top bar when the chapter-list option is on. */
@Composable
fun ChapterListControlThemePicker(show: Boolean, onDismissRequest: () -> Unit) {
    if (!show) return
    val prefs = remember { Injekt.get<ReaderPreferences>() }
    val current by prefs.controlTheme.collectAsState()
    ControlThemeDialog(
        current = current,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            prefs.controlTheme.set(it)
            onDismissRequest()
        },
    )
}

/** Row surface for a chapter item: the plain tint by default, or the chosen control theme's glass. */
@Composable
fun Modifier.chapterRowSurface(theme: ReaderControlTheme?): Modifier {
    val tint = MaterialTheme.colorScheme.surfaceContainerHigh
    return when (theme) {
        null, ReaderControlTheme.DEFAULT -> this.background(tint.copy(alpha = 0.6f))
        ReaderControlTheme.TRANSLUCENT -> this
            .background(tint.copy(alpha = 0.45f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = chapterRowShape)
        ReaderControlTheme.IOS26 -> liquidGlass(chapterRowShape, tint, tintAlpha = 0.6f)
        ReaderControlTheme.ATSU -> atsuGlass(chapterRowShape, tint, scrolling = false)
    }
}

