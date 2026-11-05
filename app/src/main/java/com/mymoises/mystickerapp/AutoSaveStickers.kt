package com.mymoises.mystickerapp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object AutoSaveStickers {
    private const val PREFS_NAME: String = "mysticker_settings"
    private const val AUTO_SAVE_KEY: String = "auto_save_stickers"

    fun isEnabled(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(AUTO_SAVE_KEY, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putBoolean(AUTO_SAVE_KEY, enabled)
        editor.apply()
    }

    fun saveIfEnabled(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val enabled: Boolean = isEnabled(context)
        if (!enabled) return null
        return try {
            val dir: File = getSavedStickersDirectory(context)
            val name: String = if (fileName.isBlank()) "Sticker_${System.currentTimeMillis()}.png" else fileName
            val file = File(dir, name)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }
}

fun getSavedStickersDirectory(context: Context): File {
    val dir = File(context.filesDir, "SavedStickers")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

fun getSavedStickers(context: Context): List<File> {
    val dir: File = getSavedStickersDirectory(context)
    val files: Array<File>? = dir.listFiles()
    if (files == null) return emptyList()
    val resultList = mutableListOf<File>()
    for (file in files) {
        if (file.isFile && file.name.endsWith(".png", ignoreCase = true)) {
            resultList.add(file)
        }
    }
    resultList.sortByDescending { it.lastModified() }
    return resultList
}

fun saveSticker(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val dir: File = getSavedStickersDirectory(context)
        val file = File(dir, "Sticker_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        true
    } catch (_: Exception) {
        false
    }
}
