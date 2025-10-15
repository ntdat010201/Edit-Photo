package com.example.editphoto.ui.activities

import android.net.Uri
import android.os.Bundle
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityEditImageBinding
import androidx.core.net.toUri

class EditImageActivity : BaseActivity() {
    private lateinit var binding: ActivityEditImageBinding
    private var uri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            uri = imageUriString.toUri()
        }
    }


    private fun initView() {
        binding.imgPreview.setImageURI(uri)
    }

    private fun initListener() {

    }
}