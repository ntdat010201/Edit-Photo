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
import com.example.editphoto.databinding.FragmentLipsBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.utils.extent.handleBackPressedCommon
import com.example.editphoto.utils.extent.handlePhysicalBackPress
import com.example.editphoto.utils.extent.toBitmap
import com.example.editphoto.utils.extent.toMat
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class LipsFragment : Fragment(), SeekBarController, OnApplyListener {

    private lateinit var binding: FragmentLipsBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var parentActivity: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    private var selectedColor: Scalar = Scalar(0.0, 0.0, 0.0)
    private var intensity: Float = 0.1f

    private var cachedLandmarks: List<NormalizedLandmark>? = null
    private var lipMask: Mat? = null
    private var lipRegion: Mat? = null
    private var baseMat: Mat? = null

    private var applyJob: Job? = null

    private var selectedColorView: ImageView? = null
    private var selectedBorderView: ImageView? = null

    // Màu "không tô"
    private val COLORLESS = Scalar(0.0, 0.0, 0.0)

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
        // Mặc định chọn colorless + ẩn SeekBar
        selectColor(binding.colorless, binding.borderColorless, COLORLESS)
    }

    private fun initListener() {
        // === CÁC MÀU ===
        binding.colorless.setOnClickListener {
            selectColor(binding.colorless, binding.borderColorless, COLORLESS)
        }
        binding.pinkLotus.setOnClickListener {
            selectColor(binding.pinkLotus, binding.borderPinkLotus, Scalar(239.0, 159.0, 196.0))
        }
        binding.lightPink.setOnClickListener {
            selectColor(binding.lightPink, binding.borderLightPink, Scalar(241.0, 106.0, 165.0))
        }
        binding.darkPink.setOnClickListener {
            selectColor(binding.darkPink, binding.borderDarkPink, Scalar(242.0, 50.0, 135.0))
        }
        binding.darkRed.setOnClickListener {
            selectColor(binding.darkRed, binding.borderDarkRed, Scalar(207.0, 2.0, 95.0))
        }
        binding.orange.setOnClickListener {
            selectColor(binding.orange, binding.borderOrange, Scalar(246.0, 74.0, 74.0))
        }
        binding.orangeRed.setOnClickListener {
            selectColor(binding.orangeRed, binding.borderOrangeRed, Scalar(228.0, 2.0, 3.0))
        }
        binding.brightRed.setOnClickListener {
            selectColor(binding.brightRed, binding.borderBrightRed, Scalar(191.0, 5.0, 6.0))
        }
        binding.earthyBrown.setOnClickListener {
            selectColor(binding.earthyBrown, binding.borderEarthyBrown, Scalar(122.0, 6.0, 6.0))
        }


    }

    private fun selectColor(colorView: ImageView, borderView: ImageView, color: Scalar) {
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
            parentActivity.attachSeekBar(this)
            parentActivity.binding.seekBarIntensity.progress = 0
            scheduleRealtimePreview()
        }
    }

    // === RESET MÔI VỀ GỐC ===
    private fun resetLipToOriginal() {
        beforeEditBitmap?.let {
            viewModel.setPreview(null)
            viewModel.updateBitmap(it)
        }
        cachedLandmarks = null
        lipMask = null
        lipRegion = null
        baseMat = null
        intensity = 0f
    }

    // === SEEKBAR CALLBACK ===
    override fun onIntensityChanged(intensity: Float) {
        if (selectedColor == COLORLESS) {
            parentActivity.detachSeekBar()
            return
        }
        this.intensity = intensity
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
        if (lipMask != null && lipRegion != null && baseMat != null) return
        val bitmap = viewModel.editedBitmap.value ?: return
        val landmarker = viewModel.getFaceLandmarker() ?: return

        withContext(Dispatchers.Default) {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)
            if (result.faceLandmarks().isNotEmpty()) {
                cachedLandmarks = result.faceLandmarks()[0]
                val (mask, region, base) = createLipMaskAndRegion(bitmap, cachedLandmarks!!)
                lipMask = mask
                lipRegion = region
                baseMat = base
            }
        }
    }

    private fun updateLipPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            prepareBaseIfNeeded()
            if (lipMask == null || lipRegion == null || baseMat == null) return@launch

            val preview = baseMat!!.clone()
            val blended = lipRegion!!.quickApplyLipColor(selectedColor, intensity)
            blended.copyTo(preview, lipMask)

            val bitmapOut = preview.toBitmap()
            withContext(Dispatchers.Main) {
                parentActivity.binding.imgPreview.setImageBitmap(bitmapOut)
            }
        }
    }

    // === XỬ LÝ MÔI ===
    private fun createLipMaskAndRegion(
        bitmap: Bitmap, landmarks: List<NormalizedLandmark>
    ): Triple<Mat, Mat, Mat> {
        val inputMat = bitmap.toMat()
        val width = bitmap.width.toDouble()
        val height = bitmap.height.toDouble()

        val outerLipIndices = listOf(
            61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409,
            270, 269, 267, 0, 37, 39, 40, 185
        )
        val innerLipIndices = listOf(
            78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308, 415,
            310, 311, 312, 13, 82
        )

        val outerPoints =
            outerLipIndices.map { Point(landmarks[it].x() * width, landmarks[it].y() * height) }
        val innerPoints =
            innerLipIndices.map { Point(landmarks[it].x() * width, landmarks[it].y() * height) }

        val mask = Mat.zeros(height.toInt(), width.toInt(), CvType.CV_8UC1)
        Imgproc.fillPoly(mask, listOf(MatOfPoint(*outerPoints.toTypedArray())), Scalar(255.0))
        Imgproc.fillPoly(mask, listOf(MatOfPoint(*innerPoints.toTypedArray())), Scalar(0.0))
        Imgproc.GaussianBlur(mask, mask, Size(7.0, 7.0), 4.0)

        val region = Mat()
        inputMat.copyTo(region, mask)

        return Triple(mask, region, inputMat)
    }

    private fun Mat.quickApplyLipColor(color: Scalar, intensity: Float): Mat {
        val overlay = Mat(this.size(), this.type(), color)
        val blended = Mat()
        Core.addWeighted(this, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)
        return blended
    }

    override fun onApply() {
        val currentBitmap = parentActivity.binding.imgPreview.drawable?.toBitmap() ?: return
        viewModel.setPreview(currentBitmap)
        viewModel.commitPreview()
        hasApplied = true
    }

    override fun onDestroyView() {
        super.onDestroyView()

        parentActivity.detachSeekBar()
    }
}