package com.example.proje2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.proje2.databinding.ActivityRegisterBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

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
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                saveUserToFirestore(userId, fullName, birthDate, gender, email)
                            }
                        } else {
                            Toast.makeText(this, "Kayıt başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.btnGoogleSignUp.setOnClickListener {
            signInWithGoogle()
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
            "profileImage" to "", // İleride eklenebilir
            "createdAt" to System.currentTimeMillis()
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

    private fun signInWithGoogle() {
        val serverClientId = "141509616391-855m0iupdif1ipo0fmro6ouefc1mfh93.apps.googleusercontent.com"
        
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@RegisterActivity,
                    request = request
                )
                
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    firebaseAuthWithGoogle(credential.idToken, credential.displayName)
                }
            } catch (e: NoCredentialException) {
                Toast.makeText(this@RegisterActivity, "Cihazda uygun Google hesabı bulunamadı.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialCancellationException) {
                Log.w("RegisterActivity", "Giriş iptal edildi")
            } catch (e: GetCredentialException) {
                Log.e("RegisterActivity", "Hata: ${e.message}")
                Toast.makeText(this@RegisterActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Beklenmedik Hata", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, displayName: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val email = auth.currentUser?.email
                    if (userId != null && email != null) {
                        // Google ile girişte eksik bilgileri (doğum tarihi, cinsiyet) boş bırakıyoruz
                        saveUserToFirestore(userId, displayName ?: "Google Kullanıcısı", "", "", email)
                    }
                } else {
                    Toast.makeText(this, "Firebase Hatası: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
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
            binding.etEmail.error = "Email gerekli"
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