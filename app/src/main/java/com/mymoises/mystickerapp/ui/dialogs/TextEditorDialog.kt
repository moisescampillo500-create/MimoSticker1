package com.mymoises.mystickerapp.ui.dialogs

import android.content.SharedPreferences
import android.graphics.Typeface
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.FontRegistry
import com.mymoises.mystickerapp.FontSpec
import com.mymoises.mystickerapp.font.FontLibrary
import com.mymoises.mystickerapp.model.StickerEditorLayer
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.createTextStickerBitmap

private val editorRedColor = Color(0xFFFF1744)

@Composable
fun TextEditorDialog(
    editingTextLayer: StickerEditorLayer?,
    initialText: String,
    initialFontFamily: String,
    initialFontStyle: String,
    initialTextAlignName: String,
    initialFontSizeSp: Float,
    initialTextColorIndex: Int,
    initialOutlineColorIndex: Int,
    initialOutlineThicknessPx: Float,
    initialLetterSpacingEm: Float,
    initialHasShadow: Boolean,
    palette: List<Int>,
    prefs: SharedPreferences,
    onSave: (
        text: String,
        fontFamily: String,
        fontStyle: String,
        textAlignName: String,
        fontSizeSp: Float,
        textColorIndex: Int,
        outlineColorIndex: Int,
        outlineThicknessPx: Float,
        letterSpacingEm: Float,
        hasShadow: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var layerText by remember { mutableStateOf(initialText) }
    var textFontFamily by remember { mutableStateOf(initialFontFamily) }
    var textFontStyle by remember { mutableStateOf(initialFontStyle) }
    var textAlignName by remember { mutableStateOf(initialTextAlignName) }
    var textFontSizeSp by remember { mutableFloatStateOf(initialFontSizeSp) }
    var textColorIndex by remember { mutableIntStateOf(initialTextColorIndex) }
    var textOutlineColorIndex by remember { mutableIntStateOf(initialOutlineColorIndex) }
    var textOutlineThicknessPx by remember { mutableFloatStateOf(initialOutlineThicknessPx) }
    var textLetterSpacingEm by remember { mutableFloatStateOf(initialLetterSpacingEm) }
    var hasTextShadow by remember { mutableStateOf(initialHasShadow) }

    var activeTextTab by remember { mutableStateOf("Fonts") }
    var fontSearchQuery by remember { mutableStateOf("") }
    var fontCategoryFilter by remember { mutableStateOf("All") }
    var favoriteFontIds by remember {
        mutableStateOf(prefs.getStringSet("favorite_font_ids", emptySet()) ?: emptySet())
    }

    val displaySampleText = if (layerText.isBlank()) "PREVIEW TEXT" else layerText
    val isEditing = editingTextLayer != null
    val dialogTitle = if (isEditing) "Edit Text Layer" else "Add Text Layer"

    val livePreviewBmp = remember(
        displaySampleText, textFontFamily, textFontStyle, textAlignName,
        textFontSizeSp, textColorIndex, textOutlineColorIndex,
        textOutlineThicknessPx, hasTextShadow, textLetterSpacingEm
    ) {
        createTextStickerBitmap(
            text = displaySampleText,
            fontFamily = textFontFamily,
            fontStyle = textFontStyle,
            textColorInt = palette[textColorIndex.coerceIn(0, palette.lastIndex)],
            outlineColorInt = palette[textOutlineColorIndex.coerceIn(0, palette.lastIndex)],
            textSizeSp = textFontSizeSp,
            textAlignName = textAlignName,
            outlineThicknessPx = textOutlineThicknessPx,
            hasShadow = hasTextShadow,
            letterSpacingEm = textLetterSpacingEm
        )
    }

    val fontCategories = listOf(
        "All", "Tall & Condensed", "Geometric", "Serif", "Retro", "Handwritten", "Outline", "Bold Display", "Minimalist", "Decorative"
    )

    val filteredFonts = remember(fontSearchQuery, fontCategoryFilter, favoriteFontIds) {
        var list = FontLibrary.FONT_ITEMS
        if (fontCategoryFilter == "★ Favorites") {
            list = list.filter { favoriteFontIds.contains(it.id) }
        } else if (fontCategoryFilter != "All") {
            list = list.filter { it.category.contains(fontCategoryFilter, ignoreCase = true) }
        }

        if (fontSearchQuery.isNotBlank()) {
            list.filter {
                it.displayName.contains(fontSearchQuery, ignoreCase = true) ||
                it.category.contains(fontSearchQuery, ignoreCase = true)
            }
        } else list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tt ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = editorRedColor)
                    Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", color = Color.LightGray, fontSize = 16.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E182E))
                        .border(1.5.dp, editorRedColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Checkerboard(Modifier.matchParentSize())
                    Image(
                        bitmap = livePreviewBmp.asImageBitmap(),
                        contentDescription = "Text Preview",
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text("Your Text", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = layerText,
                    onValueChange = { layerText = it },
                    placeholder = { Text("Preview Text") },
                    singleLine = true,
                    trailingIcon = {
                        if (layerText.isNotEmpty()) {
                            IconButton(onClick = { layerText = "" }) {
                                Text("✕", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Fonts" to "Tt\nFonts",
                        "Style" to "A\nStyle",
                        "Color" to "🎨\nColor",
                        "Outline" to "A\nOutline",
                        "Shadow" to "❐\nShadow",
                        "Spacing" to "↔\nSpacing"
                    ).forEach { (tabKey, tabLabel) ->
                        val isTabSel = activeTextTab == tabKey
                        Button(
                            onClick = { activeTextTab = tabKey },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTabSel) editorRedColor else Color(0xFF2A2A30)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(tabLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                when (activeTextTab) {
                    "Fonts" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = fontSearchQuery,
                                onValueChange = { fontSearchQuery = it },
                                placeholder = { Text("Search fonts…") },
                                leadingIcon = { Text("🔍", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = {
                                fontCategoryFilter = if (fontCategoryFilter == "★ Favorites") "All" else "★ Favorites"
                            }) {
                                Text(
                                    if (fontCategoryFilter == "★ Favorites") "❤️" else "♡",
                                    fontSize = 18.sp,
                                    color = if (fontCategoryFilter == "★ Favorites") editorRedColor else Color.LightGray
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fontCategories.forEach { cat ->
                                val isCatSel = fontCategoryFilter == cat
                                FilterChip(
                                    selected = isCatSel,
                                    onClick = { fontCategoryFilter = cat },
                                    label = { Text(cat, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = editorRedColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (filteredFonts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No fonts found in this category.", fontSize = 11.sp, color = Color.Gray)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredFonts) { font ->
                                    val isFontSel = textFontFamily == font.familyKey
                                    val isFav = favoriteFontIds.contains(font.id)

                                    val loadedTypefaceState = produceState<Typeface?>(initialValue = null, font.id, font.familyKey) {
                                        value = FontRegistry.loadFontTypeface(context, font.id)
                                    }
                                    val tf = loadedTypefaceState.value

                                    val fontSpec = remember(font.id) {
                                        FontRegistry.ALL_FONTS.firstOrNull { it.id == font.id || it.familyKey == font.familyKey }
                                            ?: FontSpec(font.id, font.displayName, font.familyKey, font.category, null)
                                    }

                                    val fontCardPreviewBmp = remember(tf, font.id, displaySampleText) {
                                        if (tf != null) {
                                            FontRegistry.renderPureFontPreview(
                                                sampleText = displaySampleText.take(15),
                                                typeface = tf,
                                                fontSpec = fontSpec,
                                                width = 320,
                                                height = 120
                                            )
                                        } else null
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                textFontFamily = font.familyKey
                                                editingTextLayer?.let { layer ->
                                                    layer.textFontFamily = font.familyKey
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isFontSel) Color(0xFF2E1A22) else Color(0xFF1E1E22)
                                        ),
                                        border = BorderStroke(if (isFontSel) 2.dp else 1.dp, if (isFontSel) editorRedColor else Color(0xFF383842))
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF151518)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (fontCardPreviewBmp != null) {
                                                        Image(
                                                            bitmap = fontCardPreviewBmp.asImageBitmap(),
                                                            contentDescription = font.displayName,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize().padding(2.dp)
                                                        )
                                                    } else {
                                                        CircularProgressIndicator(color = editorRedColor, modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Spacer(Modifier.height(2.dp))

                                                Text(
                                                    font.displayName,
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isFontSel) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(3.dp)
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(editorRedColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                                    .clickable {
                                                        val newSet = if (isFav) favoriteFontIds - font.id else favoriteFontIds + font.id
                                                        favoriteFontIds = newSet
                                                        prefs.edit().putStringSet("favorite_font_ids", newSet).apply()
                                                    }
                                            ) {
                                                Text(if (isFav) "❤️" else "♡", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Style" -> {
                        Text("Font Style / Weight:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Regular", "Bold", "Italic", "Bold Italic").forEach { style ->
                                FilterChip(
                                    selected = textFontStyle == style,
                                    onClick = { textFontStyle = style },
                                    label = { Text(style, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Font Size: ${textFontSizeSp.toInt()}sp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = textFontSizeSp,
                                onValueChange = { textFontSizeSp = it },
                                valueRange = 20f..80f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    "Color" -> {
                        Text("Text Fill Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            palette.forEachIndexed { idx, color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(if (textColorIndex == idx) 2.dp else 0.dp, editorRedColor, CircleShape)
                                        .clickable { textColorIndex = idx }
                                )
                            }
                        }
                    }

                    "Outline" -> {
                        Text("Outline Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            palette.forEachIndexed { idx, color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(if (textOutlineColorIndex == idx) 2.dp else 0.dp, editorRedColor, CircleShape)
                                        .clickable { textOutlineColorIndex = idx }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Outline Thickness: ${textOutlineThicknessPx.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = textOutlineThicknessPx,
                                onValueChange = { textOutlineThicknessPx = it },
                                valueRange = 0f..24f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    "Shadow" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drop Shadow Effect:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            FilterChip(
                                selected = hasTextShadow,
                                onClick = { hasTextShadow = !hasTextShadow },
                                label = { Text(if (hasTextShadow) "✨ Shadow Enabled" else "Shadow Disabled", fontSize = 11.sp) }
                            )
                        }
                    }

                    "Spacing" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Letter Spacing:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = textLetterSpacingEm,
                                onValueChange = { textLetterSpacingEm = it },
                                valueRange = -0.05f..0.3f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Alignment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Left", "Center", "Right").forEach { align ->
                            val isAlignSel = textAlignName == align
                            Button(
                                onClick = { textAlignName = align },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAlignSel) editorRedColor else Color(0xFF2B2B30)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(align, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val textToSave = if (layerText.isBlank()) "Sticker Text" else layerText
                    onSave(
                        textToSave, textFontFamily, textFontStyle, textAlignName,
                        textFontSizeSp, textColorIndex, textOutlineColorIndex,
                        textOutlineThicknessPx, textLetterSpacingEm, hasTextShadow
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = editorRedColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text(if (isEditing) "✓ Save Changes" else "✓ Add Text", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text("Cancel", fontSize = 13.sp, color = Color.LightGray)
            }
        }
    )
}
