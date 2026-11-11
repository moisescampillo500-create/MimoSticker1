package com.mymoises.mystickerapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mymoises.mystickerapp.ui.components.Checkerboard
import com.mymoises.mystickerapp.util.calculateVisibleBounds
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun StickerCropFullScreen(
    bitmap: Bitmap,
    onCropApplied: (croppedBmp: Bitmap, pLeft: Int, pTop: Int, pWidth: Int, pHeight: Int, sourceW: Int, sourceH: Int) -> Unit,
    onCancel: () -> Unit
) {
    val density = LocalDensity.current
    val primaryBlue = Color(0xFF1685F8)
    val darkBackground = Color(0xFF0F0F12)

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }

    var selectedRatio by remember { mutableStateOf("Free") }

    val initialBounds = remember(bitmap) {
        val b = calculateVisibleBounds(bitmap)
        if (b.width() > 0 && b.height() > 0) {
            val l = (b.left / bitmap.width.toFloat()).coerceIn(0f, 0.9f)
            val t = (b.top / bitmap.height.toFloat()).coerceIn(0f, 0.9f)
            val r = (b.right / bitmap.width.toFloat()).coerceIn(l + 0.1f, 1f)
            val bot = (b.bottom / bitmap.height.toFloat()).coerceIn(t + 0.1f, 1f)
            floatArrayOf(l, t, r, bot)
        } else {
            floatArrayOf(0f, 0f, 1f, 1f)
        }
    }

    // Normalized crop box [left, top, right, bottom] in 0f..1f
    var cropLeft by remember { mutableFloatStateOf(initialBounds[0]) }
    var cropTop by remember { mutableFloatStateOf(initialBounds[1]) }
    var cropRight by remember { mutableFloatStateOf(initialBounds[2]) }
    var cropBottom by remember { mutableFloatStateOf(initialBounds[3]) }

    var activeHandle by remember { mutableStateOf<String?>(null) }

    // Image pan & zoom state
    var imgScale by remember { mutableFloatStateOf(1.0f) }
    var imgPanX by remember { mutableFloatStateOf(0.0f) }
    var imgPanY by remember { mutableFloatStateOf(0.0f) }

    val currentAspect = remember(bitmap, rotationAngle) {
        val isRotated90 = (rotationAngle.toInt() / 90) % 2 != 0
        val w = if (isRotated90) bitmap.height else bitmap.width
        val h = if (isRotated90) bitmap.width else bitmap.height
        w.toFloat() / h.coerceAtLeast(1).toFloat()
    }

    fun applyRatio(ratioName: String) {
        selectedRatio = ratioName
        if (ratioName == "Free") {
            cropLeft = 0.0f
            cropTop = 0.0f
            cropRight = 1.0f
            cropBottom = 1.0f
            return
        }

        val targetRatio = when (ratioName) {
            "Original" -> currentAspect
            "1:1" -> 1.0f
            "4:3" -> 4f / 3f
            "16:9" -> 16f / 9f
            "3:4" -> 3f / 4f
            "9:16" -> 9f / 16f
            else -> currentAspect
        }

        val normRatio = targetRatio / currentAspect
        if (normRatio <= 1.0f) {
            val normWidth = normRatio
            cropLeft = (1.0f - normWidth) / 2f
            cropRight = cropLeft + normWidth
            cropTop = 0.0f
            cropBottom = 1.0f
        } else {
            val normHeight = 1.0f / normRatio
            cropTop = (1.0f - normHeight) / 2f
            cropBottom = cropTop + normHeight
            cropLeft = 0.0f
            cropRight = 1.0f
        }
    }

    fun rotate90() {
        rotationAngle = (rotationAngle + 90f) % 360f
        applyRatio(selectedRatio)
    }

    fun flipHorizontal() {
        isFlippedHorizontal = !isFlippedHorizontal
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = darkBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TOP TOOLBAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF18181C))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Text("✕", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Crop Picture",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            val matrix = Matrix()
                            if (rotationAngle != 0f) {
                                matrix.postRotate(rotationAngle)
                            }
                            if (isFlippedHorizontal) {
                                matrix.postScale(-1f, 1f)
                            }

                            val transformedBmp = if (rotationAngle != 0f || isFlippedHorizontal) {
                                Bitmap.createBitmap(
                                    bitmap,
                                    0, 0,
                                    bitmap.width, bitmap.height,
                                    matrix,
                                    true
                                )
                            } else {
                                bitmap
                            }

                            val pLeft = (cropLeft * transformedBmp.width).roundToInt().coerceIn(0, transformedBmp.width - 1)
                            val pTop = (cropTop * transformedBmp.height).roundToInt().coerceIn(0, transformedBmp.height - 1)
                            val pWidth = ((cropRight - cropLeft) * transformedBmp.width).roundToInt().coerceIn(1, transformedBmp.width - pLeft)
                            val pHeight = ((cropBottom - cropTop) * transformedBmp.height).roundToInt().coerceIn(1, transformedBmp.height - pTop)

                            val croppedBmp = Bitmap.createBitmap(
                                transformedBmp,
                                pLeft,
                                pTop,
                                pWidth,
                                pHeight
                            )

                            onCropApplied(croppedBmp, pLeft, pTop, pWidth, pHeight, transformedBmp.width, transformedBmp.height)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(primaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // MAIN CROP VIEWPORT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Checkerboard(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))

                    var viewportSize by remember { mutableStateOf(Size.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                viewportSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                            }
                    ) {
                        if (viewportSize.width > 0 && viewportSize.height > 0) {
                            val isRotated90 = (rotationAngle.toInt() / 90) % 2 != 0
                            val bmpW = if (isRotated90) bitmap.height.toFloat() else bitmap.width.toFloat()
                            val bmpH = if (isRotated90) bitmap.width.toFloat() else bitmap.height.toFloat()

                            val fitScale = minOf(viewportSize.width / bmpW, viewportSize.height / bmpH)
                            val displayedW = bmpW * fitScale
                            val displayedH = bmpH * fitScale
                            val imgOffsetX = (viewportSize.width - displayedW) / 2f
                            val imgOffsetY = (viewportSize.height - displayedH) / 2f

                            // Display Image Container
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = imgScale
                                        scaleY = imgScale
                                        translationX = imgPanX
                                        translationY = imgPanY
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Crop Image Target",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationZ = rotationAngle
                                            scaleX = if (isFlippedHorizontal) -1f else 1f
                                        }
                                )
                            }

                            // Interactive Crop Overlay Canvas with Unified Multi-Touch & Handle Gestures
                            val touchRadiusPx = with(density) { 38.dp.toPx() }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(selectedRatio, cropLeft, cropTop, cropRight, cropBottom, displayedW, displayedH) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val changes = event.changes

                                                if (changes.size >= 2) {
                                                    // Multi-touch Pinch to Zoom / Pan
                                                    val zoom = event.calculateZoom()
                                                    val pan = event.calculatePan()
                                                    imgScale = (imgScale * zoom).coerceIn(1f, 4f)
                                                    imgPanX += pan.x
                                                    imgPanY += pan.y
                                                    changes.forEach { it.consume() }
                                                } else if (changes.size == 1) {
                                                    // Single-touch Crop Handle Drag or Box Move
                                                    val change = changes[0]
                                                    if (change.pressed) {
                                                        val cLeft = imgOffsetX + cropLeft * displayedW
                                                        val cTop = imgOffsetY + cropTop * displayedH
                                                        val cRight = imgOffsetX + cropRight * displayedW
                                                        val cBottom = imgOffsetY + cropBottom * displayedH

                                                        if (activeHandle == null) {
                                                            val startPos = change.position
                                                            activeHandle = when {
                                                                hypot(startPos.x - cLeft, startPos.y - cTop) <= touchRadiusPx -> "TL"
                                                                hypot(startPos.x - cRight, startPos.y - cTop) <= touchRadiusPx -> "TR"
                                                                hypot(startPos.x - cLeft, startPos.y - cBottom) <= touchRadiusPx -> "BL"
                                                                hypot(startPos.x - cRight, startPos.y - cBottom) <= touchRadiusPx -> "BR"
                                                                abs(startPos.y - cTop) <= touchRadiusPx && startPos.x in (cLeft - touchRadiusPx)..(cRight + touchRadiusPx) -> "T"
                                                                abs(startPos.y - cBottom) <= touchRadiusPx && startPos.x in (cLeft - touchRadiusPx)..(cRight + touchRadiusPx) -> "B"
                                                                abs(startPos.x - cLeft) <= touchRadiusPx && startPos.y in (cTop - touchRadiusPx)..(cBottom + touchRadiusPx) -> "L"
                                                                abs(startPos.x - cRight) <= touchRadiusPx && startPos.y in (cTop - touchRadiusPx)..(cBottom + touchRadiusPx) -> "R"
                                                                startPos.x in cLeft..cRight && startPos.y in cTop..cBottom -> "MOVE"
                                                                else -> "PAN_IMAGE"
                                                            }
                                                        }

                                                        val dragAmount = change.positionChange()
                                                        if (dragAmount != Offset.Zero) {
                                                            change.consume()
                                                            val deltaX = dragAmount.x / displayedW
                                                            val deltaY = dragAmount.y / displayedH
                                                            val minDim = 0.08f

                                                            val targetRatio = when (selectedRatio) {
                                                                "Original" -> currentAspect
                                                                "1:1" -> 1.0f
                                                                "4:3" -> 4f / 3f
                                                                "16:9" -> 16f / 9f
                                                                "3:4" -> 3f / 4f
                                                                "9:16" -> 9f / 16f
                                                                else -> null
                                                            }

                                                            when (activeHandle) {
                                                                "TL" -> {
                                                                    val newL = (cropLeft + deltaX).coerceIn(0f, cropRight - minDim)
                                                                    var newT = (cropTop + deltaY).coerceIn(0f, cropBottom - minDim)
                                                                    if (targetRatio != null) {
                                                                        val normRatio = targetRatio / currentAspect
                                                                        newT = (cropBottom - ((cropRight - newL) / normRatio)).coerceIn(0f, cropBottom - minDim)
                                                                    }
                                                                    cropLeft = newL
                                                                    cropTop = newT
                                                                }
                                                                "TR" -> {
                                                                    val newR = (cropRight + deltaX).coerceIn(cropLeft + minDim, 1f)
                                                                    var newT = (cropTop + deltaY).coerceIn(0f, cropBottom - minDim)
                                                                    if (targetRatio != null) {
                                                                        val normRatio = targetRatio / currentAspect
                                                                        newT = (cropBottom - ((newR - cropLeft) / normRatio)).coerceIn(0f, cropBottom - minDim)
                                                                    }
                                                                    cropRight = newR
                                                                    cropTop = newT
                                                                }
                                                                "BL" -> {
                                                                    val newL = (cropLeft + deltaX).coerceIn(0f, cropRight - minDim)
                                                                    var newB = (cropBottom + deltaY).coerceIn(cropTop + minDim, 1f)
                                                                    if (targetRatio != null) {
                                                                        val normRatio = targetRatio / currentAspect
                                                                        newB = (cropTop + ((cropRight - newL) / normRatio)).coerceIn(cropTop + minDim, 1f)
                                                                    }
                                                                    cropLeft = newL
                                                                    cropBottom = newB
                                                                }
                                                                "BR" -> {
                                                                    val newR = (cropRight + deltaX).coerceIn(cropLeft + minDim, 1f)
                                                                    var newB = (cropBottom + deltaY).coerceIn(cropTop + minDim, 1f)
                                                                    if (targetRatio != null) {
                                                                        val normRatio = targetRatio / currentAspect
                                                                        newB = (cropTop + ((newR - cropLeft) / normRatio)).coerceIn(cropTop + minDim, 1f)
                                                                    }
                                                                    cropRight = newR
                                                                    cropBottom = newB
                                                                }
                                                                "T" -> {
                                                                    cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - minDim)
                                                                }
                                                                "B" -> {
                                                                    cropBottom = (cropBottom + deltaY).coerceIn(cropTop + minDim, 1f)
                                                                }
                                                                "L" -> {
                                                                    cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - minDim)
                                                                }
                                                                "R" -> {
                                                                    cropRight = (cropRight + deltaX).coerceIn(cropLeft + minDim, 1f)
                                                                }
                                                                "MOVE" -> {
                                                                    val w = cropRight - cropLeft
                                                                    val h = cropBottom - cropTop
                                                                    var newL = cropLeft + deltaX
                                                                    var newT = cropTop + deltaY

                                                                    if (newL < 0f) newL = 0f
                                                                    if (newL + w > 1f) newL = 1f - w
                                                                    if (newT < 0f) newT = 0f
                                                                    if (newT + h > 1f) newT = 1f - h

                                                                    cropLeft = newL
                                                                    cropRight = newL + w
                                                                    cropTop = newT
                                                                    cropBottom = newT + h
                                                                }
                                                                "PAN_IMAGE" -> {
                                                                    imgPanX += dragAmount.x
                                                                    imgPanY += dragAmount.y
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        activeHandle = null
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                val cLeft = imgOffsetX + cropLeft * displayedW
                                val cTop = imgOffsetY + cropTop * displayedH
                                val cRight = imgOffsetX + cropRight * displayedW
                                val cBottom = imgOffsetY + cropBottom * displayedH
                                val cW = cRight - cLeft
                                val cH = cBottom - cTop

                                // Dimmed region outside crop rect
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    size = size
                                )

                                // Clear crop box interior
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(cLeft, cTop),
                                    size = Size(cW, cH),
                                    blendMode = BlendMode.Clear
                                )

                                // Main Crop Rectangle Border
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(cLeft, cTop),
                                    size = Size(cW, cH),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // 3x3 Rule-of-Thirds Grid Lines
                                val thirdW = cW / 3f
                                val thirdH = cH / 3f
                                val gridColor = Color.White.copy(alpha = 0.4f)
                                val gridStroke = Stroke(width = 1.dp.toPx())

                                drawLine(gridColor, Offset(cLeft + thirdW, cTop), Offset(cLeft + thirdW, cBottom), gridStroke.width)
                                drawLine(gridColor, Offset(cLeft + thirdW * 2, cTop), Offset(cLeft + thirdW * 2, cBottom), gridStroke.width)
                                drawLine(gridColor, Offset(cLeft, cTop + thirdH), Offset(cRight, cTop + thirdH), gridStroke.width)
                                drawLine(gridColor, Offset(cLeft, cTop + thirdH * 2), Offset(cRight, cTop + thirdH * 2), gridStroke.width)

                                // Corner & Edge Handles
                                val handleRadius = 13.dp.toPx()
                                val cornerLen = 18.dp.toPx()
                                val cornerStroke = Stroke(width = 3.dp.toPx())

                                val corners = listOf(
                                    Offset(cLeft, cTop),
                                    Offset(cRight, cTop),
                                    Offset(cLeft, cBottom),
                                    Offset(cRight, cBottom)
                                )

                                corners.forEach { pos ->
                                    drawCircle(primaryBlue, center = pos, radius = handleRadius)
                                    drawCircle(Color.White, center = pos, radius = handleRadius * 0.45f)
                                }

                                // Corner L-Bracket Accents
                                drawLine(Color.White, Offset(cLeft, cTop), Offset(cLeft + cornerLen, cTop), cornerStroke.width)
                                drawLine(Color.White, Offset(cLeft, cTop), Offset(cLeft, cTop + cornerLen), cornerStroke.width)

                                drawLine(Color.White, Offset(cRight, cTop), Offset(cRight - cornerLen, cTop), cornerStroke.width)
                                drawLine(Color.White, Offset(cRight, cTop), Offset(cRight, cTop + cornerLen), cornerStroke.width)

                                drawLine(Color.White, Offset(cLeft, cBottom), Offset(cLeft + cornerLen, cBottom), cornerStroke.width)
                                drawLine(Color.White, Offset(cLeft, cBottom), Offset(cLeft, cBottom - cornerLen), cornerStroke.width)

                                drawLine(Color.White, Offset(cRight, cBottom), Offset(cRight - cornerLen, cBottom), cornerStroke.width)
                                drawLine(Color.White, Offset(cRight, cBottom), Offset(cRight, cBottom - cornerLen), cornerStroke.width)

                                // Mid-edge Pill Handles
                                val pillW = 20.dp.toPx()
                                val pillH = 4.dp.toPx()

                                // Top edge
                                drawRect(Color.White, topLeft = Offset(cLeft + cW / 2f - pillW / 2f, cTop - pillH / 2f), size = Size(pillW, pillH))
                                // Bottom edge
                                drawRect(Color.White, topLeft = Offset(cLeft + cW / 2f - pillW / 2f, cBottom - pillH / 2f), size = Size(pillW, pillH))
                                // Left edge
                                drawRect(Color.White, topLeft = Offset(cLeft - pillH / 2f, cTop + cH / 2f - pillW / 2f), size = Size(pillH, pillW))
                                // Right edge
                                drawRect(Color.White, topLeft = Offset(cRight - pillH / 2f, cTop + cH / 2f - pillW / 2f), size = Size(pillH, pillW))
                            }
                        }
                    }
                }

                // BOTTOM TOOLBAR
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF18181C)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Free", "Original", "1:1", "4:3", "16:9", "3:4", "9:16").forEach { ratio ->
                                FilterChip(
                                    selected = selectedRatio == ratio,
                                    onClick = { applyRatio(ratio) },
                                    label = { Text(ratio, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF2B2B32),
                                        labelColor = Color.LightGray
                                    )
                                )
                            }

                            // Rotate & Flip Action Chips
                            FilterChip(
                                selected = false,
                                onClick = { rotate90() },
                                label = { Text("Rotate 🔄", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF2B2B32),
                                    labelColor = Color.White
                                )
                            )

                            FilterChip(
                                selected = isFlippedHorizontal,
                                onClick = { flipHorizontal() },
                                label = { Text("Flip ↔", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF2B2B32),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
