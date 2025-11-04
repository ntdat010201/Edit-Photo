package com.example.editphoto.ui.fragments

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.editphoto.databinding.FragmentCheeksBinding
import com.example.editphoto.model.ColorScalar
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.utils.inter.UnsavedChangesListener
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.*
import kotlin.math.*

class CheeksFragment : Fragment(), SeekBarController, OnApplyListener,UnsavedChangesListener {

    private lateinit var binding: FragmentCheeksBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var parentActivity: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    private var selectedColor: ColorScalar = ColorScalar(0.0, 0.0, 0.0)
    private var intensity: Float = 0.1f

    private var cachedLandmarks: List<NormalizedLandmark>? = null
    private var leftCheekMask: Bitmap? = null
    private var rightCheekMask: Bitmap? = null
    private var baseBitmap: Bitmap? = null

    private var applyJob: Job? = null
    private var selectedColorView: ImageView? = null
    private var selectedBorderView: ImageView? = null

    private val COLORLESS = ColorScalar(0.0, 0.0, 0.0)
    private var isDirty = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCheeksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentActivity = requireActivity() as EditImageActivity
        viewModel = parentActivity.viewModel
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        beforeEditBitmap = parentActivity.viewModel.previewBitmap.value
            ?: parentActivity.viewModel.editedBitmap.value
                    ?: parentActivity.viewModel.originalBitmap.value
    }

    private fun initView() {
        selectColor(binding.colorless, binding.borderColorless, COLORLESS)
    }

    private fun initListener() {
        binding.colorless.setOnClickListener {
            selectColor(binding.colorless, binding.borderColorless, COLORLESS)
        }


        binding.colorBlush1.setOnClickListener {
            selectColor(binding.colorBlush1, binding.borderColorBlush1, ColorScalar(196.0, 159.0, 239.0)) // #FFE40203
        }
        binding.colorBlush2.setOnClickListener {
            selectColor(binding.colorBlush2, binding.borderColorBlush2, ColorScalar(165.0, 106.0, 241.0)) // #FFF64A4A
        }
        binding.colorBlush3.setOnClickListener {
            selectColor(binding.colorBlush3, binding.borderColorBlush3, ColorScalar(135.0, 50.0, 242.0)) // #FFCF025F
        }
        binding.colorBlush4.setOnClickListener {
            selectColor(binding.colorBlush4, binding.borderColorBlush4, ColorScalar(95.0, 2.0, 207.0)) // #FFF23287
        }
        binding.colorBlush5.setOnClickListener {
            selectColor(binding.colorBlush5, binding.borderColorBlush5, ColorScalar(74.0, 74.0, 246.0)) // #FFF16AA5
        }
        binding.colorBlush6.setOnClickListener {
            selectColor(binding.colorBlush6, binding.borderColorBlush6, ColorScalar(3.0, 2.0, 228.0)) // #FFEF9FC4
        }
    }

    private fun selectColor(colorView: ImageView, borderView: ImageView, color: ColorScalar) {
        selectedBorderView?.visibility = View.GONE
        borderView.visibility = View.VISIBLE
        selectedColorView = colorView
        selectedBorderView = borderView

        if (color == COLORLESS) {
            resetCheeksToOriginal()
            parentActivity.detachSeekBar()
        } else {
            selectedColor = color
            intensity = 0.0f
            isDirty = false
            parentActivity.attachSeekBar(this)
            parentActivity.binding.seekBarIntensity.progress = 0
            scheduleRealtimePreview()
        }
    }

    private fun resetCheeksToOriginal() {
        beforeEditBitmap?.let {
            viewModel.setPreview(null)
            viewModel.updateBitmap(it)
        }
        cachedLandmarks = null
        leftCheekMask = null
        rightCheekMask = null
        baseBitmap = null
        intensity = 0f
        isDirty = false
    }

    override fun onIntensityChanged(intensity: Float) {
        if (selectedColor == COLORLESS) {
            parentActivity.detachSeekBar()
            return
        }
        this.intensity = intensity
        isDirty = (selectedColor != COLORLESS && intensity > 0f)
        scheduleRealtimePreview()
    }

    override fun getDefaultIntensity(): Float = 0.1f

    private fun scheduleRealtimePreview() {
        applyJob?.cancel()
        applyJob = lifecycleScope.launch {
            delay(20)
            updateCheeksPreview()
        }
    }

    private suspend fun prepareBaseIfNeeded() {
        if (leftCheekMask != null && rightCheekMask != null && baseBitmap != null) return
        val bitmap = viewModel.editedBitmap.value ?: return
        val landmarker = viewModel.getFaceLandmarker() ?: return

        withContext(Dispatchers.Default) {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)
            if (result.faceLandmarks().isNotEmpty()) {
                cachedLandmarks = result.faceLandmarks()[0]
                val leftMask = createCheekMaskBitmap(bitmap, cachedLandmarks!!, isLeft = true)
                val rightMask = createCheekMaskBitmap(bitmap, cachedLandmarks!!, isLeft = false)
                leftCheekMask = leftMask
                rightCheekMask = rightMask
                baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        }
    }

    private fun updateCheeksPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            prepareBaseIfNeeded()
            val leftMask = leftCheekMask ?: return@launch
            val rightMask = rightCheekMask ?: return@launch
            val base = baseBitmap ?: return@launch

            val tmp = applyBlushSoftLight(base, leftMask, selectedColor, intensity)
            val bitmapOut = applyBlushSoftLight(tmp, rightMask, selectedColor, intensity)
            withContext(Dispatchers.Main) {
                parentActivity.binding.imgPreview.setImageBitmap(bitmapOut)
            }
        }
    }

    // TẠO MASK HÌNH TRÒN TỰ ĐỘNG THEO MẶT
    private fun createCheekMaskBitmap(
        bitmap: Bitmap,
        landmarks: List<NormalizedLandmark>,
        isLeft: Boolean
    ): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val mask = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Điểm trung tâm: gò má cao + khóe miệng
        val centerIdx1 = if (isLeft) 234 else 454  // khóe miệng
        val centerIdx2 = if (isLeft) 205 else 425
        val cx = (landmarks[centerIdx1].x() + landmarks[centerIdx2].x()) / 2 * w
        val cy = (landmarks[centerIdx1].y() + landmarks[centerIdx2].y()) / 2 * h

        // Tính bán kính theo chiều rộng miệng
        val mouthLeft = Point((landmarks[61].x() * w).toInt(), (landmarks[61].y() * h).toInt())
        val mouthRight = Point((landmarks[291].x() * w).toInt(), (landmarks[291].y() * h).toInt())
        val mouthWidth = hypot((mouthRight.x - mouthLeft.x).toDouble(), (mouthRight.y - mouthLeft.y).toDouble())
        val radius = (mouthWidth * 0.4).toFloat() // 22% miệng

        val oval = RectF(
            cx - radius, cy - radius * 0.9f,
            cx + radius, cy + radius * 0.9f
        )
        canvas.drawOval(oval, paint)

        return blurMaskAlpha(mask, radius = (radius * 0.4f).toInt().coerceAtLeast(12))
    }

    // BLUR GAUSSIAN THỦ CÔNG (nhanh hơn RenderScript)
    private fun blurMaskAlpha(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceAtLeast(1)
        val w = src.width; val h = src.height
        val tmp = src.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Chỉ lấy alpha
        for (i in pixels.indices) pixels[i] = (pixels[i] ushr 24)

        val out = IntArray(w * h)
        // Horizontal
        for (y in 0 until h) {
            var sum = 0
            val yi = y * w
            for (x in 0..r) if (x < w) sum += pixels[yi + x]
            for (x in 0 until w) {
                val left = (x - r - 1).coerceAtLeast(0)
                val right = (x + r).coerceAtMost(w - 1)
                if (left >= 0) sum -= pixels[yi + left]
                sum += pixels[yi + right]
                out[yi + x] = sum / (r * 2 + 1)
            }
        }
        // Vertical
        val vert = IntArray(w * h)
        for (x in 0 until w) {
            var sum = 0
            for (y in 0..r) if (y < h) sum += out[y * w + x]
            for (y in 0 until h) {
                val top = (y - r - 1).coerceAtLeast(0)
                val bot = (y + r).coerceAtMost(h - 1)
                if (top >= 0) sum -= out[top * w + x]
                sum += out[bot * w + x]
                vert[y * w + x] = sum / (r * 2 + 1)
            }
        }
        // Gán lại alpha
        for (i in pixels.indices) {
            val a = vert[i].coerceIn(0, 255)
            pixels[i] = (a shl 24) or 0x00FFFFFF
        }
        tmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return tmp
    }

    // BLEND SOFT LIGHT (GIỐNG APP LỚN)
    private fun applyBlushSoftLight(
        base: Bitmap,
        mask: Bitmap,
        colorBGR: ColorScalar,
        intensity: Float
    ): Bitmap {
        val w = base.width; val h = base.height
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val basePixels = IntArray(w * h)
        val maskPixels = IntArray(w * h)
        out.getPixels(basePixels, 0, w, 0, 0, w, h)
        mask.getPixels(maskPixels, 0, w, 0, 0, w, h)

        val cr = colorBGR.red.toInt().coerceIn(0, 255)
        val cg = colorBGR.green.toInt().coerceIn(0, 255)
        val cb = colorBGR.blue.toInt().coerceIn(0, 255)

        fun softLight(overlay: Int, base: Int): Int {
            val o = overlay / 255f
            val b = base / 255f
            val result = if (o <= 0.5f) {
                b - (1f - 2f * o) * b * (1f - b)
            } else {
                b + (2f * o - 1f) * (sqrt(b) - b)
            }
            return (result * 255).toInt().coerceIn(0, 255)
        }

        for (i in 0 until w * h) {
            val alpha = (maskPixels[i] ushr 24) / 255f * intensity.coerceIn(0f, 1f)
            if (alpha < 0.01f) continue

            val r = (basePixels[i] shr 16) and 0xFF
            val g = (basePixels[i] shr 8) and 0xFF
            val b = basePixels[i] and 0xFF

            val finalR = ((1 - alpha) * r + alpha * softLight(cr, r)).toInt()
            val finalG = ((1 - alpha) * g + alpha * softLight(cg, g)).toInt()
            val finalB = ((1 - alpha) * b + alpha * softLight(cb, b)).toInt()

            basePixels[i] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }
        out.setPixels(basePixels, 0, w, 0, 0, w, h)
        return out
    }

    override fun onApply() {
        val currentBitmap = parentActivity.binding.imgPreview.drawable?.toBitmap() ?: return
        viewModel.setPreview(currentBitmap)
        viewModel.commitPreview()
        hasApplied = true
        isDirty = false
    }

    // UnsavedChangesListener
    override fun hasUnsavedChanges(): Boolean = isDirty && !hasApplied

    override fun revertUnsavedChanges() {
        if (!hasApplied) {
            resetCheeksToOriginal()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity.detachSeekBar()
    }
}