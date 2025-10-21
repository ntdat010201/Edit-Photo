package com.example.editphoto.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.editphoto.R
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityCameraBinding
import com.example.editphoto.databinding.ActivityGalleryBinding

class GalleryActivity : BaseActivity() {
    private lateinit var binding : ActivityGalleryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
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