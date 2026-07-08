package com.example.data.utils

import com.example.data.model.RewardConfigEntity
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.Flow

/**
 * RewardConfigurationManager
 * Responsibilities:
 *  - Reward Rules
 *  - Coin Ratio
 *  - Animations
 *  - Future Campaigns
 */
class RewardConfigurationManager(
    private val walletRepository: WalletRepository
) {
    /**
     * Retrieves the active configuration flow.
     */
    fun getConfigFlow(): Flow<RewardConfigEntity?> {
        return walletRepository.getConfig()
    }

    /**
     * Retrieves the coin to rupee ratio from active config.
     * Default: 100 Coins = 1 Rupee (0.01 Rs per coin)
     */
    suspend fun getCoinToRupeeRatio(): Double {
        val config = walletRepository.getActiveConfig()
        return config.coinToRupeeRatio
    }

    /**
     * Checks if feed rewards are enabled.
     */
    suspend fun isFeedRewardEnabled(): Boolean {
        val config = walletRepository.getActiveConfig()
        return config.feedRewardCoins > 0
    }

    /**
     * Checks if reel/rewarded video rewards are enabled.
     */
    suspend fun isReelsRewardEnabled(): Boolean {
        val config = walletRepository.getActiveConfig()
        return config.reelRewardTier1 > 0 || config.reelRewardTier2 > 0
    }

    /**
     * Checks if animations are enabled.
     * (Always true for high fidelity experience, but configurable)
     */
    fun isAnimationEnabled(): Boolean {
        return true
    }

    /**
     * Maximum daily reward limit.
     */
    fun getMaximumDailyRewardLimit(): Int {
        return 1000 // Configured maximum threshold
    }

    /**
     * Support for future campaigns.
     */
    fun getCampaignMultiplier(campaignId: String): Double {
        return when (campaignId) {
            "festival_bonus" -> 1.5
            "weekend_rush" -> 1.2
            else -> 1.0
        }
    }
}
