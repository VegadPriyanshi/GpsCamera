package com.gps.camera.map.photo.gpscamera.features.grid.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gps.camera.map.photo.gpscamera.R
import com.gps.camera.map.photo.gpscamera.databinding.ItemGridStyleBinding

class GridStyleAdapter(
    private var previews: List<Bitmap>,
    private var selectedIndex: Int,
    private val onSelected: (Int) -> Unit
) : RecyclerView.Adapter<GridStyleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGridStyleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    fun updatePreviews(newPreviews: List<Bitmap>) {
        previews = newPreviews
        notifyItemRangeChanged(0, previews.size)
    }

    inner class ViewHolder(private val binding: ItemGridStyleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.ivGridStylePreview.setImageBitmap(previews[position])

            val isSelected = position == selectedIndex
            binding.root.setBackgroundColor(
                binding.root.context.getColor(if (isSelected) R.color.accent_blue else R.color.black)
            )

            binding.root.setOnClickListener {
                onSelected(position)
            }
        }
    }
}
