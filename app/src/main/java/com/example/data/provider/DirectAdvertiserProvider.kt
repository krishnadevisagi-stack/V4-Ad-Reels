package com.example.data.provider

import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.repository.AdRepository
import android.util.Log

/**
 * Direct Advertiser integration provider.
 * Allows displaying high-yield campaigns negotiated directly with brands/partners.
 */
class DirectAdvertiserProvider(private val adRepository: AdRepository) : AdvertisementProvider, ReelsAdvertisementProvider {

    override suspend fun getAdsForCategories(categories: List<String>): List<AdvertisementItem> {
        Log.d("DirectAdvertiser", "Loading directly-brokered premium campaigns for categories: $categories")
        return adRepository.getHomeFeedAds(categories).map { ad ->
            AdvertisementItem(
                id = "direct_feed_${ad.id}",
                type = if (ad.isVideo) "VIDEO" else "IMAGE",
                brandName = "${ad.brandName} ★",
                productName = ad.productName ?: "Premium Direct Campaign",
                category = ad.category,
                title = ad.title,
                description = "Direct Premium Ad: ${ad.description}",
                ctaText = ad.ctaText,
                destinationUrl = "https://www.google.com/search?q=${ad.brandName}",
                imageUrl = if (!ad.isVideo) ad.mediaUrl else null,
                videoUrl = if (ad.isVideo) ad.mediaUrl else null,
                isSponsored = true,
                isRewardEligible = ad.rewardCoins > 0,
                rewardCoins = ad.rewardCoins * 2 // Direct campaigns have double reward incentives
            )
        }
    }

    override suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> {
        Log.d("DirectAdvertiser", "Loading direct-brokered video ads for categories: $categories")
        return adRepository.getReelsAds(categories).map { ad ->
            ReelAdvertisementItem(
                id = "direct_reel_${ad.id}",
                brandName = "${ad.brandName} ★",
                productName = ad.productName ?: "Premium Direct Campaign",
                category = ad.category,
                title = ad.title,
                description = "Direct premium video campaign: ${ad.description}",
                ctaText = ad.ctaText,
                destinationUrl = "https://www.google.com/search?q=${ad.brandName}",
                videoUrl = ad.mediaUrl,
                isSponsored = true,
                isRewardEligible = ad.rewardCoins > 0,
                rewardCoins = ad.rewardCoins * 2
            )
        }
    }
}
