package com.example.editphoto.utils

import android.graphics.Bitmap
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Hàm tô màu môi chính xác theo landmark.
 * Áp dụng khi người dùng bấm "Apply" để ghi thật vào ảnh.
 *
 * @param landmarks danh sách landmarks từ MediaPipe (đã detect sẵn)
 * @param color màu tô (Scalar BGR)
 * @param intensity độ đậm (0f - 1f)
 */
fun Bitmap.applyLipColorFromLandmarks(
    landmarks: List<NormalizedLandmark>,
    color: Scalar,
    intensity: Float
): Bitmap {
    val inputMat = Mat()
    Utils.bitmapToMat(this, inputMat)

    val width = this.width.toDouble()
    val height = this.height.toDouble()

    // Các điểm môi (MediaPipe FaceMesh indices)
    val lipIndices = listOf(
        61,185,40,39,37,0,267,269,270,409,
        78,95,88,178,87,14,317,402,318,324
    )

    val lipPoints = lipIndices.map { idx ->
        val lm = landmarks[idx]
        Point(lm.x() * width, lm.y() * height)
    }.toTypedArray()

    // Tạo mask môi
    val lipMask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
    Imgproc.fillPoly(lipMask, listOf(MatOfPoint(*lipPoints)), Scalar(255.0))
    Imgproc.GaussianBlur(lipMask, lipMask, Size(3.0, 3.0), 2.0)

    // Lấy vùng môi
    val lipRegion = Mat()
    inputMat.copyTo(lipRegion, lipMask)

    // Tạo lớp overlay màu
    val overlay = Mat(lipRegion.size(), lipRegion.type(), color)

    // Blend môi theo độ đậm
    val blended = Mat()
    Core.addWeighted(lipRegion, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)

    // Gộp vào ảnh gốc (chỉ vùng môi)
    blended.copyTo(inputMat, lipMask)

    // Chuyển về Bitmap
    val outputBitmap = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(inputMat, outputBitmap)
    return outputBitmap
}

/**
 * Tạo mask môi + vùng môi + ảnh gốc (baseMat)
 * Dùng cho preview realtime — chỉ tính toán 1 lần.
 */
fun Bitmap.createLipMaskAndRegion(
    landmarks: List<NormalizedLandmark>
): Triple<Mat, Mat, Mat> {
    val inputMat = Mat()
    Utils.bitmapToMat(this, inputMat)

    val width = this.width.toDouble()
    val height = this.height.toDouble()

    val lipIndices = listOf(
        61,185,40,39,37,0,267,269,270,409,
        78,95,88,178,87,14,317,402,318,324
    )

    val lipPoints = lipIndices.map { idx ->
        val lm = landmarks[idx]
        Point(lm.x() * width, lm.y() * height)
    }.toTypedArray()

    val lipMask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
    Imgproc.fillPoly(lipMask, listOf(MatOfPoint(*lipPoints)), Scalar(255.0))
    Imgproc.GaussianBlur(lipMask, lipMask, Size(3.0, 3.0), 2.0)

    val lipRegion = Mat()
    inputMat.copyTo(lipRegion, lipMask)

    return Triple(lipMask, lipRegion, inputMat)
}

/**
 * Hàm blend nhanh cho preview realtime (khi kéo SeekBar)
 * Chỉ thao tác trên vùng môi nhỏ nên cực nhẹ.
 */
fun Mat.quickApplyLipColor(color: Scalar, intensity: Float): Mat {
    val overlay = Mat(this.size(), this.type(), color)
    val blended = Mat()
    Core.addWeighted(this, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)
    return blended
}

/**
 * Chuyển Mat -> Bitmap an toàn
 */
fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bitmap)
    return bitmap
}

/**
 * Chuyển Bitmap -> Mat nhanh
 */
fun Bitmap.toMat(): Mat {
    val mat = Mat()
    Utils.bitmapToMat(this, mat)
    return mat
}


/*package com.example.editphoto.utils

import android.graphics.Bitmap
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

*//**
 * Áp màu môi chính xác (môi trên + môi dưới)
 * Không bị ám xanh, không tô răng, blend mượt tự nhiên.
 *//*
fun Bitmap.applyLipColorFromLandmarks(
    landmarks: List<NormalizedLandmark>,
    color: Scalar,
    intensity: Float
): Bitmap {
    val inputMatRaw = Mat()
    Utils.bitmapToMat(this, inputMatRaw)

    // Đảm bảo dùng không gian màu BGR 3 kênh
    val inputMat = Mat()
    if (inputMatRaw.channels() == 4)
        Imgproc.cvtColor(inputMatRaw, inputMat, Imgproc.COLOR_RGBA2BGR)
    else
        inputMatRaw.copyTo(inputMat)

    val width = this.width.toDouble()
    val height = this.height.toDouble()

    // ✅ Danh sách môi chuẩn (MediaPipe FaceMesh 468 points)
    val outerLips = listOf(
        61,146,91,181,84,17,314,405,
        321,375,291,308,324,318,402,
        317,14,87,178,88,95,78
    )
    val innerLips = listOf(
        78,191,80,81,82,13,312,311,
        310,415,308
    )

    // --- Tạo polygon môi ---
    val outerPoints = outerLips.map { i ->
        val lm = landmarks[i]
        Point(lm.x() * width, lm.y() * height)
    }.toTypedArray()

    val innerPoints = innerLips.map { i ->
        val lm = landmarks[i]
        Point(lm.x() * width, lm.y() * height)
    }.toTypedArray()

    // --- Mask môi ---
    val outerMask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
    val innerMask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
    Imgproc.fillPoly(outerMask, listOf(MatOfPoint(*outerPoints)), Scalar(255.0))
    Imgproc.fillPoly(innerMask, listOf(MatOfPoint(*innerPoints)), Scalar(255.0))

    // Môi = outer - inner (loại bỏ răng)
    val lipMask = Mat()
    Core.subtract(outerMask, innerMask, lipMask)
    Imgproc.GaussianBlur(lipMask, lipMask, Size(5.0, 5.0), 3.0)

    // --- Blend màu ---
    val lipRegion = Mat()
    inputMat.copyTo(lipRegion, lipMask)

    val overlay = Mat(lipRegion.size(), CvType.CV_8UC3, color)
    val blended = Mat()
    Core.addWeighted(lipRegion, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)

    // Gộp lại ảnh
    blended.copyTo(inputMat, lipMask)

    // Trả về bitmap
    val output = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(inputMat, output)
    return output
}

*//**
 * Tạo mask môi + vùng môi + ảnh gốc (baseMat)
 * Dùng cho preview realtime (SeekBar)
 *//*
fun Bitmap.createLipMaskAndRegion(
    landmarks: List<NormalizedLandmark>
): Triple<Mat, Mat, Mat> {
    val inputMatRaw = Mat()
    Utils.bitmapToMat(this, inputMatRaw)

    val inputMat = Mat()
    if (inputMatRaw.channels() == 4)
        Imgproc.cvtColor(inputMatRaw, inputMat, Imgproc.COLOR_RGBA2BGR)
    else
        inputMatRaw.copyTo(inputMat)

    val width = this.width.toDouble()
    val height = this.height.toDouble()

    val outerLips = listOf(
        61,146,91,181,84,17,314,405,
        321,375,291,308,324,318,402,
        317,14,87,178,88,95,78
    )
    val innerLips = listOf(
        78,191,80,81,82,13,312,311,
        310,415,308
    )

    val outerPoints = outerLips.map { i ->
        val lm = landmarks[i]
        Point(lm.x() * width, lm.y() * height)
    }.toTypedArray()

    val innerPoints = innerLips.map { i ->
        val lm = landmarks[i]
        Point(lm.x() * width, lm.y() * height)
    }.toTypedArray()

    val outerMask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
    val innerMask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
    Imgproc.fillPoly(outerMask, listOf(MatOfPoint(*outerPoints)), Scalar(255.0))
    Imgproc.fillPoly(innerMask, listOf(MatOfPoint(*innerPoints)), Scalar(255.0))

    val lipMask = Mat()
    Core.subtract(outerMask, innerMask, lipMask)
    Imgproc.GaussianBlur(lipMask, lipMask, Size(5.0, 5.0), 3.0)

    val lipRegion = Mat()
    inputMat.copyTo(lipRegion, lipMask)

    return Triple(lipMask, lipRegion, inputMat)
}

*//**
 * Blend realtime (SeekBar) – chỉ áp dụng trong vùng môi, không ám xanh.
 *//*
fun quickApplyLipColorRealtime(
    baseMat: Mat,          // ảnh gốc
    lipMask: Mat,          // mask môi
    color: Scalar,         // màu tô
    intensity: Float       // độ đậm
): Mat {
    val bgr = Mat()
    when (baseMat.channels()) {
        4 -> Imgproc.cvtColor(baseMat, bgr, Imgproc.COLOR_RGBA2BGR)
        1 -> Imgproc.cvtColor(baseMat, bgr, Imgproc.COLOR_GRAY2BGR)
        else -> baseMat.copyTo(bgr)
    }

    val lipRegion = Mat()
    bgr.copyTo(lipRegion, lipMask)

    val overlay = Mat(lipRegion.size(), CvType.CV_8UC3, color)
    val blended = Mat()
    Core.addWeighted(lipRegion, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)

    blended.copyTo(bgr, lipMask)
    return bgr
}

*//**
 * Mat -> Bitmap
 *//*
fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bitmap)
    return bitmap
}

*//**
 * Bitmap -> Mat
 *//*
fun Bitmap.toMat(): Mat {
    val mat = Mat()
    Utils.bitmapToMat(this, mat)
    return mat
}*/

