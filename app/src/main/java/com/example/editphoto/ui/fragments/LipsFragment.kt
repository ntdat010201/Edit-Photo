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
import com.example.editphoto.databinding.FragmentLipsBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.handleBackPressedCommon
import com.example.editphoto.utils.handlePhysicalBackPress
import com.example.editphoto.utils.toBitmap
import com.example.editphoto.utils.toMat
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

class LipsFragment : Fragment() {

    private lateinit var binding: FragmentLipsBinding
    private lateinit var viewModel: EditImageViewModel

    private var beforeEditBitmap: Bitmap? = null
    private var hasApplied = false

    // Màu môi hiện tại (Scalar BGR)
    private var selectedColor: Scalar = Scalar(233.0, 30.0, 99.0)
    private var intensity: Float = 0.1f

    // Dữ liệu môi
    private var cachedLandmarks: List<NormalizedLandmark>? = null
    private var lipMask: Mat? = null
    private var lipRegion: Mat? = null
    private var baseMat: Mat? = null

    private var applyJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val act = requireActivity() as EditImageActivity
        viewModel = act.viewModel

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        beforeEditBitmap = viewModel.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun initView() {

    }

    private fun initListener() {
        binding.colorRed.setOnClickListener {
            selectedColor = Scalar(244.0, 67.0, 54.0); scheduleRealtimePreview()
        }
        binding.colorGreen.setOnClickListener {
            selectedColor = Scalar(233.0, 30.0, 99.0); scheduleRealtimePreview()
        }
        binding.colorPurple.setOnClickListener {
            selectedColor = Scalar(156.0, 39.0, 176.0); scheduleRealtimePreview()
        }
        binding.colorYellow.setOnClickListener {
            selectedColor = Scalar(103.0, 58.0, 183.0); scheduleRealtimePreview()
        }


        binding.btnApply.setOnClickListener {
            viewModel.commitPreview()
            hasApplied = true
            beforeEditBitmap = viewModel.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
            parentFragmentManager.popBackStack()
        }


        // Reset
        binding.btnReset.setOnClickListener {
            if (!hasApplied) {
                beforeEditBitmap?.let {
                    viewModel.setPreview(null)
                    viewModel.updateBitmap(it)
                }
                cachedLandmarks = null
                lipMask = null
                lipRegion = null
                baseMat = null
            }
        }
        // Back
        binding.btnBack.setOnClickListener {
            val act = requireActivity() as EditImageActivity
            handleBackPressedCommon(act, hasApplied, beforeEditBitmap)
        }

        handlePhysicalBackPress { act ->
            handleBackPressedCommon(act, hasApplied, beforeEditBitmap)
        }

        // SeekBar realtime
        binding.lipsIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                intensity = progress / 100f
                scheduleRealtimePreview()
            }

            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

    }


    private fun scheduleRealtimePreview() {
        applyJob?.cancel()
        applyJob = lifecycleScope.launch {
            delay(20)
            updateLipPreview()
        }
    }

    /** Chuẩn bị mask môi */
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

    /** Realtime preview (cập nhật vào previewBitmap) */
    private fun updateLipPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            prepareBaseIfNeeded()
            if (lipMask == null || lipRegion == null || baseMat == null) return@launch

            val blended = lipRegion!!.quickApplyLipColor(selectedColor, intensity)
            val preview = baseMat!!.clone()
            blended.copyTo(preview, lipMask)

            val bitmapOut = preview.toBitmap()
            withContext(Dispatchers.Main) {
                viewModel.setPreview(bitmapOut)  // Sử dụng previewBitmap thay vì update trực tiếp editedBitmap
            }
        }
    }

    /** Khi nhấn Áp dụng (commit preview vào editedBitmap) */
    private fun applyFinalLipColor() {
        viewModel.commitPreview()  //
        parentFragmentManager.popBackStack()  // Quay lại sau khi apply (tùy chọn)
    }


    //xử lý môi
    private fun createLipMaskAndRegion(
        bitmap: android.graphics.Bitmap,
        landmarks: List<NormalizedLandmark>
    ): Triple<Mat, Mat, Mat> {
        val inputMat = bitmap.toMat()
        val width = bitmap.width.toDouble()
        val height = bitmap.height.toDouble()

        val outerLipIndices = listOf(
            61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291,
            409, 270, 269, 267, 0, 37, 39, 40, 185
        )
        val innerLipIndices = listOf(
            78, 95, 88, 178, 87, 14, 317, 402, 318, 324,
            308, 415, 310, 311, 312, 13, 82
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

    private fun android.graphics.Bitmap.applyLipColorFromLandmarks(
        landmarks: List<NormalizedLandmark>,
        color: Scalar,
        intensity: Float
    ): android.graphics.Bitmap {
        val (mask, region, base) = createLipMaskAndRegion(this, landmarks)
        val overlay = Mat(region.size(), region.type(), color)
        val blended = Mat()
        Core.addWeighted(region, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)
        blended.copyTo(base, mask)
        return base.toBitmap()
    }
}