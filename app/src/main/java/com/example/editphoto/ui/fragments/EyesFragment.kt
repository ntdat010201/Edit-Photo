package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.editphoto.databinding.FragmentEyesBinding
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class EyesFragment : Fragment() {

    private lateinit var binding: FragmentEyesBinding
    private val viewModel: EditImageViewModel by activityViewModels()

    private var baseBitmap: Bitmap? = null
    private var cachedLandmarks: List<NormalizedLandmark>? = null
    private var leftEyeCenter = Point()
    private var rightEyeCenter = Point()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEyesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareData()
    }

    private fun prepareData() {
        val bmp = viewModel.editedBitmap.value ?: return
        baseBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)

        lifecycleScope.launch(Dispatchers.Default) {
            val landmarker = viewModel.getFaceLandmarker() ?: return@launch
            val mpImage = BitmapImageBuilder(bmp).build()
            val result = landmarker.detect(mpImage)
            if (result.faceLandmarks().isEmpty()) return@launch
            val landmarks = result.faceLandmarks()[0]
            cachedLandmarks = landmarks

            val w = bmp.width.toDouble()
            val h = bmp.height.toDouble()

            val leftEyeIndices = listOf(33, 133, 159, 145, 160, 161, 246, 130)
            val rightEyeIndices = listOf(362, 263, 386, 374, 387, 388, 466, 390)

            leftEyeCenter = meanPoint(leftEyeIndices.map {
                Point(
                    landmarks[it].x() * w,
                    landmarks[it].y() * h
                )
            })
            rightEyeCenter = meanPoint(rightEyeIndices.map {
                Point(
                    landmarks[it].x() * w,
                    landmarks[it].y() * h
                )
            })

            withContext(Dispatchers.Main) {
                setupSeekBar()
            }
        }
    }

    private fun setupSeekBar() {
        binding.eyesIntensity.max = 100
        binding.eyesIntensity.progress = 50

        binding.eyesIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val delta = (progress - 50) / 50f // -1..1
                applyRadialZoom(delta)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    /**
     * Zoom mắt giảm dần từ tâm ra viền — như Ulike/Meitu
     */
    private fun applyRadialZoom(delta: Float) {
        val bmp = baseBitmap ?: return
        val srcMat = Mat()
        Utils.bitmapToMat(bmp, srcMat)
        val result = srcMat.clone()

        // zoom mỗi mắt riêng biệt
        radialZoomEye(srcMat, result, leftEyeCenter, 90.0, delta)
        radialZoomEye(srcMat, result, rightEyeCenter, 90.0, delta)

        val outBmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, outBmp)
        viewModel.updateBitmap(outBmp)

        srcMat.release()
        result.release()
    }

    /**
     * Zoom từ tâm ra viền, giảm dần theo khoảng cách
     */
    private fun radialZoomEye(src: Mat, dst: Mat, center: Point, radius: Double, delta: Float) {
        if (abs(delta) < 0.001) return
        val k = delta * 0.5 // cường độ tối đa ±50%
        val R = radius

        val rect = Rect(
            max(0, (center.x - R).toInt()),
            max(0, (center.y - R).toInt()),
            min((R * 2).toInt(), src.cols() - (center.x - R).toInt()),
            min((R * 2).toInt(), src.rows() - (center.y - R).toInt())
        )
        if (rect.width <= 1 || rect.height <= 1) return

        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())

        for (y in 0 until roi.rows()) {
            for (x in 0 until roi.cols()) {
                val gx = x + rect.x
                val gy = y + rect.y
                val dx = gx - center.x
                val dy = gy - center.y
                val r = sqrt(dx * dx + dy * dy)

                if (r < R) {
                    val factor = 1.0 + k * (1 - (r / R).pow(2.0))
                    val srcX = (center.x + dx / factor).toInt().coerceIn(0, src.cols() - 1)
                    val srcY = (center.y + dy / factor).toInt().coerceIn(0, src.rows() - 1)
                    warped.put(y, x, *src.get(srcY, srcX))
                } else {
                    warped.put(y, x, *roi.get(y, x))
                }
            }
        }

        // mask mờ để ghép lại tự nhiên
        val mask = Mat.zeros(warped.size(), CvType.CV_8UC1)
        Imgproc.circle(
            mask,
            Point((mask.width() / 2).toDouble(), (mask.height() / 2).toDouble()),
            (R * 0.9).toInt(),
            Scalar(255.0),
            -1
        )
        Imgproc.GaussianBlur(mask, mask, Size(11.0, 11.0), 6.0)

        val sub = dst.submat(rect)
        warped.copyTo(sub, mask)

        roi.release()
        warped.release()
        mask.release()
        sub.release()
    }

    private fun meanPoint(points: List<Point>): Point {
        var sx = 0.0
        var sy = 0.0
        for (p in points) {
            sx += p.x
            sy += p.y
        }
        return Point(sx / max(1, points.size), sy / max(1, points.size))
    }
}
