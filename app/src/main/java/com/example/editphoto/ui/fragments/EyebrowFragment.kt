package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.editphoto.databinding.FragmentEyebrowBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.extent.toBitmap
import com.example.editphoto.utils.extent.toMat
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.utils.inter.UnsavedChangesListener
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class EyebrowFragment : Fragment(), SeekBarController, OnApplyListener, UnsavedChangesListener {

    private lateinit var binding: FragmentEyebrowBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var act: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    private var baseBitmap: Bitmap? = null
    private var leftBrowCenter = Point()
    private var rightBrowCenter = Point()
    private var leftBrowInner = Point()
    private var leftBrowOuter = Point()
    private var rightBrowInner = Point()
    private var rightBrowOuter = Point()

    private var currentMode = "less"
    private var seekbarCenterMode = true

    private val browParams = mutableMapOf(
        "size" to 0f,
        "position" to 0f,
        "tilt" to 0f,
        "peak" to 0f,
        "distance" to 0f,
        "length" to 0f
    )

    private var applyJob: Job? = null
    private var selectedOptionView: ImageView? = null
    private var selectedBorderView: ImageView? = null
    private var isDirty = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEyebrowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        act = requireActivity() as EditImageActivity
        viewModel = act.viewModel

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        beforeEditBitmap = act.viewModel.previewBitmap.value
            ?: act.viewModel.editedBitmap.value
                    ?: act.viewModel.originalBitmap.value
    }

    private fun initView() {
        selectOption(binding.eyebrowColorless, binding.borderEyebrowColorless, "less")
    }

    private fun initListener() {
        binding.eyebrowColorless.setOnClickListener {
            selectOption(binding.eyebrowColorless, binding.borderEyebrowColorless, "less")
        }
        binding.eyebrow1.setOnClickListener {
            selectOption(binding.eyebrow1, binding.borderEyebrow1, "size")
        }
        binding.eyebrow2.setOnClickListener {
            selectOption(binding.eyebrow2, binding.borderEyebrow2, "position")
        }
        binding.eyebrow3.setOnClickListener {
            selectOption(binding.eyebrow3, binding.borderEyebrow3, "tilt")
        }
        binding.eyebrow4.setOnClickListener {
            selectOption(binding.eyebrow4, binding.borderEyebrow4, "peak")
        }
        binding.eyebrow5.setOnClickListener {
            selectOption(binding.eyebrow5, binding.borderEyebrow5, "distance")
        }
        binding.eyebrow6.setOnClickListener {
            selectOption(binding.eyebrow6, binding.borderEyebrow6, "length")
        }
    }

    private fun selectOption(optionView: ImageView, borderView: ImageView, mode: String) {
        selectedBorderView?.visibility = View.GONE
        borderView.visibility = View.VISIBLE
        selectedOptionView = optionView
        selectedBorderView = borderView

        if (mode == "less") {
            resetToOriginal()
            act.detachSeekBar()
        } else {
            currentMode = mode
            seekbarCenterMode = true
            val value = browParams[mode] ?: 0f
            val progress = ((value + 1f) / 2f * 100).toInt().coerceIn(0, 100)
            act.binding.seekBarIntensity.progress = progress
            act.attachSeekBar(this)
            scheduleRealtimePreview()
        }
    }

    private fun resetToOriginal() {
        beforeEditBitmap?.let {
            viewModel.setPreview(null)
            viewModel.updateBitmap(it)
        }
        browParams.replaceAll { _, _ -> 0f }
        baseBitmap = null
        prepareData()
        isDirty = false
    }

    override fun onIntensityChanged(intensity: Float) {
        if (currentMode == "less") {
            act.detachSeekBar()
            return
        }
        val adjusted = if (seekbarCenterMode) (intensity * 2f - 1f) else intensity
        browParams[currentMode] = adjusted
        isDirty = (currentMode != "less") && browParams.values.any { it != 0f }
        scheduleRealtimePreview()
    }

    override fun getDefaultIntensity(): Float = if (seekbarCenterMode) 0.5f else 0f

    private fun scheduleRealtimePreview() {
        applyJob?.cancel()
        applyJob = lifecycleScope.launch {
            delay(20)
            updateEyebrowPreview()
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

            // Chọn một tập điểm lông mày từ MediaPipe FaceMesh (xấp xỉ phổ biến)
            val leftBrowIdx = listOf(70, 63, 105, 66, 107, 55, 52, 53, 46, 124)
            val rightBrowIdx = listOf(336, 296, 334, 293, 300, 283, 282, 295, 285, 413)


            val leftPoints = leftBrowIdx.map { Point(landmarks[it].x() * w, landmarks[it].y() * h) }
            val rightPoints = rightBrowIdx.map { Point(landmarks[it].x() * w, landmarks[it].y() * h) }

            leftBrowCenter = meanPoint(leftPoints)
            rightBrowCenter = meanPoint(rightPoints)

            leftBrowInner = leftPoints.maxByOrNull { it.x } ?: leftBrowCenter
            leftBrowOuter = leftPoints.minByOrNull { it.x } ?: leftBrowCenter
            rightBrowInner = rightPoints.minByOrNull { it.x } ?: rightBrowCenter
            rightBrowOuter = rightPoints.maxByOrNull { it.x } ?: rightBrowCenter
        }
    }

    private fun updateEyebrowPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            if (baseBitmap == null) return@launch

            val src = baseBitmap!!.toMat()
            val dst = src.clone()

            val leftR = calculateBrowRadius(leftBrowInner, leftBrowOuter, src)
            val rightR = calculateBrowRadius(rightBrowInner, rightBrowOuter, src)

            val p = browParams

            if ((p["size"] ?: 0f) != 0f) {
                stretchBrowVertical(src, dst, leftBrowCenter, leftR, p["size"]!!)
                stretchBrowVertical(src, dst, rightBrowCenter, rightR, p["size"]!!)
            }
            if ((p["position"] ?: 0f) != 0f) {
                val dY = (p["position"]!! * 25.0)
                moveBrow(src, dst, leftBrowCenter, leftR, dY, 0.0)
                moveBrow(src, dst, rightBrowCenter, rightR, dY, 0.0)
            }
            if ((p["tilt"] ?: 0f) != 0f) {
                tiltBrow(dst, leftBrowCenter, leftR, p["tilt"]!!, isRight = false)
                tiltBrow(dst, rightBrowCenter, rightR, p["tilt"]!!, isRight = true)
            }
            if ((p["peak"] ?: 0f) != 0f) {
                peakCenter(dst, leftBrowCenter, leftR, p["peak"]!!)
                peakCenter(dst, rightBrowCenter, rightR, p["peak"]!!)
            }
            if ((p["distance"] ?: 0f) != 0f) {
                val d = p["distance"]!! * 40.0
                // d > 0: xa nhau; d < 0: gần nhau
                moveBrow(src, dst, leftBrowCenter, leftR, 0.0, -d)
                moveBrow(src, dst, rightBrowCenter, rightR, 0.0, d)
            }
            if ((p["length"] ?: 0f) != 0f) {
                adjustBrowLength(dst, leftBrowCenter, leftR, p["length"]!!, isRight = false)
                adjustBrowLength(dst, rightBrowCenter, rightR, p["length"]!!, isRight = true)
            }

            val bitmapOut = dst.toBitmap()
            withContext(Dispatchers.Main) {
                act.updateImagePreserveZoom(bitmapOut)
            }
            dst.release(); src.release()
        }
    }

    private fun calculateBrowRadius(inner: Point, outer: Point, mat: Mat): Double {
        val width = abs(inner.x - outer.x)
        val R = width * 0.6
        val avgDist = listOf(
            abs((inner.x + outer.x) / 2 - 0), abs((inner.x + outer.x) / 2 - mat.cols()),
            abs((inner.y + outer.y) / 2 - 0), abs((inner.y + outer.y) / 2 - mat.rows())
        ).average()
        return max(10.0, min(R, avgDist * 0.12))
    }

    private fun stretchBrowVertical(src: Mat, dst: Mat, c: Point, R: Double, d: Float) {
        if (abs(d) < 0.001f) return
        val s = d * 0.1f
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - c.x; val dy = gy - c.y
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

    private fun moveBrow(src: Mat, dst: Mat, c: Point, R: Double, shiftY: Double, shiftX: Double) {
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - c.x; val dy = gy - c.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val factor = 1 - (r / R).pow(2.0)
                val srcX = (gx - shiftX * 0.4 * factor).toInt().coerceIn(0, src.cols() - 1)
                val srcY = (gy - shiftY * 0.4 * factor).toInt().coerceIn(0, src.rows() - 1)
                warped.put(y, x, *src.get(srcY, srcX))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }


    private fun tiltBrow(dst: Mat, center: Point, R: Double, delta: Float, isRight: Boolean) {
        if (abs(delta) < 0.001f) return
        val rect = makeRect(dst, center, R)
        val roi = Mat(dst, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        val sign = if (isRight) 1.0 else -1.0
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - center.x; val dy = gy - center.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val grad = (dx / R) * (1 - (r / R).pow(2))
                val shiftY = delta * 20.0 * grad * sign
                val srcY = (gy - shiftY).toInt().coerceIn(0, dst.rows() - 1)
                warped.put(y, x, *dst.get(srcY, gx))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    private fun peakCenter(dst: Mat, center: Point, R: Double, delta: Float) {
        if (abs(delta) < 0.001f) return
        val rect = makeRect(dst, center, R)
        val roi = Mat(dst, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - center.x; val dy = gy - center.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                // Đỉnh giữa: dịch Y mạnh ở tâm, yếu ở 2 đầu theo |dx|
                val decay = (1 - (abs(dx) / R)).coerceIn(0.0, 1.0)
                val factor = decay * (1 - (r / R).pow(2))
                val srcY = (gy - delta * 10.0 * factor).toInt().coerceIn(0, dst.rows() - 1)
                warped.put(y, x, *dst.get(srcY, gx))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    private fun adjustBrowLength(dst: Mat, center: Point, R: Double, delta: Float, isRight: Boolean) {
        if (abs(delta) < 0.001f) return
        val rect = makeRect(dst, center, R)
        val roi = Mat(dst, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        val outerSign = if (isRight) 1.0 else -1.0
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - center.x; val dy = gy - center.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                // Tác động mạnh ở nửa ngoài (đuôi), yếu dần về phía tâm
                val isOuterHalf = (dx * outerSign) > 0
                if (isOuterHalf) {
                    val weight = (abs(dx) / R) * (1 - (r / R).pow(2))
                    val shiftX = outerSign * delta * 30.0 * weight
                    val srcX = (gx - shiftX).toInt().coerceIn(0, dst.cols() - 1)
                    warped.put(y, x, *dst.get(gy, srcX))
                } else {
                    warped.put(y, x, *roi.get(y, x))
                }
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    private fun makeRect(src: Mat, c: Point, R: Double): Rect {
        return Rect(
            max(0, (c.x - R).toInt()),
            max(0, (c.y - R).toInt()),
            min((R * 2).toInt(), src.cols() - max(0, (c.x - R).toInt())),
            min((R * 2).toInt(), src.rows() - max(0, (c.y - R).toInt()))
        )
    }

    private fun blendRegion(dst: Mat, warped: Mat, rect: Rect, R: Double) {
        val mask = Mat.zeros(warped.size(), CvType.CV_8UC1)
        val center = Point(mask.width() / 2.0, mask.height() / 2.0)

        // Xác định hướng theo trục trong–ngoài gần nhất với tâm c
        val useLeft = sqrt((center.x + rect.x - leftBrowCenter.x).pow(2.0) + (center.y + rect.y - leftBrowCenter.y).pow(2.0)) <
                sqrt((center.x + rect.x - rightBrowCenter.x).pow(2.0) + (center.y + rect.y - rightBrowCenter.y).pow(2.0))
        val inner = if (useLeft) leftBrowInner else rightBrowInner
        val outer = if (useLeft) leftBrowOuter else rightBrowOuter

        val width = abs(inner.x - outer.x)
        val angle = Math.toDegrees(kotlin.math.atan2(inner.y - outer.y, inner.x - outer.x))
        val axes = Size(width * 0.65, width * 0.25) // dài hơn theo trục ngang, mảnh theo trục dọc

        Imgproc.ellipse(mask, center, axes, angle, 0.0, 360.0, Scalar(255.0), -1)
        Imgproc.GaussianBlur(mask, mask, Size(11.0, 11.0), 6.0)
        val sub = dst.submat(rect)
        warped.copyTo(sub, mask)
        mask.release(); sub.release()
    }

    private fun meanPoint(points: List<Point>): Point {
        var sx = 0.0; var sy = 0.0
        for (p in points) { sx += p.x; sy += p.y }
        return Point(sx / max(1, points.size), sy / max(1, points.size))
    }

    override fun onApply() {
        val currentBitmap = act.binding.imgPreview.drawable?.toBitmap() ?: return
        viewModel.setPreview(currentBitmap)
        viewModel.commitPreview()
            act.updateImagePreserveZoom(currentBitmap)
        hasApplied = true
        isDirty = false
    }

    // UnsavedChangesListener
    override fun hasUnsavedChanges(): Boolean = isDirty && !hasApplied

    override fun revertUnsavedChanges() {
        if (!hasApplied) {
            resetToOriginal()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        applyJob?.cancel()
        act.detachSeekBar()

        if (!hasApplied) {
            beforeEditBitmap?.let {
            act.updateImagePreserveZoom(it)
                viewModel.setPreview(null)
            }
        }
    }
}