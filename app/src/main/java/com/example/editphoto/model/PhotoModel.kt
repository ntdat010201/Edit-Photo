package com.example.editphoto.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_table")
data class PhotoModel(
    @PrimaryKey val id: String,
    val uri: String,
    val name: String,
    val path: String,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int
)
