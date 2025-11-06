package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
    private var blurMaskView: BlurMaskView? = null

    private var hasApplied = false
    private var isDirty = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        // Tạo BlurMaskView
        blurMaskView = BlurMaskView(requireContext()).apply {
            setEraseMode(false)
            beforeEditBitmap?.let { base ->
                attachTo(
                    parentActivity.binding.imgPreview,
                    base,
                    blurRadius = 80
                ) { out ->
                    parentActivity.updateImagePreserveZoom(out)
                }
            }
            onAppliedChange = { applied ->
                if (applied) {
                    this@BlurFragment.isDirty = true
                }
            }
        }

        // Thêm BlurMaskView
        parentActivity.binding.framePreviewContainer.addView(
            blurMaskView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        view?.post {
            parentActivity.attachSeekBar(this)
        }

        parentActivity.binding.imgPreview.setScale(1f, false)
    }

    private fun initView() {
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
        parentActivity.updateImagePreserveZoom(currentBitmap)
        hasApplied = true
        isDirty = false
    }

    override fun onPause() {
        super.onPause()
        blurMaskView?.apply {
            visibility = View.GONE
            isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        blurMaskView?.apply {
            visibility = View.VISIBLE
            isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        blurMaskView?.let {
            parentActivity.binding.framePreviewContainer.removeView(it)
            it.detach()
        }
        blurMaskView = null

        parentActivity.detachSeekBar()

        if (!hasApplied) {
            beforeEditBitmap?.let {
                parentActivity.updateImagePreserveZoom(it)
                parentActivity.viewModel.setPreview(null)
            }
        }

        parentActivity.binding.imgPreview.apply {
            attacher.update()
        }
    }

    override fun onIntensityChanged(intensity: Float) {
        blurMaskView?.setBlurIntensity(intensity)
    }

    override fun getDefaultIntensity(): Float = 0.5f

    override fun hasUnsavedChanges(): Boolean = isDirty && !hasApplied

    override fun revertUnsavedChanges() {
        if (!hasApplied) {
            blurMaskView?.detach()
            parentActivity.updateImagePreserveZoom(beforeEditBitmap!!)
            parentActivity.viewModel.setPreview(null)
            isDirty = false
        }
    }
}
