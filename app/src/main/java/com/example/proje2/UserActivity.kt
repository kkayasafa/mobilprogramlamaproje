package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proje2.databinding.ActivityUserBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupBottomNavigation()
        setupDrawer()

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

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        
        // Modern Credential Manager Sign-Out
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
        // Profil veya Ayarlar fragment'ı açılırken Bottom Navigation'ı gizle
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