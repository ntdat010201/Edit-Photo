package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentCutBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.handleBackPressedCommon
import com.example.editphoto.utils.handlePhysicalBackPress

class CutFragment : Fragment() {
    private lateinit var binding: FragmentCutBinding
    private var cropView: com.canhub.cropper.CropImageView? = null
    private var parentActivity: EditImageActivity? = null
    private var beforeCropBitmap: Bitmap? = null
    private var hasApplied = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentActivity = requireActivity() as EditImageActivity
        cropView = parentActivity?.binding?.cropImageView

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        beforeCropBitmap =
            parentActivity?.viewModel?.editedBitmap?.value?.copy(Bitmap.Config.ARGB_8888, true)
        // bật crop
        parentActivity?.enableCropMode()


    }

    private fun initView() {
        val bitmap = parentActivity?.viewModel?.editedBitmap?.value
        if (bitmap != null) {
            cropView?.setImageBitmap(bitmap)
        }
    }

    private fun initListener() {


        binding.radio11.setOnClickListener {
            cropView?.setAspectRatio(1, 1)
            cropView?.setFixedAspectRatio(true)
        }
        binding.radio34.setOnClickListener {
            cropView?.setAspectRatio(3, 4)
            cropView?.setFixedAspectRatio(true)
        }
        binding.radio43.setOnClickListener {
            cropView?.setAspectRatio(4, 3)
            cropView?.setFixedAspectRatio(true)
        }
        binding.radio916.setOnClickListener {
            cropView?.setAspectRatio(9, 16)
            cropView?.setFixedAspectRatio(true)
        }
        binding.radio169.setOnClickListener {
            cropView?.setAspectRatio(16, 9)
            cropView?.setFixedAspectRatio(true)
        }
        binding.radio75.setOnClickListener {
            cropView?.setAspectRatio(7, 5)
            cropView?.setFixedAspectRatio(true)
        }

        // Apply crop
        binding.btnApply.setOnClickListener {
            val cropped = cropView?.croppedImage
            if (cropped != null) {
                parentActivity?.viewModel?.updateBitmap(cropped)
                hasApplied = true
                beforeCropBitmap = cropped.copy(Bitmap.Config.ARGB_8888, true)
            }
            parentActivity?.disableCropMode()
            parentFragmentManager.popBackStack()
        }

        binding.btnBack.setOnClickListener {
            parentActivity?.let { act ->
                handleBackPressedCommon(
                    act,
                    hasApplied,
                    beforeCropBitmap
                ) {
                    cropView?.setImageBitmap(beforeCropBitmap)
                }
            }
            parentActivity?.disableCropMode()
        }


        handlePhysicalBackPress { act ->
            handleBackPressedCommon(
                act,
                hasApplied,
                beforeCropBitmap
            ) {
                cropView?.setImageBitmap(beforeCropBitmap)
            }
        }

        binding.btnReset.setOnClickListener {
            if (!hasApplied) {
                beforeCropBitmap?.let {
                    parentActivity?.viewModel?.updateBitmap(it)
                    cropView?.setImageBitmap(it)
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity?.disableCropMode()
        cropView = null
        parentActivity = null
    }
}
