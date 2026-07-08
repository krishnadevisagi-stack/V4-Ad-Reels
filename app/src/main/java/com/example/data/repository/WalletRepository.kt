package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * -----------------------------------------------------------------
 * WALLET & TRANSACTION REPOSITORY
 * -----------------------------------------------------------------
 * Purpose: Centrally handles users' virtual bank transactions, cashouts, rewards, and activities.
 * Responsibilities:
 *   - Guard coins balances state and record transactional ledger with full history.
 *   - Verify withdrawal requests, update config criteria, and log security audits.
 * Dependencies:
 *   - Room DAOs: [WalletDao], [RewardHistoryDao], [WalletRedeemHistoryDao], [WithdrawalHistoryDao], [WalletActivityDao], [RewardConfigDao], [WalletAnalyticsDao]
 * Future Extension: Secure database fields with standard Android Keystore SQLCipher DB encryption.
 */
class WalletRepository(
    private val walletDao: WalletDao,
    private val rewardHistoryDao: RewardHistoryDao,
    private val walletRedeemHistoryDao: WalletRedeemHistoryDao,
    private val withdrawalHistoryDao: WithdrawalHistoryDao,
    private val walletActivityDao: WalletActivityDao,
    private val rewardConfigDao: RewardConfigDao,
    private val walletAnalyticsDao: WalletAnalyticsDao,
    private val userDao: UserDao? = null
) {
    // Default config fallback
    private val defaultConfig = RewardConfigEntity()

    fun getWallet(userId: String): Flow<WalletEntity?> = walletDao.getWalletFlow(userId)

    fun getRewards(userId: String): Flow<List<RewardHistoryEntity>> = rewardHistoryDao.getRewardsFlow(userId)

    fun getRedemptions(userId: String): Flow<List<RedeemHistoryEntity>> = walletRedeemHistoryDao.getRedeemFlow(userId)

    fun getWithdrawals(userId: String): Flow<List<WithdrawalHistoryEntity>> = withdrawalHistoryDao.getWithdrawalFlow(userId)

    fun getActivities(userId: String): Flow<List<WalletActivityEntity>> = walletActivityDao.getActivityFlow(userId)

    fun getConfig(): Flow<RewardConfigEntity?> = rewardConfigDao.getConfigFlow()

    fun getAnalytics(userId: String): Flow<WalletAnalyticsEntity?> = walletAnalyticsDao.getAnalyticsFlow(userId)

    suspend fun initializeDefaultConfig() = withContext(Dispatchers.IO) {
        val existing = rewardConfigDao.getConfig()
        if (existing == null) {
            rewardConfigDao.insertConfig(defaultConfig)
        }
    }

    suspend fun getActiveConfig(): RewardConfigEntity = withContext(Dispatchers.IO) {
        rewardConfigDao.getConfig() ?: defaultConfig
    }

    suspend fun saveConfig(config: RewardConfigEntity) = withContext(Dispatchers.IO) {
        rewardConfigDao.insertConfig(config)
    }

    suspend fun initializeWalletIfNeeded(userId: String) = withContext(Dispatchers.IO) {
        val existing = walletDao.getWallet(userId)
        if (existing == null) {
            val wallet = WalletEntity(
                userId = userId,
                currentCoins = 0,
                todayCoins = 0,
                weeklyCoins = 0,
                lifetimeCoins = 0,
                pendingCoins = 0,
                redeemableCoins = 0
            )
            walletDao.insertWallet(wallet)
        }
        val existingAnalytics = walletAnalyticsDao.getAnalytics(userId)
        if (existingAnalytics == null) {
            walletAnalyticsDao.insertAnalytics(WalletAnalyticsEntity(userId = userId))
        }
    }

    suspend fun updateWallet(wallet: WalletEntity) = withContext(Dispatchers.IO) {
        walletDao.insertWallet(wallet)
        if (userDao != null) {
            val user = userDao.getActiveUserProfile()
            if (user != null) {
                userDao.updateCoins(wallet.currentCoins, wallet.currentCoins * com.example.data.config.AdConfig.COINS_TO_USD_RATIO)
                val updatedUser = userDao.getActiveUserProfile()
                if (updatedUser != null) {
                    com.example.data.firebase.FirebaseManager.syncUserProfile(updatedUser)
                }
            }
        }
    }

    suspend fun updateAnalytics(analytics: WalletAnalyticsEntity) = withContext(Dispatchers.IO) {
        walletAnalyticsDao.insertAnalytics(analytics)
    }

    suspend fun hasRewardBeenPaid(adId: String, sourceType: String): Boolean = withContext(Dispatchers.IO) {
        rewardHistoryDao.hasReward(adId, sourceType)
    }

    suspend fun insertRewardRecord(reward: RewardHistoryEntity): Boolean = withContext(Dispatchers.IO) {
        val result = rewardHistoryDao.insertReward(reward)
        if (result != -1L) {
            com.example.data.firebase.FirebaseManager.syncRewardHistory(reward)
        }
        result != -1L
    }

    suspend fun insertRedeemRecord(redeem: RedeemHistoryEntity) = withContext(Dispatchers.IO) {
        walletRedeemHistoryDao.insertRedeem(redeem)
    }

    suspend fun insertWithdrawalRecord(withdrawal: WithdrawalHistoryEntity) = withContext(Dispatchers.IO) {
        withdrawalHistoryDao.insertWithdrawal(withdrawal)
    }

    suspend fun insertActivityRecord(activity: WalletActivityEntity) = withContext(Dispatchers.IO) {
        walletActivityDao.insertActivity(activity)
        com.example.data.firebase.FirebaseManager.syncWalletActivity(activity)
    }

    suspend fun clearAllActivity(userId: String) = withContext(Dispatchers.IO) {
        walletActivityDao.clearActivity(userId)
    }
}
