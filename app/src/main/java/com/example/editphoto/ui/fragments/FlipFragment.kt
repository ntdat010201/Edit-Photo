package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentFlipBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener

class FlipFragment : Fragment(), OnApplyListener {

    private lateinit var binding: FragmentFlipBinding
    private lateinit var parentActivity: EditImageActivity

    private var beforeFlipBitmap: Bitmap? = null
    private var tempFlippedBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null

    private var hasApplied = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFlipBinding.inflate(inflater, container, false)
        parentActivity = requireActivity() as EditImageActivity
        initData()
        initView()
        initListener()
        return binding.root
    }

    private fun initData() {
        beforeFlipBitmap = parentActivity.viewModel.previewBitmap.value
            ?: parentActivity.viewModel.editedBitmap.value
                    ?: parentActivity.viewModel.originalBitmap.value

        currentBitmap = beforeFlipBitmap
    }

    private fun initView() {
        parentActivity.binding.imgPreview.setImageBitmap(currentBitmap)
    }

    private fun initListener() {
        binding.constraintHorizontal.setOnClickListener {
            currentBitmap?.let {
                val flipped = flipBitmap(it, horizontal = true)
                tempFlippedBitmap = flipped
                currentBitmap = flipped
                parentActivity.binding.imgPreview.setImageBitmap(flipped)
            }
        }

        binding.constraintVertical.setOnClickListener {
            currentBitmap?.let {
                val flipped = flipBitmap(it, horizontal = false)
                tempFlippedBitmap = flipped
                currentBitmap = flipped
                parentActivity.binding.imgPreview.setImageBitmap(flipped)
            }
        }
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

    override fun onApply() {
        val bitmap = tempFlippedBitmap ?: currentBitmap ?: return

        parentActivity.viewModel.setPreview(bitmap)
        parentActivity.viewModel.commitPreview()

        hasApplied = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!hasApplied) {
            beforeFlipBitmap?.let {
                parentActivity.binding.imgPreview.setImageBitmap(it)
            }
        }
    }
}
