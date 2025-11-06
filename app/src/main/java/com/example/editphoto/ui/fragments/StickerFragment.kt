package com.example.editphoto.ui.fragments

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.R
import com.example.editphoto.adapter.StickerAdapter
import com.example.editphoto.databinding.FragmentStickerBinding
import com.example.editphoto.ui.activities.EditImageActivity
import com.example.editphoto.utils.extent.listSticker
import com.example.editphoto.utils.inter.OnApplyListener
import com.example.editphoto.utils.inter.UnsavedChangesListener
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.SaveSettings


class StickerFragment : Fragment(), OnApplyListener, UnsavedChangesListener {
    private lateinit var binding: FragmentStickerBinding
    private lateinit var parentActivity: EditImageActivity
    private var photoEditor: PhotoEditor? = null

    private var adapter : StickerAdapter? = null

    private var hasApplied = false
    private var isDirty = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStickerBinding.inflate(layoutInflater)
        parentActivity = requireActivity() as EditImageActivity

        initData()
        initView()
        initListener()
        return binding.root
    }

    private fun initData() {
        val stickerAdapter = StickerAdapter(listSticker)

        binding.rcvSticker.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = stickerAdapter

/*            addItemDecoration(object : RecyclerView.ItemDecoration() {
                private val spacing = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)

                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val position = parent.getChildAdapterPosition(view)
                    val column = position % 5
                    outRect.left = if (column == 0) spacing else spacing / 2
                    outRect.right = if (column == 4) spacing else spacing / 2
                    outRect.top = spacing / 2
                    outRect.bottom = spacing / 2
                }
            })*/
        }

        stickerAdapter.onItemClick = { sticker ->
            addSticker(sticker.imageSticker)
        }
    }
    private fun initView() {
        parentActivity.showStickerEditor()

        photoEditor = PhotoEditor.Builder(parentActivity, parentActivity.binding.photoEditorView)
            .setPinchTextScalable(true)
            .build()
    }

    private fun initListener() {
        adapter?.onItemClick = { sticker ->
            addSticker(sticker.imageSticker)
        }
    }

    private fun addSticker(resId: Int) {
        val drawable = AppCompatResources.getDrawable(requireContext(), resId)
        val bitmap = drawable?.toBitmap()
        if (bitmap != null) {
            parentActivity.showStickerEditor()
            photoEditor?.addImage(bitmap)
            isDirty = true
        }
    }

    // OnApplyListener
    override fun onApply() {
        val saveSettings = SaveSettings.Builder()
            .setClearViewsEnabled(true)
            .setTransparencyEnabled(true)
            .build()

        try {
            photoEditor?.saveAsBitmap(saveSettings, object : OnSaveBitmap  {
                override fun onBitmapReady(bitmap: android.graphics.Bitmap) {
                    parentActivity.viewModel.updateBitmap(bitmap)
                    parentActivity.updateImagePreserveZoom(bitmap)
                    parentActivity.hideStickerEditor()
                    hasApplied = true
                    isDirty = false
                }
            })

        } catch (_: Exception) {
            parentActivity.hideStickerEditor()
        }

    }

    // UnsavedChangesListener
    override fun hasUnsavedChanges(): Boolean = isDirty && !hasApplied

    override fun revertUnsavedChanges() {
        if (!hasApplied) {
            photoEditor?.clearAllViews()
            parentActivity.hideStickerEditor()
            isDirty = false
        }
    }
}