package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable

@Serializable
data class MuseumObject(
    val objectID: Int,
    val title: String,
    val artistDisplayName: String,
    val type: String,
    val objectURL: String,
    val objectDate: String,
    val coverImage: String,
    val tracks: String,
    val length: String,
)