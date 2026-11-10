package com.mymoises.mystickerapp

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mymoises.mystickerapp.ui.components.AppButton
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.ui.dialogs.LargeStickerPreviewDialog
import com.mymoises.mystickerapp.ui.screens.AccountScreen
import com.mymoises.mystickerapp.ui.screens.CreateStickerMenu
import com.mymoises.mystickerapp.ui.screens.CreateStickerScreen
import com.mymoises.mystickerapp.ui.screens.CreatorsScreen
import com.mymoises.mystickerapp.ui.screens.HeadCutoutScreen
import com.mymoises.mystickerapp.ui.screens.MyHomeScreen
import com.mymoises.mystickerapp.ui.screens.MyProjectsScreen
import com.mymoises.mystickerapp.ui.screens.MyScreen
import com.mymoises.mystickerapp.ui.screens.SettingsScreen
import com.mymoises.mystickerapp.ui.screens.StickerEditorScreen
import com.mymoises.mystickerapp.ui.screens.TextStickerScreen
import com.mymoises.mystickerapp.ui.screens.getFavoriteStickerFiles
import com.mymoises.mystickerapp.util.deleteAllStickerFiles
import com.mymoises.mystickerapp.util.deleteStickerFile
import com.mymoises.mystickerapp.util.loadMyStickerFiles
import com.mymoises.mystickerapp.util.renameStickerFile
import com.mymoises.mystickerapp.util.saveStickerToDeviceGallery
import com.mymoises.mystickerapp.util.saveTempCutoutSticker
import com.mymoises.mystickerapp.util.shareStickerFile
import com.mymoises.mystickerapp.util.shareStickerToWhatsApp
import java.io.File

private const val PREFS = "mysticker_settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyStickerApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStickerApp() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            AdMobRewardedAdManager.initialize(context)
        } catch (_: Exception) {}
    }

    var rewardedAdOfferSpec by remember { mutableStateOf<RewardedAdOfferSpec?>(null) }

    val prefs = remember {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var selectedScreen by remember { mutableStateOf("My Home") }
    var selectedTemplate by remember { mutableStateOf<StickerTemplate?>(null) }
    var selectedUserTemplateToEdit by remember { mutableStateOf<UserStickerTemplate?>(null) }
    val isStickerEditorScreen = selectedScreen == "My Sticker" || selectedScreen == "EditSavedSticker" || selectedScreen == "TemplateEditor" || selectedScreen == "EditUserTemplate"
    var menuExpanded by remember { mutableStateOf(false) }

    var displayName by remember {
        mutableStateOf(prefs.getString("display_name", "My Creator") ?: "My Creator")
    }

    var email by remember {
        mutableStateOf(prefs.getString("email", "") ?: "")
    }

    var autoSave by remember {
        mutableStateOf(prefs.getBoolean("auto_save", true))
    }

    var darkMode by remember {
        mutableStateOf(prefs.getBoolean("dark_mode", false))
    }

    var notifications by remember {
        mutableStateOf(prefs.getBoolean("notifications", true))
    }

    var stickerRefresh by remember { mutableIntStateOf(0) }
    var selectedStickerToEdit by remember { mutableStateOf<File?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = if (darkMode) {
            darkColorScheme(primary = Color.Red)
        } else {
            lightColorScheme(primary = Color.Red)
        }
    ) {
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Delete All Stickers?") },
                text = { Text("Are you sure you want to delete ALL saved stickers? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllDialog = false
                            if (deleteAllStickerFiles(context)) {
                                stickerRefresh++
                                Toast.makeText(context, "All stickers deleted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No stickers to delete.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("Delete All") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
                }
            )
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

        Scaffold(
            topBar = {
                if (!isStickerEditorScreen) {
                    TopAppBar(
                        title = { Text("My Sticker App") },
                        actions = {
                            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                                Text("⋮", fontSize = 22.sp)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Home") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "My Home"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("My Stickers") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "MyStickers"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Creators") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "Creators"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Favorites") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "Favorites"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("My Creations") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "My Creations"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "Settings"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete All Stickers") },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteAllDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("About") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedScreen = "About"
                                    }
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isStickerEditorScreen) {
                    Column {
                        AdBannerView()
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedScreen == "My Home",
                                onClick = { selectedScreen = "My Home" },
                                icon = { Text("🏠") },
                                label = { Text("Home") }
                            )

                            NavigationBarItem(
                                selected = selectedScreen == "Search",
                                onClick = { selectedScreen = "Search" },
                                icon = { Text("🔍") },
                                label = { Text("Search") }
                            )

                            NavigationBarItem(
                                selected = selectedScreen == "Create" || selectedScreen == "Templates" || selectedScreen == "My Projects",
                                onClick = { selectedScreen = "Create" },
                                icon = { Text("➕") },
                                label = { Text("Create") }
                            )

                            NavigationBarItem(
                                selected = selectedScreen == "Creators",
                                onClick = { selectedScreen = "Creators" },
                                icon = { Text("👥") },
                                label = { Text("Creators") }
                            )

                            NavigationBarItem(
                                selected = selectedScreen == "My Screen",
                                onClick = { selectedScreen = "My Screen" },
                                icon = { Text("👤") },
                                label = { Text("My Screen") }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isStickerEditorScreen) {
                            Modifier.background(Color(0xFF111113))
                        } else if (selectedScreen == "Templates" || selectedScreen == "My Screen" || selectedScreen == "Creators") {
                            Modifier
                                .padding(paddingValues)
                                .padding(16.dp)
                        } else {
                            Modifier
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedScreen) {
                    "My Home" -> {
                        MyHomeScreen(
                            onCreateProject = { selectedScreen = "My Sticker" },
                            onSelectTemplate = { userTemplate ->
                                selectedUserTemplateToEdit = userTemplate
                                selectedScreen = "EditUserTemplate"
                            },
                            onSeeAllTemplates = { selectedScreen = "Templates" }
                        )
                    }

                    "My Screen" -> {
                        MyScreen(
                            context = context,
                            prefs = prefs,
                            displayName = displayName,
                            email = email,
                            onEditAccount = {
                                selectedScreen = "Account"
                            },
                            onMyStickers = {
                                selectedScreen = "MyStickers"
                            },
                            onFavorites = {
                                selectedScreen = "Favorites"
                            },
                            onMyCreations = {
                                selectedScreen = "My Creations"
                            },
                            onSettings = {
                                selectedScreen = "Settings"
                            }
                        )
                    }

                    "Account" -> {
                        AccountScreen(
                            displayName = displayName,
                            onNameChange = {
                                displayName = it
                                prefs.edit()
                                    .putString("display_name", it)
                                    .apply()
                            },
                            email = email,
                            onEmailChange = {
                                email = it
                                prefs.edit()
                                    .putString("email", it)
                                    .apply()
                            },
                            onBack = {
                                selectedScreen = "My Screen"
                            }
                        )
                    }

                    "Search" -> {
                        var searchText by remember { mutableStateOf("") }
                        var stickerMenuTarget by remember { mutableStateOf<File?>(null) }
                        var previewTarget by remember { mutableStateOf<File?>(null) }
                        var deleteTarget by remember { mutableStateOf<File?>(null) }
                        var renameTarget by remember { mutableStateOf<File?>(null) }
                        var renameText by remember { mutableStateOf("") }

                        val allStickers = remember(stickerRefresh) { loadMyStickerFiles(context) }
                        val filteredStickers = remember(searchText, allStickers) {
                            if (searchText.isBlank()) allStickers
                            else allStickers.filter { it.nameWithoutExtension.contains(searchText, ignoreCase = true) }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("🔍 Search Stickers", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                label = { Text("Search by sticker name…") },
                                trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                        IconButton(onClick = { searchText = "" }) {
                                            Text("✕", fontSize = 16.sp, color = Color.Gray)
                                        }
                                    } else {
                                        Text("🔍", fontSize = 18.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            if (filteredStickers.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🔍 No stickers found", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            if (searchText.isBlank()) "Create stickers first!"
                                            else "Try searching for another keyword.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Text("🖼️ Saved Stickers (${filteredStickers.size})", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))

                                filteredStickers.chunked(4).forEach { rowFiles ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowFiles.forEach { file ->
                                            key(file.absolutePath) {
                                                Card(
                                                    modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        val bitmap = remember(file.absolutePath, stickerRefresh) {
                                                            BitmapFactory.decodeFile(file.absolutePath)
                                                        }
                                                        if (bitmap != null) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(84.dp)
                                                                    .padding(4.dp)
                                                                    .clickable { previewTarget = file }
                                                            ) {
                                                                Checkerboard(Modifier.matchParentSize())
                                                                Image(
                                                                    bitmap = bitmap.asImageBitmap(),
                                                                    contentDescription = file.name,
                                                                    contentScale = ContentScale.Fit,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                            }
                                                        } else {
                                                            Box(
                                                                modifier = Modifier.fillMaxWidth().height(84.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) { Text("🖼️") }
                                                        }
                                                        Text(
                                                            file.nameWithoutExtension,
                                                            maxLines = 1,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                                            contentAlignment = Alignment.CenterEnd
                                                        ) {
                                                            TextButton(
                                                                onClick = { stickerMenuTarget = file },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                            ) { Text("⋮", fontSize = 18.sp) }
                                                            DropdownMenu(
                                                                expanded = stickerMenuTarget == file,
                                                                onDismissRequest = { stickerMenuTarget = null }
                                                            ) {
                                                                DropdownMenuItem(
                                                                    text = { Text("👁️ Preview") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        previewTarget = file
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("💾 Add to Device") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                                                                            val success = saveStickerToDeviceGallery(context, file, null)
                                                                            Toast.makeText(
                                                                                context,
                                                                                if (success) "Saved to Device Gallery!" else "Could not save.",
                                                                                Toast.LENGTH_SHORT
                                                                            ).show()
                                                                        }
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("💬 Add to WhatsApp") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        rewardedAdOfferSpec = RewardedAdOfferSpec("Send Image to WhatsApp") {
                                                                            shareStickerToWhatsApp(context, file, null)
                                                                        }
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("📤 Share") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        rewardedAdOfferSpec = RewardedAdOfferSpec("Share Image") {
                                                                            shareStickerFile(context, file)
                                                                        }
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("✏️ Edit") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        selectedStickerToEdit = file
                                                                        selectedScreen = "EditSavedSticker"
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("📝 Rename") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        renameTarget = file
                                                                        renameText = file.nameWithoutExtension
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("🗑️ Delete") },
                                                                    onClick = {
                                                                        stickerMenuTarget = null
                                                                        deleteTarget = file
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        repeat(4 - rowFiles.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }

                            previewTarget?.let { file ->
                                LargeStickerPreviewDialog(
                                    file = file,
                                    onDismiss = { previewTarget = null },
                                    onSave = { targetFile ->
                                        previewTarget = null
                                        rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                                            val success = saveStickerToDeviceGallery(context, targetFile, null)
                                            Toast.makeText(
                                                context,
                                                if (success) "Saved to Device Gallery!" else "Could not save.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onWhatsApp = { targetFile ->
                                        previewTarget = null
                                        rewardedAdOfferSpec = RewardedAdOfferSpec("Send Image to WhatsApp") {
                                            shareStickerToWhatsApp(context, targetFile, null)
                                        }
                                    },
                                    onShare = { targetFile ->
                                        previewTarget = null
                                        rewardedAdOfferSpec = RewardedAdOfferSpec("Share Image") {
                                            shareStickerFile(context, targetFile)
                                        }
                                    }
                                )
                            }

                            renameTarget?.let { file ->
                                AlertDialog(
                                    onDismissRequest = { renameTarget = null },
                                    title = { Text("Rename sticker") },
                                    text = {
                                        OutlinedTextField(
                                            value = renameText,
                                            onValueChange = { renameText = it },
                                            label = { Text("Sticker name") },
                                            singleLine = true
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val result = renameStickerFile(file, renameText)
                                            if (result) {
                                                stickerRefresh++
                                                Toast.makeText(context, "Sticker renamed.", Toast.LENGTH_SHORT).show()
                                                renameTarget = null
                                            } else {
                                                Toast.makeText(context, "Name is invalid or already in use.", Toast.LENGTH_SHORT).show()
                                            }
                                        }) { Text("Save") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
                                    }
                                )
                            }

                            deleteTarget?.let { file ->
                                AlertDialog(
                                    onDismissRequest = { deleteTarget = null },
                                    title = { Text("Delete sticker?") },
                                    text = { Text("\"${file.name}\" will be permanently deleted.") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                if (deleteStickerFile(file)) {
                                                    stickerRefresh++
                                                    Toast.makeText(context, "Sticker deleted.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Could not delete sticker.", Toast.LENGTH_SHORT).show()
                                                }
                                                deleteTarget = null
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) { Text("Delete") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                    }

                    "Create" -> {
                        CreateStickerMenu(
                            onCreateProject = { selectedScreen = "My Sticker" },
                            onMyProjects = { selectedScreen = "My Projects" },
                            onRemoveBackground = { selectedScreen = "Remove Background" },
                            onHeadCutout = { selectedScreen = "Head Cutout" },
                            onEditSticker = { selectedScreen = "My Sticker" },
                            onTemplates = { selectedScreen = "Templates" },
                            onTextSticker = { selectedScreen = "AI Text-to-Sticker" }
                        )
                    }

                    "Templates" -> {
                        TemplatesScreen(
                            onCreateNewTemplate = {
                                selectedScreen = "My Sticker"
                            },
                            onUseUserTemplate = { userTemplate ->
                                selectedUserTemplateToEdit = userTemplate
                                selectedScreen = "EditUserTemplate"
                            },
                            onBack = {
                                selectedScreen = "Create"
                            }
                        )
                    }

                    "EditUserTemplate" -> {
                        StickerEditorScreen(
                            initialUserTemplate = selectedUserTemplateToEdit,
                            creatorName = displayName,
                            onStickerSaved = { stickerRefresh++ },
                            onBack = {
                                selectedUserTemplateToEdit = null
                                selectedScreen = "Templates"
                            }
                        )
                    }

                    "TemplateEditor" -> {
                        val currentTemplate = selectedTemplate
                        if (currentTemplate != null) {
                            TemplateEditorScreen(
                                template = currentTemplate,
                                onStickerSaved = {
                                    stickerRefresh++
                                },
                                onOpenInStickerEditor = { bitmap ->
                                    val savedFile = saveTempCutoutSticker(context, bitmap)
                                    if (savedFile != null) {
                                        selectedStickerToEdit = savedFile
                                        selectedScreen = "EditSavedSticker"
                                    } else {
                                        selectedScreen = "My Sticker"
                                    }
                                },
                                onBack = {
                                    selectedScreen = "Templates"
                                }
                            )
                        } else {
                            selectedScreen = "Templates"
                        }
                    }

                    "My Sticker", "AI Picture-to-Sticker", "EditSavedSticker" -> {
                        if (selectedScreen == "My Sticker" || selectedScreen == "EditSavedSticker") {
                            StickerEditorScreen(
                                initialStickerFile = if (selectedScreen == "EditSavedSticker") selectedStickerToEdit else null,
                                creatorName = displayName,
                                onStickerSaved = { stickerRefresh++ },
                                onBack = {
                                    selectedStickerToEdit = null
                                    selectedScreen = if (selectedScreen == "EditSavedSticker") "MyStickers" else "Create"
                                }
                            )
                        } else {
                            CreateStickerScreen(
                                autoSave = autoSave,
                                screenTitle = selectedScreen,
                                onStickerSaved = { stickerRefresh++ },
                                onBack = { selectedScreen = "Create" }
                            )
                        }
                    }

                    "AI Text-to-Sticker" -> {
                        TextStickerScreen(
                            autoSave = autoSave,
                            onStickerSaved = {
                                stickerRefresh++
                            }
                        )
                    }

                    "Remove Background" -> {
                        BackgroundRemovalScreen(
                            autoSave = autoSave,
                            onStickerSaved = { stickerRefresh++ },
                            onOpenInEditor = { cutoutBitmap ->
                                val savedFile = saveTempCutoutSticker(context, cutoutBitmap)
                                if (savedFile != null) {
                                    selectedStickerToEdit = savedFile
                                    selectedScreen = "EditSavedSticker"
                                } else {
                                    selectedScreen = "My Sticker"
                                }
                            },
                            onBack = { selectedScreen = "Create" }
                        )
                    }

                    "Head Cutout" -> {
                        HeadCutoutScreen(
                            autoSave = autoSave,
                            onStickerSaved = { stickerRefresh++ },
                            onOpenInEditor = { headStickerBitmap ->
                                val savedFile = saveTempCutoutSticker(context, headStickerBitmap)
                                if (savedFile != null) {
                                    selectedStickerToEdit = savedFile
                                    selectedScreen = "EditSavedSticker"
                                } else {
                                    selectedScreen = "My Sticker"
                                }
                            },
                            onBack = { selectedScreen = "Create" }
                        )
                    }

                    "Pixel Art" -> {
                        Text("🟪 Pixel Art", fontSize = 26.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Pixel Art conversion is not implemented yet.")

                        AppButton("← Back to Create") {
                            selectedScreen = "Create"
                        }
                    }

                    "Creators" -> {
                        CreatorsScreen(
                            context = context,
                            prefs = prefs,
                            onSelectTemplate = { userTemplate ->
                                selectedUserTemplateToEdit = userTemplate
                                selectedScreen = "EditUserTemplate"
                            },
                            onBack = { selectedScreen = "My Home" }
                        )
                    }

                    "MyStickers" -> {
                        val stickerFiles = remember<List<File>>(stickerRefresh) {
                            loadMyStickerFiles(context)
                        }
                        var renameTarget by remember { mutableStateOf<File?>(null) }
                        var stickerMenuTarget by remember { mutableStateOf<File?>(null) }
                        var deleteTarget by remember { mutableStateOf<File?>(null) }
                        var previewTarget by remember { mutableStateOf<File?>(null) }
                        var renameText by remember { mutableStateOf("") }

                        Text("🖼️ My Stickers", fontSize = 28.sp)
                        Spacer(Modifier.height(16.dp))

                        if (stickerFiles.isEmpty()) {
                            Text("Your saved stickers will appear here.")
                        } else {
                            stickerFiles.chunked(4).forEach { rowFiles ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowFiles.forEach { file ->
                                        key(file.absolutePath) {
                                            Card(
                                                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    val bitmap = remember(file.absolutePath, stickerRefresh) {
                                                        BitmapFactory.decodeFile(file.absolutePath)
                                                    }
                                                    if (bitmap != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(84.dp)
                                                                .padding(4.dp)
                                                                .clickable { previewTarget = file }
                                                        ) {
                                                            Checkerboard(Modifier.matchParentSize())
                                                            Image(
                                                                bitmap = bitmap.asImageBitmap(),
                                                                contentDescription = file.name,
                                                                contentScale = ContentScale.Fit,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().height(84.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) { Text("🖼️") }
                                                    }
                                                    Text(
                                                        file.nameWithoutExtension,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        TextButton(
                                                            onClick = { stickerMenuTarget = file },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                        ) { Text("⋮", fontSize = 18.sp) }
                                                        DropdownMenu(
                                                            expanded = stickerMenuTarget == file,
                                                            onDismissRequest = { stickerMenuTarget = null }
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text("👁️ Preview") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    previewTarget = file
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("💾 Add to Device") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                                                                        val success = saveStickerToDeviceGallery(context, file, null)
                                                                        Toast.makeText(
                                                                            context,
                                                                            if (success) "Saved to Device Gallery!" else "Could not save to device.",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("💬 Add to WhatsApp") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    rewardedAdOfferSpec = RewardedAdOfferSpec("Send Image to WhatsApp") {
                                                                        shareStickerToWhatsApp(context, file, null)
                                                                    }
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("📤 Share") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    rewardedAdOfferSpec = RewardedAdOfferSpec("Share Image") {
                                                                        shareStickerFile(context, file)
                                                                    }
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("✏️ Edit") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    selectedStickerToEdit = file
                                                                    selectedScreen = "EditSavedSticker"
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("📝 Rename") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    renameTarget = file
                                                                    renameText = file.nameWithoutExtension
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("🗑️ Delete") },
                                                                onClick = {
                                                                    stickerMenuTarget = null
                                                                    deleteTarget = file
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    repeat(4 - rowFiles.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }

                        previewTarget?.let { file ->
                            LargeStickerPreviewDialog(
                                file = file,
                                onDismiss = { previewTarget = null },
                                onSave = { targetFile ->
                                    previewTarget = null
                                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image") {
                                        val success = saveStickerToDeviceGallery(context, targetFile, null)
                                        Toast.makeText(
                                            context,
                                            if (success) "Saved to Device Gallery!" else "Could not save to device.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onWhatsApp = { targetFile ->
                                    previewTarget = null
                                    rewardedAdOfferSpec = RewardedAdOfferSpec("Send Image to WhatsApp") {
                                        shareStickerToWhatsApp(context, targetFile, null)
                                    }
                                },
                                onShare = { targetFile ->
                                    previewTarget = null
                                    rewardedAdOfferSpec = RewardedAdOfferSpec("Share Image") {
                                        shareStickerFile(context, targetFile)
                                    }
                                }
                            )
                        }

                        renameTarget?.let { file ->
                            AlertDialog(
                                onDismissRequest = { renameTarget = null },
                                title = { Text("Rename sticker") },
                                text = {
                                    OutlinedTextField(
                                        value = renameText,
                                        onValueChange = { renameText = it },
                                        label = { Text("Sticker name") },
                                        singleLine = true
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val result = renameStickerFile(file, renameText)
                                        if (result) {
                                            stickerRefresh++
                                            Toast.makeText(context, "Sticker renamed.", Toast.LENGTH_SHORT).show()
                                            renameTarget = null
                                        } else {
                                            Toast.makeText(context, "Name is invalid or already in use.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) { Text("Save") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
                                }
                            )
                        }

                        deleteTarget?.let { file ->
                            AlertDialog(
                                onDismissRequest = { deleteTarget = null },
                                title = { Text("Delete sticker?") },
                                text = { Text("\"${file.name}\" will be permanently deleted.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            if (deleteStickerFile(file)) {
                                                stickerRefresh++
                                                Toast.makeText(context, "Sticker deleted.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not delete sticker.", Toast.LENGTH_SHORT).show()
                                            }
                                            deleteTarget = null
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) { Text("Delete") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                                }
                            )
                        }

                        AppButton("🔄 Refresh Stickers") {
                            stickerRefresh++
                        }

                        AppButton("➕ Create Another Sticker") {
                            selectedScreen = "Create"
                        }

                        AppButton("← Back to My Screen") {
                            selectedScreen = "My Screen"
                        }
                    }

                    "Favorites" -> {
                        val favFiles = remember(stickerRefresh) { getFavoriteStickerFiles(context, prefs) }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("❤️ Favorite Stickers (${favFiles.size})", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
                            Spacer(Modifier.height(14.dp))

                            if (favFiles.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))
                                ) {
                                    Text(
                                        "No favorite stickers added yet. Tap the heart icon or mark stickers in My Stickers gallery as favorite!",
                                        modifier = Modifier.padding(20.dp),
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(favFiles) { file ->
                                        Card(
                                            modifier = Modifier.aspectRatio(1f).clickable {
                                                rewardedAdOfferSpec = RewardedAdOfferSpec("Share Image") {
                                                    shareStickerFile(context, file)
                                                }
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
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            AppButton("← Back to My Screen") {
                                selectedScreen = "My Screen"
                            }
                        }
                    }

                    "My Projects", "My Creations" -> {
                        MyProjectsScreen(
                            onContinueEditTemplate = { userTemplate ->
                                selectedUserTemplateToEdit = userTemplate
                                selectedScreen = "EditUserTemplate"
                            },
                            onEditStickerFile = { file ->
                                selectedStickerToEdit = file
                                selectedScreen = "EditSavedSticker"
                            },
                            onBack = { selectedScreen = "Create" }
                        )
                    }

                    "Settings" -> {
                        SettingsScreen(
                            onAccountClick = {
                                selectedScreen = "Account"
                            },
                            autoSave = autoSave,
                            onAutoSaveChange = {
                                autoSave = it
                                prefs.edit()
                                    .putBoolean("auto_save", it)
                                    .apply()
                            },
                            onInviteClick = {
                                selectedScreen = "Invite Friends"
                            },
                            onConnectClick = {
                                selectedScreen = "Connect"
                            },
                            darkMode = darkMode,
                            onDarkModeChange = {
                                darkMode = it
                                prefs.edit()
                                    .putBoolean("dark_mode", it)
                                    .apply()
                            },
                            notifications = notifications,
                            onNotificationsChange = {
                                notifications = it
                                prefs.edit()
                                    .putBoolean("notifications", it)
                                    .apply()
                            },
                            onLogoutClick = {
                                selectedScreen = "Logged Out"
                            },
                            onAboutClick = {
                                selectedScreen = "About"
                            }
                        )
                    }

                    "Invite Friends" -> {
                        Text("👥 Invite Friends", fontSize = 28.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("Invite your friends to use MyStickerApp.")

                        AppButton("Invite Friends") {
                            Toast.makeText(
                                context,
                                "Invite feature is not connected yet.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        AppButton("← Back to Settings") {
                            selectedScreen = "Settings"
                        }
                    }

                    "Connect" -> {
                        Text("Connect", fontSize = 28.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("Connect with other MyStickerApp users.")

                        AppButton("← Back to Settings") {
                            selectedScreen = "Settings"
                        }
                    }

                    "Logged Out" -> {
                        Text("🔐 Logged Out", fontSize = 28.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("You have been logged out of this screen.")

                        AppButton("Back to My Home") {
                            selectedScreen = "My Home"
                        }
                    }

                    "About" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ℹ️ About MyStickerApp", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "MyStickerApp — Turn Your Photos Into Stickers!",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Welcome to MyStickerApp, your creative space for turning your favorite photos and ideas into fun, personalized stickers.",
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            AboutFeatureCard(
                                icon = "🎨",
                                title = "Create Your Own Stickers",
                                description = "Transform your favorite pictures into stickers and express your creativity in your own style."
                            )
                            Spacer(Modifier.height(10.dp))

                            AboutFeatureCard(
                                icon = "✂️",
                                title = "Easy Sticker Editing",
                                description = "Explore sticker creation and editing tools designed to help you personalize your images."
                            )
                            Spacer(Modifier.height(10.dp))

                            AboutFeatureCard(
                                icon = "🖼️",
                                title = "Save Your Creations",
                                description = "Keep your favorite sticker creations so you can find and enjoy them again."
                            )
                            Spacer(Modifier.height(10.dp))

                            AboutFeatureCard(
                                icon = "💡",
                                title = "Made for Creativity",
                                description = "Whether you want to create stickers for friends, family, or social media, MyStickerApp gives you a place to bring your ideas to life."
                            )

                            Spacer(Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Our Mission", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Our mission is to make sticker creation simple, fun, and accessible to everyone.\n\nThank you for being part of MyStickerApp!",
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Text(
                                "MyStickerApp — Create. Customize. Share.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(20.dp))

                            AppButton("← Back to Settings") {
                                selectedScreen = "Settings"
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureCard(icon: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, color = Color.DarkGray)
            }
        }
    }
}

const val ADMOB_BANNER_ID = "ca-app-pub-3095480211105710/1649661924"

@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = ADMOB_BANNER_ID
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
