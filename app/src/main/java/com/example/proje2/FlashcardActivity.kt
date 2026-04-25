package com.example.proje2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Typeface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        level = intent.getStringExtra("LEVEL") ?: "A1"
        binding.tvLevelTitle.text = "$level SEVİYESİ"

        val scale = resources.displayMetrics.density * 8000
        binding.flashcard.cameraDistance = scale

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupSwipeAndClickListener()
        fetchWords()
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

    private fun fetchWords() {
        val currentUser = auth.currentUser ?: return
        
        if (level == "MY_WORDS") {
            binding.tvLevelTitle.text = "KELİME DEFTERİM"
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { userDoc ->
                    val myWordsList = userDoc.get("myWords") as? List<Map<String, Any>> ?: emptyList()
                    wordList.clear()
                    for (map in myWordsList) {
                        val isLearned = map["isLearned"] as? Boolean ?: false
                        if (!isLearned) {
                            val word = Word(
                                english = map["english"] as? String ?: "",
                                turkish = map["turkish"] as? String ?: "",
                                englishType = map["englishType"] as? String ?: "",
                                turkishType = map["turkishType"] as? String ?: "",
                                englishSentence = map["englishSentence"] as? String ?: "",
                                turkishSentence = map["turkishSentence"] as? String ?: "",
                                level = "MY_WORDS"
                            )
                            wordList.add(word)
                        }
                    }
                    wordList.shuffle()
                    if (wordList.isNotEmpty()) {
                        updateCardUI()
                    } else {
                        Toast.makeText(this, "Kelime defteriniz henüz boş!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            return
        }

        // Once kullanicinin ogrendigi kelimeleri cek
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDoc ->
                val learnedWords = userDoc.get("learnedWords") as? List<String> ?: listOf()
                
                // Sonra ilgili seviyedeki kelimeleri cek
                db.collection("words_$level").get()
                    .addOnSuccessListener { documents ->
                        wordList.clear()
                        for (document in documents) {
                            val word = document.toObject(Word::class.java).apply { id = document.id }
                            if (!learnedWords.contains(word.english)) {
                                wordList.add(word)
                            }
                        }
                        wordList.shuffle()
                        if (wordList.isNotEmpty()) {
                            updateCardUI()
                        } else {
                            Toast.makeText(this, "Bu seviyede yeni kelime kalmadı!", Toast.LENGTH_SHORT).show()
                            finish()
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
