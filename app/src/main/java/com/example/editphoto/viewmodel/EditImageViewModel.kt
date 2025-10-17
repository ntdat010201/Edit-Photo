package com.example.editphoto.viewmodel

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class EditImageViewModel : ViewModel() {

    private val _editedImage = MutableLiveData<Bitmap>()
    val editedImage: LiveData<Bitmap> get() = _editedImage

    private var originalBitmap: Bitmap? = null
    private var lipMask: Mat? = null
    private var hasLipMask = false

    // Nhận ảnh từ Activity
    fun setOriginalImage(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        _editedImage.value = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }

    //  Detect môi bằng ML Kit (GỌI TỪ FRAGMENT)
    fun detectLips(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]

                    val bottomLip = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                    val leftLip = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                    val rightLip = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                    if (bottomLip != null && leftLip != null && rightLip != null) {
                        val padding = 20
                        val left = leftLip.x - padding
                        val right = rightLip.x + padding
                        val top = bottomLip.y - 80
                        val bottom = bottomLip.y + 20

                        setLipMaskRegion(left, top, right, bottom)
                    }
                }
            }
    }

    private fun setLipMaskRegion(left: Float, top: Float, right: Float, bottom: Float) {
        val srcBitmap = originalBitmap ?: return
        val fullMat = Mat()
        Utils.bitmapToMat(srcBitmap, fullMat)

        val mask = Mat.zeros(fullMat.size(), CvType.CV_8UC1)
        val region = Rect(
            left.toInt(),
            top.toInt(),
            (right - left).toInt(),
            (bottom - top).toInt()
        )

        Imgproc.rectangle(mask, region, Scalar(255.0), -1)

        lipMask = mask
        hasLipMask = true
        fullMat.release()
    }

    // Tô môi bằng OpenCV
    fun applyLipColor(color: Int, alpha: Float) {
        val srcBitmap = originalBitmap ?: return
        if (!hasLipMask || lipMask == null) return

        val srcMat = Mat()
        Utils.bitmapToMat(srcBitmap, srcMat)

        val b = Color.blue(color).toDouble()
        val g = Color.green(color).toDouble()
        val r = Color.red(color).toDouble()

        val maskGray = lipMask
        val alphaValue = alpha.coerceIn(0f, 1f)

        for (row in 0 until srcMat.rows()) {
            for (col in 0 until srcMat.cols()) {
                val maskVal = maskGray?.get(row, col)?.get(0) ?: 0.0
                if (maskVal > 0) {
                    val srcPixel = srcMat.get(row, col)
                    if (srcPixel != null && srcPixel.size == 3) {
                        val newPixel = doubleArrayOf(
                            srcPixel[0] * (1 - alphaValue) + b * alphaValue,
                            srcPixel[1] * (1 - alphaValue) + g * alphaValue,
                            srcPixel[2] * (1 - alphaValue) + r * alphaValue
                        )
                        srcMat.put(row, col, newPixel)
                    }
                }
            }
        }

        val outputMat = Mat()
        srcMat.copyTo(outputMat)

        val resultBitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(outputMat, resultBitmap)
        _editedImage.value = resultBitmap

        srcMat.release()
        outputMat.release()
    }
}
