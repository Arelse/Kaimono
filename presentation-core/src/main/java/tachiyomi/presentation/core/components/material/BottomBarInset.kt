package tachiyomi.presentation.core.components.material

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of a floating navigation bar drawn over the content. Screens built on this Scaffold add
 * it to their bottom padding, so content can run under the bar and the last item still scrolls
 * above it. 0 when the bar is docked or absent.
 */
val LocalFloatingBottomBarInset = compositionLocalOf<Dp> { 0.dp }
