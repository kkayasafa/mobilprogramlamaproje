package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.proje2.databinding.ActivityLoginBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                navigateToMain()
                            } else {
                                auth.signOut()
                                Toast.makeText(this, "Lütfen giriş yapmadan önce e-postanızı onaylayın.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this, "Giriş başarısız: Bilgilerinizi kontrol edin", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Arka planı transparan yapalım ki MaterialCardView'ın köşeleri görünsün
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etResetEmail)
        val btnSend = dialogView.findViewById<MaterialButton>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Sıfırlama bağlantısı e-postanıza gönderildi", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Hata: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Lütfen bir e-posta adresi girin", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun navigateToMain() {
        // MainActivity ya da UserActivity'ye yönlendir
        // Önceki kodlarda UserActivity ana ekran olarak kullanılıyordu
        startActivity(Intent(this, UserActivity::class.java))
        finish()
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "E-posta gerekli"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Şifre gerekli"
            return false
        }
        return true
    }
}