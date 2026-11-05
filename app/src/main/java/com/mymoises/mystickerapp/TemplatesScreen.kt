package com.mymoises.mystickerapp

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun TemplatesScreen(
    onCreateNewTemplate: () -> Unit,
    onUseUserTemplate: (UserStickerTemplate) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: Community Templates, 1: My Templates
    var selectedCategory by remember { mutableStateOf("All") }
    var templatesRefresh by remember { mutableIntStateOf(0) }

    var renameTarget by remember { mutableStateOf<UserStickerTemplate?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<UserStickerTemplate?>(null) }

    val communityTemplates = remember(selectedCategory, templatesRefresh) {
        UserStickerTemplatesManager.getCommunityTemplates(context, selectedCategory)
    }

    val myTemplates = remember(templatesRefresh) {
        UserStickerTemplatesManager.getMyTemplates(context)
    }

    val categoryList = listOf("All", "Cute", "Love", "Funny", "Reactions", "Meme", "Speech Bubbles")

    Column(
        modifier = Modifier
            .fillMaxSize()
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

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "🎨 Templates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onCreateNewTemplate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C55))
            ) {
                Text("➕ Create", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Tab Switcher
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0xFFFFF2F5),
            contentColor = Color(0xFFFF2C55),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = Color(0xFFFF2C55)
                )
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("🌐 Community Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("👤 My Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Display
        if (activeTab == 0) {
            // Community Gallery Category Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(categoryList) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF2C55),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFFFF2F5),
                            labelColor = Color(0xFF4A4A4A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (communityTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No community templates found in this category.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(communityTemplates) { template ->
                        CommunityTemplateItemCard(
                            template = template,
                            onUse = { onUseUserTemplate(template) }
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
                            color = Color(0xFF1E1E1E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '➕ Create' to design your custom sticker template!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myTemplates) { template ->
                        MyTemplateItemCard(
                            template = template,
                            onEdit = { onUseUserTemplate(template) },
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

    // Rename Dialog
    renameTarget?.let { template ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Template") },
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
                Button(onClick = {
                    if (UserStickerTemplatesManager.renameUserTemplate(context, template.id, renameInputText)) {
                        Toast.makeText(context, "Template renamed!", Toast.LENGTH_SHORT).show()
                        templatesRefresh++
                    }
                    renameTarget = null
                }) { Text("Rename") }
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
            title = { Text("Delete Template") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CommunityTemplateItemCard(
    template: UserStickerTemplate,
    onUse: () -> Unit
) {
    val previewBitmap = remember(template.previewPath) {
        val file = File(template.previewPath)
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = template.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🎨 Community", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Creator
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "by @${template.creatorName}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Use This Template Button
            Button(
                onClick = onUse,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C55))
            ) {
                Text("✨ Use Template", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun MyTemplateItemCard(
    template: UserStickerTemplate,
    onEdit: () -> Unit,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = template.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("📋 Custom", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    color = Color(0xFF1E1E1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (template.isPublished) "Public" else "Private",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (template.isPublished) Color(0xFF2E7D32) else Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (template.isPublished) Color(0xFFE8F5E9) else Color(0xFFEEEEEE))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("✏️ Edit", fontSize = 11.sp, color = Color(0xFFFF2C55))
                }

                TextButton(
                    onClick = onTogglePublish,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(if (template.isPublished) "🌐 Unpublish" else "🌐 Publish", fontSize = 11.sp, color = Color(0xFF0284C7))
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
                    Text("🗑️ Delete", fontSize = 11.sp, color = Color(0xFFC62828))
                }
            }
        }
    }
}
