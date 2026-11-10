package com.mymoises.mystickerapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class HeadSegmentationProcessor(private val context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val fileDescriptor = context.assets.openFd("selfie_multiclass_256x256.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(mappedByteBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun processHeadCutout(originalBitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        if (originalBitmap.width <= 0 || originalBitmap.height <= 0) return@withContext null

        val tfliteResult = processWithTFLiteMulticlass(originalBitmap)
        if (tfliteResult != null && isHeadCutoutValid(tfliteResult)) {
            return@withContext tfliteResult
        }

        return@withContext processWithMLKitHeadIsolation(originalBitmap)
    }

    private fun processWithTFLiteMulticlass(originalBitmap: Bitmap): Bitmap? {
        val interp = interpreter ?: return null
        val inputSize = 256
        val numClasses = 6

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true)
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = ((pixel ushr 16) and 0xFF) / 255.0f
            val g = ((pixel ushr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        val outputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * numClasses * 4).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        interp.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        // 6 Classes:
        // 0: Background, 1: Hair, 2: Body-skin, 3: Face-skin, 4: Clothes, 5: Accessories
        // Head components = Hair (1) + Face-skin (3) + Accessories (5)
        val maskPixels = IntArray(inputSize * inputSize)
        var headPixelCount = 0

        var minY = inputSize
        var maxY = -1

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val index = (y * inputSize + x) * numClasses
                val p1 = outputBuffer.getFloat(index * 4 + 4)  // Hair
                val p3 = outputBuffer.getFloat(index * 4 + 12) // Face-skin
                val p5 = outputBuffer.getFloat(index * 4 + 20) // Accessories

                if (p1 > 0.3f || p3 > 0.3f || p5 > 0.3f) {
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val headBottomCutoff = if (maxY > minY) (minY + (maxY - minY) * 1.05f).toInt().coerceAtMost(inputSize - 1) else inputSize

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixelIndex = y * inputSize + x
                val floatOffset = pixelIndex * numClasses * 4

                val p0 = outputBuffer.getFloat(floatOffset)      // BG
                val p1 = outputBuffer.getFloat(floatOffset + 4)  // Hair
                val p2 = outputBuffer.getFloat(floatOffset + 8)  // Body-skin
                val p3 = outputBuffer.getFloat(floatOffset + 12) // Face-skin
                val p4 = outputBuffer.getFloat(floatOffset + 16) // Clothes
                val p5 = outputBuffer.getFloat(floatOffset + 20) // Accessories

                val headScore = p1 + p3 + p5
                val bodyScore = p2 + p4 + p0

                val isHeadClass = (headScore > bodyScore && headScore > 0.25f) ||
                        (p3 > 0.35f) || (p1 > 0.35f) || (p5 > 0.35f)

                val isBelowNeck = y > headBottomCutoff && p3 < 0.2f

                if (isHeadClass && !isBelowNeck) {
                    val alpha = (headScore.coerceIn(0f, 1f) * 255).toInt()
                    maskPixels[pixelIndex] = (alpha shl 24) or 0x00FFFFFF
                    headPixelCount++
                } else {
                    maskPixels[pixelIndex] = 0x00000000
                }
            }
        }

        if (headPixelCount < 100) return null

        val maskBitmap256 = Bitmap.createBitmap(maskPixels, inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val maskBitmapFull = Bitmap.createScaledBitmap(maskBitmap256, originalBitmap.width, originalBitmap.height, true)

        val cutout = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cutout)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmapFull, 0f, 0f, paint)

        return cropToVisibleBounds(cutout)
    }

    private suspend fun processWithMLKitHeadIsolation(originalBitmap: Bitmap): Bitmap? {
        return try {
            val inputImage = InputImage.fromBitmap(originalBitmap, 0)
            var cutout: Bitmap? = null

            try {
                val subjectSegmenter = SubjectSegmentation.getClient(
                    SubjectSegmenterOptions.Builder()
                        .enableForegroundBitmap()
                        .enableForegroundConfidenceMask()
                        .build()
                )
                val result = subjectSegmenter.processSuspending(inputImage)
                val fg: Bitmap? = result.foregroundBitmap
                if (fg != null) {
                    cutout = fg.copy(Bitmap.Config.ARGB_8888, true)
                }
                subjectSegmenter.close()
            } catch (_: Exception) {}

            if (cutout == null) {
                val selfieSegmenter = Segmentation.getClient(
                    SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build()
                )
                val maskResult = selfieSegmenter.processSuspending(inputImage)
                val maskBuffer = maskResult.buffer
                val maskWidth = maskResult.width
                val maskHeight = maskResult.height

                val maskPixels = IntArray(maskWidth * maskHeight)
                maskBuffer.rewind()
                for (i in maskPixels.indices) {
                    val confidence = maskBuffer.float
                    val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                    maskPixels[i] = (alpha shl 24) or 0x00FFFFFF
                }

                val maskBmp = Bitmap.createBitmap(maskPixels, maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
                val scaledMask = Bitmap.createScaledBitmap(maskBmp, originalBitmap.width, originalBitmap.height, true)

                val tempCutout = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(tempCutout)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(scaledMask, 0f, 0f, paint)

                cutout = tempCutout
                selfieSegmenter.close()
            }

            if (cutout != null) {
                isolateHeadRegionFromSubject(cutout)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isolateHeadRegionFromSubject(subjectCutout: Bitmap): Bitmap {
        val bounds = calculateVisibleBounds(subjectCutout)
        val headHeightEstimate = bounds.height() * 0.45f
        val headBottom = bounds.top + headHeightEstimate

        val width = subjectCutout.width
        val height = subjectCutout.height
        val pixels = IntArray(width * height)
        subjectCutout.getPixels(pixels, 0, width, 0, 0, width, height)

        val headPixels = IntArray(width * height)
        for (y in 0 until height) {
            val isHeadY = y <= headBottom
            for (x in 0 until width) {
                val index = y * width + x
                if (isHeadY) {
                    headPixels[index] = pixels[index]
                } else {
                    val fadeRange = bounds.height() * 0.08f
                    val distanceBelow = y - headBottom
                    if (distanceBelow < fadeRange) {
                        val fadeFactor = (1.0f - distanceBelow / fadeRange).coerceIn(0f, 1f)
                        val origPixel = pixels[index]
                        val origAlpha = (origPixel ushr 24) and 0xFF
                        val newAlpha = (origAlpha * fadeFactor).toInt()
                        headPixels[index] = (newAlpha shl 24) or (origPixel and 0x00FFFFFF)
                    } else {
                        headPixels[index] = 0x00000000
                    }
                }
            }
        }

        val headBmp = Bitmap.createBitmap(headPixels, width, height, Bitmap.Config.ARGB_8888)
        return cropToVisibleBounds(headBmp)
    }

    private fun isHeadCutoutValid(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 10 || bitmap.height <= 10) return false
        var nonTransparentCount = 0
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (pixel in pixels) {
            if (((pixel ushr 24) and 0xFF) > 30) {
                nonTransparentCount++
            }
        }
        return nonTransparentCount > 200
    }

    private fun cropToVisibleBounds(bitmap: Bitmap): Bitmap {
        val bounds = calculateVisibleBounds(bitmap)
        val width = (bounds.width()).toInt().coerceAtLeast(1)
        val height = (bounds.height()).toInt().coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, -bounds.left, -bounds.top, paint)
        return cropped
    }

    fun close() {
        try {
            interpreter?.close()
        } catch (_: Exception) {}
    }

    private suspend fun SubjectSegmenter.processSuspending(inputImage: InputImage): SubjectSegmentationResult =
        suspendCancellableCoroutine { cont ->
            process(inputImage)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    private suspend fun Segmenter.processSuspending(inputImage: InputImage): SegmentationMask =
        suspendCancellableCoroutine { cont ->
            process(inputImage)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

fun renderStickerWithOutlineAndShadow(
    cutoutBitmap: Bitmap,
    outlineEnabled: Boolean,
    outlineColorInt: Int,
    outlineThicknessDp: Float,
    smoothEdges: Boolean,
    enableShadow: Boolean,
    density: Float = 2.0f
): Bitmap {
    if (cutoutBitmap.isRecycled || cutoutBitmap.width <= 0 || cutoutBitmap.height <= 0) {
        return cutoutBitmap
    }

    val width = cutoutBitmap.width
    val height = cutoutBitmap.height
    val strokeRadiusPx = if (outlineEnabled) (outlineThicknessDp * density).roundToInt().coerceAtLeast(0) else 0
    val shadowBlurPx = if (enableShadow) (8f * density).roundToInt() else 0
    val padding = strokeRadiusPx + shadowBlurPx + 8

    val outWidth = width + padding * 2
    val outHeight = height + padding * 2
    val resultBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)

    val baseCutout = if (smoothEdges) {
        applyEdgeSmoothing(cutoutBitmap)
    } else {
        cutoutBitmap
    }

    if (enableShadow) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 0, 0, 0)
            maskFilter = android.graphics.BlurMaskFilter(shadowBlurPx.toFloat().coerceAtLeast(1f), android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        val alphaMask = baseCutout.extractAlpha()
        canvas.drawBitmap(alphaMask, (padding + 4).toFloat(), (padding + 6).toFloat(), shadowPaint)
    }

    if (outlineEnabled && strokeRadiusPx > 0) {
        val outlineBitmap = createDilatedOutlineBitmap(baseCutout, outlineColorInt, strokeRadiusPx)
        canvas.drawBitmap(outlineBitmap, (padding - strokeRadiusPx).toFloat(), (padding - strokeRadiusPx).toFloat(), null)
    }

    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(baseCutout, padding.toFloat(), padding.toFloat(), mainPaint)

    return resultBitmap
}

private fun createDilatedOutlineBitmap(srcBitmap: Bitmap, outlineColorInt: Int, radiusPx: Int): Bitmap {
    val srcW = srcBitmap.width
    val srcH = srcBitmap.height
    val outW = srcW + radiusPx * 2
    val outH = srcH + radiusPx * 2

    val pixels = IntArray(srcW * srcH)
    srcBitmap.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)

    val alphaCore = BooleanArray(srcW * srcH) { i ->
        (pixels[i] ushr 24 and 0xFF) >= 40
    }

    val horizontalDilation = BooleanArray(srcW * srcH)
    val dilatedMask = BooleanArray(srcW * srcH)
    val prefix = IntArray(maxOf(srcW, srcH) + 1)

    for (y in 0 until srcH) {
        prefix[0] = 0
        for (x in 0 until srcW) {
            prefix[x + 1] = prefix[x] + if (alphaCore[y * srcW + x]) 1 else 0
        }
        for (x in 0 until srcW) {
            val left = (x - radiusPx).coerceAtLeast(0)
            val right = (x + radiusPx + 1).coerceAtMost(srcW)
            horizontalDilation[y * srcW + x] = prefix[right] > prefix[left]
        }
    }

    for (x in 0 until srcW) {
        prefix[0] = 0
        for (y in 0 until srcH) {
            prefix[y + 1] = prefix[y] + if (horizontalDilation[y * srcW + x]) 1 else 0
        }
        for (y in 0 until srcH) {
            val top = (y - radiusPx).coerceAtLeast(0)
            val bottom = (y + radiusPx + 1).coerceAtMost(srcH)
            dilatedMask[y * srcW + x] = prefix[bottom] > prefix[top]
        }
    }

    val outPixels = IntArray(outW * outH)
    val alphaVal = outlineColorInt ushr 24 and 0xFF
    val colorSolid = if (alphaVal == 0) (0xFF shl 24) or (outlineColorInt and 0x00FFFFFF) else outlineColorInt

    for (y in 0 until srcH) {
        for (x in 0 until srcW) {
            if (dilatedMask[y * srcW + x]) {
                val targetX = x + radiusPx
                val targetY = y + radiusPx
                outPixels[targetY * outW + targetX] = colorSolid
            }
        }
    }

    return Bitmap.createBitmap(outPixels, outW, outH, Bitmap.Config.ARGB_8888)
}

private fun applyEdgeSmoothing(srcBitmap: Bitmap): Bitmap {
    val width = srcBitmap.width
    val height = srcBitmap.height
    val copy = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(width * height)
    copy.getPixels(pixels, 0, width, 0, 0, width, height)

    val smoothed = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            val currentPixel = pixels[index]
            val currentAlpha = (currentPixel ushr 24) and 0xFF

            if (currentAlpha in 1..254) {
                var alphaSum = currentAlpha * 2
                var count = 2
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ny = y + dy
                        val nx = x + dx
                        if (nx in 0 until width && ny in 0 until height) {
                            val nAlpha = (pixels[ny * width + nx] ushr 24) and 0xFF
                            alphaSum += nAlpha
                            count++
                        }
                    }
                }
                val avgAlpha = (alphaSum / count).coerceIn(0, 255)
                smoothed[index] = (avgAlpha shl 24) or (currentPixel and 0x00FFFFFF)
            } else {
                smoothed[index] = currentPixel
            }
        }
    }
    copy.setPixels(smoothed, 0, width, 0, 0, width, height)
    return copy
}
