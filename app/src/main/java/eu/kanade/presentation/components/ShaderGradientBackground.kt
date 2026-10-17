package eu.kanade.presentation.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import eu.kanade.domain.ui.model.ShaderGradientPreset

/**
 * Port of the default shaders from ruucm/shadergradient (packages/shadergradient/src/shaders/defaults)
 * to AGSL. Same 3D simplex noise (cnoise), same displacement amplitude and frequency, and the same
 * position-based color mix: mix(mix(c1, c2, smoothstep(-3, 3, x)), c3, z).
 *
 * The original is a three.js mesh with a standard lit material. On a phone this is drawn as a full
 * screen pass instead: the displaced surface is evaluated per pixel, lit with a simple diffuse and
 * specular term from finite-difference normals. Colors, speed, strength, density and reflection come
 * from the same preset values. Two things are approximations and will not match the web pixel for
 * pixel: the sphere is ray-cast as a single unit sphere (no three.js camera/fov matching), and the
 * water-plane variant is a plane foreshortened vertically rather than the original tilted mesh.
 *
 * Needs Android 13 (RuntimeShader). Below that, [ShaderGradientBackground] falls back to the Classic
 * gradient using the preset's first two colors.
 */
private const val SHADER_GRADIENT_AGSL = """
    uniform float2 iResolution;
    uniform float uTime;
    uniform float uSpeed;
    uniform float uStrength;
    uniform float uDensity;
    uniform float uReflection;
    uniform float uType;
    uniform float3 uC1;
    uniform float3 uC2;
    uniform float3 uC3;

    float3 mod289(float3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
    float4 mod289(float4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
    float4 permute(float4 x) { return mod289(((x * 34.0) + 1.0) * x); }
    float4 taylorInvSqrt(float4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
    float3 fadeCurve(float3 t) { return t * t * t * (t * (t * 6.0 - 15.0) + 10.0); }

    float cnoise(float3 P) {
        float3 Pi0 = floor(P);
        float3 Pi1 = Pi0 + float3(1.0);
        Pi0 = mod289(Pi0);
        Pi1 = mod289(Pi1);
        float3 Pf0 = fract(P);
        float3 Pf1 = Pf0 - float3(1.0);
        float4 ix = float4(Pi0.x, Pi1.x, Pi0.x, Pi1.x);
        float4 iy = float4(Pi0.yy, Pi1.yy);
        float4 iz0 = float4(Pi0.z);
        float4 iz1 = float4(Pi1.z);

        float4 ixy = permute(permute(ix) + iy);
        float4 ixy0 = permute(ixy + iz0);
        float4 ixy1 = permute(ixy + iz1);

        float4 gx0 = ixy0 * (1.0 / 7.0);
        float4 gy0 = fract(floor(gx0) * (1.0 / 7.0)) - 0.5;
        gx0 = fract(gx0);
        float4 gz0 = float4(0.5) - abs(gx0) - abs(gy0);
        float4 sz0 = step(gz0, float4(0.0));
        gx0 -= sz0 * (step(0.0, gx0) - 0.5);
        gy0 -= sz0 * (step(0.0, gy0) - 0.5);

        float4 gx1 = ixy1 * (1.0 / 7.0);
        float4 gy1 = fract(floor(gx1) * (1.0 / 7.0)) - 0.5;
        gx1 = fract(gx1);
        float4 gz1 = float4(0.5) - abs(gx1) - abs(gy1);
        float4 sz1 = step(gz1, float4(0.0));
        gx1 -= sz1 * (step(0.0, gx1) - 0.5);
        gy1 -= sz1 * (step(0.0, gy1) - 0.5);

        float3 g000 = float3(gx0.x, gy0.x, gz0.x);
        float3 g100 = float3(gx0.y, gy0.y, gz0.y);
        float3 g010 = float3(gx0.z, gy0.z, gz0.z);
        float3 g110 = float3(gx0.w, gy0.w, gz0.w);
        float3 g001 = float3(gx1.x, gy1.x, gz1.x);
        float3 g101 = float3(gx1.y, gy1.y, gz1.y);
        float3 g011 = float3(gx1.z, gy1.z, gz1.z);
        float3 g111 = float3(gx1.w, gy1.w, gz1.w);

        float4 norm0 = taylorInvSqrt(float4(dot(g000, g000), dot(g010, g010), dot(g100, g100), dot(g110, g110)));
        g000 *= norm0.x;
        g010 *= norm0.y;
        g100 *= norm0.z;
        g110 *= norm0.w;
        float4 norm1 = taylorInvSqrt(float4(dot(g001, g001), dot(g011, g011), dot(g101, g101), dot(g111, g111)));
        g001 *= norm1.x;
        g011 *= norm1.y;
        g101 *= norm1.z;
        g111 *= norm1.w;

        float n000 = dot(g000, Pf0);
        float n100 = dot(g100, float3(Pf1.x, Pf0.yz));
        float n010 = dot(g010, float3(Pf0.x, Pf1.y, Pf0.z));
        float n110 = dot(g110, float3(Pf1.xy, Pf0.z));
        float n001 = dot(g001, float3(Pf0.xy, Pf1.z));
        float n101 = dot(g101, float3(Pf1.x, Pf0.y, Pf1.z));
        float n011 = dot(g011, float3(Pf0.x, Pf1.yz));
        float n111 = dot(g111, Pf1);

        float3 fade_xyz = fadeCurve(Pf0);
        float4 n_z = mix(float4(n000, n100, n010, n110), float4(n001, n101, n011, n111), fade_xyz.z);
        float2 n_yz = mix(n_z.xy, n_z.zw, fade_xyz.y);
        float n_xyz = mix(n_yz.x, n_yz.y, fade_xyz.x);
        return 2.2 * n_xyz;
    }

    // Plane / water plane displacement, as in shaders/defaults/plane/vertex.glsl.
    float planeHeight(float2 p, float t) {
        return 0.75 * cnoise(float3(0.43 * p * uDensity, 0.0) + t) * uStrength;
    }

    // The original gradient, evaluated at the displaced position q.
    float3 planeGradient(float3 q) {
        return mix(mix(uC1, uC2, smoothstep(-3.0, 3.0, q.x)), uC3, q.z);
    }

    float3 litColor(float3 base, float3 n) {
        float3 L = normalize(float3(-0.35, 0.55, 0.75));
        float3 V = float3(0.0, 0.0, 1.0);
        float3 H = normalize(L + V);
        float diff = max(dot(n, L), 0.0);
        float spec = pow(max(dot(n, H), 0.0), 32.0);
        return base * (0.55 + 0.6 * diff) + uReflection * spec;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        float aspect = iResolution.x / iResolution.y;
        float t = uTime * uSpeed;
        float3 col;

        if (uType > 0.5 && uType < 1.5) {
            // Sphere: ray-cast a unit sphere at the origin, camera 3.2 units back, 45 degree fov.
            float tanHalf = tan(radians(22.5));
            float2 ndc = float2((uv.x * 2.0 - 1.0) * aspect, 1.0 - 2.0 * uv.y) * tanHalf;
            float3 o = float3(0.0, 0.0, 3.2);
            float3 d = normalize(float3(ndc, -1.0));
            float b = dot(o, d);
            float c = dot(o, o) - 1.0;
            float disc = b * b - c;
            if (disc < 0.0) {
                float3 bg = mix(uC1, uC3, uv.y) * 0.12;
                return half4(clamp(bg, 0.0, 1.0), 1.0);
            }
            float3 q0 = o + d * (-b - sqrt(disc));
            float3 n = normalize(q0);
            float dr = 0.25 * cnoise(n * uDensity * 0.8 + t) * uStrength;
            float3 q = n * (1.0 + dr);
            col = mix(uC3, mix(uC2, uC1, smoothstep(-1.0, 1.0, q.y)), length(q));
            col = litColor(col, n);
        } else {
            // Plane (type 0) and water plane (type 2), the latter foreshortened vertically.
            float2 p = float2((uv.x - 0.5) * aspect, 0.5 - uv.y) * 6.0;
            if (uType > 1.5) {
                p.y *= 2.0;
            }
            float e = 0.02;
            float h = planeHeight(p, t);
            float hx = planeHeight(p + float2(e, 0.0), t);
            float hy = planeHeight(p + float2(0.0, e), t);
            float3 n = normalize(float3(-(hx - h) / e, -(hy - h) / e, 1.0));
            col = planeGradient(float3(p, h));
            col = litColor(col, n);
        }

        return half4(clamp(col, 0.0, 1.0), 1.0);
    }
"""

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun shaderGradientSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Seconds since the first frame, drawn into the Canvas so it does not recompose every frame. */
@Composable
private fun rememberGradientTime(): State<Float> {
    return produceState(0f) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameNanos ->
                value = ((frameNanos - start) / 1_000_000_000.0).toFloat() % 1000f
            }
        }
    }
}

/**
 * Animated gradient from one of the ShaderGradient presets. On Android 13+ this is the AGSL port
 * above; on older versions it falls back to the Classic accent gradient with the preset's colors.
 */
@Composable
fun ShaderGradientBackground(preset: ShaderGradientPreset, modifier: Modifier = Modifier) {
    if (shaderGradientSupported()) {
        ShaderGradientAgsl(preset = preset, modifier = modifier)
    } else {
        AccentGradientBackground(
            primary = Color(preset.color1),
            secondary = Color(preset.color2),
            modifier = modifier,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderGradientAgsl(preset: ShaderGradientPreset, modifier: Modifier) {
    val shader = remember(preset) { RuntimeShader(SHADER_GRADIENT_AGSL) }
    val time = rememberGradientTime()
    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("uTime", time.value)
        shader.setFloatUniform("uSpeed", preset.speed)
        shader.setFloatUniform("uStrength", preset.strength)
        shader.setFloatUniform("uDensity", preset.density)
        shader.setFloatUniform("uReflection", preset.reflection)
        shader.setFloatUniform("uType", preset.type.code)
        shader.setFloatUniform("uC1", Color(preset.color1).red, Color(preset.color1).green, Color(preset.color1).blue)
        shader.setFloatUniform("uC2", Color(preset.color2).red, Color(preset.color2).green, Color(preset.color2).blue)
        shader.setFloatUniform("uC3", Color(preset.color3).red, Color(preset.color3).green, Color(preset.color3).blue)
        drawRect(brush = ShaderBrush(shader))
    }
}

