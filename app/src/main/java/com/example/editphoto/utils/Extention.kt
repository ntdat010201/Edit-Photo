package com.example.editphoto.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

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
