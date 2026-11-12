package com.mymoises.mystickerapp.ui.screens

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mymoises.mystickerapp.BitmapCacheManager
import com.mymoises.mystickerapp.CreatorProfileDialog
import com.mymoises.mystickerapp.CreatorStatsData
import com.mymoises.mystickerapp.UserStickerTemplate
import com.mymoises.mystickerapp.UserStickerTemplatesManager
import com.mymoises.mystickerapp.ui.components.AppButton
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.loadMyStickerFiles
import com.mymoises.mystickerapp.util.saveStickerToDeviceGallery
import com.mymoises.mystickerapp.util.shareStickerFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// COLLECTIONS & TRASH HELPER MODELS & UTILS
data class UserCollectionItem(
    val id: String,
    val name: String,
    val stickerFileNames: List<String>
)

fun loadUserCollections(prefs: SharedPreferences): List<UserCollectionItem> {
    val jsonStr = prefs.getString("user_collections_json", null)
    if (jsonStr.isNullOrBlank()) {
        return emptyList()
    }
    return try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<UserCollectionItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val filesArray = obj.optJSONArray("stickerFileNames")
            val filesList = mutableListOf<String>()
            if (filesArray != null) {
                for (j in 0 until filesArray.length()) {
                    filesList.add(filesArray.getString(j))
                }
            }
            list.add(
                UserCollectionItem(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    stickerFileNames = filesList
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveUserCollections(prefs: SharedPreferences, collections: List<UserCollectionItem>) {
    val array = JSONArray()
    collections.forEach { col ->
        val obj = JSONObject().apply {
            put("id", col.id)
            put("name", col.name)
            val filesArray = JSONArray()
            col.stickerFileNames.forEach { filesArray.put(it) }
            put("stickerFileNames", filesArray)
        }
        array.put(obj)
    }
    prefs.edit().putString("user_collections_json", array.toString()).apply()
}

fun getTrashFiles(context: Context): List<File> {
    val trashDir = File(context.cacheDir, "trash")
    if (!trashDir.exists()) return emptyList()
    return trashDir.listFiles()?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
}

fun moveToTrash(context: Context, prefs: SharedPreferences, file: File): Boolean {
    val trashDir = File(context.cacheDir, "trash").apply { mkdirs() }
    val destFile = File(trashDir, file.name)
    val success = file.exists() && file.renameTo(destFile)
    if (success) {
        val trashList = prefs.getStringSet("trash_stickers_list", emptySet())?.toMutableSet() ?: mutableSetOf()
        trashList.add(destFile.name)
        prefs.edit().putStringSet("trash_stickers_list", trashList).apply()
    }
    return success
}

fun restoreFromTrash(context: Context, prefs: SharedPreferences, file: File): Boolean {
    val myStickersDir = File(context.filesDir, "MyStickers").apply { mkdirs() }
    val destFile = File(myStickersDir, file.name)
    val success = file.exists() && file.renameTo(destFile)
    if (success) {
        val trashList = prefs.getStringSet("trash_stickers_list", emptySet())?.toMutableSet() ?: mutableSetOf()
        trashList.remove(file.name)
        prefs.edit().putStringSet("trash_stickers_list", trashList).apply()
    }
    return success
}

fun deleteFromTrashPermanently(prefs: SharedPreferences, file: File): Boolean {
    val trashList = prefs.getStringSet("trash_stickers_list", emptySet())?.toMutableSet() ?: mutableSetOf()
    trashList.remove(file.name)
    prefs.edit().putStringSet("trash_stickers_list", trashList).apply()
    return file.exists() && file.delete()
}

fun emptyAllTrash(context: Context, prefs: SharedPreferences): Boolean {
    val files = getTrashFiles(context)
    var allDeleted = true
    files.forEach { file ->
        if (!deleteFromTrashPermanently(prefs, file)) allDeleted = false
    }
    prefs.edit().remove("trash_stickers_list").apply()
    return allDeleted
}

fun getFavoriteStickerFiles(context: Context, prefs: SharedPreferences): List<File> {
    val favNames = prefs.getStringSet("favorite_stickers", emptySet()) ?: emptySet()
    val allStickers = loadMyStickerFiles(context)
    return allStickers.filter { favNames.contains(it.name) }
}

fun toggleFavoriteSticker(prefs: SharedPreferences, fileName: String): Boolean {
    val set = prefs.getStringSet("favorite_stickers", emptySet())?.toMutableSet() ?: mutableSetOf()
    val isFav = set.contains(fileName)
    if (isFav) set.remove(fileName) else set.add(fileName)
    prefs.edit().putStringSet("favorite_stickers", set).apply()
    return !isFav
}

@Composable
fun CreatorsScreen(
    context: Context,
    prefs: SharedPreferences,
    onSelectTemplate: (UserStickerTemplate) -> Unit = {},
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Featured") }
    var selectedCreatorName by remember { mutableStateOf<String?>(null) }

    var followedIds by remember {
        mutableStateOf(prefs.getStringSet("followed_creators", emptySet()) ?: emptySet())
    }

    val realCreators = remember {
        UserStickerTemplatesManager.getTopCreators(context)
    }

    val filteredCreators = remember(searchQuery, selectedTab, followedIds, realCreators) {
        val list = when (selectedTab) {
            "Popular" -> realCreators.sortedByDescending { it.totalUses }
            "Following" -> realCreators.filter { followedIds.contains(it.id) }
            else -> realCreators
        }

        if (searchQuery.isNotBlank()) {
            list.filter { creator ->
                creator.name.contains(searchQuery, ignoreCase = true) ||
                creator.handle.contains(searchQuery, ignoreCase = true) ||
                creator.bio.contains(searchQuery, ignoreCase = true)
            }
        } else list
    }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("👥 Sticker Creators", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Discover top creators and their sticker creations.", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search creator or style…") },
            trailingIcon = { Text("🔍") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Featured", "Popular", "Following").forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (filteredCreators.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
            ) {
                Text(
                    "No creators found.",
                    modifier = Modifier.padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            filteredCreators.forEach { creator ->
                val isFollowing = followedIds.contains(creator.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { selectedCreatorName = creator.name },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color(0xFFFFF2F5), CircleShape)
                                    .border(2.dp, Color(0xFFE53935), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(creator.avatar, fontSize = 26.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(creator.name, style = MaterialTheme.typography.titleMedium)
                                    if (creator.verified) {
                                        Spacer(Modifier.width(4.dp))
                                        Text("✔️", fontSize = 12.sp, color = Color(0xFF1685F8))
                                    }
                                }
                                Text("${creator.handle} · 📥 ${creator.totalUses} uses · ❤️ ${creator.totalLikes} likes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Button(
                                onClick = {
                                    val updated = if (isFollowing) followedIds - creator.id else followedIds + creator.id
                                    followedIds = updated
                                    prefs.edit().putStringSet("followed_creators", updated).apply()
                                    Toast.makeText(
                                        context,
                                        if (!isFollowing) "Following ${creator.name}!" else "Unfollowed ${creator.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.DarkGray else Color(0xFFE53935)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(if (isFollowing) "Following ✓" else "＋ Follow", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(creator.bio, style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(8.dp))
                        Text("Published Templates: ${creator.templates.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF1744))
                    }
                }
            }
        }
    }

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

@Composable
fun MyScreen(
    context: Context,
    prefs: SharedPreferences,
    displayName: String,
    email: String,
    onEditAccount: () -> Unit,
    onMyStickers: () -> Unit,
    onFavorites: () -> Unit,
    onMyCreations: () -> Unit,
    onSettings: () -> Unit
) {
    val redPrimary = Color(0xFFE53935)
    val redGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFD32F2F),
            Color(0xFFEF5350)
        )
    )

    var creatorName by remember { mutableStateOf(prefs.getString("creator_name", displayName.ifBlank { "My Creator" }) ?: "My Creator") }
    var creatorBio by remember { mutableStateOf(prefs.getString("creator_bio", "Sticker lover & creator ✨") ?: "Sticker lover & creator ✨") }
    var creatorAvatar by remember { mutableStateOf(prefs.getString("creator_avatar", "🎨") ?: "🎨") }
    var coverUri by remember { mutableStateOf(prefs.getString("creator_cover_uri", null)) }
    var coverPresetIndex by remember { mutableIntStateOf(prefs.getInt("creator_cover_preset", 0)) }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val myStickersFiles = remember(refreshTrigger) { loadMyStickerFiles(context) }
    val myStickersCount = myStickersFiles.size

    val followedIds = remember(refreshTrigger) { prefs.getStringSet("followed_creators", emptySet()) ?: emptySet() }
    val followingCount = followedIds.size
    val followersCount = remember(refreshTrigger) { prefs.getInt("user_followers_count", 0) }

    val favoriteSet = remember(refreshTrigger) { prefs.getStringSet("favorite_stickers", emptySet()) ?: emptySet() }
    val favoriteCount = favoriteSet.size

    val collectionsList = remember(refreshTrigger) { loadUserCollections(prefs) }
    val collectionsCount = collectionsList.size

    val downloadedTemplates = remember(refreshTrigger) { UserStickerTemplatesManager.getCommunityTemplates(context) }
    val downloadsCount = downloadedTemplates.size

    val trashFiles = remember(refreshTrigger) { getTrashFiles(context) }
    val trashCount = trashFiles.size

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangeCoverDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showCollectionsDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }

    val presetBanners = listOf(
        Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFFF7043))),
        Brush.linearGradient(listOf(Color(0xFF8E24AA), Color(0xFFD81B60))),
        Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF00ACC1))),
        Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF1B5E20))),
        Brush.linearGradient(listOf(Color(0xFFFB8C00), Color(0xFFFDD835))),
        Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF455A64)))
    )

    val coverGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coverUri = uri.toString()
            prefs.edit().putString("creator_cover_uri", uri.toString()).apply()
            Toast.makeText(context, "Cover photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // MODERN RED WAVE HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(redGradient),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 My Screen & Profile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // ELEGANT CREATOR PROFILE CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-45).dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, Color(0xFFF0F0F0))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // COVER BANNER PHOTO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(
                            if (coverUri != null) Color.Gray
                            else Color.Transparent
                        )
                ) {
                    if (coverUri != null) {
                        val bitmap = remember(coverUri) {
                            try {
                                context.contentResolver.openInputStream(Uri.parse(coverUri))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Cover Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(presetBanners.getOrElse(coverPresetIndex) { presetBanners[0] }))
                        }
                    } else {
                        Box(Modifier.fillMaxSize().background(presetBanners.getOrElse(coverPresetIndex) { presetBanners[0] }))
                    }

                    // CHANGE COVER OVERLAY BUTTON
                    Surface(
                        onClick = { showChangeCoverDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📷 Change Cover", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // AVATAR OVERLAPPING BANNER
                Box(
                    modifier = Modifier
                        .offset(y = (-38).dp)
                        .size(80.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .border(3.dp, redPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(creatorAvatar, fontSize = 40.sp)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-30).dp)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(creatorName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                    Text("@${creatorName.lowercase().replace(" ", "_")}", fontSize = 12.sp, color = Color.Gray)

                    Spacer(Modifier.height(4.dp))
                    Text(
                        creatorBio,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = redPrimary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("✏️ Edit Profile", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 4-COLUMN FUNCTIONAL STATISTICS ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFF5F6))
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(myStickersCount.toString(), "My Stickers", onClick = onMyStickers)
                        Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFFFFCDD2)))
                        ProfileStatItem(followingCount.toString(), "Following", onClick = { showFollowingDialog = true })
                        Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFFFFCDD2)))
                        ProfileStatItem(followersCount.toString(), "Followers", onClick = { showFollowersDialog = true })
                        Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFFFFCDD2)))
                        ProfileStatItem(favoriteCount.toString(), "Favorites", onClick = { showFavoritesDialog = true })
                    }
                }
            }
        }

        // QUICK-ACCESS SHORTCUTS BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "⚡ Quick Shortcuts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1B4B)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickShortcutChip("🖼️ My Stickers", "$myStickersCount items", redPrimary, onClick = onMyStickers)
                QuickShortcutChip("📋 Templates", "Explore", Color(0xFF1685F8), onClick = onMyCreations)
                QuickShortcutChip("📁 Collections", "$collectionsCount custom", Color(0xFF8E24AA), onClick = { showCollectionsDialog = true })
                QuickShortcutChip("📥 Downloads", "$downloadsCount items", Color(0xFF43A047), onClick = { showDownloadsDialog = true })
                QuickShortcutChip("🗑️ Trash", "$trashCount items", Color(0xFF757575), onClick = { showTrashDialog = true })
            }
        }

        Spacer(Modifier.height(6.dp))

        // MY COLLECTIONS & SHORTCUTS SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "My Collections & Shortcuts",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1B4B)
            )

            CollectionShortcutCard(
                icon = "🖼️",
                title = "My Stickers Gallery",
                subtitle = "$myStickersCount saved stickers in gallery",
                badgeCount = myStickersCount,
                onClick = onMyStickers
            )

            CollectionShortcutCard(
                icon = "❤️",
                title = "Favorite Stickers",
                subtitle = "$favoriteCount stickers marked as favorite",
                badgeCount = favoriteCount,
                onClick = { showFavoritesDialog = true }
            )

            CollectionShortcutCard(
                icon = "📁",
                title = "My Collections",
                subtitle = "Organize stickers into custom folders ($collectionsCount collections)",
                badgeCount = collectionsCount,
                onClick = { showCollectionsDialog = true }
            )

            CollectionShortcutCard(
                icon = "📥",
                title = "Downloaded Creations",
                subtitle = "$downloadsCount downloaded templates & stickers",
                badgeCount = downloadsCount,
                onClick = { showDownloadsDialog = true }
            )

            CollectionShortcutCard(
                icon = "🗑️",
                title = "Trash & Recently Deleted",
                subtitle = "Restore or permanently delete items ($trashCount in trash)",
                badgeCount = trashCount,
                onClick = { showTrashDialog = true }
            )

            CollectionShortcutCard(
                icon = "⚙️",
                title = "App Settings & Preferences",
                subtitle = "Theme, auto save, notification options",
                badgeCount = null,
                onClick = onSettings
            )
        }
    }

    // EDIT PROFILE DIALOG
    if (showEditProfileDialog) {
        var editNameInput by remember { mutableStateOf(creatorName) }
        var editBioInput by remember { mutableStateOf(creatorBio) }
        var selectedAvatarEmoji by remember { mutableStateOf(creatorAvatar) }

        val avatarOptions = listOf("🎨", "⭐", "👑", "🚀", "🔥", "💖", "🌸", "🐶", "🐱", "✨")

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("✏️ Edit Account Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Avatar Icon:", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        avatarOptions.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedAvatarEmoji == emoji) Color(0xFFFFF2F5) else Color(0xFFF5F5F5))
                                    .border(if (selectedAvatarEmoji == emoji) 2.dp else 0.dp, redPrimary, CircleShape)
                                    .clickable { selectedAvatarEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Creator / Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editBioInput,
                        onValueChange = { editBioInput = it },
                        label = { Text("Creator Bio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = editNameInput.ifBlank { "My Creator" }
                        val newBio = editBioInput.ifBlank { "Sticker lover & creator ✨" }
                        creatorName = newName
                        creatorBio = newBio
                        creatorAvatar = selectedAvatarEmoji
                        prefs.edit()
                            .putString("creator_name", newName)
                            .putString("creator_bio", newBio)
                            .putString("creator_avatar", selectedAvatarEmoji)
                            .putString("display_name", newName)
                            .apply()
                        refreshTrigger++
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = redPrimary)
                ) { Text("Save Profile") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // CHANGE COVER DIALOG
    if (showChangeCoverDialog) {
        AlertDialog(
            onDismissRequest = { showChangeCoverDialog = false },
            title = { Text("📷 Change Cover Banner") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pick a Preset Banner Style:", fontSize = 12.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetBanners.forEachIndexed { idx, brush ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(brush)
                                    .border(if (coverPresetIndex == idx && coverUri == null) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                                    .clickable {
                                        coverPresetIndex = idx
                                        coverUri = null
                                        prefs.edit().putInt("creator_cover_preset", idx).remove("creator_cover_uri").apply()
                                    }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            coverGalleryLauncher.launch("image/*")
                            showChangeCoverDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖼️ Pick Image from Gallery")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showChangeCoverDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = redPrimary)
                ) { Text("Done") }
            }
        )
    }

    // FOLLOWING DIALOG
    if (showFollowingDialog) {
        var followedSet by remember { mutableStateOf(prefs.getStringSet("followed_creators", emptySet()) ?: emptySet()) }
        val topCreators = remember { UserStickerTemplatesManager.getTopCreators(context) }
        val followedCreators = remember(followedSet, topCreators) {
            topCreators.filter { followedSet.contains(it.id) }
        }

        AlertDialog(
            onDismissRequest = { showFollowingDialog = false },
            title = { Text("👥 Following Creators (${followedCreators.size})") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    if (followedCreators.isEmpty()) {
                        Text("You are not following any creators yet.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        followedCreators.forEach { creator ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(creator.avatar, fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(creator.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(creator.handle, fontSize = 11.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        val newSet = followedSet - creator.id
                                        followedSet = newSet
                                        prefs.edit().putStringSet("followed_creators", newSet).apply()
                                        refreshTrigger++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Unfollow", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFollowingDialog = false }) { Text("Close") } }
        )
    }

    // FOLLOWERS DIALOG
    if (showFollowersDialog) {
        val followersList = remember {
            listOf(
                Pair("Alex_Stickers", "🌟 Amazing sticker edits!"),
                Pair("MemeCraft_PH", "🔥 Love your templates"),
                Pair("Kawaii_Stickers", "💖 Super cute creations"),
                Pair("VibeArt", "🎨 Keep making awesome packs!")
            )
        }
        AlertDialog(
            onDismissRequest = { showFollowersDialog = false },
            title = { Text("👥 My Followers ($followersCount)") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    followersList.forEach { (name, note) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(Color(0xFFFFF2F5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("👤", fontSize = 18.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(note, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFollowersDialog = false }) { Text("Close") } }
        )
    }

    // COLLECTIONS MANAGER DIALOG
    if (showCollectionsDialog) {
        var collectionsState by remember { mutableStateOf(loadUserCollections(prefs)) }
        var newCollectionName by remember { mutableStateOf("") }
        var editingCollectionId by remember { mutableStateOf<String?>(null) }
        var editNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCollectionsDialog = false },
            title = { Text("📁 My Collections (${collectionsState.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            label = { Text("New Collection Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newCollectionName.isNotBlank()) {
                                    val newCol = UserCollectionItem("col_${System.currentTimeMillis()}", newCollectionName.trim(), emptyList())
                                    val updated = collectionsState + newCol
                                    collectionsState = updated
                                    saveUserCollections(prefs, updated)
                                    newCollectionName = ""
                                    refreshTrigger++
                                    Toast.makeText(context, "Collection created!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = redPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("＋ Create", fontSize = 11.sp) }
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                        if (collectionsState.isEmpty()) {
                            Text("No collections created yet.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            collectionsState.forEach { collection ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📁", fontSize = 20.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(collection.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${collection.stickerFileNames.size} stickers", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        IconButton(onClick = {
                                            editingCollectionId = collection.id
                                            editNameInput = collection.name
                                        }) { Text("✏️", fontSize = 14.sp) }

                                        IconButton(onClick = {
                                            val updated = collectionsState.filter { it.id != collection.id }
                                            collectionsState = updated
                                            saveUserCollections(prefs, updated)
                                            refreshTrigger++
                                            Toast.makeText(context, "Collection deleted.", Toast.LENGTH_SHORT).show()
                                        }) { Text("🗑️", fontSize = 14.sp) }
                                    }
                                }
                            }
                        }
                    }

                    if (editingCollectionId != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editNameInput,
                                onValueChange = { editNameInput = it },
                                label = { Text("Rename Collection") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (editNameInput.isNotBlank()) {
                                        val updated = collectionsState.map { col ->
                                            if (col.id == editingCollectionId) col.copy(name = editNameInput.trim()) else col
                                        }
                                        collectionsState = updated
                                        saveUserCollections(prefs, updated)
                                        editingCollectionId = null
                                        refreshTrigger++
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = redPrimary)
                            ) { Text("Save", fontSize = 11.sp) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCollectionsDialog = false }) { Text("Done") } }
        )
    }

    // DOWNLOADS DIALOG
    if (showDownloadsDialog) {
        val downloadedList = remember { UserStickerTemplatesManager.getCommunityTemplates(context) }
        AlertDialog(
            onDismissRequest = { showDownloadsDialog = false },
            title = { Text("📥 Downloaded Creations (${downloadedList.size})") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    if (downloadedList.isEmpty()) {
                        Text("No downloaded creations or templates yet.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        downloadedList.forEach { template ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val bitmap = remember(template.previewPath) { BitmapCacheManager.loadCachedBitmap(template.previewPath, 128) }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = template.name,
                                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Box(Modifier.size(42.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(template.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("by @${template.creatorName}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDownloadsDialog = false }) { Text("Close") } }
        )
    }

    // TRASH MANAGER DIALOG
    if (showTrashDialog) {
        var trashList by remember { mutableStateOf(getTrashFiles(context)) }

        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("🗑️ Trash & Recently Deleted (${trashList.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (trashList.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                emptyAllTrash(context, prefs)
                                trashList = emptyList()
                                refreshTrigger++
                                Toast.makeText(context, "Trash emptied!", Toast.LENGTH_SHORT).show()
                            }) { Text("Empty Trash", color = redPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        if (trashList.isEmpty()) {
                            Text("Trash is empty.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                        } else {
                            trashList.forEach { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val bitmap = remember(file.absolutePath) { BitmapCacheManager.loadCachedBitmap(file.absolutePath, 128) }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = file.name,
                                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(file.nameWithoutExtension, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)

                                        Button(
                                            onClick = {
                                                if (restoreFromTrash(context, prefs, file)) {
                                                    trashList = getTrashFiles(context)
                                                    refreshTrigger++
                                                    Toast.makeText(context, "Restored sticker!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) { Text("Restore", fontSize = 10.sp) }

                                        Spacer(Modifier.width(4.dp))

                                        IconButton(onClick = {
                                            deleteFromTrashPermanently(prefs, file)
                                            trashList = getTrashFiles(context)
                                            refreshTrigger++
                                            Toast.makeText(context, "Permanently deleted.", Toast.LENGTH_SHORT).show()
                                        }) { Text("❌", fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTrashDialog = false }) { Text("Close") } }
        )
    }

    // FAVORITES DIALOG
    if (showFavoritesDialog) {
        val favFiles = remember(refreshTrigger) { getFavoriteStickerFiles(context, prefs) }

        AlertDialog(
            onDismissRequest = { showFavoritesDialog = false },
            title = { Text("❤️ Favorite Stickers (${favFiles.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    if (favFiles.isEmpty()) {
                        Text("No favorite stickers added yet.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            favFiles.chunked(3).forEach { rowFiles ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowFiles.forEach { file ->
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clickable {
                                                    shareStickerFile(context, file)
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Checkerboard(Modifier.matchParentSize())
                                                val bitmap = remember(file.absolutePath) { BitmapCacheManager.loadCachedBitmap(file.absolutePath, 256) }
                                                if (bitmap != null) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = file.name,
                                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - rowFiles.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFavoritesDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun ProfileStatItem(
    count: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Text(count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun QuickShortcutChip(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun CollectionShortcutCard(
    icon: String,
    title: String,
    subtitle: String,
    badgeCount: Int?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFFF2F5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (badgeCount != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF0F2),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Text(
                        badgeCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            Text("›", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccountScreen(
    displayName: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var bioInput by remember { mutableStateOf("Sticker lover & creator ✨") }
    val redPrimary = Color(0xFFE53935)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✏️ Edit Account Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text("Display Name / Creator Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = bioInput,
            onValueChange = { bioInput = it },
            label = { Text("Creator Bio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        AppButton("← Back to Profile") {
            onBack()
        }
    }
}

@Composable
fun SettingsScreen(
    onAccountClick: () -> Unit,
    autoSave: Boolean,
    onAutoSaveChange: (Boolean) -> Unit,
    onInviteClick: () -> Unit,
    onConnectClick: () -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    notifications: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit = {},
    onLogoutClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("⚙️ Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
        Spacer(Modifier.height(20.dp))

        SettingRow("Account", onClick = onAccountClick)
        SettingSwitchRow("Auto Save Stickers", checked = autoSave, onCheckedChange = onAutoSaveChange)
        SettingRow("Invite Friends", onClick = onInviteClick)
        SettingRow("Connect", onClick = onConnectClick)
        SettingSwitchRow("Dark Mode", checked = darkMode, onCheckedChange = onDarkModeChange)
        SettingSwitchRow("Push Notifications", checked = notifications, onCheckedChange = onNotificationsChange)
        SettingRow("🔒 Privacy Policy", onClick = onPrivacyPolicyClick)
        SettingRow("Logout", onClick = onLogoutClick)
        SettingRow("About MyStickerApp", onClick = onAboutClick)
    }
}

@Composable
fun SettingRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp)
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
