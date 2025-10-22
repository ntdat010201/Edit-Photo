package com.example.editphoto.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.editphoto.ui.activities.EditImageActivity

/**
 * ==== HÀM DÙNG CHUNG CHO TẤT CẢ HIỆU ỨNG ====
 */

/** Chuyển Bitmap → Mat */
fun Bitmap.toMat(): Mat {
    val mat = Mat()
    Utils.bitmapToMat(this, mat)
    return mat
}

/** Chuyển Mat → Bitmap */
fun Mat.toBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bmp)
    return bmp
}

/** Làm mượt viền vùng chọn */
fun Mat.smoothMask(blurSize: Double = 7.0, sigma: Double = 4.0): Mat {
    val blurred = Mat()
    Imgproc.GaussianBlur(this, blurred, Size(blurSize, blurSize), sigma)
    return blurred
}

/** Blend 2 ảnh theo tỷ lệ (dùng chung cho môi, má, da, mắt...) */
fun Mat.blendWith(
    overlay: Mat,
    mask: Mat,
    intensity: Float
): Mat {
    val blended = Mat()
    Core.addWeighted(this, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)
    blended.copyTo(this, mask)
    return this
}


/**
 * Bắt nút back vật lý và gọi hàm xử lý truyền vào.
 * Dùng cho các fragment chỉnh ảnh (Lips, Eyes, Cut, Flip, v.v.)
 */
fun Fragment.handlePhysicalBackPress(action: (act: EditImageActivity) -> Unit) {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        val act = requireActivity() as EditImageActivity
        action(act)
    }
}

/**
 * Hàm xử lý logic Back chung cho Apply / Reset.
 * Dùng cho cả Back UI và Back vật lý.
 */
fun Fragment.handleBackPressedCommon(
    act: EditImageActivity,
    hasApplied: Boolean,
    beforeBitmap: android.graphics.Bitmap?,
    onReset: (() -> Unit)? = null
) {
    val vm = act.viewModel
    if (!hasApplied) {
        beforeBitmap?.let {
            vm.setPreview(null)
            vm.updateBitmap(it)
        }
        onReset?.invoke()
    }
    parentFragmentManager.popBackStack()
}
