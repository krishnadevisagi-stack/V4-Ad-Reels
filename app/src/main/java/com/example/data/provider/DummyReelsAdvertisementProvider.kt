package com.example.data.provider

import com.example.data.model.ReelAdvertisementItem
import com.example.data.repository.AdRepository
import java.net.URLEncoder

class DummyReelsAdvertisementProvider(private val adRepository: AdRepository) : ReelsAdvertisementProvider {

    override suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> {
        val dummyAds = adRepository.getReelsAds(categories)
        return dummyAds.map { ad ->
            val queryName = ad.productName ?: ad.serviceName ?: ad.appName ?: ad.brandName
            val encodedQuery = try {
                URLEncoder.encode(queryName, "UTF-8")
            } catch (e: Exception) {
                queryName
            }
            ReelAdvertisementItem(
                id = ad.id,
                brandName = ad.brandName,
                productName = ad.productName ?: ad.serviceName ?: ad.appName,
                category = ad.category,
                title = ad.title,
                description = ad.description,
                ctaText = ad.ctaText,
                destinationUrl = "https://www.google.com/search?q=$encodedQuery",
                videoUrl = ad.mediaUrl,
                isSponsored = ad.sponsoredStatus,
                isRewardEligible = ad.rewardCoins > 0,
                createdTime = System.currentTimeMillis() - (1000L * 60 * 60 * (1..24).random()),
                rewardCoins = ad.rewardCoins
            )
        }
    }
}
