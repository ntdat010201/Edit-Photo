package com.example.editphoto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.databinding.ItemGalleryBinding
import com.example.editphoto.model.PhotoModel
import com.example.editphoto.utils.extent.showImageGlide

class GalleryAdapter(
    private val images: MutableList<PhotoModel>,
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {
    var onItemClick: (PhotoModel) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ItemGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imgItem = images[position]
        showImageGlide(holder.itemView.context, imgItem.uri.toUri(), holder.binding.imgGallery)

        holder.itemView.setOnClickListener {
            onItemClick.invoke(imgItem)
        }
    }

    override fun getItemCount(): Int = images.size

    fun updateData(newList: List<PhotoModel>) {
        images.clear()
        images.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ImageViewHolder(val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root)
}