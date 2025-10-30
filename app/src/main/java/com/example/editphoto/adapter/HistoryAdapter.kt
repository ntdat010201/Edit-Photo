package com.example.editphoto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.databinding.ItemHistoryBinding
import com.example.editphoto.model.PhotoModel
import com.example.editphoto.utils.showImageGlide

class HistoryAdapter(
    private val images: MutableList<PhotoModel>,
) : RecyclerView.Adapter<HistoryAdapter.ImageViewHolder>() {

    var onItemClick: (PhotoModel) -> Unit = {}
    var onDeleteClick: (PhotoModel) -> Unit = {}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {
        return ImageViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        val imgItem = images[position]
        showImageGlide(holder.itemView.context, imgItem.uri, holder.binding.imgGallery)

        holder.itemView.setOnClickListener {
            onItemClick.invoke(imgItem)
        }

        holder.imgDelete.setOnClickListener {
            onDeleteClick.invoke(imgItem)
        }
    }

    override fun getItemCount(): Int {
       return images.size
    }

    inner class ImageViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
            val imgDelete = binding.imgDelete
    }


}