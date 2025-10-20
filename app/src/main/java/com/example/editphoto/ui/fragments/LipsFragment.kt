package com.example.editphoto.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.editphoto.databinding.FragmentLipsBinding
import com.example.editphoto.utils.applyLipColorFromLandmarks
import com.example.editphoto.utils.createLipMaskAndRegion
import com.example.editphoto.utils.quickApplyLipColor
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.Scalar

class LipsFragment : Fragment() {

    private lateinit var binding: FragmentLipsBinding
    private val viewModel: EditImageViewModel by activityViewModels()

    private var selectedColor: Scalar = Scalar(0.0, 0.0, 255.0) // Mặc định đỏ
    private var intensity: Float = 0.5f

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
        setupListeners()
    }

    private fun setupListeners() {
        // SeekBar điều chỉnh độ đậm
        binding.lipsIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                intensity = progress / 100f
                scheduleApply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Chọn màu môi
        binding.colorRed.setOnClickListener {
            selectedColor = Scalar(0.0, 0.0, 255.0); scheduleApply()
        }
        binding.colorGreen.setOnClickListener {
            selectedColor = Scalar(0.0, 255.0, 0.0); scheduleApply()
        }
        binding.colorPurple.setOnClickListener {
            selectedColor = Scalar(128.0, 0.0, 128.0); scheduleApply()
        }
        binding.colorYellow.setOnClickListener {
            selectedColor = Scalar(0.0, 255.0, 255.0); scheduleApply()
        }

        // Áp dụng cuối cùng (lưu)
        binding.btnApply.setOnClickListener {
            applyFinalLipColor()
        }

        // Reset về ảnh gốc
        binding.btnReset.setOnClickListener {
            viewModel.resetToOriginal()
            lipMask = null
            lipRegion = null
        }

        // Back
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun scheduleApply() {
        applyJob?.cancel()
        applyJob = lifecycleScope.launch {
            delay(20) // realtime mượt
            updateLipPreview()
        }
    }

    private fun prepareBaseIfNeeded() {
        if (baseMat != null && lipMask != null && lipRegion != null) return

        val bitmap = viewModel.editedBitmap.value ?: return
        val landmarker = viewModel.getFaceLandmarker() ?: return

        lifecycleScope.launch(Dispatchers.Default) {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detect(mpImage)
            if (result.faceLandmarks().isNotEmpty()) {
                cachedLandmarks = result.faceLandmarks()[0]
                val (mask, region, base) = bitmap.createLipMaskAndRegion(cachedLandmarks!!)
                lipMask = mask
                lipRegion = region
                baseMat = base
            }
        }
    }

    private fun updateLipPreview() {
        lifecycleScope.launch(Dispatchers.Default) {
            if (lipMask == null || lipRegion == null || baseMat == null) {
                prepareBaseIfNeeded()
                delay(100)
                if (lipMask == null) return@launch
            }

            val blended = lipRegion!!.quickApplyLipColor(selectedColor, intensity)
            val preview = baseMat!!.clone()
            blended.copyTo(preview, lipMask)

            val bitmapOut = viewModel.matToBitmap(preview)
            withContext(Dispatchers.Main) {
                viewModel.updateBitmap(bitmapOut)
            }
        }
    }

    private fun applyFinalLipColor() {
        lifecycleScope.launch(Dispatchers.Default) {
            val bitmap = viewModel.editedBitmap.value ?: return@launch
            val result = bitmap.applyLipColorFromLandmarks(
                cachedLandmarks ?: return@launch,
                selectedColor,
                intensity
            )
            withContext(Dispatchers.Main) {
                viewModel.updateBitmap(result)
            }
        }
    }
}
