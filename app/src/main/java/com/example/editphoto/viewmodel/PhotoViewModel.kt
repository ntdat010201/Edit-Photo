package com.example.editphoto.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.editphoto.base.AppDatabase
import com.example.editphoto.model.PhotoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).photoDao()

    val allPhotos: LiveData<List<PhotoModel>> = dao.getAllPhotosLive()

    fun insertPhoto(photo: PhotoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPhoto(photo)
        }
    }

    fun deletePhoto(photo: PhotoModel) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePhoto(photo)
        }
    }

}

