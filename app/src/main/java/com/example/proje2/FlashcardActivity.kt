package com.example.proje2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
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
import com.example.proje2.databinding.ActivityFlashcardBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class FlashcardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashcardBinding
    private val db = FirebaseFirestore.getInstance()
    private var wordList = mutableListOf<Word>()
    private var currentIndex = 0
    private var isFrontVisible = true
    private var isAnimating = false

    private var dX = 0f
    private var initialX = 0f
    private var isGestureStarted = false // Gesture güvenliği için
    private var level = "A1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFlashcardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        level = intent.getStringExtra("LEVEL") ?: "A1"
        binding.tvLevelTitle.text = "$level SEVİYESİ"

        // 3D derinlik efekti
        val scale = resources.displayMetrics.density * 8000
        binding.flashcard.cameraDistance = scale

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
                    val swipeThreshold = view.width * 0.40f // %40 hassasiyet

                    if (abs(deltaX) > swipeThreshold) {
                        swipeCardAway(deltaX > 0)
                    } else {
                        // Vazgeçme veya Tıklama
                        if (abs(deltaX) < 20) {
                            flipCard() // Tıklama algılandı
                        } else {
                            // Merkeze geri dön (Vazgeçti)
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
            // UI Güncelle
            updateCardUI()
            
            // Kartı FABRİKA ayarlarına döndür
            binding.flashcard.animate().cancel() // Önceki animasyonları durdur
            binding.flashcard.apply {
                translationX = 0f
                rotation = 0f
                rotationY = 0f
                alpha = 1f
                visibility = View.VISIBLE
            }
            
            // Görünürlükleri kesinleştir
            binding.layoutFront.visibility = View.VISIBLE
            binding.layoutBack.visibility = View.GONE
            isFrontVisible = true
            isAnimating = false
            initialX = 0f // Koordinatı sıfırla
        } else {
            Toast.makeText(this, "Tüm kelimeler bitti!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun markAsLearned() {
        if (currentIndex < wordList.size) {
            val word = wordList[currentIndex]
            val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val learnedSet = sharedPref.getStringSet("ogrenilenler", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            learnedSet.add(word.english)
            sharedPref.edit().putStringSet("ogrenilenler", learnedSet).apply()
        }
    }

    private fun fetchWords() {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val learnedSet = sharedPref.getStringSet("ogrenilenler", setOf()) ?: setOf()

        db.collection("words_$level").get()
            .addOnSuccessListener { documents ->
                wordList.clear()
                for (document in documents) {
                    val word = document.toObject(Word::class.java).apply { id = document.id }
                    if (!learnedSet.contains(word.english)) {
                        wordList.add(word)
                    }
                }
                wordList.shuffle()
                if (wordList.isNotEmpty()) {
                    updateCardUI()
                } else {
                    Toast.makeText(this, "Yeni kelime yok!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun updateCardUI() {
        if (currentIndex >= wordList.size) return
        val word = wordList[currentIndex]
        binding.tvEnglishWord.text = word.english
        binding.tvWordTypeEnglish.text = word.englishType
        
        // İngilizce Cümlede Kelimeyi Kalın Yap
        binding.tvEnglishSentence.text = getBoldSpannable(word.englishSentence, word.english)
        
        binding.tvTurkishWord.text = word.turkish
        binding.tvWordTypeTurkish.text = word.turkishType
        
        // Türkçe Cümlede Kelimeyi Kalın Yap
        binding.tvTurkishSentence.text = getBoldSpannable(word.turkishSentence, word.turkish)

        binding.tvProgress.text = "${currentIndex + 1} / ${wordList.size}"
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