package com.example.proje2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.proje2.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveImageToInternalStorage(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadUserProfile()
        setupSettings()

        binding.ivAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnBack.setOnClickListener {
            (activity as? UserActivity)?.navigateBack()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun setupSettings() {
        val sharedPref = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        
        // Karanlık Mod - SharedPreferences ve Delegate Kullanımı
        binding.switchDarkMode.isChecked = sharedPref.getBoolean("dark_mode", false)
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            
            // Temayı anında uygula
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                )
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        // Veri Temizleme
        binding.tvClearCache.setOnClickListener {
            val currentUser = auth.currentUser
            
            // 1. Firestore çevrimdışı verilerini temizle
            db.clearPersistence().addOnCompleteListener {
                // 2. Uygulama önbelleğini temizle
                requireContext().cacheDir.deleteRecursively()
                
                // 3. Yerel profil resmi kayıtlarını ve dosyasını temizle
                if (currentUser != null) {
                    val userPref = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    val localPath = userPref.getString("profile_path_${currentUser.uid}", null)
                    
                    // Dosyayı fiziksel olarak sil
                    localPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) file.delete()
                    }
                    
                    // SharedPreferences kaydını sil
                    userPref.edit().remove("profile_path_${currentUser.uid}").apply()
                    
                    // Görseli varsayılana döndür
                    binding.ivAvatar.setImageResource(R.drawable.baseline_person_24)
                    
                    // Nav Header'ı anında güncelle
                    (activity as? UserActivity)?.updateNavHeader()
                }
                
                Toast.makeText(context, "Yerel veriler ve profil resmi tamamen temizlendi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        val currentUser = auth.currentUser ?: return
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().filesDir, "profile_${currentUser.uid}.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Yerel yolu SharedPreferences'a kaydet
            val sharedPref = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
            sharedPref.edit().putString("profile_path_${currentUser.uid}", file.absolutePath).apply()

            // Firestore'daki dökümanı da güncelle (path bilgisini buluta yaz)
            db.collection("users").document(currentUser.uid)
                .update("profileImage", file.absolutePath)
                .addOnSuccessListener {
                    Log.d("Firestore", "Profil yolu buluta kaydedildi")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Yol kaydedilemedi: ${e.message}")
                }

            // Ekranda goster
            Glide.with(this).load(file).circleCrop().into(binding.ivAvatar)
            
            // Yan menuyu guncelle
            (activity as? UserActivity)?.updateNavHeader()
            
            Toast.makeText(context, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("InternalStorage", "Hata: ${e.message}")
            Toast.makeText(context, getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return
        
        // Önce yerel resmi kontrol et
        val sharedPref = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val localPath = sharedPref.getString("profile_path_${currentUser.uid}", null)
        
        if (localPath != null) {
            val file = File(localPath)
            if (file.exists()) {
                Glide.with(this).load(file).circleCrop().into(binding.ivAvatar)
            }
        }

        // Firestore'dan diger bilgileri cek
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    binding.tvUserName.text = document.getString("fullName") ?: "Kullanıcı"
                    binding.tvEmail.text = document.getString("email") ?: currentUser.email
                    binding.tvBirthDate.text = document.getString("birthDate") ?: "-"
                    binding.tvGender.text = document.getString("gender") ?: "-"
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
