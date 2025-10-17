package com.example.editphoto.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.editphoto.viewmodel.EditImageViewModel
import com.example.editphoto.databinding.FragmentLipsBinding

class LipsFragment : Fragment() {

    private lateinit var binding: FragmentLipsBinding
    private val viewModel: EditImageViewModel by activityViewModels()

    private var selectedColor = Color.RED
    private var intensity = 0.5f // 0–1f

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

        // Mặc định detect môi khi fragment mở và ảnh gốc đã load
        viewModel.editedImage.value?.let { bitmap ->
            viewModel.detectLips(bitmap)
        }

        setupColorPickers()
        setupSeekBar()
        setupButtons()
    }

    private fun setupColorPickers() {
        binding.colorRed.setOnClickListener {
            selectedColor = Color.RED
        }
        binding.colorGreen.setOnClickListener {
            selectedColor = Color.GREEN
        }
        binding.colorPurple.setOnClickListener {
            selectedColor = Color.parseColor("#800080")
        }
        binding.colorYellow.setOnClickListener {
            selectedColor = Color.YELLOW
        }
    }

    private fun setupSeekBar() {
        binding.lipsIntensity.max = 100
        binding.lipsIntensity.progress = 50
        binding.lipsIntensity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                intensity = progress / 100f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        binding.btnApply.setOnClickListener {
            viewModel.applyLipColor(selectedColor, intensity)
        }
        binding.btnReset.setOnClickListener {
            viewModel.editedImage.value?.let { bitmap ->
                viewModel.setOriginalImage(bitmap)
            }
        }
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
}
