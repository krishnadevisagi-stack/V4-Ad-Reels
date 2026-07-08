package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val fullName: String,
    val email: String,
    val mobile: String,
    val passwordHash: String,
    val guestAccount: Boolean,
    val walletId: String,
    val selectedCategories: String = "",
    val createdDate: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val profileCreated: Boolean = false,
    val status: String = "active",
    val coins: Int = 0,
    val walletBalance: Double = 0.0,
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "watch_histories")
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val adId: String,
    val category: String,
    val brandName: String,
    val coinsEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_ads")
data class SavedAd(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val adId: String,
    val category: String,
    val brandName: String,
    val title: String,
    val description: String,
    val mediaUrl: String,
    val isVideo: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "redeem_histories")
data class RedeemHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemTitle: String,
    val coinsSpent: Int,
    val status: String, // "Completed", "Processing"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "coin_histories")
data class CoinHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DummyAd(
    val id: String,
    val category: String,
    val brandName: String,
    val logo: String,
    val mediaUrl: String, // Image URL or Video URL
    val isVideo: Boolean,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val duration: Int, // in seconds
    val ctaText: String = "Learn More",
    val sponsoredStatus: Boolean = true,
    val productName: String? = null,
    val appName: String? = null,
    val serviceName: String? = null
)

@Entity(tableName = "ad_stats_entries")
data class AdStatsEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val adType: String,      // "FEED" or "REEL"
    val adUnitId: String,    // AdMob Unit ID
    val action: String,      // "CALLED", "OPENED", "SEEN"
    val timestamp: Long = System.currentTimeMillis()
)
