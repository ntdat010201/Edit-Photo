package com.example.editphoto.ui.activities

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.example.editphoto.R
import com.example.editphoto.adapter.MainFeaturesAdapter
import com.example.editphoto.adapter.SubOptionsAdapter
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityEditImageBinding
import com.example.editphoto.enums.FeatureType
import com.example.editphoto.enums.SubType
import com.example.editphoto.model.PhotoModel
import com.example.editphoto.model.SubModel
import com.example.editphoto.ui.fragments.BlurFragment
import com.example.editphoto.ui.fragments.CheeksFragment
import com.example.editphoto.ui.fragments.CutFragment
import com.example.editphoto.ui.fragments.EyesFragment
import com.example.editphoto.ui.fragments.FlipFragment
import com.example.editphoto.ui.fragments.LipsFragment
import com.example.editphoto.ui.fragments.TurnFragment
import com.example.editphoto.ui.fragments.EyebrowFragment
import com.example.editphoto.utils.extent.listAdjust
import com.example.editphoto.utils.extent.listAdjustSub
import com.example.editphoto.utils.extent.listFaceSub
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.utils.inter.SeekBarController
import com.example.editphoto.viewmodel.EditImageViewModel
import com.example.editphoto.viewmodel.PhotoViewModel
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import org.opencv.android.OpenCVLoader
import java.io.File

class EditImageActivity : BaseActivity() {
    internal lateinit var binding: ActivityEditImageBinding
    internal val viewModel: EditImageViewModel by viewModels()
    private val photoViewModel: PhotoViewModel by viewModels()

    private lateinit var featuresAdapter: MainFeaturesAdapter
    private var originalBitmap: Bitmap? = null
    private var faceLandmarker: FaceLandmarker? = null
    private var currentSeekBarController: SeekBarController? = null

    private var currentFeatureType: FeatureType? = null
    private var currentSubType: SubType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initView()
        initListener()
        observeViewModel()
    }

    private fun initData() {
        if (!OpenCVLoader.initDebug()) {
            throw IllegalStateException("OpenCV failed to load")
        }

        setupFaceLandmarker()

        val uriString = intent.getStringExtra("image_uri")
        originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uriString?.toUri())
        viewModel.setOriginalBitmap(originalBitmap!!)

        featuresAdapter = MainFeaturesAdapter(listAdjust)
        binding.rvMainFeatures.apply {
            layoutManager = if (listAdjust.size <= 5) {
                FlexboxLayoutManager(this@EditImageActivity).apply {
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.SPACE_BETWEEN
                }
            } else {
                LinearLayoutManager(
                    this@EditImageActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                ).also {
                    LinearSnapHelper().attachToRecyclerView(this@apply)
                }
            }
            adapter = featuresAdapter
        }
    }

    private fun initView() {
        /*
                showImageGlide(this, originalBitmap!!, binding.imgPreview)
        */

        currentFeatureType = FeatureType.ADJUST
        currentSubType = SubType.CUT

        showSubOptionsAdjust(listAdjustSub)
        supportFragmentManager.beginTransaction()
            .replace(R.id.editContainer, CutFragment())
            .commit()

        binding.editContainer.visibility = View.VISIBLE
    }

    private fun observeViewModel() {
        viewModel.editedBitmap.observe(this) { bitmap ->
            Log.d("DAT", "observeViewModel: " + "bitmap")
            binding.imgPreview.setImageBitmap(bitmap)
        }

        viewModel.previewBitmap.observe(this) { preview ->
            if (preview != null) {
                binding.imgPreview.setImageBitmap(preview)
                Log.d("DAT", "observeViewModel: " + "preview")

            } else {
                Log.d("DAT", "observeViewModel: " + "preview null")
                viewModel.editedBitmap.value?.let { binding.imgPreview.setImageBitmap(it) }

            }
        }
    }

    private fun initListener() {
        featuresAdapter.onItemClick = { item ->
            if (currentFeatureType == item.type) {
                //
            } else {
                currentFeatureType = item.type
                currentSubType = null

                when (item.type) {
                    FeatureType.ADJUST -> {
                        showSubOptionsAdjust(listAdjustSub)
                        currentSubType = SubType.CUT
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.editContainer, CutFragment())
                            .commit()
                        binding.editContainer.visibility = View.VISIBLE
                    }

                    FeatureType.FACE -> {
                        showSubOptionsFace(listFaceSub)
                        currentSubType = SubType.LIPS
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.editContainer, LipsFragment())
                            .commit()
                        binding.editContainer.visibility = View.VISIBLE
                    }

                    FeatureType.STICKER -> {
                        showSubOptionsFace(listFaceSub)
                        currentSubType = SubType.LIPS
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.editContainer, LipsFragment())
                            .commit()
                        binding.editContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.imgApply.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.editContainer)
            if (fragment is OnApplyListener) {
                fragment.onApply()
            }
        }

        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvSave.setOnClickListener {
            val bitmap = viewModel.editedBitmap.value
            if (bitmap != null) {
                val uri = saveEditedImageToGallery(bitmap)
                uri?.let { saveImageInfoToRoom(it, bitmap) }
            } else {
                Toast.makeText(this, "Không có ảnh để lưu!", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun setupFaceLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .setOutputFaceBlendshapes(true)
            .setMinFaceDetectionConfidence(0.5f)
            .build()
        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
        viewModel.setFaceLandmarker(faceLandmarker!!)
    }

    private fun showSubOptionsAdjust(list: List<SubModel>) {
        val subAdapter = SubOptionsAdapter(list)
        binding.rvSubOptions.apply {
            layoutManager = if (list.size <= 5) {
                FlexboxLayoutManager(this@EditImageActivity).apply {
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.SPACE_BETWEEN
                }
            } else {
                LinearLayoutManager(
                    this@EditImageActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                ).also {
                    LinearSnapHelper().attachToRecyclerView(this@apply)
                }
            }
            adapter = subAdapter
        }

        subAdapter.onItemClick = { item ->
            if (currentSubType == item.type) {
                //
            } else {
                currentSubType = item.type

                val fragment = when (item.type) {
                    SubType.CUT -> CutFragment()
                    SubType.FLIP -> FlipFragment()
                    SubType.TURN -> TurnFragment()
                    else -> null
                }

                fragment?.let {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.editContainer, it)
                        .commit()
                    binding.editContainer.visibility = View.VISIBLE
                }
            }
        }

    }

    private fun showSubOptionsFace(list: List<SubModel>) {
        val subAdapter = SubOptionsAdapter(list)
        binding.rvSubOptions.apply {
            if (list.size <= 5) {
                layoutManager = FlexboxLayoutManager(context).apply {
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.SPACE_BETWEEN
                }
            } else {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                LinearSnapHelper().attachToRecyclerView(this)
            }
            adapter = subAdapter
        }

        subAdapter.onItemClick = { item ->
            if (currentSubType == item.type) {
                //
            } else {
                currentSubType = item.type

                val fragment = when (item.type) {
                    SubType.LIPS -> LipsFragment()
                    SubType.EYES -> EyesFragment()
                    SubType.CHEEKS -> CheeksFragment()
                    SubType.EYEBROW -> EyebrowFragment()
                    SubType.BLUR -> BlurFragment()
                    else -> null
                }

                fragment?.let {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.editContainer, it)
                        .commit()
                    binding.editContainer.visibility = View.VISIBLE
                }
            }
        }

    }

    fun enableCropMode() {
        binding.imgPreview.visibility = View.GONE
        binding.cropImageView.visibility = View.VISIBLE

        var beforeCropBitmap = viewModel.previewBitmap.value
            ?: viewModel.editedBitmap.value

        beforeCropBitmap.let {
            binding.cropImageView.setImageBitmap(it)
            binding.cropImageView.setFixedAspectRatio(true)
        }
    }

    fun disableCropMode() {
        binding.cropImageView.visibility = View.GONE
        binding.imgPreview.visibility = View.VISIBLE
    }


    override fun onStart() {
        super.onStart()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        hideSystemUiBar(window)
    }

    override fun onDestroy() {
        super.onDestroy()
        faceLandmarker?.close()
    }

    fun attachSeekBar(controller: SeekBarController) {
        currentSeekBarController = controller
        binding.seekbar.visibility = View.VISIBLE

        val defaultProgress = (controller.getDefaultIntensity() * 100).toInt().coerceIn(0, 100)
        binding.seekBarIntensity.progress = defaultProgress

        binding.seekBarIntensity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) controller.onIntensityChanged(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun detachSeekBar() {
        currentSeekBarController = null
        binding.seekbar.visibility = View.GONE
        binding.seekBarIntensity.setOnSeekBarChangeListener(null)
    }

    private fun saveEditedImageToGallery(bitmap: Bitmap): Uri? {
        val fileName = "edited_image_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EditPhoto")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            Toast.makeText(this, "Ảnh đã lưu vào bộ sưu tập", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Lưu ảnh thất bại!", Toast.LENGTH_SHORT).show()
        }
        return uri
    }

    private fun saveImageInfoToRoom(uri: Uri, bitmap: Bitmap) {
        val filePath = getFilePathFromUri(uri)
        val file = if (filePath != null) File(filePath) else null

        val photo = PhotoModel(
            id = System.currentTimeMillis().toString(),
            uri = uri.toString(),
            name = file?.name ?: "edited_image.jpg",
            path = file?.absolutePath ?: "",
            dateAdded = System.currentTimeMillis(),
            size = file?.length() ?: 0L,
            width = bitmap.width,
            height = bitmap.height
        )

        photoViewModel.insertPhoto(photo)
        Toast.makeText(this, "Đã lưu thông tin ảnh vào Room", Toast.LENGTH_SHORT).show()
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                path = cursor.getString(columnIndex)
            }
        }
        return path
    }

}
