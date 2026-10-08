package com.gps.camera.map.photo.gpscamera.features.layout.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.databinding.ItemLayoutOptionBinding

class LayoutOptionAdapter(
    private val previews: List<Bitmap>,
    private var selectedIndex: Int,
    private val onSelected: (Int) -> Unit
) : RecyclerView.Adapter<LayoutOptionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLayoutOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = previews.size

    fun setSelected(index: Int) {
        val previous = selectedIndex
        selectedIndex = index
        notifyItemChanged(previous)
        notifyItemChanged(selectedIndex)
    }

    inner class ViewHolder(private val binding: ItemLayoutOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.ivPreview.setImageBitmap(previews[position])
            binding.tvLayoutName.text = binding.root.context.getString(R.string.layout_option_name, position + 1)

            val isSelected = position == selectedIndex
            binding.previewFrame.setBackgroundColor(
                binding.root.context.getColor(if (isSelected) R.color.accent_blue else R.color.black)
            )

            binding.root.setOnClickListener {
                onSelected(position)
            }
        }
    }
}
