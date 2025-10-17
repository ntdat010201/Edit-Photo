package com.example.editphoto.ui.activities

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.net.toUri
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
import com.example.editphoto.ui.fragments.EyesFragment
import com.example.editphoto.ui.fragments.LipsFragment
import com.example.editphoto.ui.fragments.WhiteFragment
import com.example.editphoto.utils.listAdjust
import com.example.editphoto.utils.listFace
import com.example.editphoto.viewmodel.EditImageViewModel
import org.opencv.android.OpenCVLoader

class EditImageActivity : BaseActivity() {
    private lateinit var binding: ActivityEditImageBinding
    private val viewModel: EditImageViewModel by viewModels()
    private lateinit var featuresAdapter: MainFeaturesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {

        val uriString = intent.getStringExtra("image_uri")
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uriString?.toUri())
        viewModel.setOriginalImage(bitmap)

        binding.rvMainFeatures.apply {
            featuresAdapter = MainFeaturesAdapter(listAdjust)
            layoutManager =
                LinearLayoutManager(this@EditImageActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuresAdapter
        }

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Không load được OpenCV")
        } else {
            Log.d("OpenCV", "Đã load OpenCV thành công")
        }

    }


    private fun initView() {
        viewModel.editedImage.observe(this) { updatedBitmap ->
            binding.imgPreview.setImageBitmap(updatedBitmap)
        }

    }

    private fun initListener() {
        featuresAdapter.onItemClick = { item ->
            when (item.type) {
                FeatureType.RADIO -> showSubOptions(listFace, item.text)
                FeatureType.FACE -> showSubOptions(listFace, item.text)
                FeatureType.STICKER -> showSubOptions(listFace, item.text)
            }
        }

        binding.imgBackEdit.setOnClickListener {
            binding.constraintFeature.visibility = View.VISIBLE
            binding.constraintSub.visibility = View.GONE
        }
    }

    private fun showSubOptions(list: List<SubModel>, text: String) {
        binding.constraintFeature.visibility = View.GONE
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
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.editContainer, fragment)
                .commit()
        }
    }


}