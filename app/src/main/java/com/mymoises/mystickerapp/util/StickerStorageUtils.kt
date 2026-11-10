package com.mymoises.mystickerapp.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun shareStickerFile(context: Context, file: File, caption: String? = null) {
    try {
        if (!file.exists()) {
            Toast.makeText(context, "Sticker image file not found.", Toast.LENGTH_SHORT).show()
            return
        }
        val cacheDir = File(context.cacheDir, "shared_stickers").apply { mkdirs() }
        val sharedFile = File(cacheDir, file.name)
        if (file.absolutePath != sharedFile.absolutePath) {
            file.copyTo(sharedFile, overwrite = true)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sharedFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Sticker Image"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not share sticker image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareTemplateWithImage(context: Context, templateName: String, creatorName: String, previewPath: String?) {
    try {
        val shareText = "Check out the '$templateName' sticker template by @$creatorName on Sticker Craft! 🎨 Download and customize it now!"
        val cacheDir = File(context.cacheDir, "shared_templates").apply { mkdirs() }
        val destFile = File(cacheDir, "template_${System.currentTimeMillis()}.png")

        var bitmap: Bitmap? = null
        if (!previewPath.isNullOrBlank()) {
            val srcFile = File(previewPath)
            if (srcFile.exists()) {
                bitmap = BitmapFactory.decodeFile(srcFile.absolutePath)
            }
        }

        if (bitmap != null) {
            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } else if (!previewPath.isNullOrBlank()) {
            val srcFile = File(previewPath)
            if (srcFile.exists()) {
                srcFile.copyTo(destFile, overwrite = true)
            }
        }

        if (destFile.exists() && destFile.length() > 0) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sticker Template: $templateName")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Template Image"))
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Sticker Template: $templateName")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Share Template"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not share template image: ${e.message}", Toast.LENGTH_SHORT).show()
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
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "MyStickerApp").apply { mkdirs() }
            val destFile = File(appDir, fileName)
            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            @Suppress("DEPRECATION")
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

fun saveBitmapToDeviceGallery(context: Context, bitmap: Bitmap, fileNamePrefix: String = "sticker"): Boolean {
    return try {
        val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.png"
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
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "MyStickerApp").apply { mkdirs() }
            val destFile = File(appDir, fileName)
            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            @Suppress("DEPRECATION")
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
