package com.mymoises.mystickerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint

@Composable
fun BackgroundRemovalScreen(
    autoSave: Boolean,
    onStickerSaved: () -> Unit,
    onOpenInEditor: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subjectSegmenter = remember {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .enableForegroundConfidenceMask()
                .build()
        )
    }
    val selfieSegmenter = remember {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }
    var result by remember { mutableStateOf<Bitmap?>(null) }
    var cutout by remember { mutableStateOf<Bitmap?>(null) }
    var outlineColor by remember { mutableIntStateOf(Color.White.toArgb()) }
    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Choose a photo with a subject or person in it.") }
    var eraserEnabled by remember { mutableStateOf(false) }
    var eraserSizeDp by remember { mutableFloatStateOf(32f) }
    var editRevision by remember { mutableIntStateOf(0) }
    var savedStickerPath by remember { mutableStateOf<String?>(null) }
    var originalCutout by remember { mutableStateOf<Bitmap?>(null) }
    val undoHistory = remember { mutableStateListOf<EraserSnapshot>() }

    DisposableEffect(subjectSegmenter, selfieSegmenter) {
        onDispose {
            subjectSegmenter.close()
            selfieSegmenter.close()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                isWorking = true
                result = null
                cutout = null
                originalCutout = null
                undoHistory.clear()
                eraserEnabled = false
                savedStickerPath = null
                message = "Removing background from camera photo…"
                try {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    var transparentCutout: Bitmap? = null
                    var foundSubject = false

                    try {
                        val subjectResult = subjectSegmenter.processSuspending(inputImage)
                        val fgBitmap = subjectResult.foregroundBitmap
                        if (fgBitmap != null) {
                            transparentCutout = fgBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            foundSubject = true
                        } else {
                            val maskBuffer = subjectResult.foregroundConfidenceMask
                            if (maskBuffer != null) {
                                transparentCutout = applyConfidenceMaskBuffer(bitmap, maskBuffer, bitmap.width, bitmap.height)
                                foundSubject = true
                            }
                        }
                    } catch (_: Exception) {
                        runCatching {
                            val mask = selfieSegmenter.processSuspending(inputImage)
                            val (selfieCutout, foundPerson) = applySegmentationMask(bitmap, mask)
                            if (foundPerson) {
                                transparentCutout = selfieCutout
                                foundSubject = true
                            }
                        }
                    }

                    val cutoutBitmap = transparentCutout
                    if (!foundSubject || cutoutBitmap == null) {
                        result = null
                        cutout = null
                        message = "No subject detected in camera photo. Choose or take another picture."
                    } else {
                        cutout = cutoutBitmap
                        originalCutout = cutoutBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        undoHistory.clear()
                        val transparent = withContext(Dispatchers.Default) {
                            addStickerOutline(cutoutBitmap, outlineColor)
                        }
                        result = transparent
                        if (autoSave) {
                            val savedPath = saveStickerResult(context, transparent, null)
                            if (savedPath != null) {
                                savedStickerPath = savedPath
                                onStickerSaved()
                                message = "Background removed and sticker saved."
                                Toast.makeText(context, "Transparent sticker saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                message = "Background removed. Tap Save Sticker to save it."
                            }
                        } else {
                            message = "Background removed. Preview your sticker, then save it."
                        }
                    }
                } catch (error: Exception) {
                    message = error.message ?: "Could not remove background."
                } finally {
                    isWorking = false
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                result = null
                cutout = null
                originalCutout = null
                undoHistory.clear()
                eraserEnabled = false
                savedStickerPath = null
                message = "Removing background with ML Kit Subject Segmentation…"
                try {
                    val input = withContext(Dispatchers.IO) { decodeForSticker(context, uri) }
                    val inputImage = InputImage.fromBitmap(input, 0)
                    var transparentCutout: Bitmap? = null
                    var foundSubject = false

                    try {
                        val subjectResult = subjectSegmenter.processSuspending(inputImage)
                        val fgBitmap = subjectResult.foregroundBitmap
                        if (fgBitmap != null) {
                            transparentCutout = fgBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            foundSubject = true
                        } else {
                            val maskBuffer = subjectResult.foregroundConfidenceMask
                            if (maskBuffer != null) {
                                transparentCutout = applyConfidenceMaskBuffer(input, maskBuffer, input.width, input.height)
                                foundSubject = true
                            }
                        }
                    } catch (_: Exception) {
                        // Fallback to Selfie Segmenter
                        runCatching {
                            val mask = selfieSegmenter.processSuspending(inputImage)
                            val (selfieCutout, foundPerson) = applySegmentationMask(input, mask)
                            if (foundPerson) {
                                transparentCutout = selfieCutout
                                foundSubject = true
                            }
                        }
                    }

                    val cutoutBitmap = transparentCutout
                    if (!foundSubject || cutoutBitmap == null) {
                        result = null
                        cutout = null
                        message = "No subject detected. Choose a photo with a clearer subject."
                    } else {
                        cutout = cutoutBitmap
                        originalCutout = cutoutBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        undoHistory.clear()
                        val transparent = withContext(Dispatchers.Default) {
                            addStickerOutline(cutoutBitmap, outlineColor)
                        }
                        result = transparent
                        if (autoSave) {
                            val savedPath = saveStickerResult(context, transparent, null)
                            if (savedPath != null) {
                                savedStickerPath = savedPath
                                onStickerSaved()
                                message = "Background removed and sticker saved."
                                Toast.makeText(context, "Transparent sticker saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                message = "Background removed. Tap Save Sticker to save it."
                            }
                        } else {
                            message = "Background removed. Preview your sticker, then save it."
                        }
                    }
                } catch (error: Exception) {
                    message = error.message ?: "Could not remove the background. Try another photo."
                } finally {
                    isWorking = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✂️ Remove Background", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Cut out the person, erase leftover areas, and save your sticker.")
        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp)
                .background(Color(0xFFEDEBF2), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isWorking -> CircularProgressIndicator()
                result != null -> StickerEraserPreview(
                    bitmap = result!!,
                    cutout = cutout!!,
                    eraserEnabled = eraserEnabled,
                    eraserSizeDp = eraserSizeDp,
                    revision = editRevision,
                    onEraseStart = {
                        val currentResult = result
                        val currentCutout = cutout
                        if (currentResult != null && currentCutout != null) {
                            undoHistory.add(
                                EraserSnapshot(
                                    currentResult.copy(Bitmap.Config.ARGB_8888, true),
                                    currentCutout.copy(Bitmap.Config.ARGB_8888, true)
                                )
                            )
                            if (undoHistory.size > 12) undoHistory.removeAt(0)
                        }
                    },
                    onErase = {
                        editRevision++
                        message = "Eraser edit ready. Tap Save Sticker to update the saved PNG."
                    },
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
                else -> Text("Your cutout preview will appear here.")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        result?.let { bitmap ->
            Text("Sticker outline color", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                listOf(
                    "White" to Color.White.toArgb(),
                    "Black" to Color.Black.toArgb(),
                    "Red" to Color(0xFFE53935).toArgb(),
                    "Blue" to Color(0xFF1E88E5).toArgb(),
                    "Yellow" to Color(0xFFFFD600).toArgb()
                ).forEach { (name, argb) ->
                    Box(
                        modifier = Modifier.size(38.dp)
                            .border(2.dp, if (outlineColor == argb) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                            .padding(4.dp)
                            .background(Color(argb), CircleShape)
                            .clickable(enabled = !isWorking) {
                                val source = cutout ?: return@clickable
                                outlineColor = argb
                                scope.launch {
                                    isWorking = true
                                    try {
                                        val recolored = withContext(Dispatchers.Default) { addStickerOutline(source, argb) }
                                        result = recolored
                                        editRevision++
                                        message = "$name outline selected. Tap Save Sticker Changes to keep it."
                                    } catch (error: Exception) {
                                        message = error.message ?: "Could not change the outline color."
                                    } finally {
                                        isWorking = false
                                    }
                                }
                            }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    eraserEnabled = !eraserEnabled
                    message = if (eraserEnabled) {
                        "Drag on the sticker to erase. Use two fingers to move or zoom."
                    } else {
                        "Drag to move the sticker. Pinch or use − / + to zoom."
                    }
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (eraserEnabled) "✏️ Eraser on · tap to move / zoom" else "✏️ Manual Eraser") }

            if (eraserEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✏️ Eraser size: ${eraserSizeDp.toInt()} dp", modifier = Modifier.weight(1f))
                }
                Slider(
                    value = eraserSizeDp,
                    onValueChange = { eraserSizeDp = it },
                    valueRange = 8f..80f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (undoHistory.isNotEmpty()) {
                            val snapshot = undoHistory.removeAt(undoHistory.lastIndex)
                            result = snapshot.result
                            cutout = snapshot.cutout
                            editRevision++
                            message = "Undo complete. The last erase stroke was restored."
                        }
                    },
                    enabled = undoHistory.isNotEmpty() && !isWorking,
                    modifier = Modifier.weight(1f)
                ) { Text("↶ Undo") }
                OutlinedButton(
                    onClick = {
                        val original = originalCutout
                        if (original != null) {
                            val restored = original.copy(Bitmap.Config.ARGB_8888, true)
                            cutout = restored
                            result = addStickerOutline(restored, outlineColor)
                            undoHistory.clear()
                            editRevision++
                            message = "Original cutout restored."
                        }
                    },
                    enabled = originalCutout != null && !isWorking,
                    modifier = Modifier.weight(1f)
                ) { Text("Restore Original") }
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    savedStickerPath?.let { path ->
                        runCatching { File(path).delete() }
                    }
                    savedStickerPath = null
                    result = null
                    cutout = null
                    originalCutout = null
                    undoHistory.clear()
                    eraserEnabled = false
                    editRevision++
                    onStickerSaved()
                    message = "Photo removed from the editor. Choose a photo to start again."
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete Photo") }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val savedPath = saveStickerResult(context, bitmap, savedStickerPath)
                    if (savedPath != null) {
                        savedStickerPath = savedPath
                        onStickerSaved()
                        message = "Sticker saved to My Stickers."
                        Toast.makeText(context, "Sticker saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        message = "Unable to save the sticker. Please try again."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (savedStickerPath == null) "Save Sticker as PNG" else "Save Sticker Changes") }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val transparentBitmap = cutout ?: bitmap
                    onOpenInEditor(transparentBitmap)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("🎨 Go to Edit Sticker") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { cameraLauncher.launch(null) },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text("📸 Camera") }

            Button(
                onClick = { imagePicker.launch("image/*") },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text("🖼️ + Gallery") }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("← Back to Create") }
    }
}

private fun saveStickerResult(context: Context, bitmap: Bitmap, existingPath: String?): String? {
    if (existingPath != null) {
        val existingFile = File(existingPath)
        if (existingFile.isFile) {
            return runCatching {
                FileOutputStream(existingFile, false).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                }
                existingFile.absolutePath
            }.getOrNull()
        }
    }
    // Use the same private app folder that Home reads through loadMyStickerFiles().
    if (!saveMyStickerBitmap(context, bitmap)) return null
    return loadMyStickerFiles(context).firstOrNull()?.absolutePath
}

private data class EraserSnapshot(val result: Bitmap, val cutout: Bitmap)

@Composable
private fun StickerEraserPreview(
    bitmap: Bitmap,
    cutout: Bitmap,
    eraserEnabled: Boolean,
    eraserSizeDp: Float,
    revision: Int,
    onEraseStart: () -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap, revision) { bitmap.asImageBitmap() }
    val density = LocalDensity.current.density
    var zoom by remember(cutout) { mutableFloatStateOf(1f) }
    var pan by remember(cutout) { mutableStateOf(Offset.Zero) }
    val clearPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(bitmap, cutout, eraserEnabled, eraserSizeDp) {
                awaitEachGesture {
                    var previousCount = 0
                    var previousSingle = Offset.Zero
                    var previousCentroid = Offset.Zero
                    var previousDistance = 0f
                    var eraseStrokeStarted = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        if (pressed.size >= 2) {
                            val centroid = Offset(
                                pressed.map { it.position.x }.average().toFloat(),
                                pressed.map { it.position.y }.average().toFloat()
                            )
                            val distance = pressed.map { (it.position - centroid).getDistance() }.average().toFloat()
                            if (previousCount >= 2 && previousDistance > 0f) {
                                val zoomFactor = (distance / previousDistance).coerceIn(0.8f, 1.25f)
                                val nextZoom = (zoom * zoomFactor).coerceIn(1f, 5f)
                                val appliedZoom = nextZoom / zoom
                                pan = centroid - (previousCentroid - pan) * appliedZoom
                                zoom = nextZoom
                            }
                            previousCentroid = centroid
                            previousDistance = distance
                            previousCount = pressed.size
                            event.changes.forEach { it.consume() }
                        } else {
                            val point = pressed.first().position
                            if (eraserEnabled) {
                                if (!eraseStrokeStarted) {
                                    onEraseStart()
                                    eraseStrokeStarted = true
                                }
                                val fitScale = minOf(size.width.toFloat() / bitmap.width, size.height.toFloat() / bitmap.height)
                                val scale = fitScale * zoom
                                if (scale > 0f) {
                                    val drawnWidth = bitmap.width * scale
                                    val drawnHeight = bitmap.height * scale
                                    val left = (size.width - drawnWidth) / 2f + pan.x
                                    val top = (size.height - drawnHeight) / 2f + pan.y
                                    val imageX = (point.x - left) / scale
                                    val imageY = (point.y - top) / scale
                                    if (imageX in -100f..bitmap.width.toFloat() + 100f && imageY in -100f..bitmap.height.toFloat() + 100f) {
                                        val radius = (eraserSizeDp * density / 2f) / scale
                                        val strokeWidth = radius * 2f
                                        val strokePaint = AndroidPaint(clearPaint).apply {
                                            style = AndroidPaint.Style.STROKE
                                            strokeCap = AndroidPaint.Cap.ROUND
                                            strokeJoin = AndroidPaint.Join.ROUND
                                            this.strokeWidth = strokeWidth
                                        }
                                        val prevPoint = if (previousCount == 1 && previousSingle != Offset.Zero) previousSingle else point
                                        val prevX = (prevPoint.x - left) / scale
                                        val prevY = (prevPoint.y - top) / scale

                                        if (prevX == imageX && prevY == imageY) {
                                            AndroidCanvas(bitmap).drawCircle(imageX, imageY, radius, clearPaint)
                                            AndroidCanvas(cutout).drawCircle(imageX, imageY, radius, clearPaint)
                                        } else {
                                            AndroidCanvas(bitmap).drawLine(prevX, prevY, imageX, imageY, strokePaint)
                                            AndroidCanvas(cutout).drawLine(prevX, prevY, imageX, imageY, strokePaint)
                                        }
                                        onErase()
                                    }
                                }
                            } else if (previousCount == 1) {
                                pan += point - previousSingle
                            }
                            previousSingle = point
                            previousCount = 1
                            previousDistance = 0f
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
        ) {
            // Checkerboard makes transparent pixels visible in the editor preview.
            val checkerSize = 18.dp.toPx()
            var checkerY = 0f
            var checkerRow = 0
            while (checkerY < size.height) {
                var checkerX = 0f
                var checkerColumn = 0
                while (checkerX < size.width) {
                    val tileWidth = minOf(checkerSize, size.width - checkerX)
                    val tileHeight = minOf(checkerSize, size.height - checkerY)
                    drawRect(
                        color = if ((checkerRow + checkerColumn) % 2 == 0) Color.White else Color(0xFFD6D6D6),
                        topLeft = Offset(checkerX, checkerY),
                        size = androidx.compose.ui.geometry.Size(tileWidth, tileHeight)
                    )
                    checkerX += checkerSize
                    checkerColumn++
                }
                checkerY += checkerSize
                checkerRow++
            }

            val fitScale = minOf(size.width.toFloat() / bitmap.width, size.height.toFloat() / bitmap.height)
            val scale = fitScale * zoom
            val drawWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
            val drawHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            val left = ((size.width - drawWidth) / 2f + pan.x).roundToInt()
            val top = ((size.height - drawHeight) / 2f + pan.y).roundToInt()
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(drawWidth, drawHeight)
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(1f) }) { Text("−") }
            OutlinedButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Reset") }
            OutlinedButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(5f) }) { Text("+") }
        }
    }
}

private suspend fun SubjectSegmenter.processSuspending(input: InputImage): SubjectSegmentationResult =
    suspendCancellableCoroutine { continuation ->
        process(input)
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

private suspend fun Segmenter.processSuspending(input: InputImage): SegmentationMask =
    suspendCancellableCoroutine { continuation ->
        process(input)
            .addOnSuccessListener { mask ->
                if (continuation.isActive) continuation.resume(mask)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

private fun applyConfidenceMaskBuffer(source: Bitmap, buffer: java.nio.FloatBuffer, maskWidth: Int, maskHeight: Int): Bitmap {
    val width = source.width
    val height = source.height
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val confidences = FloatArray(maskWidth * maskHeight)
    buffer.rewind()
    for (i in confidences.indices) {
        if (buffer.hasRemaining()) {
            confidences[i] = buffer.get()
        }
    }

    for (y in 0 until height) {
        val maskY = ((y + 0.5f) * maskHeight / height).toInt().coerceIn(0, maskHeight - 1)
        for (x in 0 until width) {
            val maskX = ((x + 0.5f) * maskWidth / width).toInt().coerceIn(0, maskWidth - 1)
            val index = y * width + x
            val confidence = confidences[maskY * maskWidth + maskX].coerceIn(0f, 1f)
            val matte = ((confidence - 0.25f) / 0.55f).coerceIn(0f, 1f)
            val oldAlpha = sourcePixels[index] ushr 24 and 0xFF
            val alpha = (oldAlpha * matte).toInt().coerceIn(0, 255)
            sourcePixels[index] = (sourcePixels[index] and 0x00FFFFFF) or (alpha shl 24)
        }
    }
    val cutout = Bitmap.createBitmap(sourcePixels, width, height, Bitmap.Config.ARGB_8888)
        .copy(Bitmap.Config.ARGB_8888, true)
    cutout.setHasAlpha(true)
    return cutout
}

private fun decodeForSticker(context: Context, uri: Uri): Bitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longestSide = maxOf(info.size.width, info.size.height)
            if (longestSide <= 0) error("This image has no readable dimensions.")
            val scale = minOf(1f, 1200f / longestSide)
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1)
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = context.contentResolver.openInputStream(uri)
        ?: error("Could not open the selected photo.")
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        error("This photo format is not supported on this Android version. Save it as JPG or PNG and try again.")
    }
    val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
    if (longestSide <= 0) error("The selected file is not a supported image.")
    var sampleSize = 1
    while (longestSide / sampleSize > 1200) sampleSize *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: error("Could not decode this photo. Save it as JPG or PNG and try again.")
}

private fun applySegmentationMask(source: Bitmap, mask: SegmentationMask): Pair<Bitmap, Boolean> {
    val width = source.width
    val height = source.height
    val sourcePixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    val confidences = FloatArray(mask.width * mask.height)
    val buffer: ByteBuffer = mask.buffer
    buffer.rewind()
    for (index in confidences.indices) {
        confidences[index] = buffer.float
    }

    var foregroundPixels = 0
    for (y in 0 until height) {
        val maskY = ((y + 0.5f) * mask.height / height).toInt().coerceIn(0, mask.height - 1)
        for (x in 0 until width) {
            val maskX = ((x + 0.5f) * mask.width / width).toInt().coerceIn(0, mask.width - 1)
            val index = y * width + x
            val confidence = confidences[maskY * mask.width + maskX].coerceIn(0f, 1f)
            if (confidence >= 0.5f) foregroundPixels++
            val matte = ((confidence - 0.25f) / 0.55f).coerceIn(0f, 1f)
            val oldAlpha = sourcePixels[index] ushr 24 and 0xFF
            val alpha = (oldAlpha * matte).toInt().coerceIn(0, 255)
            sourcePixels[index] = (sourcePixels[index] and 0x00FFFFFF) or (alpha shl 24)
        }
    }
    val foregroundCoverage = foregroundPixels.toFloat() / sourcePixels.size
    val cutout = Bitmap.createBitmap(sourcePixels, width, height, Bitmap.Config.ARGB_8888)
        .copy(Bitmap.Config.ARGB_8888, true)
        ?: error("Unable to prepare the cutout for editing.")
    cutout.setHasAlpha(true)
    return cutout to (foregroundCoverage >= 0.002f)
}

suspend fun performAutoBackgroundRemoval(context: Context, bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
    val subjectSegmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .enableForegroundConfidenceMask()
            .build()
    )
    val selfieSegmenter = Segmentation.getClient(
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
    )
    try {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        var transparentCutout: Bitmap? = null

        try {
            val subjectResult = subjectSegmenter.processSuspending(inputImage)
            val fgBitmap = subjectResult.foregroundBitmap
            if (fgBitmap != null) {
                transparentCutout = fgBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                val maskBuffer = subjectResult.foregroundConfidenceMask
                if (maskBuffer != null) {
                    transparentCutout = applyConfidenceMaskBuffer(bitmap, maskBuffer, bitmap.width, bitmap.height)
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        if (transparentCutout == null) {
            runCatching {
                val mask = selfieSegmenter.processSuspending(inputImage)
                val (selfieCutout, foundPerson) = applySegmentationMask(bitmap, mask)
                if (foundPerson) {
                    transparentCutout = selfieCutout
                }
            }
        }

        transparentCutout
    } finally {
        subjectSegmenter.close()
        selfieSegmenter.close()
    }
}

fun addStickerOutline(cutout: Bitmap, outlineColor: Int): Bitmap {
    val width = cutout.width
    val height = cutout.height
    val cutoutPixels = IntArray(width * height)
    cutout.getPixels(cutoutPixels, 0, width, 0, 0, width, height)
    val radius = (maxOf(width, height) * 0.008f).roundToInt().coerceIn(3, 12)
    val core = BooleanArray(cutoutPixels.size) { index ->
        (cutoutPixels[index] ushr 24 and 0xFF) >= 96
    }
    val horizontalDilation = BooleanArray(cutoutPixels.size)
    val outlined = BooleanArray(cutoutPixels.size)
    val prefix = IntArray(maxOf(width, height) + 1)

    for (y in 0 until height) {
        prefix[0] = 0
        for (x in 0 until width) {
            prefix[x + 1] = prefix[x] + if (core[y * width + x]) 1 else 0
        }
        for (x in 0 until width) {
            val left = (x - radius).coerceAtLeast(0)
            val right = (x + radius + 1).coerceAtMost(width)
            horizontalDilation[y * width + x] = prefix[right] > prefix[left]
        }
    }

    for (x in 0 until width) {
        prefix[0] = 0
        for (y in 0 until height) {
            prefix[y + 1] = prefix[y] + if (horizontalDilation[y * width + x]) 1 else 0
        }
        for (y in 0 until height) {
            val top = (y - radius).coerceAtLeast(0)
            val bottom = (y + radius + 1).coerceAtMost(height)
            outlined[y * width + x] = prefix[bottom] > prefix[top]
        }
    }

    val resultPixels = IntArray(cutoutPixels.size)
    for (index in cutoutPixels.indices) {
        if (!outlined[index]) continue
        val pixel = cutoutPixels[index]
        val alpha = pixel ushr 24 and 0xFF
        val red = pixel ushr 16 and 0xFF
        val green = pixel ushr 8 and 0xFF
        val blue = pixel and 0xFF
        val inverseAlpha = 255 - alpha
        val outlineRed = outlineColor ushr 16 and 0xFF
        val outlineGreen = outlineColor ushr 8 and 0xFF
        val outlineBlue = outlineColor and 0xFF
        val outRed = (red * alpha + outlineRed * inverseAlpha + 127) / 255
        val outGreen = (green * alpha + outlineGreen * inverseAlpha + 127) / 255
        val outBlue = (blue * alpha + outlineBlue * inverseAlpha + 127) / 255
        resultPixels[index] = (0xFF shl 24) or (outRed shl 16) or (outGreen shl 8) or outBlue
    }
    val rendered = Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
    return rendered.copy(Bitmap.Config.ARGB_8888, true)?.apply { setHasAlpha(true) }
        ?: error("Unable to prepare the sticker for editing.")
}

