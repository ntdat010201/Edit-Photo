package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentFlipBinding
import com.example.editphoto.ui.activities.EditImageActivity

class FlipFragment : Fragment() {

    private lateinit var binding: FragmentFlipBinding
    private var beforeFlipBitmap: Bitmap? = null
    private var hasPreview = false
    private var hasApplied = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFlipBinding.inflate(inflater, container, false)
        initListener()
        handlePhysicalBackPress()
        return binding.root
    }

    private fun initListener() {
        val act = requireActivity() as EditImageActivity
        val vm = act.viewModel

        beforeFlipBitmap = vm.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)

        binding.constraintHorizontal.setOnClickListener {
            val bitmap =
                vm.previewBitmap.value ?: vm.editedBitmap.value ?: getBitmapFromImageView(act)
            bitmap?.let {
                val flipped = flipBitmap(it, horizontal = true)
                vm.setPreview(flipped)
                act.binding.imgPreview.setImageBitmap(flipped)
                hasPreview = true
                hasApplied = false
            }
        }

        binding.constraintVertical.setOnClickListener {
            val bitmap =
                vm.previewBitmap.value ?: vm.editedBitmap.value ?: getBitmapFromImageView(act)
            bitmap?.let {
                val flipped = flipBitmap(it, horizontal = false)
                vm.setPreview(flipped)
                act.binding.imgPreview.setImageBitmap(flipped)
                hasPreview = true
                hasApplied = false
            }
        }

        binding.btnApply.setOnClickListener {
            vm.commitPreview()
            beforeFlipBitmap = vm.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
            hasPreview = false
            hasApplied = true
            parentFragmentManager.popBackStack()
        }

        binding.btnReset.setOnClickListener {
            if (hasPreview) {
                beforeFlipBitmap?.let { originalBeforeFlip ->
                    vm.setPreview(null)
                    act.binding.imgPreview.setImageBitmap(originalBeforeFlip)
                }
                hasPreview = false
            }
        }

        binding.btnBack.setOnClickListener {
            handleBackPressed(act)
        }
    }

    private fun handlePhysicalBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val act = requireActivity() as EditImageActivity
            handleBackPressed(act)
        }
    }

    private fun handleBackPressed(act: EditImageActivity) {
        val vm = act.viewModel
        if (!hasApplied && hasPreview) {
            beforeFlipBitmap?.let { originalBeforeFlip ->
                act.binding.imgPreview.setImageBitmap(originalBeforeFlip)
            }
            vm.setPreview(null)
        }
        parentFragmentManager.popBackStack()
    }

    private fun getBitmapFromImageView(act: EditImageActivity): Bitmap? {
        val drawable = act.binding.imgPreview.drawable
        return if (drawable is BitmapDrawable) drawable.bitmap else null
    }

    private fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                postScale(-1f, 1f, source.width / 2f, source.height / 2f)
            } else {
                postScale(1f, -1f, source.width / 2f, source.height / 2f)
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
