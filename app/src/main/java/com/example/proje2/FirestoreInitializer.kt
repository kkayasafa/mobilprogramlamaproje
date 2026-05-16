package com.example.proje2

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.io.InputStreamReader
import java.nio.charset.Charset
import kotlin.concurrent.thread

class FirestoreInitializer(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()

    fun checkAndInitializeData() {
        // Tamamen arka plan thread'inde çalıştırarak UI kilitlenmesini önlüyoruz.
        thread {
            Log.d("FirestoreInit", "Veri kontrol süreci arka planda başladı.")
            val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
            
            for (level in levels) {
                val collectionName = "words_$level"
                try {
                    // Tasks.await ile sorgu bitene kadar bu arka plan thread'ini bekletiyoruz.
                    // Bu sayede Main Thread (UI thread) asla meşgul edilmez.
                    val task = db.collection(collectionName).limit(1).get()
                    val snapshot = Tasks.await(task)

                    if (snapshot.isEmpty) {
                        Log.d("FirestoreInit", "$collectionName boş. Yükleniyor...")
                        uploadLevelDataSync(level)
                    } else {
                        Log.d("FirestoreInit", "$collectionName zaten veri içeriyor.")
                    }
                } catch (e: Exception) {
                    Log.e("FirestoreInit", "$collectionName kontrolünde hata: ${e.message}")
                }
            }
            Log.d("FirestoreInit", "Tüm seviyelerin kontrolü tamamlandı.")
        }
    }

    private fun uploadLevelDataSync(level: String) {
        val resourceName = "words_${level.lowercase()}"
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)

        if (resourceId == 0) {
            Log.e("FirestoreInit", "Kaynak bulunamadı: $resourceName")
            return
        }

        try {
            val inputStream = context.resources.openRawResource(resourceId)
            val reader = InputStreamReader(inputStream, Charset.forName("UTF-8"))
            val jsonString = reader.readText()
            val jsonArray = JSONArray(jsonString)

            val collectionName = "words_$level"
            val batch = db.batch()

            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                val english = jsonObj.optString("english", "")
                if (english.isEmpty()) continue

                // İngilizce kelimeyi döküman ID'si olarak kullanıyoruz
                val docRef = db.collection(collectionName).document(english.lowercase())
                
                val wordData = hashMapOf(
                    "english" to english,
                    "englishType" to jsonObj.optString("englishType", ""),
                    "englishSentence" to jsonObj.optString("englishSentence", ""),
                    "level" to jsonObj.optString("level", level),
                    "turkish" to jsonObj.optString("turkish", ""),
                    "turkishType" to jsonObj.optString("turkishType", ""),
                    "turkishSentence" to jsonObj.optString("turkishSentence", "")
                )
                batch.set(docRef, wordData)
            }

            // Batch işlemini de senkron olarak bekleyerek tamamlıyoruz.
            Tasks.await(batch.commit())
            Log.d("FirestoreInit", "$collectionName başarıyla yüklendi.")

        } catch (e: Exception) {
            Log.e("FirestoreInit", "JSON okuma veya yükleme hatası ($level): ${e.message}")
        }
    }
}
