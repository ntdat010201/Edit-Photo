package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.editphoto.viewmodel.EditImageViewModel
import com.example.editphoto.databinding.FragmentLipsBinding
import com.example.editphoto.utils.createMaskFromContours
import com.example.editphoto.utils.createMaskFromRect
import com.example.editphoto.utils.applyEffectWithMask
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceContour
import org.opencv.core.Mat
import android.util.Log
import kotlin.math.max
import kotlin.math.min

class LipsFragment : Fragment() {

    private lateinit var binding: FragmentLipsBinding
    private val viewModel: EditImageViewModel by activityViewModels()
    private var lipMask: Mat? = null

    // Bảng màu son môi chân thực
    private val lipColors = listOf(
        Color.rgb(200, 40, 40),   // Đỏ hồng
        Color.rgb(255, 182, 193), // Hồng phấn
        Color.rgb(255, 99, 71),   // Cam san hô
        Color.rgb(188, 143, 143), // Nude
        Color.rgb(255, 160, 122), // Hồng đào
        Color.rgb(139, 0, 139)    // Đỏ berry
    )
    private var selectedColor = lipColors[0] // Mặc định là đỏ hồng
    private var intensity = 0.5f // Alpha mặc định

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mặc định detect môi khi fragment mở
        viewModel.getOriginalBitmap()?.let { bitmap ->
            detectLips(bitmap)
            applyLipEffect(bitmap)
        } ?: run {
            Toast.makeText(requireContext(), "Không có ảnh để xử lý", Toast.LENGTH_SHORT).show()
        }

        initView()
        setupSeekBar()
        initListener()
    }

    // Detect môi bằng ML Kit
    private fun detectLips(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                Log.d("LipsFragment", "Found ${faces.size} faces")
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val upperLipTopContours = face.getContour(FaceContour.UPPER_LIP_TOP)?.points
                    val upperLipBottomContours = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points
                    val lowerLipTopContours = face.getContour(FaceContour.LOWER_LIP_TOP)?.points
                    val lowerLipBottomContours = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points

                    val lipContours = listOfNotNull(
                        upperLipTopContours,
                        upperLipBottomContours,
                        lowerLipTopContours,
                        lowerLipBottomContours
                    ).flatten()

                    if (lipContours.isNotEmpty()) {
                        Log.d("LipsFragment", "Found ${lipContours.size} lip contour points")
                        lipMask = bitmap.createMaskFromContours(lipContours)
                    } else {
                        val bottomLip = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                        val leftLip = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                        val rightLip = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
                        if (bottomLip != null && leftLip != null && rightLip != null) {
                            val faceBounds = face.boundingBox
                            val padding = (faceBounds.width() * 0.1f).toInt()
                            val left = leftLip.x - padding
                            val right = rightLip.x + padding
                            val top = bottomLip.y - (faceBounds.height() * 0.3f)
                            val bottom = bottomLip.y + (faceBounds.height() * 0.1f)
                            Log.d("LipsFragment", "Fallback to landmarks: left=$left, top=$top, right=$right, bottom=$bottom")
                            lipMask = bitmap.createMaskFromRect(left, top, right, bottom)
                        } else {
                            Log.e("LipsFragment", "Missing lip landmarks")
                        }
                    }
                } else {
                    Log.e("LipsFragment", "No faces detected")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LipsFragment", "Face detection failed: ${e.message}")
            }
    }

    // Áp màu môi
    private fun applyLipEffect(bitmap: Bitmap) {
        lipMask?.let { mask ->
            val resultBitmap = bitmap.applyEffectWithMask(mask, selectedColor, intensity)
            viewModel.updateEditedImage(resultBitmap)
        } ?: run {
            Log.e("LipsFragment", "Lip mask is null")
            Toast.makeText(requireContext(), "Không thể áp màu môi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initView() {
        binding.colorRed.setBackgroundColor(lipColors[0])
        binding.colorRed.setOnClickListener {
            selectedColor = lipColors[0]
            viewModel.getOriginalBitmap()?.let { applyLipEffect(it) }
        }
        binding.colorGreen.setBackgroundColor(lipColors[1])
        binding.colorGreen.setOnClickListener {
            selectedColor = lipColors[1]
            viewModel.getOriginalBitmap()?.let { applyLipEffect(it) }
        }
        binding.colorPurple.setBackgroundColor(lipColors[2])
        binding.colorPurple.setOnClickListener {
            selectedColor = lipColors[2]
            viewModel.getOriginalBitmap()?.let { applyLipEffect(it) }
        }
        binding.colorYellow.setBackgroundColor(lipColors[3])
        binding.colorYellow.setOnClickListener {
            selectedColor = lipColors[3]
            viewModel.getOriginalBitmap()?.let { applyLipEffect(it) }
        }
    }

    private fun setupSeekBar() {
        binding.lipsIntensity.max = 100
        binding.lipsIntensity.progress = 50 // Mặc định alpha 0.5
        binding.lipsIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                intensity = progress / 100f
                viewModel.getOriginalBitmap()?.let { applyLipEffect(it) }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initListener() {
        binding.btnApply.setOnClickListener {
            viewModel.getOriginalBitmap()?.let { applyLipEffect(it) }
            Toast.makeText(requireContext(), "Đã áp màu môi", Toast.LENGTH_SHORT).show()
        }
        binding.btnReset.setOnClickListener {
            viewModel.getOriginalBitmap()?.let { viewModel.updateEditedImage(it) }
        }
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lipMask?.release()
    }
}