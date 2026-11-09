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
        val baseDir = getUserTemplatesDirectory(context)
        val files = baseDir.listFiles()

        val existingCategories = (files ?: emptyArray()).mapNotNull { dir ->
            if (dir.isDirectory) {
                val jsonFile = File(dir, "template.json")
                if (jsonFile.exists()) {
                    try {
                        JSONObject(jsonFile.readText()).optString("category")
                    } catch (_: Exception) { null }
                } else null
            } else null
        }.toSet()

        val targetCategories = TemplateCategories.CATEGORIES_LIST.filter { it != "All" }

        data class SeedCategorySpec(
            val title: String,
            val subtitle: String,
            val creator: String,
            val cat: String,
            val emoji: String,
            val bgStart: Long,
            val bgEnd: Long,
            val textColor: Long = 0xFFFFFFFF,
            val accentColor: Long = 0xFFFFF59D,
            val uses: Long,
            val likes: Int
        )

        val allSpecs = mutableListOf<SeedCategorySpec>()

        targetCategories.forEach { category ->
            if (!existingCategories.contains(category)) {
                val specsForCat = get10SeedSpecsForCategory(category)
                allSpecs.addAll(specsForCat.map {
                    SeedCategorySpec(it.title, it.subtitle, it.creator, it.cat, it.emoji, it.bgStart, it.bgEnd, it.textColor, it.accentColor, it.uses, it.likes)
                })
            }
        }

        if (allSpecs.isEmpty()) return

        allSpecs.forEachIndexed { idx, spec ->
            val bmp = renderProceduralTemplateBitmap(
                title = spec.title,
                subtitle = spec.subtitle,
                category = spec.cat,
                emojiStamp = spec.emoji,
                bgStartColor = spec.bgStart,
                bgEndColor = spec.bgEnd,
                textColor = spec.textColor,
                accentColor = spec.accentColor
            )

            val layerData = UserTemplateLayerData(
                name = spec.title,
                bitmapFileName = null,
                visible = true
            )

            val uniqueId = "seed_cat_${spec.cat.lowercase().replace(" ", "_").replace("'", "")}_$idx"

            saveUserTemplate(
                context = context,
                templateName = spec.title,
                creatorName = spec.creator,
                category = spec.cat,
                layerDataList = listOf(Triple(spec.title, bmp, layerData)),
                previewBitmap = bmp,
                isPublished = true,
                isOwnedByCurrentUser = false,
                existingId = uniqueId,
                useCount = spec.uses,
                likeCount = spec.likes
            )
        }
    }

    private data class InternalSeedSpec(
        val title: String,
        val subtitle: String,
        val creator: String,
        val cat: String,
        val emoji: String,
        val bgStart: Long,
        val bgEnd: Long,
        val textColor: Long = 0xFFFFFFFF,
        val accentColor: Long = 0xFFFFF59D,
        val uses: Long,
        val likes: Int
    )

    private fun get10SeedSpecsForCategory(category: String): List<InternalSeedSpec> {
        return when (category) {
            "Birthday" -> listOf(
                InternalSeedSpec("🎂 Happy Birthday!", "Make A Wish Today", "PartyStudio", "Birthday", "🎂", 0xFFFF4081, 0xFFC2185B, 0xFFFFFFFF, 0xFFFFEB3B, 5400L, 1200),
                InternalSeedSpec("🎈 Balloon Bash", "Pop The Joy!", "CakeMaster", "Birthday", "🎈", 0xFF7C4DFF, 0xFF512DA8, 0xFFFFFFFF, 0xFFFFD700, 4200L, 890),
                InternalSeedSpec("🍰 Cupcake Wishes", "Sweetest Day Ever", "SweetChef", "Birthday", "🍰", 0xFFFF6E40, 0xFFE64A19, 0xFFFFFFFF, 0xFFFFF59D, 3800L, 760),
                InternalSeedSpec("🎁 Special Gift", "Unwrap Happiness", "GiftCraft", "Birthday", "🎁", 0xFF00E676, 0xFF0091EA, 0xFFFFFFFF, 0xFFFFFF00, 2900L, 510),
                InternalSeedSpec("🎉 Milestone Bash", "Cheers To Your Year", "FestiveArt", "Birthday", "🎉", 0xFFFFD700, 0xFFFF6F00, 0xFFFFFFFF, 0xFFFFFFFF, 6100L, 1430),
                InternalSeedSpec("🥳 Birthday Party", "Celebrate Good Times", "FunStation", "Birthday", "🥳", 0xFFFF1744, 0xFFD50000, 0xFFFFFFFF, 0xFFFFFF00, 4900L, 1020),
                InternalSeedSpec("👑 Birthday Queen", "Slay Your Day", "QueenStudio", "Birthday", "👑", 0xFFE040FB, 0xFF8E24AA, 0xFFFFFFFF, 0xFFFFD700, 7200L, 1650),
                InternalSeedSpec("⭐ Birthday Boy", "Shine Like A Star", "StarMaker", "Birthday", "⭐", 0xFF29B6F6, 0xFF0277BD, 0xFFFFFFFF, 0xFFFFF59D, 5100L, 1100),
                InternalSeedSpec("🕯️ Candle Blow", "Make Big Wishes", "WishFactory", "Birthday", "🕯️", 0xFFAB47BC, 0xFF6A1B9A, 0xFFFFFFFF, 0xFFFFE082, 3400L, 680),
                InternalSeedSpec("✨ Sweet Birthday", "Best Year Ahead", "SparkleArt", "Birthday", "✨", 0xFFFFAB00, 0xFFFF6D00, 0xFFFFFFFF, 0xFFFFFFFF, 4600L, 950)
            )
            "Christmas" -> listOf(
                InternalSeedSpec("🎄 Merry Christmas", "Joy To The World", "XmasStudio", "Christmas", "🎄", 0xFFD50000, 0xFF2E7D32, 0xFFFFFFFF, 0xFFFFD700, 8400L, 1920),
                InternalSeedSpec("❄️ Snow Wonderland", "Let It Snow Today", "FrostArt", "Christmas", "❄️", 0xFF0288D1, 0xFF01579B, 0xFFFFFFFF, 0xFFE0F7FA, 4100L, 880),
                InternalSeedSpec("🎅 Santa Ho Ho Ho!", "Santa Is Coming", "NorthPole", "Christmas", "🎅", 0xFFC62828, 0xFF880E4F, 0xFFFFFFFF, 0xFFFFF59D, 6900L, 1540),
                InternalSeedSpec("🎁 Holiday Gifts", "Warm Festive Hugs", "ElfCraft", "Christmas", "🎁", 0xFF1B5E20, 0xFF388E3C, 0xFFFFFFFF, 0xFFFFD700, 5200L, 1120),
                InternalSeedSpec("⭐ Silent Night", "Peace & Light", "StarGlow", "Christmas", "⭐", 0xFFFFD54F, 0xFFF57F17, 0xFF000000, 0xFFFFFFFF, 3600L, 730),
                InternalSeedSpec("🔔 Jingle Bells", "Holiday Harmony", "BellMusic", "Christmas", "🔔", 0xFFE65100, 0xFFBF360C, 0xFFFFFFFF, 0xFFFFE082, 4800L, 990),
                InternalSeedSpec("☃️ Snowman Hugs", "Cozy Winter Days", "IceCraft", "Christmas", "☃️", 0xFF0097A7, 0xFF006064, 0xFFFFFFFF, 0xFFE0F7FA, 3900L, 810),
                InternalSeedSpec("🎄 Festive Pine", "Magic Xmas Tree", "PineStudio", "Christmas", "🎄", 0xFF2E7D32, 0xFF1B5E20, 0xFFFFFFFF, 0xFFFFD700, 5700L, 1280),
                InternalSeedSpec("🕊️ Peace & Love", "Holiday Blessings", "PeaceArt", "Christmas", "🕊️", 0xFF0277BD, 0xFF002766, 0xFFFFFFFF, 0xFFFFF8E1, 3100L, 620),
                InternalSeedSpec("✨ Yuletide Magic", "Happy Holidays", "GlowStudio", "Christmas", "✨", 0xFFFF6F00, 0xFFD84315, 0xFFFFFFFF, 0xFFFFFFFF, 4400L, 910)
            )
            "Anniversary" -> listOf(
                InternalSeedSpec("💖 Happy Anniversary", "Forever & Always", "LoveStudio", "Anniversary", "💖", 0xFFAD1457, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF80AB, 7800L, 1720),
                InternalSeedSpec("🌹 Years Of Love", "Cheers To Our Love", "RoseArt", "Anniversary", "🌹", 0xFFC2185B, 0xFF880E4F, 0xFFFFFFFF, 0xFFFFD700, 6300L, 1390),
                InternalSeedSpec("🥂 Golden Cheers", "To Many More Years", "CheersCraft", "Anniversary", "🥂", 0xFFFFB300, 0xFFFF6F00, 0xFFFFFFFF, 0xFFFFFFFF, 4900L, 1050),
                InternalSeedSpec("💍 Soulmates", "Love Story Continues", "RingStudio", "Anniversary", "💍", 0xFF7B1FA2, 0xFF4A148C, 0xFFFFFFFF, 0xFFEA80FC, 5400L, 1180),
                InternalSeedSpec("✨ Together As One", "Unbreakable Bond", "BondArt", "Anniversary", "✨", 0xFFD81B60, 0xFFAD1457, 0xFFFFFFFF, 0xFFFFF8E1, 4200L, 890),
                InternalSeedSpec("❤️ Heart To Heart", "You Are My Person", "HeartCraft", "Anniversary", "❤️", 0xFFD32F2F, 0xFFB71C1C, 0xFFFFFFFF, 0xFFFFCDD2, 6800L, 1490),
                InternalSeedSpec("💐 Eternal Romance", "Growing Stronger", "FlowerStudio", "Anniversary", "💐", 0xFFE91E63, 0xFFC2185B, 0xFFFFFFFF, 0xFFF48FB1, 3700L, 780),
                InternalSeedSpec("👑 Couple Empire", "King & Queen Love", "CrownCraft", "Anniversary", "👑", 0xFF6A1B9A, 0xFF311B92, 0xFFFFFFFF, 0xFFFFD700, 5900L, 1310),
                InternalSeedSpec("🕊️ Golden Devotion", "True Love Story", "DevotionArt", "Anniversary", "🕊️", 0xFFF57F17, 0xFFE65100, 0xFFFFFFFF, 0xFFFFF59D, 3300L, 670),
                InternalSeedSpec("💞 Sweet Anniversary", "My Heart Is Yours", "SweetStudio", "Anniversary", "💞", 0xFFC2185B, 0xFF880E4F, 0xFFFFFFFF, 0xFFF8BBD0, 4600L, 980)
            )
            "Monthsary" -> listOf(
                InternalSeedSpec("💕 Happy Monthsary!", "Every Month With You", "CuteStudio", "Monthsary", "💕", 0xFFEC407A, 0xFFC2185B, 0xFFFFFFFF, 0xFFF8BBD0, 6700L, 1480),
                InternalSeedSpec("🥰 1 Month Milestone", "Sweetest Beginning", "BlushArt", "Monthsary", "🥰", 0xFFF06292, 0xFFD81B60, 0xFFFFFFFF, 0xFFFFF8E1, 5100L, 1120),
                InternalSeedSpec("💖 100 Days Love", "Counting Sweet Moments", "CountStudio", "Monthsary", "💖", 0xFFE91E63, 0xFFAD1457, 0xFFFFFFFF, 0xFFFFD700, 5800L, 1290),
                InternalSeedSpec("🌸 Favorite Person", "Love You To Moon", "FloralCraft", "Monthsary", "🌸", 0xFFAB47BC, 0xFF7B1FA2, 0xFFFFFFFF, 0xFFE1BEE7, 4300L, 910),
                InternalSeedSpec("🐣 Baby & Bear", "Cute Couple Moments", "ChicksArt", "Monthsary", "🐣", 0xFFFFA726, 0xFFFB8C00, 0xFFFFFFFF, 0xFFFFF59D, 3900L, 820),
                InternalSeedSpec("🧸 Soft Plushie Love", "Warm Hugs Always", "PlushieStudio", "Monthsary", "🧸", 0xFF8D6E63, 0xFF5D4037, 0xFFFFFFFF, 0xFFD7CCC8, 4700L, 1010),
                InternalSeedSpec("🍓 Sweet Monthsary", "Life Is Sweet", "BerryCraft", "Monthsary", "🍓", 0xFFE53935, 0xFFC62828, 0xFFFFFFFF, 0xFFFFCDD2, 5300L, 1150),
                InternalSeedSpec("🎀 Couple Date", "Treasure Memory", "RibbonArt", "Monthsary", "🎀", 0xFFBA68C8, 0xFF8E24AA, 0xFFFFFFFF, 0xFFE1BEE7, 3500L, 720),
                InternalSeedSpec("💌 Love Note Card", "Written In My Heart", "NoteStudio", "Monthsary", "💌", 0xFFEF5350, 0xFFC62828, 0xFFFFFFFF, 0xFFFFF59D, 4100L, 890),
                InternalSeedSpec("✨ Endless Hugs", "Together Is Best", "StarStudio", "Monthsary", "✨", 0xFFFF7043, 0xFFE64A19, 0xFFFFFFFF, 0xFFFFFFFF, 3800L, 790)
            )
            "Genggeng" -> listOf(
                InternalSeedSpec("🔥 Solid Genggeng!", "Tropang Malakas", "MemeKing", "Genggeng", "🔥", 0xFF111111, 0xFF000000, 0xFFFFFFFF, 0xFFFF1744, 9200L, 2100),
                InternalSeedSpec("😎 Batang Genggeng", "Pangmalakasang Flex", "FlexPro", "Genggeng", "😎", 0xFF1A1A1A, 0xFF000000, 0xFFFFFFFF, 0xFFFFC107, 7800L, 1750),
                InternalSeedSpec("🚀 Viral Tropang Pinoy", "Solid Slay Always", "TropaStudio", "Genggeng", "🚀", 0xFF1A237E, 0xFF000051, 0xFFFFFFFF, 0xFF00E676, 6400L, 1420),
                InternalSeedSpec("⚡ Genggeng Energy", "100% Solid Bro", "BoltArt", "Genggeng", "⚡", 0xFF212121, 0xFF000000, 0xFFFFFFFF, 0xFFFFEA00, 5900L, 1280),
                InternalSeedSpec("🤡 Slay Boss Energy", "Meme King Genggeng", "JokerDev", "Genggeng", "🤡", 0xFF4A148C, 0xFF1A237E, 0xFFFFFFFF, 0xFFE040FB, 8100L, 1890),
                InternalSeedSpec("💯 Batang 90s", "Tambayan Legend", "LegendArt", "Genggeng", "💯", 0xFFBF360C, 0xFF880E4F, 0xFFFFFFFF, 0xFFFFD700, 5100L, 1090),
                InternalSeedSpec("👑 Hari Ng Tambayan", "Bossing Ng Kanto", "KantoKing", "Genggeng", "👑", 0xFF004D40, 0xFF00251A, 0xFFFFFFFF, 0xFF00E676, 6700L, 1510),
                InternalSeedSpec("🔥 Main Character", "Siklab Ng Tropa", "SiklabArt", "Genggeng", "🔥", 0xFF3E2723, 0xFF1B0000, 0xFFFFFFFF, 0xFFFF3D00, 7100L, 1620),
                InternalSeedSpec("💥 Walang Suko Bro", "Laban Lang Tropa", "LabanCraft", "Genggeng", "💥", 0xFF263238, 0xFF000A12, 0xFFFFFFFF, 0xFFFF1744, 4800L, 990),
                InternalSeedSpec("🤙 Solid Tropang Pinoy", "Shaka Pinoy Flex", "PinoyGeng", "Genggeng", "🤙", 0xFF0D47A1, 0xFF002171, 0xFFFFFFFF, 0xFF29B6F6, 5300L, 1170)
            )
            "New Year" -> listOf(
                InternalSeedSpec("🎆 Happy New Year!", "Welcome Fresh 2025", "FestiveArt", "New Year", "🎆", 0xFF000428, 0xFF004E92, 0xFFFFFFFF, 0xFFFFD700, 8900L, 1980),
                InternalSeedSpec("🥂 Hello 2025", "Toast To Future", "CheersKing", "New Year", "🥂", 0xFF111111, 0xFF000000, 0xFFFFFFFF, 0xFFFFD700, 7200L, 1610),
                InternalSeedSpec("⭐ Big Beginnings", "Shine All Year", "GlowStudio", "New Year", "⭐", 0xFF1A1A2E, 0xFF16213E, 0xFFFFFFFF, 0xFFE94560, 5400L, 1190),
                InternalSeedSpec("💥 Fireworks Sky", "Light Up Night", "FireworksArt", "New Year", "💥", 0xFF0F0C29, 0xFF302B63, 0xFFFFFFFF, 0xFF00E676, 6100L, 1340),
                InternalSeedSpec("🥳 Midnight Countdown", "3 2 1 Happy 2025!", "CountdownPro", "New Year", "🥳", 0xFF141E30, 0xFF243B55, 0xFFFFFFFF, 0xFFFFE082, 6800L, 1520),
                InternalSeedSpec("✨ Sparkle 2025", "Bright & Prosperous", "SparkleStudio", "New Year", "✨", 0xFF1F1C2C, 0xFF928DAB, 0xFFFFFFFF, 0xFFFFFFFF, 4300L, 930),
                InternalSeedSpec("🏆 New Year Goals", "Achieve Big Things", "GoalCraft", "New Year", "🏆", 0xFF200122, 0xFF6F0000, 0xFFFFFFFF, 0xFFFFD700, 4900L, 1070),
                InternalSeedSpec("👑 Royal New Year", "Celebrate In Style", "CrownGlow", "New Year", "👑", 0xFF000000, 0xFF1A1A1A, 0xFFFFFFFF, 0xFFFFD700, 5700L, 1260),
                InternalSeedSpec("🎉 New Year Fiesta", "Party Round Clock", "FiestaArt", "New Year", "🎉", 0xFF11998E, 0xFF38EF7D, 0xFFFFFFFF, 0xFFFFFFFF, 3900L, 840),
                InternalSeedSpec("🚀 Soar High 2025", "Reach New Heights", "RocketStudio", "New Year", "🚀", 0xFF0F2027, 0xFF2C5364, 0xFFFFFFFF, 0xFF00E676, 4600L, 980)
            )
            "Valentine's Day" -> listOf(
                InternalSeedSpec("💘 Happy Valentine's", "Love Is In Air", "CupidStudio", "Valentine's Day", "💘", 0xFFD50000, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF8A80, 8700L, 1910),
                InternalSeedSpec("❤️ Be My Valentine", "You Have My Heart", "LoveCraft", "Valentine's Day", "❤️", 0xFFC2185B, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF4081, 7400L, 1630),
                InternalSeedSpec("💌 Sealed With Kiss", "Romantic Letter", "LetterArt", "Valentine's Day", "💌", 0xFFB71C1C, 0xFF5D0000, 0xFFFFFFFF, 0xFFFFCDD2, 5900L, 1290),
                InternalSeedSpec("🌹 Love Roses", "For My Sweetheart", "RoseStudio", "Valentine's Day", "🌹", 0xFF880E4F, 0xFF4A0033, 0xFFFFFFFF, 0xFFF48FB1, 6300L, 1380),
                InternalSeedSpec("💖 Cupid Arrow", "Struck By Love", "ArrowCraft", "Valentine's Day", "💖", 0xFFAD1457, 0xFF6A0036, 0xFFFFFFFF, 0xFFFF80AB, 5100L, 1120),
                InternalSeedSpec("🧸 Honey Bear", "Cuddle Valentine", "TeddyStudio", "Valentine's Day", "🧸", 0xFF5D4037, 0xFF3E2723, 0xFFFFFFFF, 0xFFD7CCC8, 4200L, 890),
                InternalSeedSpec("🌸 Sweetheart", "Precious Flower", "FloralGlow", "Valentine's Day", "🌸", 0xFF9C27B0, 0xFF6A1B9A, 0xFFFFFFFF, 0xFFE1BEE7, 3800L, 810),
                InternalSeedSpec("💞 Eternal Heart", "Pulse Of My Soul", "PulseCraft", "Valentine's Day", "💞", 0xFFE53935, 0xFFB71C1C, 0xFFFFFFFF, 0xFFFFCDD2, 4900L, 1070),
                InternalSeedSpec("👑 True Soulmate", "My Everything", "SoulmateArt", "Valentine's Day", "👑", 0xFF4A148C, 0xFF311B92, 0xFFFFFFFF, 0xFFEA80FC, 5600L, 1240),
                InternalSeedSpec("✨ Romance Glow", "Magic Moments", "RomanceStudio", "Valentine's Day", "✨", 0xFFD81B60, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF80AB, 4400L, 950)
            )
            "Summer" -> listOf(
                InternalSeedSpec("☀️ Summer Vibes", "Sunny Days Smiles", "SunStudio", "Summer", "☀️", 0xFFF57F17, 0xFFE65100, 0xFFFFFFFF, 0xFFFFF59D, 6800L, 1510),
                InternalSeedSpec("🏝️ Tropical Island", "Ocean Calling", "IslandCraft", "Summer", "🏝️", 0xFF00838F, 0xFF006064, 0xFFFFFFFF, 0xFF80DEEA, 5400L, 1180),
                InternalSeedSpec("🏖️ Beach Day Fun", "Sand & Sunshine", "BeachArt", "Summer", "🏖️", 0xFF0097A7, 0xFF00838F, 0xFFFFFFFF, 0xFFB2EBF2, 4900L, 1070),
                InternalSeedSpec("🍹 Sunset Drinks", "Relax & Unwind", "ChillStudio", "Summer", "🍹", 0xFFE65100, 0xFFBF360C, 0xFFFFFFFF, 0xFFFFCCBC, 4200L, 910),
                InternalSeedSpec("🏄 Wave Rider", "Catch Blue Wave", "SurfArt", "Summer", "🏄", 0xFF0277BD, 0xFF01579B, 0xFFFFFFFF, 0xFF81D4FA, 3700L, 780),
                InternalSeedSpec("🌴 Palm Breeze", "Tropical Warmth", "PalmStudio", "Summer", "🌴", 0xFF2E7D32, 0xFF1B5E20, 0xFFFFFFFF, 0xFFA5D6A7, 4500L, 980),
                InternalSeedSpec("🕶️ Cool Shades", "Chilling In Sun", "CoolCraft", "Summer", "🕶️", 0xFFFF6F00, 0xFFE65100, 0xFFFFFFFF, 0xFFFFE082, 5100L, 1120),
                InternalSeedSpec("🍉 Sweet Watermelon", "Juicy Summer Slice", "FruitStudio", "Summer", "🍉", 0xFFC62828, 0xFF2E7D32, 0xFFFFFFFF, 0xFFC8E6C9, 4800L, 1040),
                InternalSeedSpec("🍍 Island Escape", "Sweet Summer", "EscapeArt", "Summer", "🍍", 0xFFFBC02D, 0xFFF57F17, 0xFFFFFFFF, 0xFFFFF9C4, 3600L, 760),
                InternalSeedSpec("✨ Golden Hour", "Sunset Light", "GlowCraft", "Summer", "✨", 0xFFFF6D00, 0xFFDD2C00, 0xFFFFFFFF, 0xFFFFE082, 4300L, 920)
            )
            "Rainy Season" -> listOf(
                InternalSeedSpec("🌧️ Cozy Rain Day", "Rain Drops Outside", "RainStudio", "Rainy Season", "🌧️", 0xFF263238, 0xFF102027, 0xFFFFFFFF, 0xFF90A4AE, 5900L, 1290),
                InternalSeedSpec("☔ Rainy Vibes", "Under My Umbrella", "UmbrellaArt", "Rainy Season", "☔", 0xFF1A237E, 0xFF000051, 0xFFFFFFFF, 0xFF7986CB, 4600L, 990),
                InternalSeedSpec("☕ Hot Cocoa Rain", "Warm Mug Rain Day", "CozyCraft", "Rainy Season", "☕", 0xFF3E2723, 0xFF1B0000, 0xFFFFFFFF, 0xFFD7CCC8, 4100L, 880),
                InternalSeedSpec("☁️ Soft Rain Cloud", "Pitter Patter Rain", "CloudStudio", "Rainy Season", "☁️", 0xFF37474F, 0xFF102027, 0xFFFFFFFF, 0xFFCFD8DC, 3400L, 710),
                InternalSeedSpec("🌧️ Calm Rain Mood", "Listen To Rain", "RainMoodArt", "Rainy Season", "🌧️", 0xFF0D47A1, 0xFF002171, 0xFFFFFFFF, 0xFF64B5F6, 4800L, 1040),
                InternalSeedSpec("🌈 Rainbow After Storm", "Hope After Rain", "RainbowCraft", "Rainy Season", "🌈", 0xFF006064, 0xFF00363A, 0xFFFFFFFF, 0xFF80DEEA, 3900L, 830),
                InternalSeedSpec("🍵 Warm Tea Time", "Cozy Blanket Tea", "TeaStudio", "Rainy Season", "🍵", 0xFF33691E, 0xFF1B3800, 0xFFFFFFFF, 0xFFDCEDC8, 3200L, 660),
                InternalSeedSpec("📖 Rain Reading", "Cozy Book Nook", "BookArt", "Rainy Season", "📖", 0xFF4E342E, 0xFF260D0A, 0xFFFFFFFF, 0xFFD7CCC8, 3700L, 790),
                InternalSeedSpec("🏡 Rainy Haven", "Safe & Warm Inside", "HavenCraft", "Rainy Season", "🏡", 0xFF455A64, 0xFF1C313A, 0xFFFFFFFF, 0xFFB0BEC5, 4300L, 920),
                InternalSeedSpec("✨ Peaceful Rain", "Tranquil Rain Season", "TranquilArt", "Rainy Season", "✨", 0xFF01579B, 0xFF002F6C, 0xFFFFFFFF, 0xFF81D4FA, 3600L, 750)
            )
            "Coffee" -> listOf(
                InternalSeedSpec("☕ But First Coffee", "Fuel Your Morning", "CoffeeKing", "Coffee", "☕", 0xFF3E2723, 0xFF1B0000, 0xFFFFFFFF, 0xFFD7CCC8, 7200L, 1590),
                InternalSeedSpec(" Latte Love Art", "Creamy Latte Smile", "LatteArt", "Coffee", "☕", 0xFF4E342E, 0xFF260D0A, 0xFFFFFFFF, 0xFFE0D6C3, 5800L, 1260),
                InternalSeedSpec(" Espresso Boost", "Strong Dark Brew", "BrewStudio", "Coffee", "☕", 0xFF21100B, 0xFF000000, 0xFFFFFFFF, 0xFF8D6E63, 4900L, 1080),
                InternalSeedSpec(" Warm Cafe Vibes", "Cozy Corner Spot", "CafeCraft", "Coffee", "☕", 0xFF5D4037, 0xFF321B15, 0xFFFFFFFF, 0xFFA1887F, 4100L, 890),
                InternalSeedSpec(" Brewed Fresh", "Aroma In Air", "RoastArt", "Coffee", "☕", 0xFF3C2415, 0xFF190A00, 0xFFFFFFFF, 0xFFD7CCC8, 5300L, 1140),
                InternalSeedSpec(" Coffee Break", "Take Slow Sip", "SipStudio", "Coffee", "☕", 0xFF4A2E1A, 0xFF241308, 0xFFFFFFFF, 0xFFC8B6A6, 3700L, 790),
                InternalSeedSpec(" Cappuccino Hug", "Foamy Warmth", "FoamCraft", "Coffee", "☕", 0xFF5C3A21, 0xFF321C0D, 0xFFFFFFFF, 0xFFE0D2C7, 4400L, 960),
                InternalSeedSpec(" Cold Brew Chill", "Ice Cold Energy", "ColdBrewArt", "Coffee", "☕", 0xFF2C1A0E, 0xFF120800, 0xFFFFFFFF, 0xFF8D6E63, 4800L, 1030),
                InternalSeedSpec(" Coffee & Dreams", "Inspiring Sips", "DreamCafe", "Coffee", "☕", 0xFF3D2314, 0xFF1D0C03, 0xFFFFFFFF, 0xFFBCAAA4, 3900L, 820),
                InternalSeedSpec(" Morning Roast", "Start Day Right", "RoastCraft", "Coffee", "☕", 0xFF4E2A18, 0xFF271108, 0xFFFFFFFF, 0xFFD7CCC8, 4500L, 970)
            )
            "Nutrition Month" -> listOf(
                InternalSeedSpec("🥗 Healthy Eating", "Nourish Your Body", "NutriStudio", "Nutrition Month", "🥗", 0xFF1B5E20, 0xFF003300, 0xFFFFFFFF, 0xFF81C784, 6100L, 1340),
                InternalSeedSpec("🍎 Fresh & Fit", "An Apple A Day", "FitCraft", "Nutrition Month", "🍎", 0xFFC62828, 0xFF8E0000, 0xFFFFFFFF, 0xFFFFCDD2, 5300L, 1160),
                InternalSeedSpec("🥦 Green Power", "Eat Your Greens", "VeggieStudio", "Nutrition Month", "🥦", 0xFF2E7D32, 0xFF005005, 0xFFFFFFFF, 0xFFC8E6C9, 4700L, 1020),
                InternalSeedSpec("🍓 Fruit Smoothie", "Vitamins Boost", "FruitArt", "Nutrition Month", "🍓", 0xFFAD1457, 0xFF78002E, 0xFFFFFFFF, 0xFFF8BBD0, 4200L, 910),
                InternalSeedSpec("🥑 Veggie Delight", "Healthy Meal Goal", "HealthCraft", "Nutrition Month", "🥑", 0xFF33691E, 0xFF113E00, 0xFFFFFFFF, 0xFFDCEDC8, 3800L, 810),
                InternalSeedSpec("🏃 Active Body", "Exercise Good Food", "ActiveStudio", "Nutrition Month", "🏃", 0xFF00838F, 0xFF005662, 0xFFFFFFFF, 0xFF80DEEA, 4400L, 950),
                InternalSeedSpec("🍊 Vitamin C Boost", "Citrus Fresh Energy", "CitrusArt", "Nutrition Month", "🍊", 0xFFE65100, 0xFFAC1900, 0xFFFFFFFF, 0xFFFFE082, 4900L, 1060),
                InternalSeedSpec("🥕 Healthy Choice", "Good Food Mood", "CarrotCraft", "Nutrition Month", "🥕", 0xFFEF6C00, 0xFFB63D00, 0xFFFFFFFF, 0xFFFFCCBC, 3600L, 770),
                InternalSeedSpec("💪 Wellness Goal", "Strong Healthy Life", "WellnessPro", "Nutrition Month", "💪", 0xFF00695C, 0xFF003D33, 0xFFFFFFFF, 0xFFA7FFEB, 5200L, 1130),
                InternalSeedSpec("✨ Mindful Living", "Balanced Diet Always", "BalanceArt", "Nutrition Month", "✨", 0xFF558B2F, 0xFF255D00, 0xFFFFFFFF, 0xFFF0F4C3, 3900L, 840)
            )
            "National Holidays" -> listOf(
                InternalSeedSpec("🇵🇭 Araw Ng Kalayaan", "Mabuhay Ang Pilipinas!", "PinoyStudio", "National Holidays", "🇵🇭", 0xFF0D47A1, 0xFFB71C1C, 0xFFFFFFFF, 0xFFFFD700, 9800L, 2210),
                InternalSeedSpec("⭐ Bayaning Pilipino", "Dangal Ng Lahi", "HeroCraft", "National Holidays", "⭐", 0xFFFFB300, 0xFFF57F17, 0xFFFFFFFF, 0xFFFFF59D, 6400L, 1420),
                InternalSeedSpec("🇵🇭 Araw Ng Kagitingan", "Kagitingan Ng Bayan", "ValorArt", "National Holidays", "🇵🇭", 0xFF1565C0, 0xFFC62828, 0xFFFFFFFF, 0xFFFFD700, 5700L, 1260),
                InternalSeedSpec("☀️ Independence Glow", "Freedom & Honor", "FreedomStudio", "National Holidays", "☀️", 0xFFF57F17, 0xFFBC5100, 0xFFFFFFFF, 0xFFFFF9C4, 4800L, 1050),
                InternalSeedSpec("🇵🇭 Bonifacio Day", "Tapang Ng Katipunan", "KatipunanArt", "National Holidays", "🇵🇭", 0xFFB71C1C, 0xFF7F0000, 0xFFFFFFFF, 0xFFFFCDD2, 5100L, 1130),
                InternalSeedSpec("🇵🇭 Rizal Day Honor", "Dakilang Bayani", "RizalStudio", "National Holidays", "🇵🇭", 0xFF1A237E, 0xFF000051, 0xFFFFFFFF, 0xFF9FA8DA, 4300L, 920),
                InternalSeedSpec("⭐ Lupang Hinirang", "Perlas Ng Silangan", "PerlasCraft", "National Holidays", "⭐", 0xFF00695C, 0xFF003D33, 0xFFFFFFFF, 0xFF80CBC4, 3900L, 840),
                InternalSeedSpec("🇵🇭 Certified Pinoy", "Atin Ang Tagumpay", "PinoyPride", "National Holidays", "🇵🇭", 0xFFC62828, 0xFF8E0000, 0xFFFFFFFF, 0xFFFFE082, 6200L, 1370),
                InternalSeedSpec("🇵🇭 Watawat Pride", "Tatlong Bituin Araw", "FlagStudio", "National Holidays", "🇵🇭", 0xFF0D47A1, 0xFF002171, 0xFFFFFFFF, 0xFFFFCDD2, 4600L, 990),
                InternalSeedSpec("✨ Mabuhay Pilipinas", "Ipagmalaki Lahi", "MabuhayArt", "National Holidays", "✨", 0xFFD81B60, 0xFF880E4F, 0xFFFFFFFF, 0xFFFFF59D, 5300L, 1180)
            )
            "Halloween" -> listOf(
                InternalSeedSpec("🎃 Happy Halloween!", "Spooky Pumpkin Night", "SpookyStudio", "Halloween", "🎃", 0xFFE65100, 0xFFAC1900, 0xFFFFFFFF, 0xFFFFB74D, 8300L, 1840),
                InternalSeedSpec("👻 Spooky Season", "Boo! Spooky Fun", "GhostCraft", "Halloween", "👻", 0xFF311B92, 0xFF000063, 0xFFFFFFFF, 0xFFD1C4E9, 6900L, 1520),
                InternalSeedSpec("🦇 Trick Or Treat", "Candy & Costumes", "BatStudio", "Halloween", "🦇", 0xFF12005E, 0xFF000033, 0xFFFFFFFF, 0xFF9C4DCC, 5600L, 1230),
                InternalSeedSpec("🧹 Witching Hour", "Brewing Spooky Magic", "WitchArt", "Halloween", "🧹", 0xFF4A148C, 0xFF12005E, 0xFFFFFFFF, 0xFFEA80FC, 4800L, 1050),
                InternalSeedSpec("🕷️ Creepy Spider", "Entangled In Magic", "WebCraft", "Halloween", "🕷️", 0xFF212121, 0xFF000000, 0xFFFFFFFF, 0xFFB0BEC5, 3900L, 830),
                InternalSeedSpec("💀 Skeleton Squad", "Bone-Chilling Party", "SkullStudio", "Halloween", "💀", 0xFF1A1A1A, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 6100L, 1340),
                InternalSeedSpec("🌕 Full Moon Horror", "Werewolf Moon Glow", "MoonArt", "Halloween", "🌕", 0xFF263238, 0xFF102027, 0xFFFFFFFF, 0xFFFFF176, 4300L, 920),
                InternalSeedSpec("🖤 Dark Gothic", "Mysterious Spooky", "GothicCraft", "Halloween", "🖤", 0xFF000000, 0xFF1A1A1A, 0xFFFFFFFF, 0xFF8E24AA, 5100L, 1120),
                InternalSeedSpec("🍬 Candy Monster", "Sweet Treats Haul", "CandyStudio", "Halloween", "🍬", 0xFFF57F17, 0xFFBC5100, 0xFFFFFFFF, 0xFFFFE082, 4500L, 970),
                InternalSeedSpec("✨ Spooky Night", "Haunted House Fun", "HauntedArt", "Halloween", "✨", 0xFF3E2723, 0xFF1B0000, 0xFFFFFFFF, 0xFFFFAB91, 3700L, 780)
            )
            "Party" -> listOf(
                InternalSeedSpec("🎉 You're Invited!", "Join Big Celebration", "PartyKing", "Party", "🎉", 0xFFC2185B, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF80AB, 7900L, 1750),
                InternalSeedSpec("🪩 Party All Night", "Dance Floor Fever", "DiscoStudio", "Party", "🪩", 0xFF00838F, 0xFF005662, 0xFFFFFFFF, 0xFF80DEEA, 6300L, 1390),
                InternalSeedSpec("🍾 Celebration Fiesta", "Pop The Champagne", "FiestaCraft", "Party", "🍾", 0xFFFFB300, 0xFFF57F17, 0xFFFFFFFF, 0xFFFFF59D, 5800L, 1270),
                InternalSeedSpec("🎶 Disco Night", "Feel The Beats", "MusicArt", "Party", "🎶", 0xFF7B1FA2, 0xFF4A148C, 0xFFFFFFFF, 0xFFE1BEE7, 4700L, 1020),
                InternalSeedSpec("🥳 VIP Party Pass", "Exclusive Guest Access", "VIPStudio", "Party", "🥳", 0xFFD81B60, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF8A80, 5400L, 1180),
                InternalSeedSpec("💃 Dance Floor Heat", "Groove & Celebrate", "DanceCraft", "Party", "💃", 0xFFE65100, 0xFFAC1900, 0xFFFFFFFF, 0xFFFFCCBC, 4100L, 890),
                InternalSeedSpec("🍹 Cocktail Night", "Cheers Good Times", "BarStudio", "Party", "🍹", 0xFF0097A7, 0xFF006064, 0xFFFFFFFF, 0xFF80CBC4, 3800L, 810),
                InternalSeedSpec("🎈 Grand Bash", "Epic Party Tonight", "BashArt", "Party", "🎈", 0xFFFF1744, 0xFFD50000, 0xFFFFFFFF, 0xFFFFCDD2, 6600L, 1460),
                InternalSeedSpec("✨ Weekend Vibe", "Unwind & Celebrate", "WeekendCraft", "Party", "✨", 0xFF6A1B9A, 0xFF311B92, 0xFFFFFFFF, 0xFFEA80FC, 4300L, 930),
                InternalSeedSpec("🔥 Lit Night Out", "Unforgettable Night", "LitStudio", "Party", "🔥", 0xFFBF360C, 0xFF870000, 0xFFFFFFFF, 0xFFFF9E80, 5100L, 1110)
            )
            else -> listOf(
                InternalSeedSpec("🎨 Creative Design", "Express Your Art", "DesignLab", category, "🎨", 0xFF3F51B5, 0xFF1A237E, 0xFFFFFFFF, 0xFFFFD700, 3100L, 640),
                InternalSeedSpec("🌟 Star Special", "Shine Big & Bright", "StarStudio", category, "🌟", 0xFFE91E63, 0xFFAD1457, 0xFFFFFFFF, 0xFFFFF59D, 2800L, 590),
                InternalSeedSpec("💖 Love & Charm", "Made With Love", "LoveCraft", category, "💖", 0xFFD81B60, 0xFF880E4F, 0xFFFFFFFF, 0xFFFF80AB, 3400L, 720),
                InternalSeedSpec("🔥 Viral Trend", "Hot & Trending", "MemeKing", category, "🔥", 0xFF212121, 0xFF000000, 0xFFFFFFFF, 0xFFFF1744, 4100L, 880),
                InternalSeedSpec("🌸 Cute Blossom", "Sweet Pastel Vibes", "CuteArtist", category, "🌸", 0xFFAB47BC, 0xFF6A1B9A, 0xFFFFFFFF, 0xFFE1BEE7, 3600L, 750),
                InternalSeedSpec("✨ Sparkle Glow", "Magical Moments", "GlowArt", category, "✨", 0xFF00838F, 0xFF004D40, 0xFFFFFFFF, 0xFF80DEEA, 2900L, 610),
                InternalSeedSpec("👑 Royal Badge", "Premium Collection", "BadgePro", category, "👑", 0xFFFFB300, 0xFFF57F17, 0xFFFFFFFF, 0xFFFFFFFF, 3300L, 690),
                InternalSeedSpec("🚀 Rocket Boost", "Take Off High", "RocketStudio", category, "🚀", 0xFF1565C0, 0xFF0D47A1, 0xFFFFFFFF, 0xFF00E676, 2700L, 530),
                InternalSeedSpec("🌈 Rainbow Smile", "Brighten Up Today", "SmileCraft", category, "🌈", 0xFF8E24AA, 0xFF4A148C, 0xFFFFFFFF, 0xFFFFF59D, 3500L, 710),
                InternalSeedSpec("🎈 Celebration Time", "Party Every Day", "FiestaArt", category, "🎈", 0xFFFF3D00, 0xFFDD2C00, 0xFFFFFFFF, 0xFFFFFF00, 3900L, 810)
            )
        }
    }
}
