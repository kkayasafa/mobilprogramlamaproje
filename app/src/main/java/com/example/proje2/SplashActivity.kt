package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Veri tabanını kontrol et ve gerekirse başlat
        FirestoreInitializer(this).checkAndInitializeData()

        // 2 saniye bekle ve sonra yönlendir
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, 2000)
    }

    private fun checkUserStatus() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null && auth.currentUser?.isEmailVerified == true) {
            // Kullanıcı giriş yapmış ve onaylıysa ana ekrana git
            startActivity(Intent(this, UserActivity::class.java))
        } else {
            // Giriş yapmamışsa login ekranına git
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
        // Pürüzsüz geçiş animasyonu
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}