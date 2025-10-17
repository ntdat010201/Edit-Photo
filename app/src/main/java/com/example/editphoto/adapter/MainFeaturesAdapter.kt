package com.example.editphoto.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.databinding.ItemToolsBinding
import com.example.editphoto.model.AdjustModel

class MainFeaturesAdapter(
    private val listAdjust : List<AdjustModel>,

) : RecyclerView.Adapter<MainFeaturesAdapter.ViewHolder>() {

     var onItemClick: ((AdjustModel) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(ItemToolsBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val adjust = listAdjust[position]
        holder.icon.setImageResource(adjust.icon)
        holder.tvName.text = adjust.text

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(adjust)
        }
    }

    override fun getItemCount(): Int {
        return listAdjust.size
    }

    class ViewHolder(private val binding: ItemToolsBinding) : RecyclerView.ViewHolder(binding.root) {
        val icon = binding.ivIcon
        val tvName = binding.tvName
    }

}