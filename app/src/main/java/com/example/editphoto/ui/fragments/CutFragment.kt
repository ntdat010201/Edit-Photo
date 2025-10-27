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
import androidx.core.graphics.toColorInt

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
        selectedRadio()

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

    private fun selectedRadio() {
        val radioItems = listOf(
            Triple(binding.ivIconRadio11, binding.tvNameRadio11) { cropView?.setAspectRatio(1, 1) },
            Triple(binding.ivIconRadio34, binding.tvNameRadio34) { cropView?.setAspectRatio(3, 4) },
            Triple(binding.ivIconRadio43, binding.tvNameRadio43) { cropView?.setAspectRatio(4, 3) },
            Triple(binding.ivIconRadio916, binding.tvNameRadio916) { cropView?.setAspectRatio(9, 16) },
            Triple(binding.ivIconRadio169, binding.tvNameRadio169) { cropView?.setAspectRatio(16, 9) },
            Triple(binding.ivIconRadio75, binding.tvNameRadio75) { cropView?.setAspectRatio(7, 5) },
        )

        val (defaultIcon, defaultText, defaultAction) = radioItems.first()
        defaultIcon.imageTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        defaultText.setTextColor(android.graphics.Color.BLACK)
        defaultAction()
        cropView?.setFixedAspectRatio(true)

        radioItems.forEach { (icon, text, action) ->
            val clickListener = View.OnClickListener {
                radioItems.forEach { (iv, tv, _) ->
                    iv.imageTintList =
                        android.content.res.ColorStateList.valueOf("#FFBBB4CE".toColorInt())
                    tv.setTextColor("#FFBBB4CE".toColorInt())
                }
                icon.imageTintList =
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
                text.setTextColor(android.graphics.Color.BLACK)

                action()
                cropView?.setFixedAspectRatio(true)
            }

            icon.setOnClickListener(clickListener)
            text.setOnClickListener(clickListener)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity?.disableCropMode()
        cropView = null
        parentActivity = null
    }
}
