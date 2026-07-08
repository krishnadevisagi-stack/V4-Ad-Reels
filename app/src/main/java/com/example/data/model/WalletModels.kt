package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val userId: String,
    val currentCoins: Int = 0,
    val todayCoins: Int = 0,
    val weeklyCoins: Int = 0,
    val lifetimeCoins: Int = 0,
    val pendingCoins: Int = 0,
    val redeemableCoins: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_reward_history")
data class RewardHistoryEntity(
    @PrimaryKey val rewardId: String, // Security duplicate protection
    val adId: String,
    val userId: String,
    val amountCoins: Int,
    val sourceType: String, // "FEED", "REEL", "BONUS", "REFERRAL", "SPECIAL"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_redeem_history")
data class RedeemHistoryEntity(
    @PrimaryKey val redeemId: String,
    val userId: String,
    val brandName: String, // Amazon, Flipkart, Swiggy, Swiggy, Swiggy, Swiggy, Google Play, Swiggy, Zomato
    val coinsCost: Int,
    val estimatedValueRupees: Double,
    val status: String, // "Generated", "Pending"
    val voucherCode: String, // Prototype voucher code like "AMZ-93821-X"
    val timestamp: Long = System.currentTimeMillis(),
    val adminRemark: String = ""
)

@Entity(tableName = "wallet_withdrawal_history")
data class WithdrawalHistoryEntity(
    @PrimaryKey val withdrawalId: String,
    val userId: String,
    val upiId: String,
    val amountRupees: Double,
    val coinsCost: Int,
    val status: String, // "Pending", "Approved", "Rejected"
    val timestamp: Long = System.currentTimeMillis(),
    val adminRemark: String = ""
)

@Entity(tableName = "wallet_activity")
data class WalletActivityEntity(
    @PrimaryKey val activityId: String,
    val userId: String,
    val title: String,
    val description: String,
    val amountCoins: Int,
    val type: String, // "REWARD", "REDEEM", "WITHDRAW", "BONUS"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reward_config")
data class RewardConfigEntity(
    @PrimaryKey val configId: String = "default",
    val feedRewardThreshold: Int = 5,
    val feedRewardCoins: Int = 10,
    val minimumReelWatchSeconds: Int = 5,
    val reelRewardTier1: Int = 5,
    val reelRewardTier2: Int = 15,
    val coinToRupeeRatio: Double = 100.0 // 100 Coins = 1 Rupee
)

@Entity(tableName = "wallet_analytics")
data class WalletAnalyticsEntity(
    @PrimaryKey val userId: String,
    val totalFeedImpressions: Int = 0,
    val totalReelWatches: Int = 0,
    val totalRewardsEarned: Int = 0,
    val avgDailyCoins: Double = 0.0,
    val currentStreak: Int = 0,
    val lastActiveTimestamp: Long = 0L
)
