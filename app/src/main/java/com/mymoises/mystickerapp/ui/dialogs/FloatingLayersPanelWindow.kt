package com.mymoises.mystickerapp.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.model.StickerEditorLayer
import com.mymoises.mystickerapp.ui.components.Checkerboard

@Composable
fun FloatingLayersPanelWindow(
    layers: List<StickerEditorLayer>,
    activeLayer: Int,
    selectedForMerge: SnapshotStateList<Int>,
    onSelectLayer: (Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onOpenSettings: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRename: (Int) -> Unit,
    onMergeSelected: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        border = BorderStroke(1.dp, Color(0xFF42424D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2B32), RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⋮⋮ 🥞 Layers (${layers.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("➖", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.clickable { onMinimize() })
                    Text("✕", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.clickable { onClose() })
                }
            }

            Spacer(Modifier.height(4.dp))

            if (selectedForMerge.size >= 2) {
                Button(
                    onClick = onMergeSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                ) {
                    Text("🧩 Merge (${selectedForMerge.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(4.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (idx in layers.lastIndex downTo 0) {
                    val layer = layers[idx]
                    val isSel = idx == activeLayer
                    val isCheckedForMerge = selectedForMerge.contains(idx)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLayer(idx) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) Color(0xFF383840) else Color(0xFF26262B)
                        ),
                        border = BorderStroke(if (isSel) 1.5.dp else 0.dp, if (isSel) Color(0xFFFF1744) else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isCheckedForMerge,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedForMerge.contains(idx)) selectedForMerge.add(idx)
                                        } else {
                                            selectedForMerge.remove(idx)
                                        }
                                    },
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.width(2.dp))

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF151518)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Checkerboard(Modifier.matchParentSize())
                                    if (!layer.bitmap.isRecycled) {
                                        Image(
                                            bitmap = layer.bitmap.asImageBitmap(),
                                            contentDescription = layer.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    layer.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (layer.visible) "👁️" else "🙈",
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable { onToggleVisibility(idx) }
                                )

                                Text(
                                    if (layer.isLocked) "🔒" else "🔓",
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable { onToggleLock(idx) }
                                )

                                Text(
                                    "⚙️",
                                    fontSize = 10.sp,
                                    modifier = Modifier.clickable { onOpenSettings(idx) }
                                )

                                if (idx < layers.lastIndex) {
                                    Text("▲", color = Color.LightGray, fontSize = 9.sp, modifier = Modifier.clickable { onMoveUp(idx) })
                                }

                                if (idx > 0) {
                                    Text("▼", color = Color.LightGray, fontSize = 9.sp, modifier = Modifier.clickable { onMoveDown(idx) })
                                }

                                Text("📋", fontSize = 9.sp, modifier = Modifier.clickable { onDuplicate(idx) })

                                Text("🗑", color = Color.Red, fontSize = 9.sp, modifier = Modifier.clickable { onDelete(idx) })
                            }
                        }
                    }
                }
            }
        }
    }
}
