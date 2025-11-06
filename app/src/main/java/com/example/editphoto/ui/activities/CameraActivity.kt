package com.example.editphoto.ui.activities

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.ScaleGestureDetector
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
// removed DisplayManager; use previewView.display for Display
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.camera2.interop.Camera2Interop
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.editphoto.R
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityCameraBinding
import com.example.editphoto.utils.Const.ASPECT_RATIO_FULL
import com.example.editphoto.utils.Const.MAX_ZOOM_RATIO
import com.example.editphoto.utils.extent.showImageGlide
import com.example.editphoto.viewmodel.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraActivity : BaseActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private val viewModel: CameraViewModel by viewModels()
    private var camera: Camera? = null
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private val AF_SIZE = 1.0f / 6.0f
    private val AE_SIZE = AF_SIZE * 1.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        startCamera()
        updateZoomState(1f)
        updateAspectRatioUI(viewModel.aspectRatio.value ?: AspectRatio.RATIO_4_3)
        updateFlashUI(viewModel.flashMode.value ?: ImageCapture.FLASH_MODE_OFF)
        scaleGestureDetector = ScaleGestureDetector(this, ScaleGestureListener())

    }

    private fun initView() {
        viewModel.imageUri.observe(this) { uri ->
            if (uri != null) {
                showImageGlide(this, uri.toUri(), binding.imgCamera)
            }
        }

        viewModel.cameraSelector.observe(this) {
            startCamera()
            if (viewModel.cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA &&
                viewModel.flashMode.value == ImageCapture.FLASH_MODE_ON
            ) {
                viewModel.disableFlash()
                updateFlashUI(ImageCapture.FLASH_MODE_OFF)
                Toast.makeText(this, "Camera trước không hỗ trợ flash!", Toast.LENGTH_SHORT).show()
            }
        }

        // cập nhật preview
        viewModel.aspectRatio.observe(this) { ratio ->
            updatePreviewViewAspectRatio(ratio)
        }

        viewModel.flashMode.observe(this) {
            updateFlashUI(it)
            imageCapture?.flashMode = it
        }
    }

    private fun initListener() {
        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.flipCamera.setOnClickListener { viewModel.flipCamera() }

        binding.imgCamera.setOnClickListener {
            val imageUri = viewModel.imageUri.value
            if (imageUri != null) {
                val intent = Intent(this, EditImageActivity::class.java)
                intent.putExtra("image_uri", imageUri)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Chưa có ảnh để xem!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.zoom1x.setOnClickListener {
            updateZoomState(1f)
            updateZoomUI(1f)
        }

        binding.zoom2x.setOnClickListener {
            updateZoomState(2f)
            updateZoomUI(2f)
        }

        // Pinch-to-zoom + tap-to-focus
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                camera?.cameraInfo?.let { info ->
                    val width = binding.previewView.width.toFloat()
                    val height = binding.previewView.height.toFloat()
                    val display = binding.previewView.display ?: return false
                    val factory = DisplayOrientedMeteringPointFactory(
                        display,
                        info,
                        width,
                        height
                    )
                    val afPoint = factory.createPoint(event.x, event.y, AF_SIZE)
                    val aePoint = factory.createPoint(event.x, event.y, AE_SIZE)
                    val action = FocusMeteringAction.Builder(afPoint, FocusMeteringAction.FLAG_AF)
                        .addPoint(aePoint, FocusMeteringAction.FLAG_AE)
                        .build()
                    camera?.cameraControl?.startFocusAndMetering(action)
                    return true
                }
                return false
            }
        })

        binding.previewView.setOnTouchListener { _, event ->
            val handledGesture = gestureDetector.onTouchEvent(event)
            val handledScale = scaleGestureDetector.onTouchEvent(event)
            handledGesture || handledScale
        }


        binding.constraintFull.setOnClickListener {
            viewModel.setAspectRatio(ASPECT_RATIO_FULL)
            updateAspectRatioUI(ASPECT_RATIO_FULL)
        }

        binding.constraint169.setOnClickListener {
            viewModel.setAspectRatio(AspectRatio.RATIO_16_9)
            updateAspectRatioUI(AspectRatio.RATIO_16_9)
        }

        binding.constraint43.setOnClickListener {
            viewModel.setAspectRatio(AspectRatio.RATIO_4_3)
            updateAspectRatioUI(AspectRatio.RATIO_4_3)
        }

        binding.constraintFlash.setOnClickListener {
            if (viewModel.cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA) {
                Toast.makeText(this, "Camera trước không hỗ trợ flash!", Toast.LENGTH_SHORT).show()
            } else if (camera?.cameraInfo?.hasFlashUnit() == true) {
                viewModel.toggleFlash()
            } else {
                Toast.makeText(this, "Thiết bị không hỗ trợ flash!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val previewBuilder = Preview.Builder()
            val previewExtender = Camera2Interop.Extender(previewBuilder)
            previewExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            previewExtender.setCaptureRequestOption(
                CaptureRequest.NOISE_REDUCTION_MODE,
                CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
            )
            previewExtender.setCaptureRequestOption(
                CaptureRequest.EDGE_MODE,
                CaptureRequest.EDGE_MODE_HIGH_QUALITY
            )
            previewExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )

            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                )
                .build()

            val icBuilder = ImageCapture.Builder()
            val icExtender = Camera2Interop.Extender(icBuilder)
            icExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            icExtender.setCaptureRequestOption(
                CaptureRequest.NOISE_REDUCTION_MODE,
                CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
            )
            icExtender.setCaptureRequestOption(
                CaptureRequest.EDGE_MODE,
                CaptureRequest.EDGE_MODE_HIGH_QUALITY
            )
            icExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
            icExtender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_LOCK,
                false
            )

            imageCapture = icBuilder
                .setResolutionSelector(resolutionSelector)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(viewModel.flashMode.value ?: ImageCapture.FLASH_MODE_AUTO)
                .setTargetRotation(binding.previewView.display.rotation)
                .setJpegQuality(100)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    viewModel.cameraSelector.value ?: CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                Log.d("CameraActivity", "Flash available: ${camera?.cameraInfo?.hasFlashUnit()}")
                // Make image slightly brighter compared to system camera: +1 EV if supported
                camera?.cameraInfo?.exposureState?.exposureCompensationRange?.let { range ->
                    if (range.upper >= 1) {
                        camera?.cameraControl?.setExposureCompensationIndex(1)
                    }
                }
                updateZoomState(viewModel.zoomState.value ?: 1f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val centerFocusAndCapture = {
            val fileName = "IMG_${System.currentTimeMillis()}"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EditPhoto")
            }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                        Toast.makeText(this@CameraActivity, "Lưu ảnh thất bại!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri ?: return
                        val ratio = viewModel.aspectRatio.value ?: AspectRatio.RATIO_4_3

                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                                    val src = ImageDecoder.createSource(contentResolver, savedUri)
                                    ImageDecoder.decodeBitmap(src)
                                } else {
                                    val input = contentResolver.openInputStream(savedUri)
                                    BitmapFactory.decodeStream(input)
                                }
                                val cropped = cropBitmapToRatio(bitmap, ratio)
                                val out = contentResolver.openOutputStream(savedUri)
                                out?.let { cropped.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                                out?.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@CameraActivity,
                                    "Ảnh đã lưu vào Pictures/EditPhoto",
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.setImageUri(savedUri.toString())
                            }
                        }
                    }
                }
            )
        }

        // Lấy nét trung tâm trước khi chụp để ảnh sắc nét hơn
        val info = camera?.cameraInfo
        val control = camera?.cameraControl
        if (info != null && control != null) {
            val width = binding.previewView.width.toFloat()
            val height = binding.previewView.height.toFloat()
            val display = binding.previewView.display
            if (display == null) {
                centerFocusAndCapture()
                return
            }
            val factory = DisplayOrientedMeteringPointFactory(
                display,
                info,
                width,
                height
            )
            val x = width / 2f
            val y = height / 2f
            val afPoint = factory.createPoint(x, y, AF_SIZE)
            val aePoint = factory.createPoint(x, y, AE_SIZE)
            val action = FocusMeteringAction.Builder(afPoint, FocusMeteringAction.FLAG_AF)
                .addPoint(aePoint, FocusMeteringAction.FLAG_AE)
                .build()
            val future = control.startFocusAndMetering(action)
            future.addListener({
                try {
                    val result = future.get()
                    // Nếu cần, có thể kiểm tra result.isFocusSuccessful để quyết định retry
                } catch (_: Exception) {
                }
                centerFocusAndCapture()
            }, ContextCompat.getMainExecutor(this))
        } else {
            centerFocusAndCapture()
        }
    }

    private fun cropBitmapToRatio(bitmap: Bitmap, aspectRatio: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val targetRatio = when (aspectRatio) {
            AspectRatio.RATIO_16_9 -> 9f / 16f
            ASPECT_RATIO_FULL -> height.toFloat() / width
            else -> 3f / 4f
        }

        val currentRatio = width.toFloat() / height.toFloat()
        return if (currentRatio > targetRatio) {
            val newWidth = (height * targetRatio).toInt()
            val xOffset = (width - newWidth) / 2
            Bitmap.createBitmap(bitmap, xOffset, 0, newWidth, height)
        } else {
            val newHeight = (width / targetRatio).toInt()
            val yOffset = (height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, yOffset, width, newHeight)
        }
    }

    private fun updateZoomState(zoomRatio: Float) {
        camera?.let {
            val minZoom = it.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
            val maxZoom = minOf(it.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f, MAX_ZOOM_RATIO)
            val clampedZoom = zoomRatio.coerceIn(minZoom, maxZoom)
            it.cameraControl.setZoomRatio(clampedZoom)
            viewModel.setZoomState(clampedZoom)
            updateZoomUI(clampedZoom)
        }
    }

    private fun updateZoomUI(zoomRatio: Float) {
        fun formatZoom(value: Float): String {
            val formatted = String.format("%.1f", value)
            return if (formatted.endsWith(".0")) formatted.dropLast(2) + "x" else "$formatted" + "x"
        }

        val zoomText1x = binding.zoom1x.findViewById<TextView>(R.id.zoomText1x)
        val zoomText2x = binding.zoom2x.findViewById<TextView>(R.id.zoomText2x)

        when {
            zoomRatio < 2f -> {
                binding.zoom1x.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_white)
                binding.zoom2x.background = ContextCompat.getDrawable(this, android.R.color.transparent)
                zoomText1x.text = formatZoom(zoomRatio)
                zoomText2x.text = "2x"
            }
            else -> {
                binding.zoom1x.background = ContextCompat.getDrawable(this, android.R.color.transparent)
                binding.zoom2x.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_white)
                zoomText1x.text = "1x"
                zoomText2x.text = formatZoom(zoomRatio)
            }
        }
    }

    private fun updateAspectRatioUI(aspectRatio: Int) {
        binding.constraintFull.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_white)
        binding.constraint169.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_white)
        binding.constraint43.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_white)

        when (aspectRatio) {
            ASPECT_RATIO_FULL -> binding.constraintFull.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_yellow)
            AspectRatio.RATIO_16_9 -> binding.constraint169.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_yellow)
            AspectRatio.RATIO_4_3 -> binding.constraint43.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_yellow)
        }
    }

    private fun updateFlashUI(flashMode: Int) {
        if (flashMode == ImageCapture.FLASH_MODE_ON) {
            binding.constraintFlash.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_yellow)
            binding.imgFlash.setImageResource(R.drawable.ic_flash_on)
        } else {
            binding.constraintFlash.background = ContextCompat.getDrawable(this, R.drawable.circle_bgr_white)
            binding.imgFlash.setImageResource(R.drawable.ic_flash_off)
        }
    }

    private fun updatePreviewViewAspectRatio(aspectRatio: Int) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.main)

        when (aspectRatio) {
            ASPECT_RATIO_FULL -> {
                constraintSet.clear(R.id.previewView, ConstraintSet.TOP)
                constraintSet.clear(R.id.previewView, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.previewView, ConstraintSet.TOP, R.id.main, ConstraintSet.TOP)
                constraintSet.connect(R.id.previewView, ConstraintSet.BOTTOM, R.id.main, ConstraintSet.BOTTOM)
                constraintSet.setDimensionRatio(R.id.previewView, null)
            }

            AspectRatio.RATIO_16_9 -> {
                constraintSet.clear(R.id.previewView, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.previewView, ConstraintSet.TOP, R.id.main, ConstraintSet.TOP)
                constraintSet.connect(R.id.previewView, ConstraintSet.BOTTOM, R.id.constrain_1, ConstraintSet.BOTTOM)
                constraintSet.setDimensionRatio(R.id.previewView, "9:16")
                val marginTopPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 30f, binding.root.resources.displayMetrics
                ).toInt()
                constraintSet.setMargin(R.id.previewView, ConstraintSet.TOP, marginTopPx)
            }

            else -> { // 4:3
                constraintSet.clear(R.id.previewView, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.previewView, ConstraintSet.TOP, R.id.main, ConstraintSet.TOP)
                constraintSet.connect(R.id.previewView, ConstraintSet.BOTTOM, R.id.constrain_1, ConstraintSet.TOP)
                constraintSet.setDimensionRatio(R.id.previewView, "3:4")
                val marginTopPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 30f, binding.root.resources.displayMetrics
                ).toInt()
                constraintSet.setMargin(R.id.previewView, ConstraintSet.TOP, marginTopPx)
            }
        }

        constraintSet.connect(R.id.previewView, ConstraintSet.START, R.id.main, ConstraintSet.START)
        constraintSet.connect(R.id.previewView, ConstraintSet.END, R.id.main, ConstraintSet.END)
        constraintSet.applyTo(binding.main)
    }

    private inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            camera?.let {
                val currentZoom = it.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                val minZoom = it.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                val maxZoom = minOf(it.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f, MAX_ZOOM_RATIO)
                val newZoom = (currentZoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
                updateZoomState(newZoom)
            }
            return true
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
