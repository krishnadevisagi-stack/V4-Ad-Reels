package com.example.data.usecase

import com.example.data.model.AdvertisementItem
import com.example.data.model.FeedConfig
import com.example.data.repository.FeedRepository
import com.example.data.utils.RewardEngine

/**
 * -----------------------------------------------------------------
 * FEED INTERACTIONS USE CASE
 * -----------------------------------------------------------------
 * Purpose: Implements business-logic operations for the home feed advertisement interactions.
 * Responsibilities:
 *   - Fetch feed advertisement lists matching interest tags.
 *   - Enforce buffer size limits and cache constraints.
 *   - Track impression records and validate milestones.
 * Dependencies:
 *   - [FeedRepository], [RewardEngine]
 * Future Extension: Integrate with real-time ML-based ad recommendation engines.
 */
class FeedUseCase(
    private val feedRepository: FeedRepository,
    val rewardEngine: RewardEngine
) {
    /**
     * Fetch raw ads from the repository filtered by categories.
     */
    suspend fun loadFeedAds(categories: List<String>): List<AdvertisementItem> {
        return feedRepository.fetchAdsForCategories(categories)
    }

    /**
     * Limit the returned list of ads to match the buffer requirements:
     * - "Initially load 3-4 advertisements only."
     * - "Keep: Current Visible Ads + Next 3 Prepared Ads. No more. Prevent memory waste."
     *
     * @param allAds The full pool of matching advertisements for the categories.
     * @param maxIndexReached The highest item index the user has reached/scrolled to.
     * @param config The FeedConfig specifying the buffer size.
     */
    fun applyFeedBuffering(
        allAds: List<AdvertisementItem>,
        maxIndexReached: Int,
        config: FeedConfig
    ): List<AdvertisementItem> {
        if (allAds.isEmpty()) return emptyList()
        
        // Initially (maxIndexReached = 0), we load 3-4 items.
        // Standard formula: maxIndexReached + 1 (the visible item) + config.feedBufferSize (the next prepared items)
        val initialLoadSize = 4
        val targetSize = (maxIndexReached + 1 + config.feedBufferSize).coerceAtLeast(initialLoadSize)
        
        return allAds.take(targetSize)
    }

    /**
     * Record impression and compute if a reward event should occur.
     */
    suspend fun validateImpressionAndCheckReward(adId: String, config: FeedConfig): Boolean {
        return rewardEngine.onValidatedImpression(adId, config)
    }

    /**
     * Record loaded event log
     */
    suspend fun recordAdLoaded(adId: String) {
        feedRepository.recordEvent("LOADED", adId)
    }

    /**
     * Record visible event log
     */
    suspend fun recordAdVisible(adId: String) {
        feedRepository.recordEvent("VISIBLE", adId)
    }

    /**
     * Record click event log
     */
    suspend fun recordAdClicked(adId: String) {
        feedRepository.recordEvent("CLICKED", adId)
    }

    /**
     * Record saved event log
     */
    suspend fun recordAdSaved(adId: String) {
        feedRepository.recordEvent("SAVED", adId)
    }

    /**
     * Record shared event log
     */
    suspend fun recordAdShared(adId: String) {
        feedRepository.recordEvent("SHARED", adId)
    }
}
