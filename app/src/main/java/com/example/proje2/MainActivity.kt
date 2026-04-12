package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proje2.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Firebase'i her şeyden önce başlat
        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Firestore uzerinden JSON koleksiyonlarini kontrol et ve gerekiyorsa yukle
        checkAndUploadWordCollections()

        binding.btnAdminMode.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        binding.btnUserMode.setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
        }
    }

    private data class WordFile(
        val level: String,
        val resId: Int,
        val collectionName: String
    )

    private fun checkAndUploadWordCollections() {
        val files = listOf(
            WordFile("A1", R.raw.words_a1, "words_A1"),
            WordFile("A2", R.raw.words_a2, "words_A2"),
            WordFile("B1", R.raw.words_b1, "words_B1"),
            WordFile("B2", R.raw.words_b2, "words_B2"),
            WordFile("C1", R.raw.words_c1, "words_C1"),
            WordFile("C2", R.raw.words_c2, "words_C2")
        )

        files.forEach { file ->
            db.collection(file.collectionName).limit(1).get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Log.d("Firestore", "${file.level} bos, JSON'dan yukleniyor...")
                        uploadWordsFromJson(file)
                    } else {
                        Log.d("Firestore", "${file.level} zaten dolu.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Baglanti hatasi (${file.level}): ${e.message}")
                }
        }
    }

    private fun uploadWordsFromJson(file: WordFile) {
        try {
            val jsonString = resources.openRawResource(file.resId).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() == 0) {
                Log.w("Firestore", "${file.level} JSON bos.")
                return
            }

            val tasks = mutableListOf<com.google.android.gms.tasks.Task<Void>>()
            var batch = db.batch()
            var opCount = 0

            // Firestore batch limiti 500 islem; guvende kalmak icin 400 kullaniyoruz.
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
                val docRef = db.collection(file.collectionName).document()
                batch.set(docRef, word)
                opCount++

                if (opCount == 400) {
                    tasks.add(batch.commit())
                    batch = db.batch()
                    opCount = 0
                }
            }

            if (opCount > 0) {
                tasks.add(batch.commit())
            }

            Tasks.whenAllComplete(tasks)
                .addOnSuccessListener {
                    Log.d("Firestore", "${file.level} yuklemesi basarili.")
                    Toast.makeText(this, "${file.level} yuklendi", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "${file.level} yukleme hatasi: ${e.message}")
                    Toast.makeText(this, "${file.level} yuklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e("Firestore", "JSON okuma hatasi (${file.level}): ${e.message}")
        }
    }
}
