package com.example.data.provider

import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.repository.AdRepository
import android.util.Log

/**
 * Custom Advertisement Network Provider.
 * Allows integrating arbitrary third-party REST API ad platforms.
 */
class CustomAdvertisementProvider(private val adRepository: AdRepository) : AdvertisementProvider, ReelsAdvertisementProvider {

    override suspend fun getAdsForCategories(categories: List<String>): List<AdvertisementItem> {
        Log.d("CustomAdProvider", "Fetching from third-party custom network for categories: $categories")
        return adRepository.getHomeFeedAds(categories).map { ad ->
            AdvertisementItem(
                id = "custom_feed_${ad.id}",
                type = if (ad.isVideo) "VIDEO" else "IMAGE",
                brandName = "${ad.brandName} (Custom Net)",
                productName = ad.productName ?: "Third-Party Ad",
                category = ad.category,
                title = ad.title,
                description = "Custom Ad network placeholder: ${ad.description}",
                ctaText = ad.ctaText,
                destinationUrl = "https://www.google.com/search?q=${ad.brandName}",
                imageUrl = if (!ad.isVideo) ad.mediaUrl else null,
                videoUrl = if (ad.isVideo) ad.mediaUrl else null,
                isSponsored = true,
                isRewardEligible = ad.rewardCoins > 0,
                rewardCoins = ad.rewardCoins
            )
        }
    }

    override suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> {
        Log.d("CustomAdProvider", "Fetching reels from custom ad network for categories: $categories")
        return adRepository.getReelsAds(categories).map { ad ->
            ReelAdvertisementItem(
                id = "custom_reel_${ad.id}",
                brandName = "${ad.brandName} (Custom Net)",
                productName = ad.productName ?: "Third-Party Ad",
                category = ad.category,
                title = ad.title,
                description = "Custom Ad Network Reel: ${ad.description}",
                ctaText = ad.ctaText,
                destinationUrl = "https://www.google.com/search?q=${ad.brandName}",
                videoUrl = ad.mediaUrl,
                isSponsored = true,
                isRewardEligible = ad.rewardCoins > 0,
                rewardCoins = ad.rewardCoins
            )
        }
    }
}
