package com.example.data.utils

import android.util.Log
import com.example.data.model.*
import com.example.data.repository.WalletRepository
import com.example.data.adengine.ApplicationEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

// Decooupled event data models
data class RewardEvent(
    val rewardId: String,
    val adId: String,
    val userId: String,
    val amountCoins: Int,
    val sourceType: String, // "FEED", "REEL", "BONUS"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 1. WalletRewardEngine
 * Validates and triggers reward events. Implements security rules and duplicate protection.
 */
class WalletRewardEngine(
    private val walletRepository: WalletRepository,
    private val walletEngine: WalletEngine,
    private val analyticsEngine: AnalyticsEngine,
    private val rewardValidator: RewardValidator = RewardValidator(walletRepository),
    private val isTamperedCheck: () -> Boolean = { false }
) {
    /**
     * Receives and validates Feed Impression Events.
     * Every X valid impressions credits Y coins.
     */
    suspend fun processFeedImpression(
        adId: String,
        userId: String,
        totalImpressionsCount: Int
    ): Boolean {
        if (isTamperedCheck()) {
            Log.e("RewardEngine", "TAMPER DETECTED: Source code modifications detected! Rewards are completely disabled.")
            return false
        }
        val config = walletRepository.getActiveConfig()
        val threshold = config.feedRewardThreshold.coerceAtLeast(1)
        val coinsToAward = config.feedRewardCoins

        // Increment total feed impression in analytics first
        analyticsEngine.incrementFeedImpressions(userId)

        // Rule: Reward every X valid feed impressions
        if (totalImpressionsCount > 0 && totalImpressionsCount % threshold == 0) {
            val rewardId = "FEED_${adId}_${totalImpressionsCount}_${UUID.randomUUID().toString().take(6)}"
            
            // Validate using RewardValidator
            val isValidated = rewardValidator.validateAndApprove(
                userId = userId,
                adId = adId,
                amountCoins = coinsToAward,
                sourceType = "FEED"
            )
            if (!isValidated) {
                return false
            }

            val event = RewardEvent(
                rewardId = rewardId,
                adId = adId,
                userId = userId,
                amountCoins = coinsToAward,
                sourceType = "FEED"
            )

            // Insert Reward Record (transacts duplicate check at DB index level)
            val inserted = walletRepository.insertRewardRecord(
                RewardHistoryEntity(
                    rewardId = event.rewardId,
                    adId = event.adId,
                    userId = event.userId,
                    amountCoins = event.amountCoins,
                    sourceType = event.sourceType,
                    timestamp = event.timestamp
                )
            )

            if (inserted) {
                Log.d("RewardEngine", "Feed Reward Event validated & issued: $rewardId")
                walletEngine.handleRewardEvent(event)
                return true
            }
        }
        return false
    }

    /**
     * Receives and validates Reel Watch duration milestones.
     * Watch Threshold Reached -> Tier 1 Reward.
     * Longer Watch Threshold Reached -> Tier 2 Reward.
     */
    suspend fun processReelWatch(
        adId: String,
        userId: String,
        watchSeconds: Int
    ): RewardEvent? {
        if (isTamperedCheck()) {
            Log.e("RewardEngine", "TAMPER DETECTED: Source code modifications detected! Rewards are completely disabled.")
            return null
        }
        val config = walletRepository.getActiveConfig()
        val minSeconds = config.minimumReelWatchSeconds.coerceAtLeast(1)

        // Increment total reel watch count in analytics first
        analyticsEngine.incrementReelWatches(userId)

        // Determine tier payout eligibility
        val isTier2 = watchSeconds >= 15
        val isTier1 = watchSeconds >= minSeconds

        if (!isTier1) return null

        val tierLabel = if (isTier2) "REEL_TIER2" else "REEL_TIER1"
        val amountCoins = if (isTier2) config.reelRewardTier2 else config.reelRewardTier1

        // Validate using RewardValidator
        val isValidated = rewardValidator.validateAndApprove(
            userId = userId,
            adId = adId,
            amountCoins = amountCoins,
            sourceType = tierLabel
        )
        if (!isValidated) {
            return null
        }

        val rewardId = "${tierLabel}_${adId}_${UUID.randomUUID().toString().take(6)}"
        val event = RewardEvent(
            rewardId = rewardId,
            adId = adId,
            userId = userId,
            amountCoins = amountCoins,
            sourceType = if (isTier2) "REEL_TIER2" else "REEL_TIER1"
        )

        // Insert secure record
        val inserted = walletRepository.insertRewardRecord(
            RewardHistoryEntity(
                rewardId = event.rewardId,
                adId = event.adId,
                userId = event.userId,
                amountCoins = event.amountCoins,
                sourceType = event.sourceType,
                timestamp = event.timestamp
            )
        )

        if (inserted) {
            Log.d("RewardEngine", "Reel Reward Event validated & issued: $rewardId")
            walletEngine.handleRewardEvent(event)
            return event
        }

        return null
    }

    /**
     * Issue any custom bonus reward.
     */
    suspend fun processBonusReward(
        userId: String,
        bonusCoins: Int,
        title: String
    ) {
        if (isTamperedCheck()) {
            Log.e("RewardEngine", "TAMPER DETECTED: Source code modifications detected! Rewards are completely disabled.")
            return
        }
        val rewardId = "BONUS_${UUID.randomUUID().toString().take(8)}"
        val event = RewardEvent(
            rewardId = rewardId,
            adId = "bonus_system",
            userId = userId,
            amountCoins = bonusCoins,
            sourceType = "BONUS"
        )

        walletRepository.insertRewardRecord(
            RewardHistoryEntity(
                rewardId = event.rewardId,
                adId = event.adId,
                userId = event.userId,
                amountCoins = event.amountCoins,
                sourceType = event.sourceType,
                timestamp = event.timestamp
            )
        )

        walletEngine.handleRewardEvent(event, customTitle = title)
    }
}

/**
 * 2. WalletEngine
 * Listens to RewardEvents and safely updates the Wallet tables and logs timeline activity.
 */
class WalletEngine(
    private val walletRepository: WalletRepository,
    private val notificationEngine: NotificationEngine
) {
    suspend fun handleRewardEvent(event: RewardEvent, customTitle: String? = null) {
        walletRepository.initializeWalletIfNeeded(event.userId)
        val wallet = walletRepository.getWallet(event.userId).firstOrNull() ?: return

        // Update Wallet Balance
        val updatedWallet = wallet.copy(
            currentCoins = wallet.currentCoins + event.amountCoins,
            todayCoins = wallet.todayCoins + event.amountCoins,
            weeklyCoins = wallet.weeklyCoins + event.amountCoins,
            lifetimeCoins = wallet.lifetimeCoins + event.amountCoins,
            redeemableCoins = wallet.redeemableCoins + event.amountCoins,
            lastUpdated = System.currentTimeMillis()
        )

        var success = false
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts && !success) {
            try {
                walletRepository.updateWallet(updatedWallet)
                success = true
            } catch (e: Exception) {
                attempts++
                Log.e("WalletEngine", "Wallet Update Failed (Attempt $attempts/$maxAttempts). Retrying...", e)
            }
        }

        if (!success) {
            Log.e("WalletEngine", "CRITICAL ERROR: Failed to update wallet balance after $maxAttempts attempts. Rolling back transaction.")
            return
        }

        // Log to WalletActivity Timeline
        val title = customTitle ?: when (event.sourceType) {
            "FEED" -> "Feed Reward Earned"
            "REEL_TIER1" -> "Reel Tier 1 Reward"
            "REEL_TIER2" -> "Reel Tier 2 Reward"
            else -> "App Bonus Reward"
        }
        val description = when (event.sourceType) {
            "FEED" -> "Earned by completing feed impressions"
            "REEL_TIER1" -> "Earned by watching reels over threshold"
            "REEL_TIER2" -> "Earned by watching reels over extended threshold"
            else -> "Added directly to your balance"
        }

        val activity = WalletActivityEntity(
            activityId = UUID.randomUUID().toString(),
            userId = event.userId,
            title = title,
            description = description,
            amountCoins = event.amountCoins,
            type = "REWARD",
            timestamp = System.currentTimeMillis()
        )

        var activitySuccess = false
        var activityAttempts = 0
        while (activityAttempts < maxAttempts && !activitySuccess) {
            try {
                walletRepository.insertActivityRecord(activity)
                activitySuccess = true
            } catch (e: Exception) {
                activityAttempts++
                Log.e("WalletEngine", "Activity Record Failed (Attempt $activityAttempts/$maxAttempts). Retrying...", e)
            }
        }

        if (!activitySuccess) {
            Log.e("WalletEngine", "CRITICAL ERROR: Failed to insert activity record. Rolling back wallet balance to prevent corruption.")
            try {
                walletRepository.updateWallet(wallet) // Reset to original wallet balance
            } catch (e: Exception) {
                Log.e("WalletEngine", "FATAL ERROR: Could not rollback wallet balance!", e)
            }
            return
        }

        // Notify UI about visual floating coin animation
        notificationEngine.triggerCoinFlyAnimation(event.amountCoins)

        // Emit events on the event bus for analytics & monitoring
        ApplicationEventBus.emit(ApplicationEventBus.Event.RewardAdded(event.userId, event.rewardId, event.amountCoins, event.sourceType))
        ApplicationEventBus.emit(ApplicationEventBus.Event.WalletUpdated(event.userId, updatedWallet.redeemableCoins, event.amountCoins, event.sourceType))
    }

    /**
     * Safe internal deduction of coins for redemption or cashout.
     */
    suspend fun deductCoins(userId: String, coins: Int, activityTitle: String, activityDesc: String, type: String, customActivityId: String? = null): Boolean {
        val wallet = walletRepository.getWallet(userId).firstOrNull() ?: return false
        if (wallet.redeemableCoins < coins || wallet.currentCoins < coins) return false

        val updatedWallet = wallet.copy(
            currentCoins = wallet.currentCoins - coins,
            redeemableCoins = wallet.redeemableCoins - coins,
            lastUpdated = System.currentTimeMillis()
        )

        var success = false
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts && !success) {
            try {
                walletRepository.updateWallet(updatedWallet)
                success = true
            } catch (e: Exception) {
                attempts++
                Log.e("WalletEngine", "Deduct Wallet Update Failed (Attempt $attempts/$maxAttempts). Retrying...", e)
            }
        }

        if (!success) {
            Log.e("WalletEngine", "CRITICAL ERROR: Failed to deduct coins after $maxAttempts attempts. Rolling back transaction.")
            return false
        }

        val activity = WalletActivityEntity(
            activityId = customActivityId ?: UUID.randomUUID().toString(),
            userId = userId,
            title = activityTitle,
            description = activityDesc,
            amountCoins = -coins,
            type = type,
            timestamp = System.currentTimeMillis()
        )

        var activitySuccess = false
        var activityAttempts = 0
        while (activityAttempts < maxAttempts && !activitySuccess) {
            try {
                walletRepository.insertActivityRecord(activity)
                activitySuccess = true
            } catch (e: Exception) {
                activityAttempts++
                Log.e("WalletEngine", "Deduct Activity Record Failed (Attempt $activityAttempts/$maxAttempts). Retrying...", e)
            }
        }

        if (!activitySuccess) {
            Log.e("WalletEngine", "CRITICAL ERROR: Failed to insert deduct activity record. Rolling back wallet deduction to original balance.")
            try {
                walletRepository.updateWallet(wallet) // Rollback to original balance
            } catch (e: Exception) {
                Log.e("WalletEngine", "FATAL ERROR: Could not rollback wallet balance deduction!", e)
            }
            return false
        }

        // Emit events on the event bus for analytics & monitoring
        ApplicationEventBus.emit(ApplicationEventBus.Event.RedeemRequested(userId, type, coins, activityTitle))
        ApplicationEventBus.emit(ApplicationEventBus.Event.WalletUpdated(userId, updatedWallet.redeemableCoins, -coins, type))

        return true
    }
}

/**
 * 3. AnalyticsEngine
 * Subscribes to rewards/activities and compiles usage streaks, avg daily coins, etc.
 */
class AnalyticsEngine(
    private val walletRepository: WalletRepository
) {
    suspend fun incrementFeedImpressions(userId: String) {
        walletRepository.initializeWalletIfNeeded(userId)
        val stats = walletRepository.getAnalytics(userId).firstOrNull() ?: return
        val updated = stats.copy(
            totalFeedImpressions = stats.totalFeedImpressions + 1,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        walletRepository.updateAnalytics(updated)
    }

    suspend fun incrementReelWatches(userId: String) {
        walletRepository.initializeWalletIfNeeded(userId)
        val stats = walletRepository.getAnalytics(userId).firstOrNull() ?: return
        val updated = stats.copy(
            totalReelWatches = stats.totalReelWatches + 1,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        walletRepository.updateAnalytics(updated)
    }

    suspend fun recalculateDailyAnalytics(userId: String) {
        walletRepository.initializeWalletIfNeeded(userId)
        val stats = walletRepository.getAnalytics(userId).firstOrNull() ?: return
        val wallet = walletRepository.getWallet(userId).firstOrNull() ?: return

        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60L * 60L * 1000L
        val diffTime = now - stats.lastActiveTimestamp

        var newStreak = stats.currentStreak
        if (diffTime.compareTo(oneDayMs) < 0) {
            // Logged in today or within 24 hours
            if (newStreak == 0) newStreak = 1
        } else if (diffTime.compareTo(oneDayMs) >= 0 && diffTime.compareTo(oneDayMs * 2L) <= 0) {
            // Day streak continued!
            newStreak += 1
        } else {
            // Streak broken
            newStreak = 1
        }

        val totalCoinsEarned = wallet.lifetimeCoins
        val daysElapsed = ((now - wallet.lastUpdated) / oneDayMs).coerceAtLeast(1)
        val avgCoins = totalCoinsEarned.toDouble() / daysElapsed

        val updated = stats.copy(
            totalRewardsEarned = totalCoinsEarned,
            avgDailyCoins = avgCoins,
            currentStreak = newStreak,
            lastActiveTimestamp = now
        )
        walletRepository.updateAnalytics(updated)
    }
}

/**
 * 4. NotificationEngine
 * Emits visual notifications and triggers high fidelity flying coin overlays.
 */
class NotificationEngine {
    data class CoinAnimEvent(val amount: Int, val id: Long = System.currentTimeMillis() + (0..1000).random())

    private val _coinAnimFlow = MutableSharedFlow<CoinAnimEvent>(extraBufferCapacity = 16)
    val coinAnimFlow = _coinAnimFlow.asSharedFlow()

    suspend fun triggerCoinFlyAnimation(amount: Int) {
        _coinAnimFlow.emit(CoinAnimEvent(amount = amount))
    }
}
