package com.mymoises.mystickerapp.ui.components

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Checkerboard(modifier: Modifier = Modifier) {
    ComposeCanvas(modifier = modifier) {
        val tileSize = 18.dp.toPx()
        val columns = (size.width / tileSize).toInt() + 1
        val rows = (size.height / tileSize).toInt() + 1
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                drawRect(
                    color = if ((row + column) % 2 == 0) {
                        Color(0xFFE8E8E8)
                    } else {
                        Color.White
                    },
                    topLeft = Offset(
                        column * tileSize,
                        row * tileSize
                    ),
                    size = Size(tileSize, tileSize)
                )
            }
        }
    }
}
