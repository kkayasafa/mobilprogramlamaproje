package com.example.proje2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proje2.databinding.FragmentLeaderboardBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupMenuButton()
        fetchLeaderboard("A1") // Varsayılan olarak A1'i getir
    }

    private fun setupMenuButton() {
        binding.btnMenu.setOnClickListener {
            (activity as? UserActivity)?.openDrawer()
        }
    }

    private fun setupRecyclerView() {
        adapter = LeaderboardAdapter()
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(context)
        binding.rvLeaderboard.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayoutLevels.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val level = tab?.text.toString()
                fetchLeaderboard(level)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun fetchLeaderboard(level: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        // 1. İlgili seviyenin tüm kelimelerini çekelim (Karşılaştırma yapmak için)
        db.collection("words_$level").get()
            .addOnSuccessListener { levelWordsSnapshot ->
                val levelWordEnglishes = levelWordsSnapshot.documents.mapNotNull { it.getString("english") }.toSet()
                
                // 2. Tüm kullanıcıları çekip o seviyedeki skorlarını hesaplayalım
                db.collection("users").get()
                    .addOnSuccessListener { userDocuments ->
                        val userList = mutableListOf<LeaderboardUser>()
                        
                        for (document in userDocuments) {
                            val learnedWords = document.get("learnedWords") as? List<String> ?: emptyList()
                            
                            // Sadece bu seviyeye ait kelimelerin sayısını hesapla
                            val scoreInLevel = learnedWords.count { it in levelWordEnglishes }
                            
                            if (scoreInLevel > 0) { // Sadece en az 1 kelime bilenleri göster (isteğe bağlı)
                                userList.add(LeaderboardUser(
                                    uid = document.id,
                                    fullName = document.getString("fullName") ?: "Anonim",
                                    profileImage = document.getString("profileImage") ?: "",
                                    score = scoreInLevel
                                ))
                            }
                        }
                        
                        // 3. Skora göre sırala ve Rank ata
                        val sortedList = userList.sortedByDescending { it.score }
                            .mapIndexed { index, user -> user.copy(rank = index + 1) }
                            .take(50) // İlk 50 kullanıcı
                            
                        adapter.submitList(sortedList)
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // İç Adapter Sınıfı
    inner class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
        private var users = listOf<LeaderboardUser>()

        fun submitList(newList: List<LeaderboardUser>) {
            users = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(users[position])
        }

        override fun getItemCount() = users.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvRank: TextView = view.findViewById(R.id.tvRank)
            private val ivProfile: ShapeableImageView = view.findViewById(R.id.ivProfile)
            private val tvName: TextView = view.findViewById(R.id.tvName)
            private val tvScore: TextView = view.findViewById(R.id.tvScore)

            fun bind(user: LeaderboardUser) {
                tvRank.text = user.rank.toString()
                tvName.text = user.fullName
                tvScore.text = user.score.toString()

                // İlk 3 için özel renkler
                when (user.rank) {
                    1 -> tvRank.setTextColor(context!!.getColor(R.color.type_verb)) // Altın
                    2 -> tvRank.setTextColor(context!!.getColor(R.color.slate_light)) // Gümüş
                    3 -> tvRank.setTextColor(context!!.getColor(R.color.type_adj)) // Bronz
                    else -> tvRank.setTextColor(context!!.getColor(R.color.text_primary))
                }

                if (user.profileImage.isNotEmpty()) {
                    Glide.with(ivProfile.context).load(user.profileImage).circleCrop().into(ivProfile)
                } else {
                    ivProfile.setImageResource(R.drawable.baseline_person_24)
                }
            }
        }
    }
}