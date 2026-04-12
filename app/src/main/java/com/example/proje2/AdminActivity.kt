package com.example.proje2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.proje2.databinding.ActivityAdminBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verileri bir kez yüklemek için SharedPreferences kullanıyoruz
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isDataLoaded = sharedPref.getBoolean("isDataLoaded", false)

        if (!isDataLoaded) {
            uploadAllWords()
            with(sharedPref.edit()) {
                putBoolean("isDataLoaded", true)
                apply()
            }
        }

        setupButtons()
    }

    private fun setupButtons() {
        val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val buttons = listOf(
            binding.btnA1Level, binding.btnA2Level,
            binding.btnB1Level, binding.btnB2Level,
            binding.btnC1Level, binding.btnC2Level
        )

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val intent = Intent(this, WordListActivity::class.java)
                intent.putExtra("LEVEL", levels[index])
                startActivity(intent)
            }
        }
    }

    private fun uploadAllWords() {
        uploadWordsFromJson(R.raw.words_a1, "words_A1")
        uploadWordsFromJson(R.raw.words_a2, "words_A2")
        uploadWordsFromJson(R.raw.words_b1, "words_B1")
        uploadWordsFromJson(R.raw.words_b2, "words_B2")
        uploadWordsFromJson(R.raw.words_c1, "words_C1")
        uploadWordsFromJson(R.raw.words_c2, "words_C2")
    }

    private fun uploadWordsFromJson(resourceId: Int, collectionName: String) {
        try {
            val jsonString = resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            var batch = db.batch()
            var count = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val word = hashMapOf(
                    "english" to obj.getString("english"),
                    "englishType" to obj.getString("englishType"),
                    "englishSentence" to obj.getString("englishSentence"),
                    "level" to obj.getString("level"),
                    "turkish" to obj.getString("turkish"),
                    "turkishType" to obj.getString("turkishType"),
                    "turkishSentence" to obj.getString("turkishSentence")
                )

                val docRef = db.collection(collectionName).document()
                batch.set(docRef, word)
                count++

                if (count == 500) {
                    batch.commit()
                    batch = db.batch()
                    count = 0
                }
            }

            if (count > 0) {
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Batch upload successful for $collectionName")
                    }
            }
        } catch (e: Exception) {
            Log.e("JSON", "Error reading JSON", e)
        }
    }
}
