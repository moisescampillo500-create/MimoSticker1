package com.mymoises.mystickerapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.mymoises.mystickerapp.FontRegistry
import com.mymoises.mystickerapp.model.StickerEditorLayer

fun resolveTypeface(fontFamily: String, fontStyle: String, context: Context? = null): Typeface {
    return FontRegistry.getOrLoadTypeface(context, fontFamily, fontStyle)
}

fun applySpecialFontEffects(paint: Paint, fontFamily: String) {
    when (fontFamily) {
        "Bebas Neue", "Anton", "Oswald" -> {
            paint.letterSpacing = 0.08f
            paint.strokeJoin = Paint.Join.BEVEL
        }
        "Bangers", "Lilita One", "Bungee" -> {
            paint.textSkewX = -0.15f
            paint.strokeJoin = Paint.Join.ROUND
        }
        "Pacifico", "Great Vibes", "Allura", "Lobster" -> {
            paint.letterSpacing = 0.02f
            paint.textSkewX = -0.10f
        }
        "Special Elite", "Permanent Marker" -> {
            paint.letterSpacing = 0.05f
            paint.strokeWidth = 14f
        }
        "Monoton" -> {
            paint.letterSpacing = 0.12f
            paint.strokeWidth = 18f
        }
        "Alfa Slab One", "Abril Fatface" -> {
            paint.letterSpacing = 0.04f
        }
    }
}

fun createTextStickerBitmapWithTf(
    text: String,
    typeface: Typeface,
    fontFamily: String = "Sans Serif",
    textColorInt: Int = AndroidColor.RED,
    outlineColorInt: Int = AndroidColor.WHITE,
    textSizeSp: Float = 48f,
    textAlignName: String = "Center",
    outlineThicknessPx: Float = 10f,
    hasShadow: Boolean = true,
    letterSpacingEm: Float = 0f,
    bitmapWidth: Int = 512,
    bitmapHeight: Int = 256
): Bitmap {
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val align = when (textAlignName) {
        "Left" -> Paint.Align.LEFT
        "Right" -> Paint.Align.RIGHT
        else -> Paint.Align.CENTER
    }

    val isUppercase = fontFamily in listOf("Bebas Neue", "Anton", "Bungee", "Monoton")
    val displayText = if (isUppercase) text.uppercase() else text

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = align
        this.typeface = typeface
        textSize = (textSizeSp * 1.35f).coerceIn(32f, 110f)
        strokeJoin = Paint.Join.ROUND
        if (letterSpacingEm != 0f) {
            letterSpacing = letterSpacingEm
        }
        applySpecialFontEffects(this, fontFamily)
    }

    val x = when (align) {
        Paint.Align.LEFT -> 24f
        Paint.Align.RIGHT -> bitmapWidth - 24f
        else -> bitmapWidth / 2f
    }

    val lines = displayText.split("\n")
    val lineSpacing = paint.textSize * 1.2f
    val startY = bitmapHeight / 2f - ((lines.size - 1) * lineSpacing / 2f) + (paint.textSize / 3.2f)

    lines.forEachIndexed { idx, line ->
        val y = startY + (idx * lineSpacing)

        if (hasShadow) {
            paint.style = Paint.Style.FILL
            paint.color = AndroidColor.parseColor("#88000000")
            paint.setShadowLayer(8f, 3f, 3f, AndroidColor.parseColor("#88000000"))
            canvas.drawText(line, x + 2f, y + 2f, paint)
            paint.clearShadowLayer()
        }

        if (outlineThicknessPx > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outlineThicknessPx
            paint.color = outlineColorInt
            canvas.drawText(line, x, y, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = textColorInt
        canvas.drawText(line, x, y, paint)
    }

    return bitmap
}

fun createTextStickerBitmap(
    text: String,
    fontFamily: String = "Sans Serif",
    fontStyle: String = "Bold",
    textColorInt: Int = AndroidColor.RED,
    outlineColorInt: Int = AndroidColor.WHITE,
    textSizeSp: Float = 48f,
    textAlignName: String = "Center",
    outlineThicknessPx: Float = 10f,
    hasShadow: Boolean = true,
    letterSpacingEm: Float = 0f,
    bitmapWidth: Int = 512,
    bitmapHeight: Int = 256,
    context: Context? = null
): Bitmap {
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val tf = resolveTypeface(fontFamily, fontStyle, context)
    val align = when (textAlignName) {
        "Left" -> Paint.Align.LEFT
        "Right" -> Paint.Align.RIGHT
        else -> Paint.Align.CENTER
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = align
        typeface = tf
        textSize = textSizeSp * 1.25f
        strokeJoin = Paint.Join.ROUND
        if (letterSpacingEm != 0f) {
            letterSpacing = letterSpacingEm
        }
    }

    val x = when (align) {
        Paint.Align.LEFT -> 32f
        Paint.Align.RIGHT -> bitmapWidth - 32f
        else -> bitmapWidth / 2f
    }

    val lines = text.split("\n")
    val lineSpacing = paint.textSize * 1.25f
    val startY = bitmapHeight / 2f - ((lines.size - 1) * lineSpacing / 2f) + (paint.textSize / 3f)

    lines.forEachIndexed { idx, line ->
        val y = startY + (idx * lineSpacing)

        if (hasShadow) {
            paint.style = Paint.Style.FILL
            paint.color = AndroidColor.parseColor("#88000000")
            paint.setShadowLayer(8f, 3f, 3f, AndroidColor.parseColor("#88000000"))
            canvas.drawText(line, x + 2f, y + 2f, paint)
            paint.clearShadowLayer()
        }

        if (outlineThicknessPx > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outlineThicknessPx
            paint.color = outlineColorInt
            canvas.drawText(line, x, y, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = textColorInt
        canvas.drawText(line, x, y, paint)
    }

    return bitmap
}

fun mergeSelectedLayers(
    layers: SnapshotStateList<StickerEditorLayer>,
    selectedIndicesList: List<Int>
) {
    if (selectedIndicesList.size < 2) return
    val sortedIndices = selectedIndicesList.sorted()
    val targetWidth = 1024
    val targetHeight = 1024

    val mergedBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mergedBmp)

    sortedIndices.forEach { idx ->
        if (idx in 0..layers.lastIndex) {
            val layer = layers[idx]
            if (!layer.bitmap.isRecycled) {
                canvas.save()
                canvas.translate(targetWidth / 2f + layer.offsetX, targetHeight / 2f + layer.offsetY)
                canvas.rotate(layer.rotation)
                canvas.scale(layer.scale, layer.scale)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = (layer.alpha * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawBitmap(
                    layer.bitmap,
                    -layer.bitmap.width / 2f,
                    -layer.bitmap.height / 2f,
                    paint
                )
                canvas.restore()
            }
        }
    }

    val firstIndex = sortedIndices.first()
    for (i in sortedIndices.sortedDescending()) {
        if (i in 0..layers.lastIndex) {
            layers.removeAt(i)
        }
    }

    val newLayer = StickerEditorLayer("Merged Layer", mergedBmp)
    val insertPos = firstIndex.coerceIn(0, layers.size)
    layers.add(insertPos, newLayer)
}

fun createSpeechBubbleBitmap(
    bubbleStyle: String,
    text: String,
    textColorInt: Int,
    textSizeSp: Float,
    fillColorInt: Int,
    strokeColorInt: Int,
    strokeThicknessPx: Float = 10f,
    opacityAlpha: Float = 1.0f,
    tailDirection: String = "Bottom Left",
    fontFamily: String = "Sans Serif",
    fontStyle: String = "Bold",
    textAlignName: String = "Center",
    hasShadow: Boolean = true,
    bitmapSize: Int = 512,
    context: Context? = null
): Bitmap {
    val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val logicalSize = 512f
    val scale = bitmapSize.toFloat() / logicalSize
    canvas.scale(scale, scale)

    val rect = RectF(36f, 36f, 476f, 400f)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColorInt
        alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeThicknessPx
        color = strokeColorInt
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
    }

    val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeThicknessPx + 18f
        color = AndroidColor.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
    }

    val bodyPath = Path()
    val tailPath = Path()

    fun addTail(p: Path, r: RectF) {
        when (tailDirection) {
            "Bottom Left" -> {
                p.moveTo(r.left + 80f, r.bottom - 4f)
                p.lineTo(r.left + 30f, r.bottom + 55f)
                p.lineTo(r.left + 150f, r.bottom - 4f)
            }
            "Bottom Right" -> {
                p.moveTo(r.right - 150f, r.bottom - 4f)
                p.lineTo(r.right - 30f, r.bottom + 55f)
                p.lineTo(r.right - 80f, r.bottom - 4f)
            }
            "Top Left" -> {
                p.moveTo(r.left + 80f, r.top + 4f)
                p.lineTo(r.left + 30f, r.top - 30f)
                p.lineTo(r.left + 150f, r.top + 4f)
            }
            "Top Right" -> {
                p.moveTo(r.right - 150f, r.top + 4f)
                p.lineTo(r.right - 30f, r.top - 30f)
                p.lineTo(r.right - 80f, r.top + 4f)
            }
        }
        p.close()
    }

    when (bubbleStyle) {
        "Classic Comic Bubble", "Rounded Rectangle", "Rounded Rectangle Speech Bubble" -> {
            bodyPath.addRoundRect(rect, 44f, 44f, Path.Direction.CW)
            addTail(tailPath, rect)
        }
        "Cloud Thought Bubble", "Cloud / Thought", "Cloud / Thought Bubble", "Thought Bubble" -> {
            bodyPath.addOval(RectF(70f, 40f, 442f, 360f), Path.Direction.CW)
            bodyPath.addCircle(120f, 130f, 65f, Path.Direction.CW)
            bodyPath.addCircle(390f, 130f, 65f, Path.Direction.CW)
            bodyPath.addCircle(150f, 290f, 60f, Path.Direction.CW)
            bodyPath.addCircle(360f, 290f, 60f, Path.Direction.CW)
            bodyPath.addCircle(256f, 80f, 70f, Path.Direction.CW)

            val dot1 = Path().apply { addCircle(110f, 395f, 22f, Path.Direction.CW) }
            val dot2 = Path().apply { addCircle(75f, 435f, 15f, Path.Direction.CW) }
            val dot3 = Path().apply { addCircle(50f, 465f, 9f, Path.Direction.CW) }

            canvas.drawPath(dot1, whiteBorderPaint)
            canvas.drawPath(dot1, fillPaint)
            canvas.drawPath(dot1, strokePaint)
            canvas.drawPath(dot2, whiteBorderPaint)
            canvas.drawPath(dot2, fillPaint)
            canvas.drawPath(dot2, strokePaint)
            canvas.drawPath(dot3, whiteBorderPaint)
            canvas.drawPath(dot3, fillPaint)
            canvas.drawPath(dot3, strokePaint)
        }
        "Comic Explosion" -> {
            val cx = rect.centerX()
            val cy = rect.centerY()
            val rx = rect.width() / 2f
            val ry = rect.height() / 2f
            val points = 16
            for (i in 0 until points) {
                val angle = Math.toRadians(i * 360.0 / points)
                val rX = if (i % 2 == 0) rx else rx * 0.70f
                val rY = if (i % 2 == 0) ry else ry * 0.70f
                val px = cx + (rX * Math.cos(angle)).toFloat()
                val py = cy + (rY * Math.sin(angle)).toFloat()
                if (i == 0) bodyPath.moveTo(px, py) else bodyPath.lineTo(px, py)
            }
            bodyPath.close()
        }
        "Shout Bubble", "Spiky Burst", "Spiky Speech Bubble" -> {
            val cx = rect.centerX()
            val cy = rect.centerY()
            val rx = rect.width() / 2f
            val ry = rect.height() / 2f
            val points = 24
            for (i in 0 until points) {
                val angle = Math.toRadians(i * 360.0 / points)
                val rX = if (i % 2 == 0) rx else rx * 0.55f
                val rY = if (i % 2 == 0) ry else ry * 0.55f
                val px = cx + (rX * Math.cos(angle)).toFloat()
                val py = cy + (rY * Math.sin(angle)).toFloat()
                if (i == 0) bodyPath.moveTo(px, py) else bodyPath.lineTo(px, py)
            }
            bodyPath.close()
        }
        "Oval Dialogue Bubble", "Oval Speech Bubble" -> {
            bodyPath.addOval(rect, Path.Direction.CW)

            val dot1 = Path().apply { addCircle(110f, 395f, 22f, Path.Direction.CW) }
            val dot2 = Path().apply { addCircle(75f, 435f, 15f, Path.Direction.CW) }
            val dot3 = Path().apply { addCircle(50f, 465f, 9f, Path.Direction.CW) }

            canvas.drawPath(dot1, whiteBorderPaint)
            canvas.drawPath(dot1, fillPaint)
            canvas.drawPath(dot1, strokePaint)
            canvas.drawPath(dot2, whiteBorderPaint)
            canvas.drawPath(dot2, fillPaint)
            canvas.drawPath(dot2, strokePaint)
            canvas.drawPath(dot3, whiteBorderPaint)
            canvas.drawPath(dot3, fillPaint)
            canvas.drawPath(dot3, strokePaint)
        }
        "Heart Bubble", "Heart Speech Bubble" -> {
            val cx = rect.centerX()
            val cy = rect.centerY() - 10f
            val r = rect.width() / 2.2f
            bodyPath.moveTo(cx, cy + r)
            bodyPath.cubicTo(cx - r * 1.2f, cy + r * 0.2f, cx - r * 1.2f, cy - r * 0.8f, cx, cy - r * 0.3f)
            bodyPath.cubicTo(cx + r * 1.2f, cy - r * 0.8f, cx + r * 1.2f, cy + r * 0.2f, cx, cy + r)
            bodyPath.close()
            addTail(tailPath, rect)
        }
        "Pixel Bubble", "Pixel Speech Bubble" -> {
            bodyPath.addRect(rect.left + 16f, rect.top + 16f, rect.right - 16f, rect.bottom - 16f, Path.Direction.CW)
            tailPath.addRect(rect.left + 60f, rect.bottom - 16f, rect.left + 110f, rect.bottom, Path.Direction.CW)
            tailPath.addRect(rect.left + 40f, rect.bottom, rect.left + 90f, rect.bottom + 25f, Path.Direction.CW)
            tailPath.addRect(rect.left + 20f, rect.bottom + 25f, rect.left + 60f, rect.bottom + 50f, Path.Direction.CW)
        }
        "Double Dialogue Bubble", "Double Bubble", "Double Speech Bubble" -> {
            val leftRect = RectF(rect.left, rect.top + 20f, rect.right - 100f, rect.bottom - 20f)
            val rightRect = RectF(rect.right - 150f, rect.top, rect.right, rect.bottom - 60f)

            bodyPath.addRoundRect(leftRect, 36f, 36f, Path.Direction.CW)
            val leftTail = Path().apply {
                moveTo(leftRect.left + 60f, leftRect.bottom - 4f)
                lineTo(leftRect.left + 20f, leftRect.bottom + 45f)
                lineTo(leftRect.left + 120f, leftRect.bottom - 4f)
                close()
            }
            bodyPath.op(leftTail, Path.Op.UNION)

            val rightBubble = Path().apply {
                addRoundRect(rightRect, 28f, 28f, Path.Direction.CW)
                val rt = Path().apply {
                    moveTo(rightRect.right - 50f, rightRect.bottom - 4f)
                    lineTo(rightRect.right - 15f, rightRect.bottom + 35f)
                    lineTo(rightRect.right - 90f, rightRect.bottom - 4f)
                    close()
                }
                op(rt, Path.Op.UNION)
            }
            bodyPath.op(rightBubble, Path.Op.UNION)
        }
        "Ribbon Bubble", "Ribbon Speech Bubble" -> {
            bodyPath.addRect(rect.left + 40f, rect.top + 20f, rect.right - 40f, rect.bottom - 20f, Path.Direction.CW)
            val leftRibbon = Path().apply {
                moveTo(rect.left + 40f, rect.top + 20f)
                lineTo(rect.left, rect.top + 40f)
                lineTo(rect.left + 25f, rect.centerY())
                lineTo(rect.left, rect.bottom - 20f)
                lineTo(rect.left + 40f, rect.bottom - 20f)
                close()
            }
            val rightRibbon = Path().apply {
                moveTo(rect.right - 40f, rect.top + 20f)
                lineTo(rect.right, rect.top + 40f)
                lineTo(rect.right - 25f, rect.centerY())
                lineTo(rect.right, rect.bottom - 20f)
                lineTo(rect.right - 40f, rect.bottom - 20f)
                close()
            }
            bodyPath.op(leftRibbon, Path.Op.UNION)
            bodyPath.op(rightRibbon, Path.Op.UNION)
            addTail(tailPath, rect)
        }
        "Question Bubble" -> {
            bodyPath.addOval(rect, Path.Direction.CW)
            addTail(tailPath, rect)
        }
        "Exclamation Bubble" -> {
            bodyPath.addRoundRect(rect, 44f, 44f, Path.Direction.CW)

            val dot1 = Path().apply { addCircle(110f, 395f, 22f, Path.Direction.CW) }
            val dot2 = Path().apply { addCircle(75f, 435f, 15f, Path.Direction.CW) }
            val dot3 = Path().apply { addCircle(50f, 465f, 9f, Path.Direction.CW) }

            canvas.drawPath(dot1, whiteBorderPaint)
            canvas.drawPath(dot1, fillPaint)
            canvas.drawPath(dot1, strokePaint)
            canvas.drawPath(dot2, whiteBorderPaint)
            canvas.drawPath(dot2, fillPaint)
            canvas.drawPath(dot2, strokePaint)
            canvas.drawPath(dot3, whiteBorderPaint)
            canvas.drawPath(dot3, fillPaint)
            canvas.drawPath(dot3, strokePaint)
        }
        "Emotional Reaction Bubble" -> {
            bodyPath.addOval(rect, Path.Direction.CW)
            addTail(tailPath, rect)
        }
        else -> {
            bodyPath.addRoundRect(rect, 36f, 36f, Path.Direction.CW)
            addTail(tailPath, rect)
        }
    }

    val finalPath = Path(bodyPath)
    if (!tailPath.isEmpty) {
        finalPath.op(tailPath, Path.Op.UNION)
    }

    // Shadow offset path
    val shadowPath = Path()
    val shadowMatrix = android.graphics.Matrix().apply { postTranslate(6f, 6f) }
    finalPath.transform(shadowMatrix, shadowPath)

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#33000000")
        alpha = (opacityAlpha * 0.25f * 255).toInt().coerceIn(0, 255)
    }

    // 1. Draw Shadow
    canvas.drawPath(shadowPath, shadowPaint)

    // 2. Draw Thick White Sticker Border
    canvas.drawPath(finalPath, whiteBorderPaint)

    // 3. Draw Interior Fill
    canvas.drawPath(finalPath, fillPaint)

    // 4. Draw Inner Black Comic Outline
    canvas.drawPath(finalPath, strokePaint)

    // Decorative Comic Accents
    if (bubbleStyle == "Comic Explosion") {
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = AndroidColor.parseColor("#FFD600")
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(rect.right - 60f, rect.top + 10f, rect.right - 30f, rect.top - 15f, accentPaint)
        canvas.drawLine(rect.right - 40f, rect.top + 20f, rect.right - 10f, rect.top + 5f, accentPaint)
        canvas.drawLine(rect.right - 20f, rect.top + 35f, rect.right + 10f, rect.top + 30f, accentPaint)
    }

    if (bubbleStyle == "Question Bubble") {
        val qPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E53935") // Red
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText("??", rect.right - 10f, rect.bottom + 15f, qPaint)
    }

    if (bubbleStyle == "Exclamation Bubble") {
        val ePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#E53935") // Red
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText("!!", rect.right - 10f, rect.bottom + 15f, ePaint)
    }

    if (bubbleStyle == "Emotional Reaction Bubble") {
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#EC407A") // Pink
            textSize = 36f
            alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawText("💕", rect.right - 10f, rect.bottom + 15f, heartPaint)
    }

    if (text.isNotBlank()) {
        val tf = resolveTypeface(fontFamily, fontStyle, context)
        val align = when (textAlignName) {
            "Left" -> Paint.Align.LEFT
            "Right" -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorInt
            textSize = textSizeSp * 1.25f
            textAlign = align
            typeface = tf
            alpha = (opacityAlpha * 255).toInt().coerceIn(0, 255)
            if (hasShadow) {
                setShadowLayer(6f, 2f, 2f, AndroidColor.parseColor("#88000000"))
            }
        }

        val x = when (align) {
            Paint.Align.LEFT -> rect.left + 40f
            Paint.Align.RIGHT -> rect.right - 40f
            else -> rect.centerX()
        }

        val lines = text.split("\n")
        val lineSpacing = textPaint.textSize * 1.25f
        val startY = rect.centerY() - ((lines.size - 1) * lineSpacing / 2f) + (textPaint.textSize / 3f)

        lines.forEachIndexed { idx, line ->
            canvas.drawText(line, x, startY + (idx * lineSpacing), textPaint)
        }
    }

    return bitmap
}

fun drawStrokeOnBitmap(
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

    val scaleValue = if (kotlin.math.abs(layer.scale) < 0.001f) {
        if (layer.scale >= 0f) 0.001f else -0.001f
    } else {
        layer.scale
    }

    val effectiveScale = (fitScale * kotlin.math.abs(scaleValue)).coerceAtLeast(0.001f)

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
        val dx = p.x - centerCanvasX - layerTranslationX
        val dy = p.y - centerCanvasY - layerTranslationY
        val negRad = Math.toRadians(-layer.rotation.toDouble())
        val rx = (dx * Math.cos(negRad) - dy * Math.sin(negRad)).toFloat()
        val ry = (dx * Math.sin(negRad) + dy * Math.cos(negRad)).toFloat()
        val unscaledX = rx / scaleValue
        val unscaledY = ry / scaleValue
        val bx = (unscaledX / fitScale) + (bitmap.width / 2f)
        val by = (unscaledY / fitScale) + (bitmap.height / 2f)
        return Offset(bx, by)
    }

    val start = canvasToBitmapCoords(prevPos)
    val end = canvasToBitmapCoords(currentPos)
    if (start == end) {
        val dotPaint = Paint(paint).apply {
            style = Paint.Style.FILL
        }
        canvas.drawCircle(start.x, start.y, (brushSize / effectiveScale) / 2f, dotPaint)
    } else {
        canvas.drawLine(start.x, start.y, end.x, end.y, paint)
    }
    layer.updateVisibleBounds()
}

fun findLayerAtTouch(
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

        val scaleValue = if (kotlin.math.abs(layer.scale) < 0.001f) {
            if (layer.scale >= 0f) 0.001f else -0.001f
        } else {
            layer.scale
        }

        val dx = p.x - centerCanvasX - layerTranslationX
        val dy = p.y - centerCanvasY - layerTranslationY
        val negRad = Math.toRadians(-layer.rotation.toDouble())
        val rx = (dx * Math.cos(negRad) - dy * Math.sin(negRad)).toFloat()
        val ry = (dx * Math.sin(negRad) + dy * Math.cos(negRad)).toFloat()
        val unscaledX = rx / scaleValue
        val unscaledY = ry / scaleValue
        val bx = (unscaledX / fitScale) + (bitmap.width / 2f)
        val by = (unscaledY / fitScale) + (bitmap.height / 2f)

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
