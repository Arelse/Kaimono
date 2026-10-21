package eu.kanade.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Liquid-glass surface: a translucent tint that is brighter at the top, with a light rim that is
 * stronger along the top edge. Content underneath stays visible through it.
 *
 * Note: this tints what is behind the surface rather than blurring it. A live blur of scrolling
 * content needs a backdrop-capture library, which this project does not use.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color,
    tintAlpha: Float = 0.42f,
    rimAlpha: Float = 0.28f,
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                tint.copy(alpha = tintAlpha),
                tint.copy(alpha = (tintAlpha + 0.12f).coerceAtMost(1f)),
            ),
        ),
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = rimAlpha),
                Color.White.copy(alpha = rimAlpha * 0.2f),
            ),
        ),
        shape = shape,
    )

/**
 * Default UI nav bar: solid and opaque with a hairline rim, so nothing shows through it.
 */
fun Modifier.solidNavSurface(shape: Shape, tint: Color): Modifier = this
    .clip(shape)
    .background(tint)
    .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = shape)

/**
 * Translucent UI nav bar: a light tint over the content, with a faint rim and no sheen.
 */
fun Modifier.translucentNavSurface(shape: Shape, tint: Color): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = 0.22f))
    .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = shape)

/**
 * Atsu UI nav bar: a frosted white wash over a darker tint, with a bright rim.
 */
fun Modifier.frostedNavSurface(shape: Shape, tint: Color): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                tint.copy(alpha = 0.45f),
            ),
        ),
    )
    .border(width = 1.dp, color = Color.White.copy(alpha = 0.35f), shape = shape)

/**
 * Vision Glass nav bar: a near-opaque tint with a clear outline, so it reads as an outlined pill
 * rather than a fill or a glass surface.
 */
fun Modifier.outlinedNavSurface(shape: Shape, tint: Color): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = 0.85f))
    .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.35f), shape = shape)

/**
 * Neumorphism nav bar: a dark, near-black surface with a soft drop shadow and no bright rim.
 */
fun Modifier.neumorphNavSurface(shape: Shape): Modifier = this
    .shadow(elevation = 10.dp, shape = shape, clip = false)
    .clip(shape)
    .background(Color(0xFF0D0D0D))
    .border(width = 1.dp, color = Color.White.copy(alpha = 0.04f), shape = shape)

/**
 * Atsu reader surface: a deeper tint with a two-tone rim (bright top edge, faint bottom edge).
 * While [scrolling] is true (auto-scroll running) the surface eases to a lighter, more
 * see-through state, and returns to full strength when scrolling stops.
 *
 * This is a tint, not a true backdrop blur; content behind it is visible but not blurred.
 */
fun Modifier.atsuGlass(shape: Shape, tint: Color, scrolling: Boolean): Modifier = composed {
    val strength by animateFloatAsState(
        targetValue = if (scrolling) 0.6f else 1f,
        animationSpec = tween(durationMillis = 450),
        label = "atsuGlassStrength",
    )
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.14f * strength),
                    tint.copy(alpha = 0.55f * strength),
                    tint.copy(alpha = 0.70f * strength),
                ),
            ),
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.18f),
                ),
            ),
            shape = shape,
        )
}
