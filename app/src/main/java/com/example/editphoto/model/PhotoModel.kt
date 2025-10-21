package com.example.editphoto.model

import android.net.Uri

data class PhotoModel(
    val id: String,
    val uri: Uri,
    val name: String,
    val path: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int
)
