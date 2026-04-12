package com.example.proje2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proje2.databinding.ActivityWordListBinding
import com.example.proje2.databinding.DialogAddWordBinding
import com.google.firebase.firestore.FirebaseFirestore

class WordListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordListBinding
    private val db = FirebaseFirestore.getInstance()
    private val wordList = mutableListOf<Word>()
    private lateinit var adapter: WordAdapter
    private var currentLevel: String = "A1"
    private var collectionName: String = "words_A1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLevel = intent.getStringExtra("LEVEL") ?: "A1"
        collectionName = "words_$currentLevel"
        
        supportActionBar?.title = "$currentLevel Level Words"

        setupRecyclerView()
        fetchWords()

        binding.fabAddWord.setOnClickListener {
            showWordDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = WordAdapter(wordList, 
            onEditClick = { word -> showWordDialog(word) },
            onDeleteClick = { word -> deleteWord(word) }
        )
        binding.rvWords.layoutManager = LinearLayoutManager(this)
        binding.rvWords.adapter = adapter
    }

    private fun fetchWords() {
        db.collection(collectionName)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Error fetching words: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                wordList.clear()
                value?.documents?.forEach { doc ->
                    val word = doc.toObject(Word::class.java)
                    if (word != null) {
                        word.id = doc.id
                        wordList.add(word)
                    }
                }
                adapter.updateWords(wordList)
            }
    }

    private fun showWordDialog(word: Word?) {
        val dialogBinding = DialogAddWordBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)

        if (word != null) {
            dialogBinding.tvDialogTitle.text = "Update Word"
            dialogBinding.etEnglish.setText(word.english)
            dialogBinding.etEnglishType.setText(word.englishType)
            dialogBinding.etEnglishSentence.setText(word.englishSentence)
            dialogBinding.etTurkish.setText(word.turkish)
            dialogBinding.etTurkishType.setText(word.turkishType)
            dialogBinding.etTurkishSentence.setText(word.turkishSentence)
            dialogBinding.etLevel.setText(word.level)
        } else {
            dialogBinding.etLevel.setText(currentLevel)
        }

        builder.setPositiveButton(if (word == null) "Add" else "Update") { _, _ ->
            val english = dialogBinding.etEnglish.text.toString()
            val englishType = dialogBinding.etEnglishType.text.toString()
            val englishSentence = dialogBinding.etEnglishSentence.text.toString()
            val turkish = dialogBinding.etTurkish.text.toString()
            val turkishType = dialogBinding.etTurkishType.text.toString()
            val turkishSentence = dialogBinding.etTurkishSentence.text.toString()
            val level = dialogBinding.etLevel.text.toString().ifEmpty { currentLevel }

            if (english.isNotEmpty() && turkish.isNotEmpty()) {
                val newWord = Word(
                    id = word?.id ?: "",
                    english = english,
                    englishType = englishType,
                    englishSentence = englishSentence,
                    level = level,
                    turkish = turkish,
                    turkishType = turkishType,
                    turkishSentence = turkishSentence
                )
                if (word == null) {
                    addWord(newWord)
                } else {
                    updateWord(word.id, newWord)
                }
            } else {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun addWord(word: Word) {
        db.collection(collectionName).add(word)
            .addOnSuccessListener {
                Toast.makeText(this, "Word added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add word", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateWord(id: String, word: Word) {
        db.collection(collectionName).document(id).set(word)
            .addOnSuccessListener {
                Toast.makeText(this, "Word updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteWord(word: Word) {
        AlertDialog.Builder(this)
            .setTitle("Delete Word")
            .setMessage("Are you sure you want to delete '${word.english}'?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection(collectionName).document(word.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Word deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
