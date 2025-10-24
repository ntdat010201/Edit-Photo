package com.example.editphoto.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.editphoto.utils.Const.KEY_DENY_CAMERA
import com.example.editphoto.utils.Const.KEY_DENY_GALLERY

class PermissionManager(private val activity: Activity) {

    private val prefs: SharedPreferences =
        activity.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)

    private var onGrantedAction: (() -> Unit)? = null

    fun requestCameraPermission(
        launcher: ActivityResultLauncher<Array<String>>,
        onGranted: () -> Unit
    ) {
        onGrantedAction = onGranted
        val neededPermissions = arrayOf(Manifest.permission.CAMERA)
        handlePermissionRequest(
            permissions = neededPermissions,
            launcher = launcher,
            isCamera = true
        )
    }

    fun requestGalleryPermission(
        launcher: ActivityResultLauncher<Array<String>>,
        onGranted: () -> Unit
    ) {
        onGrantedAction = onGranted

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED // Android 14+
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        handlePermissionRequest(
            permissions = permissions,
            launcher = launcher,
            isCamera = false
        )
    }


    private fun handlePermissionRequest(
        permissions: Array<String>,
        launcher: ActivityResultLauncher<Array<String>>,
        isCamera: Boolean
    ) {
        if (checkPermissions(permissions)) {
            onGrantedAction?.invoke()
            return
        }

        val denyCount = getDenyCount(isCamera)

        if (denyCount >= 2) {
            showSettingsDialog()
        } else {
            launcher.launch(permissions)
        }
    }

    fun handleResult(permissions: Map<String, Boolean>) {
        var allGranted = true

        permissions.forEach { (permission, granted) ->
            if (!granted) {
                allGranted = false
                if (permission == Manifest.permission.CAMERA) {
                    increaseDenyCount(true)
                } else {
                    increaseDenyCount(false)
                }
            }
        }

        if (allGranted) {
            onGrantedAction?.invoke()
        }
    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun getDenyCount(isCamera: Boolean): Int {
        return prefs.getInt(if (isCamera) KEY_DENY_CAMERA else KEY_DENY_GALLERY, 0)
    }

    private fun increaseDenyCount(isCamera: Boolean) {
        val key = if (isCamera) KEY_DENY_CAMERA else KEY_DENY_GALLERY
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(activity).apply {
            setTitle("Yêu cầu quyền")
            setMessage("Bạn đã từ chối quyền nhiều lần. Vui lòng bật lại trong Cài đặt để sử dụng tính năng.")
            setPositiveButton("Đi đến Cài đặt") { _, _ ->
                openSettings()
            }
            setNegativeButton("Hủy", null)
            show()
        }
    }

    private fun openSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        activity.startActivity(intent)
    }
}
