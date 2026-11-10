package com.mymoises.mystickerapp.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymoises.mystickerapp.model.StickerSizeOption

val defaultStickerSizeOptions = listOf(
    StickerSizeOption("60 dp", 60, "Mini Sticker (60x60 dp)"),
    StickerSizeOption("128 dp", 128, "Small Sticker (128x128 dp)"),
    StickerSizeOption("152 dp", 152, "Medium Sticker (152x152 dp)"),
    StickerSizeOption("256 dp", 256, "Large Sticker (256x256 dp)"),
    StickerSizeOption("512 dp", 512, "WhatsApp HD Standard (512x512 dp)"),
    StickerSizeOption("Original", null, "Keep Original Size")
)

@Composable
fun StickerSizeChipSelector(
    selectedSizePx: Int?,
    onSizeSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "📐 Select Output Size (Laki ng Sticker):",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "512 dp" to 512,
                "Original" to null
            ).forEach { (label, size) ->
                FilterChip(
                    selected = selectedSizePx == size,
                    onClick = { onSizeSelected(size) },
                    label = {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (selectedSizePx == size) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun StickerSizeSelectionDialog(
    actionTitle: String = "Select Sticker Size (Pumili ng Laki)",
    onDismiss: () -> Unit,
    onSizeSelected: (Int?) -> Unit
) {
    var selectedOption by remember { mutableStateOf<Int?>(512) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(actionTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Pumili ng laki ng image bago i-save o i-share sa device/WhatsApp:", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))

                defaultStickerSizeOptions.forEach { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option.sizePx },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOption == option.sizePx) Color(0xFFFFF0F2) else Color(0xFFF6F6F8)
                        ),
                        border = if (selectedOption == option.sizePx) BorderStroke(2.dp, Color.Red) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == option.sizePx,
                                onClick = { selectedOption = option.sizePx }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    option.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedOption == option.sizePx) Color.Red else Color.Unspecified
                                )
                                Text(option.description, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSizeSelected(selectedOption)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Confirm & Continue", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
