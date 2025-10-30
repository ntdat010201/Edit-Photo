package com.example.editphoto.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.editphoto.databinding.ItemHistoryBinding
import com.example.editphoto.model.PhotoModel
import com.example.editphoto.utils.extent.showImageGlide

class HistoryAdapter(
    private var photos: MutableList<PhotoModel>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    var onItemClick: (PhotoModel) -> Unit = {}

    var onDeleteClick :(PhotoModel) -> Unit = {}

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
            var imgDelete = binding.imgDelete
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = photos.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val photo = photos[position]

        showImageGlide(holder.itemView.context,photo.uri.toUri(),holder.binding.imgHistory)

        holder.itemView.setOnClickListener {
            onItemClick.invoke(photo)
        }

        holder.imgDelete.setOnClickListener {
            onDeleteClick.invoke(photo)
        }
    }

    fun updateData(newList: List<PhotoModel>) {
        photos.clear()
        photos.addAll(newList)
        notifyDataSetChanged()
    }
}
