package com.example.proje2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proje2.databinding.ItemLearnedWordBinding

class LearnedWordAdapter(private var words: List<Word>) :
    RecyclerView.Adapter<LearnedWordAdapter.WordViewHolder>() {

    class WordViewHolder(val binding: ItemLearnedWordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemLearnedWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        val context = holder.itemView.context
        
        holder.binding.apply {
            tvEnglishWord.text = word.english
            tvTurkishWord.text = word.turkish
            tvWordType.text = word.englishType

            // Renklendirme mantığı
            val typeColor = when (word.englishType?.lowercase()) {
                "noun" -> R.color.type_noun
                "verb" -> R.color.type_verb
                "adjective" -> R.color.type_adj
                else -> R.color.slate_dark
            }
            tvWordType.setTextColor(ContextCompat.getColor(context, typeColor))
        }
    }

    override fun getItemCount(): Int = words.size

    fun updateList(newWords: List<Word>) {
        words = newWords
        notifyDataSetChanged()
    }
}