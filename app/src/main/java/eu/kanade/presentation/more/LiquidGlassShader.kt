package eu.kanade.presentation.more

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * AGSL port of the fragment shader in liquid-glass-js (https://github.com/dashersw/liquid-glass-js,
 * `container.js`), running as a RuntimeShader on the layer behind the glass content.
 *
 * Ported as-is from that shader:
 *  - rounded-rect signed distance, with the outward edge normal taken from the SDF gradient
 *  - base / edge / rim refraction with the same exp() falloffs and intensities
 *  - corner boost and the sin() ripple along the edge
 *  - Gaussian blur of the backdrop at the refracted position
 *  - vertical white-to-grey tint and the sampled-gradient tint
 *  - anti-aliased shape mask
 *
 * Deliberate differences, because the browser version works on a full-page screenshot:
 *  - `content` is the backdrop drawn behind this one card (see [liquidGlassBackdrop]), not a
 *    page screenshot. Refraction can only bend what is actually drawn behind the card.
 *  - the blur uses a 9x9 kernel instead of 13x13 to keep the per-frame cost acceptable.
 *  - the "sampled gradient" pass reads 15 rows of backdrop instead of the source's ~650 reads.
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

    // Signed distance to the rounded rectangle: negative inside, positive outside.
    float roundedRectSdf(float2 p) {
        float2 halfSize = uSize * 0.5;
        float r = min(uRadius, min(halfSize.x, halfSize.y));
        float2 q = abs(p - halfSize) - (halfSize - float2(r));
        return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
    }

    // Backdrop lookup, clamped so samples past the card edge repeat the edge pixel.
    half4 backdrop(float2 p) {
        return content.eval(clamp(p, float2(0.0), uSize));
    }

    half4 main(float2 fragCoord) {
        float minDim = min(uSize.x, uSize.y);
        float2 coord = fragCoord / uSize;
        float sdf = roundedRectSdf(fragCoord);

        // Outward normal of the shape edge, from the SDF gradient.
        float gx = roundedRectSdf(fragCoord + float2(1.0, 0.0)) - roundedRectSdf(fragCoord - float2(1.0, 0.0));
        float gy = roundedRectSdf(fragCoord + float2(0.0, 1.0)) - roundedRectSdf(fragCoord - float2(0.0, 1.0));
        float2 grad = float2(gx, gy);
        float2 shapeNormal = length(grad) > 0.0001 ? normalize(grad) : float2(0.0, 1.0);

        float distFromEdgeShape = max(-sdf, 0.0);
        float distFromEdge = distFromEdgeShape / minDim;
        float normalizedDistance = distFromEdge * minDim;

        float baseIntensity = 1.0 - exp(-normalizedDistance * uBaseDistance);
        float edgeIntensity = exp(-normalizedDistance * uEdgeDistance);
        float rimIntensity = exp(-normalizedDistance * uRimDistance);

        float baseComponent = uWarp > 0.5 ? baseIntensity * uBaseIntensity : 0.0;
        float totalIntensity = baseComponent + edgeIntensity * uEdgeIntensity + rimIntensity * uRimIntensity;
        float2 baseRefraction = shapeNormal * totalIntensity;

        float cornerProximityX = min(coord.x, 1.0 - coord.x);
        float cornerProximityY = min(coord.y, 1.0 - coord.y);
        float cornerNormalized = max(cornerProximityX, cornerProximityY) * minDim;
        float cornerBoost = exp(-cornerNormalized * 0.3) * uCornerBoost;
        float2 cornerRefraction = shapeNormal * cornerBoost;

        float2 perpendicular = float2(-shapeNormal.y, shapeNormal.x);
        float ripple = sin(distFromEdge * 25.0) * uRippleEffect * rimIntensity;
        float2 textureRefraction = perpendicular * ripple;

        // The source adds the refraction to a 0..1 texture coordinate; here it is scaled to pixels.
        float2 totalRefraction = baseRefraction + cornerRefraction + textureRefraction;
        float2 refracted = fragCoord + totalRefraction * uSize;

        // Gaussian blur of the backdrop around the refracted position.
        float sigma = max(uBlurRadius * 0.5, 0.001);
        half4 acc = half4(0.0);
        float wsum = 0.0;
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                float2 off = float2(float(i), float(j));
                float d = length(off);
                if (d <= 4.0) {
                    float w = exp(-(d * d) / (2.0 * sigma * sigma));
                    acc += backdrop(refracted + off * sigma) * half(w);
                    wsum += w;
                }
            }
        }
        half4 blurred = acc / half(wsum);

        // Vertical white-to-grey tint, as in the source.
        float3 gradientTint = mix(float3(1.0), float3(0.7), coord.y);
        float3 tinted = mix(float3(blurred.rgb), gradientTint, uTintOpacity);

        // Sampled-gradient tint: average the backdrop along top, middle and bottom rows.
        float3 topC = float3(0.0);
        float3 midC = float3(0.0);
        float3 botC = float3(0.0);
        for (int k = 0; k < 5; k++) {
            float x = uSize.x * (0.1 + 0.2 * float(k));
            topC += float3(backdrop(float2(x, uSize.y * 0.1)).rgb);
            midC += float3(backdrop(float2(x, uSize.y * 0.5)).rgb);
            botC += float3(backdrop(float2(x, uSize.y * 0.9)).rgb);
        }
        topC /= 5.0;
        midC /= 5.0;
        botC /= 5.0;
        float3 sampled = coord.y < 0.5 ? mix(topC, midC, coord.y * 2.0) : mix(midC, botC, (coord.y - 0.5) * 2.0);
        float3 finalRgb = mix(tinted, sampled, uTintOpacity * 0.3);

        // Anti-aliased shape mask, same smoothstep as the source.
        float mask = 1.0 - smoothstep(-1.0, 1.0, sdf);
        return half4(half3(finalRgb) * half(mask), half(mask));
    }
"""

/**
 * Defaults copied from liquid-glass-js's `Container` class (the `gl.uniform1f(...)` calls in its
 * `setupShader`). `warp` is off there too, so only the edge, rim and corner terms are active.
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

/** True when the AGSL shader can run; below API 33 the caller falls back to the sweep approximation. */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun liquidGlassShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * The backdrop glass cards refract. Refraction needs visible colour behind the glass, so this is a
 * gradient built from the active theme rather than one flat colour.
 */
@Composable
fun liquidGlassBackdrop(): Brush {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        Brush.linearGradient(
            colors = listOf(
                scheme.primary.copy(alpha = 0.22f),
                scheme.background,
                scheme.tertiary.copy(alpha = 0.18f),
            ),
        )
    }
}

/**
 * Renders [content] on a glass surface: [backdrop] is drawn into the layer and refracted by the
 * liquid-glass shader, then [content] is drawn on top, undistorted.
 * Only call this when [liquidGlassShaderSupported] is true.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LiquidGlassShaderSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    backdrop: Brush,
    config: LiquidGlassConfig = LiquidGlassConfig(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val shader = remember { RuntimeShader(LIQUID_GLASS_AGSL) }
    val radiusPx = with(density) { cornerRadius.toPx() }

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
        // Refracted layer: the backdrop, distorted by the shader. Only this layer sets the size
        // of the surface, through onSizeChanged, so the content below decides the height.
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { size ->
                    shader.setFloatUniform("uSize", size.width.toFloat(), size.height.toFloat())
                }
                .graphicsLayer { this.renderEffect = renderEffect }
                .background(backdrop),
        )
        // Undistorted layer: the real content, drawn normally on top. It must size itself to its
        // content (no matchParentSize), otherwise every card collapses to zero height.
        Box {
            content()
        }
    }
}
