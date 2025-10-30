package com.example.editphoto.utils.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.editphoto.model.PhotoModel

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoModel)

    @Query("SELECT * FROM photo_table ORDER BY dateAdded DESC")
    suspend fun getAllPhotos(): List<PhotoModel>

    @Query("SELECT * FROM photo_table ORDER BY dateAdded DESC")
    fun getAllPhotosLive(): LiveData<List<PhotoModel>>

    @Delete
    suspend fun deletePhoto(photo: PhotoModel)
}
