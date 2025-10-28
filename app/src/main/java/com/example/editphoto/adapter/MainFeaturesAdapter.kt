package com.example.editphoto.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.editphoto.R
import com.example.editphoto.databinding.ItemToolsBinding
import com.example.editphoto.model.AdjustModel
import androidx.core.graphics.toColorInt

class MainFeaturesAdapter(
    private val listAdjust : List<AdjustModel>,
) : RecyclerView.Adapter<MainFeaturesAdapter.ViewHolder>() {
     var onItemClick: ((AdjustModel) -> Unit)? = null
    var selectedPosition = 0

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

        if (position == selectedPosition) {
            holder.tvName.setTextColor("#FF63626E".toColorInt())
            holder.icon.setBackgroundResource(R.drawable.circle_bgr_grey)
            holder.icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        } else {
            holder.tvName.setTextColor("#FFBBB4CE".toColorInt())
            holder.icon.setBackgroundResource(R.drawable.circle_bgr_gray)
            holder.icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#BBB4CE"))
        }


        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            if (previousPosition != -1) notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

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