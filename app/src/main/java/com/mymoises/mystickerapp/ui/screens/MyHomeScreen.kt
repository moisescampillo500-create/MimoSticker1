package com.mymoises.mystickerapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.BitmapCacheManager
import com.mymoises.mystickerapp.CreatorProfileDialog
import com.mymoises.mystickerapp.TemplateCategories
import com.mymoises.mystickerapp.TemplatePreviewDialog
import com.mymoises.mystickerapp.UserStickerTemplate
import com.mymoises.mystickerapp.UserStickerTemplatesManager
import com.mymoises.mystickerapp.ui.components.Checkerboard

@Composable
fun MyHomeScreen(
    onCreateProject: () -> Unit,
    onSelectTemplate: (UserStickerTemplate) -> Unit,
    onSeeAllTemplates: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var templatesRefresh by remember { mutableIntStateOf(0) }
    var previewTemplate by remember { mutableStateOf<UserStickerTemplate?>(null) }

    val trendingTemplates = remember(selectedCategory, searchQuery, templatesRefresh) {
        UserStickerTemplatesManager.getDiscoveryTemplates(
            context = context,
            section = "Trending",
            categoryFilter = selectedCategory,
            searchQuery = searchQuery
        )
    }

    val popularTemplates = remember(selectedCategory, searchQuery, templatesRefresh) {
        UserStickerTemplatesManager.getDiscoveryTemplates(
            context = context,
            section = "Popular",
            categoryFilter = selectedCategory,
            searchQuery = searchQuery
        )
    }

    val newestTemplates = remember(selectedCategory, searchQuery, templatesRefresh) {
        UserStickerTemplatesManager.getDiscoveryTemplates(
            context = context,
            section = "Newest",
            categoryFilter = selectedCategory,
            searchQuery = searchQuery
        )
    }

    val categories = TemplateCategories.CATEGORIES_LIST

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Sticker Craft", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Design & Create Custom Stickers", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search templates by name, category or creator...") },
            leadingIcon = { Text("🔍", fontSize = 18.sp) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF1744),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCreateProject() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF1744))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("➕ CREATE PROJECT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Start with a blank canvas or import a photo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Text("🎨", fontSize = 28.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("🏷️ Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Browse templates by category", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            TextButton(onClick = onSeeAllTemplates) {
                Text("SEE ALL →", fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF1744),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF5F5F7),
                        labelColor = Color.DarkGray
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (searchQuery.isNotBlank()) {
            val searchResults = trendingTemplates
            Text("🔍 Search Results (${searchResults.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (searchResults.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍 No templates found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("No templates match '$searchQuery'.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { searchQuery = "" }) {
                            Text("Clear Search")
                        }
                    }
                }
            } else {
                TemplateGrid4Columns(
                    templates = searchResults,
                    onTemplateClick = { previewTemplate = it }
                )
            }
        } else {
            val topCreators = remember(templatesRefresh) {
                UserStickerTemplatesManager.getTopCreators(context)
            }
            var selectedCreatorName by remember { mutableStateOf<String?>(null) }

            if (topCreators.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("👥 Featured Creators", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Top sticker artists & community rankings", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(topCreators) { index, creator ->
                            val rankBadge = when (index) {
                                0 -> "🏆 #1"
                                1 -> "🥈 #2"
                                2 -> "🥉 #3"
                                else -> "#${index + 1}"
                            }

                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { selectedCreatorName = creator.name },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(rankBadge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFFFF0F3), CircleShape)
                                            .border(2.dp, Color(0xFFFF1744), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(creator.avatar, fontSize = 22.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        creator.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        creator.handle,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "📥 ${creator.totalUses} uses",
                                        fontSize = 9.sp,
                                        color = Color.DarkGray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                selectedCreatorName?.let { name ->
                    CreatorProfileDialog(
                        creatorName = name,
                        onDismiss = { selectedCreatorName = null },
                        onUseTemplate = { selected ->
                            selectedCreatorName = null
                            onSelectTemplate(selected)
                        }
                    )
                }
            }

            TemplateHomeSection(
                title = "🔥 Trending Templates",
                subtitle = "Highest recent user engagement",
                templates = trendingTemplates,
                onSeeAll = onSeeAllTemplates,
                onTemplateClick = { previewTemplate = it }
            )

            Spacer(Modifier.height(24.dp))

            TemplateHomeSection(
                title = "⭐ Popular Templates",
                subtitle = "Most frequently used community templates",
                templates = popularTemplates,
                onSeeAll = onSeeAllTemplates,
                onTemplateClick = { previewTemplate = it }
            )

            Spacer(Modifier.height(24.dp))

            TemplateHomeSection(
                title = "✨ Newest Templates",
                subtitle = "Recently published community templates",
                templates = newestTemplates,
                onSeeAll = onSeeAllTemplates,
                onTemplateClick = { previewTemplate = it }
            )
        }
    }

    previewTemplate?.let { template ->
        TemplatePreviewDialog(
            template = template,
            onDismiss = { previewTemplate = null },
            onUseTemplate = { selected ->
                previewTemplate = null
                onSelectTemplate(selected)
            }
        )
    }
}

@Composable
fun TemplateHomeSection(
    title: String,
    subtitle: String,
    templates: List<UserStickerTemplate>,
    onSeeAll: () -> Unit,
    onTemplateClick: (UserStickerTemplate) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            TextButton(onClick = onSeeAll) {
                Text("SEE ALL →", fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
            }
        }

        Spacer(Modifier.height(10.dp))

        if (templates.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
            ) {
                Text("No templates in this section.", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            TemplateGrid4Columns(
                templates = templates.take(4),
                onTemplateClick = onTemplateClick
            )
        }
    }
}

@Composable
fun TemplateGrid4Columns(
    templates: List<UserStickerTemplate>,
    onTemplateClick: (UserStickerTemplate) -> Unit
) {
    templates.chunked(4).forEach { rowTemplates ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rowTemplates.forEach { template ->
                key(template.id) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTemplateClick(template) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val previewBitmap = remember(template.previewPath) {
                                BitmapCacheManager.loadCachedBitmap(template.previewPath, 256)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8F8F8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkerboard(Modifier.matchParentSize())
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap.asImageBitmap(),
                                        contentDescription = template.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                } else {
                                    Text("🎨", fontSize = 16.sp)
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                template.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                "by @${template.creatorName}",
                                fontSize = 8.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            repeat(4 - rowTemplates.size) {
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun CreateStickerMenu(
    onCreateProject: () -> Unit,
    onMyProjects: () -> Unit,
    onRemoveBackground: () -> Unit,
    onHeadCutout: () -> Unit = {},
    onEditSticker: () -> Unit,
    onTemplates: () -> Unit,
    onTextSticker: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Create & Edit", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Choose a tool to start creating custom stickers.", color = Color.Gray)
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCreateProject() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF1744))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("🎨", fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("CREATE PROJECT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text("Start new canvas", fontSize = 11.sp, color = Color.White.copy(0.85f))
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onMyProjects() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("📁", fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("MY PROJECTS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text("Saved creations", fontSize = 11.sp, color = Color.White.copy(0.85f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Creation Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        val tools = listOf(
            Triple("✂️ Remove BG", "Cut out subject from photo automatically", onRemoveBackground),
            Triple("👤 Head Cutout", "Isolate head with sticker outline effect", onHeadCutout),
            Triple("🖼️ Edit Photo", "Open photo in full editor with layers", onEditSticker),
            Triple("🎨 Templates", "Ready-made sticker designs", onTemplates),
            Triple("📝 Text Sticker", "Create custom styled text stickers", onTextSticker)
        )

        tools.chunked(2).forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTools.forEach { (title, subtitle, action) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { action() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(subtitle, fontSize = 11.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun CreateOptionCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            }
            Spacer(Modifier.width(12.dp))
            Text(icon, fontSize = 28.sp)
        }
    }
}
