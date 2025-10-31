package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentEyebrowBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.viewmodel.EditImageViewModel

class EyebrowFragment : Fragment() {

    private lateinit var binding: FragmentEyebrowBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var act: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEyebrowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        act = requireActivity() as EditImageActivity
        viewModel = act.viewModel

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        beforeEditBitmap = act.viewModel.previewBitmap.value
            ?: act.viewModel.editedBitmap.value
                    ?: act.viewModel.originalBitmap.value
    }

    private fun initView() {
    }

    private fun initListener() {
        binding.eyebrowColorless.setOnClickListener {
        }
        binding.eyebrow1.setOnClickListener {
        }
        binding.eyebrow2.setOnClickListener {
        }
        binding.eyebrow3.setOnClickListener {
        }
        binding.eyebrow4.setOnClickListener {
        }
        binding.eyebrow5.setOnClickListener {
        }
        binding.eyebrow6.setOnClickListener {
        }
    }
}