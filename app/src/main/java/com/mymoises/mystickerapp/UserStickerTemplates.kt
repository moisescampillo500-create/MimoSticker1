package com.mymoises.mystickerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

data class CreatorStatsData(
    val id: String,
    val name: String,
    val handle: String,
    val avatar: String,
    val bio: String,
    val templatesCount: Int,
    val totalUses: Long,
    val totalLikes: Int,
    val engagementScore: Long,
    val verified: Boolean = true,
    val templates: List<UserStickerTemplate>
)

data class UserStickerTemplate(
    val id: String,
    val name: String,
    val creatorName: String = "My Creator",
    val category: String = "Cute",
    val createdAt: Long,
    val useCount: Long = 0L,
    val likeCount: Int = 0,
    val isPublished: Boolean = true,
    val isOwnedByCurrentUser: Boolean = true,
    val dirPath: String,
    val previewPath: String,
    val layers: List<UserTemplateLayerData>
)

object BitmapCacheManager {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16)

    private val memoryCache = object : android.util.LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getBitmap(key: String): Bitmap? = memoryCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null && !bitmap.isRecycled) {
            memoryCache.put(key, bitmap)
        }
    }

    fun loadCachedBitmap(filePath: String, targetSizePx: Int = 512): Bitmap? {
        val cached = getBitmap(filePath)
        if (cached != null && !cached.isRecycled) return cached

        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            val srcW = options.outWidth
            val srcH = options.outHeight
            var sampleSize = 1
            if (srcW > targetSizePx || srcH > targetSizePx) {
                val halfW = srcW / 2
                val halfH = srcH / 2
                while ((halfW / sampleSize) >= targetSizePx && (halfH / sampleSize) >= targetSizePx) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(filePath, decodeOptions)
            if (decoded != null) {
                putBitmap(filePath, decoded)
            }
            decoded
        } catch (_: Exception) {
            null
        }
    }
}

object UserStickerTemplatesManager {

    @Volatile
    private var templateMemoryCache: List<UserStickerTemplate>? = null

    fun invalidateCache() {
        templateMemoryCache = null
    }

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
        existingId: String? = null,
        useCount: Long = 0L,
        likeCount: Int = 0
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
                put("useCount", useCount)
                put("likeCount", likeCount)
                put("isPublished", isPublished)
                put("isOwnedByCurrentUser", isOwnedByCurrentUser)
                put("layers", layersJsonArray)
            }

            val jsonFile = File(templateDir, "template.json")
            jsonFile.writeText(rootObj.toString(2))
            invalidateCache()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun asyncSeedTemplates(context: Context) = withContext(Dispatchers.IO) {
        invalidateCache()
    }

    fun getAllTemplates(context: Context): List<UserStickerTemplate> {
        val cached = templateMemoryCache
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

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
                        val isOwnedByCurrentUser = rootObj.optBoolean("isOwnedByCurrentUser", true)

                        if (id.startsWith("seed_cat_") || !isOwnedByCurrentUser) {
                            dir.deleteRecursively()
                            continue
                        }

                        val name = rootObj.optString("name", "Template")
                        val creatorName = rootObj.optString("creatorName", "My Creator")
                        val category = rootObj.optString("category", "Cute")
                        val createdAt = rootObj.optLong("createdAt", dir.lastModified())
                        val useCount = rootObj.optLong("useCount", 0L)
                        val likeCount = rootObj.optInt("likeCount", 0)
                        val isPublished = rootObj.optBoolean("isPublished", true)

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
                                useCount = useCount,
                                likeCount = likeCount,
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
        templateMemoryCache = resultList
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

    fun getDiscoveryTemplates(
        context: Context,
        section: String = "Trending",
        categoryFilter: String = "All",
        searchQuery: String = ""
    ): List<UserStickerTemplate> {
        val allPublished = getAllTemplates(context).filter { it.isPublished }

        val catFiltered = if (categoryFilter == "All") {
            allPublished
        } else {
            allPublished.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }

        val searchFiltered = if (searchQuery.isNotBlank()) {
            catFiltered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.creatorName.contains(searchQuery, ignoreCase = true)
            }
        } else {
            catFiltered
        }

        val now = System.currentTimeMillis()
        return when (section) {
            "Trending" -> {
                searchFiltered.sortedByDescending { t ->
                    val hoursOld = ((now - t.createdAt) / (1000f * 3600f)).coerceAtLeast(0.1f)
                    (t.useCount * 10f + t.likeCount * 5f + 10f) / Math.pow(hoursOld.toDouble() + 2.0, 1.1).toFloat()
                }
            }
            "Popular" -> {
                searchFiltered.sortedWith(
                    compareByDescending<UserStickerTemplate> { it.useCount }
                        .thenByDescending { it.likeCount }
                        .thenByDescending { it.createdAt }
                )
            }
            "Newest" -> {
                searchFiltered.sortedByDescending { it.createdAt }
            }
            else -> searchFiltered.sortedByDescending { it.createdAt }
        }
    }

    fun recordTemplateUsage(context: Context, templateId: String) {
        try {
            val baseDir = getUserTemplatesDirectory(context)
            val templateDir = File(baseDir, templateId)
            val jsonFile = File(templateDir, "template.json")
            if (jsonFile.exists()) {
                val rootObj = JSONObject(jsonFile.readText())
                val current = rootObj.optLong("useCount", 0L)
                rootObj.put("useCount", current + 1L)
                jsonFile.writeText(rootObj.toString(2))
            }
        } catch (_: Exception) {}
    }

    fun toggleTemplateLike(context: Context, templateId: String, increment: Boolean): Int {
        return try {
            val baseDir = getUserTemplatesDirectory(context)
            val templateDir = File(baseDir, templateId)
            val jsonFile = File(templateDir, "template.json")
            if (jsonFile.exists()) {
                val rootObj = JSONObject(jsonFile.readText())
                val current = rootObj.optInt("likeCount", 0)
                val newCount = (if (increment) current + 1 else current - 1).coerceAtLeast(0)
                rootObj.put("likeCount", newCount)
                jsonFile.writeText(rootObj.toString(2))
                newCount
            } else 0
        } catch (_: Exception) { 0 }
    }

    fun getMyTemplates(context: Context): List<UserStickerTemplate> {
        return getAllTemplates(context).filter { it.isOwnedByCurrentUser }
    }

    fun getTemplatesByCreator(context: Context, creatorName: String): List<UserStickerTemplate> {
        return getAllTemplates(context).filter {
            it.isPublished && it.creatorName.equals(creatorName, ignoreCase = true)
        }
    }

    fun getTopCreators(context: Context): List<CreatorStatsData> {
        val allPublished = getAllTemplates(context).filter { it.isPublished }
        val grouped = allPublished.groupBy { it.creatorName }

        val creatorAvatarMap = mapOf(
            "MemeKing" to "👑",
            "StickerPro" to "⚡",
            "BubbleMaster" to "💬",
            "CloudMaker" to "☁️",
            "JokerDev" to "😸",
            "LoveStudio" to "💖",
            "CuteArtist" to "🌸",
            "BadgePro" to "🏆",
            "ArtStudio" to "🌌",
            "DesignLab" to "🎨",
            "My Creator" to "⭐"
        )

        val creatorBioMap = mapOf(
            "MemeKing" to "Top viral meme sticker creator. Daily fresh memes!",
            "StickerPro" to "Reaction & trending stickers specialist.",
            "BubbleMaster" to "Custom speech bubble & text frame designer.",
            "CloudMaker" to "Dreamy thought cloud & aesthetic stickers.",
            "JokerDev" to "Funny cat memes & hilarious sticker packs.",
            "LoveStudio" to "Romantic hearts, love frames & cute stickers.",
            "CuteArtist" to "Kawaii animals, cute stickers & pastel frames.",
            "BadgePro" to "Golden badges, achievements & trophies.",
            "ArtStudio" to "Cosmic backgrounds, galaxy glow & dark themes.",
            "DesignLab" to "Creative pop art & vibrant aesthetic designs.",
            "My Creator" to "Sticker lover & creator ✨"
        )

        val result = grouped.map { (name, templates) ->
            val totalUses = templates.sumOf { it.useCount }
            val totalLikes = templates.sumOf { it.likeCount }
            val score = totalUses * 2L + totalLikes * 5L + templates.size * 10L
            val handle = "@${name.replace(" ", "_").lowercase()}"
            val avatar = creatorAvatarMap[name] ?: "🎨"
            val bio = creatorBioMap[name] ?: "Sticker Craft Creator"

            CreatorStatsData(
                id = "creator_${name.lowercase().replace(" ", "_")}",
                name = name,
                handle = handle,
                avatar = avatar,
                bio = bio,
                templatesCount = templates.size,
                totalUses = totalUses,
                totalLikes = totalLikes,
                engagementScore = score,
                verified = true,
                templates = templates
            )
        }

        return result.sortedByDescending { it.engagementScore }
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
            val res = if (templateDir.exists()) {
                templateDir.deleteRecursively()
            } else true
            invalidateCache()
            res
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
                invalidateCache()
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun deleteAllMyTemplates(context: Context): Boolean {
        return try {
            val myTemplates = getMyTemplates(context)
            myTemplates.forEach { template ->
                deleteUserTemplate(context, template.id)
            }
            invalidateCache()
            true
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

    private fun renderProceduralTemplateBitmap(
        title: String,
        subtitle: String,
        category: String,
        emojiStamp: String,
        bgStartColor: Long,
        bgEndColor: Long,
        textColor: Long = 0xFFFFFFFF,
        accentColor: Long = 0xFFFFF59D
    ): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val gradient = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            bgStartColor.toInt(), bgEndColor.toInt(),
            android.graphics.Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        val bgRect = android.graphics.RectF(16f, 16f, size - 16f, size - 16f)
        canvas.drawRoundRect(bgRect, 36f, 36f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = AndroidColor.WHITE
        }
        val innerRect = android.graphics.RectF(32f, 32f, size - 32f, size - 32f)
        canvas.drawRoundRect(innerRect, 28f, 28f, borderPaint)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 96f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(emojiStamp, size / 2f, 190f, emojiPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toInt()
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 2f, 2f, AndroidColor.parseColor("#88000000"))
        }
        canvas.drawText(title, size / 2f, 310f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor.toInt()
            textSize = 20f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            setShadowLayer(4f, 1f, 1f, AndroidColor.parseColor("#66000000"))
        }
        canvas.drawText(subtitle, size / 2f, 360f, subPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.parseColor("#44000000")
        }
        val badgeRect = android.graphics.RectF(size / 2f - 95f, 410f, size / 2f + 95f, 452f)
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)

        val categoryTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 17f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("🏷️ $category", size / 2f, 438f, categoryTagPaint)

        return bmp
    }

    private fun seedCommunityTemplatesIfEmpty(context: Context) {
        // No default templates to seed
    }
}
