package com.mymoises.mystickerapp.model

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mymoises.mystickerapp.util.calculateVisibleBounds

data class EditorToolSpec(
    val name: String,
    val icon: String,
    val onClick: () -> Unit
)

class StickerEditorLayer(var name: String, var bitmap: Bitmap) {

    var visible by mutableStateOf(true)
    var isLocked by mutableStateOf(false)
    var scale by mutableFloatStateOf(1f)
    var rotation by mutableFloatStateOf(0f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var alpha by mutableFloatStateOf(1f)
    var visibleBounds by mutableStateOf(calculateVisibleBounds(bitmap))

    // Advanced Text & Font Customization Properties
    var textFontFamily by mutableStateOf("Sans Serif")
    var textFontStyle by mutableStateOf("Bold")
    var textAlignName by mutableStateOf("Center")
    var textFontSizeSp by mutableFloatStateOf(48f)
    var textColorIndex by mutableIntStateOf(0)
    var textOutlineColorIndex by mutableIntStateOf(2)
    var textOutlineThicknessPx by mutableFloatStateOf(10f)
    var textLetterSpacingEm by mutableFloatStateOf(0f)
    var hasTextShadow by mutableStateOf(true)

    // Speech Bubble Editable Properties
    var isSpeechBubble by mutableStateOf(false)
    var bubbleStyle by mutableStateOf("Classic Comic Bubble")
    var bubbleText by mutableStateOf("")
    var bubbleTextColorIndex by mutableIntStateOf(1)
    var bubbleFillColorIndex by mutableIntStateOf(2)
    var bubbleStrokeColorIndex by mutableIntStateOf(1)
    var bubbleTextSizeSp by mutableFloatStateOf(32f)
    var bubbleOutlineThicknessSp by mutableFloatStateOf(10f)
    var bubbleOpacityAlpha by mutableFloatStateOf(1.0f)
    var bubbleTailDirection by mutableStateOf("Bottom Left")
    var bubbleFontFamily by mutableStateOf("Sans Serif")
    var bubbleFontStyle by mutableStateOf("Bold")
    var bubbleTextAlignName by mutableStateOf("Center")

    // Shape Customization Properties
    var isShape by mutableStateOf(false)
    var shapeType by mutableStateOf("Rectangle")
    var shapeStrokeColor by mutableIntStateOf(android.graphics.Color.BLACK)
    var shapeStrokeWidthPx by mutableFloatStateOf(16f)
    var isShapeFilled by mutableStateOf(true)
    var shapeFillColor by mutableIntStateOf(android.graphics.Color.RED)

    fun updateVisibleBounds() {
        visibleBounds = calculateVisibleBounds(bitmap)
    }
}

data class StickerEditorHistoryLayer(
    val name: String,
    val bitmap: Bitmap,
    val visible: Boolean,
    val isLocked: Boolean = false,
    val scale: Float,
    val rotation: Float,
    val offsetX: Float,
    val offsetY: Float,
    val alpha: Float
)

data class OnlineBgItem(
    val title: String,
    val source: String,
    val url: String
)

data class StickerSizeOption(
    val label: String,
    val sizePx: Int?,
    val description: String
)
