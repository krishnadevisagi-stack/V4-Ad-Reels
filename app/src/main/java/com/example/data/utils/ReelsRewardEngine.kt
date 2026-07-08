package com.example.data.utils

import android.util.Log
import com.example.data.model.ReelsConfig
import com.example.data.repository.ReelsRepository
import com.example.data.adengine.AdvertisementEventBus

class ReelsRewardEngine(
    private val reelsRepository: ReelsRepository
) {
    sealed class RewardStatus {
        data class Awarded(val coins: Int, val isTier2: Boolean) : RewardStatus()
        object AlreadyRewarded : RewardStatus()
        object ThresholdNotMet : RewardStatus()
    }

    /**
     * Evaluates watch progress events and decides if the user qualifies for a coin reward.
     * Restricts payouts to a maximum of one reward per eligible advertisement.
     */
    suspend fun evaluateWatchProgress(
        adId: String,
        durationMs: Long,
        config: ReelsConfig
    ): RewardStatus {
        val secondsWatched = (durationMs / 1000).toInt()
        val minSeconds = config.minimumWatchSeconds.coerceAtLeast(1)

        // Step 1: Security - Check if already rewarded to prevent double-dipping
        val isAlreadyRewarded = reelsRepository.hasRewarded(adId)
        if (isAlreadyRewarded) {
            return RewardStatus.AlreadyRewarded
        }

        // Step 2: Validate threshold criteria
        if (secondsWatched < minSeconds) {
            return RewardStatus.ThresholdNotMet
        }

        // Step 3: Log milestone events in db event logs
        reelsRepository.recordEvent("THRESHOLD_REACHED", adId)

        // Step 4: Tier Payout logic: Tier 2 (15s+) or Tier 1 (5s+)
        val isTier2 = secondsWatched >= 15
        val rewardAmount = if (isTier2) config.rewardTier2 else config.rewardTier1

        // Step 5: Save record securely to DB first to avoid race conditions
        reelsRepository.saveRewardToHistory(adId, rewardAmount)
        reelsRepository.recordEvent("COMPLETED", adId)

        Log.d("ReelsRewardEngine", "Reward issued for ad $adId: $rewardAmount Coins (Tier2=$isTier2)")

        // Emit to central Event Bus for event-driven coordination
        AdvertisementEventBus.emit(
            AdvertisementEventBus.Event.RewardEligible(
                adId = adId,
                coins = rewardAmount,
                isVideo = true
            )
        )

        return RewardStatus.Awarded(coins = rewardAmount, isTier2 = isTier2)
    }
}
