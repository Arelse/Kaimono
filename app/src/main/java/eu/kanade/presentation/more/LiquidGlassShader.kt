package eu.kanade.presentation.more

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity

/**
 * Port of the liquid-glass-js (https://github.com/dashersw/liquid-glass-js) refraction shader
 * to AGSL, so it can run as a real-time [RuntimeShader] on Android 13+ instead of the
 * approximated specular-sweep card used on older API levels.
 *
 * This keeps the same uniforms, the same rounded-rect/edge/rim/base refraction math, the same
 * 13x13 Gaussian blur kernel and the same vertical gradient tint as the original Container
 * fragment shader. The one deliberate simplification: liquid-glass-js samples a live
 * html2canvas screenshot of the whole page as `u_image`/`content`, because on the web these
 * cards can float over photographic/scrolled content. In this app the Settings cards sit over a
 * single flat screen background (no photo/scrolled content behind them, since cards never
 * overlap each other), so `content` here is that same background color instead of a captured
 * screenshot - the per-pixel math producing the glass look is otherwise identical. The source's
 * secondary "sampled gradient" pass (an extra ~650 texture reads per pixel, meant for reflecting
 * distant parts of a tall page) is dropped for mobile frame-time reasons; everything else -
 * edge/rim/base refraction, corner boost, ripple, blur, tint, shape mask - is ported as-is.
 */
private const val LIQUID_GLASS_AGSL = """
    uniform shader content;
    uniform float2 uSize;
    uniform float uRadius;
    uniform float uBlurRadius;
    uniform float uWarp;
    uniform float uEdgeIntensity;
    uniform float uRimIntensity;
    uniform float uBaseIntensity;
    uniform float uEdgeDistance;
    uniform float uRimDistance;
    uniform float uBaseDistance;
    uniform float uCornerBoost;
    uniform float uRippleEffect;
    uniform float uTintOpacity;

    half4 main(float2 fragCoord) {
        float2 size = uSize;
        float2 coord = fragCoord / size;
        float radius = uRadius;
        float minDim = min(size.x, size.y);

        // roundedRectDistance(), ported directly - negative inside the shape, positive outside.
        float2 center = size * 0.5;
        float2 toCorner = abs(fragCoord - center) - (center - radius);
        float outsideCorner = length(max(toCorner, float2(0.0)));
        float insideCorner = min(max(toCorner.x, toCorner.y), 0.0);
        float rectDist = outsideCorner + insideCorner - radius;

        float distFromEdgeShape = max(-rectDist, 0.0);
        float distFromEdge = distFromEdgeShape / minDim;
        float normalizedDistance = distFromEdge * minDim;

        float2 shapeNormal = normalize(coord - float2(0.5, 0.5));

        float baseIntensity = 1.0 - exp(-normalizedDistance * uBaseDistance);
        float edgeIntensity = exp(-normalizedDistance * uEdgeDistance);
        float rimIntensity = exp(-normalizedDistance * uRimDistance);

        float baseComponent = uWarp > 0.5 ? baseIntensity * uBaseIntensity : 0.0;
        float totalIntensity = baseComponent + edgeIntensity * uEdgeIntensity + rimIntensity * uRimIntensity;
        float2 baseRefraction = shapeNormal * totalIntensity;

        float distFromLeft = coord.x;
        float distFromRight = 1.0 - coord.x;
        float distFromTop = coord.y;
        float distFromBottom = 1.0 - coord.y;
        float cornerProximityX = min(distFromLeft, distFromRight);
        float cornerProximityY = min(distFromTop, distFromBottom);
        float cornerDistance = max(cornerProximityX, cornerProximityY);
        float cornerNormalized = cornerDistance * minDim;
        float cornerBoost = exp(-cornerNormalized * 0.3) * uCornerBoost;
        float2 cornerRefraction = shapeNormal * cornerBoost;

        float2 perpendicular = float2(-shapeNormal.y, shapeNormal.x);
        float ripple = sin(distFromEdge * 25.0) * uRippleEffect * rimIntensity;
        float2 textureRefraction = perpendicular * ripple;

        float2 totalRefraction = baseRefraction + cornerRefraction + textureRefraction;
        float2 refractedPx = fragCoord + totalRefraction * size;

        // Gaussian blur - same 13x13 kernel as the source.
        half4 color = half4(0.0);
        float sigma = max(uBlurRadius * 0.5, 0.001);
        float totalWeight = 0.0;
        for (int i = -6; i <= 6; i++) {
            for (int j = -6; j <= 6; j++) {
                float dist = length(float2(float(i), float(j)));
                if (dist <= 6.0) {
                    float weight = exp(-(dist * dist) / (2.0 * sigma * sigma));
                    float2 samplePx = refractedPx + float2(float(i), float(j)) * sigma;
                    color += content.eval(samplePx) * half(weight);
                    totalWeight += weight;
                }
            }
        }
        color /= half(totalWeight);

        // Simple vertical gradient tint - lighter at the top, same as the source.
        float3 topTint = float3(1.0, 1.0, 1.0);
        float3 bottomTint = float3(0.7, 0.7, 0.7);
        float3 gradientTint = mix(topTint, bottomTint, coord.y);
        float3 tinted = mix(float3(color.rgb), gradientTint, uTintOpacity);
        color = half4(half3(tinted), color.a);

        // Shape mask, same smoothstep as the source.
        float mask = 1.0 - smoothstep(-1.0, 1.0, rectDist);
        return half4(color.rgb * half(mask), half(mask));
    }
"""

/**
 * Default uniform values, copied from liquid-glass-js's `Container` class defaults (not
 * `Button`'s, which differ) - see the `gl.uniform1f(...)` calls in its `setupShader`.
 */
data class LiquidGlassConfig(
    val blurRadius: Float = 5.0f,
    val warp: Boolean = false,
    val edgeIntensity: Float = 0.01f,
    val rimIntensity: Float = 0.05f,
    val baseIntensity: Float = 0.01f,
    val edgeDistance: Float = 0.15f,
    val rimDistance: Float = 0.8f,
    val baseDistance: Float = 0.1f,
    val cornerBoost: Float = 0.02f,
    val rippleEffect: Float = 0.1f,
    val tintOpacity: Float = 0.2f,
)

/** True when the real AGSL shader can run; below API 33 the caller should fall back. */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun liquidGlassShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Renders [content] over a glass surface that refracts/blurs [backdropColor] using the ported
 * liquid-glass-js shader. Only call this when [liquidGlassShaderSupported] is true.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiquidGlassShaderSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    backdropColor: Color,
    config: LiquidGlassConfig = LiquidGlassConfig(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val shader = remember { RuntimeShader(LIQUID_GLASS_AGSL) }
    val radiusPx = with(density) { cornerRadius.toPx() }

    // Cheap scalar uniform updates; re-applied every recomposition rather than gated behind a
    // remember key, since RuntimeShader.setFloatUniform is just a field write.
    shader.setFloatUniform("uRadius", radiusPx)
    shader.setFloatUniform("uBlurRadius", config.blurRadius)
    shader.setFloatUniform("uWarp", if (config.warp) 1f else 0f)
    shader.setFloatUniform("uEdgeIntensity", config.edgeIntensity)
    shader.setFloatUniform("uRimIntensity", config.rimIntensity)
    shader.setFloatUniform("uBaseIntensity", config.baseIntensity)
    shader.setFloatUniform("uEdgeDistance", config.edgeDistance)
    shader.setFloatUniform("uRimDistance", config.rimDistance)
    shader.setFloatUniform("uBaseDistance", config.baseDistance)
    shader.setFloatUniform("uCornerBoost", config.cornerBoost)
    shader.setFloatUniform("uRippleEffect", config.rippleEffect)
    shader.setFloatUniform("uTintOpacity", config.tintOpacity)

    val renderEffect = remember(shader) {
        RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        // Distorted layer: only the flat backdrop color goes through the shader.
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { size ->
                    shader.setFloatUniform("uSize", size.width.toFloat(), size.height.toFloat())
                }
                .graphicsLayer { this.renderEffect = renderEffect }
                .background(backdropColor),
        )
        // Undistorted layer: the actual settings items, drawn normally on top.
        Box(modifier = Modifier.matchParentSize()) {
            content()
        }
    }
}
