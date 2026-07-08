package com.example.data.provider

import com.example.data.model.AdvertisementItem
import com.example.data.repository.AdRepository
import java.net.URLEncoder

class DummyAdvertisementProvider(private val adRepository: AdRepository) : AdvertisementProvider {

    override suspend fun getAdsForCategories(categories: List<String>): List<AdvertisementItem> {
        val dummyAds = adRepository.getHomeFeedAds(categories)
        return dummyAds.map { ad ->
            val queryName = ad.productName ?: ad.serviceName ?: ad.appName ?: ad.brandName
            val encodedQuery = try {
                URLEncoder.encode(queryName, "UTF-8")
            } catch (e: Exception) {
                queryName
            }
            AdvertisementItem(
                id = ad.id,
                type = if (ad.isVideo) "VIDEO" else "IMAGE",
                brandName = ad.brandName,
                productName = ad.productName ?: ad.serviceName ?: ad.appName,
                category = ad.category,
                title = ad.title,
                description = ad.description,
                ctaText = ad.ctaText,
                destinationUrl = "https://www.google.com/search?q=$encodedQuery",
                imageUrl = if (!ad.isVideo) ad.mediaUrl else null,
                videoUrl = if (ad.isVideo) ad.mediaUrl else null,
                isSponsored = ad.sponsoredStatus,
                isRewardEligible = ad.rewardCoins > 0,
                createdTime = System.currentTimeMillis() - (1000L * 60 * 60 * (1..24).random()),
                rewardCoins = ad.rewardCoins
            )
        }
    }
}
