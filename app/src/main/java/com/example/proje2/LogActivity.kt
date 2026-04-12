package com.example.proje2

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proje2.databinding.ActivityLogBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: LearnedWordAdapter
    private val allLearnedWords = mutableListOf<Word>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTabLayout()
        
        // İlk açılışta A1 verilerini getir
        loadLearnedWords("A1")
    }

    private fun setupRecyclerView() {
        adapter = LearnedWordAdapter(emptyList())
        binding.rvLearnedWords.layoutManager = LinearLayoutManager(this)
        binding.rvLearnedWords.adapter = adapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val level = tab?.text.toString()
                loadLearnedWords(level)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadLearnedWords(level: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvLearnedWords.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val learnedSet = sharedPref.getStringSet("ogrenilenler", setOf()) ?: setOf()

        if (learnedSet.isEmpty()) {
            showEmptyState()
            return
        }

        db.collection("words_$level")
            .get()
            .addOnSuccessListener { documents ->
                val filteredList = mutableListOf<Word>()
                for (document in documents) {
                    val word = document.toObject(Word::class.java)
                    if (learnedSet.contains(word.english)) {
                        filteredList.add(word)
                    }
                }

                if (filteredList.isEmpty()) {
                    showEmptyState()
                } else {
                    adapter.updateList(filteredList)
                    binding.rvLearnedWords.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmptyState() {
        adapter.updateList(emptyList())
        binding.progressBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }
}