package com.example.proje2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proje2.databinding.FragmentLogBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: LearnedWordAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabLayout()
        loadLearnedWords("A1")
    }

    private fun setupRecyclerView() {
        adapter = LearnedWordAdapter(emptyList())
        binding.rvLearnedWords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLearnedWords.adapter = adapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadLearnedWords(tab?.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadLearnedWords(level: String) {
        val binding = _binding ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.rvLearnedWords.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        val context = context ?: return
        val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val learnedSet = sharedPref.getStringSet("ogrenilenler", setOf()) ?: setOf()

        if (learnedSet.isEmpty()) {
            showEmptyState()
            return
        }

        db.collection("words_$level").get()
            .addOnSuccessListener { documents ->
                val binding = _binding ?: return@addOnSuccessListener
                val filteredList = documents.mapNotNull { it.toObject(Word::class.java) }
                    .filter { learnedSet.contains(it.english) }

                if (filteredList.isEmpty()) {
                    showEmptyState()
                } else {
                    adapter.updateList(filteredList)
                    binding.rvLearnedWords.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                val binding = _binding ?: return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmptyState() {
        val binding = _binding ?: return
        adapter.updateList(emptyList())
        binding.progressBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}