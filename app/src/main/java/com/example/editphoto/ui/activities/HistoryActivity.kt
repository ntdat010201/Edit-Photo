package com.example.editphoto.ui.activities

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.editphoto.adapter.HistoryAdapter
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityHistoryBinding

class HistoryActivity : BaseActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private var adapter: HistoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        adapter = HistoryAdapter(mutableListOf())

        binding.rcvHistory.layoutManager = GridLayoutManager(this, 2)
        binding.rcvHistory.adapter = adapter
    }

    private fun initView() {

    }

    private fun initListener() {
        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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