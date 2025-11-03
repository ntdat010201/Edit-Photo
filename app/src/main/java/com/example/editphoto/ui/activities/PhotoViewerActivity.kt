package com.example.editphoto.ui.activities

import android.os.Bundle
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityPhotoViewerBinding

class PhotoViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityPhotoViewerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {

    }

    private fun initView() {

    }

    private fun initListener() {

    }
}