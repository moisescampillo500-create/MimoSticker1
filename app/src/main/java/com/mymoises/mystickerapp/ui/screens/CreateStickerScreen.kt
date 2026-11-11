package com.mymoises.mystickerapp.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.AdMobRewardedAdManager
import com.mymoises.mystickerapp.RewardedAdOfferDialog
import com.mymoises.mystickerapp.RewardedAdOfferSpec
import com.mymoises.mystickerapp.performAutoBackgroundRemoval
import com.mymoises.mystickerapp.ui.components.AppButton
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.DisplayResizedImage
import com.mymoises.mystickerapp.util.fetchAiBackgroundBitmap
import com.mymoises.mystickerapp.util.saveMyStickerBitmap
import kotlinx.coroutines.launch

@Composable
fun CreateStickerScreen(
    autoSave: Boolean,
    screenTitle: String = "My Sticker",
    onStickerSaved: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var rewardedAdOfferSpec by remember { mutableStateOf<RewardedAdOfferSpec?>(null) }

    var selectedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var message by remember {
        mutableStateOf("Choose a picture from your Gallery.")
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                null
            }

            selectedBitmap = bitmap

            if (bitmap == null) {
                message = "Unable to open this picture."
            } else if (autoSave) {
                rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                    val saved = saveMyStickerBitmap(context, bitmap, null)

                    if (saved) {
                        onStickerSaved()
                        message = "Picture selected and saved automatically!"
                        Toast.makeText(
                            context,
                            "Sticker saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        message = "Unable to save. Please try again."
                    }
                }
            } else {
                message = "Picture ready! Tap Save Sticker to save it."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppButton(
                text = "← Back",
                onClick = onBack,
                modifier = Modifier
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (screenTitle == "AI Picture-to-Sticker") "🤖 AI Picture-to-Sticker" else "🎨 Create Sticker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "Choose a picture, preview it, and save it as PNG.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF131722), RoundedCornerShape(16.dp))
                .border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Checkerboard(Modifier.matchParentSize())

            val bitmap = selectedBitmap

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Sticker preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            } else {
                Text(
                    "🖼️\n\nYour picture preview will appear here",
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                galleryLauncher.launch("image/*")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("🖼️ Select Photo from Gallery", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                val bitmap = selectedBitmap

                if (bitmap != null) {
                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                        val saved = saveMyStickerBitmap(context, bitmap, null)

                        if (saved) {
                            onStickerSaved()
                            message = "Sticker saved successfully!"

                            Toast.makeText(
                                context,
                                "Sticker saved to My Stickers!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            message = "Unable to save sticker."
                        }
                    }
                }
            },
            enabled = selectedBitmap != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 Save Sticker as PNG")
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

        Spacer(Modifier.height(12.dp))

        Text(
            if (autoSave) {
                "🟢 Auto Save is ON"
            } else {
                "⚪ Auto Save is OFF · Manual save is available"
            },
            fontSize = 14.sp
        )

        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 14.sp)

        Spacer(Modifier.height(16.dp))

        AppButton("← Back to Create") {
            onBack()
        }
    }
}

@Composable
fun TextStickerScreen(
    autoSave: Boolean,
    onStickerSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rewardedAdOfferSpec by remember { mutableStateOf<RewardedAdOfferSpec?>(null) }
    var stickerText by remember { mutableStateOf("Astronaut in a jungle, cold color palette, muted colors, detailed, 8k") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var removeBgToggle by remember { mutableStateOf(true) }

    val presetPrompts = listOf(
        "Astronaut in a jungle, cold color palette, 8k",
        "Cute chibi anime girl with cat ears, kawaii sticker",
        "Cyberpunk cat with neon sunglasses, sticker art",
        "Cute puppy floating in space with stars",
        "Pastel fairy with magical sparkling wings"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✨ AI Prompt & Text Sticker", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Type any Stable Diffusion prompt to generate custom AI stickers!", fontSize = 12.sp, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = stickerText,
            onValueChange = { stickerText = it },
            label = { Text("AI Prompt or Sticker Text") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetPrompts.forEach { prompt ->
                FilterChip(
                    selected = stickerText == prompt,
                    onClick = { stickerText = prompt },
                    label = { Text(prompt.take(28) + "…", fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏁 Remove Background", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = removeBgToggle,
                onCheckedChange = { removeBgToggle = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (stickerText.isBlank()) {
                    Toast.makeText(context, "Type your prompt or text first.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    isGenerating = true
                    Toast.makeText(context, "Generating Stable Diffusion sticker...", Toast.LENGTH_SHORT).show()
                    val generatedBmp = fetchAiBackgroundBitmap(context, stickerText)
                    val baseBmp = generatedBmp ?: run {
                        val fallback = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(fallback)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.RED
                            textSize = 48f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        canvas.drawText(stickerText.take(20), 256f, 140f, paint)
                        fallback
                    }

                    val finalBmp = if (removeBgToggle) {
                        performAutoBackgroundRemoval(context, baseBmp) ?: baseBmp
                    } else baseBmp

                    previewBitmap = finalBmp
                    isGenerating = false

                    if (autoSave) {
                        rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image & Export") {
                            saveMyStickerBitmap(context, finalBmp, null)
                            onStickerSaved()
                            Toast.makeText(context, "AI Sticker saved automatically!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            enabled = !isGenerating && stickerText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generating AI Sticker...")
            } else {
                Text("🤖 Generate Stable Diffusion AI Sticker")
            }
        }

        Spacer(Modifier.height(16.dp))

        previewBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Preview:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.size(180.dp).background(Color(0xFF131722), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkerboard(Modifier.matchParentSize())
                        DisplayResizedImage(originalBitmap = bitmap)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image & Export") {
                                if (saveMyStickerBitmap(context, bitmap, null)) {
                                    onStickerSaved()
                                    Toast.makeText(context, "Saved to My Stickers!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("💾 Save Sticker to My Stickers")
                    }
                }
            }
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
