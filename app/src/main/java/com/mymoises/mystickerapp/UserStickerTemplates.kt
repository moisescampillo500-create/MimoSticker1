package com.mymoises.mystickerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class UserTemplateLayerData(
    val name: String,
    val bitmapFileName: String?,
    val visible: Boolean = true,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val alpha: Float = 1f
)

data class UserStickerTemplate(
    val id: String,
    val name: String,
    val creatorName: String = "My Creator",
    val category: String = "Cute",
    val createdAt: Long,
    val isPublished: Boolean = true,
    val isOwnedByCurrentUser: Boolean = true,
    val dirPath: String,
    val previewPath: String,
    val layers: List<UserTemplateLayerData>
)

object UserStickerTemplatesManager {

    fun getUserTemplatesDirectory(context: Context): File {
        val dir = File(context.filesDir, "UserTemplates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveUserTemplate(
        context: Context,
        templateName: String,
        creatorName: String = "My Creator",
        category: String = "Cute",
        layerDataList: List<Triple<String, Bitmap, UserTemplateLayerData>>,
        previewBitmap: Bitmap,
        isPublished: Boolean = true,
        isOwnedByCurrentUser: Boolean = true,
        existingId: String? = null
    ): Boolean {
        return try {
            val baseDir = getUserTemplatesDirectory(context)
            val templateId = existingId ?: "user_template_${System.currentTimeMillis()}"
            val templateDir = File(baseDir, templateId)
            if (!templateDir.exists()) {
                templateDir.mkdirs()
            }

            // Save Preview Bitmap
            val previewFile = File(templateDir, "preview.png")
            FileOutputStream(previewFile).use { out ->
                previewBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val layersJsonArray = JSONArray()

            // Save Layer Bitmaps and Layer JSON
            layerDataList.forEachIndexed { index, (layerName, bitmap, transform) ->
                val bitmapFileName = "layer_$index.png"
                val layerFile = File(templateDir, bitmapFileName)
                FileOutputStream(layerFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val layerObj = JSONObject().apply {
                    put("name", layerName)
                    put("bitmapFileName", bitmapFileName)
                    put("visible", transform.visible)
                    put("scale", transform.scale.toDouble())
                    put("rotation", transform.rotation.toDouble())
                    put("offsetX", transform.offsetX.toDouble())
                    put("offsetY", transform.offsetY.toDouble())
                    put("alpha", transform.alpha.toDouble())
                }
                layersJsonArray.put(layerObj)
            }

            // Root JSON
            val rootObj = JSONObject().apply {
                put("id", templateId)
                put("name", if (templateName.isBlank()) "My Template" else templateName)
                put("creatorName", if (creatorName.isBlank()) "My Creator" else creatorName)
                put("category", if (category.isBlank()) "Cute" else category)
                put("createdAt", System.currentTimeMillis())
                put("isPublished", isPublished)
                put("isOwnedByCurrentUser", isOwnedByCurrentUser)
                put("layers", layersJsonArray)
            }

            val jsonFile = File(templateDir, "template.json")
            jsonFile.writeText(rootObj.toString(2))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getAllTemplates(context: Context): List<UserStickerTemplate> {
        seedCommunityTemplatesIfEmpty(context)
        val baseDir = getUserTemplatesDirectory(context)
        val files = baseDir.listFiles() ?: return emptyList()
        val resultList = mutableListOf<UserStickerTemplate>()

        for (dir in files) {
            if (dir.isDirectory) {
                val jsonFile = File(dir, "template.json")
                val previewFile = File(dir, "preview.png")
                if (jsonFile.exists() && jsonFile.isFile) {
                    try {
                        val jsonString = jsonFile.readText()
                        val rootObj = JSONObject(jsonString)

                        val id = rootObj.optString("id", dir.name)
                        val name = rootObj.optString("name", "Template")
                        val creatorName = rootObj.optString("creatorName", "Community Creator")
                        val category = rootObj.optString("category", "Cute")
                        val createdAt = rootObj.optLong("createdAt", dir.lastModified())
                        val isPublished = rootObj.optBoolean("isPublished", true)
                        val isOwnedByCurrentUser = rootObj.optBoolean("isOwnedByCurrentUser", true)

                        val layersArray = rootObj.optJSONArray("layers") ?: JSONArray()
                        val layerList = mutableListOf<UserTemplateLayerData>()

                        for (i in 0 until layersArray.length()) {
                            val layerObj = layersArray.getJSONObject(i)
                            layerList.add(
                                UserTemplateLayerData(
                                    name = layerObj.optString("name", "Layer"),
                                    bitmapFileName = if (layerObj.has("bitmapFileName")) layerObj.getString("bitmapFileName") else null,
                                    visible = layerObj.optBoolean("visible", true),
                                    scale = layerObj.optDouble("scale", 1.0).toFloat(),
                                    rotation = layerObj.optDouble("rotation", 0.0).toFloat(),
                                    offsetX = layerObj.optDouble("offsetX", 0.0).toFloat(),
                                    offsetY = layerObj.optDouble("offsetY", 0.0).toFloat(),
                                    alpha = layerObj.optDouble("alpha", 1.0).toFloat()
                                )
                            )
                        }

                        resultList.add(
                            UserStickerTemplate(
                                id = id,
                                name = name,
                                creatorName = creatorName,
                                category = category,
                                createdAt = createdAt,
                                isPublished = isPublished,
                                isOwnedByCurrentUser = isOwnedByCurrentUser,
                                dirPath = dir.absolutePath,
                                previewPath = previewFile.absolutePath,
                                layers = layerList
                            )
                        )
                    } catch (_: Exception) {
                        // Skip corrupted template
                    }
                }
            }
        }

        resultList.sortByDescending { it.createdAt }
        return resultList
    }

    fun getCommunityTemplates(context: Context, categoryFilter: String = "All"): List<UserStickerTemplate> {
        val all = getAllTemplates(context).filter { it.isPublished }
        return if (categoryFilter == "All") {
            all
        } else {
            all.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }
    }

    fun getMyTemplates(context: Context): List<UserStickerTemplate> {
        return getAllTemplates(context).filter { it.isOwnedByCurrentUser }
    }

    fun publishUserTemplate(context: Context, templateId: String, isPublished: Boolean): Boolean {
        return try {
            val baseDir = getUserTemplatesDirectory(context)
            val templateDir = File(baseDir, templateId)
            val jsonFile = File(templateDir, "template.json")
            if (jsonFile.exists()) {
                val rootObj = JSONObject(jsonFile.readText())
                rootObj.put("isPublished", isPublished)
                jsonFile.writeText(rootObj.toString(2))
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun deleteUserTemplate(context: Context, templateId: String): Boolean {
        return try {
            val baseDir = getUserTemplatesDirectory(context)
            val templateDir = File(baseDir, templateId)
            if (templateDir.exists()) {
                templateDir.deleteRecursively()
            } else true
        } catch (_: Exception) {
            false
        }
    }

    fun renameUserTemplate(context: Context, templateId: String, newName: String): Boolean {
        return try {
            val baseDir = getUserTemplatesDirectory(context)
            val templateDir = File(baseDir, templateId)
            val jsonFile = File(templateDir, "template.json")
            if (jsonFile.exists()) {
                val rootObj = JSONObject(jsonFile.readText())
                rootObj.put("name", newName)
                jsonFile.writeText(rootObj.toString(2))
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun loadLayerBitmap(templateDirPath: String, bitmapFileName: String): Bitmap? {
        return try {
            val file = File(templateDirPath, bitmapFileName)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun seedCommunityTemplatesIfEmpty(context: Context) {
        val baseDir = getUserTemplatesDirectory(context)
        val files = baseDir.listFiles()
        if (files != null && files.isNotEmpty()) return

        val samples = listOf(
            Triple("Cat Love Frame", "CuteArtist", "Cute" to R.drawable.ic_sticker_cute_cat_heart),
            Triple("LOL Party Meme", "MemeKing", "Meme" to R.drawable.ic_sticker_bubble_lol),
            Triple("Double Heart Kiss", "LoveStudio", "Love" to R.drawable.ic_sticker_love_double_hearts),
            Triple("Cool Doge Sticker", "StickerPro", "Reactions" to R.drawable.ic_sticker_meme_doge)
        )

        samples.forEachIndexed { idx, (name, creator, catPair) ->
            val (cat, resId) = catPair
            val bmp = BitmapFactory.decodeResource(context.resources, resId) ?: return@forEachIndexed
            val preview = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(preview)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            val scale = minOf(400f / bmp.width, 400f / bmp.height)
            val w = bmp.width * scale
            val h = bmp.height * scale
            canvas.drawBitmap(bmp, null, android.graphics.RectF((512 - w) / 2f, (512 - h) / 2f, (512 + w) / 2f, (512 + h) / 2f), paint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 36f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(name, 256f, 470f, textPaint)

            val layerData = UserTemplateLayerData(
                name = name,
                bitmapFileName = null,
                visible = true
            )

            saveUserTemplate(
                context = context,
                templateName = name,
                creatorName = creator,
                category = cat,
                layerDataList = listOf(Triple(name, bmp, layerData)),
                previewBitmap = preview,
                isPublished = true,
                isOwnedByCurrentUser = false,
                existingId = "community_seed_$idx"
            )
        }
    }
}
