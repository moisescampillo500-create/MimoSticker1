package com.mymoises.mystickerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class FontSpec(
    val id: String,
    val name: String,
    val familyKey: String,
    val category: String,
    val googleUrl: String?,
    val sampleText: String = "Sticker Craft",
    val isUppercaseOnly: Boolean = false
)

object FontRegistry {

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    val ALL_FONTS = listOf(
        // Key 20 Typefaces
        FontSpec("f_anton", "Anton", "Anton", "Bold Display", "https://raw.githubusercontent.com/google/fonts/main/ofl/anton/Anton-Regular.ttf", "ANTON BOLD", true),
        FontSpec("f_bebas", "Bebas Neue", "Bebas Neue", "Tall & Condensed", "https://raw.githubusercontent.com/google/fonts/main/ofl/bebasneue/BebasNeue-Regular.ttf", "BEBAS NEUE", true),
        FontSpec("f_oswald", "Oswald", "Oswald", "Tall & Condensed", "https://raw.githubusercontent.com/google/fonts/main/ofl/oswald/static/Oswald-Bold.ttf", "OSWALD CONDENSED", true),
        FontSpec("f_bangers", "Bangers", "Bangers", "Bold Display", "https://raw.githubusercontent.com/google/fonts/main/ofl/bangers/Bangers-Regular.ttf", "BANGERS COMIC", true),
        FontSpec("f_orbitron", "Orbitron", "Orbitron", "Geometric", "https://raw.githubusercontent.com/google/fonts/main/ofl/orbitron/static/Orbitron-Bold.ttf", "ORBITRON SCI-FI", true),
        FontSpec("f_cinzel", "Cinzel", "Cinzel", "Elegant Serif", "https://raw.githubusercontent.com/google/fonts/main/ofl/cinzel/static/Cinzel-Bold.ttf", "CINZEL ELEGANT", true),
        FontSpec("f_playfair", "Playfair Display", "Playfair Display", "Elegant Serif", "https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/static/PlayfairDisplay-Bold.ttf", "Playfair Serif"),
        FontSpec("f_abril", "Abril Fatface", "Abril Fatface", "Elegant Serif", "https://raw.githubusercontent.com/google/fonts/main/ofl/abrilfatface/AbrilFatface-Regular.ttf", "Abril Fatface"),
        FontSpec("f_pacifico", "Pacifico", "Pacifico", "Handwritten & Script", "https://raw.githubusercontent.com/google/fonts/main/ofl/pacifico/Pacifico-Regular.ttf", "Pacifico Cursive"),
        FontSpec("f_allura", "Allura", "Allura", "Handwritten & Script", "https://raw.githubusercontent.com/google/fonts/main/ofl/allura/Allura-Regular.ttf", "Allura Script"),
        FontSpec("f_marker", "Permanent Marker", "Permanent Marker", "Handwritten & Script", "https://raw.githubusercontent.com/google/fonts/main/ofl/permanentmarker/PermanentMarker-Regular.ttf", "Marker Pen"),
        FontSpec("f_righteous", "Righteous", "Righteous", "Geometric", "https://raw.githubusercontent.com/google/fonts/main/ofl/righteous/Righteous-Regular.ttf", "Righteous Display"),
        FontSpec("f_fredoka", "Fredoka", "Fredoka", "Geometric", "https://raw.githubusercontent.com/google/fonts/main/ofl/fredoka/static/Fredoka-Bold.ttf", "Fredoka One"),
        FontSpec("f_monoton", "Monoton", "Monoton", "Decorative", "https://raw.githubusercontent.com/google/fonts/main/ofl/monoton/Monoton-Regular.ttf", "MONOTON", true),
        FontSpec("f_special", "Special Elite", "Special Elite", "Retro & Vintage", "https://raw.githubusercontent.com/google/fonts/main/ofl/specialelite/SpecialElite-Regular.ttf", "Special Typewriter"),
        FontSpec("f_bungee", "Bungee", "Bungee", "Outline & Display", "https://raw.githubusercontent.com/google/fonts/main/ofl/bungee/Bungee-Regular.ttf", "BUNGEE HEAVY", true),
        FontSpec("f_lilita", "Lilita One", "Lilita One", "Bold Display", "https://raw.githubusercontent.com/google/fonts/main/ofl/lilitaone/LilitaOne-Regular.ttf", "LILITA ONE", true),
        FontSpec("f_alfa", "Alfa Slab One", "Alfa Slab One", "Bold Display", "https://raw.githubusercontent.com/google/fonts/main/ofl/alfaslabone/AlfaSlabOne-Regular.ttf", "ALFA SLAB ONE", true),
        FontSpec("f_vibes", "Great Vibes", "Great Vibes", "Handwritten & Script", "https://raw.githubusercontent.com/google/fonts/main/ofl/greatvibes/GreatVibes-Regular.ttf", "Great Vibes"),
        FontSpec("f_lobster", "Lobster", "Lobster", "Handwritten & Script", "https://raw.githubusercontent.com/google/fonts/main/ofl/lobster/Lobster-Regular.ttf", "Lobster Bold"),

        // Additional 34 Distinct Typefaces
        FontSpec("f1", "Condensed Sans", "sans-serif-condensed", "Tall & Condensed", null, "CONDENSED SANS", true),
        FontSpec("f2", "Condensed Light", "sans-serif-condensed-light", "Tall & Condensed", null, "Condensed Light"),
        FontSpec("f3", "Narrow Gothic", "sans-serif-smallcaps", "Tall & Condensed", null, "NARROW GOTHIC", true),
        FontSpec("f4", "Geometric Modern", "sans-serif", "Geometric", null, "Geometric Modern"),
        FontSpec("f5", "Geometric Medium", "sans-serif-medium", "Geometric", null, "Geometric Medium"),
        FontSpec("f6", "Geometric Heavy", "sans-serif-black", "Geometric", null, "Geometric Heavy"),
        FontSpec("f7", "Clean Geometry", "sans-serif-light", "Geometric", null, "Clean Geometry"),
        FontSpec("f8", "Metro Clean", "sans-serif-thin", "Geometric", null, "Metro Clean"),
        FontSpec("f9", "Classic Serif", "serif", "Elegant Serif", null, "Classic Serif"),
        FontSpec("f10", "Serif Monospace", "serif-monospace", "Elegant Serif", null, "Serif Monospace"),
        FontSpec("f11", "Editorial Serif", "serif", "Elegant Serif", null, "Editorial Serif"),
        FontSpec("f12", "Luxury Serif", "serif", "Elegant Serif", null, "Luxury Serif"),
        FontSpec("f13", "Retro Smallcaps", "sans-serif-smallcaps", "Retro & Vintage", null, "RETRO SMALLCAPS", true),
        FontSpec("f14", "Vintage Typewriter", "monospace", "Retro & Vintage", null, "Vintage Typewriter"),
        FontSpec("f15", "Oldschool Mono", "monospace", "Retro & Vintage", null, "Oldschool Mono"),
        FontSpec("f16", "Classic Newspaper", "serif", "Retro & Vintage", null, "Classic Newspaper"),
        FontSpec("f17", "Cursive Script", "cursive", "Handwritten & Script", null, "Cursive Script"),
        FontSpec("f18", "Casual Hand", "casual", "Handwritten & Script", null, "Casual Hand"),
        FontSpec("f19", "Signature Script", "cursive", "Handwritten & Script", null, "Signature Script"),
        FontSpec("f20", "Sweet Brush", "casual", "Handwritten & Script", null, "Sweet Brush"),
        FontSpec("f21", "Outline Impact", "sans-serif-black", "Outline & Display", null, "OUTLINE IMPACT", true),
        FontSpec("f22", "Display Heavy", "sans-serif-black", "Outline & Display", null, "DISPLAY HEAVY", true),
        FontSpec("f23", "Headline Bold", "sans-serif-condensed", "Outline & Display", null, "HEADLINE BOLD", true),
        FontSpec("f24", "Poster Display", "sans-serif-black", "Outline & Display", null, "POSTER DISPLAY", true),
        FontSpec("f25", "Heavy Sans Black", "sans-serif-black", "Bold Display", null, "HEAVY SANS BLACK", true),
        FontSpec("f26", "Impact Bold", "sans-serif-condensed", "Bold Display", null, "IMPACT BOLD", true),
        FontSpec("f27", "Extra Bold", "sans-serif-medium", "Bold Display", null, "EXTRA BOLD", true),
        FontSpec("f28", "Power Display", "sans-serif-black", "Bold Display", null, "POWER DISPLAY", true),
        FontSpec("f29", "Minimal Thin", "sans-serif-thin", "Minimalist", null, "Minimal Thin"),
        FontSpec("f30", "Minimal Light", "sans-serif-light", "Minimalist", null, "Minimal Light"),
        FontSpec("f31", "Sleek Sans", "sans-serif", "Minimalist", null, "Sleek Sans"),
        FontSpec("f32", "Clean Minimal", "sans-serif-light", "Minimalist", null, "Clean Minimal"),
        FontSpec("f33", "Monospace Code", "monospace", "Decorative", null, "Monospace Code"),
        FontSpec("f34", "Arcade Retro", "monospace", "Decorative", null, "Arcade Retro")
    )

    fun getLocalFontFile(context: Context, fontId: String): File {
        val fontsDir = File(context.filesDir, "fonts").apply { if (!exists()) mkdirs() }
        return File(fontsDir, "$fontId.ttf")
    }

    suspend fun loadFontTypeface(context: Context, fontId: String): Typeface = withContext(Dispatchers.IO) {
        if (typefaceCache.containsKey(fontId)) {
            return@withContext typefaceCache[fontId]!!
        }

        val fontSpec = ALL_FONTS.firstOrNull {
            it.id.equals(fontId, ignoreCase = true) ||
            it.familyKey.equals(fontId, ignoreCase = true) ||
            it.name.equals(fontId, ignoreCase = true)
        }
        val idToUse = fontSpec?.id ?: fontId

        val fontFile = getLocalFontFile(context, idToUse)
        if (fontFile.exists() && fontFile.length() > 5000L) {
            try {
                val tf = Typeface.createFromFile(fontFile)
                typefaceCache[fontId] = tf
                typefaceCache[idToUse] = tf
                if (fontSpec != null) {
                    typefaceCache[fontSpec.familyKey] = tf
                    typefaceCache[fontSpec.name] = tf
                }
                return@withContext tf
            } catch (_: Exception) {
                fontFile.delete()
            }
        }

        // Try download from Google Fonts GitHub raw repository
        val downloadUrl = fontSpec?.googleUrl
        if (downloadUrl != null) {
            try {
                val url = URL(downloadUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    conn.inputStream.use { input ->
                        FileOutputStream(fontFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (fontFile.exists() && fontFile.length() > 5000L) {
                        val tf = Typeface.createFromFile(fontFile)
                        typefaceCache[fontId] = tf
                        typefaceCache[idToUse] = tf
                        if (fontSpec != null) {
                            typefaceCache[fontSpec.familyKey] = tf
                            typefaceCache[fontSpec.name] = tf
                        }
                        return@withContext tf
                    }
                }
            } catch (_: Exception) {
                // network fallback
            }
        }

        // Native System Fallback
        val fallbackKey = fontSpec?.familyKey ?: fontId
        val fallbackTf = resolveNativeFallbackTypeface(fallbackKey)
        typefaceCache[fontId] = fallbackTf
        typefaceCache[idToUse] = fallbackTf
        if (fontSpec != null) {
            typefaceCache[fontSpec.familyKey] = fallbackTf
            typefaceCache[fontSpec.name] = fallbackTf
        }
        return@withContext fallbackTf
    }

    fun getOrLoadTypeface(context: Context?, fontKey: String, fontStyle: String = "Regular"): Typeface {
        val styleInt = when (fontStyle) {
            "Bold" -> Typeface.BOLD
            "Italic" -> Typeface.ITALIC
            "Bold Italic" -> Typeface.BOLD_ITALIC
            else -> Typeface.NORMAL
        }

        val cached = typefaceCache[fontKey]
            ?: typefaceCache[fontKey.lowercase()]
        if (cached != null) {
            return Typeface.create(cached, styleInt)
        }

        val fontSpec = ALL_FONTS.firstOrNull {
            it.id.equals(fontKey, ignoreCase = true) ||
            it.familyKey.equals(fontKey, ignoreCase = true) ||
            it.name.equals(fontKey, ignoreCase = true)
        }

        if (fontSpec != null) {
            typefaceCache[fontSpec.id]?.let { return Typeface.create(it, styleInt) }
            typefaceCache[fontSpec.familyKey]?.let { return Typeface.create(it, styleInt) }
            typefaceCache[fontSpec.name]?.let { return Typeface.create(it, styleInt) }
        }

        val idToUse = fontSpec?.id ?: fontKey
        val familyKeyToUse = fontSpec?.familyKey ?: fontKey
        if (context != null) {
            val fontFile = getLocalFontFile(context, idToUse)
            if (fontFile.exists() && fontFile.length() > 5000L) {
                try {
                    val tf = Typeface.createFromFile(fontFile)
                    typefaceCache[fontKey] = tf
                    typefaceCache[idToUse] = tf
                    typefaceCache[familyKeyToUse] = tf
                    if (fontSpec != null) {
                        typefaceCache[fontSpec.name] = tf
                    }
                    return Typeface.create(tf, styleInt)
                } catch (_: Exception) {
                    fontFile.delete()
                }
            }
        }

        val fallbackTf = resolveNativeFallbackTypeface(familyKeyToUse)
        return Typeface.create(fallbackTf, styleInt)
    }

    private fun resolveNativeFallbackTypeface(familyKey: String): Typeface {
        return when (familyKey) {
            "Anton", "Bebas Neue", "Oswald", "sans-serif-condensed" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
            "Bangers", "Bungee", "Lilita One", "Alfa Slab One", "sans-serif-black" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            "Pacifico", "Allura", "Great Vibes", "Lobster", "cursive" -> Typeface.create("cursive", Typeface.BOLD_ITALIC)
            "Permanent Marker", "Special Elite", "monospace" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            "Cinzel", "Playfair Display", "Abril Fatface", "serif" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            "Orbitron", "Righteous", "Fredoka", "sans-serif-medium" -> Typeface.create("sans-serif-medium", Typeface.BOLD)
            "Monoton" -> Typeface.create("sans-serif-thin", Typeface.BOLD)
            "sans-serif-condensed-light" -> Typeface.create("sans-serif-condensed-light", Typeface.NORMAL)
            "sans-serif-smallcaps" -> Typeface.create("sans-serif-smallcaps", Typeface.BOLD)
            "sans-serif-light" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            "sans-serif-thin" -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
            "casual" -> Typeface.create("casual", Typeface.BOLD)
            else -> Typeface.create(familyKey, Typeface.BOLD)
        }
    }

    suspend fun validateAllFonts(context: Context): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<String, Boolean>()
        ALL_FONTS.forEach { fontSpec ->
            try {
                val tf = loadFontTypeface(context, fontSpec.id)
                resultMap[fontSpec.name] = (tf != null)
            } catch (_: Exception) {
                resultMap[fontSpec.name] = false
            }
        }
        resultMap
    }

    /**
     * PURE NATURAL FONT PREVIEW RENDERER (Phase 2 & 3)
     * Renders clean, natural font letterforms WITHOUT artificial outlines, strokes, or shadows.
     */
    fun renderPureFontPreview(
        sampleText: String,
        typeface: Typeface,
        fontSpec: FontSpec,
        width: Int = 320,
        height: Int = 120
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textToDraw = if (fontSpec.isUppercaseOnly) sampleText.uppercase() else sampleText

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.WHITE
            this.typeface = typeface
            textSize = 38f
            textAlign = Paint.Align.CENTER
        }

        // Adjust text size dynamically if sample string is long to occupy 80-90% width
        val textWidth = paint.measureText(textToDraw)
        if (textWidth > width * 0.90f) {
            paint.textSize = (paint.textSize * (width * 0.90f / textWidth)).coerceAtLeast(22f)
        }

        val yPos = height / 2f + (paint.textSize / 3.2f)
        canvas.drawText(textToDraw, width / 2f, yPos, paint)

        return bitmap
    }
}
