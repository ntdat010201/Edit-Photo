package com.example.editphoto.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.editphoto.R
import com.example.editphoto.ui.activities.EditImageActivity
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc



/** Chuyển Bitmap → Mat */
fun Bitmap.toMat(): Mat {
    val mat = Mat()
    Utils.bitmapToMat(this, mat)
    return mat
}

/** Chuyển Mat → Bitmap */
fun Mat.toBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(this.cols(), this.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bmp)
    return bmp
}

fun Mat.smoothMask(blurSize: Double = 7.0, sigma: Double = 4.0): Mat {
    val blurred = Mat()
    Imgproc.GaussianBlur(this, blurred, Size(blurSize, blurSize), sigma)
    return blurred
}

fun Mat.blendWith(
    overlay: Mat,
    mask: Mat,
    intensity: Float
): Mat {
    val blended = Mat()
    Core.addWeighted(this, 1.0 - intensity, overlay, intensity.toDouble(), 0.0, blended)
    blended.copyTo(this, mask)
    return this
}



fun Fragment.handlePhysicalBackPress(action: (act: EditImageActivity) -> Unit) {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
        val act = requireActivity() as EditImageActivity
        action(act)
    }
}


fun Fragment.handleBackPressedCommon(
    act: EditImageActivity,
    hasApplied: Boolean,
    beforeBitmap: android.graphics.Bitmap?,
    onReset: (() -> Unit)? = null
) {
    val vm = act.viewModel
    if (!hasApplied) {
        beforeBitmap?.let {
            vm.setPreview(null)
            vm.updateBitmap(it)
        }
        onReset?.invoke()
    }
    parentFragmentManager.popBackStack()
}

fun showImageGlide(context: Context, uri: Uri, view: ImageView) {
    Glide.with(context)
        .load(uri)
        .placeholder(R.drawable.ic_gallery)
        .error(R.drawable.img_view_gallery)
        .into(view)
}

fun showImageGlide(context: Context, bitmap: Bitmap, view: ImageView) {
    Glide.with(context)
        .load(bitmap)
        .placeholder(R.drawable.ic_gallery)
        .error(R.drawable.img_view_gallery)
        .into(view)
}


