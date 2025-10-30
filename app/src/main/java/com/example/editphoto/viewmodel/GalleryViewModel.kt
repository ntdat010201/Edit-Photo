package com.example.editphoto.viewmodel

import android.app.Application
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.editphoto.enums.SortType
import com.example.editphoto.model.PhotoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val _photos = MutableLiveData<List<PhotoModel>>()
    val photos: LiveData<List<PhotoModel>> = _photos

    private var originalList = mutableListOf<PhotoModel>()

    // SharedPreferences
    private val prefs = application.getSharedPreferences("gallery_sort", Context.MODE_PRIVATE)
    private val KEY_SORT_TYPE = "sort_type"
    private val KEY_ASCENDING = "ascending"

    private var _currentSortType: SortType
    private var _isAscending: Boolean

    val currentSortType: SortType get() = _currentSortType
    val isAscending: Boolean get() = _isAscending

    init {
        _currentSortType = try {
            SortType.valueOf(prefs.getString(KEY_SORT_TYPE, SortType.DATE.name)!!)
        } catch (e: Exception) {
            SortType.DATE
        }
        _isAscending = prefs.getBoolean(KEY_ASCENDING, false)
    }

    fun loadAllImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = mutableListOf<PhotoModel>()
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            getApplication<Application>().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val uri = android.content.ContentUris.withAppendedId(uri, id)
                    list.add(
                        PhotoModel(
                            id = id.toString(),
                            uri = uri.toString(),
                            name = cursor.getString(nameIdx) ?: "Unknown",
                            path = cursor.getString(pathIdx) ?: "",
                            dateAdded = cursor.getLong(dateIdx),
                            size = cursor.getLong(sizeIdx),
                            width = cursor.getInt(widthIdx),
                            height = cursor.getInt(heightIdx)
                        )
                    )
                }
            }

            originalList = list
            sortPhotos(_currentSortType, _isAscending)
        }
    }

    fun sortPhotos(type: SortType, ascending: Boolean) {
        if (originalList.isEmpty()) return

        _currentSortType = type
        _isAscending = ascending

        // Lưu SharedPreferences
        prefs.edit()
            .putString(KEY_SORT_TYPE, type.name)
            .putBoolean(KEY_ASCENDING, ascending)
            .apply()

        val sorted = when (type) {
            SortType.DATE -> originalList.sortedWith(compareBy { it.dateAdded })
            SortType.NAME -> originalList.sortedWith(compareBy { it.name.lowercase() })
            SortType.SIZE -> originalList.sortedWith(compareBy { it.size })
        }

        _photos.postValue(if (ascending) sorted else sorted.reversed())
    }
}