package com.mymoises.mystickerapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// Red Theme Palette Definitions
private val primaryRed = Color(0xFFFF1744)
private val deepRed = Color(0xFFD50032)
private val redGradient = Brush.horizontalGradient(listOf(primaryRed, deepRed))
private val softPink = Color(0xFFFFCDD2)
private val bgLightPink = Color(0xFFFFF5F7)
private val darkNavy = Color(0xFF10143A)

@Composable
fun TemplatesScreen(
    onCreateNewTemplate: () -> Unit,
    onUseUserTemplate: (UserStickerTemplate) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: Community Discovery, 1: My Templates
    var activeDiscoverySection by remember { mutableStateOf("Trending") } // Trending, Popular, Newest, All
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var templatesRefresh by remember { mutableIntStateOf(0) }

    var renameTarget by remember { mutableStateOf<UserStickerTemplate?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<UserStickerTemplate?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var previewTarget by remember { mutableStateOf<UserStickerTemplate?>(null) }

    val communityTemplates = remember(activeDiscoverySection, selectedCategory, searchQuery, templatesRefresh) {
        UserStickerTemplatesManager.getDiscoveryTemplates(
            context = context,
            section = activeDiscoverySection,
            categoryFilter = selectedCategory,
            searchQuery = searchQuery
        )
    }

    val myTemplates = remember(templatesRefresh) {
        UserStickerTemplatesManager.getMyTemplates(context)
    }

    val categoryList = TemplateCategories.CATEGORIES_LIST
    val discoverySections = listOf(
        "Trending" to "🔥 Trending",
        "Popular" to "⭐ Popular",
        "Newest" to "✨ Newest",
        "All" to "🌐 All"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgLightPink)
            .padding(12.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, softPink),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = darkNavy
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("← Back", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🖼️ ", fontSize = 20.sp)
                Text(
                    text = "Templates",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkNavy
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(redGradient)
                    .clickable { onCreateNewTemplate() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("＋ Create", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Tab Switcher with Red Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFF0F3))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Community Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activeTab == 0) redGradient else Brush.horizontalGradient(listOf(Color.White, Color.White)))
                        .clickable { activeTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🌐 Discovery",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (activeTab == 0) Color.White else darkNavy
                    )
                }

                // My Templates Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activeTab == 1) redGradient else Brush.horizontalGradient(listOf(Color.White, Color.White)))
                        .clickable { activeTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "👤 My Templates (${myTemplates.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (activeTab == 1) Color.White else darkNavy
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search and Clear Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search templates by name, keyword...", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", fontSize = 16.sp, color = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = primaryRed,
                    unfocusedBorderColor = softPink
                ),
                modifier = Modifier.weight(1f).height(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Display
        if (activeTab == 0) {
            // Discovery Section Selector Chips (Trending, Popular, Newest)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                discoverySections.forEach { (secKey, label) ->
                    val isSel = activeDiscoverySection == secKey
                    FilterChip(
                        selected = isSel,
                        onClick = { activeDiscoverySection = secKey },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = darkNavy
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSel,
                            borderColor = softPink,
                            selectedBorderColor = primaryRed
                        )
                    )
                }
            }

            // Category Swipeable Horizontal Menu
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categoryList) { cat ->
                    val isSel = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) redGradient else Brush.horizontalGradient(listOf(Color.White, Color.White)))
                            .border(if (isSel) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, softPink), RoundedCornerShape(16.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else darkNavy
                        )
                    }
                }
            }

            if (communityTemplates.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍 No Templates Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = darkNavy)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No templates match '$searchQuery'." else "No templates in category '$selectedCategory'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || selectedCategory != "All") {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { searchQuery = ""; selectedCategory = "All"; activeDiscoverySection = "Trending" }) {
                                Text("Clear Search & Filter")
                            }
                        }
                    }
                }
            } else {
                // EXACTLY 4 COLUMNS GRID
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(communityTemplates) { template ->
                        CommunityTemplateItemCard(
                            template = template,
                            onUse = { previewTarget = template },
                            onLikeToggle = { templatesRefresh++ }
                        )
                    }
                }
            }
        } else {
            // My Templates Section
            if (myTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📋 No Custom Templates Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkNavy
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '＋ Create' to design your custom sticker template!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Header Bar for My Templates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Showing ${myTemplates.size} Custom Templates",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkNavy
                        )

                        Button(
                            onClick = { showDeleteAllConfirmDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = deepRed)
                        ) {
                            Text("🗑️ Delete All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // 2-COLUMN GRID FOR MY TEMPLATES WITH EDITING CONTROLS
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(myTemplates) { template ->
                            MyTemplateItemCard(
                                template = template,
                                onPreview = { previewTarget = template },
                                onTogglePublish = {
                                    UserStickerTemplatesManager.publishUserTemplate(context, template.id, !template.isPublished)
                                    templatesRefresh++
                                },
                                onRename = {
                                    renameTarget = template
                                    renameInputText = template.name
                                },
                                onDelete = {
                                    deleteTarget = template
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text("Delete All My Templates?", color = darkNavy, fontWeight = FontWeight.Bold) },
            text = { Text("Sigurado ka bang gusto mong burahin ang LAHAT ng iyong sariling templates? Hindi ito mababawi. Ang community templates ay HINDI mabubura.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (UserStickerTemplatesManager.deleteAllMyTemplates(context)) {
                            Toast.makeText(context, "Lahat ng iyong templates ay na-delete na.", Toast.LENGTH_SHORT).show()
                            templatesRefresh++
                        }
                        showDeleteAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = deepRed)
                ) { Text("Delete All") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    renameTarget?.let { template ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Template", color = darkNavy, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Template Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (UserStickerTemplatesManager.renameUserTemplate(context, template.id, renameInputText)) {
                            Toast.makeText(context, "Template renamed!", Toast.LENGTH_SHORT).show()
                            templatesRefresh++
                        }
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryRed)
                ) { Text("Rename") }
            },
            dismissButton = {
                OutlinedButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Template", color = darkNavy, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete template '${template.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (UserStickerTemplatesManager.deleteUserTemplate(context, template.id)) {
                            Toast.makeText(context, "Template deleted!", Toast.LENGTH_SHORT).show()
                            templatesRefresh++
                        }
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = deepRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Separate Template Preview Screen Dialog
    previewTarget?.let { template ->
        TemplatePreviewDialog(
            template = template,
            onDismiss = { previewTarget = null },
            onUseTemplate = { selected ->
                previewTarget = null
                onUseUserTemplate(selected)
            }
        )
    }
}

// 4-COLUMN COMMUNITY TEMPLATE CARD
@Composable
private fun CommunityTemplateItemCard(
    template: UserStickerTemplate,
    onUse: () -> Unit,
    onLikeToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val previewBitmap = remember(template.previewPath) {
        val file = File(template.previewPath)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    var likesCount by remember(template.id, template.likeCount) { mutableIntStateOf(template.likeCount) }
    var isLiked by remember(template.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUse() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Box - Whole thumbnail clickable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF9F9F9))
                    .clickable { onUse() },
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = template.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🎨", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title & Creator
            Text(
                text = template.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = darkNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "by @${template.creatorName}",
                fontSize = 8.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Download Metric & Like Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📥 ${template.useCount}",
                    fontSize = 8.sp,
                    color = Color.Gray
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isLiked = !isLiked
                        likesCount = UserStickerTemplatesManager.toggleTemplateLike(context, template.id, isLiked)
                        onLikeToggle()
                    }
                ) {
                    Text(
                        text = if (isLiked) "❤️" else "♡",
                        fontSize = 9.sp,
                        color = primaryRed
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$likesCount",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ✨ Preview Template Red Gradient Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(redGradient)
                    .clickable { onUse() }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("👁️ Preview", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun MyTemplateItemCard(
    template: UserStickerTemplate,
    onPreview: () -> Unit,
    onTogglePublish: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val previewBitmap = remember(template.previewPath) {
        val file = File(template.previewPath)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Box - Whole thumbnail clickable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF9F9F9))
                    .clickable { onPreview() },
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = template.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("📋 Custom", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = darkNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable { onPreview() }
                )

                Text(
                    text = if (template.isPublished) "🌐 Public" else "🔒 Private",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (template.isPublished) Color(0xFF2E7D32) else Color(0xFFC2410C),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (template.isPublished) Color(0xFFE8F5E9) else Color(0xFFFFEDD5))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("👁️ Preview", fontSize = 11.sp, color = primaryRed)
                }

                TextButton(
                    onClick = onTogglePublish,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(if (template.isPublished) "🔒 Make Private" else "🌐 Make Public", fontSize = 11.sp, color = Color(0xFF0284C7))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onRename,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("📝 Rename", fontSize = 11.sp, color = Color(0xFF1565C0))
                }

                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("🗑️ Delete", fontSize = 11.sp, color = deepRed)
                }
            }
        }
    }
}

@Composable
fun ZoomablePreviewBox(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    contentDescription: String = "Preview"
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val zoomPercent = (scale * 100).roundToInt()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E182E))
            .border(1.dp, Color(0xFF383248), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1.0f, 5.0f)
                        if (scale > 1.0f) {
                            offsetX = (offsetX + pan.x).coerceIn(-300f * (scale - 1f), 300f * (scale - 1f))
                            offsetY = (offsetY + pan.y).coerceIn(-300f * (scale - 1f), 300f * (scale - 1f))
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Checkerboard(Modifier.matchParentSize())

            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            } else {
                Text("🎨", fontSize = 48.sp)
            }
        }

        // Floating Zoom Controls: [-] 100% [+] [Reset]
        androidx.compose.material3.Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xDD10143A),
            border = BorderStroke(1.dp, primaryRed)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom Out
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2438))
                        .clickable {
                            scale = (scale / 1.25f).coerceAtLeast(1.0f)
                            if (scale == 1.0f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Percentage Indicator
                Text(
                    text = "$zoomPercent%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Zoom In
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2438))
                        .clickable {
                            scale = (scale * 1.25f).coerceAtMost(5.0f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Reset
                if (scale != 1.0f || offsetX != 0f || offsetY != 0f) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryRed)
                            .clickable {
                                scale = 1.0f
                                offsetX = 0f
                                offsetY = 0f
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reset", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// SEPARATE TEMPLATE PREVIEW SCREEN DIALOG
@Composable
fun TemplatePreviewDialog(
    template: UserStickerTemplate,
    onDismiss: () -> Unit,
    onUseTemplate: (UserStickerTemplate) -> Unit
) {
    val context = LocalContext.current
    val previewBitmap = remember(template.previewPath) {
        val file = File(template.previewPath)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Navigation Header with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("← Back", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryRed)
                    }

                    Text(
                        text = "Template Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = darkNavy
                    )

                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Large Zoomable Preview Display Box
                ZoomablePreviewBox(
                    bitmap = previewBitmap,
                    contentDescription = template.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Template Information & Creator Trigger
                var showCreatorProfile by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = template.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = darkNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category: ${template.category}", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "by @${template.creatorName} 👤",
                            fontSize = 12.sp,
                            color = primaryRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showCreatorProfile = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📥 ${template.useCount} Uses", fontSize = 11.sp, color = Color.Gray)
                        Text("❤️ ${template.likeCount} Likes", fontSize = 11.sp, color = primaryRed, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Use Template & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(redGradient)
                            .clickable {
                                UserStickerTemplatesManager.recordTemplateUsage(context, template.id)
                                onUseTemplate(template)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎨 Use Template",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = "Check out the '${template.name}' sticker template by @${template.creatorName} on Sticker Craft! 🎨 Download and customize it now!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Sticker Template: ${template.name}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Template"))
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, softPink)
                    ) {
                        Text("📤 Share", fontSize = 13.sp, color = darkNavy, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, softPink)
                ) {
                    Text("Back to Templates", fontSize = 13.sp, color = Color.Gray)
                }

                if (showCreatorProfile) {
                    CreatorProfileDialog(
                        creatorName = template.creatorName,
                        onDismiss = { showCreatorProfile = false },
                        onUseTemplate = { selected ->
                            showCreatorProfile = false
                            onUseTemplate(selected)
                        }
                    )
                }
            }
        }
    }
}

// CREATOR PROFILE DIALOG
@Composable
fun CreatorProfileDialog(
    creatorName: String,
    onDismiss: () -> Unit,
    onUseTemplate: (UserStickerTemplate) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mysticker_settings", Context.MODE_PRIVATE) }
    var followedIds by remember {
        mutableStateOf(prefs.getStringSet("followed_creators", setOf("c1")) ?: setOf("c1"))
    }

    val topCreators = remember { UserStickerTemplatesManager.getTopCreators(context) }
    val creatorStats = remember(creatorName, topCreators) {
        topCreators.firstOrNull { it.name.equals(creatorName, ignoreCase = true) } ?: CreatorStatsData(
            id = "creator_${creatorName.lowercase().replace(" ", "_")}",
            name = creatorName,
            handle = "@${creatorName.lowercase().replace(" ", "_")}",
            avatar = "🎨",
            bio = "Sticker Craft Creator",
            templatesCount = UserStickerTemplatesManager.getTemplatesByCreator(context, creatorName).size,
            totalUses = UserStickerTemplatesManager.getTemplatesByCreator(context, creatorName).sumOf { it.useCount },
            totalLikes = UserStickerTemplatesManager.getTemplatesByCreator(context, creatorName).sumOf { it.likeCount },
            engagementScore = 100L,
            verified = true,
            templates = UserStickerTemplatesManager.getTemplatesByCreator(context, creatorName)
        )
    }

    val isFollowing = followedIds.contains(creatorStats.id)
    var selectedPreviewTemplate by remember { mutableStateOf<UserStickerTemplate?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("← Back", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryRed)
                    }
                    Text("Creator Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = darkNavy)
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Avatar, Name, Handle, Verified, Follow Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFFFFF0F3), CircleShape)
                            .border(2.dp, primaryRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(creatorStats.avatar, fontSize = 28.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(creatorStats.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = darkNavy)
                            if (creatorStats.verified) {
                                Spacer(Modifier.width(4.dp))
                                Text("✔️", fontSize = 12.sp, color = Color(0xFF1685F8))
                            }
                        }
                        Text(creatorStats.handle, fontSize = 12.sp, color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val newSet = if (isFollowing) followedIds - creatorStats.id else followedIds + creatorStats.id
                            followedIds = newSet
                            prefs.edit().putStringSet("followed_creators", newSet).apply()
                            Toast.makeText(context, if (!isFollowing) "Following ${creatorStats.name}!" else "Unfollowed ${creatorStats.name}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) Color.DarkGray else primaryRed),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(if (isFollowing) "Following ✓" else "＋ Follow", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(creatorStats.bio, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(14.dp))

                // Real Engagement Metrics Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF5F7))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${creatorStats.templatesCount}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = darkNavy)
                        Text("Templates", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${creatorStats.totalUses}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = darkNavy)
                        Text("Uses 📥", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${creatorStats.totalLikes}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryRed)
                        Text("Likes ❤️", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Published Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = darkNavy, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(8.dp))

                // Creator Templates 4-Column Grid
                val templates = creatorStats.templates
                if (templates.isEmpty()) {
                    Text("No published templates yet.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(templates) { template ->
                            CommunityTemplateItemCard(
                                template = template,
                                onUse = { selectedPreviewTemplate = template }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPreviewTemplate?.let { template ->
        TemplatePreviewDialog(
            template = template,
            onDismiss = { selectedPreviewTemplate = null },
            onUseTemplate = { selected ->
                selectedPreviewTemplate = null
                onDismiss()
                onUseTemplate(selected)
            }
        )
    }
}
