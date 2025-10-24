package com.example.editphoto.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.editphoto.R
import com.example.editphoto.adapter.GalleryAdapter
import com.example.editphoto.base.BaseActivity
import com.example.editphoto.databinding.ActivityGalleryBinding
import com.example.editphoto.enums.SortType
import com.example.editphoto.permission.PermissionManager
import com.example.editphoto.viewmodel.GalleryViewModel

class GalleryActivity : BaseActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var permissionManager: PermissionManager
    private val viewModel: GalleryViewModel by viewModels()
    private var adapter: GalleryAdapter? = null

    private var sortPopup: PopupWindow? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissionManager.handleResult(permissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initView()
        initListener()
        observeData()
    }


    private fun initData() {

        permissionManager = PermissionManager(this)

        permissionManager.requestGalleryPermission(permissionLauncher) {
            viewModel.loadAllImages()
        }
    }

    private fun initView() {
        adapter = GalleryAdapter(mutableListOf())
        binding.rcvGallery.layoutManager = GridLayoutManager(this, 3)
        binding.rcvGallery.adapter = adapter
    }


    private fun initListener() {
        binding.imgBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        adapter?.onItemClick = { item ->
            val intent = Intent(this, EditImageActivity::class.java)
            intent.putExtra("image_uri", item.uri.toString())
            startActivity(intent)
        }

        binding.imgMore.setOnClickListener { showSortPopup(it) }

    }

    private fun observeData() {
        viewModel.photos.observe(this) { list ->
            adapter?.updateData(list.toMutableList())
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showSortPopup(anchor: View) {
        sortPopup?.dismiss()

        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_sort_menu, null)
        val selectedColor = ContextCompat.getColor(this, R.color.text_sort_selected)
        val unselectedColor = ContextCompat.getColor(this, R.color.text_sort_unselected)

        val tvLastModified = popupView.findViewById<TextView>(R.id.tv_last_modified)
        val imgClock = popupView.findViewById<ImageView>(R.id.img_clock)

        val tvName = popupView.findViewById<TextView>(R.id.tv_name)
        val imgName = popupView.findViewById<ImageView>(R.id.img_name)

        val tvSize = popupView.findViewById<TextView>(R.id.tv_size)
        val imgSize = popupView.findViewById<ImageView>(R.id.img_size)

        val tvAsc = popupView.findViewById<TextView>(R.id.tv_arrow_up)
        val imgAsc = popupView.findViewById<ImageView>(R.id.img_arrow_up)

        val tvDesc = popupView.findViewById<TextView>(R.id.tv_arrow_down)
        val imgDesc = popupView.findViewById<ImageView>(R.id.img_arrow_down)

        val itemLastModified = tvLastModified.parent as ViewGroup
        val itemName = tvName.parent as ViewGroup
        val itemSize = tvSize.parent as ViewGroup
        val itemAsc = tvAsc.parent as ViewGroup
        val itemDesc = tvDesc.parent as ViewGroup

        fun updateSelection() {
            val type = viewModel.currentSortType
            val asc = viewModel.isAscending

            tvLastModified.setTextColor(if (type == SortType.DATE) selectedColor else unselectedColor)
            imgClock.setColorFilter(if (type == SortType.DATE) selectedColor else unselectedColor)

            tvName.setTextColor(if (type == SortType.NAME) selectedColor else unselectedColor)
            imgName.setColorFilter(if (type == SortType.NAME) selectedColor else unselectedColor)

            tvSize.setTextColor(if (type == SortType.SIZE) selectedColor else unselectedColor)
            imgSize.setColorFilter(if (type == SortType.SIZE) selectedColor else unselectedColor)

            tvAsc.setTextColor(if (asc) selectedColor else unselectedColor)
            imgAsc.setColorFilter(if (asc) selectedColor else unselectedColor)

            tvDesc.setTextColor(if (!asc) selectedColor else unselectedColor)
            imgDesc.setColorFilter(if (!asc) selectedColor else unselectedColor)
        }

        updateSelection()

        itemLastModified.setOnClickListener {
            viewModel.sortPhotos(SortType.DATE, viewModel.isAscending)
            sortPopup?.dismiss()
        }
        itemName.setOnClickListener {
            viewModel.sortPhotos(SortType.NAME, viewModel.isAscending)
            sortPopup?.dismiss()
        }
        itemSize.setOnClickListener {
            viewModel.sortPhotos(SortType.SIZE, viewModel.isAscending)
            sortPopup?.dismiss()
        }
        itemAsc.setOnClickListener {
            viewModel.sortPhotos(viewModel.currentSortType, true)
            sortPopup?.dismiss()
        }
        itemDesc.setOnClickListener {
            viewModel.sortPhotos(viewModel.currentSortType, false)
            sortPopup?.dismiss()
        }

        sortPopup = PopupWindow(popupView, 160.dp, ViewGroup.LayoutParams.WRAP_CONTENT, false).apply {
            isOutsideTouchable = true
            isFocusable = false
            elevation = 8f
            setBackgroundDrawable(0.toDrawable())

            setTouchInterceptor { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                    true
                } else false
            }

            setOnDismissListener {
                sortPopup = null
                hideSystemUiBar(window)
            }

            showAsDropDown(anchor, -180, 8)

            popupView.rootView.systemUiVisibility = window.decorView.systemUiVisibility
            hideSystemUiBar(window)
        }
    }


    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()


    override fun onStart() {
        super.onStart()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        hideSystemUiBar(window)
    }


}
