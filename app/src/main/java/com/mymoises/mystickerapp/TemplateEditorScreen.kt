package com.mymoises.mystickerapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val COLOR_PALETTE = listOf(
    Color.White,
    Color.Black,
    Color(0xFFFF2C55), // Pink/Red
    Color(0xFFE50914), // Red
    Color(0xFFD81B60), // Deep Pink
    Color(0xFF8E24AA), // Purple
    Color(0xFF1E88E5), // Blue
    Color(0xFF00ACC1), // Cyan
    Color(0xFF43A047), // Green
    Color(0xFFFDD835), // Yellow
    Color(0xFFFB8C00)  // Orange
)

@Composable
fun TemplateEditorScreen(
    template: StickerTemplate,
    onStickerSaved: () -> Unit,
    onOpenInStickerEditor: ((Bitmap) -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var textState by remember { mutableStateOf(template.defaultText) }
    var selectedColor by remember { mutableStateOf(template.textColor) }
    var textSizeSp by remember { mutableFloatStateOf(32f) }
    var userPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    userPhotoBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val liveBitmap = remember(template, textState, selectedColor, textSizeSp, userPhotoBitmap) {
        renderTemplateToBitmap(
            context = context,
            template = template,
            text = textState,
            textColor = selectedColor,
            textSizeSp = textSizeSp,
            photoBitmap = userPhotoBitmap
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFF2F5),
                    contentColor = Color(0xFFFF2C55)
                )
            ) {
                Text("← Back", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "✏️ Edit Template",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Preview Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = liveBitmap.asImageBitmap(),
                    contentDescription = "Template Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Controls Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Customize Text",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text("Sticker Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Text Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1E1E)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(COLOR_PALETTE) { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) Color(0xFFFF2C55) else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Text Size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E)
                    )
                    Text(
                        text = "${textSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Slider(
                    value = textSizeSp,
                    onValueChange = { textSizeSp = it },
                    valueRange = 18f..56f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF2C55),
                        activeTrackColor = Color(0xFFFF2C55)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Custom Photo/Sticker",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1E1E)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

        // Save Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val saved = saveMyStickerBitmap(context, liveBitmap)
                    if (saved) {
                        Toast.makeText(context, "Sticker saved to My Stickers!", Toast.LENGTH_SHORT).show()
                        onStickerSaved()
                        onBack()
                    } else {
                        Toast.makeText(context, "Failed to save sticker.", Toast.LENGTH_SHORT).show()
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
    }
}
