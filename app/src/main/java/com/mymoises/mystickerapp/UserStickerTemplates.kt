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
                        val useCount = rootObj.optLong("useCount", 0L)
                        val likeCount = rootObj.optInt("likeCount", 0)
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

    fun deleteAllMyTemplates(context: Context): Boolean {
        return try {
            val myTemplates = getMyTemplates(context)
            myTemplates.forEach { template ->
                deleteUserTemplate(context, template.id)
            }
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

    private fun seedCommunityTemplatesIfEmpty(context: Context) {
        val baseDir = getUserTemplatesDirectory(context)
        val files = baseDir.listFiles()
        if (files != null && files.isNotEmpty()) return

        data class SeedSpec(
            val name: String,
            val creator: String,
            val cat: String,
            val resId: Int,
            val uses: Long,
            val likes: Int
        )

        val samples = listOf(
            SeedSpec("LOL Party Meme", "MemeKing", "Meme", R.drawable.ic_sticker_bubble_lol, 4800L, 920),
            SeedSpec("Cool Doge Reaction", "StickerPro", "Reaction", R.drawable.ic_sticker_meme_doge, 3500L, 710),
            SeedSpec("Shoutout Speech Bubble", "BubbleMaster", "Speech Bubbles", R.drawable.ic_sticker_bubble_lol, 2900L, 430),
            SeedSpec("Deep Thought Cloud", "CloudMaker", "Thought Bubbles", R.drawable.ic_sticker_bubble_lol, 1800L, 290),
            SeedSpec("Funny Laughing Cat", "JokerDev", "Funny", R.drawable.ic_sticker_cute_cat_heart, 5200L, 1150),
            SeedSpec("Double Heart Kiss", "LoveStudio", "Love", R.drawable.ic_sticker_love_double_hearts, 6400L, 1420),
            SeedSpec("Cat Love Frame", "CuteArtist", "Cute", R.drawable.ic_sticker_cute_cat_heart, 4100L, 880),
            SeedSpec("Golden Badge Sticker", "BadgePro", "Stickers", R.drawable.ic_sticker_love_double_hearts, 2200L, 380),
            SeedSpec("Galaxy Background Glow", "ArtStudio", "Backgrounds", R.drawable.ic_sticker_bubble_lol, 3100L, 640),
            SeedSpec("Neon Pop Creative Art", "DesignLab", "Creative", R.drawable.ic_sticker_cute_cat_heart, 2700L, 510)
        )

        samples.forEachIndexed { idx, spec ->
            val bmp = BitmapFactory.decodeResource(context.resources, spec.resId) ?: return@forEachIndexed
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
            canvas.drawText(spec.name, 256f, 470f, textPaint)

            val layerData = UserTemplateLayerData(
                name = spec.name,
                bitmapFileName = null,
                visible = true
            )

            saveUserTemplate(
                context = context,
                templateName = spec.name,
                creatorName = spec.creator,
                category = spec.cat,
                layerDataList = listOf(Triple(spec.name, bmp, layerData)),
                previewBitmap = preview,
                isPublished = true,
                isOwnedByCurrentUser = false,
                existingId = "community_seed_$idx",
                useCount = spec.uses,
                likeCount = spec.likes
            )
        }
    }
}
