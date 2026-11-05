package com.mymoises.mystickerapp



import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas as ComposeCanvas


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

            MobileAds.initialize(context)

        } catch (_: Exception) {}

    }



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

                                selected = selectedScreen == "Create" || selectedScreen == "Templates",

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

                        } else if (selectedScreen == "Templates") {

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

                            stickerFiles = remember<List<File>>(stickerRefresh) {

                                loadMyStickerFiles(context)

                            },

                            onAddSticker = { selectedScreen = "Create" },

                            onSeeAllStickers = { selectedScreen = "MyStickers" }

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

                        var sizeDialogTarget by remember { mutableStateOf<File?>(null) }

                        var sizeDialogAction by remember { mutableStateOf<String?>(null) }



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

                                                                        sizeDialogTarget = file

                                                                        sizeDialogAction = "device_gallery"

                                                                    }

                                                                )

                                                                DropdownMenuItem(

                                                                    text = { Text("💬 Add to WhatsApp") },

                                                                    onClick = {

                                                                        stickerMenuTarget = null

                                                                        sizeDialogTarget = file

                                                                        sizeDialogAction = "whatsapp"

                                                                    }

                                                                )

                                                                DropdownMenuItem(

                                                                    text = { Text("📤 Share") },

                                                                    onClick = {

                                                                        stickerMenuTarget = null

                                                                        shareStickerFile(context, file)

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



// Size Selection Dialog for Search

                            sizeDialogTarget?.let { file ->

                                StickerSizeSelectionDialog(

                                    actionTitle = if (sizeDialogAction == "whatsapp") "Share to WhatsApp (Size)" else "Save to Device Gallery (Size)",

                                    onDismiss = {

                                        sizeDialogTarget = null

                                        sizeDialogAction = null

                                    },

                                    onSizeSelected = { targetSizePx ->

                                        val target = sizeDialogTarget

                                        val action = sizeDialogAction

                                        sizeDialogTarget = null

                                        sizeDialogAction = null

                                        if (target != null) {

                                            if (action == "whatsapp") {

                                                shareStickerToWhatsApp(context, target, targetSizePx)

                                            } else {

                                                val success = saveStickerToDeviceGallery(context, target, targetSizePx)

                                                val sizeLabel = if (targetSizePx != null) "${targetSizePx}dp" else "Original"

                                                Toast.makeText(

                                                    context,

                                                    if (success) "Saved to Device Gallery ($sizeLabel)!" else "Could not save.",

                                                    Toast.LENGTH_SHORT

                                                ).show()

                                            }

                                        }

                                    }

                                )

                            }



// Search Preview Dialog

                            previewTarget?.let { file ->

                                val bitmap = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath) }

                                AlertDialog(

                                    onDismissRequest = { previewTarget = null },

                                    title = { Text(file.nameWithoutExtension) },

                                    text = {

                                        Column(

                                            horizontalAlignment = Alignment.CenterHorizontally,

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Box(

                                                modifier = Modifier

                                                    .fillMaxWidth()

                                                    .height(240.dp)

                                                    .background(Color(0xFF1E1E22), RoundedCornerShape(16.dp))

                                                    .padding(8.dp),

                                                contentAlignment = Alignment.Center

                                            ) {

                                                Checkerboard(Modifier.matchParentSize())

                                                if (bitmap != null) {

                                                    Image(

                                                        bitmap = bitmap.asImageBitmap(),

                                                        contentDescription = file.name,

                                                        contentScale = ContentScale.Fit,

                                                        modifier = Modifier.fillMaxSize()

                                                    )

                                                }

                                            }

                                        }

                                    },

                                    confirmButton = {

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                            TextButton(onClick = {

                                                val target = previewTarget

                                                previewTarget = null

                                                if (target != null) {

                                                    sizeDialogTarget = target

                                                    sizeDialogAction = "device_gallery"

                                                }

                                            }) { Text("💾 Save") }

                                            TextButton(onClick = {

                                                val target = previewTarget

                                                previewTarget = null

                                                if (target != null) {

                                                    sizeDialogTarget = target

                                                    sizeDialogAction = "whatsapp"

                                                }

                                            }) { Text("💬 WhatsApp") }

                                            TextButton(onClick = {

                                                val target = previewTarget

                                                previewTarget = null

                                                if (target != null) shareStickerFile(context, target)

                                            }) { Text("📤 Share") }

                                            TextButton(onClick = {

                                                val target = previewTarget

                                                previewTarget = null

                                                if (target != null) {

                                                    selectedStickerToEdit = target

                                                    selectedScreen = "EditSavedSticker"

                                                }

                                            }) { Text("✏️ Edit") }

                                            TextButton(onClick = { previewTarget = null }) { Text("Close") }

                                        }

                                    }

                                )

                            }



// Rename Dialog

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



// Delete Dialog

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

                            onRemoveBackground = {

                                selectedScreen = "Remove Background"

                            },

                            onEditSticker = {

                                selectedScreen = "My Sticker"

                            },

                            onTemplates = {

                                selectedScreen = "Templates"

                            }

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

                        var sizeDialogTarget by remember { mutableStateOf<File?>(null) }

                        var sizeDialogAction by remember { mutableStateOf<String?>(null) }



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

                                                                    sizeDialogTarget = file

                                                                    sizeDialogAction = "device_gallery"

                                                                }

                                                            )

                                                            DropdownMenuItem(

                                                                text = { Text("💬 Add to WhatsApp") },

                                                                onClick = {

                                                                    stickerMenuTarget = null

                                                                    sizeDialogTarget = file

                                                                    sizeDialogAction = "whatsapp"

                                                                }

                                                            )

                                                            DropdownMenuItem(

                                                                text = { Text("📤 Share") },

                                                                onClick = {

                                                                    stickerMenuTarget = null

                                                                    shareStickerFile(context, file)

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



// Size Selection Dialog for MyStickers

                        sizeDialogTarget?.let { file ->

                            StickerSizeSelectionDialog(

                                actionTitle = if (sizeDialogAction == "whatsapp") "Share to WhatsApp (Size)" else "Save to Device Gallery (Size)",

                                onDismiss = {

                                    sizeDialogTarget = null

                                    sizeDialogAction = null

                                },

                                onSizeSelected = { targetSizePx ->

                                    val target = sizeDialogTarget

                                    val action = sizeDialogAction

                                    sizeDialogTarget = null

                                    sizeDialogAction = null

                                    if (target != null) {

                                        if (action == "whatsapp") {

                                            shareStickerToWhatsApp(context, target, targetSizePx)

                                        } else {

                                            val success = saveStickerToDeviceGallery(context, target, targetSizePx)

                                            val sizeLabel = if (targetSizePx != null) "${targetSizePx}dp" else "Original"

                                            Toast.makeText(

                                                context,

                                                if (success) "Saved to Device Gallery ($sizeLabel)!" else "Could not save to device.",

                                                Toast.LENGTH_SHORT

                                            ).show()

                                        }

                                    }

                                }

                            )

                        }



// Preview Dialog

                        previewTarget?.let { file ->

                            val bitmap = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath) }

                            AlertDialog(

                                onDismissRequest = { previewTarget = null },

                                title = { Text(file.nameWithoutExtension) },

                                text = {

                                    Column(

                                        horizontalAlignment = Alignment.CenterHorizontally,

                                        modifier = Modifier.fillMaxWidth()

                                    ) {

                                        Box(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .height(240.dp)

                                                .background(Color(0xFF1E1E22), RoundedCornerShape(16.dp))

                                                .padding(8.dp),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Checkerboard(Modifier.matchParentSize())

                                            if (bitmap != null) {

                                                Image(

                                                    bitmap = bitmap.asImageBitmap(),

                                                    contentDescription = file.name,

                                                    contentScale = ContentScale.Fit,

                                                    modifier = Modifier.fillMaxSize()

                                                )

                                            }

                                        }

                                    }

                                },

                                confirmButton = {

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                        TextButton(onClick = {

                                            val target = previewTarget

                                            previewTarget = null

                                            if (target != null) {

                                                sizeDialogTarget = target

                                                sizeDialogAction = "device_gallery"

                                            }

                                        }) { Text("💾 Save") }

                                        TextButton(onClick = {

                                            val target = previewTarget

                                            previewTarget = null

                                            if (target != null) {

                                                sizeDialogTarget = target

                                                sizeDialogAction = "whatsapp"

                                            }

                                        }) { Text("💬 WhatsApp") }

                                        TextButton(onClick = {

                                            val target = previewTarget

                                            previewTarget = null

                                            if (target != null) shareStickerFile(context, target)

                                        }) { Text("📤 Share") }

                                        TextButton(onClick = {

                                            val target = previewTarget

                                            previewTarget = null

                                            if (target != null) {

                                                selectedStickerToEdit = target

                                                selectedScreen = "EditSavedSticker"

                                            }

                                        }) { Text("✏️ Edit") }

                                        TextButton(onClick = { previewTarget = null }) { Text("Close") }

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

                        Text("❤️ Favorites", fontSize = 28.sp)

                        Spacer(Modifier.height(16.dp))

                        Text("Your favorite stickers will appear here.")



                        AppButton("← Back to My Screen") {

                            selectedScreen = "My Screen"

                        }

                    }



                    "My Creations" -> {

                        Text("🎨 My Creations", fontSize = 28.sp)

                        Spacer(Modifier.height(16.dp))

                        Text("Your creations will appear here.")



                        AppButton("← Back to My Screen") {

                            selectedScreen = "My Screen"

                        }

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



// =====================================

// HOME SCREEN

// =====================================



@Composable

private fun Checkerboard(modifier: Modifier = Modifier) {

    ComposeCanvas(modifier = modifier) {

        val tileSize = 18.dp.toPx()

        val columns = (size.width / tileSize).toInt() + 1

        val rows = (size.height / tileSize).toInt() + 1

        for (row in 0 until rows) {

            for (column in 0 until columns) {

                drawRect(

                    color = if ((row + column) % 2 == 0) {

                        Color(0xFFE8E8E8)

                    } else {

                        Color.White

                    },

                    topLeft = Offset(

                        column * tileSize,

                        row * tileSize

                    ),

                    size = Size(tileSize, tileSize)

                )

            }

        }

    }

}



@Composable

private fun MyHomeScreen(

    stickerFiles: List<File>,

    onAddSticker: () -> Unit,

    onSeeAllStickers: () -> Unit

) {

    Column(

        modifier = Modifier.fillMaxWidth(),

        horizontalAlignment = Alignment.Start

    ) {

        Text("My Home", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))



        Button(

            onClick = onAddSticker,

            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),

            shape = RoundedCornerShape(16.dp)

        ) {

            Text("＋ Add Sticker", fontSize = 18.sp)

        }



        Spacer(Modifier.height(12.dp))

        Text("Dito makikita ang mga sticker mo.")

        Spacer(Modifier.height(20.dp))



        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text("My Stickers", style = MaterialTheme.typography.titleLarge)

            TextButton(onClick = onSeeAllStickers) { Text("See all") }

        }



        if (stickerFiles.isEmpty()) {

            Card(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))

            ) {

                Text(

                    "Wala ka pang sticker. Pindutin ang Add Sticker para gumawa ng una mo.",

                    modifier = Modifier.padding(18.dp),

                    color = Color.DarkGray

                )

            }

        } else {

            stickerFiles.take(4).chunked(4).forEach { rowFiles ->

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    rowFiles.forEach { file ->

                        key(file.absolutePath) {

                            Card(

                                modifier = Modifier.weight(1f),

                                shape = RoundedCornerShape(16.dp)

                            ) {

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                    val bitmap = remember(file.absolutePath) {

                                        BitmapFactory.decodeFile(file.absolutePath)

                                    }

                                    if (bitmap != null) {

                                        Box(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .height(84.dp)

                                                .padding(4.dp)

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

                                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),

                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)

                                    )

                                }

                            }

                        }

                    }

                    repeat(4 - rowFiles.size) { Spacer(Modifier.weight(1f)) }

                }

                Spacer(Modifier.height(12.dp))

            }

        }

    }

}



// =====================================

// CREATE MENU

// =====================================



private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    @Suppress("DEPRECATION")
    val activeNetwork = cm?.activeNetworkInfo
    @Suppress("DEPRECATION")
    return activeNetwork != null && activeNetwork.isConnected
}

private fun generateLocalStickerBitmap(
    inputBmp: Bitmap,
    styleName: String,
    variationIndex: Int
): Bitmap {
    val size = 512
    val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = when ((variationIndex + styleName.length) % 6) {
            0 -> AndroidColor.parseColor("#FFECF2")
            1 -> AndroidColor.parseColor("#E0F2FE")
            2 -> AndroidColor.parseColor("#F3E8FF")
            3 -> AndroidColor.parseColor("#FEF3C7")
            4 -> AndroidColor.parseColor("#DCFCE7")
            else -> AndroidColor.parseColor("#FFEDD5")
        }
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 20f, bgPaint)

    val filterType = when (styleName) {
        "Anime" -> "Contrast"
        "Cartoon" -> "Brightness"
        "Cute" -> "Warm"
        else -> "Vintage"
    }
    val styledInput = applyImageFilter(inputBmp, filterType)

    val srcRect = android.graphics.Rect(0, 0, styledInput.width, styledInput.height)
    val destRect = android.graphics.RectF(48f, 48f, size - 48f, size - 48f)
    canvas.drawBitmap(styledInput, srcRect, destRect, paint)

    return result
}

@Composable
private fun CreateStickerMenu(
    onRemoveBackground: () -> Unit,
    onEditSticker: () -> Unit,
    onTemplates: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Create Sticker", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Choose how you want to create your sticker.")
        Spacer(Modifier.height(18.dp))

        CreateOptionCard(
            icon = "✂️",
            title = "Remove BG",
            subtitle = "Cut out the subject and make a sticker.",
            onClick = onRemoveBackground
        )
        Spacer(Modifier.height(10.dp))
        CreateOptionCard(
            icon = "🖼️",
            title = "Edit My Sticker",
            subtitle = "Choose a photo, edit it, and save as PNG.",
            onClick = onEditSticker
        )
        Spacer(Modifier.height(10.dp))
        CreateOptionCard(
            icon = "🎨",
            title = "Templates",
            subtitle = "Choose from ready-made sticker designs and customize.",
            onClick = onTemplates
        )
    }
}



@Composable

private fun CreateOptionCard(

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



// =====================================

// STICKER EDITOR SCREEN

// =====================================

private data class EditorToolSpec(
    val name: String,
    val icon: String,
    val onClick: () -> Unit
)

private fun calculateVisibleBounds(bitmap: Bitmap): RectF {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0 || bitmap.isRecycled) {
        return RectF(0f, 0f, width.coerceAtLeast(1).toFloat(), height.coerceAtLeast(1).toFloat())
    }

    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
            if (alpha > 10) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    return if (maxX >= minX && maxY >= minY) {
        RectF(minX.toFloat(), minY.toFloat(), (maxX + 1).toFloat(), (maxY + 1).toFloat())
    } else {
        RectF(0f, 0f, width.toFloat(), height.toFloat())
    }
}

private class StickerEditorLayer(val name: String, var bitmap: Bitmap) {

    var visible by mutableStateOf(true)

    var scale by mutableFloatStateOf(1f)

    var rotation by mutableFloatStateOf(0f)

    var offsetX by mutableFloatStateOf(0f)

    var offsetY by mutableFloatStateOf(0f)

    var alpha by mutableFloatStateOf(1f)

    var visibleBounds by mutableStateOf(calculateVisibleBounds(bitmap))

    fun updateVisibleBounds() {
        visibleBounds = calculateVisibleBounds(bitmap)
    }

}



private data class StickerEditorHistoryLayer(

    val name: String,

    val bitmap: Bitmap,

    val visible: Boolean,

    val scale: Float,

    val rotation: Float,

    val offsetX: Float,

    val offsetY: Float,

    val alpha: Float

)



fun generateFreeAutoBackgroundBitmap(width: Int, height: Int): Bitmap {

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bitmap)

    val random = java.util.Random()



    val gradients = listOf(

        intArrayOf(AndroidColor.parseColor("#FF9A9E"), AndroidColor.parseColor("#FECFEF")),

        intArrayOf(AndroidColor.parseColor("#a18cd1"), AndroidColor.parseColor("#fbc2eb")),

        intArrayOf(AndroidColor.parseColor("#ff007f"), AndroidColor.parseColor("#00f0ff")),

        intArrayOf(AndroidColor.parseColor("#4facfe"), AndroidColor.parseColor("#00f2fe")),

        intArrayOf(AndroidColor.parseColor("#43e97b"), AndroidColor.parseColor("#38f9d7")),

        intArrayOf(AndroidColor.parseColor("#fa709a"), AndroidColor.parseColor("#fee140")),

        intArrayOf(AndroidColor.parseColor("#30cfd0"), AndroidColor.parseColor("#330867"))

    )



    val selectedGradient = gradients[random.nextInt(gradients.size)]

    val shader = android.graphics.LinearGradient(

        0f, 0f, width.toFloat(), height.toFloat(),

        selectedGradient, null, android.graphics.Shader.TileMode.CLAMP

    )

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { setShader(shader) }

    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)



    val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textSize = width * 0.06f

        textAlign = Paint.Align.CENTER

    }



    val patternItems = listOf("✨", "⭐", "💖", "🌟", "🌸", "⚡", "🔮")

    val selectedItem = patternItems[random.nextInt(patternItems.size)]



    for (i in 0..18) {

        val px = random.nextFloat() * width

        val py = random.nextFloat() * height

        patternPaint.textSize = (24..64).random().toFloat() * (width / 512f)

        canvas.drawText(selectedItem, px, py, patternPaint)

    }



    return bitmap

}



suspend fun fetchAiBackgroundBitmap(context: Context, prompt: String): Bitmap? = withContext(Dispatchers.IO) {

    try {

        val encoded = java.net.URLEncoder.encode(prompt.ifBlank { "aesthetic galaxy" }, "UTF-8")

        val urlString = "https://image.pollinations.ai/prompt/$encoded?width=1024&height=1024&nologo=true"

        val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection

        connection.connectTimeout = 15000

        connection.readTimeout = 15000

        connection.doInput = true

        connection.connect()

        connection.inputStream.use { inputStream ->

            BitmapFactory.decodeStream(inputStream)

        }

    } catch (e: Exception) {

        e.printStackTrace()

        null

    }

}



data class OnlineBgItem(

    val title: String,

    val source: String,

    val url: String

)



val curatedOnlineBackgrounds = mapOf(

    "Aesthetic" to listOf(

        OnlineBgItem("Aesthetic Pastel Cloud", "Unsplash", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1024&q=80"),

        OnlineBgItem("Soft Gradient Aura", "Unsplash", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1024&q=80"),

        OnlineBgItem("Kawaii Pink Sky", "Pixabay", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1024&q=80"),

        OnlineBgItem("Aesthetic Vaporwave", "Unsplash", "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1024&q=80")

    ),

    "Nature" to listOf(

        OnlineBgItem("Tropical Palm Beach", "Unsplash", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1024&q=80"),

        OnlineBgItem("Forest Fog Trees", "Pixabay", "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1024&q=80"),

        OnlineBgItem("Cherry Blossom Pink", "Unsplash", "https://images.unsplash.com/photo-1522383225653-ed111181a951?w=1024&q=80"),

        OnlineBgItem("Mountain Lake Blue", "Pixabay", "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1024&q=80")

    ),

    "Galaxy" to listOf(

        OnlineBgItem("Purple Galaxy Nebula", "Unsplash", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1024&q=80"),

        OnlineBgItem("Starry Night Cosmos", "Pixabay", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1024&q=80"),

        OnlineBgItem("Deep Blue Milky Way", "Unsplash", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1024&q=80")

    ),

    "Sunset" to listOf(

        OnlineBgItem("Golden Hour Ocean", "Unsplash", "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?w=1024&q=80"),

        OnlineBgItem("Purple Sunset Clouds", "Pixabay", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1024&q=80"),

        OnlineBgItem("Pink Horizon Sky", "Unsplash", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1024&q=80")

    ),

    "Patterns" to listOf(

        OnlineBgItem("Kawaii Heart Pattern", "Pixabay", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1024&q=80"),

        OnlineBgItem("Geometric Gold Lines", "Unsplash", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1024&q=80"),

        OnlineBgItem("Aesthetic Marble Stone", "Pixabay", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1024&q=80")

    )

)



suspend fun fetchOnlineImageBitmap(context: Context, imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {

    try {

        val url = java.net.URL(imageUrl)

        val connection = url.openConnection() as java.net.HttpURLConnection

        connection.connectTimeout = 15000

        connection.readTimeout = 15000

        connection.doInput = true

        connection.connect()

        connection.inputStream.use { inputStream ->

            BitmapFactory.decodeStream(inputStream)

        }

    } catch (e: Exception) {

        e.printStackTrace()

        null

    }

}

private fun applyImageFilter(source: Bitmap, filterType: String): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cm = android.graphics.ColorMatrix()
    when (filterType) {
        "Grayscale" -> cm.setSaturation(0f)
        "Sepia" -> {
            cm.set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Invert" -> {
            cm.set(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Brightness" -> {
            cm.set(floatArrayOf(
                1.3f, 0f, 0f, 0f, 40f,
                0f, 1.3f, 0f, 0f, 40f,
                0f, 0f, 1.3f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Contrast" -> {
            val scale = 1.5f
            val translate = (-0.5f * scale + 0.5f) * 255f
            cm.set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Vintage" -> {
            cm.set(floatArrayOf(
                0.9f, 0f, 0f, 0f, 25f,
                0f, 0.7f, 0f, 0f, 15f,
                0f, 0f, 0.5f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Neon" -> {
            cm.set(floatArrayOf(
                0f, 1f, 1f, 0f, 0f,
                1f, 0f, 1f, 0f, 0f,
                1f, 1f, 0f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Warm" -> {
            cm.set(floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.1f, 0f, 0f, 10f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        "Cool" -> {
            cm.set(floatArrayOf(
                0.8f, 0f, 0f, 0f, 0f,
                0f, 1.0f, 0f, 0f, 10f,
                0f, 0f, 1.3f, 0f, 30f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        else -> return source
    }
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
}

private fun createShapeBitmap(shapeType: String, size: Int, color: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val center = size / 2f
    val radius = size / 2f - 16f
    when (shapeType) {
        "Circle" -> canvas.drawCircle(center, center, radius, paint)
        "Rectangle" -> canvas.drawRect(16f, 16f, size - 16f, size - 16f, paint)
        "Rounded Rect" -> canvas.drawRoundRect(16f, 16f, size - 16f, size - 16f, 40f, 40f, paint)
        "Star" -> {
            val path = android.graphics.Path()
            val outerRadius = radius
            val innerRadius = radius * 0.4f
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val angle = Math.toRadians((i * 36 - 90).toDouble())
                val x = (center + r * Math.cos(angle)).toFloat()
                val y = (center + r * Math.sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
        "Heart" -> {
            val path = android.graphics.Path()
            val w = size.toFloat()
            val h = size.toFloat()
            path.moveTo(w / 2, h / 5 * 4)
            path.cubicTo(w / 14, h / 15 * 9, 0f, h / 5 * 2, w / 4, h / 5)
            path.cubicTo(w / 8 * 3, h / 10, w / 2, h / 5 * 2, w / 2, h / 5 * 2)
            path.cubicTo(w / 2, h / 5 * 2, w / 8 * 5, h / 10, w / 4 * 3, h / 5)
            path.cubicTo(w, h / 5 * 2, w / 14 * 13, h / 15 * 9, w / 2, h / 5 * 4)
            path.close()
            canvas.drawPath(path, paint)
        }
        "Frame" -> {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 24f
            canvas.drawRect(24f, 24f, size - 24f, size - 24f, paint)
        }
    }
    return bmp
}

private fun drawStrokeOnBitmap(
    bitmap: Bitmap,
    prevPos: Offset,
    currentPos: Offset,
    canvasSize: Size,
    layer: StickerEditorLayer,
    color: Int,
    brushSize: Float,
    isEraser: Boolean
) {
    val canvasBaseWidth = 1024f
    val canvasBaseHeight = 1024f

    val centerCanvasX = canvasSize.width / 2f
    val centerCanvasY = canvasSize.height / 2f

    val layerTranslationX = layer.offsetX * (canvasSize.width / canvasBaseWidth)
    val layerTranslationY = layer.offsetY * (canvasSize.height / canvasBaseHeight)

    val fitScale = minOf(
        canvasSize.width / bitmap.width.coerceAtLeast(1).toFloat(),
        canvasSize.height / bitmap.height.coerceAtLeast(1).toFloat()
    )

    val effectiveScale = (fitScale * layer.scale).coerceAtLeast(0.01f)

    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
        this.strokeWidth = brushSize / effectiveScale
        if (isEraser) {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            this.color = color
        }
    }

    fun canvasToBitmapCoords(p: Offset): Offset {
        val dx = (p.x - centerCanvasX - layerTranslationX) / layer.scale.coerceAtLeast(0.01f)
        val dy = (p.y - centerCanvasY - layerTranslationY) / layer.scale.coerceAtLeast(0.01f)
        val rad = Math.toRadians(-layer.rotation.toDouble())
        val rx = (dx * Math.cos(rad) - dy * Math.sin(rad)).toFloat()
        val ry = (dx * Math.sin(rad) + dy * Math.cos(rad)).toFloat()
        val bx = (rx / fitScale) + (bitmap.width / 2f)
        val by = (ry / fitScale) + (bitmap.height / 2f)
        return Offset(bx, by)
    }

    val start = canvasToBitmapCoords(prevPos)
    val end = canvasToBitmapCoords(currentPos)
    canvas.drawLine(start.x, start.y, end.x, end.y, paint)
    layer.updateVisibleBounds()
}

private fun findLayerAtTouch(
    p: Offset,
    layers: List<StickerEditorLayer>,
    canvasSize: Size
): Int {
    val canvasBaseWidth = 1024f
    val canvasBaseHeight = 1024f

    val centerCanvasX = canvasSize.width / 2f
    val centerCanvasY = canvasSize.height / 2f

    for (i in layers.lastIndex downTo 0) {
        val layer = layers[i]
        if (!layer.visible) continue

        val bitmap = layer.bitmap
        if (bitmap.isRecycled) continue

        val layerTranslationX = layer.offsetX * (canvasSize.width / canvasBaseWidth)
        val layerTranslationY = layer.offsetY * (canvasSize.height / canvasBaseHeight)

        val fitScale = minOf(
            canvasSize.width / bitmap.width.coerceAtLeast(1).toFloat(),
            canvasSize.height / bitmap.height.coerceAtLeast(1).toFloat()
        )

        val dx = (p.x - centerCanvasX - layerTranslationX) / layer.scale.coerceAtLeast(0.01f)
        val dy = (p.y - centerCanvasY - layerTranslationY) / layer.scale.coerceAtLeast(0.01f)
        val rad = Math.toRadians(-layer.rotation.toDouble())
        val rx = (dx * Math.cos(rad) - dy * Math.sin(rad)).toFloat()
        val ry = (dx * Math.sin(rad) + dy * Math.cos(rad)).toFloat()
        val bx = (rx / fitScale) + (bitmap.width / 2f)
        val by = (ry / fitScale) + (bitmap.height / 2f)

        val ix = bx.toInt()
        val iy = by.toInt()

        if (ix in 0 until bitmap.width && iy in 0 until bitmap.height) {
            val pixel = bitmap.getPixel(ix, iy)
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha > 10) {
                return i
            }
        }
    }
    return -1
}

@Composable
private fun StickerEditorScreen(
    initialStickerFile: File? = null,
    initialUserTemplate: UserStickerTemplate? = null,
    creatorName: String = "My Creator",
    onStickerSaved: () -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val layers = remember { mutableStateListOf<StickerEditorLayer>() }

    var currentTool by remember { mutableStateOf("Move") }

    var textDialog by remember { mutableStateOf(false) }

    var emojiDialog by remember { mutableStateOf(false) }

    var backgroundGenDialog by remember { mutableStateOf(false) }

    var onlineBgDialog by remember { mutableStateOf(false) }

    var onlineSearchQuery by remember { mutableStateOf("") }

    var onlineSelectedCategory by remember { mutableStateOf("Aesthetic") }

    var isDownloadingOnlineBg by remember { mutableStateOf(false) }

    var bgDialogTab by remember { mutableStateOf("Free") }

    var previewBgBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var aiPromptText by remember { mutableStateOf("pink galaxy with cute stars") }

    var isAiGenerating by remember { mutableStateOf(false) }

    var layerText by remember { mutableStateOf("") }

    var notice by remember { mutableStateOf("I-edit ang sticker gamit ang Move, Remove BG, Outline, Brush, atbp.") }

    var activeLayer by remember { mutableIntStateOf(-1) }

    var colorIndex by remember { mutableIntStateOf(0) }

    var outlineColorIndex by remember { mutableIntStateOf(2) }

    var brushSize by remember { mutableFloatStateOf(18f) }

    var canvasVersion by remember { mutableIntStateOf(0) }

    var selectedShape by remember { mutableStateOf("Rectangle") }

    var showSaveSizeDialog by remember { mutableStateOf(false) }

    var showSaveTemplateDialog by remember { mutableStateOf(false) }

    var templateNameInput by remember { mutableStateOf("My Template") }

    var selectedCategoryInput by remember { mutableStateOf("Cute") }

    var isPublishedInput by remember { mutableStateOf(true) }

    var pendingOutputBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isRemovingBg by remember { mutableStateOf(false) }

    var activeFilter by remember { mutableStateOf("Original") }

    var lastTouchPos by remember { mutableStateOf<Offset?>(null) }

    val originalBitmapsMap = remember { mutableMapOf<String, Bitmap>() }



    val palette = listOf(

        AndroidColor.RED,

        AndroidColor.BLACK,

        AndroidColor.WHITE,

        AndroidColor.BLUE,

        AndroidColor.GREEN,

        AndroidColor.YELLOW,

        AndroidColor.parseColor("#FFA500"),

        AndroidColor.parseColor("#800080"),

        AndroidColor.parseColor("#00FFFF"),

        AndroidColor.parseColor("#FFC0CB")

    )



    val history = remember { mutableStateListOf<List<StickerEditorHistoryLayer>>() }

    val redoHistory = remember { mutableStateListOf<List<StickerEditorHistoryLayer>>() }

    var originalLayers by remember { mutableStateOf<List<Bitmap>>(emptyList()) }



    fun snapshot() {

        history.add(layers.map { StickerEditorHistoryLayer(it.name, it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.visible, it.scale, it.rotation, it.offsetX, it.offsetY, it.alpha) })

        redoHistory.clear()

        if (history.size > 20) history.removeAt(0)

    }



    fun addBitmap(name: String, bitmap: Bitmap) {

        snapshot()

        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val layer = StickerEditorLayer(name, copy)

        layers.add(layer)

        originalBitmapsMap[name] = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        activeLayer = layers.lastIndex

        canvasVersion++

    }



    val canvasBaseWidth = 1024f

    val canvasBaseHeight = 1024f

    val canvasAspectRatio = 1f



    LaunchedEffect(initialStickerFile) {

        if (initialStickerFile != null && initialStickerFile.exists()) {

            val bitmap = BitmapFactory.decodeFile(initialStickerFile.absolutePath)

            if (bitmap != null) {

                addBitmap("Base Sticker", bitmap)

                originalLayers = listOf(bitmap.copy(Bitmap.Config.ARGB_8888, true))

            }

        }

    }

    LaunchedEffect(initialUserTemplate) {

        if (initialUserTemplate != null) {

            layers.clear()

            val loadedBitmaps = mutableListOf<Bitmap>()

            initialUserTemplate.layers.forEach { layerData ->

                val fileName = layerData.bitmapFileName

                if (fileName != null) {

                    val bmp = UserStickerTemplatesManager.loadLayerBitmap(initialUserTemplate.dirPath, fileName)

                    if (bmp != null) {

                        val layer = StickerEditorLayer(layerData.name, bmp.copy(Bitmap.Config.ARGB_8888, true)).apply {

                            visible = layerData.visible

                            scale = layerData.scale

                            rotation = layerData.rotation

                            offsetX = layerData.offsetX

                            offsetY = layerData.offsetY

                            alpha = layerData.alpha

                        }

                        layers.add(layer)

                        loadedBitmaps.add(bmp.copy(Bitmap.Config.ARGB_8888, true))

                    }

                }

            }

            if (loadedBitmaps.isNotEmpty()) {

                originalLayers = loadedBitmaps

                activeLayer = layers.lastIndex

            }

        }

    }



    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->

        if (uri != null) {

            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

            if (bitmap != null) {

                val targetWidth = originalLayers.firstOrNull()?.width ?: 1024

                val targetHeight = originalLayers.firstOrNull()?.height ?: 1024

                val copy = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

                val target = Canvas(copy)

                val scale = minOf(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height)

                val w = bitmap.width * scale

                val h = bitmap.height * scale


                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                target.drawBitmap(bitmap, null, android.graphics.RectF((targetWidth-w)/2, (targetHeight-h)/2, (targetWidth+w)/2, (targetHeight+h)/2), paint)

                addBitmap("Photo ${layers.size + 1}", copy)

                if (originalLayers.isEmpty()) originalLayers = listOf(copy.copy(Bitmap.Config.ARGB_8888, true))

                notice = "Naidagdag ang larawan bilang bagong layer."

            } else notice = "Hindi mabuksan ang larawan."

        }

    }



    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

        if (bitmap != null) {

            val targetWidth = originalLayers.firstOrNull()?.width ?: 1024

            val targetHeight = originalLayers.firstOrNull()?.height ?: 1024

            val copy = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

            val target = Canvas(copy)

            val scale = minOf(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height)

            val w = bitmap.width * scale

            val h = bitmap.height * scale

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            target.drawBitmap(bitmap, null, android.graphics.RectF((targetWidth-w)/2, (targetHeight-h)/2, (targetWidth+w)/2, (targetHeight+h)/2), paint)

            addBitmap("Camera ${layers.size + 1}", copy)

            if (originalLayers.isEmpty()) originalLayers = listOf(copy.copy(Bitmap.Config.ARGB_8888, true))

            notice = "Naidagdag ang kuha na larawan mula sa camera."

        }

    }



    val editorRed = Color(0xFF1685F8)

    val panelColor = Color(0xFF29292D)

    val editorTools = listOf(

        "Remove BG" to "✂️",

        "Outline" to "🎨",

        "Eraser" to "🧹",

        "Brush" to "🖌️",

        "Camera" to "📸",

        "Gallery" to "🖼️",

        "Restore" to "↩️",

        "Shapes" to "◩",

        "Text" to "T",

        "Filters" to "✨",

        "Emoji" to "⭐",

        "Auto BG" to "🌌",

        "Online BG" to "🌐",

        "Layers" to "🥞"

    )



    Column(

        Modifier.fillMaxSize().background(Color(0xFF111113)).padding(bottom = 12.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {

// Top Bar Container

        Column(

            modifier = Modifier

                .fillMaxWidth()

                .background(Color(0xFF1D1D20), RoundedCornerShape(16.dp))

                .border(1.dp, Color(0xFF383840), RoundedCornerShape(16.dp))

                .padding(horizontal = 12.dp, vertical = 10.dp)

        ) {

            // ROW 1: Navigation Header with Centered Title

            Box(

                modifier = Modifier.fillMaxWidth(),

                contentAlignment = Alignment.Center

            ) {

                IconButton(

                    onClick = onBack,

                    modifier = Modifier.align(Alignment.CenterStart)

                ) {

                    Text("‹", color = Color.White, fontSize = 28.sp)

                }



                Text(

                    text = "Edit My Sticker",

                    color = Color.White,

                    fontSize = 18.sp,

                    fontWeight = FontWeight.Bold,

                    textAlign = TextAlign.Center

                )

            }



            Spacer(Modifier.height(10.dp))



            // ROW 2: Action Buttons Row (Undo, Redo, Template, Save)

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(8.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                // 1. Undo

                Button(

                    onClick = {

                        if (history.isNotEmpty()) {

                            redoHistory.add(layers.map { StickerEditorHistoryLayer(it.name, it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.visible, it.scale, it.rotation, it.offsetX, it.offsetY, it.alpha) })

                            val previous = history.removeAt(history.lastIndex)

                            layers.clear()

                            previous.forEach { s -> layers.add(StickerEditorLayer(s.name, s.bitmap).also { it.visible=s.visible; it.scale=s.scale; it.rotation=s.rotation; it.offsetX=s.offsetX; it.offsetY=s.offsetY; it.alpha=s.alpha }) }

                            activeLayer = layers.lastIndex; canvasVersion++

                        }

                    },

                    modifier = Modifier.weight(1f).height(40.dp),

                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),

                    shape = RoundedCornerShape(10.dp),

                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B30))

                ) {

                    Text("↶ Undo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                }



                // 2. Redo

                Button(

                    onClick = {

                        if (redoHistory.isNotEmpty()) {

                            history.add(layers.map { StickerEditorHistoryLayer(it.name, it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.visible, it.scale, it.rotation, it.offsetX, it.offsetY, it.alpha) })

                            val next = redoHistory.removeAt(redoHistory.lastIndex)

                            layers.clear()

                            next.forEach { s -> layers.add(StickerEditorLayer(s.name, s.bitmap).also { it.visible=s.visible; it.scale=s.scale; it.rotation=s.rotation; it.offsetX=s.offsetX; it.offsetY=s.offsetY; it.alpha=s.alpha }) }

                            activeLayer = layers.lastIndex; canvasVersion++

                        }

                    },

                    modifier = Modifier.weight(1f).height(40.dp),

                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),

                    shape = RoundedCornerShape(10.dp),

                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B30))

                ) {

                    Text("↷ Redo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                }



                // 3. Template

                Button(

                    onClick = {

                        val visibleLayers = layers.filter { it.visible }

                        if (visibleLayers.isEmpty()) {

                            notice = "Magdagdag muna ng larawan bago mag-save ng template."

                        } else {

                            showSaveTemplateDialog = true

                        }

                    },

                    modifier = Modifier.weight(1f).height(40.dp),

                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),

                    shape = RoundedCornerShape(10.dp),

                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))

                ) {

                    Text("📋 Template", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                }



                // 4. Save

                Button(

                    onClick = {

                        val visibleLayers = layers.filter { it.visible }

                        if (visibleLayers.isEmpty()) {

                            notice = "Magdagdag muna ng larawan bago mag-save."

                        } else {

                            val outputWidth = originalLayers.firstOrNull()?.width ?: visibleLayers.first().bitmap.width

                            val outputHeight = originalLayers.firstOrNull()?.height ?: visibleLayers.first().bitmap.height

                            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)

                            val outputCanvas = Canvas(output)

                            visibleLayers.forEach { layer ->

                                outputCanvas.save()

                                outputCanvas.translate(outputWidth / 2f + layer.offsetX, outputHeight / 2f + layer.offsetY)

                                outputCanvas.rotate(layer.rotation)

                                outputCanvas.scale(layer.scale, layer.scale)

                                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {

                                    alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)

                                }

                                outputCanvas.drawBitmap(

                                    layer.bitmap,

                                    -layer.bitmap.width / 2f,

                                    -layer.bitmap.height / 2f,

                                    paint

                                )

                                outputCanvas.restore()

                            }

                            pendingOutputBitmap = output

                            showSaveSizeDialog = true

                        }

                    },

                    modifier = Modifier.weight(1f).height(40.dp),

                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),

                    shape = RoundedCornerShape(10.dp),

                    colors = ButtonDefaults.buttonColors(containerColor = editorRed)

                ) {

                    Text("💾 Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                }

            }

        }



        if (showSaveSizeDialog) {

            StickerSizeSelectionDialog(

                actionTitle = "Save Sticker Output Size",

                onDismiss = {

                    showSaveSizeDialog = false

                    pendingOutputBitmap = null

                },

                onSizeSelected = { targetSizePx ->

                    val bmp = pendingOutputBitmap

                    showSaveSizeDialog = false

                    pendingOutputBitmap = null

                    if (bmp != null) {

                        if (saveMyStickerBitmap(context, bmp, targetSizePx)) {

                            onStickerSaved()

                            val sizeLabel = if (targetSizePx != null) "${targetSizePx}dp" else "Original"

                            notice = "Na-save ang sticker ($sizeLabel) sa My Stickers!"

                            Toast.makeText(context, "Sticker saved ($sizeLabel) successfully!", Toast.LENGTH_SHORT).show()

                        } else notice = "Hindi na-save ang sticker."

                    }

                }

            )

        }

        if (showSaveTemplateDialog) {

            val templateCats = listOf("Cute", "Love", "Funny", "Reactions", "Meme", "Speech Bubbles")

            AlertDialog(

                onDismissRequest = { showSaveTemplateDialog = false },

                title = { Text("Save as Template") },

                text = {

                    Column {

                        Text("Template Name:")

                        Spacer(Modifier.height(4.dp))

                        OutlinedTextField(

                            value = templateNameInput,

                            onValueChange = { templateNameInput = it },

                            singleLine = true,

                            label = { Text("Template Name") },

                            modifier = Modifier.fillMaxWidth()

                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Category:")

                        Spacer(Modifier.height(4.dp))

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .horizontalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            templateCats.forEach { cat ->

                                FilterChip(

                                    selected = selectedCategoryInput == cat,

                                    onClick = { selectedCategoryInput = cat },

                                    label = { Text(cat, fontSize = 12.sp) }

                                )

                            }

                        }

                        Spacer(Modifier.height(12.dp))

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            modifier = Modifier.clickable { isPublishedInput = !isPublishedInput }

                        ) {

                            Checkbox(

                                checked = isPublishedInput,

                                onCheckedChange = { isPublishedInput = it }

                            )

                            Spacer(Modifier.width(6.dp))

                            Text("Publish to Community Templates", fontSize = 13.sp)

                        }

                    }

                },

                confirmButton = {

                    Button(onClick = {

                        val visibleLayers = layers.filter { it.visible }

                        if (visibleLayers.isNotEmpty()) {

                            val outputWidth = originalLayers.firstOrNull()?.width ?: visibleLayers.first().bitmap.width

                            val outputHeight = originalLayers.firstOrNull()?.height ?: visibleLayers.first().bitmap.height

                            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)

                            val outputCanvas = Canvas(output)

                            visibleLayers.forEach { layer ->

                                outputCanvas.save()

                                outputCanvas.translate(outputWidth / 2f + layer.offsetX, outputHeight / 2f + layer.offsetY)

                                outputCanvas.rotate(layer.rotation)

                                outputCanvas.scale(layer.scale, layer.scale)

                                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {

                                    alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)

                                }

                                outputCanvas.drawBitmap(

                                    layer.bitmap,

                                    -layer.bitmap.width / 2f,

                                    -layer.bitmap.height / 2f,

                                    paint

                                )

                                outputCanvas.restore()

                            }



                            val layerDataList = visibleLayers.map { layer ->

                                Triple(

                                    layer.name,

                                    layer.bitmap,

                                    UserTemplateLayerData(

                                        name = layer.name,

                                        bitmapFileName = null,

                                        visible = layer.visible,

                                        scale = layer.scale,

                                        rotation = layer.rotation,

                                        offsetX = layer.offsetX,

                                        offsetY = layer.offsetY,

                                        alpha = layer.alpha

                                    )

                                )

                            }



                            val saved = UserStickerTemplatesManager.saveUserTemplate(

                                context = context,

                                templateName = templateNameInput,

                                creatorName = creatorName,

                                category = selectedCategoryInput,

                                layerDataList = layerDataList,

                                previewBitmap = output,

                                isPublished = isPublishedInput,

                                isOwnedByCurrentUser = true

                            )



                            if (saved) {

                                notice = "Template '$templateNameInput' saved to My Templates!"

                                Toast.makeText(context, "Template saved to My Templates!", Toast.LENGTH_SHORT).show()

                                showSaveTemplateDialog = false

                            } else {

                                notice = "Failed to save template."

                                Toast.makeText(context, "Failed to save template.", Toast.LENGTH_SHORT).show()

                            }

                        }

                    }) { Text("Save Template") }

                },

                dismissButton = {

                    OutlinedButton(onClick = { showSaveTemplateDialog = false }) { Text("Cancel") }

                }

            )

        }



// Full Screen Canvas Container

        var canvasSizePx by remember { mutableStateOf(Size.Zero) }

        Box(

            Modifier

                .fillMaxWidth()

                .aspectRatio(canvasAspectRatio)

                .border(1.dp, Color(0xFF3B3B42), RoundedCornerShape(12.dp))

                .background(Color(0xFF151518), RoundedCornerShape(12.dp))

                .onGloballyPositioned { coordinates ->

                    canvasSizePx = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())

                },

            contentAlignment = Alignment.Center

        ) {

            val density = LocalDensity.current

            val containerWidthPx = if (canvasSizePx.width > 0f) canvasSizePx.width else 1024f

            val containerHeightPx = if (canvasSizePx.height > 0f) canvasSizePx.height else 1024f

            val visibleLayers = layers.filter { it.visible }



            if (visibleLayers.isEmpty()) Text("Pumili o magdagdag ng larawan", color = Color.White)

            else Box(Modifier.fillMaxSize().pointerInput(currentTool, activeLayer, colorIndex, brushSize, layers.size, canvasVersion) {

                if (currentTool != "Brush" && currentTool != "Eraser") {

                    awaitEachGesture {

                        val down = awaitFirstDown(requireUnconsumed = false)

                        val hitIndex = findLayerAtTouch(down.position, layers, Size(size.width.toFloat(), size.height.toFloat()))

                        activeLayer = hitIndex



                        do {

                            val event = awaitPointerEvent()

                            val canceled = event.changes.any { it.isConsumed }

                            if (!canceled && activeLayer in 0..layers.lastIndex) {

                                val pan = event.calculatePan()

                                val zoom = event.calculateZoom()

                                val rotationChange = event.calculateRotation()



                                val layer = layers[activeLayer]

                                if (layer.visible) {

                                    layer.offsetX += pan.x * canvasBaseWidth / size.width.coerceAtLeast(1)

                                    layer.offsetY += pan.y * canvasBaseHeight / size.height.coerceAtLeast(1)

                                    layer.scale = (layer.scale * zoom).coerceIn(0.2f, 5f)

                                    layer.rotation += rotationChange

                                }



                                if (pan != Offset.Zero || zoom != 1f || rotationChange != 0f) {

                                    event.changes.forEach { it.consume() }

                                }

                            }

                        } while (event.changes.any { it.pressed })

                    }

                } else if (currentTool == "Brush" || currentTool == "Eraser") {

                    detectDragGestures(

                        onDragStart = { offset ->

                            val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@detectDragGestures

                            if (!layer.visible) return@detectDragGestures

                            snapshot()

                            lastTouchPos = offset

                        },

                        onDrag = { change, _ ->

                            val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@detectDragGestures

                            if (!layer.visible) return@detectDragGestures

                            val prev = lastTouchPos ?: change.position

                            drawStrokeOnBitmap(

                                layer.bitmap,

                                prev,

                                change.position,

                                Size(size.width.toFloat(), size.height.toFloat()),

                                layer,

                                palette[colorIndex],

                                brushSize,

                                isEraser = (currentTool == "Eraser")

                            )

                            lastTouchPos = change.position

                            canvasVersion++

                        },

                        onDragEnd = { lastTouchPos = null },

                        onDragCancel = { lastTouchPos = null }

                    )

                }

            }) {

                Checkerboard(Modifier.matchParentSize())



                val version = canvasVersion

                visibleLayers.forEachIndexed { index, layer ->

                    val isSelected = index == activeLayer

                    Box(

                        Modifier

                            .fillMaxSize()

                            .graphicsLayer {

                                translationX = layer.offsetX * containerWidthPx / canvasBaseWidth

                                translationY = layer.offsetY * containerHeightPx / canvasBaseHeight

                                scaleX = layer.scale

                                scaleY = layer.scale

                                rotationZ = layer.rotation

                                alpha = layer.alpha

                            },

                        contentAlignment = Alignment.TopStart

                    ) {

                        Image(

                            bitmap = layer.bitmap.asImageBitmap(),

                            contentDescription = layer.name,

                            contentScale = ContentScale.Fit,

                            modifier = Modifier.fillMaxSize()

                        )



                        if (isSelected && currentTool != "Brush" && currentTool != "Eraser") {

                            val bmpW = layer.bitmap.width.coerceAtLeast(1).toFloat()

                            val bmpH = layer.bitmap.height.coerceAtLeast(1).toFloat()



                            val fitScale = minOf(containerWidthPx / bmpW, containerHeightPx / bmpH)

                            val bitmapLeftInBox = (containerWidthPx - bmpW * fitScale) / 2f

                            val bitmapTopInBox = (containerHeightPx - bmpH * fitScale) / 2f



                            val bounds = layer.visibleBounds

                            val selLeftDp = with(density) { (bitmapLeftInBox + bounds.left * fitScale).toDp() }

                            val selTopDp = with(density) { (bitmapTopInBox + bounds.top * fitScale).toDp() }

                            val selWidthDp = with(density) { (bounds.width() * fitScale).toDp() }

                            val selHeightDp = with(density) { (bounds.height() * fitScale).toDp() }



                            Box(

                                modifier = Modifier

                                    .offset(x = selLeftDp, y = selTopDp)

                                    .size(width = selWidthDp, height = selHeightDp)

                                    .border(2.dp, editorRed, RoundedCornerShape(4.dp))

                            )

                        }

                    }

                }



                if (isRemovingBg) {

                    Box(

                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),

                        contentAlignment = Alignment.Center

                    ) {

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            CircularProgressIndicator(color = editorRed)

                            Spacer(Modifier.height(8.dp))

                            Text("Auto Removing Background…", color = Color.White, fontSize = 14.sp)

                        }

                    }

                }

            }

        }



// Active Tool Control Sub-Panel

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .heightIn(min = 44.dp)

                .background(panelColor, RoundedCornerShape(8.dp))

                .padding(horizontal = 6.dp, vertical = 4.dp),

            contentAlignment = Alignment.Center

        ) {

            when (currentTool) {

                "Move" -> {

                    Row(

                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),

                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        FilterChip(

                            selected = false,

                            onClick = {

                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@FilterChip

                                snapshot()

                                layer.rotation = (layer.rotation + 90f) % 360f

                                canvasVersion++

                            },

                            label = { Text("🔄 Rotate 90°") }

                        )

                        FilterChip(

                            selected = false,

                            onClick = {

                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@FilterChip

                                snapshot()

                                layer.scale = -layer.scale

                                canvasVersion++

                            },

                            label = { Text("↔ Flip") }

                        )

                        FilterChip(

                            selected = false,

                            onClick = {

                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@FilterChip

                                snapshot()

                                layer.offsetX = 0f

                                layer.offsetY = 0f

                                canvasVersion++

                            },

                            label = { Text("🎯 Center") }

                        )

                        FilterChip(

                            selected = false,

                            onClick = {

                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@FilterChip

                                snapshot()

                                layer.offsetX = 0f

                                layer.offsetY = 0f

                                layer.scale = 1f

                                layer.rotation = 0f

                                canvasVersion++

                            },

                            label = { Text("↩ Reset") }

                        )

                        FilterChip(

                            selected = false,

                            onClick = {

                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@FilterChip

                                addBitmap("${layer.name} Copy", layer.bitmap)

                                notice = "Na-duplicate ang layer."

                            },

                            label = { Text("📋 Duplicate") }

                        )

                    }

                }



                "Remove BG" -> {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(8.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Button(

                            onClick = {

                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null

                                if (layer == null) {

                                    notice = "Pumili muna ng layer na tatanggalan ng background."

                                    return@Button

                                }

                                scope.launch {

                                    isRemovingBg = true

                                    notice = "Awtomatikong tinatanggal ang background ng layer..."

                                    val cutout = performAutoBackgroundRemoval(context, layer.bitmap)

                                    if (cutout != null) {

                                        snapshot()

                                        layer.bitmap = cutout

                                        canvasVersion++

                                        notice = "Na-remove na ang background ng layer!"

                                        Toast.makeText(context, "Background removed!", Toast.LENGTH_SHORT).show()

                                    } else {

                                        notice = "Walang subject na nahanap sa layer."

                                        Toast.makeText(context, "Could not detect subject.", Toast.LENGTH_SHORT).show()

                                    }

                                    isRemovingBg = false

                                }

                            },

                            enabled = !isRemovingBg && activeLayer in 0..layers.lastIndex,

                            colors = ButtonDefaults.buttonColors(containerColor = editorRed),

                            modifier = Modifier.weight(1f)

                        ) {

                            Text("✨ Auto Cutout Layer", fontSize = 12.sp)

                        }

                    }

                }



                "Outline" -> {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        Text("Choose Outline Color & Apply:", fontSize = 11.sp, color = Color.White)

                        Spacer(Modifier.height(4.dp))

                        Row(

                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(6.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            palette.forEachIndexed { idx, color ->

                                Box(

                                    modifier = Modifier

                                        .size(26.dp)

                                        .clip(CircleShape)

                                        .background(Color(color))

                                        .border(if (outlineColorIndex == idx) 2.dp else 0.dp, Color.White, CircleShape)

                                        .clickable { outlineColorIndex = idx }

                                )

                            }

                            Button(

                                onClick = {

                                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null

                                    if (layer != null) {

                                        snapshot()

                                        layer.bitmap = addStickerOutline(layer.bitmap, palette[outlineColorIndex])

                                        canvasVersion++

                                        notice = "Na-apply ang outline sa layer!"

                                    } else notice = "Pumili muna ng layer."

                                },

                                colors = ButtonDefaults.buttonColors(containerColor = editorRed),

                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)

                            ) { Text("Apply", fontSize = 11.sp) }

                        }

                    }

                }



                "Eraser" -> {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text("Eraser ${brushSize.toInt()}dp", fontSize = 11.sp, color = Color.White)

                        Spacer(Modifier.width(6.dp))

                        Slider(

                            value = brushSize,

                            onValueChange = { brushSize = it },

                            valueRange = 8f..80f,

                            modifier = Modifier.weight(1f)

                        )

                        TextButton(onClick = {

                            val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null

                            if (layer != null) {

                                snapshot()

                                layer.bitmap.eraseColor(AndroidColor.TRANSPARENT)

                                canvasVersion++

                                notice = "Na-clear ang layer."

                            }

                        }) { Text("Clear", color = Color.Red, fontSize = 11.sp) }

                    }

                }



                "Brush" -> {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Text("Brush ${brushSize.toInt()}dp", fontSize = 11.sp, color = Color.White)

                            Spacer(Modifier.width(6.dp))

                            Slider(

                                value = brushSize,

                                onValueChange = { brushSize = it },

                                valueRange = 4f..60f,

                                modifier = Modifier.weight(1f)

                            )

                        }

                        Row(

                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            palette.forEachIndexed { idx, color ->

                                Box(

                                    modifier = Modifier

                                        .size(24.dp)

                                        .clip(CircleShape)

                                        .background(Color(color))

                                        .border(if (colorIndex == idx) 2.dp else 0.dp, Color.White, CircleShape)

                                        .clickable { colorIndex = idx }

                                )

                            }

                        }

                    }

                }



                "Shapes" -> {

                    Row(

                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),

                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        listOf("Rectangle", "Circle", "Heart", "Star", "Rounded Rect", "Frame").forEach { shape ->

                            FilterChip(

                                selected = selectedShape == shape,

                                onClick = {

                                    selectedShape = shape

                                    val shapeBmp = createShapeBitmap(shape, 512, palette[colorIndex])

                                    addBitmap("Shape $shape", shapeBmp)

                                    notice = "Naidagdag ang hugis $shape."

                                },

                                label = { Text(shape, fontSize = 11.sp) }

                            )

                        }

                    }

                }



                "Filters" -> {

                    Row(

                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),

                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        listOf("Original", "Grayscale", "Sepia", "Brightness", "Contrast", "Invert", "Vintage", "Neon", "Warm", "Cool").forEach { filter ->

                            FilterChip(

                                selected = activeFilter == filter,

                                onClick = {

                                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null

                                    if (layer != null) {

                                        val baseOrig = originalBitmapsMap[layer.name] ?: layer.bitmap.copy(Bitmap.Config.ARGB_8888, true)

                                        if (!originalBitmapsMap.containsKey(layer.name)) {

                                            originalBitmapsMap[layer.name] = baseOrig

                                        }

                                        snapshot()

                                        activeFilter = filter

                                        layer.bitmap = applyImageFilter(baseOrig, filter)

                                        canvasVersion++

                                        notice = "Filter: $filter"

                                    } else notice = "Pumili muna ng layer."

                                },

                                label = { Text(filter, fontSize = 11.sp) }

                            )

                        }

                    }

                }



                "Layers" -> {

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .heightIn(max = 120.dp)

                            .verticalScroll(rememberScrollState()),

                        verticalArrangement = Arrangement.spacedBy(4.dp)

                    ) {

                        layers.forEachIndexed { idx, l ->

                            val isSel = idx == activeLayer

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .background(if (isSel) Color(0xFF383840) else Color(0xFF1B1B1E), RoundedCornerShape(6.dp))

                                    .clickable { activeLayer = idx }

                                    .padding(horizontal = 6.dp, vertical = 2.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(

                                    if (l.visible) "👁️" else "🙈",

                                    fontSize = 12.sp,

                                    modifier = Modifier.clickable { l.visible = !l.visible; canvasVersion++ }.padding(2.dp)

                                )

                                Spacer(Modifier.width(4.dp))

                                Text(l.name, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))

                                if (idx < layers.lastIndex) {

                                    Text("▲", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.clickable {

                                        snapshot()

                                        val temp = layers[idx]

                                        layers[idx] = layers[idx + 1]

                                        layers[idx + 1] = temp

                                        activeLayer = idx + 1

                                        canvasVersion++

                                    }.padding(2.dp))

                                }

                                if (idx > 0) {

                                    Text("▼", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.clickable {

                                        snapshot()

                                        val temp = layers[idx]

                                        layers[idx] = layers[idx - 1]

                                        layers[idx - 1] = temp

                                        activeLayer = idx - 1

                                        canvasVersion++

                                    }.padding(2.dp))

                                }

                                Text("🗑", color = Color.Red, fontSize = 11.sp, modifier = Modifier.clickable {

                                    if (layers.size > 1) {

                                        snapshot()

                                        layers.removeAt(idx)

                                        activeLayer = layers.lastIndex

                                        canvasVersion++

                                    } else notice = "Kailangan ng kahit isang layer."

                                }.padding(2.dp))

                            }

                        }

                    }

                }



                else -> {

                    Text(

                        when (currentTool) {

                            "Text" -> "I-tap ang 'Add Text' button o papalitan ang text layer."

                            "Emoji" -> "Pumili ng emoji stamp na ilalagay sa sticker."

                            "Auto BG" -> "Mag-generate ng auto gradient o AI background."

                            "Online BG" -> "Mag-download ng HD online backgrounds."

                            "Camera" -> "Kumuha ng litrato mula sa camera."

                            "Gallery" -> "Pumili ng larawan sa gallery."

                            "Restore" -> "I-restore ang layer pabalik sa orihinal."

                            else -> "Pumili ng tool para mag-edit."

                        },

                        fontSize = 11.sp,

                        color = Color.LightGray

                    )

                }

            }

        }



        // Customizable 3-Page Tool Distribution & Swipe Pager

        val defaultToolOrder = remember {

            listOf(

                "Rotate 90°", "Flip", "Center", "Reset",

                "Duplicate", "Remove BG", "Outline", "Eraser",

                "Brush", "Emoji", "Auto BG", "Online BG",

                "Camera", "Gallery", "Restore", "Shapes",

                "Text", "Filters", "Layers"

            )

        }



        val prefs = remember { context.getSharedPreferences("mysticker_settings", Context.MODE_PRIVATE) }

        var isCustomizeMode by remember { mutableStateOf(false) }

        var draggedToolName by remember { mutableStateOf<String?>(null) }

        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        var lastPageSwitchTime by remember { mutableLongStateOf(0L) }



        val customToolOrder = remember {

            val savedOrder = prefs.getString("custom_tool_order", null)

            val list = mutableStateListOf<String>()

            if (!savedOrder.isNullOrEmpty()) {

                val items = savedOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                list.addAll(items)

                defaultToolOrder.forEach { tool ->

                    if (!list.contains(tool)) list.add(tool)

                }

            } else {

                list.addAll(defaultToolOrder)

            }

            list

        }



        val allToolsMap: Map<String, EditorToolSpec> = remember(activeLayer, layers.size, canvasVersion, currentTool) {

            mapOf(

                "Rotate 90°" to EditorToolSpec("Rotate 90°", "🔄") {

                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@EditorToolSpec

                    snapshot()

                    layer.rotation = (layer.rotation + 90f) % 360f

                    canvasVersion++

                },

                "Flip" to EditorToolSpec("Flip", "↔") {

                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@EditorToolSpec

                    snapshot()

                    layer.scale = -layer.scale

                    canvasVersion++

                },

                "Center" to EditorToolSpec("Center", "🎯") {

                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@EditorToolSpec

                    snapshot()

                    layer.offsetX = 0f

                    layer.offsetY = 0f

                    canvasVersion++

                },

                "Reset" to EditorToolSpec("Reset", "↺") {

                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@EditorToolSpec

                    snapshot()

                    layer.offsetX = 0f

                    layer.offsetY = 0f

                    layer.scale = 1f

                    layer.rotation = 0f

                    canvasVersion++

                },

                "Duplicate" to EditorToolSpec("Duplicate", "📋") {

                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@EditorToolSpec

                    addBitmap("${layer.name} Copy", layer.bitmap)

                    notice = "Na-duplicate ang layer."

                },

                "Remove BG" to EditorToolSpec("Remove BG", "✂️") { currentTool = "Remove BG" },

                "Outline" to EditorToolSpec("Outline", "🎨") { currentTool = "Outline" },

                "Eraser" to EditorToolSpec("Eraser", "🧹") { currentTool = "Eraser" },

                "Brush" to EditorToolSpec("Brush", "🖌️") { currentTool = "Brush" },

                "Emoji" to EditorToolSpec("Emoji", "⭐") { currentTool = "Emoji"; emojiDialog = true },

                "Auto BG" to EditorToolSpec("Auto BG", "🌌") {

                    currentTool = "Auto BG"

                    previewBgBitmap = generateFreeAutoBackgroundBitmap(1024, 1024)

                    backgroundGenDialog = true

                },

                "Online BG" to EditorToolSpec("Online BG", "🌐") { currentTool = "Online BG"; onlineBgDialog = true },

                "Camera" to EditorToolSpec("Camera", "📸") { currentTool = "Camera"; cameraLauncher.launch(null) },

                "Gallery" to EditorToolSpec("Gallery", "🖼️") { currentTool = "Gallery"; launcher.launch("image/*") },

                "Restore" to EditorToolSpec("Restore", "↩️") {

                    currentTool = "Restore"

                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null

                    if (layer != null) {

                        val orig = originalBitmapsMap[layer.name] ?: originalLayers.firstOrNull()

                        if (orig != null) {

                            snapshot()

                            layer.bitmap = orig.copy(Bitmap.Config.ARGB_8888, true)

                            canvasVersion++

                            notice = "Na-restore ang layer."

                        } else notice = "Walang nahanap na original copy."

                    } else notice = "Pumili muna ng layer."

                },

                "Shapes" to EditorToolSpec("Shapes", "◩") { currentTool = "Shapes" },

                "Text" to EditorToolSpec("Text", "T") { currentTool = "Text"; textDialog = true },

                "Filters" to EditorToolSpec("Filters", "✨") { currentTool = "Filters" },

                "Layers" to EditorToolSpec("Layers", "🥞") { currentTool = "Layers" }

            )

        }



        val toolPages: List<List<EditorToolSpec?>> = remember(customToolOrder.toList(), allToolsMap) {

            val specs = customToolOrder.map { allToolsMap[it] }

            val page1 = specs.take(12).let { list -> list + List((12 - list.size).coerceAtLeast(0)) { null } }

            val page2 = specs.drop(12).take(12).let { list -> list + List((12 - list.size).coerceAtLeast(0)) { null } }

            val page3 = specs.drop(24).take(12).let { list -> list + List((12 - list.size).coerceAtLeast(0)) { null } }

            listOf(page1, page2, page3)

        }



        val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })



        HorizontalPager(

            state = pagerState,

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 4.dp)

        ) { pageIndex ->

            val pageTools = toolPages.getOrElse(pageIndex) { List(12) { null } }



            Column(

                modifier = Modifier.fillMaxWidth(),

                verticalArrangement = Arrangement.spacedBy(6.dp)

            ) {

                for (rowIndex in 0..2) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        for (colIndex in 0..3) {

                            val toolIdx = rowIndex * 4 + colIndex

                            val globalIdx = pageIndex * 12 + toolIdx

                            val spec = pageTools.getOrNull(toolIdx)



                            if (spec != null) {

                                val isSelected = currentTool == spec.name

                                val isBeingDragged = spec.name == draggedToolName



                                Surface(

                                    onClick = {

                                        if (!isCustomizeMode) {

                                            spec.onClick()

                                        }

                                    },

                                    shape = RoundedCornerShape(10.dp),

                                    color = if (isBeingDragged) Color(0xFF33333E) else if (isSelected) editorRed else panelColor,

                                    border = if (isBeingDragged) BorderStroke(2.dp, editorRed) else if (isCustomizeMode) BorderStroke(1.dp, Color(0xFF4A4A52)) else if (isSelected) BorderStroke(1.dp, editorRed) else BorderStroke(1.dp, Color(0xFF383840)),

                                    modifier = Modifier

                                        .weight(1f)

                                        .height(42.dp)

                                        .graphicsLayer {

                                            if (isBeingDragged) {

                                                translationX = dragOffset.x

                                                translationY = dragOffset.y

                                                scaleX = 1.08f

                                                scaleY = 1.08f

                                                shadowElevation = 12f

                                            }

                                        }

                                        .pointerInput(spec.name, isCustomizeMode) {

                                            if (isCustomizeMode) {

                                                detectDragGesturesAfterLongPress(

                                                    onDragStart = {

                                                        draggedToolName = spec.name

                                                        dragOffset = Offset.Zero

                                                    },

                                                    onDrag = { change, dragAmount ->

                                                        change.consume()

                                                        dragOffset += dragAmount

                                                        val currentToolName = draggedToolName ?: return@detectDragGesturesAfterLongPress

                                                        val currentIdx = customToolOrder.indexOf(currentToolName)

                                                        if (currentIdx == -1) return@detectDragGesturesAfterLongPress



                                                        val itemWidthPx = (size.width.toFloat()).coerceAtLeast(1f)

                                                        val itemHeightPx = 42.dp.toPx()

                                                        val colShift = (dragOffset.x / itemWidthPx).toInt()

                                                        val rowShift = (dragOffset.y / itemHeightPx).toInt()

                                                        val targetIdx = (currentIdx + rowShift * 4 + colShift)

                                                            .coerceIn(0, customToolOrder.lastIndex)



                                                        if (targetIdx != currentIdx) {

                                                            val movedItem = customToolOrder.removeAt(currentIdx)

                                                            customToolOrder.add(targetIdx, movedItem)

                                                            dragOffset -= Offset(colShift * itemWidthPx, rowShift * itemHeightPx)

                                                        }



                                                        val targetPage = targetIdx / 12

                                                        val now = System.currentTimeMillis()

                                                        if (targetPage != pagerState.currentPage && now - lastPageSwitchTime > 350L) {

                                                            lastPageSwitchTime = now

                                                            scope.launch { pagerState.animateScrollToPage(targetPage) }

                                                        }

                                                    },

                                                    onDragEnd = {

                                                        draggedToolName = null

                                                        dragOffset = Offset.Zero

                                                    },

                                                    onDragCancel = {

                                                        draggedToolName = null

                                                        dragOffset = Offset.Zero

                                                    }

                                                )

                                            }

                                        }

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),

                                        verticalAlignment = Alignment.CenterVertically,

                                        horizontalArrangement = Arrangement.Center

                                    ) {

                                        Text(spec.icon, color = Color.White, fontSize = 13.sp)

                                        Spacer(Modifier.width(3.dp))

                                        Text(

                                            spec.name,

                                            color = Color.White,

                                            fontSize = 10.sp,

                                            fontWeight = if (isSelected || isBeingDragged) FontWeight.Bold else FontWeight.Normal,

                                            maxLines = 1,

                                            overflow = TextOverflow.Ellipsis

                                        )

                                    }

                                }

                            } else {

                                Spacer(modifier = Modifier.weight(1f))

                            }

                        }

                    }

                }

            }

        }



        // Page Indicator Dots & Customize Mode Controls

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(vertical = 4.dp),

            horizontalArrangement = Arrangement.Center,

            verticalAlignment = Alignment.CenterVertically

        ) {

            repeat(3) { pageIdx ->

                val isActive = pagerState.currentPage == pageIdx

                Box(

                    modifier = Modifier

                        .padding(horizontal = 4.dp)

                        .size(8.dp)

                        .clip(CircleShape)

                        .background(if (isActive) editorRed else Color(0xFF55555C))

                )

            }



            Spacer(Modifier.width(16.dp))



            if (!isCustomizeMode) {

                TextButton(

                    onClick = {

                        isCustomizeMode = true

                        notice = "⚙️ Tool Rearrangement Mode — Long press and drag any tool button!"

                    },

                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)

                ) {

                    Text("⚙️ Customize", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                }

            } else {

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                    Button(

                        onClick = {

                            isCustomizeMode = false

                            val savedStr = customToolOrder.joinToString(",")

                            prefs.edit().putString("custom_tool_order", savedStr).apply()

                            Toast.makeText(context, "Tool layout saved!", Toast.LENGTH_SHORT).show()

                            notice = "Tool layout saved!"

                        },

                        colors = ButtonDefaults.buttonColors(containerColor = editorRed),

                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                    ) {

                        Text("✅ Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    }



                    OutlinedButton(

                        onClick = {

                            customToolOrder.clear()

                            customToolOrder.addAll(defaultToolOrder)

                            prefs.edit().remove("custom_tool_order").apply()

                            Toast.makeText(context, "Tool layout reset to default.", Toast.LENGTH_SHORT).show()

                            notice = "Tool layout reset to default."

                        },

                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                    ) {

                        Text("↺ Reset", fontSize = 11.sp)

                    }

                }

            }

        }



        Text(notice, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)

    }



    if (textDialog) {

        AlertDialog(

            onDismissRequest = { textDialog = false },

            title = { Text("Add Text Layer") },

            text = {

                OutlinedTextField(

                    value = layerText,

                    onValueChange = { layerText = it },

                    label = { Text("Your text") },

                    singleLine = true

                )

            },

            confirmButton = {

                TextButton(onClick = {

                    if (layerText.isNotBlank()) {

                        val bitmap = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)

                        val canvas = Canvas(bitmap)

                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

                            color = palette[colorIndex]

                            textSize = 64f

                            textAlign = Paint.Align.CENTER

                            typeface = Typeface.DEFAULT_BOLD

                        }

                        canvas.drawText(layerText, 256f, 140f, paint)

                        addBitmap("Text: $layerText", bitmap)

                        currentTool = "Move"

                        layerText = ""

                    }

                    textDialog = false

                }) { Text("Add") }

            },

            dismissButton = { TextButton(onClick = { textDialog = false }) { Text("Cancel") } }

        )

    }



    if (emojiDialog) {

        AlertDialog(

            onDismissRequest = { emojiDialog = false },

            title = { Text("Select Emoji Stamp") },

            text = {

                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    listOf("⭐", "🔥", "💖", "🌸", "🚀", "🎉", "👑", "🐶", "🐱").forEach { emoji ->

                        Text(

                            emoji,

                            fontSize = 32.sp,

                            modifier = Modifier.clickable {

                                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)

                                val canvas = Canvas(bitmap)

                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

                                    textSize = 180f

                                    textAlign = Paint.Align.CENTER

                                }

                                canvas.drawText(emoji, 128f, 190f, paint)

                                addBitmap("Emoji $emoji", bitmap)

                                emojiDialog = false

                            }.padding(4.dp)

                        )

                    }

                }

            },

            confirmButton = { TextButton(onClick = { emojiDialog = false }) { Text("Close") } }

        )

    }



    if (backgroundGenDialog) {

        AlertDialog(

            onDismissRequest = { backgroundGenDialog = false },

            title = { Text("🌌 Auto Background Generator") },

            text = {

                Column(Modifier.fillMaxWidth()) {

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                        FilterChip(selected = bgDialogTab == "Free", onClick = { bgDialogTab = "Free" }, label = { Text("Free Auto BG") })

                        FilterChip(selected = bgDialogTab == "AI", onClick = { bgDialogTab = "AI" }, label = { Text("AI Prompt (Pollinations)") })

                    }

                    Spacer(Modifier.height(12.dp))



                    if (bgDialogTab == "Free") {

                        previewBgBitmap?.let { bitmap ->

                            Image(

                                bitmap = bitmap.asImageBitmap(),

                                contentDescription = "Auto BG Preview",

                                modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.DarkGray, RoundedCornerShape(8.dp))

                            )

                        }

                        Spacer(Modifier.height(8.dp))

                        Button(

                            onClick = { previewBgBitmap = generateFreeAutoBackgroundBitmap(1024, 1024) },

                            modifier = Modifier.fillMaxWidth()

                        ) { Text("🎲 Shuffle Gradient Pattern") }

                    } else {

                        OutlinedTextField(

                            value = aiPromptText,

                            onValueChange = { aiPromptText = it },

                            label = { Text("AI Background Prompt") },

                            modifier = Modifier.fillMaxWidth()

                        )

                        Spacer(Modifier.height(8.dp))

                        Button(

                            onClick = {

                                scope.launch {

                                    isAiGenerating = true

                                    val bitmap = fetchAiBackgroundBitmap(context, aiPromptText)

                                    if (bitmap != null) previewBgBitmap = bitmap

                                    else Toast.makeText(context, "Could not generate AI background.", Toast.LENGTH_SHORT).show()

                                    isAiGenerating = false

                                }

                            },

                            enabled = !isAiGenerating,

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            if (isAiGenerating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))

                            else Text("🤖 Generate AI Background")

                        }

                        previewBgBitmap?.let { bitmap ->

                            Spacer(Modifier.height(8.dp))

                            Image(

                                bitmap = bitmap.asImageBitmap(),

                                contentDescription = "AI BG Preview",

                                modifier = Modifier.fillMaxWidth().height(140.dp).background(Color.DarkGray, RoundedCornerShape(8.dp))

                            )

                        }

                    }

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        val bmp = previewBgBitmap

                        if (bmp != null) {

                            snapshot()

                            val bgLayer = StickerEditorLayer("Background", bmp.copy(Bitmap.Config.ARGB_8888, true))

                            layers.add(0, bgLayer)

                            activeLayer = 0

                            canvasVersion++

                            notice = "Inilagay ang background sa likod ng sticker!"

                        }

                        backgroundGenDialog = false

                    },

                    enabled = previewBgBitmap != null

                ) { Text("Apply Background") }

            },

            dismissButton = { TextButton(onClick = { backgroundGenDialog = false }) { Text("Cancel") } }

        )

    }



    if (onlineBgDialog) {

        AlertDialog(

            onDismissRequest = { onlineBgDialog = false },

            title = { Text("🖼️ Online Background Gallery") },

            text = {

                Column(modifier = Modifier.fillMaxWidth()) {

                    Text("Free Unsplash & Pixabay HD backgrounds", fontSize = 12.sp, color = Color.Gray)

                    Spacer(Modifier.height(8.dp))



                    OutlinedTextField(

                        value = onlineSearchQuery,

                        onValueChange = { onlineSearchQuery = it },

                        label = { Text("Search (e.g. nature, galaxy, sunset)") },

                        trailingIcon = { Text("🔍") },

                        modifier = Modifier.fillMaxWidth(),

                        singleLine = true,

                        shape = RoundedCornerShape(12.dp)

                    )



                    Spacer(Modifier.height(10.dp))



                    Row(

                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),

                        horizontalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        listOf("Aesthetic", "Nature", "Galaxy", "Sunset", "Patterns").forEach { cat ->

                            FilterChip(

                                selected = onlineSelectedCategory == cat && onlineSearchQuery.isBlank(),

                                onClick = {

                                    onlineSelectedCategory = cat

                                    onlineSearchQuery = ""

                                },

                                label = { Text(cat) }

                            )

                        }

                    }



                    Spacer(Modifier.height(12.dp))



                    if (isDownloadingOnlineBg) {

                        Box(

                            modifier = Modifier.fillMaxWidth().height(200.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                                CircularProgressIndicator(color = Color.White)

                                Spacer(Modifier.height(8.dp))

                                Text("Downloading HD Background…", fontSize = 12.sp, color = Color.White)

                            }

                        }

                    } else {

                        val currentItems = remember(onlineSearchQuery, onlineSelectedCategory) {

                            if (onlineSearchQuery.isNotBlank()) {

                                val query = onlineSearchQuery.trim()

                                val encoded = java.net.URLEncoder.encode(query, "UTF-8")

                                listOf(

                                    OnlineBgItem("$query Unsplash HD", "Unsplash", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1024&q=80"),

                                    OnlineBgItem("$query Pixabay HD", "Pixabay", "https://image.pollinations.ai/prompt/hd%20background%20$encoded?width=1024&height=1024&nologo=true")

                                )

                            } else {

                                curatedOnlineBackgrounds[onlineSelectedCategory] ?: emptyList()

                            }

                        }



                        Column(

                            modifier = Modifier.fillMaxWidth().height(260.dp).verticalScroll(rememberScrollState()),

                            verticalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            currentItems.forEach { bgItem ->

                                Card(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .clickable {

                                            scope.launch {

                                                isDownloadingOnlineBg = true

                                                val bitmap = fetchOnlineImageBitmap(context, bgItem.url)

                                                if (bitmap != null) {

                                                    snapshot()

                                                    val bgLayer = StickerEditorLayer("Online BG ${layers.size + 1}", bitmap.copy(Bitmap.Config.ARGB_8888, true))

                                                    layers.add(0, bgLayer)

                                                    activeLayer = 0

                                                    canvasVersion++

                                                    notice = "Inilagay ang ${bgItem.title} sa likod ng sticker!"

                                                    onlineBgDialog = false

                                                } else {

                                                    Toast.makeText(context, "Could not download background image.", Toast.LENGTH_SHORT).show()

                                                }

                                                isDownloadingOnlineBg = false

                                            }

                                        },

                                    shape = RoundedCornerShape(12.dp),

                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222226))

                                ) {

                                    Row(

                                        modifier = Modifier.padding(10.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Box(

                                            modifier = Modifier

                                                .size(54.dp)

                                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),

                                            contentAlignment = Alignment.Center

                                        ) {

                                            Text("🖼️", fontSize = 24.sp)

                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {

                                            Text(bgItem.title, style = MaterialTheme.typography.titleSmall, color = Color.White)

                                            Text("Source: ${bgItem.source}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                                        }

                                        Text("＋ Apply", fontSize = 12.sp, color = editorRed, fontWeight = FontWeight.Bold)

                                    }

                                }

                            }

                        }

                    }

                }

            },

            confirmButton = {

                TextButton(onClick = { onlineBgDialog = false }) { Text("Close") }

            }

        )

    }

}



@Composable

fun CreateStickerScreen(

    autoSave: Boolean,

    screenTitle: String = "My Sticker",

    onStickerSaved: () -> Unit = {},

    onBack: () -> Unit = {}

) {

    val context = LocalContext.current



    var selectedBitmap by remember {

        mutableStateOf<Bitmap?>(null)

    }



    var selectedSizePx by remember {

        mutableStateOf<Int?>(512)

    }



    var message by remember {

        mutableStateOf("Choose a picture from your Gallery.")

    }



    val galleryLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        if (uri != null) {

            val bitmap = try {

                context.contentResolver.openInputStream(uri)?.use {

                    BitmapFactory.decodeStream(it)

                }

            } catch (e: Exception) {

                null

            }



            selectedBitmap = bitmap



            if (bitmap == null) {

                message = "Unable to open this picture."

            } else if (autoSave) {

                val saved = saveMyStickerBitmap(context, bitmap, selectedSizePx)



                if (saved) {

                    onStickerSaved()

                    val sizeLabel = if (selectedSizePx != null) "${selectedSizePx}dp" else "Original"

                    message = "Picture selected and saved automatically ($sizeLabel)!"

                    Toast.makeText(

                        context,

                        "Sticker saved ($sizeLabel) successfully!",

                        Toast.LENGTH_SHORT

                    ).show()

                } else {

                    message = "Unable to save. Please try again."

                }

            } else {

                message = "Picture ready! Tap Save Sticker to save it."

            }

        }

    }



    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(bottom = 20.dp),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text(

            if (screenTitle == "AI Picture-to-Sticker") {

                "🤖 AI Picture-to-Sticker"

            } else {

                "🎨 Create Sticker"

            },

            fontSize = 26.sp

        )



        Spacer(Modifier.height(8.dp))



        Text(

            "Choose a picture, preview it, and save it as PNG.",

            fontSize = 14.sp

        )



        Spacer(Modifier.height(20.dp))



        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(280.dp)

                .background(Color(0xFFF2F2F2))

                .border(1.dp, Color.LightGray),

            contentAlignment = Alignment.Center

        ) {

            val bitmap = selectedBitmap



            if (bitmap != null) {

                Image(

                    bitmap = bitmap.asImageBitmap(),

                    contentDescription = "Sticker preview",

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(12.dp)

                )

            } else {

                Text(

                    "🖼️\n\nYour picture preview will appear here",

                    color = Color.DarkGray

                )

            }

        }



        Spacer(Modifier.height(16.dp))



        StickerSizeChipSelector(

            selectedSizePx = selectedSizePx,

            onSizeSelected = { selectedSizePx = it }

        )



        Spacer(Modifier.height(12.dp))



        Button(

            onClick = {

                galleryLauncher.launch("image/*")

            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("🖼️ Choose Picture from Gallery")

        }



        Spacer(Modifier.height(10.dp))



        Button(

            onClick = {

                val bitmap = selectedBitmap



                if (bitmap != null) {

                    val saved = saveMyStickerBitmap(context, bitmap, selectedSizePx)



                    if (saved) {

                        onStickerSaved()

                        val sizeLabel = if (selectedSizePx != null) "${selectedSizePx}dp" else "Original"

                        message = "Sticker saved successfully ($sizeLabel)!"



                        Toast.makeText(

                            context,

                            "Sticker saved ($sizeLabel) to My Stickers!",

                            Toast.LENGTH_SHORT

                        ).show()

                    } else {

                        message = "Unable to save sticker."

                    }

                }

            },

            enabled = selectedBitmap != null,

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("💾 Save Sticker as PNG")

        }



        Spacer(Modifier.height(12.dp))



        Text(

            if (autoSave) {

                "🟢 Auto Save is ON"

            } else {

                "⚪ Auto Save is OFF · Manual save is available"

            },

            fontSize = 14.sp

        )



        Spacer(Modifier.height(8.dp))

        Text(message, fontSize = 14.sp)



        Spacer(Modifier.height(16.dp))



        AppButton("← Back to Create") {

            onBack()

        }

    }

}



// =====================================

// TEXT-TO-STICKER SCREEN

// =====================================



@Composable
fun TextStickerScreen(
    autoSave: Boolean,
    onStickerSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stickerText by remember { mutableStateOf("Astronaut in a jungle, cold color palette, muted colors, detailed, 8k") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedSizePx by remember { mutableStateOf<Int?>(512) }
    var isGenerating by remember { mutableStateOf(false) }
    var removeBgToggle by remember { mutableStateOf(true) }

    val presetPrompts = listOf(
        "Astronaut in a jungle, cold color palette, 8k",
        "Cute chibi anime girl with cat ears, kawaii sticker",
        "Cyberpunk cat with neon sunglasses, sticker art",
        "Cute puppy floating in space with stars",
        "Pastel fairy with magical sparkling wings"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✨ AI Prompt & Text Sticker", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Type any Stable Diffusion prompt to generate custom AI stickers!", fontSize = 12.sp, color = Color.Gray)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = stickerText,
            onValueChange = { stickerText = it },
            label = { Text("AI Prompt or Sticker Text") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetPrompts.forEach { prompt ->
                FilterChip(
                    selected = stickerText == prompt,
                    onClick = { stickerText = prompt },
                    label = { Text(prompt.take(28) + "…", fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏁 Remove Background", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = removeBgToggle,
                onCheckedChange = { removeBgToggle = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (stickerText.isBlank()) {
                    Toast.makeText(context, "Type your prompt or text first.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    isGenerating = true
                    Toast.makeText(context, "Generating Stable Diffusion sticker...", Toast.LENGTH_SHORT).show()
                    val generatedBmp = fetchAiBackgroundBitmap(context, stickerText)
                    val baseBmp = generatedBmp ?: run {
                        val fallback = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(fallback)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.RED
                            textSize = 48f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        canvas.drawText(stickerText.take(20), 256f, 140f, paint)
                        fallback
                    }

                    val finalBmp = if (removeBgToggle) {
                        performAutoBackgroundRemoval(context, baseBmp) ?: baseBmp
                    } else baseBmp

                    previewBitmap = finalBmp
                    isGenerating = false

                    if (autoSave) {
                        saveMyStickerBitmap(context, finalBmp, selectedSizePx ?: 512)
                        onStickerSaved()
                        Toast.makeText(context, "AI Sticker saved automatically!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isGenerating && stickerText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generating AI Sticker...")
            } else {
                Text("🤖 Generate Stable Diffusion AI Sticker")
            }
        }

        Spacer(Modifier.height(16.dp))

        previewBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Preview:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.size(180.dp).background(Color(0xFF131722), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkerboard(Modifier.matchParentSize())
                        DisplayResizedImage(originalBitmap = bitmap)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (saveMyStickerBitmap(context, bitmap, selectedSizePx ?: 512)) {
                                onStickerSaved()
                                Toast.makeText(context, "Saved to My Stickers!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("💾 Save Sticker to My Stickers")
                    }
                }
            }
        }
    }
}

fun createTextStickerBitmap(text: String): Bitmap {

    val bitmap = Bitmap.createBitmap(

        512,

        256,

        Bitmap.Config.ARGB_8888

    )



    val canvas = Canvas(bitmap)



    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

        textAlign = Paint.Align.CENTER

        typeface = Typeface.create(

            "sans-serif-black",

            Typeface.BOLD

        )

        textSize = (180f / text.length.coerceAtLeast(1))

            .coerceIn(28f, 100f)

        strokeJoin = Paint.Join.ROUND

    }



    val x = bitmap.width / 2f

    val y = bitmap.height / 2f + paint.textSize / 3f



    paint.style = Paint.Style.STROKE

    paint.strokeWidth = 12f

    paint.color = AndroidColor.WHITE

    canvas.drawText(text, x, y, paint)



    paint.style = Paint.Style.FILL

    paint.color = AndroidColor.RED

    canvas.drawText(text, x, y, paint)



    return bitmap

}



data class StickerSizeOption(

    val label: String,

    val sizePx: Int?,

    val description: String

)



val defaultStickerSizeOptions = listOf(

    StickerSizeOption("60 dp", 60, "Mini Sticker (60x60 dp)"),

    StickerSizeOption("128 dp", 128, "Small Sticker (128x128 dp)"),

    StickerSizeOption("152 dp", 152, "Medium Sticker (152x152 dp)"),

    StickerSizeOption("256 dp", 256, "Large Sticker (256x256 dp)"),

    StickerSizeOption("512 dp", 512, "WhatsApp HD Standard (512x512 dp)"),

    StickerSizeOption("Original", null, "Keep Original Size")

)



@Composable

fun StickerSizeChipSelector(

    selectedSizePx: Int?,

    onSizeSelected: (Int?) -> Unit,

    modifier: Modifier = Modifier

) {

    Column(modifier = modifier.fillMaxWidth()) {

        Text(

            "📐 Select Output Size (Laki ng Sticker):",

            fontSize = 13.sp,

            fontWeight = FontWeight.SemiBold

        )

        Spacer(Modifier.height(6.dp))

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .horizontalScroll(rememberScrollState()),

            horizontalArrangement = Arrangement.spacedBy(6.dp)

        ) {

            listOf(

                "60 dp" to 60,

                "128 dp" to 128,

                "152 dp" to 152,

                "256 dp" to 256,

                "512 dp" to 512,

                "Original" to null

            ).forEach { (label, size) ->

                FilterChip(

                    selected = selectedSizePx == size,

                    onClick = { onSizeSelected(size) },

                    label = {

                        Text(

                            label,

                            fontSize = 12.sp,

                            fontWeight = if (selectedSizePx == size) FontWeight.Bold else FontWeight.Normal

                        )

                    }

                )

            }

        }

    }

}



@Composable

fun StickerSizeSelectionDialog(

    actionTitle: String = "Select Sticker Size (Pumili ng Laki)",

    onDismiss: () -> Unit,

    onSizeSelected: (Int?) -> Unit

) {

    var selectedOption by remember { mutableStateOf<Int?>(512) }



    AlertDialog(

        onDismissRequest = onDismiss,

        title = { Text(actionTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold) },

        text = {

            Column(

                modifier = Modifier.fillMaxWidth(),

                verticalArrangement = Arrangement.spacedBy(6.dp)

            ) {

                Text("Pumili ng laki ng image bago i-save o i-share sa device/WhatsApp:", fontSize = 13.sp, color = Color.Gray)

                Spacer(Modifier.height(4.dp))



                defaultStickerSizeOptions.forEach { option ->

                    Card(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clickable { selectedOption = option.sizePx },

                        shape = RoundedCornerShape(12.dp),

                        colors = CardDefaults.cardColors(

                            containerColor = if (selectedOption == option.sizePx) Color(0xFFFFF0F2) else Color(0xFFF6F6F8)

                        ),

                        border = if (selectedOption == option.sizePx) BorderStroke(2.dp, Color.Red) else null

                    ) {

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(horizontal = 14.dp, vertical = 10.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            RadioButton(

                                selected = selectedOption == option.sizePx,

                                onClick = { selectedOption = option.sizePx }

                            )

                            Spacer(Modifier.width(8.dp))

                            Column {

                                Text(

                                    option.label,

                                    fontSize = 15.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = if (selectedOption == option.sizePx) Color.Red else Color.Unspecified

                                )

                                Text(option.description, fontSize = 12.sp, color = Color.Gray)

                            }

                        }

                    }

                }

            }

        },

        confirmButton = {

            Button(

                onClick = {

                    onSizeSelected(selectedOption)

                },

                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)

            ) {

                Text("Confirm & Continue", color = Color.White)

            }

        },

        dismissButton = {

            TextButton(onClick = onDismiss) {

                Text("Cancel")

            }

        }

    )

}



fun shareStickerFile(context: Context, file: File) {

    try {

        val uri: Uri = FileProvider.getUriForFile(

            context,

            "${context.packageName}.fileprovider",

            file

        )

        val intent = Intent(Intent.ACTION_SEND).apply {

            type = "image/png"

            putExtra(Intent.EXTRA_STREAM, uri)

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        }

        context.startActivity(Intent.createChooser(intent, "Share Sticker"))

    } catch (e: Exception) {

        Toast.makeText(context, "Could not share sticker: ${e.message}", Toast.LENGTH_SHORT).show()

    }

}



fun createHighQualityScaledBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {

    if (targetWidth <= 0 || targetHeight <= 0) return src

    if (src.width == targetWidth && src.height == targetHeight) return src



    val scaled = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(scaled)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    val srcRect = android.graphics.Rect(0, 0, src.width, src.height)

    val destRect = android.graphics.RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())

    canvas.drawBitmap(src, srcRect, destRect, paint)

    return scaled

}



fun Bitmap.resizeTo(targetWidth: Int, targetHeight: Int): Bitmap {

    return createHighQualityScaledBitmap(this, targetWidth, targetHeight)

}



fun Bitmap.resizeTo(targetSize: Int): Bitmap {

    return createHighQualityScaledBitmap(this, targetSize, targetSize)

}



fun Bitmap.resizeTo60(): Bitmap {

    return createHighQualityScaledBitmap(this, 60, 60)

}



@Composable
fun DisplayResizedImage(
    originalBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    Image(
        bitmap = originalBitmap.asImageBitmap(),
        contentDescription = "Sticker",
        modifier = modifier.size(60.dp),
        contentScale = ContentScale.Fit
    )
}



fun shareStickerToWhatsApp(context: Context, file: File, targetSizePx: Int? = null) {

    try {

        val targetFile = if (targetSizePx != null && targetSizePx > 0) {

            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            if (bitmap != null) {

                val resized = createHighQualityScaledBitmap(bitmap, targetSizePx, targetSizePx)

                val cacheDir = File(context.cacheDir, "resized_whatsapp").apply { mkdirs() }

                val tempFile = File(cacheDir, "${file.nameWithoutExtension}_${targetSizePx}dp.png")

                FileOutputStream(tempFile).use { output ->

                    resized.compress(Bitmap.CompressFormat.PNG, 100, output)

                }

                tempFile

            } else file

        } else file



        val uri: Uri = FileProvider.getUriForFile(

            context,

            "${context.packageName}.fileprovider",

            targetFile

        )

        val intent = Intent(Intent.ACTION_SEND).apply {

            type = "image/png"

            putExtra(Intent.EXTRA_STREAM, uri)

            setPackage("com.whatsapp")

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        }

        if (intent.resolveActivity(context.packageManager) != null) {

            context.startActivity(intent)

        } else {

            val generalIntent = Intent(Intent.ACTION_SEND).apply {

                type = "image/png"

                putExtra(Intent.EXTRA_STREAM, uri)

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            }

            context.startActivity(Intent.createChooser(generalIntent, "Share Sticker to WhatsApp"))

        }

    } catch (e: Exception) {

        Toast.makeText(context, "Could not share to WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()

    }

}



fun saveStickerToDeviceGallery(context: Context, file: File, targetSizePx: Int? = null): Boolean {

    return try {

        var bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false

        if (targetSizePx != null && targetSizePx > 0) {

            bitmap = createHighQualityScaledBitmap(bitmap, targetSizePx, targetSizePx)

        }

        val fileName = if (targetSizePx != null && targetSizePx > 0) {

            "${file.nameWithoutExtension}_${targetSizePx}dp.png"

        } else {

            file.name

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues().apply {

                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)

                put(MediaStore.Images.Media.MIME_TYPE, "image/png")

                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyStickerApp")

                put(MediaStore.Images.Media.IS_PENDING, 1)

            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val itemUri = context.contentResolver.insert(collection, values)

            if (itemUri != null) {

                context.contentResolver.openOutputStream(itemUri)?.use { output ->

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)

                }

                values.clear()

                values.put(MediaStore.Images.Media.IS_PENDING, 0)

                context.contentResolver.update(itemUri, values, null, null)

                return true

            }

            false

        } else {

            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

            val appDir = File(picturesDir, "MyStickerApp").apply { mkdirs() }

            val destFile = File(appDir, fileName)

            FileOutputStream(destFile).use { output ->

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)

            }

            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)

            mediaScanIntent.data = Uri.fromFile(destFile)

            context.sendBroadcast(mediaScanIntent)

            true

        }

    } catch (e: Exception) {

        e.printStackTrace()

        false

    }

}



fun saveTempCutoutSticker(context: Context, bitmap: Bitmap): File? {

    return try {

        val folder = File(context.filesDir, "MyStickers")

        if (!folder.exists() && !folder.mkdirs()) return null

        val file = File(folder, "cutout_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use { output ->

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)

        }

        file

    } catch (e: Exception) {

        null

    }

}



fun saveMyStickerBitmap(

    context: Context,

    bitmap: Bitmap,

    targetSizePx: Int? = null

): Boolean {

    return try {

        val folder = File(context.filesDir, "MyStickers")



        if (!folder.exists() && !folder.mkdirs()) {

            return false

        }



        val resizedBitmap = if (targetSizePx != null && targetSizePx > 0 &&

            (bitmap.width != targetSizePx || bitmap.height != targetSizePx)) {

            createHighQualityScaledBitmap(bitmap, targetSizePx, targetSizePx)

        } else {

            bitmap

        }



        val file = File(

            folder,

            "sticker_${System.currentTimeMillis()}.png"

        )



        FileOutputStream(file).use { output ->

            resizedBitmap.compress(

                Bitmap.CompressFormat.PNG,

                100,

                output

            )

        }

        true

    } catch (e: Exception) {

        e.printStackTrace()

        false

    }

}



fun loadMyStickerFiles(context: Context): List<File> {

    val folder = File(context.filesDir, "MyStickers")



    return folder.listFiles()

        ?.filter { it.isFile && it.extension == "png" }

        ?.sortedByDescending { it.lastModified() }

        ?: emptyList()

}



fun renameStickerFile(file: File, requestedName: String): Boolean {

    return try {

        val folder = File(file.parentFile ?: return false, "")

        val canonicalFolder = folder.canonicalFile

        val canonicalSource = file.canonicalFile

        if (canonicalSource.parentFile != canonicalFolder ||

            !canonicalSource.isFile ||

            !canonicalSource.extension.equals("png", ignoreCase = true)

        ) return false



        val withoutExtension = requestedName.trim().let {

            if (it.endsWith(".png", ignoreCase = true)) it.dropLast(4) else it

        }

        val safeName = withoutExtension

            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")

            .trim()

            .trim('.')

        if (safeName.isBlank()) return false



        val newFile = File(canonicalFolder, "$safeName.png")

        if (newFile.canonicalFile == canonicalSource) return true

        if (newFile.exists()) return false



        canonicalSource.renameTo(newFile)

    } catch (_: Exception) {

        false

    }

}



fun deleteStickerFile(file: File): Boolean {

    return try {

        file.exists() && file.isFile && file.delete()

    } catch (_: Exception) {

        false

    }

}



fun deleteAllStickerFiles(context: Context): Boolean {

    return try {

        val folder = File(context.filesDir, "MyStickers")

        if (folder.exists() && folder.isDirectory) {

            val files = folder.listFiles()?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }

            if (!files.isNullOrEmpty()) {

                files.forEach { it.delete() }

                true

            } else false

        } else false

    } catch (_: Exception) {

        false

    }

}



data class CreatorModel(

    val id: String,

    val name: String,

    val handle: String,

    val avatar: String,

    val bio: String,

    val stickersCount: Int,

    val sampleStickers: List<String>,

    val verified: Boolean = true

)



@Composable

fun CreatorsScreen(

    context: Context,

    prefs: android.content.SharedPreferences,

    onBack: () -> Unit

) {

    var searchQuery by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf("Featured") }



    var followedIds by remember {

        mutableStateOf(prefs.getStringSet("followed_creators", setOf("c1")) ?: setOf("c1"))

    }



    val sampleCreators = remember {

        listOf(

            CreatorModel(

                id = "c1",

                name = "Mimo Studio",

                handle = "@mimo_official",

                avatar = "🎨",

                bio = "Official MySticker creator. Premium HD stickers & memes!",

                stickersCount = 120,

                sampleStickers = listOf("🐱", "🚀", "🔥", "✨")

            ),

            CreatorModel(

                id = "c2",

                name = "Pixel Master",

                handle = "@pixel_art",

                avatar = "👾",

                bio = "Retro 8-bit & pixel art sticker collections.",

                stickersCount = 85,

                sampleStickers = listOf("🎮", "👾", "🕹️", "⚡")

            ),

            CreatorModel(

                id = "c3",

                name = "Doodle Queen",

                handle = "@doodle_q",

                avatar = "🌸",

                bio = "Hand-drawn cute animals, kawaii expressions & flowers.",

                stickersCount = 64,

                sampleStickers = listOf("🌸", "🐰", "💖", "🍓")

            )

        )

    }



    val filteredCreators = remember(searchQuery, selectedTab, followedIds) {

        sampleCreators.filter { creator ->

            val matchesQuery = searchQuery.isBlank() ||

                    creator.name.contains(searchQuery, ignoreCase = true) ||

                    creator.handle.contains(searchQuery, ignoreCase = true) ||

                    creator.bio.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {

                "Following" -> followedIds.contains(creator.id)

                else -> true

            }

            matchesQuery && matchesTab

        }

    }



    Column(modifier = Modifier.fillMaxWidth()) {

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

                        .padding(vertical = 6.dp),

                    shape = RoundedCornerShape(16.dp),

                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))

                ) {

                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Box(

                                modifier = Modifier

                                    .size(50.dp)

                                    .background(Color(0xFFFFF2F5), CircleShape)

                                    .border(2.dp, Color.Red, CircleShape),

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

                                Text(creator.handle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

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

                                    containerColor = if (isFollowing) Color.DarkGray else Color.Red

                                ),

                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)

                            ) {

                                Text(if (isFollowing) "Following ✓" else "＋ Follow", fontSize = 12.sp)

                            }

                        }



                        Spacer(Modifier.height(8.dp))

                        Text(creator.bio, style = MaterialTheme.typography.bodySmall)



                        Spacer(Modifier.height(12.dp))



                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            creator.sampleStickers.forEach { stickerEmoji ->

                                Card(

                                    modifier = Modifier

                                        .weight(1f)

                                        .height(60.dp),

                                    colors = CardDefaults.cardColors(containerColor = Color.White),

                                    shape = RoundedCornerShape(12.dp)

                                ) {

                                    Box(

                                        modifier = Modifier.fillMaxSize(),

                                        contentAlignment = Alignment.Center

                                    ) {

                                        Text(stickerEmoji, fontSize = 28.sp)

                                    }

                                }

                            }

                        }

                    }

                }

            }

        }



        Spacer(Modifier.height(20.dp))

    }

}



@Composable

fun MyScreen(

    context: Context,

    prefs: android.content.SharedPreferences,

    displayName: String,

    email: String,

    onEditAccount: () -> Unit,

    onMyStickers: () -> Unit,

    onFavorites: () -> Unit,

    onMyCreations: () -> Unit,

    onSettings: () -> Unit

) {

    val myStickersCount = remember { loadMyStickerFiles(context).size }

    val followingCount = remember { prefs.getStringSet("followed_creators", setOf("c1"))?.size ?: 1 }



    Column(

        modifier = Modifier.fillMaxWidth(),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text("👤 My Screen & Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(16.dp))



        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F5))

        ) {

            Column(

                modifier = Modifier.fillMaxWidth().padding(20.dp),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {

                Box(

                    modifier = Modifier

                        .size(80.dp)

                        .background(Color.White, CircleShape)

                        .border(3.dp, Color.Red, CircleShape),

                    contentAlignment = Alignment.Center

                ) {

                    Text("🎨", fontSize = 42.sp)

                }



                Spacer(Modifier.height(10.dp))

                Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)



                if (email.isNotBlank()) {

                    Text(email, fontSize = 14.sp, color = Color.Gray)

                } else {

                    Text("Creator Bio: Sticker lover & creator ✨", fontSize = 13.sp, color = Color.Gray)

                }



                Spacer(Modifier.height(14.dp))



                OutlinedButton(onClick = onEditAccount) {

                    Text("✏️ Edit Account Profile")

                }

            }

        }



        Spacer(Modifier.height(20.dp))



        Card(

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(16.dp),

            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))

        ) {

            Row(

                modifier = Modifier.fillMaxWidth().padding(16.dp),

                horizontalArrangement = Arrangement.SpaceEvenly

            ) {

                ProfileStat(myStickersCount.toString(), "Stickers")

                ProfileStat(followingCount.toString(), "Following")

            }

        }



        Spacer(Modifier.height(22.dp))



        Text(

            "My Collections & Shortcuts",

            fontSize = 20.sp,

            fontWeight = FontWeight.Bold,

            modifier = Modifier.align(Alignment.Start)

        )

        Spacer(Modifier.height(10.dp))



        AppButton("🖼️ My Stickers Gallery ($myStickersCount)", onMyStickers)

        AppButton("❤️ Favorite Stickers", onFavorites)

        AppButton("🎨 My Creations & Edits", onMyCreations)

        AppButton("⚙️ App Settings & Preferences", onSettings)

    }

}



@Composable

fun ProfileStat(count: String, label: String) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(count, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Text(label, fontSize = 13.sp, color = Color.Gray)

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

    Column(

        modifier = Modifier.fillMaxWidth(),

        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Text("✏️ Edit Account Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)

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

    onLogoutClick: () -> Unit,

    onAboutClick: () -> Unit

) {

    Column(modifier = Modifier.fillMaxWidth()) {

        Text("⚙️ Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))



        SettingRow("Account", onClick = onAccountClick)

        SettingSwitchRow("Auto Save Stickers", checked = autoSave, onCheckedChange = onAutoSaveChange)

        SettingRow("Invite Friends", onClick = onInviteClick)

        SettingRow("Connect", onClick = onConnectClick)

        SettingSwitchRow("Dark Mode", checked = darkMode, onCheckedChange = onDarkModeChange)

        SettingSwitchRow("Push Notifications", checked = notifications, onCheckedChange = onNotificationsChange)

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



@Composable

fun AppButton(

    text: String,

    onClick: () -> Unit

) {

    Button(

        onClick = onClick,

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 6.dp)

    ) {

        Text(text)

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