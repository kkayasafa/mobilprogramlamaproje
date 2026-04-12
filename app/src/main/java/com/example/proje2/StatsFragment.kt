package com.example.proje2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proje2.databinding.FragmentStatsBinding

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStatistics()
    }

    private fun updateStatistics() {
        val binding = _binding ?: return
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val learnedSet = sharedPref.getStringSet("ogrenilenler", setOf()) ?: setOf()
        
        val totalLearned = learnedSet.size
        val maxWords = 150

        // 1. Genel İlerleme
        binding.tvTotalCount.text = "$totalLearned / $maxWords"
        binding.circularProgress.max = maxWords
        binding.circularProgress.setProgress(totalLearned, true)

        // 2. Kelime Anatomisi (Sıfat, İsim, Fiil Dağılımı)
        calculateWordAnatomy(learnedSet)

        // 3. Seviye Bazlı İlerleme
        setupLevelProgress(totalLearned)
    }

    private fun calculateWordAnatomy(learnedSet: Set<String>) {
        if (learnedSet.isEmpty()) {
            updateAnatomyUI(mapOf())
            updateLevelProgressUI(mapOf())
            return
        }
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val wordTypes = mutableMapOf<String, Int>()
        val levelCounts = mutableMapOf<String, Int>()
        var processedLevels = 0

        levels.forEach { level ->
            db.collection("words_$level").get().addOnSuccessListener { documents ->
                var countForThisLevel = 0
                documents.forEach { doc ->
                    val word = doc.toObject(Word::class.java)
                    if (learnedSet.contains(word.english)) {
                        val type = word.englishType.lowercase().trim()
                        wordTypes[type] = wordTypes.getOrDefault(type, 0) + 1
                        countForThisLevel++
                    }
                }
                levelCounts[level] = countForThisLevel
                
                processedLevels++
                if (processedLevels == levels.size) {
                    updateAnatomyUI(wordTypes)
                    updateLevelProgressUI(levelCounts)
                }
            }.addOnFailureListener {
                processedLevels++
                if (processedLevels == levels.size) {
                    updateAnatomyUI(wordTypes)
                    updateLevelProgressUI(levelCounts)
                }
            }
        }
    }

    private fun updateLevelProgressUI(levelCounts: Map<String, Int>) {
        val binding = _binding ?: return
        
        val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        levels.forEach { level ->
            val count = levelCounts.getOrDefault(level, 0)
            val maxPerLevel = 25 // Varsayılan hedef her seviye için 25 kelime

            when (level) {
                "A1" -> {
                    binding.progressA1.max = maxPerLevel
                    binding.progressA1.setProgress(count, true)
                    binding.valA1.text = "$count / $maxPerLevel"
                }
                "A2" -> {
                    binding.progressA2.max = maxPerLevel
                    binding.progressA2.setProgress(count, true)
                    binding.valA2.text = "$count / $maxPerLevel"
                }
                "B1" -> {
                    binding.progressB1.max = maxPerLevel
                    binding.progressB1.setProgress(count, true)
                    binding.valB1.text = "$count / $maxPerLevel"
                }
                "B2" -> {
                    binding.progressB2.max = maxPerLevel
                    binding.progressB2.setProgress(count, true)
                    binding.valB2.text = "$count / $maxPerLevel"
                }
                "C1" -> {
                    binding.progressC1.max = maxPerLevel
                    binding.progressC1.setProgress(count, true)
                    binding.valC1.text = "$count / $maxPerLevel"
                }
                "C2" -> {
                    binding.progressC2.max = maxPerLevel
                    binding.progressC2.setProgress(count, true)
                    binding.valC2.text = "$count / $maxPerLevel"
                }
            }
        }
    }

    private fun updateAnatomyUI(types: Map<String, Int>) {
        val binding = _binding ?: return
        
        val nouns = types.getOrDefault("noun", 0)
        val verbs = types.getOrDefault("verb", 0)
        val adjectives = types.getOrDefault("adjective", 0)
        val total = nouns + verbs + adjectives
        
        if (total > 0) {
            binding.progressNoun.max = total
            binding.progressNoun.setProgress(nouns, true)
            
            binding.progressVerb.max = total
            binding.progressVerb.setProgress(verbs, true)
            
            binding.progressAdjective.max = total
            binding.progressAdjective.setProgress(adjectives, true)
        } else {
            binding.progressNoun.setProgress(0, true)
            binding.progressVerb.setProgress(0, true)
            binding.progressAdjective.setProgress(0, true)
        }
    }

    private fun setupLevelProgress(total: Int) {
        // Artık calculateWordAnatomy içinden dinamik olarak güncelleniyor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}