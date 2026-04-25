package com.example.proje2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.ArrayAdapter
import com.example.proje2.databinding.FragmentMyWordsBinding
import com.example.proje2.databinding.DialogMyWordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class MyWordsFragment : Fragment() {

    private var _binding: FragmentMyWordsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: MyWordAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadMyWords()

        binding.btnBack.setOnClickListener {
            (activity as? UserActivity)?.navigateBack()
        }

        binding.fabAddWord.setOnClickListener {
            showAddWordDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = MyWordAdapter(
            onDeleteClick = { word -> deleteWord(word) },
            onEditClick = { word -> showEditWordDialog(word) }
        )
        binding.rvMyWords.layoutManager = LinearLayoutManager(context)
        binding.rvMyWords.adapter = adapter
    }

    private fun loadMyWords() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val wordsList = document.get("myWords") as? List<Map<String, Any>> ?: emptyList()
                val words = wordsList.map { map ->
                    MyWord(
                        english = map["english"] as? String ?: "",
                        turkish = map["turkish"] as? String ?: "",
                        type = map["type"] as? String ?: "noun",
                        englishType = map["englishType"] as? String ?: "",
                        turkishType = map["turkishType"] as? String ?: "",
                        englishSentence = map["englishSentence"] as? String ?: "",
                        turkishSentence = map["turkishSentence"] as? String ?: "",
                        isLearned = (map["isLearned"] as? Boolean) ?: false
                    )
                }
                adapter.submitList(words)
            }
    }

    private fun showAddWordDialog() {
        val dialogBinding = DialogMyWordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Dropdown menü seçenekleri (Sadece Noun, Verb, Adjective)
        val types = arrayOf("noun", "verb", "adjective")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        dialogBinding.actvEnglishType.setAdapter(adapter)

        // İngilizce tür seçildiğinde Türkçesini otomatik ayarla
        dialogBinding.actvEnglishType.setOnItemClickListener { _, _, position, _ ->
            val selectedType = types[position]
            val turkishType = when (selectedType) {
                "noun" -> "isim"
                "verb" -> "fiil"
                "adjective" -> "sıfat"
                else -> selectedType
            }
            dialogBinding.etTurkishType.setText(turkishType)
            // Hata uyarısını temizle
            dialogBinding.tilEnglishType.error = null
            dialogBinding.tilTurkishType.error = null
        }

        dialogBinding.btnSave.setOnClickListener {
            if (validateInputs(dialogBinding)) {
                val eng = dialogBinding.etEnglish.text.toString().trim()
                val tr = dialogBinding.etTurkish.text.toString().trim()
                val engType = dialogBinding.actvEnglishType.text.toString().trim()
                val trType = dialogBinding.etTurkishType.text.toString().trim()
                val engSentence = dialogBinding.etEnglishSentence.text.toString().trim()
                val trSentence = dialogBinding.etTurkishSentence.text.toString().trim()

                val wordMap = mapOf(
                    "english" to eng,
                    "turkish" to tr,
                    "englishType" to engType,
                    "turkishType" to trType,
                    "englishSentence" to engSentence,
                    "turkishSentence" to trSentence,
                    "type" to engType,
                    "isLearned" to false
                )
                addWordToFirebase(wordMap)
                dialog.dismiss()
            }
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun validateInputs(binding: DialogMyWordBinding): Boolean {
        var isValid = true

        if (binding.etEnglish.text.toString().trim().isEmpty()) {
            binding.tilEnglish.error = "Bu alan boş bırakılamaz"
            isValid = false
        } else binding.tilEnglish.error = null

        if (binding.actvEnglishType.text.toString().trim().isEmpty()) {
            binding.tilEnglishType.error = "Lütfen bir tür seçin"
            isValid = false
        } else binding.tilEnglishType.error = null

        if (binding.etEnglishSentence.text.toString().trim().isEmpty()) {
            binding.tilEnglishSentence.error = "Örnek cümle gereklidir"
            isValid = false
        } else binding.tilEnglishSentence.error = null

        if (binding.etTurkish.text.toString().trim().isEmpty()) {
            binding.tilTurkish.error = "Bu alan boş bırakılamaz"
            isValid = false
        } else binding.tilTurkish.error = null

        if (binding.etTurkishSentence.text.toString().trim().isEmpty()) {
            binding.tilTurkishSentence.error = "Cümle çevirisi gereklidir"
            isValid = false
        } else binding.tilTurkishSentence.error = null

        return isValid
    }

    private fun addWordToFirebase(wordMap: Map<String, Any>) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId)
            .update("myWords", FieldValue.arrayUnion(wordMap))
            .addOnSuccessListener {
                loadMyWords()
                Toast.makeText(context, "Kelime eklendi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteWord(word: MyWord) {
        val userId = auth.currentUser?.uid ?: return
        // Silme işlemi için tam eşleşme gerekir ya da id bazlı sistem kurulmalı. 
        // Mevcut array yapısında tam map verilmeli.
        val wordMap = mapOf(
            "english" to word.english,
            "turkish" to word.turkish,
            "englishType" to word.englishType,
            "turkishType" to word.turkishType,
            "englishSentence" to word.englishSentence,
            "turkishSentence" to word.turkishSentence,
            "type" to word.type,
            "isLearned" to word.isLearned
        )
        
        db.collection("users").document(userId)
            .update("myWords", FieldValue.arrayRemove(wordMap))
            .addOnSuccessListener {
                loadMyWords()
            }
    }

    private fun showEditWordDialog(oldWord: MyWord) {
        // Düzenleme mantığı eklenebilir
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class MyWord(
    val english: String = "",
    val turkish: String = "",
    val type: String = "noun",
    val englishType: String = "",
    val turkishType: String = "",
    val englishSentence: String = "",
    val turkishSentence: String = "",
    val isLearned: Boolean = false
)
