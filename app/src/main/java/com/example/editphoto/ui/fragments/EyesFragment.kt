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
import com.example.editphoto.databinding.FragmentEyesBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.utils.extent.toBitmap
import com.example.editphoto.utils.extent.toMat
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

class EyesFragment : Fragment(), SeekBarController, OnApplyListener {

    private lateinit var binding: FragmentEyesBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var act: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    private var baseBitmap: Bitmap? = null
    private var leftEyeCenter = Point()
    private var rightEyeCenter = Point()
    private var currentMode = "less"

    // Chế độ seekbar giữa (50 = 0)
    private var seekbarCenterMode = true

    private val eyeParams = mutableMapOf(
        "size" to 0f,
        "height" to 0f,
        "width" to 0f,
        "location" to 0f,
        "distance" to 0f,
        "corner" to 0f
    )

    private var applyJob: Job? = null
    private var selectedOptionView: ImageView? = null
    private var selectedBorderView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEyesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        act = requireActivity() as EditImageActivity
        viewModel = act.viewModel

/*
        // Lưu ảnh gốc trước khi chỉnh
        beforeEditBitmap = viewModel.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
            ?: viewModel.originalBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
*/

        beforeEditBitmap = act.viewModel.previewBitmap.value
            ?: act.viewModel.editedBitmap.value
                    ?: act.viewModel.originalBitmap.value


        prepareData()
        initView()
        initListener()
    }

    private fun initView() {
        selectOption(binding.ivIconEyeLess, binding.borderEyeLess, "less")
    }

    private fun initListener() {
        binding.eyeLess.setOnClickListener {
            selectOption(binding.ivIconEyeLess, binding.borderEyeLess, "less")
        }
        binding.eyeSize.setOnClickListener {
            selectOption(binding.ivIconEyeSize, binding.borderEyeSize, "size")
        }
        binding.eyeHeight.setOnClickListener {
            selectOption(binding.ivIconEyeHeight, binding.borderEyeHeight, "height")
        }
        binding.eyeWidth.setOnClickListener {
            selectOption(binding.ivIconEyeWidth, binding.borderEyeWidth, "width")
        }
        binding.eyeLocation.setOnClickListener {
            selectOption(binding.ivIconEyeLocation, binding.borderEyeLocation, "location")
        }
        binding.eyeDistance.setOnClickListener {
            selectOption(binding.ivIconEyeDistance, binding.borderEyeDistance, "distance")
        }
        binding.eyeCorner.setOnClickListener {
            selectOption(binding.ivIconEyeCorner, binding.borderEyeCorner, "corner")
        }
    }

    private fun selectOption(optionView: ImageView, borderView: ImageView, mode: String) {
        selectedBorderView?.visibility = View.GONE
        borderView.visibility = View.VISIBLE
        selectedOptionView = optionView
        selectedBorderView = borderView

        if (mode == "less") {
            resetEyesToOriginal()
            act.detachSeekBar()
        } else {
            currentMode = mode
            seekbarCenterMode = true
            val value = eyeParams[mode] ?: 0f
            val progress = ((value + 1f) / 2f * 100).toInt().coerceIn(0, 100)
            act.binding.seekBarIntensity.progress = progress
            act.attachSeekBar(this)
            scheduleRealtimePreview()
        }
    }

    private fun resetEyesToOriginal() {
        beforeEditBitmap?.let {
            viewModel.setPreview(null)
            viewModel.updateBitmap(it) // khôi phục ảnh gốc
        }
        eyeParams.replaceAll { _, _ -> 0f }
        baseBitmap = null
        prepareData()
    }

    override fun onIntensityChanged(intensity: Float) {
        if (currentMode == "less") {
            act.detachSeekBar()
            return
        }
        val adjusted = if (seekbarCenterMode) (intensity * 2f - 1f) else intensity
        eyeParams[currentMode] = adjusted
        scheduleRealtimePreview()
    }

    override fun getDefaultIntensity(): Float = if (seekbarCenterMode) 0.5f else 0f

    private fun scheduleRealtimePreview() {
        applyJob?.cancel()
        applyJob = lifecycleScope.launch {
            delay(20)
            updateEyesPreview()
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

            val leftEyeIndices = listOf(33, 133, 159, 145, 160, 161, 246, 163, 144, 145, 153, 154, 155, 133)
            val rightEyeIndices = listOf(362, 263, 386, 374, 387, 388, 466, 390, 373, 374, 380, 381, 382, 263)

            val leftPoints = leftEyeIndices.map { Point(landmarks[it].x() * w, landmarks[it].y() * h) }
            val rightPoints = rightEyeIndices.map { Point(landmarks[it].x() * w, landmarks[it].y() * h) }

            leftEyeCenter = meanPoint(leftPoints)
            rightEyeCenter = meanPoint(rightPoints)
        }
    }

    private fun updateEyesPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            if (baseBitmap == null) return@launch

            val src = baseBitmap!!.toMat()
            val dst = src.clone()

            val leftR = calculateEyeRadius(leftEyeCenter, src)
            val rightR = calculateEyeRadius(rightEyeCenter, src)

            val p = eyeParams

            if (p["size"] != 0f) {
                stretchEye(src, dst, leftEyeCenter, leftR, p["size"]!!)
                stretchEye(src, dst, rightEyeCenter, rightR, p["size"]!!)
            }
            if (p["height"] != 0f) {
                stretchEyeVertical(dst, dst, leftEyeCenter, leftR, p["height"]!!)
                stretchEyeVertical(dst, dst, rightEyeCenter, rightR, p["height"]!!)
            }
            if (p["width"] != 0f) {
                stretchEyeHorizontal(dst, dst, leftEyeCenter, leftR, p["width"]!!)
                stretchEyeHorizontal(dst, dst, rightEyeCenter, rightR, p["width"]!!)
            }
            if (p["location"] != 0f) {
                moveEye(dst, dst, leftEyeCenter, leftR, p["location"]!! * 30.0, 0.0)
                moveEye(dst, dst, rightEyeCenter, rightR, p["location"]!! * 30.0, 0.0)
            }
            if (p["distance"] != 0f) {
                val d = p["distance"]!! * 40
                moveEye(dst, dst, leftEyeCenter, leftR, 0.0, d.toDouble())
                moveEye(dst, dst, rightEyeCenter, rightR, 0.0, -d.toDouble())
            }
            if (p["corner"] != 0f) {
                rotateEye(dst, leftEyeCenter, leftR, p["corner"]!!, false)
                rotateEye(dst, rightEyeCenter, rightR, p["corner"]!!, true)
            }

            val bitmapOut = dst.toBitmap()
            withContext(Dispatchers.Main) {
               /* viewModel.setPreview(bitmapOut)*/
                act.binding.imgPreview.setImageBitmap(bitmapOut)
            }
            dst.release()
            src.release()
        }
    }

    // === Các hàm xử lý ảnh (giữ nguyên) ===
    private fun calculateEyeRadius(center: Point, mat: Mat): Double {
        val avgDist = listOf(
            abs(center.x - 0), abs(center.x - mat.cols()),
            abs(center.y - 0), abs(center.y - mat.rows())
        ).average()
        return avgDist * 0.15
    }

    private fun stretchEye(src: Mat, dst: Mat, c: Point, R: Double, d: Float) {
        if (abs(d) < 0.001f) return
        val s = d * 0.4f
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - c.x; val dy = gy - c.y
            val r = sqrt(dx * dx + dy * dy)
            if (r < R) {
                val f = 1 + s * (1 - (r / R).pow(2))
                val srcX = (c.x + dx / f).toInt().coerceIn(0, src.cols() - 1)
                val srcY = (c.y + dy / f).toInt().coerceIn(0, src.rows() - 1)
                warped.put(y, x, *src.get(srcY, srcX))
            } else warped.put(y, x, *roi.get(y, x))
        }
        blendRegion(dst, warped, rect, R)
        roi.release(); warped.release()
    }

    private fun stretchEyeVertical(src: Mat, dst: Mat, c: Point, R: Double, d: Float) {
        if (abs(d) < 0.001f) return
        val s = d * 0.3f
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

    private fun stretchEyeHorizontal(src: Mat, dst: Mat, c: Point, R: Double, d: Float) {
        if (abs(d) < 0.001f) return
        val s = d * 0.3f
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - c.x; val dy = gy - c.y
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

    private fun moveEye(src: Mat, dst: Mat, c: Point, R: Double, shiftY: Double, shiftX: Double) {
        val rect = makeRect(src, c, R)
        val roi = Mat(src, rect)
        val warped = Mat.zeros(roi.size(), roi.type())
        for (y in 0 until roi.rows()) for (x in 0 until roi.cols()) {
            val gx = x + rect.x; val gy = y + rect.y
            val dx = gx - c.x; val dy = gy - c.y
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

    private fun rotateEye(dst: Mat, center: Point, R: Double, delta: Float, isRight: Boolean) {
        if (abs(delta) < 0.001f) return
        val angle = delta * 5 * if (isRight) 1 else -1
        val rect = makeRect(dst, center, R)
        val roi = Mat(dst, rect)
        val rotationMat = Imgproc.getRotationMatrix2D(
            Point(roi.width() / 2.0, roi.height() / 2.0), angle.toDouble(), 1.0
        )
        val rotated = Mat()
        Imgproc.warpAffine(roi, rotated, rotationMat, roi.size(), Imgproc.INTER_LINEAR)
        blendRegion(dst, rotated, rect, R)
        roi.release(); rotated.release(); rotationMat.release()
    }

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
        Imgproc.circle(mask, Point(mask.width() / 2.0, mask.height() / 2.0), (R * 0.9).toInt(), Scalar(255.0), -1)
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
        viewModel.commitPreview() // Lưu thay đổi vào editedBitmap
        act.binding.imgPreview.setImageBitmap(currentBitmap)

        hasApplied = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        applyJob?.cancel()
        act.detachSeekBar()

        if (!hasApplied) {
            beforeEditBitmap?.let {
                act.binding.imgPreview.setImageBitmap(it)
                viewModel.setPreview(null)
            }
        }
    }
}