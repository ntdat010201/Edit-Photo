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
import org.opencv.android.Utils
import org.opencv.core.Mat

/**
 * ViewModel trung tâm cho các chức năng chỉnh sửa ảnh:
 * - Lưu trữ ảnh gốc & ảnh đang chỉnh
 * - Cung cấp FaceLandmarker để các fragment dùng chung
 * - Chuyển đổi Bitmap <-> Mat để xử lý bằng OpenCV
 */
class EditImageViewModel(application: Application) : AndroidViewModel(application) {

    /** Ảnh gốc (ban đầu load vào) */
    val originalBitmap = MutableLiveData<Bitmap>()

    /** Ảnh đang chỉnh sửa realtime */
    val editedBitmap = MutableLiveData<Bitmap>()

    /** FaceLandmarker (MediaPipe) - khởi tạo 1 lần dùng chung */
    private var faceLandmarker: FaceLandmarker? = null

    /** Cờ xử lý để UI biết đang loading */
    val isProcessing = MutableLiveData(false)

    // ---------------------------
    // ====== HÀM CHÍNH ======
    // ---------------------------

    /**
     * Cập nhật ảnh hiện tại (realtime hiển thị)
     */
    fun updateBitmap(bitmap: Bitmap) {
        editedBitmap.postValue(bitmap)
    }

    /**
     * Set ảnh gốc khi user vừa chọn ảnh
     */
    fun setOriginalBitmap(bitmap: Bitmap) {
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val safeBitmap = bitmap.copy(config, true)
        originalBitmap.value = safeBitmap
        editedBitmap.value = safeBitmap.copy(config, true)
    }


    /**
     * Reset ảnh về gốc
     */
    fun resetToOriginal() {
        originalBitmap.value?.let { bmp ->
            val config = bmp.config ?: Bitmap.Config.ARGB_8888
            updateBitmap(bmp.copy(config, true))
        }
    }

    /**
     * Set hoặc update FaceLandmarker
     */
    fun setFaceLandmarker(landmarker: FaceLandmarker) {
        faceLandmarker = landmarker
    }

    /**
     * Lấy FaceLandmarker hiện có
     */
    fun getFaceLandmarker(): FaceLandmarker? = faceLandmarker

    /**
     * Dò landmarks trên ảnh (nếu cần)
     */
    suspend fun detectFaceLandmarks(bitmap: Bitmap): FaceLandmarkerResult? {
        return withContext(Dispatchers.Default) {
            faceLandmarker?.let {
                val mpImage = BitmapImageBuilder(bitmap).build()
                it.detect(mpImage)
            }
        }
    }

    /**
     * Đặt trạng thái đang xử lý (để hiển thị progress)
     */
    fun setProcessing(isLoading: Boolean) {
        isProcessing.postValue(isLoading)
    }


    override fun onCleared() {
        super.onCleared()
        faceLandmarker?.close()
        faceLandmarker = null
    }
}
