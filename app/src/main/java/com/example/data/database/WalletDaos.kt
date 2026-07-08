package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    fun getWalletFlow(userId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    suspend fun getWallet(userId: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)
}

@Dao
interface RewardHistoryDao {
    @Query("SELECT * FROM wallet_reward_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getRewardsFlow(userId: String): Flow<List<RewardHistoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wallet_reward_history WHERE adId = :adId AND sourceType = :sourceType LIMIT 1)")
    suspend fun hasReward(adId: String, sourceType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReward(reward: RewardHistoryEntity): Long // Returns -1 if ignored (duplicate protection)
}

@Dao
interface WalletRedeemHistoryDao {
    @Query("SELECT * FROM wallet_redeem_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getRedeemFlow(userId: String): Flow<List<RedeemHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedeem(redeem: RedeemHistoryEntity)
}

@Dao
interface WithdrawalHistoryDao {
    @Query("SELECT * FROM wallet_withdrawal_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWithdrawalFlow(userId: String): Flow<List<WithdrawalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalHistoryEntity)
}

@Dao
interface WalletActivityDao {
    @Query("SELECT * FROM wallet_activity WHERE userId = :userId ORDER BY timestamp DESC")
    fun getActivityFlow(userId: String): Flow<List<WalletActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: WalletActivityEntity)

    @Query("DELETE FROM wallet_activity WHERE userId = :userId")
    suspend fun clearActivity(userId: String)
}

@Dao
interface RewardConfigDao {
    @Query("SELECT * FROM reward_config WHERE configId = :configId LIMIT 1")
    fun getConfigFlow(configId: String = "default"): Flow<RewardConfigEntity?>

    @Query("SELECT * FROM reward_config WHERE configId = :configId LIMIT 1")
    suspend fun getConfig(configId: String = "default"): RewardConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: RewardConfigEntity)
}

@Dao
interface WalletAnalyticsDao {
    @Query("SELECT * FROM wallet_analytics WHERE userId = :userId LIMIT 1")
    fun getAnalyticsFlow(userId: String): Flow<WalletAnalyticsEntity?>

    @Query("SELECT * FROM wallet_analytics WHERE userId = :userId LIMIT 1")
    suspend fun getAnalytics(userId: String): WalletAnalyticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: WalletAnalyticsEntity)
}
