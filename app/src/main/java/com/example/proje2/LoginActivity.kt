package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.proje2.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        if (auth.currentUser != null) {
            navigateToMain()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            navigateToMain()
                        } else {
                            Toast.makeText(this, "Giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signInWithGoogle() {
        val serverClientId = "141509616391-855m0iupdif1ipo0fmro6ouefc1mfh93.apps.googleusercontent.com"
        
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Tüm hesapları göster
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false) // Kullanıcıya seçme şansı ver
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    firebaseAuthWithGoogle(credential.idToken)
                }
            } catch (e: NoCredentialException) {
                Log.e("LoginActivity", "Kayıtlı hesap bulunamadı", e)
                Toast.makeText(this@LoginActivity, "Cihazda uygun Google hesabı bulunamadı.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialCancellationException) {
                Log.w("LoginActivity", "Giriş iptal edildi")
            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "Google Sign-In Hatası: ${e.message}", e)
                val errorMessage = if (e.message?.contains("10") == true) {
                    "Konfigürasyon Hatası (10): Lütfen SHA-1 sertifikasını Firebase Console'da kontrol edin."
                } else {
                    "Hata: ${e.message}"
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("LoginActivity", "Beklenmedik Hata", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Firebase Hatası: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email gerekli"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Şifre gerekli"
            return false
        }
        return true
    }
}