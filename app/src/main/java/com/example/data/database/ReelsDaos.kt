package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelsRewardHistoryDao {
    @Query("SELECT EXISTS(SELECT 1 FROM reels_reward_history WHERE adId = :adId)")
    suspend fun hasRewarded(adId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: ReelsRewardHistory)

    @Query("DELETE FROM reels_reward_history")
    suspend fun clearHistory()
}

@Dao
interface ReelsAnalyticsDao {
    @Query("SELECT * FROM reels_analytics WHERE id = 1 LIMIT 1")
    fun getAnalyticsFlow(): Flow<ReelsAnalytics?>

    @Query("SELECT * FROM reels_analytics WHERE id = 1 LIMIT 1")
    suspend fun getAnalytics(): ReelsAnalytics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAnalytics(analytics: ReelsAnalytics)
}

@Dao
interface ReelsEventLogDao {
    @Query("SELECT * FROM reels_event_logs ORDER BY timestamp DESC")
    fun getEventLogsFlow(): Flow<List<ReelsEventLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReelsEventLog)

    @Query("DELETE FROM reels_event_logs")
    suspend fun clearLogs()
}

@Dao
interface ReelsConfigDao {
    @Query("SELECT * FROM reels_configs WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<ReelsConfig?>

    @Query("SELECT * FROM reels_configs WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): ReelsConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ReelsConfig)
}
