package com.mymoises.mystickerapp.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.AdMobRewardedAdManager
import com.mymoises.mystickerapp.RewardedAdOfferDialog
import com.mymoises.mystickerapp.RewardedAdOfferSpec
import com.mymoises.mystickerapp.ui.components.AppButton
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.ui.dialogs.BrushCustomColorPickerDialog
import com.mymoises.mystickerapp.util.DisplayResizedImage
import com.mymoises.mystickerapp.util.HeadSegmentationProcessor
import com.mymoises.mystickerapp.util.renderStickerWithOutlineAndShadow
import com.mymoises.mystickerapp.util.saveMyStickerBitmap
import kotlinx.coroutines.launch

@Composable
fun HeadCutoutScreen(
    autoSave: Boolean,
    onStickerSaved: () -> Unit,
    onOpenInEditor: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val headProcessor = remember { HeadSegmentationProcessor(context) }

    DisposableEffect(Unit) {
        onDispose {
            headProcessor.close()
        }
    }

    var rawHeadCutout by remember { mutableStateOf<Bitmap?>(null) }
    var processedSticker by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Select a photo to isolate head & generate sticker.") }

    var outlineEnabled by remember { mutableStateOf(true) }
    var outlineColorInt by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var outlineThicknessDp by remember { mutableFloatStateOf(12f) }
    var smoothEdges by remember { mutableStateOf(true) }
    var enableShadow by remember { mutableStateOf(false) }

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var rewardedAdOfferSpec by remember { mutableStateOf<RewardedAdOfferSpec?>(null) }

    val presetColors = listOf(
        AndroidColor.WHITE,
        AndroidColor.YELLOW,
        AndroidColor.parseColor("#FF1744"), // Red
        AndroidColor.parseColor("#00E5FF"), // Cyan
        AndroidColor.parseColor("#FF4081"), // Pink
        AndroidColor.parseColor("#00E676"), // Green
        AndroidColor.BLACK,
        AndroidColor.parseColor("#E040FB"), // Purple
        AndroidColor.parseColor("#FF9100"), // Orange
        AndroidColor.parseColor("#2979FF")  // Blue
    )

    fun updateStickerPreview() {
        val raw = rawHeadCutout ?: return
        processedSticker = renderStickerWithOutlineAndShadow(
            cutoutBitmap = raw,
            outlineEnabled = outlineEnabled,
            outlineColorInt = outlineColorInt,
            outlineThicknessDp = outlineThicknessDp,
            smoothEdges = smoothEdges,
            enableShadow = enableShadow,
            density = density
        )
    }

    LaunchedEffect(outlineEnabled, outlineColorInt, outlineThicknessDp, smoothEdges, enableShadow, rawHeadCutout) {
        if (rawHeadCutout != null) {
            updateStickerPreview()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val originalBitmap = try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                null
            }

            if (originalBitmap == null) {
                message = "Unable to open selected image."
            } else {
                scope.launch {
                    isProcessing = true
                    message = "Detecting & isolating head using TFLite Multiclass model…"
                    val cutout = headProcessor.processHeadCutout(originalBitmap)
                    isProcessing = false

                    if (cutout != null) {
                        rawHeadCutout = cutout
                        message = "Head isolated! Customize outline, edges & shadow below."
                    } else {
                        message = "Could not detect head in this image. Try another photo."
                        Toast.makeText(context, "No head detected in photo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppButton("← Back") { onBack() }
            Spacer(Modifier.width(12.dp))
            Text(
                "🗣️ Head Cutout Sticker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Extract hair, face, ears, jawline & chin with MediaPipe TFLite model.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF131722), RoundedCornerShape(16.dp))
                .border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Checkerboard(Modifier.matchParentSize())

            if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFF1744))
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            } else {
                val currentBmp = processedSticker ?: rawHeadCutout
                if (currentBmp != null) {
                    DisplayResizedImage(originalBitmap = currentBmp)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(message, color = Color.LightGray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { galleryLauncher.launch("image/*") },
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🖼️ Select Photo from Gallery", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (rawHeadCutout != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("🎨 Outline & Sticker Controls", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable Sticker Outline", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = outlineEnabled,
                            onCheckedChange = { outlineEnabled = it }
                        )
                    }

                    if (outlineEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Text("Outline Color:", fontSize = 13.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetColors.forEach { colorInt ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (outlineColorInt == colorInt) 3.dp else 1.dp,
                                            color = if (outlineColorInt == colorInt) Color(0xFFFF1744) else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { outlineColorInt = colorInt }
                                )
                            }

                            OutlinedButton(
                                onClick = { showCustomColorDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Custom…", fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Outline Thickness", fontSize = 13.sp)
                            Text("${outlineThicknessDp.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = outlineThicknessDp,
                            onValueChange = { outlineThicknessDp = it },
                            valueRange = 0f..35f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Smooth Outline Edges", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = smoothEdges,
                            onCheckedChange = { smoothEdges = it }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Drop Shadow Effect", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = enableShadow,
                            onCheckedChange = { enableShadow = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val finalSticker = processedSticker ?: rawHeadCutout
                        if (finalSticker != null) {
                            onOpenInEditor(finalSticker)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("➕ Add to Canvas", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val finalSticker = processedSticker ?: rawHeadCutout
                        if (finalSticker != null) {
                            rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                                val saved = saveMyStickerBitmap(context, finalSticker, null)
                                if (saved) {
                                    onStickerSaved()
                                    Toast.makeText(context, "Head Sticker saved to My Stickers!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Could not save sticker.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 Save PNG", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showCustomColorDialog) {
            BrushCustomColorPickerDialog(
                initialColor = outlineColorInt,
                onColorSelected = { selectedColor ->
                    outlineColorInt = selectedColor
                    showCustomColorDialog = false
                },
                onDismiss = { showCustomColorDialog = false }
            )
        }

        rewardedAdOfferSpec?.let { spec ->
            RewardedAdOfferDialog(
                actionType = spec.actionType,
                onWatchAd = {
                    val currentSpec = spec
                    rewardedAdOfferSpec = null
                    val activity = context as? Activity
                    if (activity != null) {
                        if (AdMobRewardedAdManager.isAdAvailable()) {
                            AdMobRewardedAdManager.showRewardedAd(
                                activity = activity,
                                onRewardGranted = {
                                    currentSpec.onExecuteAction()
                                },
                                onAdClosed = { rewardEarned ->
                                    if (!rewardEarned) {
                                        Toast.makeText(context, "Ad completion required to continue.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        } else {
                            AdMobRewardedAdManager.loadAd(activity)
                            Toast.makeText(context, "Ad is loading. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = {
                    rewardedAdOfferSpec = null
                }
            )
        }
    }
}
