package com.example.proje2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.proje2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAdminMode.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        binding.btnUserMode.setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
        }
    }
}
