package com.mymoises.mystickerapp.ui.dialogs

import android.content.SharedPreferences
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.FontRegistry
import com.mymoises.mystickerapp.FontSpec
import com.mymoises.mystickerapp.font.FontItemSpec
import com.mymoises.mystickerapp.font.FontLibrary

val fontPickerRed = Color(0xFFFF1744)

@Composable
fun FontSelectionPanelDialog(
    initialFontFamily: String,
    sampleText: String,
    onFontSelected: (FontItemSpec) -> Unit,
    onApply: (FontItemSpec) -> Unit,
    onCancel: () -> Unit,
    prefs: SharedPreferences
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoriteFontIds by remember {
        mutableStateOf(prefs.getStringSet("favorite_font_ids", emptySet()) ?: emptySet())
    }

    var selectedFont by remember(initialFontFamily) {
        mutableStateOf(
            FontLibrary.FONT_ITEMS.firstOrNull { it.familyKey == initialFontFamily || it.displayName == initialFontFamily }
                ?: FontLibrary.FONT_ITEMS.first()
        )
    }

    val categories = listOf(
        "All", "★ Favorites", "Tall & Condensed", "Geometric", "Elegant Serif",
        "Retro & Vintage", "Handwritten & Script", "Outline & Display", "Bold Display", "Minimalist", "Decorative"
    )

    val filteredFonts = remember(searchQuery, selectedCategory, favoriteFontIds) {
        var list = FontLibrary.FONT_ITEMS
        if (selectedCategory == "★ Favorites") {
            list = list.filter { favoriteFontIds.contains(it.id) }
        } else if (selectedCategory != "All") {
            list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            list.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        } else list
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🔤 Visual Font Picker", fontWeight = FontWeight.Bold, color = fontPickerRed, fontSize = 16.sp)
                Text("${filteredFonts.size} Fonts", fontSize = 11.sp, color = Color.Gray)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search font name or style…") },
                    trailingIcon = { Text("🔍") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = fontPickerRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (filteredFonts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No fonts found in this category.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFonts) { font ->
                            val isSelected = selectedFont.id == font.id
                            val isFav = favoriteFontIds.contains(font.id)

                            val loadedTypefaceState = produceState<Typeface?>(initialValue = null, font.id, font.familyKey) {
                                value = FontRegistry.loadFontTypeface(context, font.id)
                            }
                            val tf = loadedTypefaceState.value

                            val fontSpec = remember(font.id) {
                                FontRegistry.ALL_FONTS.firstOrNull { it.id == font.id || it.familyKey == font.familyKey }
                                    ?: FontSpec(font.id, font.displayName, font.familyKey, font.category, null)
                            }

                            val fontPreviewBmp = remember(tf, font.id, sampleText) {
                                if (tf != null) {
                                    FontRegistry.renderPureFontPreview(
                                        sampleText = if (sampleText.isBlank()) font.displayName else sampleText.take(15),
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
                                        selectedFont = font
                                        onFontSelected(font)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF2E1A22) else Color(0xFF1E1E22)
                                ),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) fontPickerRed else Color(0xFF383842))
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            font.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            if (isFav) "★" else "☆",
                                            fontSize = 12.sp,
                                            color = if (isFav) Color(0xFFFFD700) else Color.Gray,
                                            modifier = Modifier.clickable {
                                                val newSet = if (isFav) favoriteFontIds - font.id else favoriteFontIds + font.id
                                                favoriteFontIds = newSet
                                                prefs.edit().putStringSet("favorite_font_ids", newSet).apply()
                                            }
                                        )
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF151518)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (fontPreviewBmp != null) {
                                            Image(
                                                bitmap = fontPreviewBmp.asImageBitmap(),
                                                contentDescription = font.displayName,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize().padding(2.dp)
                                            )
                                        } else {
                                            CircularProgressIndicator(color = fontPickerRed, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        font.category,
                                        fontSize = 8.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedFont) },
                colors = ButtonDefaults.buttonColors(containerColor = fontPickerRed)
            ) {
                Text("Apply Font", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
