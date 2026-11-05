package com.mymoises.mystickerapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class StickerPack(
    val id: String,
    val name: String,
    val stickerNames: List<String> = emptyList()
)

fun getStickerPacks(context: Context): List<StickerPack> {
    val prefs = context.getSharedPreferences("sticker_packs_pref", Context.MODE_PRIVATE)
    val packSet = prefs.getStringSet("packs_list", setOf("Pack 1", "Pack 2", "Pack 3")) ?: setOf("Pack 1", "Pack 2", "Pack 3")
    return packSet.sorted().map { name ->
        val stickers = prefs.getStringSet("pack_stickers_$name", emptySet())?.toList() ?: emptyList()
        StickerPack(id = name, name = name, stickerNames = stickers)
    }
}

fun createStickerPack(context: Context, packName: String): StickerPack {
    val prefs = context.getSharedPreferences("sticker_packs_pref", Context.MODE_PRIVATE)
    val currentSet = prefs.getStringSet("packs_list", setOf("Pack 1", "Pack 2", "Pack 3"))?.toMutableSet() ?: mutableSetOf("Pack 1", "Pack 2", "Pack 3")
    currentSet.add(packName)
    prefs.edit().putStringSet("packs_list", currentSet).apply()
    val stickers = prefs.getStringSet("pack_stickers_$packName", emptySet())?.toList() ?: emptyList()
    return StickerPack(id = packName, name = packName, stickerNames = stickers)
}

@Composable
fun StickerPacksScreen(
    context: Context,
    onOpenPack: (String) -> Unit,
    onBack: () -> Unit
) {
    val packs = remember { getStickerPacks(context) }
    var newPackName by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, color = Color.White)
            }
            Text("📦 My Sticker Packs", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("＋ Create New Sticker Pack")
        }

        Spacer(Modifier.height(16.dp))

        packs.forEach { pack ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onOpenPack(pack.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2648))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", fontSize = 32.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pack.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${pack.stickerNames.size} stickers", fontSize = 12.sp, color = Color.LightGray)
                    }
                    Text("›", fontSize = 28.sp, color = Color.White)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Create New Pack") },
            text = {
                OutlinedTextField(
                    value = newPackName,
                    onValueChange = { newPackName = it },
                    label = { Text("Pack Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPackName.isNotBlank()) {
                            val pack = createStickerPack(context, newPackName.trim())
                            onOpenPack(pack.id)
                        }
                        showDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StickerPackDetailScreen(
    context: Context,
    packId: String,
    stickerFiles: List<File>,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("sticker_packs_pref", Context.MODE_PRIVATE) }
    var packStickers by remember {
        mutableStateOf(prefs.getStringSet("pack_stickers_$packId", emptySet()) ?: emptySet())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 24.sp, color = Color.White)
            }
            Text("📁 $packId", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Text("Organize stickers in this pack ($packId):", fontSize = 14.sp, color = Color.LightGray)
        Spacer(Modifier.height(16.dp))

        if (stickerFiles.isEmpty()) {
            Text("No stickers created yet. Create a sticker first!", color = Color.Gray)
        } else {
            stickerFiles.forEach { file ->
                val isInPack = packStickers.contains(file.name)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInPack) Color(0xFF1B5E20) else Color(0xFF1E2648)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🖼️", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(file.name, modifier = Modifier.weight(1f), color = Color.White, fontSize = 14.sp)
                        Button(
                            onClick = {
                                val updated = if (isInPack) packStickers - file.name else packStickers + file.name
                                packStickers = updated
                                prefs.edit().putStringSet("pack_stickers_$packId", updated).apply()
                                onSave()
                            }
                        ) {
                            Text(if (isInPack) "✓ In Pack" else "＋ Add")
                        }
                    }
                }
            }
        }
    }
}
