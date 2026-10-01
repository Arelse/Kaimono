package eu.kanade.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import eu.kanade.domain.ui.model.AccentColor
import eu.kanade.domain.ui.model.ParticleEffect
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * A slow, looping flowing gradient built from an [AccentColor]'s two tones. Independent of
 * Liquid mode's background image and of the app's actual AppTheme/color scheme - this is a
 * purely decorative "flourish" layer.
 */
@Composable
fun AccentGradientBackground(accent: AccentColor, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "accentGradient")
    val t by transition.animateFloatLoop(durationMillis = 14000, label = "accentGradientTime")

    Canvas(modifier = modifier.fillMaxSize()) {
        val angle = t * 2f * PI.toFloat()
        val cx = size.width * (0.5f + 0.35f * kotlin.math.cos(angle))
        val cy = size.height * (0.4f + 0.3f * sin(angle * 1.3f))
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.primary.copy(alpha = 0.55f),
                    accent.secondary.copy(alpha = 0.85f),
                    Color.Black,
                ),
                center = Offset(cx, cy),
                radius = size.maxDimension * 0.9f,
            ),
        )
    }
}

private data class ParticleSpec(
    val xFraction: Float,
    val sizeDp: Float,
    val speed: Float,
    val phase: Float,
    val drift: Float,
)

/**
 * Generic animated particle field. Each particle's position is a pure function of a single
 * looping time value rather than per-frame mutable state, which keeps this cheap regardless of
 * particle count.
 */
@Composable
fun ParticleOverlay(effect: ParticleEffect, modifier: Modifier = Modifier) {
    if (effect == ParticleEffect.NONE) return

    val count = if (effect == ParticleEffect.STARS) 60 else 36
    val particles = remember(effect) {
        val random = Random(effect.ordinal * 7919 + 13)
        List(count) {
            ParticleSpec(
                xFraction = random.nextFloat(),
                sizeDp = random.nextFloat(),
                speed = 0.5f + random.nextFloat() * 0.8f,
                phase = random.nextFloat(),
                drift = random.nextFloat() * 2f - 1f,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val durationMillis = when (effect) {
        ParticleEffect.RAIN -> 2500
        ParticleEffect.EMBERS, ParticleEffect.FIREFLIES -> 9000
        ParticleEffect.STARS -> 5000
        else -> 12000
    }
    val time by transition.animateFloatLoop(durationMillis = durationMillis, label = "particleTime")

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val t = (time * p.speed + p.phase) % 1f
            val baseX = p.xFraction * w

            when (effect) {
                ParticleEffect.SNOW -> {
                    val y = t * h
                    val x = baseX + sin(t * 6f * PI.toFloat() + p.phase * 10f) * 20f * p.drift
                    val radius = 2f + p.sizeDp * 3f
                    drawCircle(Color.White.copy(alpha = 0.75f), radius, Offset(x, y))
                }
                ParticleEffect.RAIN -> {
                    val y = t * (h + 60f) - 30f
                    val x = baseX - 15f
                    drawLine(
                        color = Color(0xFFBFE3FF).copy(alpha = 0.55f),
                        start = Offset(x, y),
                        end = Offset(x - 8f, y + 28f),
                        strokeWidth = 2f,
                    )
                }
                ParticleEffect.STARS -> {
                    val y = p.sizeDp * h
                    val alpha = (sin(t * 2f * PI.toFloat() + p.phase * 10f) * 0.5f + 0.5f) * 0.9f
                    val radius = 1f + p.sizeDp * 2f
                    drawCircle(Color.White.copy(alpha = alpha), radius, Offset(baseX, y))
                }
                ParticleEffect.SAKURA -> {
                    val y = t * h
                    val x = baseX + sin(t * 4f * PI.toFloat() + p.phase * 10f) * 36f * p.drift
                    val radius = 3f + p.sizeDp * 4f
                    drawCircle(Color(0xFFFFB7D5).copy(alpha = 0.7f), radius, Offset(x, y))
                }
                ParticleEffect.FIREFLIES -> {
                    val y = h - (t * h)
                    val x = baseX + sin(t * 5f * PI.toFloat() + p.phase * 10f) * 24f * p.drift
                    val alpha = (sin(t * 8f * PI.toFloat() + p.phase * 20f) * 0.5f + 0.5f) * 0.8f
                    val radius = 2f + p.sizeDp * 2.5f
                    drawCircle(Color(0xFFD9F55A).copy(alpha = alpha), radius, Offset(x, y))
                }
                ParticleEffect.HEARTS -> {
                    val y = h - (t * h)
                    val x = baseX + sin(t * 3f * PI.toFloat() + p.phase * 10f) * 18f * p.drift
                    val alpha = (1f - t) * 0.75f
                    val radius = 3f + p.sizeDp * 4f
                    drawCircle(Color(0xFFFF6FA5).copy(alpha = alpha), radius, Offset(x, y))
                }
                ParticleEffect.EMBERS -> {
                    val y = h - (t * h)
                    val x = baseX + sin(t * 6f * PI.toFloat() + p.phase * 10f) * 14f * p.drift
                    val alpha = (1f - t) * 0.8f
                    val radius = 1.5f + p.sizeDp * 2.5f
                    drawCircle(Color(0xFFFF8A3D).copy(alpha = alpha), radius, Offset(x, y))
                }
                ParticleEffect.NONE -> Unit
            }
        }
    }
}

@Composable
private fun androidx.compose.animation.core.InfiniteTransition.animateFloatLoop(
    durationMillis: Int,
    label: String,
) = animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = durationMillis, easing = LinearEasing),
    ),
    label = label,
)
