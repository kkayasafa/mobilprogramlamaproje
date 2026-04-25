package com.example.proje2

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proje2.databinding.ItemWordBinding

class WordAdapter(
    private var words: List<Word>,
    private val onEditClick: (Word) -> Unit,
    private val onDeleteClick: (Word) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    class WordViewHolder(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        val context = holder.itemView.context
        holder.binding.apply {
            tvEnglish.text = word.english
            tvEnglishType.text = "(${word.englishType})"
            tvEnglishSentence.text = word.englishSentence
            
            tvTurkish.text = word.turkish
            tvTurkishType.text = "(${word.turkishType})"
            tvTurkishSentence.text = word.turkishSentence
            
            tvLevel.text = word.level

            // Renklendirme mantığı
            val (typeColor, typeBg) = when (word.englishType?.lowercase()) {
                "noun" -> Pair(R.color.type_noun, R.color.type_noun_bg)
                "verb" -> Pair(R.color.type_verb, R.color.type_verb_bg)
                "adjective" -> Pair(R.color.type_adj, R.color.type_adj_bg)
                else -> Pair(R.color.slate_dark, R.color.slate_light)
            }

            tvEnglishType.setTextColor(ContextCompat.getColor(context, typeColor))
            tvTurkishType.setTextColor(ContextCompat.getColor(context, typeColor))

            btnEdit.setOnClickListener { onEditClick(word) }
            btnDelete.setOnClickListener { onDeleteClick(word) }
        }
    }

    override fun getItemCount(): Int = words.size

    fun updateWords(newWords: List<Word>) {
        words = newWords
        notifyDataSetChanged()
    }
}
