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
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.utils.inter.UnsavedChangesListener
import com.example.editphoto.view.BlurMaskView

class BlurFragment : Fragment(), OnApplyListener, SeekBarController, UnsavedChangesListener {
    private lateinit var binding: FragmentBlurBinding
    private lateinit var parentActivity: EditImageActivity
    private var beforeEditBitmap: Bitmap? = null
    internal var hasApplied = false
    private var isDirty = false

    private var blurMaskView: BlurMaskView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlurBinding.inflate(layoutInflater)
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

        blurMaskView = BlurMaskView(requireContext()).apply {
            setEraseMode(false)
            beforeEditBitmap?.let {
                attachTo(
                    parentActivity.binding.imgPreview,
                    it,
                    blurRadius = 80
                ) { out ->

                    parentActivity.binding.imgPreview.setImageBitmap(out)
                }
            }
            onAppliedChange = { applied ->
                if (applied) {
                    this@BlurFragment.isDirty = true
                }
            }
        }

        view?.post {
            parentActivity.attachSeekBar(this)
        }
    }

    private fun initView() {

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
        parentActivity.binding.imgPreview.setImageBitmap(currentBitmap)
        hasApplied = true
        isDirty = false

    }

    override fun onDestroyView() {
        super.onDestroyView()
        blurMaskView?.detach()
        blurMaskView = null
        parentActivity.detachSeekBar()
        if (!hasApplied) {
            beforeEditBitmap.let {
                parentActivity.binding.imgPreview.setImageBitmap(it)
                parentActivity.viewModel.setPreview(null)
            }
        }
    }

    // Nhận giá trị cường độ từ SeekBar và cập nhật độ mờ
    override fun onIntensityChanged(intensity: Float) {
        blurMaskView?.setBlurIntensity(intensity)
    }

    // Mặc định đặt SeekBar ở giữa
    override fun getDefaultIntensity(): Float = 0.5f

    // UnsavedChangesListener
    override fun hasUnsavedChanges(): Boolean = isDirty && !hasApplied

    override fun revertUnsavedChanges() {
        if (!hasApplied) {
            blurMaskView?.detach()
            parentActivity.binding.imgPreview.setImageBitmap(beforeEditBitmap)
            parentActivity.viewModel.setPreview(null)
            isDirty = false
        }
    }


}