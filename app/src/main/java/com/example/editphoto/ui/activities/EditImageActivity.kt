package com.example.editphoto.ui.activities

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.editphoto.R
import com.example.editphoto.adapter.MainFeaturesAdapter
import com.example.editphoto.adapter.SubOptionsAdapter
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityEditImageBinding
import com.example.editphoto.enums.FeatureType
import com.example.editphoto.enums.SubType
import com.example.editphoto.model.SubModel
import com.example.editphoto.ui.fragments.BlurFragment
import com.example.editphoto.ui.fragments.CheeksFragment
import com.example.editphoto.ui.fragments.CutFragment
import com.example.editphoto.ui.fragments.EyesFragment
import com.example.editphoto.ui.fragments.FlipFragment
import com.example.editphoto.ui.fragments.LipsFragment
import com.example.editphoto.ui.fragments.TurnFragment
import com.example.editphoto.ui.fragments.WhiteFragment
import com.example.editphoto.utils.listAdjust
import com.example.editphoto.utils.listAdjustSub
import com.example.editphoto.utils.listFaceSub
import com.example.editphoto.utils.showImageGlide
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import org.opencv.android.OpenCVLoader

class EditImageActivity : BaseActivity() {
    internal lateinit var binding: ActivityEditImageBinding
    internal val viewModel: EditImageViewModel by viewModels()
    private lateinit var featuresAdapter: MainFeaturesAdapter
    private var originalBitmap: Bitmap? = null
    private var faceLandmarker: FaceLandmarker? = null

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
        //opencv
        if (!OpenCVLoader.initDebug()) {
            throw IllegalStateException("OpenCV failed to load")
        }
        // mediaPipe
        setupFaceLandmarker()

        val uriString = intent.getStringExtra("image_uri")

        originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uriString?.toUri())
        viewModel.setOriginalBitmap(originalBitmap!!)
        binding.rvMainFeatures.apply {
            featuresAdapter = MainFeaturesAdapter(listAdjust)
            layoutManager =
                LinearLayoutManager(this@EditImageActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuresAdapter
        }
    }

    private fun initView() {
        showImageGlide(this,originalBitmap!!,binding.imgPreview)
    }

    private fun observeViewModel() {
        viewModel.editedBitmap.observe(this) { bitmap ->
            binding.imgPreview.setImageBitmap(bitmap)
        }

        viewModel.previewBitmap.observe(this) { preview ->
            if (preview != null) {
                binding.imgPreview.setImageBitmap(preview)
            } else {
                viewModel.editedBitmap.value?.let { binding.imgPreview.setImageBitmap(it) }
            }
        }
    }

    private fun initListener() {
        featuresAdapter.onItemClick = { item ->
            when (item.type) {
                FeatureType.ADJUST -> showSubOptionsAdjust(listAdjustSub, item.text)
                FeatureType.FACE -> showSubOptionsFace(listFaceSub, item.text)
                FeatureType.STICKER -> showSubOptionsFace(listFaceSub, item.text)
            }
        }

        binding.imgBackEdit.setOnClickListener {
            binding.constraintFeature.visibility = View.VISIBLE
            binding.constraintTool.visibility = View.VISIBLE
            binding.constraintSub.visibility = View.GONE
        }

        binding.imgApply.setOnClickListener {
            saveEditedImageToGallery()
        }
        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupFaceLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task") // Load assets
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

    private fun showSubOptionsAdjust(list: List<SubModel>, text: String) {
        binding.constraintFeature.visibility = View.GONE
        binding.constraintTool.visibility = View.GONE
        binding.constraintSub.visibility = View.VISIBLE
        binding.tvNameEdit.text = text

        val subAdapter = SubOptionsAdapter(list)
        binding.rvSubOptions.apply {
            layoutManager =
                LinearLayoutManager(this@EditImageActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = subAdapter
        }

        subAdapter.onItemClick = { item ->
            val fragment = when (item.type) {
                SubType.CUT -> CutFragment()
                SubType.FLIP -> FlipFragment()
                SubType.TURN -> TurnFragment()
                else -> null

            }
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.editContainer, it)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun showSubOptionsFace(list: List<SubModel>, text: String) {
        binding.constraintFeature.visibility = View.GONE
        binding.constraintTool.visibility = View.GONE
        binding.constraintSub.visibility = View.VISIBLE
        binding.tvNameEdit.text = text

        val subAdapter = SubOptionsAdapter(list)
        binding.rvSubOptions.apply {
            layoutManager =
                LinearLayoutManager(this@EditImageActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = subAdapter
        }

        subAdapter.onItemClick = { item ->
            val fragment = when (item.type) {
                SubType.LIPS -> LipsFragment()
                SubType.EYES -> EyesFragment()
                SubType.CHEEKS -> CheeksFragment()
                SubType.WHITE -> WhiteFragment()
                SubType.BLUR -> BlurFragment()
                else -> null

            }
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.editContainer, it)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    fun enableCropMode() {
        binding.imgPreview.visibility = View.GONE
        binding.cropImageView.visibility = View.VISIBLE

        // crop view
        viewModel.editedBitmap.value?.let {
            binding.cropImageView.setImageBitmap(it)
            binding.cropImageView.setFixedAspectRatio(true)
        }
    }

    fun disableCropMode() {
        binding.cropImageView.visibility = View.GONE
        binding.imgPreview.visibility = View.VISIBLE
    }

    private fun saveEditedImageToGallery() {
        viewModel.editedBitmap.value?.let { bitmap ->
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "edited_image_${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EditPhoto")
                }
            }

            val uri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                }
                Toast.makeText(this, "Ảnh đã lưu vào Gallery", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        faceLandmarker?.close()
    }

}