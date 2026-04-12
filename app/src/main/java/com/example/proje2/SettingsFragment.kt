package com.example.proje2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proje2.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val sharedPref = requireContext().getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
        
        binding.switchNotifications.isChecked = sharedPref.getBoolean("notifications", true)
        
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notifications", isChecked).apply()
        }

        binding.btnBack.setOnClickListener {
            (activity as? UserActivity)?.loadFragment(LearnFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}