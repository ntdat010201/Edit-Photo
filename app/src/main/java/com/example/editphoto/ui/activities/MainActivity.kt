package com.example.editphoto.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityMainBinding
import com.example.editphoto.permission.PermissionManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

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
            permissionManager.requestGalleryPermission(permissionLauncher) {
                startActivity(Intent(this, GalleryActivity::class.java))
            }
        }

        binding.constraintHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
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
