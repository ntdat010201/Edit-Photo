package com.example.editphoto.viewmodel

import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val _imageUri = MutableLiveData<String?>()
    val imageUri: LiveData<String?> = _imageUri

    private val _cameraSelector = MutableLiveData<CameraSelector>()
    val cameraSelector: LiveData<CameraSelector> = _cameraSelector

    private val _zoomState = MutableLiveData<Float>()
    val zoomState: LiveData<Float> = _zoomState

    private val _aspectRatio = MutableLiveData<Int>()
    val aspectRatio: LiveData<Int> = _aspectRatio

    private val _flashMode = MutableLiveData<Int>()
    val flashMode: LiveData<Int> = _flashMode



    init {
        _cameraSelector.value = CameraSelector.DEFAULT_BACK_CAMERA
        _zoomState.value = 1f
        _aspectRatio.value = AspectRatio.RATIO_4_3
        _flashMode.value = ImageCapture.FLASH_MODE_OFF
    }

    fun setImageUri(uri: String?) {
        _imageUri.value = uri
    }

    fun flipCamera() {
        _cameraSelector.value = if (_cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        if (_cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA) {
            _flashMode.value = ImageCapture.FLASH_MODE_OFF
        }
    }

    fun setZoomState(zoomRatio: Float) {
        _zoomState.value = zoomRatio
    }

    fun setAspectRatio(ratio: Int) {
        _aspectRatio.value = ratio
    }

    fun toggleFlash() {
        _flashMode.value = if (_flashMode.value == ImageCapture.FLASH_MODE_ON) {
            ImageCapture.FLASH_MODE_OFF
        } else {
            ImageCapture.FLASH_MODE_ON
        }
    }

    fun disableFlash() {
        _flashMode.value = ImageCapture.FLASH_MODE_OFF
    }
}