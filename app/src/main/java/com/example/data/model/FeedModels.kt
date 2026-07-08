package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Clean domain model for advertisements in the engine.
 * Decouples the UI from any concrete provider (e.g. DummyAd, future AdMob, etc.)
 */
data class AdvertisementItem(
    val id: String,
    val type: String, // "IMAGE" or "VIDEO"
    val brandName: String,
    val productName: String?,
    val category: String,
    val title: String,
    val description: String,
    val ctaText: String,
    val destinationUrl: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val isSponsored: Boolean,
    val isRewardEligible: Boolean,
    val createdTime: Long = System.currentTimeMillis(),
    val rewardCoins: Int = 10
)

@Entity(tableName = "impression_cache")
data class ImpressionCache(
    @PrimaryKey val adId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_analytics")
data class FeedAnalytics(
    @PrimaryKey val id: Int = 1,
    val totalFeedSessions: Int = 0,
    val feedOpenCount: Int = 0,
    val feedScrollDistance: Float = 0f,
    val feedImpressions: Int = 0,
    val adClicks: Int = 0,
    val adSaves: Int = 0,
    val adShares: Int = 0,
    val totalSessionDurationMs: Long = 0L
)

@Entity(tableName = "feed_event_logs")
data class FeedEventLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventType: String, // "LOADED", "VISIBLE", "IMPRESSION_COUNTED", "CLICKED", "SAVED", "SHARED"
    val adId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_configs")
data class FeedConfig(
    @PrimaryKey val id: Int = 1,
    val feedBufferSize: Int = 3, // Prepared ads after visible buffer
    val lazyLoadingThreshold: Int = 1, // trigger loading when threshold items from end
    val cacheSize: Int = 50,
    val rewardRatio: Int = 5, // 5 impressions = 1 reward
    val skeletonCount: Int = 3,
    val imageQuality: String = "HIGH",
    val animationSpeedMs: Int = 300
)
