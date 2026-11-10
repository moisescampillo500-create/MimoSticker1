package com.mymoises.mystickerapp.font

import android.content.Context
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FontAssetManager {
    private val typefaceCache = mutableMapOf<String, Typeface>()

    val GOOGLE_FONTS_URL_MAP = mapOf(
        "Anton" to "https://raw.githubusercontent.com/google/fonts/main/ofl/anton/Anton-Regular.ttf",
        "Bebas Neue" to "https://raw.githubusercontent.com/google/fonts/main/ofl/bebasneue/BebasNeue-Regular.ttf",
        "Oswald" to "https://raw.githubusercontent.com/google/fonts/main/ofl/oswald/static/Oswald-Bold.ttf",
        "Bangers" to "https://raw.githubusercontent.com/google/fonts/main/ofl/bangers/Bangers-Regular.ttf",
        "Orbitron" to "https://raw.githubusercontent.com/google/fonts/main/ofl/orbitron/static/Orbitron-Bold.ttf",
        "Cinzel" to "https://raw.githubusercontent.com/google/fonts/main/ofl/cinzel/static/Cinzel-Bold.ttf",
        "Playfair Display" to "https://raw.githubusercontent.com/google/fonts/main/ofl/playfairdisplay/static/PlayfairDisplay-Bold.ttf",
        "Abril Fatface" to "https://raw.githubusercontent.com/google/fonts/main/ofl/abrilfatface/AbrilFatface-Regular.ttf",
        "Pacifico" to "https://raw.githubusercontent.com/google/fonts/main/ofl/pacifico/Pacifico-Regular.ttf",
        "Allura" to "https://raw.githubusercontent.com/google/fonts/main/ofl/allura/Allura-Regular.ttf",
        "Permanent Marker" to "https://raw.githubusercontent.com/google/fonts/main/ofl/permanentmarker/PermanentMarker-Regular.ttf",
        "Righteous" to "https://raw.githubusercontent.com/google/fonts/main/ofl/righteous/Righteous-Regular.ttf",
        "Fredoka" to "https://raw.githubusercontent.com/google/fonts/main/ofl/fredoka/static/Fredoka-Bold.ttf",
        "Monoton" to "https://raw.githubusercontent.com/google/fonts/main/ofl/monoton/Monoton-Regular.ttf",
        "Special Elite" to "https://raw.githubusercontent.com/google/fonts/main/apache/specialelite/SpecialElite-Regular.ttf",
        "Bungee" to "https://raw.githubusercontent.com/google/fonts/main/ofl/bungee/Bungee-Regular.ttf",
        "Lilita One" to "https://raw.githubusercontent.com/google/fonts/main/ofl/lilitaone/LilitaOne-Regular.ttf",
        "Alfa Slab One" to "https://raw.githubusercontent.com/google/fonts/main/ofl/alfaslabone/AlfaSlabOne-Regular.ttf",
        "Great Vibes" to "https://raw.githubusercontent.com/google/fonts/main/ofl/greatvibes/GreatVibes-Regular.ttf",
        "Lobster" to "https://raw.githubusercontent.com/google/fonts/main/ofl/lobster/Lobster-Regular.ttf"
    )

    fun getFontFile(context: Context, fontName: String): File {
        val fontsDir = File(context.filesDir, "fonts").apply { if (!exists()) mkdirs() }
        val safeName = fontName.lowercase().replace(" ", "_")
        return File(fontsDir, "$safeName.ttf")
    }

    suspend fun loadTypeface(context: Context, fontName: String): Typeface = withContext(Dispatchers.IO) {
        if (typefaceCache.containsKey(fontName)) {
            return@withContext typefaceCache[fontName]!!
        }

        val fontFile = getFontFile(context, fontName)
        if (fontFile.exists() && fontFile.length() > 5000L) {
            try {
                val tf = Typeface.createFromFile(fontFile)
                typefaceCache[fontName] = tf
                return@withContext tf
            } catch (_: Exception) {
                fontFile.delete()
            }
        }

        val urlStr = GOOGLE_FONTS_URL_MAP[fontName]
        if (urlStr != null) {
            try {
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
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
                        typefaceCache[fontName] = tf
                        return@withContext tf
                    }
                }
            } catch (_: Exception) {
                // network fallback
            }
        }

        val fallback = resolveTypefaceFallback(fontName)
        typefaceCache[fontName] = fallback
        return@withContext fallback
    }

    private fun resolveTypefaceFallback(fontName: String): Typeface {
        return when (fontName) {
            "Anton", "Bebas Neue", "Oswald" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
            "Bangers", "Bungee", "Lilita One", "Alfa Slab One" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            "Pacifico", "Allura", "Great Vibes", "Lobster" -> Typeface.create("cursive", Typeface.BOLD_ITALIC)
            "Permanent Marker", "Special Elite" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            "Cinzel", "Playfair Display", "Abril Fatface" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            "Orbitron", "Righteous", "Fredoka" -> Typeface.create("sans-serif-medium", Typeface.BOLD)
            "Monoton" -> Typeface.create("sans-serif-thin", Typeface.BOLD)
            else -> Typeface.create("sans-serif-black", Typeface.BOLD)
        }
    }
}

data class FontItemSpec(
    val id: String,
    val displayName: String,
    val familyKey: String,
    val category: String,
    val previewText: String = "Sticker Craft"
)

object FontLibrary {
    val FONT_ITEMS = listOf(
        // Key 20 Requested Typefaces
        FontItemSpec("f_anton", "Anton", "Anton", "Bold Display", "ANTON BOLD"),
        FontItemSpec("f_bebas", "Bebas Neue", "Bebas Neue", "Tall & Condensed", "BEBAS NEUE"),
        FontItemSpec("f_oswald", "Oswald", "Oswald", "Tall & Condensed", "OSWALD CONDENSED"),
        FontItemSpec("f_bangers", "Bangers", "Bangers", "Bold Display", "BANGERS COMIC"),
        FontItemSpec("f_orbitron", "Orbitron", "Orbitron", "Geometric", "ORBITRON SCI-FI"),
        FontItemSpec("f_cinzel", "Cinzel", "Cinzel", "Elegant Serif", "CINZEL ELEGANT"),
        FontItemSpec("f_playfair", "Playfair Display", "Playfair Display", "Elegant Serif", "Playfair Serif"),
        FontItemSpec("f_abril", "Abril Fatface", "Abril Fatface", "Elegant Serif", "Abril Fatface"),
        FontItemSpec("f_pacifico", "Pacifico", "Pacifico", "Handwritten & Script", "Pacifico Cursive"),
        FontItemSpec("f_allura", "Allura", "Allura", "Handwritten & Script", "Allura Script"),
        FontItemSpec("f_marker", "Permanent Marker", "Permanent Marker", "Handwritten & Script", "Marker Pen"),
        FontItemSpec("f_righteous", "Righteous", "Righteous", "Geometric", "Righteous Display"),
        FontItemSpec("f_fredoka", "Fredoka", "Fredoka", "Geometric", "Fredoka One"),
        FontItemSpec("f_monoton", "Monoton", "Monoton", "Decorative", "MONOTON"),
        FontItemSpec("f_special", "Special Elite", "Special Elite", "Retro & Vintage", "Special Typewriter"),
        FontItemSpec("f_bungee", "Bungee", "Bungee", "Outline & Display", "BUNGEE HEAVY"),
        FontItemSpec("f_lilita", "Lilita One", "Lilita One", "Bold Display", "LILITA ONE"),
        FontItemSpec("f_alfa", "Alfa Slab One", "Alfa Slab One", "Bold Display", "ALFA SLAB ONE"),
        FontItemSpec("f_vibes", "Great Vibes", "Great Vibes", "Handwritten & Script", "Great Vibes"),
        FontItemSpec("f_lobster", "Lobster", "Lobster", "Handwritten & Script", "Lobster Bold"),

        // Additional Distinct Typefaces
        FontItemSpec("f1", "Condensed Sans", "sans-serif-condensed", "Tall & Condensed"),
        FontItemSpec("f2", "Condensed Light", "sans-serif-condensed-light", "Tall & Condensed"),
        FontItemSpec("f3", "Narrow Gothic", "sans-serif-smallcaps", "Tall & Condensed"),
        FontItemSpec("f4", "Geometric Modern", "sans-serif", "Geometric"),
        FontItemSpec("f5", "Geometric Medium", "sans-serif-medium", "Geometric"),
        FontItemSpec("f6", "Geometric Heavy", "sans-serif-black", "Geometric"),
        FontItemSpec("f7", "Clean Geometry", "sans-serif-light", "Geometric"),
        FontItemSpec("f8", "Metro Clean", "sans-serif-thin", "Geometric"),
        FontItemSpec("f9", "Classic Serif", "serif", "Elegant Serif"),
        FontItemSpec("f10", "Serif Monospace", "serif-monospace", "Elegant Serif"),
        FontItemSpec("f11", "Editorial Serif", "serif", "Elegant Serif"),
        FontItemSpec("f12", "Luxury Serif", "serif", "Elegant Serif"),
        FontItemSpec("f13", "Retro Smallcaps", "sans-serif-smallcaps", "Retro & Vintage"),
        FontItemSpec("f14", "Vintage Typewriter", "monospace", "Retro & Vintage"),
        FontItemSpec("f15", "Oldschool Mono", "monospace", "Retro & Vintage"),
        FontItemSpec("f16", "Classic Newspaper", "serif", "Retro & Vintage"),
        FontItemSpec("f17", "Cursive Script", "cursive", "Handwritten & Script"),
        FontItemSpec("f18", "Casual Hand", "casual", "Handwritten & Script"),
        FontItemSpec("f19", "Signature Script", "cursive", "Handwritten & Script"),
        FontItemSpec("f20", "Sweet Brush", "casual", "Handwritten & Script"),
        FontItemSpec("f21", "Outline Impact", "sans-serif-black", "Outline & Display"),
        FontItemSpec("f22", "Display Heavy", "sans-serif-black", "Outline & Display"),
        FontItemSpec("f23", "Headline Bold", "sans-serif-condensed", "Outline & Display"),
        FontItemSpec("f24", "Poster Display", "sans-serif-black", "Outline & Display"),
        FontItemSpec("f25", "Heavy Sans Black", "sans-serif-black", "Bold Display"),
        FontItemSpec("f26", "Impact Bold", "sans-serif-condensed", "Bold Display"),
        FontItemSpec("f27", "Extra Bold", "sans-serif-medium", "Bold Display"),
        FontItemSpec("f28", "Power Display", "sans-serif-black", "Bold Display"),
        FontItemSpec("f29", "Minimal Thin", "sans-serif-thin", "Minimalist"),
        FontItemSpec("f30", "Minimal Light", "sans-serif-light", "Minimalist"),
        FontItemSpec("f31", "Sleek Sans", "sans-serif", "Minimalist"),
        FontItemSpec("f32", "Clean Minimal", "sans-serif-light", "Minimalist"),
        FontItemSpec("f33", "Monospace Code", "monospace", "Decorative"),
        FontItemSpec("f34", "Arcade Retro", "monospace", "Decorative")
    )
}
