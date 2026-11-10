package com.mymoises.mystickerapp

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.saveMyStickerBitmap

@Composable
fun TemplateEditorScreen(
    template: StickerTemplate,
    onStickerSaved: () -> Unit = {},
    onOpenInStickerEditor: ((Bitmap) -> Unit)? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var rewardedAdOfferSpec by remember { mutableStateOf<RewardedAdOfferSpec?>(null) }
    var userText by remember { mutableStateOf(template.defaultText) }
    var userPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var userTextColor by remember { mutableStateOf(template.textColor) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
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
            userPhotoBitmap = bitmap
        }
    }

    val liveBitmap = remember(userText, userPhotoBitmap, userTextColor) {
        renderTemplateToBitmap(
            context = context,
            template = template,
            text = userText,
            textColor = userTextColor,
            textSizeSp = 36f,
            photoBitmap = userPhotoBitmap,
            targetSizePx = 512
        )
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
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Category: ${template.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF5F5F7))
                .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Checkerboard(Modifier.matchParentSize())
            Image(
                bitmap = liveBitmap.asImageBitmap(),
                contentDescription = template.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "✏️ Customize Text & Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userText,
                    onValueChange = { userText = it },
                    label = { Text("Sticker Text") },
                    placeholder = { Text("Type custom text...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "🎨 Choose Text Color:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val paletteColors = listOf(
                    Color.White, Color.Black, Color(0xFFFF2C55), Color(0xFFFFD700),
                    Color(0xFF00E676), Color(0xFF29B6F6), Color(0xFFAB47BC), Color(0xFFFF9800)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(paletteColors) { color ->
                        val isSelected = userTextColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFF2C55) else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { userTextColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C55))
                    ) {
                        Text(if (userPhotoBitmap == null) "🖼️ Add Photo" else "🖼️ Change Photo")
                    }

                    if (userPhotoBitmap != null) {
                        OutlinedButton(
                            onClick = { userPhotoBitmap = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("❌ Remove")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image & Export") {
                        val saved = saveMyStickerBitmap(context, liveBitmap)
                        if (saved) {
                            Toast.makeText(context, "Sticker saved to My Stickers!", Toast.LENGTH_SHORT).show()
                            onStickerSaved()
                            onBack()
                        } else {
                            Toast.makeText(context, "Failed to save sticker.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2C55),
                    contentColor = Color.White
                )
            ) {
                Text("💾 Save PNG", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Project & Template") {
                        val layerDataList = listOf(
                            Triple(
                                template.title,
                                liveBitmap,
                                UserTemplateLayerData(
                                    name = template.title,
                                    bitmapFileName = null,
                                    visible = true
                                )
                            )
                        )
                        val saved = UserStickerTemplatesManager.saveUserTemplate(
                            context = context,
                            templateName = template.title,
                            layerDataList = layerDataList,
                            previewBitmap = liveBitmap
                        )
                        if (saved) {
                            Toast.makeText(context, "Saved to My Templates!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not save template.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White
                )
            ) {
                Text("📋 Save Template", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (onOpenInStickerEditor != null) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onOpenInStickerEditor(liveBitmap) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("✏️ Open in Sticker Editor", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF2C55))
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
