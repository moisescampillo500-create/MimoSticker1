package com.mymoises.mystickerapp.ui.dialogs

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.ui.components.Checkerboard

@Composable
fun BrushCustomColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    dialogTitle: String = "🎨 Select Color"
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).apply { AndroidColor.colorToHSV(initialColor, this) }
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf((AndroidColor.alpha(initialColor) / 255f).coerceIn(0.05f, 1f)) }

    val currentColorInt = remember(hue, saturation, value, alpha) {
        val hsvColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        val aInt = (alpha * 255).toInt().coerceIn(0, 255)
        (aInt shl 24) or (hsvColor and 0x00FFFFFF)
    }

    val presetColors = listOf(
        AndroidColor.RED, AndroidColor.parseColor("#E91E63"), AndroidColor.parseColor("#9C27B0"),
        AndroidColor.parseColor("#673AB7"), AndroidColor.parseColor("#3F51B5"), AndroidColor.BLUE,
        AndroidColor.parseColor("#03A9F4"), AndroidColor.CYAN, AndroidColor.parseColor("#009688"),
        AndroidColor.GREEN, AndroidColor.parseColor("#8BC34A"), AndroidColor.YELLOW,
        AndroidColor.parseColor("#FFC107"), AndroidColor.parseColor("#FF9800"), AndroidColor.parseColor("#FF5722"),
        AndroidColor.BLACK, AndroidColor.parseColor("#424242"), AndroidColor.parseColor("#9E9E9E"),
        AndroidColor.WHITE, AndroidColor.parseColor("#795548")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF8B5CF6))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Checkerboard(Modifier.matchParentSize())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(Color(currentColorInt))
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E182E))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Selected Color", fontSize = 11.sp, color = Color.Gray)
                        val hexString = String.format("#%08X", currentColorInt)
                        Text(hexString, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp, 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkerboard(Modifier.matchParentSize())
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(currentColorInt))
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Hue Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Hue", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${hue.toInt()}°", fontSize = 11.sp, color = Color.Gray)
                    }
                    val rainbowGradient = Brush.horizontalGradient(
                        listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(rainbowGradient)
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Saturation Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Saturation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${(saturation * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                    }
                    val pureHueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, value)))
                    val satGradient = Brush.horizontalGradient(listOf(Color.White, pureHueColor))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(satGradient)
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Value / Brightness Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Brightness", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${(value * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                    }
                    val fullSatColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                    val valGradient = Brush.horizontalGradient(listOf(Color.Black, fullSatColor))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(valGradient)
                    )
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Opacity / Alpha Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Opacity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${(alpha * 100).toInt()}%", fontSize = 11.sp, color = Color.Gray)
                    }
                    val solidColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))
                    val alphaGradient = Brush.horizontalGradient(listOf(Color.Transparent, solidColor))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(alphaGradient)
                    )
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("Quick Preset Swatches", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { pColor ->
                        val isSelected = (currentColorInt and 0x00FFFFFF) == (pColor and 0x00FFFFFF)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(pColor))
                                .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color(0xFF8B5CF6) else Color.DarkGray, CircleShape)
                                .clickable {
                                    val newHsv = FloatArray(3).apply { AndroidColor.colorToHSV(pColor, this) }
                                    hue = newHsv[0]
                                    saturation = newHsv[1]
                                    value = newHsv[2]
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = if (pColor == AndroidColor.WHITE || pColor == AndroidColor.YELLOW) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColorInt)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
            ) {
                Text("Apply Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
