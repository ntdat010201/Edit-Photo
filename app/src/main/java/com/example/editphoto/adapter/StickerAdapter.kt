package com.example.editphoto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.databinding.ItemStickerBinding
import com.example.editphoto.model.PhotoModel
import com.example.editphoto.model.StickerModel

class StickerAdapter(
    private val images: List<StickerModel>,
) : RecyclerView.Adapter<StickerAdapter.ImageViewHolder>() {
    var onItemClick: (StickerModel) -> Unit = {}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {
        return ImageViewHolder(
            ItemStickerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        val itemSticker = images[position]
        holder.imgSticker.setImageResource(itemSticker.imageSticker)

        holder.itemView.setOnClickListener {
            onItemClick.invoke(itemSticker)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ImageViewHolder(val binding: ItemStickerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imgSticker = binding.imgItemSticker
    }

}
