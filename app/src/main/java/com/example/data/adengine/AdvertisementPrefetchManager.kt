package com.example.data.adengine

import android.util.Log
import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem

class AdvertisementPrefetchManager(
    private val cache: AdvertisementCache,
    private val loader: AdvertisementLoader,
    private val queueManager: AdvertisementQueueManager
) {

    // FEED SCROLL PREFETCH STRATEGY
    suspend fun onFeedScrollPositionChanged(
        visibleIndex: Int,
        totalItemsCount: Int,
        isScrolling: Boolean,
        categories: List<String>
    ) {
        if (!isScrolling) {
            Log.d("PrefetchManager", "User stopped scrolling. Pausing prefetch requests.")
            return
        }

        val threshold = AppConfiguration.performance.prefetchThresholdItems
        val bufferSize = AppConfiguration.advertisement.feedBufferSize

        if (visibleIndex >= totalItemsCount - threshold - 1) {
            Log.d("PrefetchManager", "Approaching feed buffer end (Index $visibleIndex of $totalItemsCount). Requesting next advertisements...")
            try {
                val nextBatch = loader.loadFeedAdsWithRetry(categories)
                val adsToEnqueue = nextBatch.take(bufferSize)
                adsToEnqueue.forEach { ad ->
                    cache.putFeedAd(ad)
                    queueManager.enqueueFeedAd(ad)
                }
            } catch (e: Exception) {
                Log.e("PrefetchManager", "Error prefetching next feed ads", e)
            }
        }
    }

    // REELS VIDEO PLAYBACK PREFETCH STRATEGY
    fun shouldPrepareNextVideo(
        currentPositionMs: Long,
        thresholdMs: Long,
        currentIndex: Int,
        totalReels: Int
    ): Boolean {
        // Only preload if watch threshold (e.g., 5s) has been reached and there is a next video
        return currentPositionMs >= thresholdMs && currentIndex + 1 < totalReels
    }

    suspend fun prefetchNextReel(
        nextIndex: Int,
        reelsList: List<ReelAdvertisementItem>
    ) {
        if (nextIndex < reelsList.size) {
            val nextAd = reelsList[nextIndex]
            Log.d("PrefetchManager", "Reels watch threshold met. Prefetching metadata and preparing resources for next ad: ${nextAd.id}")
            cache.putReelsAd(nextAd)
            queueManager.enqueueReelsAd(nextAd)
        }
    }
}
