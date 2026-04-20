package com.example.proje2

import android.content.Intent
import android.os.Bundle
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

class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
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

    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                }
                R.id.nav_logout -> {
                    logoutUser()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    fun updateNavHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val ivHeaderAvatar = headerView.findViewById<ShapeableImageView>(R.id.ivHeaderAvatar)
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
        val tvHeaderEmail = headerView.findViewById<TextView>(R.id.tvHeaderEmail)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: "Kullanıcı"
                        val email = document.getString("email") ?: currentUser.email
                        val profileImage = document.getString("profileImage")

                        tvHeaderName.text = fullName
                        tvHeaderEmail.text = email

                        if (!profileImage.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profileImage)
                                .placeholder(R.drawable.baseline_person_24)
                                .into(ivHeaderAvatar)
                        }
                    } else {
                        tvHeaderName.text = currentUser.displayName ?: "Kullanıcı"
                        tvHeaderEmail.text = currentUser.email
                    }
                }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        
        val credentialManager = androidx.credentials.CredentialManager.create(this)
        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Hata olsa bile login ekranına dön
            }
            val intent = Intent(this@UserActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
                else -> false
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        if (fragment is ProfileFragment || fragment is SettingsFragment) {
            binding.bottomNavigation.visibility = View.GONE
        } else {
            binding.bottomNavigation.visibility = View.VISIBLE
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
