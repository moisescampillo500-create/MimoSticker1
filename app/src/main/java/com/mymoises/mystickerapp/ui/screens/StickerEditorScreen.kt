package com.mymoises.mystickerapp.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.AdMobRewardedAdManager
import com.mymoises.mystickerapp.FontRegistry
import com.mymoises.mystickerapp.RewardedAdOfferDialog
import com.mymoises.mystickerapp.RewardedAdOfferSpec
import com.mymoises.mystickerapp.UserStickerTemplate
import com.mymoises.mystickerapp.UserStickerTemplatesManager
import com.mymoises.mystickerapp.UserTemplateLayerData
import com.mymoises.mystickerapp.addStickerOutline
import com.mymoises.mystickerapp.model.EditorToolSpec
import com.mymoises.mystickerapp.model.OnlineBgItem
import com.mymoises.mystickerapp.model.StickerEditorHistoryLayer
import com.mymoises.mystickerapp.model.StickerEditorLayer
import com.mymoises.mystickerapp.performAutoBackgroundRemoval
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.ui.dialogs.BrushCustomColorPickerDialog
import com.mymoises.mystickerapp.ui.dialogs.FloatingLayersPanelWindow
import com.mymoises.mystickerapp.ui.dialogs.FontSelectionPanelDialog
import com.mymoises.mystickerapp.ui.dialogs.SpeechBubbleEditorDialog
import com.mymoises.mystickerapp.ui.dialogs.TextEditorDialog
import com.mymoises.mystickerapp.util.applyImageFilter
import com.mymoises.mystickerapp.util.createShapeBitmap
import com.mymoises.mystickerapp.util.createSpeechBubbleBitmap
import com.mymoises.mystickerapp.util.createTextStickerBitmap
import com.mymoises.mystickerapp.util.curatedOnlineBackgrounds
import com.mymoises.mystickerapp.util.drawStrokeOnBitmap
import com.mymoises.mystickerapp.util.fetchAiBackgroundBitmap
import com.mymoises.mystickerapp.util.fetchOnlineImageBitmap
import com.mymoises.mystickerapp.util.findLayerAtTouch
import com.mymoises.mystickerapp.util.generateFreeAutoBackgroundBitmap
import com.mymoises.mystickerapp.util.mergeSelectedLayers
import com.mymoises.mystickerapp.util.saveBitmapToDeviceGallery
import com.mymoises.mystickerapp.util.saveMyStickerBitmap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun StickerEditorScreen(
    initialStickerFile: File? = null,
    initialUserTemplate: UserStickerTemplate? = null,
    creatorName: String = "My Creator",
    onStickerSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mysticker_settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val layers = remember { mutableStateListOf<StickerEditorLayer>() }
    var currentTool by remember { mutableStateOf("Move") }
    var textDialog by remember { mutableStateOf(false) }
    var editingTextLayer by remember { mutableStateOf<StickerEditorLayer?>(null) }
    var textFontFamily by remember { mutableStateOf("Sans Serif") }
    var textFontStyle by remember { mutableStateOf("Bold") }
    var textAlignName by remember { mutableStateOf("Center") }
    var textFontSizeSp by remember { mutableFloatStateOf(48f) }
    var textColorIndex by remember { mutableIntStateOf(0) }
    var textOutlineColorIndex by remember { mutableIntStateOf(2) }
    var textOutlineThicknessPx by remember { mutableFloatStateOf(10f) }
    var textLetterSpacingEm by remember { mutableFloatStateOf(0f) }
    var hasTextShadow by remember { mutableStateOf(true) }
    var speechBubbleDialog by remember { mutableStateOf(false) }
    var editingSpeechBubbleLayer by remember { mutableStateOf<StickerEditorLayer?>(null) }
    var bubbleFontFamily by remember { mutableStateOf("Sans Serif") }
    var bubbleFontStyle by remember { mutableStateOf("Bold") }
    var bubbleTextAlignName by remember { mutableStateOf("Center") }
    var fontPickerModalDialog by remember { mutableStateOf(false) }
    var selectedBubbleStyle by remember { mutableStateOf("Classic Speech Bubble") }
    var bubbleText by remember { mutableStateOf("Hello!") }
    var bubbleTextColorIndex by remember { mutableIntStateOf(1) }
    var bubbleFillColorIndex by remember { mutableIntStateOf(2) }
    var bubbleStrokeColorIndex by remember { mutableIntStateOf(1) }
    var bubbleTextSizeSp by remember { mutableFloatStateOf(32f) }
    var bubbleOutlineThicknessSp by remember { mutableFloatStateOf(10f) }
    var bubbleOpacityAlpha by remember { mutableFloatStateOf(1.0f) }
    var bubbleTailDirection by remember { mutableStateOf("Bottom Left") }
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
    var outlineThickness by remember { mutableFloatStateOf(16f) }
    var brushSize by remember { mutableFloatStateOf(18f) }
    var brushColorInt by remember { mutableIntStateOf(AndroidColor.RED) }
    var showBrushColorPickerDialog by remember { mutableStateOf(false) }

    var selectedShape by remember { mutableStateOf("Rectangle") }
    var shapeStrokeColorInt by remember { mutableIntStateOf(AndroidColor.BLACK) }
    var shapeStrokeWidthPx by remember { mutableFloatStateOf(16f) }
    var isShapeFilled by remember { mutableStateOf(true) }
    var shapeFillColorInt by remember { mutableIntStateOf(AndroidColor.RED) }
    var showShapeOutlineColorPickerDialog by remember { mutableStateOf(false) }
    var showShapeFillColorPickerDialog by remember { mutableStateOf(false) }

    var canvasVersion by remember { mutableIntStateOf(0) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var rewardedAdOfferSpec by remember { mutableStateOf<RewardedAdOfferSpec?>(null) }
    var templateNameInput by remember { mutableStateOf("My Template") }
    var selectedCategoryInput by remember { mutableStateOf("Cute") }
    var isPublishedInput by remember { mutableStateOf(true) }
    var isRemovingBg by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("Original") }
    var floatingPanelOffsetX by remember { mutableFloatStateOf(160f) }
    var floatingPanelOffsetY by remember { mutableFloatStateOf(20f) }
    var isPanelMinimized by remember { mutableStateOf(false) }
    var isPanelHidden by remember { mutableStateOf(true) }
    var layerSettingsTargetIndex by remember { mutableStateOf<Int?>(null) }
    var showRightLayerPanel by remember { mutableStateOf(false) }
    val selectedForMerge = remember { mutableStateListOf<Int>() }
    var renameLayerTargetIndex by remember { mutableStateOf<Int?>(null) }
    var renameLayerInputText by remember { mutableStateOf("") }
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
        history.add(layers.map { StickerEditorHistoryLayer(it.name, it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.visible, it.isLocked, it.scale, it.rotation, it.offsetX, it.offsetY, it.alpha) })
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

    fun updateOrCreateShapeLayer(
        shape: String,
        strokeColor: Int,
        strokeWidth: Float,
        isFilled: Boolean,
        fillColor: Int
    ) {
        val bmp = createShapeBitmap(
            shapeType = shape,
            size = 512,
            color = fillColor,
            strokeColor = strokeColor,
            strokeWidthPx = strokeWidth,
            isFilled = isFilled,
            fillColor = fillColor
        )
        val layer = if (activeLayer in 0..layers.lastIndex && layers[activeLayer].isShape) layers[activeLayer] else null
        if (layer != null) {
            snapshot()
            layer.bitmap = bmp
            layer.shapeType = shape
            layer.shapeStrokeColor = strokeColor
            layer.shapeStrokeWidthPx = strokeWidth
            layer.isShapeFilled = isFilled
            layer.shapeFillColor = fillColor
            layer.updateVisibleBounds()
            canvasVersion++
        } else {
            addBitmap("Shape $shape", bmp)
            val newLayer = layers.lastOrNull()
            if (newLayer != null) {
                newLayer.isShape = true
                newLayer.shapeType = shape
                newLayer.shapeStrokeColor = strokeColor
                newLayer.shapeStrokeWidthPx = strokeWidth
                newLayer.isShapeFilled = isFilled
                newLayer.shapeFillColor = fillColor
            }
        }
    }

    val canvasBaseWidth = 1024f
    val canvasBaseHeight = 1024f

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

    val defaultToolOrder = remember {
        listOf(
            "Camera", "Gallery", "Remove BG", "Auto BG",
            "Outline", "Eraser", "Brush", "Shapes",
            "Layers", "Text", "Speech Bubbles", "Duplicate",
            "Rotate 90°", "Flip", "Center", "Reset",
            "Emoji", "Online BG", "Filters", "Restore"
        )
    }

    var isCustomizeMode by remember { mutableStateOf(false) }
    var draggedToolName by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lastPageSwitchTime by remember { mutableLongStateOf(0L) }

    val customToolOrder = remember {
        val savedOrder = prefs.getString("custom_tool_order_v2", null)
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
            "Remove BG" to EditorToolSpec("Remove BG", "✂️") {
                currentTool = "Remove BG"
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer == null) {
                    notice = "Pumili muna ng layer o larawan."
                    Toast.makeText(context, "Please select an image layer first.", Toast.LENGTH_SHORT).show()
                    return@EditorToolSpec
                }

                scope.launch {
                    isRemovingBg = true
                    notice = "Awtomatikong tinatanggal ang background ng layer..."
                    val cutout = performAutoBackgroundRemoval(context, layer.bitmap)
                    if (cutout != null) {
                        snapshot()
                        layer.bitmap = cutout
                        layer.updateVisibleBounds()
                        canvasVersion++
                        notice = "Na-remove na ang background ng layer!"
                        Toast.makeText(context, "Background removed successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        notice = "Hindi matukoy ang subject sa larawan."
                        Toast.makeText(context, "Could not detect subject in image.", Toast.LENGTH_SHORT).show()
                    }
                    isRemovingBg = false
                }
            },
            "Outline" to EditorToolSpec("Outline", "🎨") { currentTool = "Outline" },
            "Eraser" to EditorToolSpec("Eraser", "🧹") { currentTool = "Eraser" },
            "Brush" to EditorToolSpec("Brush", "🖌️") { currentTool = "Brush" },
            "Emoji" to EditorToolSpec("Emoji", "⭐") { currentTool = "Emoji"; emojiDialog = true },
            "Auto BG" to EditorToolSpec("+ Background", "🌌") {
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
            "Text" to EditorToolSpec("Text", "T") {
                currentTool = "Text"
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer != null && layer.isSpeechBubble) {
                    editingSpeechBubbleLayer = layer
                    selectedBubbleStyle = layer.bubbleStyle
                    bubbleText = layer.bubbleText
                    bubbleTextColorIndex = layer.bubbleTextColorIndex
                    bubbleFillColorIndex = layer.bubbleFillColorIndex
                    bubbleStrokeColorIndex = layer.bubbleStrokeColorIndex
                    bubbleTextSizeSp = layer.bubbleTextSizeSp
                    bubbleOutlineThicknessSp = layer.bubbleOutlineThicknessSp
                    bubbleOpacityAlpha = layer.bubbleOpacityAlpha
                    bubbleTailDirection = layer.bubbleTailDirection
                    bubbleFontFamily = layer.bubbleFontFamily
                    bubbleFontStyle = layer.bubbleFontStyle
                    bubbleTextAlignName = layer.bubbleTextAlignName
                    speechBubbleDialog = true
                } else {
                    textDialog = true
                }
            },
            "Filters" to EditorToolSpec("Filters", "✨") { currentTool = "Filters" },
            "Font" to EditorToolSpec("Font", "🔤") {
                currentTool = "Font"
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer == null) {
                    notice = "Pumili muna ng text o speech bubble layer sa canvas."
                    Toast.makeText(context, "Please select a text or speech bubble layer first.", Toast.LENGTH_SHORT).show()
                    return@EditorToolSpec
                }
                fontPickerModalDialog = true
            },
            "Layers" to EditorToolSpec("Layers", "🥞") {
                currentTool = "Layers"
                isPanelHidden = false
                isPanelMinimized = false
                showRightLayerPanel = true
                notice = "Pinalitaw ang Floating Layers Panel."
            },
            "Speech Bubbles" to EditorToolSpec("Bubbles", "💬") {
                currentTool = "Speech Bubbles"
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer != null && layer.isSpeechBubble) {
                    editingSpeechBubbleLayer = layer
                    selectedBubbleStyle = layer.bubbleStyle
                    bubbleText = layer.bubbleText
                    bubbleTextColorIndex = layer.bubbleTextColorIndex
                    bubbleFillColorIndex = layer.bubbleFillColorIndex
                    bubbleStrokeColorIndex = layer.bubbleStrokeColorIndex
                    bubbleTextSizeSp = layer.bubbleTextSizeSp
                    bubbleOutlineThicknessSp = layer.bubbleOutlineThicknessSp
                    bubbleOpacityAlpha = layer.bubbleOpacityAlpha
                    bubbleTailDirection = layer.bubbleTailDirection
                    bubbleFontFamily = layer.bubbleFontFamily
                    bubbleFontStyle = layer.bubbleFontStyle
                    bubbleTextAlignName = layer.bubbleTextAlignName
                } else {
                    editingSpeechBubbleLayer = null
                    bubbleText = "Hello!"
                }
                speechBubbleDialog = true
            }
        )
    }

    val toolPages: List<List<EditorToolSpec?>> = remember(customToolOrder.toList(), allToolsMap) {
        val specs = customToolOrder.map { allToolsMap[it] }
        val page1 = specs.take(12).let { list -> list + List((12 - list.size).coerceAtLeast(0)) { null } }
        val page2 = specs.drop(12).take(12).let { list -> list + List((12 - list.size).coerceAtLeast(0)) { null } }
        listOf(page1, page2)
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF111113)).padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StickerEditorHeaderBar(
            onBack = onBack,
            onUndo = {
                if (history.isNotEmpty()) {
                    redoHistory.add(layers.map { StickerEditorHistoryLayer(it.name, it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.visible, it.isLocked, it.scale, it.rotation, it.offsetX, it.offsetY, it.alpha) })
                    val previous = history.removeAt(history.lastIndex)
                    layers.clear()
                    previous.forEach { s -> layers.add(StickerEditorLayer(s.name, s.bitmap).also { it.visible=s.visible; it.isLocked=s.isLocked; it.scale=s.scale; it.rotation=s.rotation; it.offsetX=s.offsetX; it.offsetY=s.offsetY; it.alpha=s.alpha }) }
                    activeLayer = layers.lastIndex; canvasVersion++
                }
            },
            onRedo = {
                if (redoHistory.isNotEmpty()) {
                    history.add(layers.map { StickerEditorHistoryLayer(it.name, it.bitmap.copy(Bitmap.Config.ARGB_8888, true), it.visible, it.isLocked, it.scale, it.rotation, it.offsetX, it.offsetY, it.alpha) })
                    val next = redoHistory.removeAt(redoHistory.lastIndex)
                    layers.clear()
                    next.forEach { s -> layers.add(StickerEditorLayer(s.name, s.bitmap).also { it.visible=s.visible; it.isLocked=s.isLocked; it.scale=s.scale; it.rotation=s.rotation; it.offsetX=s.offsetX; it.offsetY=s.offsetY; it.alpha=s.alpha }) }
                    activeLayer = layers.lastIndex; canvasVersion++
                }
            },
            onTemplateClick = {
                val visibleLayers = layers.filter { it.visible }
                if (visibleLayers.isEmpty()) {
                    notice = "Magdagdag muna ng larawan bago mag-save ng template."
                } else {
                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Project & Template") {
                        showSaveTemplateDialog = true
                    }
                }
            },
            onSaveToDeviceClick = {
                val visibleLayers = layers.filter { it.visible }
                if (visibleLayers.isEmpty()) {
                    notice = "Magdagdag muna ng larawan bago mag-save."
                } else {
                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save to Device Gallery") {
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
                        if (saveBitmapToDeviceGallery(context, output)) {
                            notice = "Na-save ang sticker sa Gallery!"
                            Toast.makeText(context, "Sticker saved to Device Gallery!", Toast.LENGTH_SHORT).show()
                        } else {
                            notice = "Hindi na-save ang sticker sa Gallery."
                            Toast.makeText(context, "Could not save to device gallery.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onSaveClick = {
                val visibleLayers = layers.filter { it.visible }
                if (visibleLayers.isEmpty()) {
                    notice = "Magdagdag muna ng larawan bago mag-save."
                } else {
                    rewardedAdOfferSpec = RewardedAdOfferSpec("Save Image & Export") {
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
                        if (saveMyStickerBitmap(context, output, null)) {
                            onStickerSaved()
                            notice = "Na-save ang sticker sa My Stickers!"
                            Toast.makeText(context, "Sticker saved successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            notice = "Hindi na-save ang sticker."
                        }
                    }
                }
            },
            editorRed = editorRed
        )

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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            StickerEditorCanvas(
                layers = layers,
                activeLayer = activeLayer,
                currentTool = currentTool,
                canvasVersion = canvasVersion,
                isRemovingBg = isRemovingBg,
                editorRed = editorRed,
                canvasBaseWidth = canvasBaseWidth,
                canvasBaseHeight = canvasBaseHeight,
                brushColorInt = brushColorInt,
                brushSize = brushSize,
                showRightLayerPanel = showRightLayerPanel,
                isPanelHidden = isPanelHidden,
                isPanelMinimized = isPanelMinimized,
                floatingPanelOffsetX = floatingPanelOffsetX,
                floatingPanelOffsetY = floatingPanelOffsetY,
                selectedForMerge = selectedForMerge,
                onActiveLayerChange = { activeLayer = it },
                onSnapshot = { snapshot() },
                onCanvasVersionIncrement = { canvasVersion++ },
                onNoticeChange = { notice = it },
                onToggleVisibility = { idx ->
                    snapshot()
                    layers[idx].visible = !layers[idx].visible
                    canvasVersion++
                },
                onToggleLock = { idx ->
                    snapshot()
                    layers[idx].isLocked = !layers[idx].isLocked
                    canvasVersion++
                },
                onOpenSettings = { idx -> layerSettingsTargetIndex = idx },
                onMoveUp = { idx ->
                    if (idx < layers.lastIndex) {
                        snapshot()
                        val temp = layers[idx]
                        layers[idx] = layers[idx + 1]
                        layers[idx + 1] = temp
                        activeLayer = idx + 1
                        canvasVersion++
                    }
                },
                onMoveDown = { idx ->
                    if (idx > 0) {
                        snapshot()
                        val temp = layers[idx]
                        layers[idx] = layers[idx - 1]
                        layers[idx - 1] = temp
                        activeLayer = idx - 1
                        canvasVersion++
                    }
                },
                onDuplicate = { idx ->
                    snapshot()
                    val target = layers[idx]
                    val dup = StickerEditorLayer("${target.name} Copy", target.bitmap.copy(Bitmap.Config.ARGB_8888, true)).apply {
                        visible = target.visible
                        isLocked = false
                        scale = target.scale
                        rotation = target.rotation
                        offsetX = target.offsetX + 20f
                        offsetY = target.offsetY + 20f
                        alpha = target.alpha
                    }
                    layers.add(dup)
                    activeLayer = layers.lastIndex
                    canvasVersion++
                },
                onDelete = { idx ->
                    if (layers.size > 1) {
                        snapshot()
                        layers.removeAt(idx)
                        activeLayer = layers.lastIndex.coerceAtLeast(0)
                        canvasVersion++
                    } else {
                        notice = "Kailangan ng kahit isang layer."
                    }
                },
                onRename = { idx ->
                    renameLayerTargetIndex = idx
                    renameLayerInputText = layers[idx].name
                },
                onMergeSelected = {
                    if (selectedForMerge.size >= 2) {
                        snapshot()
                        mergeSelectedLayers(layers, selectedForMerge)
                        selectedForMerge.clear()
                        activeLayer = layers.lastIndex.coerceAtLeast(0)
                        canvasVersion++
                        notice = "Na-merge na ang mga napiling layers!"
                    }
                },
                onPanelMinimize = { isPanelMinimized = true },
                onPanelClose = {
                    isPanelHidden = true
                    showRightLayerPanel = false
                },
                onPanelDrag = { dx, dy ->
                    floatingPanelOffsetX += dx
                    floatingPanelOffsetY += dy
                }
            )
        }

        val showToolSubPanel = when (currentTool) {
            "Outline", "Eraser", "Brush", "Shapes", "Filters" -> true
            "Move" -> activeLayer in 0..layers.lastIndex && layers[activeLayer].isSpeechBubble
            else -> false
        }

        if (showToolSubPanel) {
            StickerEditorToolSubPanel(
                currentTool = currentTool,
                activeLayer = activeLayer,
                layers = layers,
                outlineThickness = outlineThickness,
                outlineColorIndex = outlineColorIndex,
                brushSize = brushSize,
                brushColorInt = brushColorInt,
                selectedShape = selectedShape,
                shapeStrokeColorInt = shapeStrokeColorInt,
                shapeStrokeWidthPx = shapeStrokeWidthPx,
                isShapeFilled = isShapeFilled,
                shapeFillColorInt = shapeFillColorInt,
                activeFilter = activeFilter,
                palette = palette,
                editorRed = editorRed,
                context = context,
                originalBitmapsMap = originalBitmapsMap,
                onOutlineThicknessChange = { outlineThickness = it },
                onOutlineColorIndexChange = { outlineColorIndex = it },
                onApplyOutline = {
                    val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                    if (layer != null) {
                        snapshot()
                        layer.bitmap = addStickerOutline(layer.bitmap, palette[outlineColorIndex])
                        canvasVersion++
                        notice = "Naidagdag ang outline sa layer!"
                        Toast.makeText(context, "Outline applied to layer!", Toast.LENGTH_SHORT).show()
                    } else notice = "Pumili muna ng layer."
                },
                onBrushSizeChange = { brushSize = it },
                onBrushColorIntChange = { brushColorInt = it },
                onShowBrushColorPicker = { showBrushColorPickerDialog = true },
                onShapeTypeSelected = { shape ->
                    selectedShape = shape
                    updateOrCreateShapeLayer(shape, shapeStrokeColorInt, shapeStrokeWidthPx, isShapeFilled, shapeFillColorInt)
                    notice = "Naidagdag/Na-update ang hugis $shape."
                },
                onShapeFillTypeChange = { filled ->
                    isShapeFilled = filled
                    updateOrCreateShapeLayer(selectedShape, shapeStrokeColorInt, shapeStrokeWidthPx, filled, shapeFillColorInt)
                },
                onShapeFillColorSelected = { color ->
                    shapeFillColorInt = color
                    updateOrCreateShapeLayer(selectedShape, shapeStrokeColorInt, shapeStrokeWidthPx, true, color)
                },
                onShowShapeFillColorPicker = { showShapeFillColorPickerDialog = true },
                onShapeStrokeColorSelected = { color ->
                    shapeStrokeColorInt = color
                    updateOrCreateShapeLayer(selectedShape, color, shapeStrokeWidthPx, isShapeFilled, shapeFillColorInt)
                },
                onShowShapeStrokeColorPicker = { showShapeOutlineColorPickerDialog = true },
                onShapeStrokeWidthChange = { width ->
                    shapeStrokeWidthPx = width
                    if (activeLayer in 0..layers.lastIndex && layers[activeLayer].isShape) {
                        updateOrCreateShapeLayer(selectedShape, shapeStrokeColorInt, width, isShapeFilled, shapeFillColorInt)
                    }
                },
                onFilterSelected = { filter ->
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
                onEditSpeechBubble = { layer ->
                    editingSpeechBubbleLayer = layer
                    selectedBubbleStyle = layer.bubbleStyle
                    bubbleText = layer.bubbleText
                    bubbleTextColorIndex = layer.bubbleTextColorIndex
                    bubbleFillColorIndex = layer.bubbleFillColorIndex
                    bubbleStrokeColorIndex = layer.bubbleStrokeColorIndex
                    bubbleTextSizeSp = layer.bubbleTextSizeSp
                    bubbleOutlineThicknessSp = layer.bubbleOutlineThicknessSp
                    bubbleOpacityAlpha = layer.bubbleOpacityAlpha
                    bubbleTailDirection = layer.bubbleTailDirection
                    bubbleFontFamily = layer.bubbleFontFamily
                    bubbleFontStyle = layer.bubbleFontStyle
                    bubbleTextAlignName = layer.bubbleTextAlignName
                    speechBubbleDialog = true
                }
            )
        }

        StickerEditorZoomResetRow(
            panelColor = panelColor,
            onZoomIn = {
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer != null) {
                    snapshot()
                    layer.scale = (layer.scale * 1.15f).coerceAtMost(5f)
                    canvasVersion++
                    notice = "Zoom In: ${(layer.scale * 100).toInt()}%"
                } else notice = "Pumili muna ng layer."
            },
            onReset = {
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer != null) {
                    snapshot()
                    layer.offsetX = 0f
                    layer.offsetY = 0f
                    layer.scale = 1f
                    layer.rotation = 0f
                    canvasVersion++
                    notice = "Reset layer transform."
                } else notice = "Pumili muna ng layer."
            },
            onZoomOut = {
                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
                if (layer != null) {
                    snapshot()
                    layer.scale = (layer.scale / 1.15f).coerceAtLeast(0.2f)
                    canvasVersion++
                    notice = "Zoom Out: ${(layer.scale * 100).toInt()}%"
                } else notice = "Pumili muna ng layer."
            }
        )

        StickerEditorToolBar(
            pagerState = pagerState,
            toolPages = toolPages,
            currentTool = currentTool,
            isCustomizeMode = isCustomizeMode,
            draggedToolName = draggedToolName,
            dragOffset = dragOffset,
            customToolOrder = customToolOrder,
            defaultToolOrder = defaultToolOrder,
            prefs = prefs,
            context = context,
            editorRed = editorRed,
            panelColor = panelColor,
            scope = scope,
            onToolClick = { spec -> spec.onClick() },
            onCustomizeModeChange = { isCustomizeMode = it },
            onNoticeChange = { notice = it },
            onDraggedToolNameChange = { draggedToolName = it },
            onDragOffsetChange = { dragOffset = it },
            onLastPageSwitchTimeChange = { lastPageSwitchTime = it },
            lastPageSwitchTime = lastPageSwitchTime
        )

        Text(notice, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
    }

    if (showSaveTemplateDialog) {
        StickerEditorSaveTemplateDialog(
            templateNameInput = templateNameInput,
            onTemplateNameChange = { templateNameInput = it },
            selectedCategoryInput = selectedCategoryInput,
            onSelectedCategoryChange = { selectedCategoryInput = it },
            isPublishedInput = isPublishedInput,
            onIsPublishedChange = { isPublishedInput = it },
            editorRed = editorRed,
            onConfirm = {
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

                    val savedTemplate = UserStickerTemplatesManager.saveUserTemplate(
                        context = context,
                        templateName = templateNameInput.ifBlank { "My Sticker Template" },
                        creatorName = creatorName,
                        category = selectedCategoryInput,
                        layerDataList = layerDataList,
                        previewBitmap = output,
                        isPublished = isPublishedInput
                    )

                    if (savedTemplate != null) {
                        notice = "Na-save ang template bilang '$templateNameInput'!"
                        Toast.makeText(context, "Template saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        notice = "Hindi na-save ang template."
                    }
                }
                showSaveTemplateDialog = false
            },
            onDismiss = { showSaveTemplateDialog = false }
        )
    }

    renameLayerTargetIndex?.let { idx ->
        if (idx in 0..layers.lastIndex) {
            val targetLayer = layers[idx]
            StickerEditorRenameLayerDialog(
                initialName = renameLayerInputText,
                editorRed = editorRed,
                onSave = { newName ->
                    if (newName.isNotBlank()) {
                        snapshot()
                        targetLayer.name = newName
                        canvasVersion++
                    }
                    renameLayerTargetIndex = null
                },
                onDismiss = { renameLayerTargetIndex = null }
            )
        }
    }

    layerSettingsTargetIndex?.let { idx ->
        if (idx in 0..layers.lastIndex) {
            val targetLayer = layers[idx]
            StickerEditorLayerPropertiesDialog(
                layer = targetLayer,
                layerIndex = idx,
                editorRed = editorRed,
                onAlphaChange = { alpha ->
                    targetLayer.alpha = alpha
                    canvasVersion++
                },
                onToggleLock = {
                    targetLayer.isLocked = !targetLayer.isLocked
                    canvasVersion++
                },
                onRotate90 = {
                    targetLayer.rotation = (targetLayer.rotation + 90f) % 360f
                    canvasVersion++
                },
                onFlipH = {
                    targetLayer.scale = -targetLayer.scale
                    canvasVersion++
                },
                onCenter = {
                    targetLayer.offsetX = 0f
                    targetLayer.offsetY = 0f
                    canvasVersion++
                },
                onRename = {
                    renameLayerTargetIndex = idx
                    renameLayerInputText = targetLayer.name
                    layerSettingsTargetIndex = null
                },
                onDuplicate = {
                    snapshot()
                    val dup = StickerEditorLayer("${targetLayer.name} Copy", targetLayer.bitmap.copy(Bitmap.Config.ARGB_8888, true)).apply {
                        visible = targetLayer.visible
                        isLocked = false
                        scale = targetLayer.scale
                        rotation = targetLayer.rotation
                        offsetX = targetLayer.offsetX + 20f
                        offsetY = targetLayer.offsetY + 20f
                        alpha = targetLayer.alpha
                    }
                    layers.add(dup)
                    activeLayer = layers.lastIndex
                    canvasVersion++
                    layerSettingsTargetIndex = null
                },
                onDelete = {
                    if (layers.size > 1) {
                        snapshot()
                        layers.removeAt(idx)
                        activeLayer = layers.lastIndex.coerceAtLeast(0)
                        canvasVersion++
                    }
                    layerSettingsTargetIndex = null
                },
                onDismiss = { layerSettingsTargetIndex = null }
            )
        }
    }

    if (fontPickerModalDialog) {
        val targetLayer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else null
        val originalFontFamily = remember(targetLayer) {
            targetLayer?.let { if (it.isSpeechBubble) it.bubbleFontFamily else it.textFontFamily } ?: "Sans Serif"
        }

        if (targetLayer != null) {
            val sampleText = if (targetLayer.isSpeechBubble) targetLayer.bubbleText else targetLayer.name.removePrefix("Text: ")

            FontSelectionPanelDialog(
                initialFontFamily = originalFontFamily,
                sampleText = sampleText,
                onFontSelected = { font ->
                    snapshot()
                    if (targetLayer.isSpeechBubble) {
                        targetLayer.bubbleFontFamily = font.familyKey
                        targetLayer.bitmap = createSpeechBubbleBitmap(
                            bubbleStyle = targetLayer.bubbleStyle,
                            text = targetLayer.bubbleText,
                            textColorInt = palette[targetLayer.bubbleTextColorIndex.coerceIn(0, palette.lastIndex)],
                            textSizeSp = targetLayer.bubbleTextSizeSp,
                            fillColorInt = palette[targetLayer.bubbleFillColorIndex.coerceIn(0, palette.lastIndex)],
                            strokeColorInt = palette[targetLayer.bubbleStrokeColorIndex.coerceIn(0, palette.lastIndex)],
                            strokeThicknessPx = targetLayer.bubbleOutlineThicknessSp,
                            opacityAlpha = targetLayer.bubbleOpacityAlpha,
                            tailDirection = targetLayer.bubbleTailDirection,
                            fontFamily = font.familyKey,
                            fontStyle = targetLayer.bubbleFontStyle,
                            textAlignName = targetLayer.bubbleTextAlignName,
                            context = context
                        )
                    } else {
                        targetLayer.textFontFamily = font.familyKey
                        targetLayer.bitmap = createTextStickerBitmap(
                            text = targetLayer.name.removePrefix("Text: "),
                            fontFamily = font.familyKey,
                            fontStyle = targetLayer.textFontStyle,
                            textColorInt = palette[targetLayer.textColorIndex.coerceIn(0, palette.lastIndex)],
                            outlineColorInt = palette[targetLayer.textOutlineColorIndex.coerceIn(0, palette.lastIndex)],
                            textSizeSp = targetLayer.textFontSizeSp,
                            textAlignName = targetLayer.textAlignName,
                            outlineThicknessPx = targetLayer.textOutlineThicknessPx,
                            hasShadow = targetLayer.hasTextShadow,
                            letterSpacingEm = targetLayer.textLetterSpacingEm,
                            context = context
                        )
                    }
                    targetLayer.updateVisibleBounds()
                    canvasVersion++
                },
                onApply = { font ->
                    notice = "Na-apply ang font '${font.displayName}'!"
                    Toast.makeText(context, "Applied font: ${font.displayName}", Toast.LENGTH_SHORT).show()
                    fontPickerModalDialog = false
                },
                onCancel = {
                    if (targetLayer.isSpeechBubble) {
                        targetLayer.bubbleFontFamily = originalFontFamily
                        targetLayer.bitmap = createSpeechBubbleBitmap(
                            bubbleStyle = targetLayer.bubbleStyle,
                            text = targetLayer.bubbleText,
                            textColorInt = palette[targetLayer.bubbleTextColorIndex.coerceIn(0, palette.lastIndex)],
                            textSizeSp = targetLayer.bubbleTextSizeSp,
                            fillColorInt = palette[targetLayer.bubbleFillColorIndex.coerceIn(0, palette.lastIndex)],
                            strokeColorInt = palette[targetLayer.bubbleStrokeColorIndex.coerceIn(0, palette.lastIndex)],
                            strokeThicknessPx = targetLayer.bubbleOutlineThicknessSp,
                            opacityAlpha = targetLayer.bubbleOpacityAlpha,
                            tailDirection = targetLayer.bubbleTailDirection,
                            fontFamily = originalFontFamily,
                            fontStyle = targetLayer.bubbleFontStyle,
                            textAlignName = targetLayer.bubbleTextAlignName,
                            context = context
                        )
                    } else {
                        targetLayer.textFontFamily = originalFontFamily
                        targetLayer.bitmap = createTextStickerBitmap(
                            text = targetLayer.name.removePrefix("Text: "),
                            fontFamily = originalFontFamily,
                            fontStyle = targetLayer.textFontStyle,
                            textColorInt = palette[targetLayer.textColorIndex.coerceIn(0, palette.lastIndex)],
                            outlineColorInt = palette[targetLayer.textOutlineColorIndex.coerceIn(0, palette.lastIndex)],
                            textSizeSp = targetLayer.textFontSizeSp,
                            textAlignName = targetLayer.textAlignName,
                            outlineThicknessPx = targetLayer.textOutlineThicknessPx,
                            hasShadow = targetLayer.hasTextShadow,
                            letterSpacingEm = targetLayer.textLetterSpacingEm,
                            context = context
                        )
                    }
                    targetLayer.updateVisibleBounds()
                    canvasVersion++
                    fontPickerModalDialog = false
                },
                prefs = prefs
            )
        }
    }

    if (textDialog) {
        TextEditorDialog(
            editingTextLayer = editingTextLayer,
            initialText = layerText,
            initialFontFamily = textFontFamily,
            initialFontStyle = textFontStyle,
            initialTextAlignName = textAlignName,
            initialFontSizeSp = textFontSizeSp,
            initialTextColorIndex = textColorIndex,
            initialOutlineColorIndex = textOutlineColorIndex,
            initialOutlineThicknessPx = textOutlineThicknessPx,
            initialLetterSpacingEm = textLetterSpacingEm,
            initialHasShadow = hasTextShadow,
            palette = palette,
            prefs = prefs,
            onSave = { text, fontFamily, fontStyle, alignName, sizeSp, colorIdx, outlineIdx, outlineThick, spacingEm, shadow ->
                val bmp = createTextStickerBitmap(
                    text = text,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    textColorInt = palette[colorIdx.coerceIn(0, palette.lastIndex)],
                    outlineColorInt = palette[outlineIdx.coerceIn(0, palette.lastIndex)],
                    textSizeSp = sizeSp,
                    textAlignName = alignName,
                    outlineThicknessPx = outlineThick,
                    hasShadow = shadow,
                    letterSpacingEm = spacingEm,
                    context = context
                )

                val target = editingTextLayer
                if (target != null) {
                    snapshot()
                    target.bitmap = bmp
                    target.name = "Text: $text"
                    target.updateVisibleBounds()
                    target.textFontFamily = fontFamily
                    target.textFontStyle = fontStyle
                    target.textAlignName = alignName
                    target.textFontSizeSp = sizeSp
                    target.textColorIndex = colorIdx
                    target.textOutlineColorIndex = outlineIdx
                    target.textOutlineThicknessPx = outlineThick
                    target.textLetterSpacingEm = spacingEm
                    target.hasTextShadow = shadow
                    canvasVersion++
                    notice = "Na-update ang Text layer!"
                    Toast.makeText(context, "Text layer updated!", Toast.LENGTH_SHORT).show()
                } else {
                    addBitmap("Text: $text", bmp)
                    val newLayer = layers.lastOrNull()
                    if (newLayer != null) {
                        newLayer.textFontFamily = fontFamily
                        newLayer.textFontStyle = fontStyle
                        newLayer.textAlignName = alignName
                        newLayer.textFontSizeSp = sizeSp
                        newLayer.textColorIndex = colorIdx
                        newLayer.textOutlineColorIndex = outlineIdx
                        newLayer.textOutlineThicknessPx = outlineThick
                        newLayer.textLetterSpacingEm = spacingEm
                        newLayer.hasTextShadow = shadow
                    }
                    notice = "Naidagdag ang Text layer!"
                }
                layerText = ""
                editingTextLayer = null
                textDialog = false
            },
            onDismiss = {
                textDialog = false
                editingTextLayer = null
            }
        )
    }

    if (speechBubbleDialog) {
        SpeechBubbleEditorDialog(
            editingSpeechBubbleLayer = editingSpeechBubbleLayer,
            initialBubbleStyle = selectedBubbleStyle,
            initialText = bubbleText,
            initialTextColorIndex = bubbleTextColorIndex,
            initialFillColorIndex = bubbleFillColorIndex,
            initialStrokeColorIndex = bubbleStrokeColorIndex,
            initialTextSizeSp = bubbleTextSizeSp,
            initialOutlineThicknessSp = bubbleOutlineThicknessSp,
            initialOpacityAlpha = bubbleOpacityAlpha,
            initialTailDirection = bubbleTailDirection,
            initialFontFamily = bubbleFontFamily,
            initialFontStyle = bubbleFontStyle,
            initialTextAlignName = bubbleTextAlignName,
            palette = palette,
            onOpenFontPicker = { fontPickerModalDialog = true },
            onSave = { style, text, textClr, fillClr, strokeClr, txtSize, outlineThick, opacity, tailDir, family, fStyle, alignName ->
                val bmp = createSpeechBubbleBitmap(
                    bubbleStyle = style,
                    text = text,
                    textColorInt = palette[textClr.coerceIn(0, palette.lastIndex)],
                    textSizeSp = txtSize,
                    fillColorInt = palette[fillClr.coerceIn(0, palette.lastIndex)],
                    strokeColorInt = palette[strokeClr.coerceIn(0, palette.lastIndex)],
                    strokeThicknessPx = outlineThick,
                    opacityAlpha = opacity,
                    tailDirection = tailDir,
                    fontFamily = family,
                    fontStyle = fStyle,
                    textAlignName = alignName,
                    context = context
                )

                val target = editingSpeechBubbleLayer
                if (target != null) {
                    snapshot()
                    target.bitmap = bmp
                    target.name = "Speech Bubble: $text"
                    target.updateVisibleBounds()
                    target.isSpeechBubble = true
                    target.bubbleStyle = style
                    target.bubbleText = text
                    target.bubbleTextColorIndex = textClr
                    target.bubbleFillColorIndex = fillClr
                    target.bubbleStrokeColorIndex = strokeClr
                    target.bubbleTextSizeSp = txtSize
                    target.bubbleOutlineThicknessSp = outlineThick
                    target.bubbleOpacityAlpha = opacity
                    target.bubbleTailDirection = tailDir
                    target.bubbleFontFamily = family
                    target.bubbleFontStyle = fStyle
                    target.bubbleTextAlignName = alignName
                    canvasVersion++
                    notice = "Na-update ang Speech Bubble text!"
                    Toast.makeText(context, "Speech Bubble updated!", Toast.LENGTH_SHORT).show()
                } else {
                    addBitmap("Speech Bubble: $text", bmp)
                    val newLayer = layers.lastOrNull()
                    if (newLayer != null) {
                        newLayer.isSpeechBubble = true
                        newLayer.bubbleStyle = style
                        newLayer.bubbleText = text
                        newLayer.bubbleTextColorIndex = textClr
                        newLayer.bubbleFillColorIndex = fillClr
                        newLayer.bubbleStrokeColorIndex = strokeClr
                        newLayer.bubbleTextSizeSp = txtSize
                        newLayer.bubbleOutlineThicknessSp = outlineThick
                        newLayer.bubbleOpacityAlpha = opacity
                        newLayer.bubbleTailDirection = tailDir
                        newLayer.bubbleFontFamily = family
                        newLayer.bubbleFontStyle = fStyle
                        newLayer.bubbleTextAlignName = alignName
                    }
                    notice = "Naidagdag ang Speech Bubble!"
                }
                editingSpeechBubbleLayer = null
                speechBubbleDialog = false
            },
            onDismiss = { speechBubbleDialog = false }
        )
    }

    if (emojiDialog) {
        StickerEditorEmojiDialog(
            onSelectEmoji = { emoji ->
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 180f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(emoji, 128f, 190f, paint)
                addBitmap("Emoji $emoji", bitmap)
                emojiDialog = false
            },
            onDismiss = { emojiDialog = false }
        )
    }

    if (backgroundGenDialog) {
        StickerEditorAutoBgDialog(
            previewBgBitmap = previewBgBitmap,
            bgDialogTab = bgDialogTab,
            onTabChange = { bgDialogTab = it },
            aiPromptText = aiPromptText,
            onAiPromptChange = { aiPromptText = it },
            isAiGenerating = isAiGenerating,
            onGenerateFreeBg = { previewBgBitmap = generateFreeAutoBackgroundBitmap(1024, 1024) },
            onGenerateAiBg = {
                scope.launch {
                    isAiGenerating = true
                    val bitmap = fetchAiBackgroundBitmap(context, aiPromptText)
                    if (bitmap != null) previewBgBitmap = bitmap
                    else Toast.makeText(context, "Could not generate AI background.", Toast.LENGTH_SHORT).show()
                    isAiGenerating = false
                }
            },
            onApplyBg = {
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
            onDismiss = { backgroundGenDialog = false }
        )
    }

    if (onlineBgDialog) {
        StickerEditorOnlineBgDialog(
            onlineSearchQuery = onlineSearchQuery,
            onSearchQueryChange = { onlineSearchQuery = it },
            onlineSelectedCategory = onlineSelectedCategory,
            onSelectedCategoryChange = { onlineSelectedCategory = it },
            isDownloadingOnlineBg = isDownloadingOnlineBg,
            editorRed = editorRed,
            onSelectBgItem = { bgItem ->
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
            onDismiss = { onlineBgDialog = false }
        )
    }

    if (showBrushColorPickerDialog) {
        BrushCustomColorPickerDialog(
            initialColor = brushColorInt,
            dialogTitle = "🎨 Brush Custom Color",
            onColorSelected = { selectedColor ->
                brushColorInt = selectedColor
            },
            onDismiss = { showBrushColorPickerDialog = false }
        )
    }

    if (showShapeOutlineColorPickerDialog) {
        BrushCustomColorPickerDialog(
            initialColor = shapeStrokeColorInt,
            dialogTitle = "🎨 Shape Outline Color",
            onColorSelected = { selectedColor ->
                shapeStrokeColorInt = selectedColor
                if (activeLayer in 0..layers.lastIndex && layers[activeLayer].isShape) {
                    val layer = layers[activeLayer]
                    val bmp = createShapeBitmap(
                        shapeType = layer.shapeType,
                        size = 512,
                        color = shapeFillColorInt,
                        strokeColor = selectedColor,
                        strokeWidthPx = shapeStrokeWidthPx,
                        isFilled = isShapeFilled,
                        fillColor = shapeFillColorInt
                    )
                    snapshot()
                    layer.bitmap = bmp
                    layer.shapeStrokeColor = selectedColor
                    layer.updateVisibleBounds()
                    canvasVersion++
                }
            },
            onDismiss = { showShapeOutlineColorPickerDialog = false }
        )
    }

    if (showShapeFillColorPickerDialog) {
        BrushCustomColorPickerDialog(
            initialColor = shapeFillColorInt,
            dialogTitle = "🎨 Shape Fill Color",
            onColorSelected = { selectedColor ->
                shapeFillColorInt = selectedColor
                isShapeFilled = true
                if (activeLayer in 0..layers.lastIndex && layers[activeLayer].isShape) {
                    val layer = layers[activeLayer]
                    val bmp = createShapeBitmap(
                        shapeType = layer.shapeType,
                        size = 512,
                        color = selectedColor,
                        strokeColor = shapeStrokeColorInt,
                        strokeWidthPx = shapeStrokeWidthPx,
                        isFilled = true,
                        fillColor = selectedColor
                    )
                    snapshot()
                    layer.bitmap = bmp
                    layer.shapeFillColor = selectedColor
                    layer.isShapeFilled = true
                    layer.updateVisibleBounds()
                    canvasVersion++
                }
            },
            onDismiss = { showShapeFillColorPickerDialog = false }
        )
    }
}

@Composable
private fun StickerEditorHeaderBar(
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTemplateClick: () -> Unit,
    onSaveToDeviceClick: () -> Unit,
    onSaveClick: () -> Unit,
    editorRed: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D1D20), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF383840), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onUndo,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B30))
            ) {
                Text("↶ Undo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onRedo,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B30))
            ) {
                Text("↷ Redo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onTemplateClick,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("📋 Template", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSaveToDeviceClick,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
            ) {
                Text("📥 Save to Device", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = editorRed)
            ) {
                Text("💾 Save", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StickerEditorCanvas(
    layers: SnapshotStateList<StickerEditorLayer>,
    activeLayer: Int,
    currentTool: String,
    canvasVersion: Int,
    isRemovingBg: Boolean,
    editorRed: Color,
    canvasBaseWidth: Float,
    canvasBaseHeight: Float,
    brushColorInt: Int,
    brushSize: Float,
    showRightLayerPanel: Boolean,
    isPanelHidden: Boolean,
    isPanelMinimized: Boolean,
    floatingPanelOffsetX: Float,
    floatingPanelOffsetY: Float,
    selectedForMerge: SnapshotStateList<Int>,
    onActiveLayerChange: (Int) -> Unit,
    onSnapshot: () -> Unit,
    onCanvasVersionIncrement: () -> Unit,
    onNoticeChange: (String) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onOpenSettings: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRename: (Int) -> Unit,
    onMergeSelected: () -> Unit,
    onPanelMinimize: () -> Unit,
    onPanelClose: () -> Unit,
    onPanelDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                canvasSizePx = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
            },
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = if (canvasSizePx.width > 0f) canvasSizePx.width else 1024f
        val containerHeightPx = if (canvasSizePx.height > 0f) canvasSizePx.height else 1024f
        val visibleLayers = layers.filter { it.visible }

        if (visibleLayers.isEmpty()) {
            Text("Pumili o magdagdag ng larawan", color = Color.White)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(currentTool, activeLayer, brushColorInt, brushSize, layers.size) {
                        if (currentTool != "Brush" && currentTool != "Eraser") {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val hitIndex = findLayerAtTouch(down.position, layers, Size(size.width.toFloat(), size.height.toFloat()))
                                onActiveLayerChange(hitIndex)

                                do {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (!canceled && activeLayer in 0..layers.lastIndex) {
                                        val pan = event.calculatePan()
                                        val zoom = event.calculateZoom()
                                        val rotationChange = event.calculateRotation()

                                        val layer = layers[activeLayer]
                                        if (layer.visible && !layer.isLocked) {
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
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val layer = if (activeLayer in 0..layers.lastIndex) layers[activeLayer] else return@awaitEachGesture
                                if (!layer.visible || layer.isLocked) return@awaitEachGesture

                                onSnapshot()
                                var prevPos = down.position

                                drawStrokeOnBitmap(
                                    layer.bitmap,
                                    prevPos,
                                    down.position,
                                    Size(size.width.toFloat(), size.height.toFloat()),
                                    layer,
                                    if (currentTool == "Eraser") AndroidColor.TRANSPARENT else brushColorInt,
                                    brushSize,
                                    isEraser = (currentTool == "Eraser")
                                )
                                down.consume()
                                onCanvasVersionIncrement()

                                do {
                                    val event = awaitPointerEvent()
                                    val pointerChange = event.changes.firstOrNull { it.pressed }
                                    if (pointerChange != null) {
                                        val historical = pointerChange.historical
                                        for (i in 0 until historical.size) {
                                            val histPos = historical[i].position
                                            if (histPos != prevPos) {
                                                drawStrokeOnBitmap(
                                                    layer.bitmap,
                                                    prevPos,
                                                    histPos,
                                                    Size(size.width.toFloat(), size.height.toFloat()),
                                                    layer,
                                                    if (currentTool == "Eraser") AndroidColor.TRANSPARENT else brushColorInt,
                                                    brushSize,
                                                    isEraser = (currentTool == "Eraser")
                                                )
                                                prevPos = histPos
                                            }
                                        }

                                        val currentPos = pointerChange.position
                                        if (currentPos != prevPos) {
                                            drawStrokeOnBitmap(
                                                layer.bitmap,
                                                prevPos,
                                                currentPos,
                                                Size(size.width.toFloat(), size.height.toFloat()),
                                                layer,
                                                if (currentTool == "Eraser") AndroidColor.TRANSPARENT else brushColorInt,
                                                brushSize,
                                                isEraser = (currentTool == "Eraser")
                                            )
                                            prevPos = currentPos
                                        }
                                        pointerChange.consume()
                                        onCanvasVersionIncrement()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
            ) {
                Checkerboard(Modifier.matchParentSize())

                @Suppress("UNUSED_VARIABLE")
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

        val isFloatingPanelVisible = !isPanelHidden && showRightLayerPanel && layers.isNotEmpty()

        if (isFloatingPanelVisible) {
            if (isPanelMinimized) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(floatingPanelOffsetX.roundToInt(), floatingPanelOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onPanelDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(editorRed)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onPanelMinimize() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🥞", fontSize = 20.sp)
                }
            } else {
                FloatingLayersPanelWindow(
                    layers = layers,
                    activeLayer = activeLayer,
                    selectedForMerge = selectedForMerge,
                    onSelectLayer = { idx -> onActiveLayerChange(idx); onCanvasVersionIncrement() },
                    onToggleVisibility = onToggleVisibility,
                    onToggleLock = onToggleLock,
                    onOpenSettings = onOpenSettings,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    onRename = onRename,
                    onMergeSelected = onMergeSelected,
                    onMinimize = onPanelMinimize,
                    onClose = onPanelClose,
                    onDrag = onPanelDrag,
                    modifier = Modifier
                        .offset { IntOffset(floatingPanelOffsetX.roundToInt(), floatingPanelOffsetY.roundToInt()) }
                        .width(145.dp)
                        .heightIn(max = 240.dp)
                )
            }
        }
    }
}

@Composable
private fun StickerEditorToolSubPanel(
    currentTool: String,
    activeLayer: Int,
    layers: List<StickerEditorLayer>,
    outlineThickness: Float,
    outlineColorIndex: Int,
    brushSize: Float,
    brushColorInt: Int,
    selectedShape: String,
    shapeStrokeColorInt: Int,
    shapeStrokeWidthPx: Float,
    isShapeFilled: Boolean,
    shapeFillColorInt: Int,
    activeFilter: String,
    palette: List<Int>,
    editorRed: Color,
    context: Context,
    originalBitmapsMap: MutableMap<String, Bitmap>,
    onOutlineThicknessChange: (Float) -> Unit,
    onOutlineColorIndexChange: (Int) -> Unit,
    onApplyOutline: () -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onBrushColorIntChange: (Int) -> Unit,
    onShowBrushColorPicker: () -> Unit,
    onShapeTypeSelected: (String) -> Unit,
    onShapeFillTypeChange: (Boolean) -> Unit,
    onShapeFillColorSelected: (Int) -> Unit,
    onShowShapeFillColorPicker: () -> Unit,
    onShapeStrokeColorSelected: (Int) -> Unit,
    onShowShapeStrokeColorPicker: () -> Unit,
    onShapeStrokeWidthChange: (Float) -> Unit,
    onFilterSelected: (String) -> Unit,
    onEditSpeechBubble: (StickerEditorLayer) -> Unit
) {
    val panelColor = Color(0xFF29292D)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .background(panelColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (currentTool) {
            "Move" -> {
                if (activeLayer in 0..layers.lastIndex && layers[activeLayer].isSpeechBubble) {
                    val activeBubbleLayer = layers[activeLayer]
                    FilterChip(
                        selected = true,
                        onClick = { onEditSpeechBubble(activeBubbleLayer) },
                        label = { Text("✏️ Edit Speech Bubble", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = editorRed, labelColor = Color.White)
                    )
                }
            }

            "Outline" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Outline Size ${outlineThickness.toInt()}px", fontSize = 11.sp, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Slider(
                            value = outlineThickness,
                            onValueChange = onOutlineThicknessChange,
                            valueRange = 4f..40f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = onApplyOutline,
                            colors = ButtonDefaults.buttonColors(containerColor = editorRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Apply", fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color: ", fontSize = 11.sp, color = Color.LightGray)
                        palette.forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (outlineColorIndex == idx) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { onOutlineColorIndexChange(idx) }
                            )
                        }
                    }
                }
            }

            "Eraser" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Eraser Size ${brushSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Slider(
                        value = brushSize,
                        onValueChange = onBrushSizeChange,
                        valueRange = 4f..60f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            "Brush" -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Brush Size ${brushSize.toInt()}dp", fontSize = 11.sp, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Slider(
                            value = brushSize,
                            onValueChange = onBrushSizeChange,
                            valueRange = 4f..60f,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Color:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        OutlinedButton(
                            onClick = onShowBrushColorPicker,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2A2438))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(brushColorInt))
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("🎨 Custom", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        palette.forEach { color ->
                            val isSelected = brushColorInt == color
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color(0xFF8B5CF6) else Color.Gray, CircleShape)
                                    .clickable { onBrushColorIntChange(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text("✓", color = if (color == AndroidColor.WHITE || color == AndroidColor.YELLOW) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "Shapes" -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Shape:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        listOf("Rectangle", "Circle", "Heart", "Star", "Rounded Rect", "Frame").forEach { shape ->
                            FilterChip(
                                selected = selectedShape == shape,
                                onClick = { onShapeTypeSelected(shape) },
                                label = { Text(shape, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fill:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        FilterChip(
                            selected = isShapeFilled,
                            onClick = { onShapeFillTypeChange(true) },
                            label = { Text("Solid Fill", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = !isShapeFilled,
                            onClick = { onShapeFillTypeChange(false) },
                            label = { Text("Unfill (Transparent)", fontSize = 11.sp) }
                        )

                        if (isShapeFilled) {
                            Spacer(Modifier.width(4.dp))
                            Text("Fill Color:", fontSize = 11.sp, color = Color.White)

                            OutlinedButton(
                                onClick = onShowShapeFillColorPicker,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2A2438))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(shapeFillColorInt))
                                        .border(1.dp, Color.White, CircleShape)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("🎨 Custom", fontSize = 10.sp, color = Color.White)
                            }

                            palette.forEach { color ->
                                val isSel = shapeFillColorInt == color
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(if (isSel) 2.dp else 1.dp, if (isSel) Color(0xFF8B5CF6) else Color.Gray, CircleShape)
                                        .clickable { onShapeFillColorSelected(color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Text("✓", color = if (color == AndroidColor.WHITE || color == AndroidColor.YELLOW) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Outline:", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)

                        OutlinedButton(
                            onClick = onShowShapeStrokeColorPicker,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2A2438))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(shapeStrokeColorInt))
                                    .border(1.dp, Color.White, CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("🎨 Custom", fontSize = 10.sp, color = Color.White)
                        }

                        palette.forEach { color ->
                            val isSel = shapeStrokeColorInt == color
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(if (isSel) 2.dp else 1.dp, if (isSel) Color(0xFF8B5CF6) else Color.Gray, CircleShape)
                                    .clickable { onShapeStrokeColorSelected(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Text("✓", color = if (color == AndroidColor.WHITE || color == AndroidColor.YELLOW) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Outline Thickness ${shapeStrokeWidthPx.toInt()}px", fontSize = 11.sp, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Slider(
                            value = shapeStrokeWidthPx,
                            onValueChange = onShapeStrokeWidthChange,
                            valueRange = 2f..48f,
                            modifier = Modifier.weight(1f)
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
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerEditorZoomResetRow(
    panelColor: Color,
    onZoomIn: () -> Unit,
    onReset: () -> Unit,
    onZoomOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = panelColor,
            border = BorderStroke(1.dp, Color(0xFF383840))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clickable { onZoomIn() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("+", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF383840)))

                Box(
                    modifier = Modifier
                        .clickable { onReset() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Reset", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF383840)))

                Box(
                    modifier = Modifier
                        .clickable { onZoomOut() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("−", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StickerEditorToolBar(
    pagerState: PagerState,
    toolPages: List<List<EditorToolSpec?>>,
    currentTool: String,
    isCustomizeMode: Boolean,
    draggedToolName: String?,
    dragOffset: Offset,
    customToolOrder: SnapshotStateList<String>,
    defaultToolOrder: List<String>,
    prefs: android.content.SharedPreferences,
    context: Context,
    editorRed: Color,
    panelColor: Color,
    scope: CoroutineScope,
    onToolClick: (EditorToolSpec) -> Unit,
    onCustomizeModeChange: (Boolean) -> Unit,
    onNoticeChange: (String) -> Unit,
    onDraggedToolNameChange: (String?) -> Unit,
    onDragOffsetChange: (Offset) -> Unit,
    onLastPageSwitchTimeChange: (Long) -> Unit,
    lastPageSwitchTime: Long
) {
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
                        val spec = pageTools.getOrNull(toolIdx)

                        if (spec != null) {
                            val isSelected = currentTool == spec.name
                            val isBeingDragged = spec.name == draggedToolName

                            Surface(
                                onClick = {
                                    if (!isCustomizeMode) {
                                        onToolClick(spec)
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
                                                    onDraggedToolNameChange(spec.name)
                                                    onDragOffsetChange(Offset.Zero)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val newOffset = dragOffset + dragAmount
                                                    onDragOffsetChange(newOffset)
                                                    val currentToolName = draggedToolName ?: return@detectDragGesturesAfterLongPress
                                                    val currentIdx = customToolOrder.indexOf(currentToolName)
                                                    if (currentIdx == -1) return@detectDragGesturesAfterLongPress

                                                    val itemWidthPx = (size.width.toFloat()).coerceAtLeast(1f)
                                                    val itemHeightPx = 42.dp.toPx()
                                                    val colShift = (newOffset.x / itemWidthPx).toInt()
                                                    val rowShift = (newOffset.y / itemHeightPx).toInt()
                                                    val targetIdx = (currentIdx + rowShift * 4 + colShift)
                                                        .coerceIn(0, customToolOrder.lastIndex)

                                                    if (targetIdx != currentIdx) {
                                                        val movedItem = customToolOrder.removeAt(currentIdx)
                                                        customToolOrder.add(targetIdx, movedItem)
                                                        onDragOffsetChange(newOffset - Offset(colShift * itemWidthPx, rowShift * itemHeightPx))
                                                    }

                                                    val targetPage = targetIdx / 12
                                                    val now = System.currentTimeMillis()
                                                    if (targetPage != pagerState.currentPage && now - lastPageSwitchTime > 350L) {
                                                        onLastPageSwitchTimeChange(now)
                                                        scope.launch { pagerState.animateScrollToPage(targetPage) }
                                                    }
                                                },
                                                onDragEnd = {
                                                    onDraggedToolNameChange(null)
                                                    onDragOffsetChange(Offset.Zero)
                                                },
                                                onDragCancel = {
                                                    onDraggedToolNameChange(null)
                                                    onDragOffsetChange(Offset.Zero)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) { pageIdx ->
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
                    onCustomizeModeChange(true)
                    onNoticeChange("⚙️ Tool Rearrangement Mode — Long press and drag any tool button!")
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("⚙️ Customize", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        onCustomizeModeChange(false)
                        val savedStr = customToolOrder.joinToString(",")
                        prefs.edit().putString("custom_tool_order_v2", savedStr).apply()
                        Toast.makeText(context, "Tool layout saved!", Toast.LENGTH_SHORT).show()
                        onNoticeChange("Tool layout saved!")
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
                        prefs.edit().remove("custom_tool_order_v2").apply()
                        Toast.makeText(context, "Tool layout reset to default.", Toast.LENGTH_SHORT).show()
                        onNoticeChange("Tool layout reset to default.")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("↺ Reset", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StickerEditorSaveTemplateDialog(
    templateNameInput: String,
    onTemplateNameChange: (String) -> Unit,
    selectedCategoryInput: String,
    onSelectedCategoryChange: (String) -> Unit,
    isPublishedInput: Boolean,
    onIsPublishedChange: (Boolean) -> Unit,
    editorRed: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val templateCats = listOf("Cute", "Love", "Funny", "Reactions", "Meme", "Speech Bubbles")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Template") },
        text = {
            Column {
                Text("Template Name:")
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = templateNameInput,
                    onValueChange = onTemplateNameChange,
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
                            onClick = { onSelectedCategoryChange(cat) },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Visibility Setting:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isPublishedInput,
                        onClick = { onIsPublishedChange(false) },
                        label = { Text("🔒 Private (Only Me)", fontSize = 12.sp) }
                    )

                    FilterChip(
                        selected = isPublishedInput,
                        onClick = { onIsPublishedChange(true) },
                        label = { Text("🌐 Public (Community)", fontSize = 12.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = editorRed)
            ) { Text("Save Template") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StickerEditorRenameLayerDialog(
    initialName: String,
    editorRed: Color,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Layer") },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                label = { Text("Layer Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameInput) },
                colors = ButtonDefaults.buttonColors(containerColor = editorRed)
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StickerEditorLayerPropertiesDialog(
    layer: StickerEditorLayer,
    layerIndex: Int,
    editorRed: Color,
    onAlphaChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onRotate90: () -> Unit,
    onFlipH: () -> Unit,
    onCenter: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Layer Properties: ${layer.name}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Opacity ${(layer.alpha * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = layer.alpha,
                        onValueChange = onAlphaChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = layer.isLocked,
                        onClick = onToggleLock,
                        label = { Text(if (layer.isLocked) "🔒 Locked" else "🔓 Unlocked", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onRotate90,
                        label = { Text("🔄 Rotate 90°", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onFlipH,
                        label = { Text("↔ Flip H", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onCenter,
                        label = { Text("🎯 Center", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onRename,
                        label = { Text("📝 Rename", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onDuplicate,
                        label = { Text("📋 Duplicate", fontSize = 11.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onDelete,
                        label = { Text("🗑 Delete", fontSize = 11.sp, color = editorRed) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = editorRed)
            ) { Text("Done") }
        }
    )
}

@Composable
private fun StickerEditorEmojiDialog(
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Emoji Stamp") },
        text = {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("⭐", "🔥", "💖", "🌸", "🚀", "🎉", "👑", "🐶", "🐱").forEach { emoji ->
                    Text(
                        emoji,
                        fontSize = 32.sp,
                        modifier = Modifier.clickable { onSelectEmoji(emoji) }.padding(4.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun StickerEditorAutoBgDialog(
    previewBgBitmap: Bitmap?,
    bgDialogTab: String,
    onTabChange: (String) -> Unit,
    aiPromptText: String,
    onAiPromptChange: (String) -> Unit,
    isAiGenerating: Boolean,
    onGenerateFreeBg: () -> Unit,
    onGenerateAiBg: () -> Unit,
    onApplyBg: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌌 Auto Background Generator") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = bgDialogTab == "Free", onClick = { onTabChange("Free") }, label = { Text("Free Auto BG") })
                    FilterChip(selected = bgDialogTab == "AI", onClick = { onTabChange("AI") }, label = { Text("AI Prompt (Pollinations)") })
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
                        onClick = onGenerateFreeBg,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🎲 Shuffle Gradient Pattern") }
                } else {
                    OutlinedTextField(
                        value = aiPromptText,
                        onValueChange = onAiPromptChange,
                        label = { Text("AI Background Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onGenerateAiBg,
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
                onClick = onApplyBg,
                enabled = previewBgBitmap != null
            ) { Text("Apply Background") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StickerEditorOnlineBgDialog(
    onlineSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onlineSelectedCategory: String,
    onSelectedCategoryChange: (String) -> Unit,
    isDownloadingOnlineBg: Boolean,
    editorRed: Color,
    onSelectBgItem: (OnlineBgItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🖼️ Online Background Gallery") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Free Unsplash & Pixabay HD backgrounds", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = onlineSearchQuery,
                    onValueChange = onSearchQueryChange,
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
                                onSelectedCategoryChange(cat)
                                onSearchQueryChange("")
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
                                    .clickable { onSelectBgItem(bgItem) },
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
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
