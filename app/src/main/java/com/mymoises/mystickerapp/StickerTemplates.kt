package com.mymoises.mystickerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class BubbleShape {
    SPEECH_BUBBLE,
    THOUGHT_CLOUD,
    HEART,
    BADGE,
    MEME_BOX,
    BANNER,
    CUTE_FRAME,
    GLOSSY_BADGE,
    OVAL_BUBBLE,
    SHIELD,
    DRAWABLE_RESOURCE
}

data class StickerTemplate(
    val id: String,
    val title: String,
    val category: String, // Cute, Love, Funny, Meme, Speech Bubbles, Reactions
    val defaultText: String,
    val backgroundColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val bubbleShape: BubbleShape,
    val decorations: List<String> = emptyList(),
    val hasPhotoSlot: Boolean = true,
    val drawableResId: Int? = null
)

object TemplateCategories {
    val ALL = "All"
    val CUTE = "Cute"
    val LOVE = "Love"
    val FUNNY = "Funny"
    val MEME = "Meme"
    val SPEECH_BUBBLES = "Speech Bubbles"
    val REACTIONS = "Reactions"

    val CATEGORIES_LIST = listOf(
        ALL,
        CUTE,
        LOVE,
        FUNNY,
        MEME,
        SPEECH_BUBBLES,
        REACTIONS
    )
}

val STICKER_TEMPLATES = emptyList<StickerTemplate>()

fun renderTemplateToBitmap(
    context: Context,
    template: StickerTemplate,
    text: String,
    textColor: Color,
    textSizeSp: Float,
    photoBitmap: Bitmap? = null,
    targetSizePx: Int = 512
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val width = targetSizePx.toFloat()
    val height = targetSizePx.toFloat()
    val padding = width * 0.08f

    if (template.drawableResId != null) {
        val drawableBmp = try {
            BitmapFactory.decodeResource(context.resources, template.drawableResId)
        } catch (_: Exception) {
            null
        }

        if (drawableBmp != null) {
            val srcW = drawableBmp.width.toFloat()
            val srcH = drawableBmp.height.toFloat()
            val scale = minOf((width - padding * 2) / srcW, (height - padding * 2) / srcH)
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(
                    padding + ((width - padding * 2) - srcW * scale) / 2f,
                    padding + ((height - padding * 2) - srcH * scale) / 2f
                )
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(drawableBmp, matrix, paint)
        }
    } else {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = template.backgroundColor.toArgb()
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width * 0.02f
            color = template.accentColor.toArgb()
        }

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.parseColor("#33000000")
        }

        val shapeBounds = RectF(padding, padding, width - padding, height - padding)
        val shadowBounds = RectF(padding + 8f, padding + 12f, width - padding + 8f, height - padding + 12f)

        when (template.bubbleShape) {
            BubbleShape.SPEECH_BUBBLE -> {
                val path = Path().apply {
                    addRoundRect(RectF(shapeBounds.left, shapeBounds.top, shapeBounds.right, shapeBounds.bottom - 40f), 32f, 32f, Path.Direction.CW)
                    moveTo(shapeBounds.left + 80f, shapeBounds.bottom - 40f)
                    lineTo(shapeBounds.left + 40f, shapeBounds.bottom)
                    lineTo(shapeBounds.left + 120f, shapeBounds.bottom - 40f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, strokePaint)
            }
            else -> {
                canvas.drawRoundRect(shadowBounds, 32f, 32f, shadowPaint)
                canvas.drawRoundRect(shapeBounds, 32f, 32f, fillPaint)
                canvas.drawRoundRect(shapeBounds, 32f, 32f, strokePaint)
            }
        }
    }

    // Draw user photo if provided
    if (photoBitmap != null && !photoBitmap.isRecycled) {
        val photoRadius = (width * 0.22f)
        val photoCx = width / 2f
        val photoCy = height * 0.42f
        val photoRect = RectF(photoCx - photoRadius, photoCy - photoRadius, photoCx + photoRadius, photoCy + photoRadius)

        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(photoRect, 28f, 28f, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val srcW = photoBitmap.width.toFloat()
        val srcH = photoBitmap.height.toFloat()
        val scale = maxOf(photoRect.width() / srcW, photoRect.height() / srcH)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(
                photoRect.left + (photoRect.width() - srcW * scale) / 2f,
                photoRect.top + (photoRect.height() - srcH * scale) / 2f
            )
        }
        val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(photoBitmap, matrix, photoPaint)
        canvas.restore()
    }

    // Draw Text if non-empty
    if (text.isNotBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textSize = (textSizeSp * context.resources.displayMetrics.scaledDensity).coerceIn(24f, 64f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 2f, 2f, AndroidColor.parseColor("#88000000"))
        }

        val lines = text.split("\n")
        val lineSpacing = textPaint.textSize * 1.2f
        val startY = height * 0.82f - ((lines.size - 1) * lineSpacing / 2f)

        lines.forEachIndexed { idx, line ->
            canvas.drawText(line, width / 2f, startY + (idx * lineSpacing), textPaint)
        }
    }

    return bitmap
}
