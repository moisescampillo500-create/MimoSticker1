package com.mymoises.mystickerapp.model

data class CreatorModel(
    val id: String,
    val name: String,
    val handle: String,
    val avatar: String,
    val bio: String,
    val stickersCount: Int,
    val sampleStickers: List<String>,
    val verified: Boolean = true
)
