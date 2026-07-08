package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImpressionCacheDao {
    @Query("SELECT * FROM impression_cache")
    fun getAllCachedImpressionsFlow(): Flow<List<ImpressionCache>>

    @Query("SELECT EXISTS(SELECT 1 FROM impression_cache WHERE adId = :adId)")
    suspend fun hasImpression(adId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImpression(impression: ImpressionCache)

    @Query("DELETE FROM impression_cache")
    suspend fun clearCache()
}

@Dao
interface FeedAnalyticsDao {
    @Query("SELECT * FROM feed_analytics WHERE id = 1 LIMIT 1")
    fun getAnalyticsFlow(): Flow<FeedAnalytics?>

    @Query("SELECT * FROM feed_analytics WHERE id = 1 LIMIT 1")
    suspend fun getAnalytics(): FeedAnalytics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAnalytics(analytics: FeedAnalytics)
}

@Dao
interface FeedEventLogDao {
    @Query("SELECT * FROM feed_event_logs ORDER BY timestamp DESC")
    fun getEventLogsFlow(): Flow<List<FeedEventLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FeedEventLog)

    @Query("DELETE FROM feed_event_logs")
    suspend fun clearLogs()
}

@Dao
interface FeedConfigDao {
    @Query("SELECT * FROM feed_configs WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<FeedConfig?>

    @Query("SELECT * FROM feed_configs WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): FeedConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: FeedConfig)
}
