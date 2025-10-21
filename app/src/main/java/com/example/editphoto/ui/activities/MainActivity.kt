package com.example.editphoto.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityMainBinding
import com.example.editphoto.permission.PermissionManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var selectedUri: Uri? = null
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedUri = it
                openEditActivity(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initListener()
    }

    private fun initData() {
        permissionManager = PermissionManager(this)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissionManager.handleResult(permissions)
            }
    }

    private fun initListener() {

        binding.constraintCamera.setOnClickListener {
            permissionManager.requestCameraPermission(permissionLauncher) {
                openCamera()
            }
        }

        binding.constraintGallery.setOnClickListener {
            pickImageFromGallery()
        }
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }



    private fun openEditActivity(uri: Uri) {
        val intent = Intent(this, EditImageActivity::class.java)
        intent.putExtra("image_uri", uri.toString())
        startActivity(intent)
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }
}
