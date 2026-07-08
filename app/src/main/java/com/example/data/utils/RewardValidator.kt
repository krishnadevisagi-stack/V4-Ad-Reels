package com.example.data.utils

import android.util.Log
import com.example.data.repository.WalletRepository

/**
 * RewardValidator
 * Responsibilities:
 *  - Validate Reward Event
 *  - Check Duplicate
 *  - Check User
 *  - Check Advertisement
 *  - Check Eligibility
 *  - Approve Reward
 */
class RewardValidator(
    private val walletRepository: WalletRepository
) {
    /**
     * Validates a Reward Event according to policy requirements:
     * - Checks if the user ID is valid (non-blank).
     * - Checks if the ad ID is valid (non-blank).
     * - Checks for duplicate rewards to prevent fraud.
     * - Verifies that the coin amount is valid (positive).
     * - Determines overall eligibility and approves the reward.
     */
    suspend fun validateAndApprove(
        userId: String,
        adId: String,
        amountCoins: Int,
        sourceType: String
    ): Boolean {
        // 1. Check User
        if (userId.isBlank()) {
            Log.e("RewardValidator", "Validation failed: User ID is blank.")
            return false
        }

        // 2. Check Advertisement
        if (adId.isBlank()) {
            Log.e("RewardValidator", "Validation failed: Advertisement ID is blank.")
            return false
        }

        // 3. Check Eligibility
        if (amountCoins <= 0) {
            Log.e("RewardValidator", "Validation failed: Amount must be positive. Found: $amountCoins")
            return false
        }

        // 4. Check Duplicate Protection
        val isDuplicate = walletRepository.hasRewardBeenPaid(adId, sourceType)
        if (isDuplicate) {
            Log.d("RewardValidator", "Duplicate detected: Ad $adId already rewarded for $sourceType.")
            return false
        }

        // 5. Approved!
        Log.d("RewardValidator", "Reward Event approved for User: $userId, Ad: $adId, Coins: $amountCoins ($sourceType)")
        return true
    }
}
