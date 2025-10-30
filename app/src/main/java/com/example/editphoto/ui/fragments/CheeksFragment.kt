package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentCheeksBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.viewmodel.EditImageViewModel

class CheeksFragment : Fragment() {

    private lateinit var binding: FragmentCheeksBinding
    private lateinit var viewModel: EditImageViewModel
    private lateinit var parentActivity: EditImageActivity

    private var beforeEditBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCheeksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentActivity = requireActivity() as EditImageActivity
        viewModel = parentActivity.viewModel
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

    }

    private fun initListener() {
        binding.colorless.setOnClickListener {
        }
        binding.colorBlush1.setOnClickListener {
        }
        binding.colorBlush2.setOnClickListener {
        }
        binding.colorBlush3.setOnClickListener {
        }
        binding.colorBlush4.setOnClickListener {
        }
        binding.colorBlush5.setOnClickListener {
        }
        binding.colorBlush6.setOnClickListener {
        }
    }

}