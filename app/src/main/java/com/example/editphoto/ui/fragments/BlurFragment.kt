package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentBlurBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.view.BlurMaskView

class BlurFragment : Fragment(), OnApplyListener {
    private lateinit var binding: FragmentBlurBinding
    private lateinit var parentActivity: EditImageActivity
    private var beforeEditBitmap: Bitmap? = null

    private var blurMaskView: BlurMaskView? = null

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
        initView()
        initListener()
    }

    private fun initData() {
        beforeEditBitmap = parentActivity.viewModel.previewBitmap.value
            ?: parentActivity.viewModel.editedBitmap.value
                    ?: parentActivity.viewModel.originalBitmap.value


    }

    private fun initView() {

        blurMaskView = BlurMaskView(requireContext()).apply {
            setEraseMode(false)
            beforeEditBitmap?.let {
                attachTo(
                    parentActivity.binding.imgPreview,
                    it,
                    blurRadius = 12
                ) { out ->
                    parentActivity.binding.imgPreview.setImageBitmap(out)
                }
            }
        }
        // UI highlight mặc định
        binding.constraintBlur.alpha = 1f
        binding.constraintErase.alpha = 0.6f


    }

    private fun initListener() {
        binding.constraintBlur.setOnClickListener {
            blurMaskView?.setEraseMode(false)
            binding.constraintBlur.alpha = 1f
            binding.constraintErase.alpha = 0.6f
        }

        binding.constraintErase.setOnClickListener {
            blurMaskView?.setEraseMode(true)
            binding.constraintBlur.alpha = 0.6f
            binding.constraintErase.alpha = 1f
        }
    }

    override fun onApply() {
        val currentBitmap = parentActivity.binding.imgPreview.drawable?.toBitmap() ?: return
        parentActivity.viewModel.setPreview(currentBitmap)
        parentActivity.viewModel.commitPreview()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity.binding.imgPreview.setImageBitmap(beforeEditBitmap)
        blurMaskView?.detach()
        blurMaskView = null
    }


}