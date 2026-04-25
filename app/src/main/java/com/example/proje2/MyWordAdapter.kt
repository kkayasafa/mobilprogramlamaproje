package com.example.proje2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proje2.databinding.ItemMyWordBinding
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

class MyWordAdapter(
    private val onDeleteClick: (MyWord) -> Unit,
    private val onEditClick: (MyWord) -> Unit
) : ListAdapter<MyWord, MyWordAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMyWordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(word: MyWord) {
            binding.tvEnglish.text = word.english
            binding.tvTurkish.text = word.turkish
            binding.tvType.text = word.type.uppercase()

            // Kelime türüne göre renkleri ayarla
            val colorRes = when (word.type.lowercase()) {
                "noun" -> R.color.type_noun
                "verb" -> R.color.type_verb
                "adjective" -> R.color.type_adj
                else -> R.color.type_noun
            }
            val bgRes = when (word.type.lowercase()) {
                "noun" -> R.color.type_noun_bg
                "verb" -> R.color.type_verb_bg
                "adjective" -> R.color.type_adj_bg
                else -> R.color.type_noun_bg
            }

            val color = ContextCompat.getColor(binding.root.context, colorRes)
            binding.tvType.setTextColor(color)
            binding.typeIndicator.backgroundTintList = ColorStateList.valueOf(color)
            binding.typeBadge.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, bgRes))

            // Öğrenilmiş kelime ise arka planı yeşil yap
            if (word.isLearned) {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.learned_bg))
            } else {
                // Varsayılan kart rengi (temaya göre değişebilir, beyaz varsayıyoruz)
                binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.bg_card))
            }

            binding.btnDelete.setOnClickListener { onDeleteClick(word) }
            binding.root.setOnClickListener { onEditClick(word) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MyWord>() {
        override fun areItemsTheSame(oldItem: MyWord, newItem: MyWord): Boolean = oldItem.english == newItem.english
        override fun areContentsTheSame(oldItem: MyWord, newItem: MyWord): Boolean = oldItem == newItem
    }
}