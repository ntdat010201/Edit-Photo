package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentTurnBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.handleBackPressedCommon
import com.example.editphoto.utils.handlePhysicalBackPress

class TurnFragment : Fragment() {

    private lateinit var binding: FragmentTurnBinding
    private var beforeRotateBitmap: Bitmap? = null

    private var hasPreview = false
    private var hasApplied = false
    private var currentRotation = 0f
    private var totalRotation = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTurnBinding.inflate(inflater, container, false)
        initSeekBar()
        initListener()
        /*handlePhysicalBackPress()*/
        return binding.root
    }

    private fun initSeekBar() {
        binding.ruler.max = 900
        binding.ruler.progress = 450
    }

    private fun initListener() {
        val act = requireActivity() as EditImageActivity
        val vm = act.viewModel

        beforeRotateBitmap = vm.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)

        binding.turn90.setOnClickListener {
            totalRotation += 90f
            val bitmap = vm.editedBitmap.value ?: getBitmapFromImageView(act)
            bitmap?.let {
                val rotated = rotateBitmap(it, totalRotation + currentRotation)
                vm.setPreview(rotated)
                act.binding.imgPreview.setImageBitmap(rotated)
                hasPreview = true
                hasApplied = false
            }
        }

        binding.ruler.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentRotation = (progress - 450) / 10f
                    val bitmap = vm.editedBitmap.value ?: getBitmapFromImageView(act)
                    bitmap?.let {
                        val rotated = rotateBitmap(it, totalRotation + currentRotation)
                        vm.setPreview(rotated)
                        act.binding.imgPreview.setImageBitmap(rotated)
                        hasPreview = true
                        hasApplied = false
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        binding.btnApply.setOnClickListener {
            vm.commitPreview()
            beforeRotateBitmap = vm.editedBitmap.value?.copy(Bitmap.Config.ARGB_8888, true)
            hasPreview = false
            hasApplied = true
            totalRotation += currentRotation
            currentRotation = 0f
            binding.ruler.progress = 450
            parentFragmentManager.popBackStack()
        }

        binding.btnReset.setOnClickListener {
            if (hasPreview) {
                beforeRotateBitmap?.let { originalBeforeRotate ->
                    vm.setPreview(null)
                    act.binding.imgPreview.setImageBitmap(originalBeforeRotate)
                }
                hasPreview = false
                totalRotation = 0f
                currentRotation = 0f
                binding.ruler.progress = 450
            }
        }

        binding.btnBack.setOnClickListener {
            val act = requireActivity() as EditImageActivity
            handleBackPressedCommon(act, hasApplied, beforeRotateBitmap)
        }

        handlePhysicalBackPress { act ->
            handleBackPressedCommon(act, hasApplied, beforeRotateBitmap)
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
            beforeRotateBitmap?.let { originalBeforeRotate ->
                act.binding.imgPreview.setImageBitmap(originalBeforeRotate)
            }
            vm.setPreview(null)
            totalRotation = 0f
            currentRotation = 0f
            binding.ruler.progress = 450
        }
        parentFragmentManager.popBackStack()
    }

    private fun getBitmapFromImageView(act: EditImageActivity): Bitmap? {
        val drawable = act.binding.imgPreview.drawable
        return if (drawable is BitmapDrawable) drawable.bitmap else null
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees, source.width / 2f, source.height / 2f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}