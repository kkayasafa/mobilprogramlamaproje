package com.example.proje2

data class LeaderboardUser(
    val uid: String = "",
    val fullName: String = "",
    val profileImage: String = "",
    val score: Int = 0,
    val rank: Int = 0
)