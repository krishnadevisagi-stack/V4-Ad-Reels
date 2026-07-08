package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Clean domain model for video advertisements in the Reels Engine.
 * Decouples the UI from any concrete provider (e.g. DummyAd, future AdMob, etc.)
 */
data class ReelAdvertisementItem(
    val id: String,
    val brandName: String,
    val productName: String?,
    val category: String,
    val title: String,
    val description: String,
    val ctaText: String,
    val destinationUrl: String,
    val videoUrl: String,
    val isSponsored: Boolean = true,
    val isRewardEligible: Boolean = true,
    val createdTime: Long = System.currentTimeMillis(),
    val rewardCoins: Int = 30
)

@Entity(tableName = "reels_reward_history")
data class ReelsRewardHistory(
    @PrimaryKey val adId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val coinsEarned: Int
)

@Entity(tableName = "reels_analytics")
data class ReelsAnalytics(
    @PrimaryKey val id: Int = 1,
    val totalReelsSessions: Int = 0,
    val reelsOpenCount: Int = 0,
    val reelsAdViews: Int = 0,
    val validWatchEvents: Int = 0,
    val completedThresholdEvents: Int = 0,
    val ctaClicks: Int = 0,
    val savedAds: Int = 0,
    val sharedAds: Int = 0,
    val totalWatchDurationMs: Long = 0L,
    val totalSessionDurationMs: Long = 0L
)

@Entity(tableName = "reels_event_logs")
data class ReelsEventLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventType: String, // "LOADED", "VISIBLE", "THRESHOLD_REACHED", "COMPLETED", "CLICKED", "SAVED", "SHARED"
    val adId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reels_configs")
data class ReelsConfig(
    @PrimaryKey val id: Int = 1,
    val minimumWatchSeconds: Int = 5,
    val rewardTier1: Int = 20, // 5 seconds award
    val rewardTier2: Int = 50, // 15 seconds award
    val animationDurationMs: Int = 800,
    val autoSwipe: Boolean = false,
    val autoPlay: Boolean = true,
    val muteDefault: Boolean = false,
    val preloadDistance: Int = 1, // Only prepare 1 next advertisement
    val cacheSize: Int = 30
)
