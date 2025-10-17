package com.example.editphoto.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import kotlin.math.min

class EditImageViewModel : ViewModel() {

    private val _editedImage = MutableLiveData<Bitmap>()
    val editedImage: LiveData<Bitmap> get() = _editedImage

    private var originalBitmap: Bitmap? = null

    // Nhận ảnh từ Activity và giảm độ phân giải nếu cần
    fun setOriginalImage(bitmap: Bitmap) {
        val maxSize = 1024
        val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
        if (scale < 1f) {
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            originalBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
            _editedImage.value = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            Log.d("EditImageViewModel", "Scaled bitmap to ${scaledBitmap.width}x${scaledBitmap.height}")
        } else {
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            _editedImage.value = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            Log.d("EditImageViewModel", "Set original bitmap: ${bitmap.width}x${bitmap.height}")
        }
    }

    // Cập nhật ảnh đã chỉnh sửa
    fun updateEditedImage(bitmap: Bitmap?) {
        bitmap?.let {
            _editedImage.value = it.copy(Bitmap.Config.ARGB_8888, true)
            Log.d("EditImageViewModel", "Updated edited image: ${it.width}x${it.height}")
        } ?: Log.e("EditImageViewModel", "Updated bitmap is null")
    }

    // Lấy ảnh gốc
    fun getOriginalBitmap(): Bitmap? {
        return originalBitmap
    }

    override fun onCleared() {
        super.onCleared()
        originalBitmap?.recycle()
        Log.d("EditImageViewModel", "Cleared resources")
    }
}