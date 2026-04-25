package com.example.proje2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proje2.databinding.FragmentLogBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: LearnedWordAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMenu.setOnClickListener {
            activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.openDrawer(GravityCompat.END)
        }

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
        val currentUser = auth.currentUser ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.rvLearnedWords.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        // 1. Kullanicinin ogrenilenler listesini Firestore'dan cek
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                if (isAdded && userDoc.exists()) {
                    val learnedWords = userDoc.get("learnedWords") as? List<String> ?: listOf()
                    
                    if (learnedWords.isEmpty()) {
                        showEmptyState()
                        return@addOnSuccessListener
                    }

                    // 2. Ilgili seviyedeki kelimeleri cek ve kullanicinin listesiyle karsilastir
                    db.collection("words_$level").get()
                        .addOnSuccessListener { documents ->
                            if (isAdded) {
                                val filteredList = documents.mapNotNull { it.toObject(Word::class.java) }
                                    .filter { learnedWords.contains(it.english) }

                                if (filteredList.isEmpty()) {
                                    showEmptyState()
                                } else {
                                    adapter.updateList(filteredList)
                                    binding.rvLearnedWords.visibility = View.VISIBLE
                                    binding.progressBar.visibility = View.GONE
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showEmptyState() {
        adapter.updateList(emptyList())
        binding.progressBar.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
