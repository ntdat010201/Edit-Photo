package com.example.editphoto.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.R
import com.example.editphoto.databinding.ItemSubBinding
import com.example.editphoto.model.AdjustModel
import com.example.editphoto.model.SubModel

class SubOptionsAdapter(
    private val list: List<SubModel>
) : RecyclerView.Adapter<SubOptionsAdapter.ViewHolder>() {
    var onItemClick: ((SubModel) -> Unit)? = null

     var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        holder.binding.tvSub.text = item.text

        if (position == selectedPosition) {
            holder.binding.tvSub.setTextColor(Color.BLACK)
        } else {
            holder.binding.tvSub.setTextColor(
                ContextCompat.getColor(context, R.color.text_sort_unselected)
            )
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            if (previousPosition != -1) notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount() = list.size

    class ViewHolder(val binding: ItemSubBinding) : RecyclerView.ViewHolder(binding.root)
}
