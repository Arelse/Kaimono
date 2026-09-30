package tachiyomi.presentation.core.components.material

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether an app-wide liquid (blurred image) background is currently active.
 *
 * Provided a real value from the app's root composable, where the actual background
 * image, blur and scrim are drawn once behind the whole navigation stack. [Scaffold]
 * reads this to decide whether its own container should paint an opaque background
 * (default, when false) or stay transparent so the root's background shows through
 * (when true) - without presentation-core needing to depend on the app module's
 * preferences.
 */
val LocalLiquidBackgroundActive = compositionLocalOf { false }
