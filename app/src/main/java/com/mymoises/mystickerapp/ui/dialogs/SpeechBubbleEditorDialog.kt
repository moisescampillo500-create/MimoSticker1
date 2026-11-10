package com.mymoises.mystickerapp.ui.dialogs

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mymoises.mystickerapp.FontRegistry
import com.mymoises.mystickerapp.ZoomablePreviewBox
import com.mymoises.mystickerapp.model.StickerEditorLayer
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.createSpeechBubbleBitmap

@Composable
fun SpeechBubbleEditorDialog(
    editingSpeechBubbleLayer: StickerEditorLayer?,
    initialBubbleStyle: String,
    initialText: String,
    initialTextColorIndex: Int,
    initialFillColorIndex: Int,
    initialStrokeColorIndex: Int,
    initialTextSizeSp: Float,
    initialOutlineThicknessSp: Float,
    initialOpacityAlpha: Float,
    initialTailDirection: String,
    initialFontFamily: String,
    initialFontStyle: String,
    initialTextAlignName: String,
    palette: List<Int>,
    onOpenFontPicker: () -> Unit,
    onSave: (
        bubbleStyle: String,
        text: String,
        textColorIndex: Int,
        fillColorIndex: Int,
        strokeColorIndex: Int,
        textSizeSp: Float,
        outlineThicknessSp: Float,
        opacityAlpha: Float,
        tailDirection: String,
        fontFamily: String,
        fontStyle: String,
        textAlignName: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedBubbleStyle by remember { mutableStateOf(initialBubbleStyle) }
    var bubbleText by remember { mutableStateOf(initialText) }
    var bubbleTextColorIndex by remember { mutableIntStateOf(initialTextColorIndex) }
    var bubbleFillColorIndex by remember { mutableIntStateOf(initialFillColorIndex) }
    var bubbleStrokeColorIndex by remember { mutableIntStateOf(initialStrokeColorIndex) }
    var bubbleTextSizeSp by remember { mutableFloatStateOf(initialTextSizeSp) }
    var bubbleOutlineThicknessSp by remember { mutableFloatStateOf(initialOutlineThicknessSp) }
    var bubbleOpacityAlpha by remember { mutableFloatStateOf(initialOpacityAlpha) }
    var bubbleTailDirection by remember { mutableStateOf(initialTailDirection) }
    var bubbleFontFamily by remember { mutableStateOf(initialFontFamily) }
    var bubbleFontStyle by remember { mutableStateOf(initialFontStyle) }
    var bubbleTextAlignName by remember { mutableStateOf(initialTextAlignName) }

    var isBubblePreviewMinimized by remember { mutableStateOf(false) }
    var isBubblePreviewHidden by remember { mutableStateOf(false) }
    var showFontPickerModalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bubbleFontFamily) {
        FontRegistry.loadFontTypeface(context, bubbleFontFamily)
    }

    val bubbleStyles = listOf(
        "Classic Comic Bubble", "Cloud Thought Bubble", "Comic Explosion",
        "Shout Bubble", "Oval Dialogue Bubble", "Heart Bubble",
        "Pixel Bubble", "Double Dialogue Bubble", "Ribbon Bubble",
        "Question Bubble", "Exclamation Bubble", "Emotional Reaction Bubble"
    )

    val tailDirections = listOf("Bottom Left", "Bottom Right", "Top Left", "Top Right")

    val mainPreviewBmp = remember(
        selectedBubbleStyle, bubbleText, bubbleTextColorIndex,
        bubbleTextSizeSp, bubbleFillColorIndex, bubbleStrokeColorIndex,
        bubbleOutlineThicknessSp, bubbleOpacityAlpha, bubbleTailDirection,
        bubbleFontFamily, bubbleFontStyle, bubbleTextAlignName
    ) {
        createSpeechBubbleBitmap(
            bubbleStyle = selectedBubbleStyle,
            text = bubbleText,
            textColorInt = palette[bubbleTextColorIndex.coerceIn(0, palette.lastIndex)],
            textSizeSp = bubbleTextSizeSp,
            fillColorInt = palette[bubbleFillColorIndex.coerceIn(0, palette.lastIndex)],
            strokeColorInt = palette[bubbleStrokeColorIndex.coerceIn(0, palette.lastIndex)],
            strokeThicknessPx = bubbleOutlineThicknessSp,
            opacityAlpha = bubbleOpacityAlpha,
            tailDirection = bubbleTailDirection,
            fontFamily = bubbleFontFamily,
            fontStyle = bubbleFontStyle,
            textAlignName = bubbleTextAlignName,
            context = context
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF9F7FE),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp).heightIn(max = 570.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF8B5CF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", color = Color.White, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (editingSpeechBubbleLayer != null) "Edit Speech Bubble" else "Add Speech Bubble",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E1065)
                            )
                            Text("Choose style, text, font & colors.", fontSize = 11.sp, color = Color(0xFF6B7280))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (!isBubblePreviewHidden) {
                    if (isBubblePreviewMinimized) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E1065))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isBubblePreviewMinimized = false }
                            ) {
                                Text("💬", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("Bubble Preview (Click to restore)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔍 Restore", fontSize = 11.sp, color = Color(0xFFA78BFA), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isBubblePreviewMinimized = false })
                                Text("✕", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.clickable { isBubblePreviewHidden = true })
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2E1065))
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("💬 Sticky Canvas Preview", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("➖ Minimize", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.clickable { isBubblePreviewMinimized = true })
                                    Text("✕ Hide", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.clickable { isBubblePreviewHidden = true })
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            ZoomablePreviewBox(
                                bitmap = mainPreviewBmp,
                                contentDescription = "Speech Bubble Preview",
                                modifier = Modifier.fillMaxWidth().height(145.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    TextButton(
                        onClick = {
                            isBubblePreviewHidden = false
                            isBubblePreviewMinimized = false
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("💬 Restore Preview Canvas", fontSize = 11.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF8B5CF6))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Select Bubble Design (12 Styles):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bubbleStyles.chunked(3).forEach { rowStyles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStyles.forEach { styleName ->
                                    val isSelected = selectedBubbleStyle == styleName
                                    val thumbBmp = remember(styleName) {
                                        createSpeechBubbleBitmap(
                                            bubbleStyle = styleName,
                                            text = "ABC",
                                            textColorInt = AndroidColor.BLACK,
                                            textSizeSp = 20f,
                                            fillColorInt = AndroidColor.WHITE,
                                            strokeColorInt = AndroidColor.BLACK,
                                            strokeThicknessPx = 6f,
                                            bitmapSize = 120
                                        )
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedBubbleStyle = styleName },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF2A2438) else Color(0xFF1E182E)
                                        ),
                                        border = if (isSelected) BorderStroke(2.dp, Color(0xFF8B5CF6)) else BorderStroke(1.dp, Color(0xFF383248))
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(58.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFF2EDFE)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Checkerboard(Modifier.matchParentSize())
                                                    Image(
                                                        bitmap = thumbBmp.asImageBitmap(),
                                                        contentDescription = styleName,
                                                        modifier = Modifier.fillMaxSize().padding(2.dp)
                                                    )
                                                }

                                                Spacer(Modifier.height(4.dp))

                                                Text(
                                                    text = styleName,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF8B5CF6)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = bubbleText,
                        onValueChange = { bubbleText = it },
                        label = { Text("Bubble Text") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Text("Font Family (54 Typefaces):", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                showFontPickerModalDialog = true
                                onOpenFontPicker()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("🔤 Font Picker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        val displayFonts = remember(bubbleFontFamily) {
                            val topFonts = FontRegistry.ALL_FONTS.take(20)
                            val activeSpec = FontRegistry.ALL_FONTS.firstOrNull {
                                it.familyKey == bubbleFontFamily || it.name == bubbleFontFamily || it.id == bubbleFontFamily
                            }
                            if (activeSpec != null && topFonts.none { it.id == activeSpec.id || it.familyKey == activeSpec.familyKey }) {
                                listOf(activeSpec) + topFonts
                            } else {
                                topFonts
                            }
                        }

                        displayFonts.forEach { font ->
                            FilterChip(
                                selected = bubbleFontFamily == font.familyKey || bubbleFontFamily == font.name || bubbleFontFamily == font.id,
                                onClick = { bubbleFontFamily = font.familyKey },
                                label = { Text(font.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (showFontPickerModalDialog) {
                        val prefs = remember { context.getSharedPreferences("sticker_app_prefs", android.content.Context.MODE_PRIVATE) }
                        FontSelectionPanelDialog(
                            initialFontFamily = bubbleFontFamily,
                            sampleText = if (bubbleText.isBlank()) "Bubble Text" else bubbleText,
                            onFontSelected = { font ->
                                bubbleFontFamily = font.familyKey
                            },
                            onApply = { font ->
                                bubbleFontFamily = font.familyKey
                                showFontPickerModalDialog = false
                            },
                            onCancel = {
                                showFontPickerModalDialog = false
                            },
                            prefs = prefs
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Font Style / Weight:", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Regular", "Bold", "Italic", "Bold Italic").forEach { style ->
                            FilterChip(
                                selected = bubbleFontStyle == style,
                                onClick = { bubbleFontStyle = style },
                                label = { Text(style, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Text Alignment:", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Left", "Center", "Right").forEach { align ->
                            FilterChip(
                                selected = bubbleTextAlignName == align,
                                onClick = { bubbleTextAlignName = align },
                                label = { Text(align, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Tail Direction:", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tailDirections.forEach { dir ->
                            FilterChip(
                                selected = bubbleTailDirection == dir,
                                onClick = { bubbleTailDirection = dir },
                                label = { Text(dir, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Text Color:", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        palette.forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (bubbleTextColorIndex == idx) 2.dp else 0.dp, Color(0xFF8B5CF6), CircleShape)
                                    .clickable { bubbleTextColorIndex = idx }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Fill Color:", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        palette.forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (bubbleFillColorIndex == idx) 2.dp else 0.dp, Color(0xFF8B5CF6), CircleShape)
                                    .clickable { bubbleFillColorIndex = idx }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Outline Color:", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        palette.forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (bubbleStrokeColorIndex == idx) 2.dp else 0.dp, Color(0xFF8B5CF6), CircleShape)
                                    .clickable { bubbleStrokeColorIndex = idx }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Font Size: ${bubbleTextSizeSp.toInt()}sp", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = bubbleTextSizeSp,
                            onValueChange = { bubbleTextSizeSp = it },
                            valueRange = 18f..60f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Outline Thickness: ${bubbleOutlineThicknessSp.toInt()}px", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = bubbleOutlineThicknessSp,
                            onValueChange = { bubbleOutlineThicknessSp = it },
                            valueRange = 2f..24f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity: ${(bubbleOpacityAlpha * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF2E1065), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = bubbleOpacityAlpha,
                            onValueChange = { bubbleOpacityAlpha = it },
                            valueRange = 0.2f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val textToSave = if (bubbleText.isBlank()) "Hello!" else bubbleText
                            onSave(
                                selectedBubbleStyle, textToSave, bubbleTextColorIndex,
                                bubbleFillColorIndex, bubbleStrokeColorIndex, bubbleTextSizeSp,
                                bubbleOutlineThicknessSp, bubbleOpacityAlpha, bubbleTailDirection,
                                bubbleFontFamily, bubbleFontStyle, bubbleTextAlignName
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(if (editingSpeechBubbleLayer != null) "Save Changes" else "Add Speech Bubble", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF8B5CF6))
                    ) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6A1B9A))
                    }
                }
            }
        }
    }
}
