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
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.handleBackPressedCommon
import com.example.editphoto.utils.handlePhysicalBackPress
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private lateinit var viewModel: EditImageViewModel

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    private var baseBitmap: Bitmap? = null
    private var leftEyeCenter = Point()
    private var rightEyeCenter = Point()
    private var currentMode = "size"

    // Lưu trạng thái
    private val eyeParams = mutableMapOf(
        "size" to 0f,
        "height" to 0f,
        "width" to 0f,
        "location" to 0f,
        "distance" to 0f,
        "corner" to 0f
    )

    private var applyJob: Job? = null

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
        val act = requireActivity() as EditImageActivity
        viewModel = act.viewModel

        beforeEditBitmap = viewModel.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)

        prepareData()

        binding.eyeSize.setOnClickListener { setupSeekBarForMode("size") }
        binding.eyeHeight.setOnClickListener { setupSeekBarForMode("height") }
        binding.eyeWidth.setOnClickListener { setupSeekBarForMode("width") }
        binding.eyeLocation.setOnClickListener { setupSeekBarForMode("location") }
        binding.eyeDistance.setOnClickListener { setupSeekBarForMode("distance") }
        binding.eyeCorner.setOnClickListener { setupSeekBarForMode("corner") }

        binding.btnApply.setOnClickListener {
            viewModel.commitPreview()
            hasApplied = true
            beforeEditBitmap = viewModel.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
            parentFragmentManager.popBackStack()
        }

        binding.btnBack.setOnClickListener {
            handleBackPressedCommon(
                act,
                hasApplied,
                beforeEditBitmap
            ) {
                eyeParams.replaceAll { _, _ -> 0f }
                baseBitmap = null
                prepareData()
            }
        }

        handlePhysicalBackPress { act2 ->
            handleBackPressedCommon(
                act2,
                hasApplied,
                beforeEditBitmap
            ) {
                eyeParams.replaceAll { _, _ -> 0f }
                baseBitmap = null
                prepareData()
            }
        }
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

            val w = bmp.width.toDouble()
            val h = bmp.height.toDouble()

            val leftEyeIndices = listOf(33, 133, 159, 145, 160, 161, 246, 130)
            val rightEyeIndices = listOf(362, 263, 386, 374, 387, 388, 466, 390)

            leftEyeCenter = meanPoint(leftEyeIndices.map {
                Point(landmarks[it].x() * w, landmarks[it].y() * h)
            })
            rightEyeCenter = meanPoint(rightEyeIndices.map {
                Point(landmarks[it].x() * w, landmarks[it].y() * h)
            })

            withContext(Dispatchers.Main) {
                setupSeekBarForMode("size")
            }
        }
    }

    private fun setupSeekBarForMode(mode: String) {
        currentMode = mode
        binding.eyesIntensity.max = 100
        val currentDelta = eyeParams[mode] ?: 0f
        binding.eyesIntensity.progress = (currentDelta * 50 + 50).toInt()

        binding.eyesIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val delta = (progress - 50) / 50f
                eyeParams[mode] = delta
                scheduleRealtimePreview()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun scheduleRealtimePreview() {
        applyJob?.cancel()
        applyJob = lifecycleScope.launch {
            delay(20)
            applyAllEffects()
        }
    }

    /**
     * Áp dụng tất cả hiệu ứng hiện tại (cho preview)
     */
    private fun applyAllEffects() {
        val bmp = baseBitmap ?: return
        val src = Mat()
        Utils.bitmapToMat(bmp, src)
        val result = src.clone()

        val size = eyeParams["size"] ?: 0f
        val height = eyeParams["height"] ?: 0f
        val width = eyeParams["width"] ?: 0f
        val loc = eyeParams["location"] ?: 0f
        val dist = eyeParams["distance"] ?: 0f
        val corner = eyeParams["corner"] ?: 0f

        if (size != 0f) {
            radialZoomEye(src, result, leftEyeCenter, 90.0, size)
            radialZoomEye(src, result, rightEyeCenter, 90.0, size)
        }
        if (height != 0f) {
            stretchEyeVertical(src, result, leftEyeCenter, 90.0, height)
            stretchEyeVertical(src, result, rightEyeCenter, 90.0, height)
        }
        if (width != 0f) {
            stretchEyeHorizontal(src, result, leftEyeCenter, 90.0, width)
            stretchEyeHorizontal(src, result, rightEyeCenter, 90.0, width)
        }
        if (loc != 0f) {
            moveEye(result, result, leftEyeCenter, 90.0, loc * 20.0, 0.0)
            moveEye(result, result, rightEyeCenter, 90.0, loc * 20.0, 0.0)
        }
        if (dist != 0f) {
            moveEye(result, result, leftEyeCenter, 90.0, 0.0, -dist * 20.0)
            moveEye(result, result, rightEyeCenter, 90.0, 0.0, dist * 20.0)
        }
        if (corner != 0f) {
            rotateEye(result, leftEyeCenter, 90.0, corner, false)
            rotateEye(result, rightEyeCenter, 90.0, corner, true)
        }

        val outBmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, outBmp)
        viewModel.setPreview(outBmp)

        src.release()
        result.release()
    }

    /**
     * Khi nhấn Apply (commit preview vào editedBitmap)
     */
    private fun applyFinalEyeEffects() {
        viewModel.commitPreview()
        parentFragmentManager.popBackStack()
    }

    //EYE SIZE
    private fun radialZoomEye(src: Mat, dst: Mat, c: Point, R: Double, delta: Float) {
        if (abs(delta) < 0.001) return
        val k = delta * 0.2
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x;
            val gy = y + rect.y
            val dx = gx - c.x;
            val dy = gy - c.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val f = 1 + k * (1 - (r / R).pow(2))
                val srcX = (c.x + dx / f).toInt().coerceIn(0, src.cols() - 1)
                val srcY = (c.y + dy / f).toInt().coerceIn(0, src.rows() - 1)
                warped.put(y, x, *src.get(srcY, srcX))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    //HEIGHT / WIDTH
    private fun stretchEyeVertical(src: Mat, dst: Mat, c: Point, R: Double, d: Float) {
        if (abs(d) < 0.001) return
        val s = d * 0.3
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x;
            val gy = y + rect.y
            val dx = gx - c.x;
            val dy = gy - c.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val fY = 1 + s * (1 - (r / R).pow(2))
                val srcY = (c.y + dy / fY).toInt().coerceIn(0, src.rows() - 1)
                warped.put(y, x, *src.get(srcY, gx))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    private fun stretchEyeHorizontal(src: Mat, dst: Mat, c: Point, R: Double, d: Float) {
        if (abs(d) < 0.001) return
        val s = d * 0.3
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x;
            val gy = y + rect.y
            val dx = gx - c.x;
            val dy = gy - c.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val fX = 1 + s * (1 - (r / R).pow(2))
                val srcX = (c.x + dx / fX).toInt().coerceIn(0, src.cols() - 1)
                warped.put(y, x, *src.get(gy, srcX))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    // LOCATION / DISTANCE
    private fun moveEye(src: Mat, dst: Mat, c: Point, R: Double, shiftY: Double, shiftX: Double) {
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x;
            val gy = y + rect.y
            val dx = gx - c.x;
            val dy = gy - c.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val factor = 1 - (r / R).pow(2.0)
                val srcX = (gx - shiftX * factor).toInt().coerceIn(0, src.cols() - 1)
                val srcY = (gy - shiftY * factor).toInt().coerceIn(0, src.rows() - 1)
                warped.put(y, x, *src.get(srcY, srcX))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    // CORNER
    private fun rotateEye(dst: Mat, center: Point, R: Double, delta: Float, isRight: Boolean) {
        if (abs(delta) < 0.001) return
        val angle = delta * 5 * if (isRight) 1 else -1
        val rect = makeRect(dst, center, R)
        val roi = Mat(dst, rect)

        val rotationMat = Imgproc.getRotationMatrix2D(
            Point(roi.width() / 2.0, roi.height() / 2.0),
            angle.toDouble(), 1.0
        )

        val rotated = Mat()
        Imgproc.warpAffine(roi, rotated, rotationMat, roi.size(), Imgproc.INTER_LINEAR)

        // Blend mịn
        blendRegion(dst, rotated, rect, R)
        roi.release(); rotated.release()
    }

    // UTILITIES
    private fun makeRect(src: Mat, c: Point, R: Double): Rect {
        return Rect(
            max(0, (c.x - R).toInt()),
            max(0, (c.y - R).toInt()),
            min((R * 2).toInt(), src.cols() - (c.x - R).toInt()),
            min((R * 2).toInt(), src.rows() - (c.y - R).toInt())
        )
    }

    private fun blendRegion(dst: Mat, warped: Mat, rect: Rect, R: Double) {
        val mask = Mat.zeros(warped.size(), CvType.CV_8UC1)
        Imgproc.circle(
            mask, Point(mask.width() / 2.0, mask.height() / 2.0),
            (R * 0.9).toInt(), Scalar(255.0), -1
        )
        Imgproc.GaussianBlur(mask, mask, Size(11.0, 11.0), 6.0)
        val sub = dst.submat(rect)
        warped.copyTo(sub, mask)
        mask.release(); sub.release()
    }

    private fun meanPoint(points: List<Point>): Point {
        var sx = 0.0;
        var sy = 0.0
        for (p in points) {
            sx += p.x; sy += p.y
        }
        return Point(sx / max(1, points.size), sy / max(1, points.size))
    }
}