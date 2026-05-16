package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.proje2.databinding.ActivityUserBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File

class UserActivity : BaseActivity() {

    private lateinit var binding: ActivityUserBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var isProgrammaticChange = false
    private var lastMainItemId = R.id.nav_learn

    override fun onCreate(savedInstanceState: Bundle?) {
        // Temayı Uygula (setContentView'dan önce)
        applySavedTheme()
        
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupBottomNavigation()
        setupDrawer()
        updateNavHeader()

        // İlk açılışta Öğren sayfasını yükle
        if (savedInstanceState == null) {
            loadFragment(LearnFragment())
        }
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("Settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        if (isDarkMode) {
            setTheme(com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar)
            // Not: Eğer özel bir dark temanız varsa onu buraya yazın. 
            // Şimdilik AppCompatDelegate kullanmak daha garanti olabilir:
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            )
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                }
                R.id.nav_my_words -> {
                    loadFragment(MyWordsFragment())
                }
                R.id.nav_logout -> {
                    showLogoutConfirmation()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    fun showLogoutConfirmation() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_confirm)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Popup'ın ekran genişliğinin %90'ını kaplamasını sağla
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }
        
        dialog.show()
    }

    fun updateNavHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val ivHeaderAvatar = headerView.findViewById<ShapeableImageView>(R.id.ivHeaderAvatar)
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
        val tvHeaderEmail = headerView.findViewById<TextView>(R.id.tvHeaderEmail)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Önce YEREL dosyayı kontrol et
            val sharedPref = getSharedPreferences("UserProfile", MODE_PRIVATE)
            val localPath = sharedPref.getString("profile_path_${currentUser.uid}", null)

            if (localPath != null) {
                val file = File(localPath)
                if (file.exists()) {
                    Glide.with(this)
                        .load(file)
                        .circleCrop()
                        .placeholder(R.drawable.baseline_person_24)
                        .into(ivHeaderAvatar)
                } else {
                    ivHeaderAvatar.setImageResource(R.drawable.baseline_person_24)
                }
            } else {
                // Yerel dosya yoksa varsayılana dön
                ivHeaderAvatar.setImageResource(R.drawable.baseline_person_24)
            }

            // Diğer bilgileri (isim-email) Firestore'dan güncelle
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        tvHeaderName.text = document.getString("fullName") ?: "Kullanıcı"
                        tvHeaderEmail.text = document.getString("email") ?: currentUser.email
                    }
                }
        }
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.END)
    }

    private fun logoutUser() {
        showLoading()
        auth.signOut()
        
        val credentialManager = androidx.credentials.CredentialManager.create(this)
        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Hata olsa bile login ekranına dön
            }
            hideLoading()
            val intent = Intent(this@UserActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivityWithLoading(intent, true)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isProgrammaticChange) return@setOnItemSelectedListener true
            
            lastMainItemId = item.itemId
            when (item.itemId) {
                R.id.nav_learn -> {
                    loadFragment(LearnFragment())
                    true
                }
                R.id.nav_stats -> {
                    loadFragment(StatsFragment())
                    true
                }
                R.id.nav_log -> {
                    loadFragment(LogFragment())
                    true
                }
                R.id.nav_leaderboard -> {
                    loadFragment(LeaderboardFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        if (fragment is ProfileFragment || fragment is MyWordsFragment) {
            binding.bottomNavigation.visibility = View.GONE
        } else {
            binding.bottomNavigation.visibility = View.VISIBLE
            
            // Alt navigasyondaki ikonun dogru sayfayı gostermesini sagla
            isProgrammaticChange = true
            binding.bottomNavigation.selectedItemId = lastMainItemId
            isProgrammaticChange = false
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateBack() {
        val fragment = when (lastMainItemId) {
            R.id.nav_stats -> StatsFragment()
            R.id.nav_log -> LogFragment()
            else -> LearnFragment()
        }
        loadFragment(fragment)
    }
}
