package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentTurnBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.inter.OnApplyListener

class TurnFragment : Fragment(), OnApplyListener {

    private lateinit var binding: FragmentTurnBinding
    private lateinit var parentActivity: EditImageActivity

    private var beforeRotateBitmap: Bitmap? = null
    private var tempRotatedBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null

    private var totalRotation = 0f
    private var hasApplied = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTurnBinding.inflate(inflater, container, false)
        parentActivity = requireActivity() as EditImageActivity

        initData()
        initView()
        initRuler()
        initListener()

        return binding.root
    }

    private fun initData() {
        beforeRotateBitmap = parentActivity.viewModel.previewBitmap.value
            ?: parentActivity.viewModel.editedBitmap.value
                    ?: parentActivity.viewModel.originalBitmap.value

        currentBitmap = beforeRotateBitmap
        totalRotation = 0f
    }

    private fun initView() {
        parentActivity.binding.imgPreview.setImageBitmap(currentBitmap)
    }

    private fun initRuler() {
        binding.rotationRuler.onDegreeChange = { degree ->
            currentBitmap?.let {
                val rotated = rotateBitmap(beforeRotateBitmap ?: it, totalRotation + degree)
                tempRotatedBitmap = rotated
                parentActivity.binding.imgPreview.setImageBitmap(rotated)
            }
        }
    }

    private fun initListener() {
        binding.turn90.setOnClickListener {
            currentBitmap?.let {
                totalRotation = (totalRotation + 90f) % 360f

                binding.rotationRuler.setDegree(0f)

                val rotated = rotateBitmap(beforeRotateBitmap ?: it, totalRotation)
                tempRotatedBitmap = rotated
                currentBitmap = rotated
                parentActivity.binding.imgPreview.setImageBitmap(rotated)
            }
        }
    }

    override fun onApply() {
        val vm = parentActivity.viewModel
        val bitmap = tempRotatedBitmap ?: currentBitmap ?: return
        vm.setPreview(bitmap)
        hasApplied = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!hasApplied) {
            beforeRotateBitmap?.let {
                parentActivity.binding.imgPreview.setImageBitmap(it)
            }
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees, source.width / 2f, source.height / 2f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

}
