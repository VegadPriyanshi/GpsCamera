package com.gps.camera.map.photo.gpscamera.features.language.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gps.camera.map.photo.gpscamera.databinding.ItemLanguageBinding
import com.gps.camera.map.photo.gpscamera.features.language.data.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.VH>() {

    private var selectedPosition = -1

    inner class VH(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val language = languages[position]
        val isSelected = position == selectedPosition
        holder.binding.tvLanguageName.text = language.displayName
        holder.binding.tvLanguageNative.text = language.nativeName
        holder.binding.root.isSelected = isSelected
        holder.binding.root.setBackgroundResource(
            if (isSelected) com.gps.camera.map.photo.gpscamera.R.drawable.bg_language_item_selected
            else com.gps.camera.map.photo.gpscamera.R.drawable.bg_language_item
        )
        holder.binding.ivRadio.setImageResource(
            if (isSelected) com.gps.camera.map.photo.gpscamera.R.drawable.ic_radio_checked
            else com.gps.camera.map.photo.gpscamera.R.drawable.ic_radio_unchecked
        )
        holder.binding.root.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onSelected(language)
        }
    }

    override fun getItemCount(): Int = languages.size
}
