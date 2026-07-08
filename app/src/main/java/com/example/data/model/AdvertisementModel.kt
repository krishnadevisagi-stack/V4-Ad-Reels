package com.example.data.model

/**
 * Standardized Advertisement Model as defined in Chapter 6.
 * Decouples the UI from any concrete provider and enforces strict metadata policy rules.
 */
data class AdvertisementModel(
    val adId: String,
    val adType: String,       // "IMAGE" or "VIDEO"
    val headline: String?,    // Headline (null if missing)
    val body: String?,        // Body description (null if missing)
    val advertiser: String?,  // Advertiser Name (null if missing)
    val ctaText: String?,     // CTA Text (null if missing)
    val mediaUrl: String?,    // Image or Video URL
    val isSponsored: Boolean = true,
    val category: String?,    // Category (null if missing)
    val destinationUrl: String?,
    val loadingState: String  // "IDLE", "LOADING", "LOADED", "FAILED", "DISPOSED"
)
