package com.example.proje2

import android.view.LayoutInflater
import android.view.ViewGroup
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
        holder.binding.tvEnglishWord.text = word.english
        holder.binding.tvTurkishWord.text = word.turkish
        holder.binding.tvWordType.text = word.englishType
    }

    override fun getItemCount(): Int = words.size

    fun updateList(newWords: List<Word>) {
        words = newWords
        notifyDataSetChanged()
    }
}