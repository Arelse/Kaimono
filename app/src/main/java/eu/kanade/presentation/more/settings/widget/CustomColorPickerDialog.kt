package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

private fun hslToColorInt(h: Float, s: Float, l: Float): Int =
    ColorUtils.HSLToColor(floatArrayOf(h, s, l))

private fun colorIntToHex(color: Int): String =
    "#%06X".format(color and 0xFFFFFF)

/**
 * Hue / saturation / lightness color picker with a live preview and a hex code field.
 * Calls [onConfirm] with an opaque ARGB color int.
 */
@Composable
fun CustomColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialHsl = remember {
        FloatArray(3).also { ColorUtils.colorToHSL(initialColor, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsl[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsl[1]) }
    var lightness by remember { mutableFloatStateOf(initialHsl[2]) }

    val currentColor = hslToColorInt(hue, saturation, lightness)
    var hexText by remember { mutableStateOf(colorIntToHex(initialColor)) }

    fun syncHexFromSliders() {
        hexText = colorIntToHex(hslToColorInt(hue, saturation, lightness))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom color picker",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(currentColor)),
                    )
                }

                Text("Hue", style = MaterialTheme.typography.labelLarge)
                GradientSlider(
                    value = hue,
                    range = 0f..360f,
                    brush = Brush.horizontalGradient(
                        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map {
                            Color(hslToColorInt(it, 1f, 0.5f))
                        },
                    ),
                    onValueChange = {
                        hue = it
                        syncHexFromSliders()
                    },
                )

                Text("Saturation", style = MaterialTheme.typography.labelLarge)
                GradientSlider(
                    value = saturation,
                    range = 0f..1f,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(hslToColorInt(hue, 0f, lightness)),
                            Color(hslToColorInt(hue, 1f, lightness)),
                        ),
                    ),
                    onValueChange = {
                        saturation = it
                        syncHexFromSliders()
                    },
                )

                Text("Lightness", style = MaterialTheme.typography.labelLarge)
                GradientSlider(
                    value = lightness,
                    range = 0f..1f,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Black,
                            Color(hslToColorInt(hue, saturation, 0.5f)),
                            Color.White,
                        ),
                    ),
                    onValueChange = {
                        lightness = it
                        syncHexFromSliders()
                    },
                )

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val cleaned = input.removePrefix("#")
                        if (cleaned.length == 6) {
                            cleaned.toLongOrNull(16)?.let { parsed ->
                                val hsl = FloatArray(3)
                                ColorUtils.colorToHSL((parsed or 0xFF000000).toInt(), hsl)
                                hue = hsl[0]
                                saturation = hsl[1]
                                lightness = hsl[2]
                            }
                        }
                    },
                    label = { Text("Hex color code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentColor or 0xFF000000.toInt()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun GradientSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    onValueChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(brush),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

