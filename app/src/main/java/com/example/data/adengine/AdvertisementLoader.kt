package com.example.data.adengine

import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.provider.AdvertisementProvider
import com.example.data.provider.ReelsAdvertisementProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log

class AdvertisementLoader(
    private val feedProvider: AdvertisementProvider,
    private val reelsProvider: ReelsAdvertisementProvider
) {

    suspend fun loadFeedAdsWithRetry(categories: List<String>): List<AdvertisementItem> = withContext(Dispatchers.IO) {
        var attempt = 1
        val maxRetries = AppConfiguration.advertisement.maxRetries
        val baseDelay = AppConfiguration.advertisement.retryDelayMs

        while (attempt <= maxRetries) {
            try {
                Log.d("AdvertisementLoader", "Attempting to load feed ads (Attempt $attempt/$maxRetries)")
                val ads = feedProvider.getAdsForCategories(categories)
                Log.d("AdvertisementLoader", "Loaded ${ads.size} feed ads successfully")
                return@withContext ads
            } catch (e: Exception) {
                Log.e("AdvertisementLoader", "Error loading feed ads on attempt $attempt", e)
                if (attempt == maxRetries) {
                    throw e
                }
                delay(baseDelay * attempt)
                attempt++
            }
        }
        emptyList()
    }

    suspend fun loadReelsAdsWithRetry(categories: List<String>): List<ReelAdvertisementItem> = withContext(Dispatchers.IO) {
        var attempt = 1
        val maxRetries = AppConfiguration.advertisement.maxRetries
        val baseDelay = AppConfiguration.advertisement.retryDelayMs

        while (attempt <= maxRetries) {
            try {
                Log.d("AdvertisementLoader", "Attempting to load reels ads (Attempt $attempt/$maxRetries)")
                val ads = reelsProvider.getReelsAdsForCategories(categories)
                Log.d("AdvertisementLoader", "Loaded ${ads.size} reels ads successfully")
                return@withContext ads
            } catch (e: Exception) {
                Log.e("AdvertisementLoader", "Error loading reels ads on attempt $attempt", e)
                if (attempt == maxRetries) {
                    throw e
                }
                delay(baseDelay * attempt)
                attempt++
            }
        }
        emptyList()
    }
}
