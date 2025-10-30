package com.example.editphoto.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class EditImageViewModel(application: Application) : AndroidViewModel(application) {

    val originalBitmap = MutableLiveData<Bitmap>()

    val editedBitmap = MutableLiveData<Bitmap>()

    // Ảnh preview tạm
    val previewBitmap = MutableLiveData<Bitmap?>()

    // FaceLandmarker (MediaPipe)
    private var faceLandmarker: FaceLandmarker? = null

    val isProcessing = MutableLiveData(false)


    fun updateBitmap(bitmap: Bitmap) {
        editedBitmap.value = bitmap
    }



    fun setOriginalBitmap(bitmap: Bitmap) {
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val safeBitmap = bitmap.copy(config, true)
        originalBitmap.value = safeBitmap
        editedBitmap.value = safeBitmap.copy(config, true)
    }

    fun resetToOriginal() {
        originalBitmap.value?.let { bmp ->
            val config = bmp.config ?: Bitmap.Config.ARGB_8888
            updateBitmap(bmp.copy(config, true))
        }
        previewBitmap.postValue(null)
    }


    fun setPreview(bitmap: Bitmap?) {
        previewBitmap.value = bitmap
    }

    fun commitPreview() {
        previewBitmap.value?.let { bmp ->
            val config = bmp.config ?: Bitmap.Config.ARGB_8888
            editedBitmap.value = bmp.copy(config, true)
            previewBitmap.value = null
        }
    }


    fun setFaceLandmarker(landmarker: FaceLandmarker) {
        faceLandmarker = landmarker
    }

    fun getFaceLandmarker(): FaceLandmarker? = faceLandmarker

    suspend fun detectFaceLandmarks(bitmap: Bitmap): FaceLandmarkerResult? {
        return withContext(Dispatchers.Default) {
            faceLandmarker?.let {
                val mpImage = BitmapImageBuilder(bitmap).build()
                it.detect(mpImage)
            }
        }
    }


    fun setProcessing(isLoading: Boolean) {
        isProcessing.postValue(isLoading)
    }

    override fun onCleared() {
        super.onCleared()
        faceLandmarker?.close()
        faceLandmarker = null
    }
}