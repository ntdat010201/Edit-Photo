package com.example.editphoto.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.editphoto.adapter.HistoryAdapter
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityHistoryBinding
import com.example.editphoto.viewmodel.PhotoViewModel

class HistoryActivity : BaseActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val photoViewModel: PhotoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initRecyclerView()
        observePhotoData()
        initListener()
    }

    private fun initRecyclerView() {
        adapter = HistoryAdapter(mutableListOf())
        binding.rcvHistory.layoutManager = GridLayoutManager(this, 2)
        binding.rcvHistory.adapter = adapter
    }

    private fun observePhotoData() {
        photoViewModel.allPhotos.observe(this, Observer { photos ->
            adapter.updateData(photos)
        })
    }

    private fun initListener() {
        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        adapter.onDeleteClick = { photo ->
            photoViewModel.deletePhoto(photo)
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
