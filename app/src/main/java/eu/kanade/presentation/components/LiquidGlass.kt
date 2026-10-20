package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    
