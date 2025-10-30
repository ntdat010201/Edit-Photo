package com.example.editphoto.utils.inter

interface SeekBarController {
    fun onIntensityChanged(intensity: Float)
    fun getDefaultIntensity(): Float = 0.0f
}