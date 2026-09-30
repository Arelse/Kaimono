package eu.kanade.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import kotlin.random.Random
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Purely decorative ambient effects (Bloom glow, film grain) drawn once at the app root,
 * behind the whole navigation stack. Both are independent of Liquid mode's background image.
 */
@Composable
fun AmbientEffectsOverlay(modifier: Modifier = Modifier) {
    val uiPreferences = Injekt.get<UiPreferences>()
    val bloomEnabled by uiPreferences.bloomEnabled.collectAsState()
    val grainEnabled by uiPreferences.grainOverlayEnabled.collectAsState()

    if (!bloomEnabled && !grainEnabled) return

    Box(modifier = modifier.fillMaxSize()) {
        if (bloomEnabled) {
            BloomGlow()
        }
        if (grainEnabled) {
            GrainOverlay()
        }
    }
}

@Composable
private fun BloomGlow() {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(80.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.9f)
                .aspectRatio(1.4f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.35f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.25f), Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
private fun GrainOverlay() {
    val noiseBrush = remember { createNoiseBrush() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(brush = noiseBrush, alpha = 0.05f, blendMode = BlendMode.Overlay)
    }
}

private fun createNoiseBrush(): ShaderBrush {
    val size = 128
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val random = Random(42)
    for (x in 0 until size) {
        for (y in 0 until size) {
            val gray = random.nextInt(200, 256)
            bitmap.setPixel(x, y, android.graphics.Color.argb(255, gray, gray, gray))
        }
    }
    val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    return ShaderBrush(shader)
}
