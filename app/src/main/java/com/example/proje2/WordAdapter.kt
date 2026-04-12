package com.example.proje2

import android.view.LayoutInflater
import android.view.ViewGroup
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
        holder.binding.apply {
            tvEnglish.text = word.english
            tvEnglishType.text = "(${word.englishType})"
            tvEnglishSentence.text = word.englishSentence
            
            tvTurkish.text = word.turkish
            tvTurkishType.text = "(${word.turkishType})"
            tvTurkishSentence.text = word.turkishSentence
            
            tvLevel.text = word.level

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
