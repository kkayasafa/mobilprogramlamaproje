package com.example.proje2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.proje2.databinding.FragmentLearnBinding

class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMenu.setOnClickListener {
            activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.openDrawer(GravityCompat.END)
        }

        setupLevelButtons()
    }

    private fun setupLevelButtons() {
        val buttons = listOf(
            binding.btnA1 to "A1",
            binding.btnA2 to "A2",
            binding.btnB1 to "B1",
            binding.btnB2 to "B2",
            binding.btnC1 to "C1",
            binding.btnC2 to "C2"
        )

        buttons.forEach { (button, level) ->
            button.setOnClickListener {
                val intent = Intent(requireContext(), FlashcardActivity::class.java)
                intent.putExtra("LEVEL", level)
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}