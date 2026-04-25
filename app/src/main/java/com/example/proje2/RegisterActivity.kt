package com.example.proje2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proje2.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupGenderSpinner()
        setupDatePicker()

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val birthDate = binding.etBirthDate.text.toString().trim()
            val gender = binding.spinnerGender.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(fullName, birthDate, gender, email, password, confirmPassword)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        val userId = user.uid
                                        saveUserToFirestore(userId, fullName, birthDate, gender, email)
                                        
                                        Toast.makeText(this, "Doğrulama e-postası gönderildi. Lütfen e-postanızı onaylayın.", Toast.LENGTH_LONG).show()
                                        
                                        // Kullanıcıyı Firestore'a kaydettikten sonra çıkış yaptırıyoruz ki 
                                        // doğrulanmamış hesapla login kalmasın
                                        auth.signOut()
                                        
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Doğrulama e-postası gönderilemedi: ${verifyTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Kayıt başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupGenderSpinner() {
        val genders = arrayOf("Erkek", "Kadın", "Belirtmek İstemiyorum")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.spinnerGender.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.etBirthDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.etBirthDate.setText(date)
            }, year, month, day)
            datePickerDialog.show()
        }
    }

    private fun saveUserToFirestore(userId: String, fullName: String, birthDate: String, gender: String, email: String) {
        val userMap = hashMapOf(
            "uid" to userId,
            "fullName" to fullName,
            "birthDate" to birthDate,
            "gender" to gender,
            "email" to email,
            "profileImage" to "",
            "createdAt" to System.currentTimeMillis(),
            "learnedWords" to arrayListOf<String>()
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Firestore hatası: ${e.message}")
                Toast.makeText(this, "Bilgiler kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, UserActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun validateInput(fullName: String, birthDate: String, gender: String, email: String, password: String, confirmPassword: String): Boolean {
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Ad Soyad gerekli"
            return false
        }
        if (birthDate.isEmpty()) {
            binding.etBirthDate.error = "Doğum tarihi gerekli"
            return false
        }
        if (gender.isEmpty()) {
            binding.spinnerGender.error = "Cinsiyet gerekli"
            return false
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "E-posta gerekli"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Şifre gerekli"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Şifre en az 6 karakter olmalı"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Şifreler uyuşmuyor"
            return false
        }
        return true
    }
}