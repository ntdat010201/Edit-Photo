package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentBlurBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener

class BlurFragment : Fragment(), OnApplyListener {
    private lateinit var binding: FragmentBlurBinding
    private lateinit var parentActivity: EditImageActivity
    private var baseBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlurBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentActivity = requireActivity() as EditImageActivity

        initData()
        initListener()
    }

    private fun initData() {

        baseBitmap = parentActivity.viewModel.previewBitmap.value
            ?: parentActivity.viewModel.editedBitmap.value
                    ?: parentActivity.viewModel.originalBitmap.value

        baseBitmap?.let {
            parentActivity.binding.blurDrawView.setImage(it)
        }

        parentActivity.enableBlurMode()
    }

    private fun initListener() {
        binding.constraintBlur.setOnClickListener {
            parentActivity.enableBlurMode()
        }

        binding.constraintErase.setOnClickListener {
            parentActivity.enableEraseMode()
        }
    }

    override fun onApply() {
        val blurredBitmap = parentActivity.binding.blurDrawView.getFinalBitmap()
        if (blurredBitmap != null) {
            parentActivity.viewModel.setPreview(blurredBitmap)
            parentActivity.viewModel.commitPreview()
            parentActivity.binding.imgPreview.setImageBitmap(blurredBitmap)
        }

        // Ẩn blur view sau khi apply
        parentActivity.disableBlurFeature()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity.disableBlurFeature()
    }
}
