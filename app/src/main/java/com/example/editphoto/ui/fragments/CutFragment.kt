package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.canhub.cropper.CropImageView
import com.example.editphoto.R
import com.example.editphoto.databinding.FragmentCutBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.viewmodel.EditImageViewModel

class CutFragment : Fragment(), OnApplyListener {
    private lateinit var binding: FragmentCutBinding
    private var cropView: com.canhub.cropper.CropImageView? = null
    private lateinit var parentActivity: EditImageActivity
    private var beforeCropBitmap: Bitmap? = null

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
        cropView = parentActivity.binding.cropImageView

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        beforeCropBitmap = parentActivity.viewModel.previewBitmap.value
            ?: parentActivity.viewModel.editedBitmap.value
                    ?: parentActivity.viewModel.originalBitmap.value

        parentActivity.enableCropMode()
    }

    private fun initView() {

    }

    private fun initListener() {
        selectedRadio()
    }

    private fun selectedRadio() {
        val radioItems = listOf(
            Triple(binding.ivIconRadio11, binding.tvNameRadio11) { cropView?.setAspectRatio(1, 1)
                cropView?.setFixedAspectRatio(true) },
            Triple(binding.ivIconRadio34, binding.tvNameRadio34) { cropView?.setAspectRatio(3, 4)
                cropView?.setFixedAspectRatio(true)},
            Triple(binding.ivIconRadio43, binding.tvNameRadio43) { cropView?.setAspectRatio(4, 3)
                cropView?.setFixedAspectRatio(true)},
            Triple(binding.ivIconRadio916, binding.tvNameRadio916) { cropView?.setAspectRatio(9, 16)
                cropView?.setFixedAspectRatio(true)},
            Triple(binding.ivIconRadio169, binding.tvNameRadio169) { cropView?.setAspectRatio(16, 9)
                cropView?.setFixedAspectRatio(true)},
            Triple(binding.ivIconRadio75, binding.tvNameRadio75) { cropView?.setAspectRatio(7, 5)
                cropView?.setFixedAspectRatio(true)},
        )

        // Mặc định chọn 1:1
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

    override fun onApply() {
        val cropped = cropView?.croppedImage ?: return
        parentActivity.viewModel.setPreview(cropped)
        parentActivity.viewModel.commitPreview()

        cropView?.setImageBitmap(cropped)

            parentActivity.updateImagePreserveZoom(cropped)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity.disableCropMode()
        cropView = null
    }
}
