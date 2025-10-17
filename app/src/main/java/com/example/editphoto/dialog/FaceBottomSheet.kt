package com.example.editphoto.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.editphoto.base.BaseFeatureBottomSheet
import com.example.editphoto.databinding.BottomsheetFaceBinding

class FaceBottomSheet : BaseFeatureBottomSheet() {
    private lateinit var binding: BottomsheetFaceBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomsheetFaceBinding.inflate(inflater, container, false)
        initData()
        initView()
        initListener()
        return binding.root
    }

    private fun initData() {

    }

    private fun initView() {

    }

    private fun initListener() {
        binding.lipsTools.setOnClickListener {

        }

        binding.eyesTools.setOnClickListener {

        }

        binding.cheeksTools.setOnClickListener {

        }

        binding.whiteTools.setOnClickListener {

        }

        binding.transparentTools.setOnClickListener {

        }
    }


}