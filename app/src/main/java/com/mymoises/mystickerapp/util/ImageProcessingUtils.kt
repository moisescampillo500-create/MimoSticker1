package com.mymoises.mystickerapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import com.mymoises.mystickerapp.model.OnlineBgItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    @Suppress("DEPRECATION")
    val activeNetwork = cm?.activeNetworkInfo
    @Suppress("DEPRECATION")
    return activeNetwork != null && activeNetwork.isConnected
}

fun calculateVisibleBounds(bitmap: Bitmap): RectF {
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

fun applyImageFilter(source: Bitmap, filterType: String): Bitmap {
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

fun createShapeBitmap(
    shapeType: String,
    size: Int = 512,
    color: Int = AndroidColor.RED,
    strokeColor: Int = AndroidColor.BLACK,
    strokeWidthPx: Float = 16f,
    isFilled: Boolean = true,
    fillColor: Int = color
): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = strokeColor
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidthPx.coerceAtLeast(2f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val center = size / 2f
    val margin = (strokeWidthPx / 2f).coerceAtLeast(16f)
    val radius = size / 2f - margin

    when (shapeType) {
        "Circle" -> {
            if (isFilled) canvas.drawCircle(center, center, radius, fillPaint)
            if (strokeWidthPx > 0f) canvas.drawCircle(center, center, radius, strokePaint)
        }
        "Rectangle" -> {
            val rect = android.graphics.RectF(margin, margin, size - margin, size - margin)
            if (isFilled) canvas.drawRect(rect, fillPaint)
            if (strokeWidthPx > 0f) canvas.drawRect(rect, strokePaint)
        }
        "Rounded Rect" -> {
            val rect = android.graphics.RectF(margin, margin, size - margin, size - margin)
            if (isFilled) canvas.drawRoundRect(rect, 40f, 40f, fillPaint)
            if (strokeWidthPx > 0f) canvas.drawRoundRect(rect, 40f, 40f, strokePaint)
        }
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
            if (isFilled) canvas.drawPath(path, fillPaint)
            if (strokeWidthPx > 0f) canvas.drawPath(path, strokePaint)
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
            if (isFilled) canvas.drawPath(path, fillPaint)
            if (strokeWidthPx > 0f) canvas.drawPath(path, strokePaint)
        }
        "Frame" -> {
            val frameMargin = (strokeWidthPx * 1.2f).coerceAtLeast(24f)
            val rect = android.graphics.RectF(frameMargin, frameMargin, size - frameMargin, size - frameMargin)
            if (isFilled) canvas.drawRect(rect, fillPaint)
            if (strokeWidthPx > 0f) {
                val framePaint = Paint(strokePaint).apply { strokeWidth = (strokeWidthPx * 1.5f).coerceAtLeast(12f) }
                canvas.drawRect(rect, framePaint)
            }
        }
    }
    return bmp
}

fun generateLocalStickerBitmap(
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
