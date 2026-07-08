package com.example.data.model

/**
 * Common, provider-agnostic advertisement metadata layer used solely for UI presentation.
 * Exposes a standardized schema for both Feed and Reels.
 */
data class AdvertisementMetadata(
    val adId: String,
    val adType: String, // "IMAGE" or "VIDEO"
    val brandName: String,
    val productName: String?,
    val category: String,
    val title: String,
    val description: String,
    val destinationUrl: String,
    val cta: String,
    val sponsored: Boolean,
    val isRewardEligible: Boolean = true,
    val rewardCoins: Int = 10,
    val videoUrl: String? = null,
    val imageUrl: String? = null
)
