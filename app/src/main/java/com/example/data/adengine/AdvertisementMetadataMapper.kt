package com.example.data.adengine

import android.util.Log
import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.model.AdvertisementMetadata
import com.example.data.model.AdvertisementModel

/**
 * AdvertisementMetadataMapper
 * Responsibilities:
 *  - Receive Provider Data
 *  - Validate
 *  - Normalize
 *  - Convert into AdvertisementModel
 *  - Return to UI
 * Enforces missing metadata policy strategies and ensures we never fabricate fake profile information.
 */
object AdvertisementMetadataMapper {

    /**
     * Maps an AdvertisementItem (from feed) into standard AdvertisementModel.
     */
    fun mapFromFeedItem(item: AdvertisementItem, loadingState: String = "LOADED"): AdvertisementModel? {
        // 1. Validate Core Metadata
        if (item.id.isBlank()) {
            Log.e("MetadataMapper", "Validation failed: Ad ID is blank.")
            return null
        }

        // 2. Normalize and apply missing metadata strategies
        val normalizedHeadline = item.title.trim().takeIf { it.isNotBlank() }
        val normalizedBody = item.description.trim().takeIf { it.isNotBlank() }
        
        // Advertiser Missing Strategy: Use Advertiser if present, otherwise null (UI will fallback to "Sponsored Advertisement")
        val normalizedAdvertiser = item.brandName.trim().takeIf { it.isNotBlank() }
        
        // CTA Missing Strategy: Keep original if present, otherwise null (UI will hide CTA area)
        val normalizedCta = item.ctaText.trim().takeIf { it.isNotBlank() }
        
        val media = if (item.type == "VIDEO") item.videoUrl else item.imageUrl
        val normalizedMedia = media?.trim()?.takeIf { it.isNotBlank() }

        val normalizedCategory = item.category.trim().takeIf { it.isNotBlank() }
        val normalizedDestination = item.destinationUrl.trim().takeIf { it.isNotBlank() }

        return AdvertisementModel(
            adId = item.id,
            adType = if (item.type == "VIDEO") "VIDEO" else "IMAGE",
            headline = normalizedHeadline,
            body = normalizedBody,
            advertiser = normalizedAdvertiser,
            ctaText = normalizedCta,
            mediaUrl = normalizedMedia,
            isSponsored = item.isSponsored,
            category = normalizedCategory,
            destinationUrl = normalizedDestination,
            loadingState = loadingState
        )
    }

    /**
     * Maps a ReelAdvertisementItem (from Reels) into standard AdvertisementModel.
     */
    fun mapFromReelItem(item: ReelAdvertisementItem, loadingState: String = "LOADED"): AdvertisementModel? {
        // 1. Validate Core Metadata
        if (item.id.isBlank()) {
            Log.e("MetadataMapper", "Validation failed: Reel Ad ID is blank.")
            return null
        }

        // 2. Normalize
        val normalizedHeadline = item.title.trim().takeIf { it.isNotBlank() }
        val normalizedBody = item.description.trim().takeIf { it.isNotBlank() }
        val normalizedAdvertiser = item.brandName.trim().takeIf { it.isNotBlank() }
        val normalizedCta = item.ctaText.trim().takeIf { it.isNotBlank() }
        val normalizedMedia = item.videoUrl.trim().takeIf { it.isNotBlank() }
        val normalizedCategory = item.category.trim().takeIf { it.isNotBlank() }
        val normalizedDestination = item.destinationUrl.trim().takeIf { it.isNotBlank() }

        return AdvertisementModel(
            adId = item.id,
            adType = "VIDEO",
            headline = normalizedHeadline,
            body = normalizedBody,
            advertiser = normalizedAdvertiser,
            ctaText = normalizedCta,
            mediaUrl = normalizedMedia,
            isSponsored = item.isSponsored,
            category = normalizedCategory,
            destinationUrl = normalizedDestination,
            loadingState = loadingState
        )
    }

    /**
     * Maps an AdvertisementMetadata item into standard AdvertisementModel.
     */
    fun mapFromMetadata(item: AdvertisementMetadata, loadingState: String = "LOADED"): AdvertisementModel? {
        if (item.adId.isBlank()) {
            Log.e("MetadataMapper", "Validation failed: Metadata Ad ID is blank.")
            return null
        }

        val normalizedHeadline = item.title.trim().takeIf { it.isNotBlank() }
        val normalizedBody = item.description.trim().takeIf { it.isNotBlank() }
        val normalizedAdvertiser = item.brandName.trim().takeIf { it.isNotBlank() }
        val normalizedCta = item.cta.trim().takeIf { it.isNotBlank() }
        val media = if (item.adType == "VIDEO") item.videoUrl else item.imageUrl
        val normalizedMedia = media?.trim()?.takeIf { it.isNotBlank() }
        val normalizedCategory = item.category.trim().takeIf { it.isNotBlank() }
        val normalizedDestination = item.destinationUrl.trim().takeIf { it.isNotBlank() }

        return AdvertisementModel(
            adId = item.adId,
            adType = item.adType,
            headline = normalizedHeadline,
            body = normalizedBody,
            advertiser = normalizedAdvertiser,
            ctaText = normalizedCta,
            mediaUrl = normalizedMedia,
            isSponsored = item.sponsored,
            category = normalizedCategory,
            destinationUrl = normalizedDestination,
            loadingState = loadingState
        )
    }
}
