package com.example.data.utils

import android.util.Log
import com.example.data.model.FeedConfig
import com.example.data.repository.FeedRepository
import com.example.data.adengine.AdvertisementEventBus
import kotlinx.coroutines.flow.firstOrNull

class RewardEngine(
    private val feedRepository: FeedRepository
) {
    /**
     * Receives a validated impression event for an advertisement.
     * Returns true if a reward event should be triggered based on configuration ratio.
     */
    suspend fun onValidatedImpression(adId: String, config: FeedConfig): Boolean {
        // Step 1: Ensure we don't count duplicate impressions for the same ad
        val alreadyCounted = feedRepository.hasImpression(adId)
        if (alreadyCounted) {
            Log.d("RewardEngine", "Ad $adId was already counted in history. No reward credit.")
            return false
        }

        // Step 2: Record validated impression in DB event log & update analytics
        feedRepository.recordEvent("IMPRESSION_COUNTED", adId)

        // Step 3: Check how many total impressions have been counted in database
        val currentStats = feedRepository.getAnalyticsDirectly()
        val impressionsCount = currentStats.feedImpressions

        Log.d("RewardEngine", "Validated impression recorded. Total impressions: $impressionsCount")

        // Step 4: Every X impressions, trigger one Reward Event
        val ratio = config.rewardRatio.coerceAtLeast(1)
        val isMilestoneReached = (impressionsCount > 0 && impressionsCount % ratio == 0)
        
        if (isMilestoneReached) {
            // Emit RewardEligible event to central event bus
            AdvertisementEventBus.emit(
                AdvertisementEventBus.Event.RewardEligible(
                    adId = adId,
                    coins = 50,
                    isVideo = false
                )
            )
        }
        
        return isMilestoneReached
    }
}
