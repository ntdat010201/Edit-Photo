package com.example.editphoto.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityPhotoViewerBinding
import com.example.editphoto.utils.extent.showImageGlide

class PhotoViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityPhotoViewerBinding
    private var uri: Uri? = null
    private var toggle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        uri = intent.getStringExtra("Uri_Item")?.toUri()
    }

    private fun initView() {
        showImageGlide(this, uri!!, binding.imgViewer)
    }

    private fun initListener() {
        binding.constraintEdit.setOnClickListener {
            var intent = Intent(this, EditImageActivity::class.java)
            intent.putExtra("image_uri", uri.toString())
            startActivity(intent)
        }

        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.imgViewer.setOnClickListener {
            toggle = !toggle
            if (toggle) {
                binding.constraintTool.visibility = View.GONE
                binding.constraintEdit.visibility = View.GONE
            } else {
                binding.constraintTool.visibility = View.VISIBLE
                binding.constraintEdit.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        hideSystemUiBar(window)
    }
}