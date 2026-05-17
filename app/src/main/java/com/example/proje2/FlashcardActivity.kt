package com.example.proje2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.proje2.databinding.ActivityFlashcardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class FlashcardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashcardBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var wordList = mutableListOf<Word>()
    private var currentIndex = 0
    private var isFrontVisible = true
    private var isAnimating = false

    private var dX = 0f
    private var initialX = 0f
    private var isGestureStarted = false 
    private var level = "A1"
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Veri gelene kadar default XML metinlerini göstermemek için kartı gizle.
        binding.cardContainer.visibility = View.INVISIBLE

        level = intent.getStringExtra("LEVEL") ?: "A1"
        binding.tvLevelTitle.text = "$level SEVİYESİ"

        val scale = resources.displayMetrics.density * 8000
        binding.flashcard.cameraDistance = scale

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupSwipeAndClickListener()
        fetchWords()
        binding.btnResetLevel.setOnClickListener {
            resetLevelProgress()
        }

        binding.btnSpeak.setOnClickListener {
            playCurrentWordSound()
        }
    }

    private fun playCurrentWordSound() {
        if (currentIndex >= wordList.size) return
        val word = wordList[currentIndex]
        val wordLevel = word.level?.uppercase() ?: level.uppercase()
        val soundPath = "word_sounds/$wordLevel/${word.english.lowercase()}.mp3"

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                val descriptor = assets.openFd(soundPath)
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                descriptor.close()
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetLevelProgress() {
        val user = auth.currentUser ?: return
        val progressRef = db.collection("users").document(user.uid)

        if (level == "MY_WORDS") {
            // Kelime Defterim sıfırlama (isLearned = false)
            progressRef.get().addOnSuccessListener { document ->
                val myWords = document.get("myWords") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                val updatedMyWords = myWords.map { word ->
                    val mutableWord = word.toMutableMap()
                    mutableWord["isLearned"] = false
                    mutableWord
                }
                progressRef.update("myWords", updatedMyWords)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Defteriniz sıfırlandı!", Toast.LENGTH_SHORT).show()
                        fetchWords() // Yeniden yükle
                    }
            }
        } else {
            // Normal Seviye sıfırlama (learnedWords içinden bu level'ın kelimelerini sil)
            progressRef.get().addOnSuccessListener { document ->
                val learnedWords = document.get("learnedWords") as? List<String> ?: emptyList()
                
                // Bu level'a ait kelimeleri bulmak için words_$level koleksiyonunu kullanıyoruz
                db.collection("words_$level").get()
                    .addOnSuccessListener { allWordsSnapshot ->
                        val levelWordEnglishes = allWordsSnapshot.documents.mapNotNull { it.getString("english") }
                        val updatedLearnedWords = learnedWords.filterNot { it in levelWordEnglishes }
                        
                        progressRef.update("learnedWords", updatedLearnedWords)
                            .addOnSuccessListener {
                                Toast.makeText(this, "$level Seviyesi sıfırlandı!", Toast.LENGTH_SHORT).show()
                                fetchWords() // Yeniden yükle
                            }
                    }
            }
        }
    }

    private fun setupSwipeAndClickListener() {
        binding.flashcard.setOnTouchListener { view, event ->
            if (isAnimating) return@setOnTouchListener true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    dX = view.translationX - event.rawX
                    isGestureStarted = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isGestureStarted) return@setOnTouchListener true
                    val newX = event.rawX + dX
                    view.translationX = newX
                    view.rotation = (event.rawX - initialX) / 25
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isGestureStarted) return@setOnTouchListener true
                    isGestureStarted = false
                    
                    val deltaX = event.rawX - initialX
                    val swipeThreshold = view.width * 0.40f

                    if (abs(deltaX) > swipeThreshold) {
                        swipeCardAway(deltaX > 0)
                    } else {
                        if (abs(deltaX) < 20) {
                            flipCard()
                        } else {
                            view.animate()
                                .translationX(0f)
                                .rotation(0f)
                                .setDuration(250)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun flipCard() {
        isAnimating = true
        val startRotation = 0f
        val endRotation = 90f

        val anim1 = ObjectAnimator.ofFloat(binding.flashcard, "rotationY", startRotation, endRotation)
        anim1.duration = 150
        anim1.interpolator = AccelerateInterpolator()

        anim1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (isFrontVisible) {
                    binding.layoutFront.visibility = View.GONE
                    binding.layoutBack.visibility = View.VISIBLE
                } else {
                    binding.layoutFront.visibility = View.VISIBLE
                    binding.layoutBack.visibility = View.GONE
                }
                isFrontVisible = !isFrontVisible
                
                binding.flashcard.rotationY = -90f
                val anim2 = ObjectAnimator.ofFloat(binding.flashcard, "rotationY", -90f, 0f)
                anim2.duration = 150
                anim2.interpolator = DecelerateInterpolator()
                anim2.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isAnimating = false
                    }
                })
                anim2.start()
            }
        })
        anim1.start()
    }

    private fun swipeCardAway(isRight: Boolean) {
        isAnimating = true
        val translationX = if (isRight) widthValue() * 1.5f else -widthValue() * 1.5f

        binding.flashcard.animate()
            .translationX(translationX)
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isRight) markAsLearned()
                    showNextWord()
                }
            }).start()
    }

    private fun widthValue(): Float = resources.displayMetrics.widthPixels.toFloat()

    private fun showNextWord() {
        currentIndex++
        if (currentIndex < wordList.size) {
            updateCardUI()
            binding.flashcard.animate().cancel()
            binding.flashcard.apply {
                translationX = 0f
                rotation = 0f
                rotationY = 0f
                alpha = 1f
                visibility = View.VISIBLE
            }
            binding.layoutFront.visibility = View.VISIBLE
            binding.layoutBack.visibility = View.GONE
            isFrontVisible = true
            isAnimating = false
            initialX = 0f
        } else {
            Toast.makeText(this, "Tüm kelimeler bitti!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun markAsLearned() {
        val currentUser = auth.currentUser ?: return
        if (currentIndex < wordList.size) {
            val word = wordList[currentIndex]
            
            if (level == "MY_WORDS") {
                // Kelime defterindeki kelimeyi ogrenildi olarak isaretle
                db.collection("users").document(currentUser.uid).get().addOnSuccessListener { doc ->
                    val myWords = doc.get("myWords") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                    val updatedWords = myWords.map { map ->
                        if (map["english"] == word.english) {
                            map.toMutableMap().apply { put("isLearned", true) }
                        } else {
                            map
                        }
                    }
                    db.collection("users").document(currentUser.uid).update("myWords", updatedWords)
                }
            } else {
                // Normal seviye kelimesini ogrenildi olarak isaretle
                db.collection("users").document(currentUser.uid)
                    .update("learnedWords", FieldValue.arrayUnion(word.english))
            }
        }
    }

    private fun finalizeWordList(learnedCount: Int, totalInLevel: Int) {
        if (wordList.isEmpty()) {
            binding.cardContainer.visibility = View.GONE
            binding.instructionText.visibility = View.GONE
            binding.layoutFinished.visibility = View.VISIBLE
            
            if (totalInLevel == 0 && level == "MY_WORDS") {
                binding.tvFinishedMessage.text = "Henüz kelime defterine kelime eklememişsin."
                binding.btnResetLevel.visibility = View.GONE
            } else {
                binding.tvFinishedMessage.text = "Tebrikler! $level seviyesindeki tüm kelimeleri bitirdin."
                binding.btnResetLevel.visibility = View.VISIBLE
            }
        } else {
            binding.cardContainer.visibility = View.VISIBLE
            binding.instructionText.visibility = View.VISIBLE
            binding.layoutFinished.visibility = View.GONE
            currentIndex = 0
            updateCardUI()
        }
    }

    private fun fetchWords() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                if (level == "MY_WORDS") {
                    binding.tvLevelTitle.text = "KELİME DEFTERİM"
                    val myWordsList = userDoc.get("myWords") as? List<Map<String, Any>> ?: emptyList()
                    val learnedCount = myWordsList.count { it["isLearned"] == true }
                    
                    wordList.clear()
                    for (map in myWordsList) {
                        val isLearned = map["isLearned"] as? Boolean ?: false
                        if (!isLearned) {
                            wordList.add(Word(
                                english = map["english"] as? String ?: "",
                                turkish = map["turkish"] as? String ?: "",
                                englishType = map["englishType"] as? String ?: "",
                                turkishType = map["turkishType"] as? String ?: "",
                                englishSentence = map["englishSentence"] as? String ?: "",
                                turkishSentence = map["turkishSentence"] as? String ?: "",
                                level = "MY_WORDS"
                            ))
                        }
                    }
                    finalizeWordList(learnedCount, myWordsList.size)
                } else {
                    val learnedWords = userDoc.get("learnedWords") as? List<String> ?: listOf()
                    db.collection("words_$level").get()
                        .addOnSuccessListener { documents ->
                            wordList.clear()
                            val allWordsCount = documents.size()
                            for (document in documents) {
                                val word = document.toObject(Word::class.java).apply { id = document.id }
                                if (!learnedWords.contains(word.english)) {
                                    wordList.add(word)
                                }
                            }
                            wordList.sortBy { it.english }
                            finalizeWordList(learnedWords.count { id -> documents.any { it.get("english") == id } }, allWordsCount)
                        }
                }
            }
    }

    private fun updateCardUI() {
        if (currentIndex >= wordList.size) return
        val word = wordList[currentIndex]
        
        binding.tvEnglishWord.text = word.english
        binding.tvWordTypeEnglish.text = word.englishType
        binding.tvEnglishSentence.text = getBoldSpannable(word.englishSentence, word.english)
        
        binding.tvTurkishWord.text = word.turkish
        binding.tvWordTypeTurkish.text = word.turkishType
        binding.tvTurkishSentence.text = getBoldSpannable(word.turkishSentence, word.turkish)
        
        binding.tvProgress.text = "${currentIndex + 1} / ${wordList.size}"

        // Görsel Yükleme Mantığı (Assets Klasöründen)
        val imagePath = "word_images/${level.uppercase()}/${word.english.lowercase()}.png"
        
        try {
            // Assets klasöründe dosya var mı kontrol edelim
            val inputStream = assets.open(imagePath)
            inputStream.close()
            
            // Dosya varsa Glide ile yükle
            binding.imageContainer.visibility = View.VISIBLE
            Glide.with(this)
                .load("file:///android_asset/$imagePath")
                .fitCenter()
                .into(binding.ivWordImage)
        } catch (e: Exception) {
            // Dosya yoksa konteynerı gizle
            binding.imageContainer.visibility = View.GONE
        }

        // Ses Dosyası Kontrolü
        val wordLevel = word.level?.uppercase() ?: level.uppercase()
        val soundPath = "word_sounds/$wordLevel/${word.english.lowercase()}.mp3"
        try {
            val inputStream = assets.open(soundPath)
            inputStream.close()
            binding.btnSpeak.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.btnSpeak.visibility = View.GONE
        }

        // Renklendirme mantığı
        val (typeColor, typeBg) = when (word.englishType?.lowercase()) {
            "noun" -> Pair(R.color.type_noun, R.color.type_noun_bg)
            "verb" -> Pair(R.color.type_verb, R.color.type_verb_bg)
            "adjective" -> Pair(R.color.type_adj, R.color.type_adj_bg)
            else -> Pair(R.color.accent_indigo, R.color.accent_light)
        }

        binding.tvWordTypeEnglish.setTextColor(ContextCompat.getColor(this, typeColor))
        binding.tvWordTypeEnglish.setBackgroundColor(ContextCompat.getColor(this, typeBg))
        
        binding.tvWordTypeTurkish.setTextColor(ContextCompat.getColor(this, typeColor))
        binding.tvWordTypeTurkish.setBackgroundColor(ContextCompat.getColor(this, typeBg))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun getBoldSpannable(sentence: String, wordToBold: String): SpannableString {
        val spannable = SpannableString(sentence)
        val startIndex = sentence.indexOf(wordToBold, ignoreCase = true)
        if (startIndex != -1) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                startIndex + wordToBold.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }
}
