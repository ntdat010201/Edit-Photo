package com.example.editphoto.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.databinding.ItemToolsBinding
import com.example.editphoto.model.SubModel

class SubOptionsAdapter(
    private val list: List<SubModel>
) : RecyclerView.Adapter<SubOptionsAdapter.ViewHolder>() {

    var onItemClick: ((SubModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemToolsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.ivIcon.setImageResource(item.icon)
        holder.binding.tvName.text = item.text

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount() = list.size

    class ViewHolder(val binding: ItemToolsBinding) : RecyclerView.ViewHolder(binding.root)
}
