package com.mymoises.mystickerapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.BitmapCacheManager
import com.mymoises.mystickerapp.TemplatePreviewDialog
import com.mymoises.mystickerapp.UserStickerTemplate
import com.mymoises.mystickerapp.UserStickerTemplatesManager
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.loadMyStickerFiles
import java.io.File

@Composable
fun MyProjectsScreen(
    onContinueEditTemplate: (UserStickerTemplate) -> Unit,
    onEditStickerFile: (File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var previewProjectTarget by remember { mutableStateOf<UserStickerTemplate?>(null) }

    val userTemplates = remember(refreshKey) {
        UserStickerTemplatesManager.getMyTemplates(context)
    }

    val stickerFiles = remember(refreshKey) {
        loadMyStickerFiles(context)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text("📁 My Projects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Editable Projects (${userTemplates.size})") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF1744),
                    selectedLabelColor = Color.White
                )
            )

            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Saved Stickers (${stickerFiles.size})") },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF1744),
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            if (userTemplates.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📁 No Saved Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Save your work as a template in the editor to continue editing later.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                userTemplates.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { template ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { previewProjectTarget = template },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    val previewBmp = remember(template.previewPath) {
                                        BitmapCacheManager.loadCachedBitmap(template.previewPath, 256)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(Color(0xFFF8F8F8))
                                            .clickable { previewProjectTarget = template },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Checkerboard(Modifier.matchParentSize())
                                        if (previewBmp != null) {
                                            Image(
                                                bitmap = previewBmp.asImageBitmap(),
                                                contentDescription = template.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize().padding(8.dp)
                                            )
                                        } else {
                                            Text("🎨", fontSize = 32.sp)
                                        }
                                    }

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(template.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(2.dp))
                                        Text("${template.layers.size} Layers", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { previewProjectTarget = template },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("👁️ Preview Project", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            if (stickerFiles.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🖼️ No Saved Stickers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Create and save stickers to view them here.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                stickerFiles.chunked(2).forEach { rowFiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowFiles.forEach { file ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    val bitmap = remember(file.absolutePath) { BitmapCacheManager.loadCachedBitmap(file.absolutePath, 256) }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .background(Color(0xFFF8F8F8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Checkerboard(Modifier.matchParentSize())
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = file.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize().padding(8.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(file.nameWithoutExtension, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { onEditStickerFile(file) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("✏️ Edit in Editor", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (rowFiles.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    previewProjectTarget?.let { template ->
        TemplatePreviewDialog(
            template = template,
            onDismiss = { previewProjectTarget = null },
            onUseTemplate = { selected ->
                previewProjectTarget = null
                onContinueEditTemplate(selected)
            }
        )
    }
}
