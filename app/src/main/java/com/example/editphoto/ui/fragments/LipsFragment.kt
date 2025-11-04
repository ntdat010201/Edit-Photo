package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.editphoto.databinding.FragmentLipsBinding
import com.example.editphoto.model.ColorScalar
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.utils.inter.UnsavedChangesListener

import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

class LipsFragment : Fragment(), SeekBarController, OnApplyListener,UnsavedChangesListener {

    private lateinit var binding: FragmentLipsBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var parentActivity: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    private var selectedColor: ColorScalar = ColorScalar(0.0, 0.0, 0.0)
    private var intensity: Float = 0.1f

    private var cachedLandmarks: List<NormalizedLandmark>? = null
    private var lipMaskBitmap: Bitmap? = null
    private var baseBitmap: Bitmap? = null

    private var applyJob: Job? = null

    private var selectedColorView: ImageView? = null
    private var selectedBorderView: ImageView? = null

    private val COLORLESS = ColorScalar(0.0, 0.0, 0.0)
    private var isDirty = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLipsBinding.inflate(inflater, container, false)
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
        // MÀU HỒNG SEN: #EF9FC4 → RGB(239,159,196) → BGR(196,159,239)
        binding.pinkLotus.setOnClickListener {
            selectColor(binding.pinkLotus, binding.borderPinkLotus, ColorScalar(196.0, 159.0, 239.0))
        }
        binding.lightPink.setOnClickListener {
            selectColor(binding.lightPink, binding.borderLightPink, ColorScalar(165.0, 106.0, 241.0))
        }
        binding.darkPink.setOnClickListener {
            selectColor(binding.darkPink, binding.borderDarkPink, ColorScalar(135.0, 50.0, 242.0))
        }
        binding.darkRed.setOnClickListener {
            selectColor(binding.darkRed, binding.borderDarkRed, ColorScalar(95.0, 2.0, 207.0))
        }
        binding.orange.setOnClickListener {
            selectColor(binding.orange, binding.borderOrange, ColorScalar(74.0, 74.0, 246.0))
        }
        binding.orangeRed.setOnClickListener {
            selectColor(binding.orangeRed, binding.borderOrangeRed, ColorScalar(3.0, 2.0, 228.0))
        }
        binding.brightRed.setOnClickListener {
            selectColor(binding.brightRed, binding.borderBrightRed, ColorScalar(6.0, 5.0, 191.0))
        }
        binding.earthyBrown.setOnClickListener {
            selectColor(binding.earthyBrown, binding.borderEarthyBrown, ColorScalar(6.0, 6.0, 122.0))
        }
    }

    private fun selectColor(colorView: ImageView, borderView: ImageView, color: ColorScalar) {
        selectedBorderView?.visibility = View.GONE
        borderView.visibility = View.VISIBLE
        selectedColorView = colorView
        selectedBorderView = borderView

        if (color == COLORLESS) {
            resetLipToOriginal()
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

    private fun resetLipToOriginal() {
        beforeEditBitmap?.let {
            viewModel.setPreview(null)
            viewModel.updateBitmap(it)
        }
        cachedLandmarks = null
        lipMaskBitmap = null
        baseBitmap = null
        intensity = 0f
        isDirty = false
    }

    // Public API để Activity kiểm tra trạng thái và hoàn tác khi rời fragment
    override fun hasUnsavedChanges(): Boolean = isDirty && !hasApplied

    override fun revertUnsavedChanges() {
        if (!hasApplied) {
            resetLipToOriginal()
        }
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
            updateLipPreview()
        }
    }

    private suspend fun prepareBaseIfNeeded() {
        if (lipMaskBitmap != null && baseBitmap != null) return
        val bitmap = viewModel.editedBitmap.value ?: return
        val landmarker = viewModel.getFaceLandmarker() ?: return

        withContext(Dispatchers.Default) {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)
            if (result.faceLandmarks().isNotEmpty()) {
                cachedLandmarks = result.faceLandmarks()[0]
                val mask = createLipMaskBitmap(bitmap, cachedLandmarks!!)
                lipMaskBitmap = mask
                baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        }
    }

    private fun updateLipPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            prepareBaseIfNeeded()
            val mask = lipMaskBitmap ?: return@launch
            val base = baseBitmap ?: return@launch

            val bitmapOut = applyLipColorBitmap(base, mask, selectedColor, intensity)
            withContext(Dispatchers.Main) {
                parentActivity.binding.imgPreview.setImageBitmap(bitmapOut)
            }
        }
    }

    private fun createLipMaskBitmap(
        bitmap: Bitmap, landmarks: List<NormalizedLandmark>
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)

        val outerIdx = listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185)
        val innerIdx = listOf(78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308, 415, 310, 311, 312, 13, 82)

        val outerPath = Path().apply {
            for ((i, idx) in outerIdx.withIndex()) {
                val x = landmarks[idx].x() * width
                val y = landmarks[idx].y() * height
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        val innerPath = Path().apply {
            for ((i, idx) in innerIdx.withIndex()) {
                val x = landmarks[idx].x() * width
                val y = landmarks[idx].y() * height
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        canvas.drawPath(outerPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        })

        canvas.drawPath(innerPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })

        return blurMaskAlpha(mask, radius = 6)
    }

    private fun blurMaskAlpha(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return src
        val w = src.width; val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val inPixels = IntArray(w * h)
        val tmpPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        for (i in 0 until w * h) tmpPixels[i] = (inPixels[i] ushr 24) and 0xFF

        val r = radius
        for (y in 0 until h) {
            var sum = 0
            val yi = y * w
            for (x in 0..r) if (x < w) sum += tmpPixels[yi + x]
            for (x in 0 until w) {
                val left = x - r - 1
                val right = minOf(w - 1, x + r)
                if (left >= 0) sum -= tmpPixels[yi + left]
                sum += tmpPixels[yi + right]
                outPixels[yi + x] = sum / (r * 2 + 1)
            }
        }

        val vertPixels = IntArray(w * h)
        for (x in 0 until w) {
            var sum = 0
            for (y in 0..r) if (y < h) sum += outPixels[y * w + x]
            for (y in 0 until h) {
                val top = y - r - 1
                val bottom = minOf(h - 1, y + r)
                if (top >= 0) sum -= outPixels[top * w + x]
                sum += outPixels[bottom * w + x]
                vertPixels[y * w + x] = sum / (r * 2 + 1)
            }
        }

        for (i in 0 until w * h) {
            val a = vertPixels[i].coerceIn(0, 255)
            outPixels[i] = (a shl 24) or 0xFFFFFF
        }
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applyLipColorBitmap(
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

        val targetR = colorBGR.red.toInt().coerceIn(0, 255)
        val targetG = colorBGR.green.toInt().coerceIn(0, 255)
        val targetBlue = colorBGR.blue.toInt().coerceIn(0, 255)

        val (targetL, targetA, targetBLab) = rgbToLab(targetR, targetG, targetBlue)
        val chromaBoost = 1.45f
        val boostedA = targetA * chromaBoost
        val boostedB = targetBLab * chromaBoost
        val finalL = targetL.coerceIn(40.0, 85.0)

        val (cleanR, cleanG, cleanB) = labToRgb(finalL, boostedA, boostedB)
        val keepTexture = 0.25f
        val applyStrength = (intensity* 0.5f).coerceIn(0f, 1f)

        for (i in 0 until w * h) {
            val alphaMask = (maskPixels[i] ushr 24) / 255.0f
            if (alphaMask == 0f) continue

            val pixel = basePixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val mixedR = (cleanR * (1 - keepTexture) + r * keepTexture).toInt().coerceIn(0, 255)
            val mixedG = (cleanG * (1 - keepTexture) + g * keepTexture).toInt().coerceIn(0, 255)
            val mixedB = (cleanB * (1 - keepTexture) + b * keepTexture).toInt().coerceIn(0, 255)

            val finalR = (mixedR * applyStrength + r * (1 - applyStrength)).toInt()
            val finalG = (mixedG * applyStrength + g * (1 - applyStrength)).toInt()
            val finalB = (mixedB * applyStrength + b * (1 - applyStrength)).toInt()

            val outR = (finalR * alphaMask + r * (1 - alphaMask)).toInt()
            val outG = (finalG * alphaMask + g * (1 - alphaMask)).toInt()
            val outB = (finalB * alphaMask + b * (1 - alphaMask)).toInt()

            basePixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        out.setPixels(basePixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun rgbToLab(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        fun srgbToLinear(c: Double): Double =
            if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

        val R = srgbToLinear(r / 255.0)
        val G = srgbToLinear(g / 255.0)
        val B = srgbToLinear(b / 255.0)

        val X = R * 0.4124 + G * 0.3576 + B * 0.1805
        val Y = R * 0.2126 + G * 0.7152 + B * 0.0722
        val Z = R * 0.0193 + G * 0.1192 + B * 0.9505

        val Xn = 0.95047; val Yn = 1.0; val Zn = 1.08883
        var x = X / Xn; var y = Y / Yn; var z = Z / Zn

        fun f(t: Double): Double =
            if (t > 0.008856) Math.cbrt(t) else (7.787 * t + 16.0 / 116.0)

        val fx = f(x); val fy = f(y); val fz = f(z)
        val L = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val bLab = 200.0 * (fy - fz)
        return Triple(L, a, bLab)
    }

    private fun labToRgb(L: Double, a: Double, b: Double): Triple<Int, Int, Int> {
        val fy = (L + 16.0) / 116.0
        val fx = a / 500.0 + fy
        val fz = fy - b / 200.0

        fun fInv(t: Double): Double {
            val t3 = t * t * t
            return if (t3 > 0.008856) t3 else (t - 16.0 / 116.0) / 7.787
        }

        val Xn = 0.95047; val Yn = 1.0; val Zn = 1.08883
        val X = Xn * fInv(fx)
        val Y = Yn * fInv(fy)
        val Z = Zn * fInv(fz)

        var R = 3.2406 * X - 1.5372 * Y - 0.4986 * Z
        var G = -0.9689 * X + 1.8758 * Y + 0.0415 * Z
        var B = 0.0557 * X - 0.2040 * Y + 1.0570 * Z

        fun linearToSrgb(c: Double): Double =
            if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1.0 / 2.4) - 0.055

        R = linearToSrgb(R); G = linearToSrgb(G); B = linearToSrgb(B)
        return Triple(
            (R * 255).toInt().coerceIn(0, 255),
            (G * 255).toInt().coerceIn(0, 255),
            (B * 255).toInt().coerceIn(0, 255)
        )
    }

    override fun onApply() {
        val currentBitmap = parentActivity.binding.imgPreview.drawable?.toBitmap() ?: return
        viewModel.setPreview(currentBitmap)
        viewModel.commitPreview()
        hasApplied = true
        isDirty = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity.detachSeekBar()
    }
}