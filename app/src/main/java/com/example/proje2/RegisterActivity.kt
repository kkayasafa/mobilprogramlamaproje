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
import com.example.proje2.databinding.ActivityRegisterBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(email, password, confirmPassword)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                            navigateToMain()
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
                    firebaseAuthWithGoogle(credential.idToken)
                }
            } catch (e: NoCredentialException) {
                Toast.makeText(this@RegisterActivity, "Cihazda uygun Google hesabı bulunamadı.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialCancellationException) {
                Log.w("RegisterActivity", "Giriş iptal edildi")
            } catch (e: GetCredentialException) {
                Log.e("RegisterActivity", "Hata: ${e.message}")
                val msg = if (e.message?.contains("10") == true) "Konfigürasyon Hatası (10)" else e.message
                Toast.makeText(this@RegisterActivity, "Hata: $msg", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Beklenmedik Hata", e)
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
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun validateInput(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }
}