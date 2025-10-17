package com.example.editphoto.utils

import android.graphics.PointF



import android.graphics.Bitmap
import android.graphics.Color
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import android.util.Log
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.max
import kotlin.math.min

// Extension để tạo mặt nạ từ danh sách điểm contour
fun Bitmap.createMaskFromContours(points: List<PointF>, blurSize: Double = 7.0): Mat? {
    val fullMat = Mat()
    Utils.bitmapToMat(this, fullMat)

    // Nội suy để thêm điểm trung gian
    val interpolatedPoints = mutableListOf<Point>()
    for (i in 0 until points.size) {
        val current = points[i]
        interpolatedPoints.add(Point(current.x.toDouble(), current.y.toDouble()))
        if (i < points.size - 1) {
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2
            val midY = (current.y + next.y) / 2
            interpolatedPoints.add(Point(midX.toDouble(), midY.toDouble()))
        }
    }

    val mask = Mat.zeros(fullMat.size(), CvType.CV_8UC1)
    val pointMat = MatOfPoint(*interpolatedPoints.toTypedArray())

    // Vẽ đa giác bao quanh vùng
    val pointList = listOf(pointMat)
    Imgproc.fillPoly(mask, pointList, Scalar(255.0))

    // Làm mịn mặt nạ
    val blurredMask = Mat()
    Imgproc.GaussianBlur(mask, blurredMask, Size(blurSize, blurSize), 0.0)

    fullMat.release()
    pointMat.release()
    mask.release()
    return blurredMask
}

// Extension để tạo mặt nạ từ vùng hình chữ nhật
fun Bitmap.createMaskFromRect(left: Float, top: Float, right: Float, bottom: Float, blurSize: Double = 7.0): Mat? {
    val fullMat = Mat()
    Utils.bitmapToMat(this, fullMat)

    val mask = Mat.zeros(fullMat.size(), CvType.CV_8UC1)
    val region = Rect(
        maxOf(0, left.toInt()),
        maxOf(0, top.toInt()),
        minOf(fullMat.cols() - 1, (right - left).toInt()),
        minOf(fullMat.rows() - 1, (bottom - top).toInt())
    )

    if (region.width > 0 && region.height > 0) {
        Imgproc.rectangle(mask, region, Scalar(255.0), -1)
        val blurredMask = Mat()
        Imgproc.GaussianBlur(mask, blurredMask, Size(blurSize, blurSize), 0.0)
        fullMat.release()
        mask.release()
        return blurredMask
    } else {
        Log.e("ImageProcessingExtensions", "Invalid region: $region")
        fullMat.release()
        return null
    }
}

// Extension để áp hiệu ứng lên vùng với mặt nạ
fun Bitmap.applyEffectWithMask(mask: Mat?, color: Int, alpha: Float, blurSize: Double = 7.0): Bitmap? {
    if (mask == null) {
        Log.e("ImageProcessingExtensions", "Mask is null")
        return null
    }

    val srcMat = Mat()
    Utils.bitmapToMat(this, srcMat)

    // Tạo Mat màu đồng nhất
    val colorMat = Mat(srcMat.size(), srcMat.type(), Scalar(
        Color.blue(color).toDouble(),
        Color.green(color).toDouble(),
        Color.red(color).toDouble(),
        255.0
    ))

    // Cho phép alpha từ 0.0 (trong suốt) đến 1.0 (màu đậm nhất)
    val alphaDouble = alpha.toDouble().coerceIn(0.0, 1.0)
    val betaDouble = 1.0 - alphaDouble

    // Tạo Mat cho vùng đã blend
    val regionMat = Mat()
    Core.addWeighted(srcMat, betaDouble, colorMat, alphaDouble, 0.0, regionMat, -1)

    // Tăng độ sáng nhẹ
    val brightMat = Mat()
    Core.convertScaleAbs(regionMat, brightMat, 1.1, 10.0)

    // Làm mịn vùng đã tô
    val blurredRegionMat = Mat()
    Imgproc.GaussianBlur(brightMat, blurredRegionMat, Size(blurSize, blurSize), 0.0)

    // Áp mặt nạ để chỉ giữ màu trong vùng
    val maskedRegionMat = Mat()
    Core.bitwise_and(blurredRegionMat, blurredRegionMat, maskedRegionMat, mask)

    // Giữ nguyên vùng ngoài từ ảnh gốc
    val resultMat = srcMat.clone()
    val inverseMask = Mat()
    Core.bitwise_not(mask, inverseMask)
    Core.bitwise_and(srcMat, srcMat, resultMat, inverseMask)

    // Kết hợp vùng đã tô màu với ảnh gốc
    Core.add(resultMat, maskedRegionMat, resultMat)

    // Chuyển về Bitmap
    val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(resultMat, resultBitmap)

    // Giải phóng bộ nhớ
    srcMat.release()
    colorMat.release()
    regionMat.release()
    brightMat.release()
    blurredRegionMat.release()
    maskedRegionMat.release()
    resultMat.release()
    inverseMask.release()

    return resultBitmap
}