package com.example.data.repository

import android.util.LruCache
import com.example.data.database.*
import com.example.data.model.*
import com.example.data.provider.ReelsAdvertisementProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * -----------------------------------------------------------------
 * REELS ADVERTISEMENT DATA REPOSITORY
 * -----------------------------------------------------------------
 * Purpose: Manages local data persistence and remote supplies of Reels video ads.
 * Responsibilities:
 *   - Feed and pre-cache high-performance video advertisement assets.
 *   - Manage watch completion cycles, logging milestones to Local DB.
 * Dependencies:
 *   - Room DAOs: [ReelsRewardHistoryDao], [ReelsAnalyticsDao], [ReelsEventLogDao], [ReelsConfigDao]
 *   - Reels Provider: [ReelsAdvertisementProvider]
 * Future Extension: Support server side validation (SSV) for reels rewards to prevent spoofing.
 */
class ReelsRepository(
    private val reelsRewardHistoryDao: ReelsRewardHistoryDao,
    private val reelsAnalyticsDao: ReelsAnalyticsDao,
    private val reelsEventLogDao: ReelsEventLogDao,
    private val reelsConfigDao: ReelsConfigDao,
    private val adProvider: ReelsAdvertisementProvider
) {
    // In-memory LRU Cache for Reels Metadata
    private val metadataCache = LruCache<String, ReelAdvertisementItem>(30)

    // Flows for observing database changes in real-time
    val configFlow: Flow<ReelsConfig?> = reelsConfigDao.getConfigFlow()
    val analyticsFlow: Flow<ReelsAnalytics?> = reelsAnalyticsDao.getAnalyticsFlow()
    val eventLogsFlow: Flow<List<ReelsEventLog>> = reelsEventLogDao.getEventLogsFlow()

    suspend fun ensureDefaultConfigAndAnalytics() = withContext(Dispatchers.IO) {
        if (reelsConfigDao.getConfig() == null) {
            reelsConfigDao.insertOrUpdateConfig(ReelsConfig())
        }
        if (reelsAnalyticsDao.getAnalytics() == null) {
            reelsAnalyticsDao.insertOrUpdateAnalytics(ReelsAnalytics())
        }
    }

    suspend fun getReelsConfig(): ReelsConfig = withContext(Dispatchers.IO) {
        ensureDefaultConfigAndAnalytics()
        reelsConfigDao.getConfig() ?: ReelsConfig()
    }

    suspend fun updateReelsConfig(config: ReelsConfig) = withContext(Dispatchers.IO) {
        reelsConfigDao.insertOrUpdateConfig(config)
    }

    suspend fun fetchReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> = withContext(Dispatchers.IO) {
        val ads = adProvider.getReelsAdsForCategories(categories)
        ads.forEach { ad ->
            metadataCache.put(ad.id, ad)
        }
        ads
    }

    fun getCachedReelAd(adId: String): ReelAdvertisementItem? {
        return metadataCache.get(adId)
    }

    suspend fun hasRewarded(adId: String): Boolean = withContext(Dispatchers.IO) {
        reelsRewardHistoryDao.hasRewarded(adId)
    }

    suspend fun saveRewardToHistory(adId: String, coinsEarned: Int) = withContext(Dispatchers.IO) {
        reelsRewardHistoryDao.insertReward(ReelsRewardHistory(adId = adId, coinsEarned = coinsEarned))
    }

    suspend fun recordEvent(eventType: String, adId: String) = withContext(Dispatchers.IO) {
        reelsEventLogDao.insertLog(ReelsEventLog(eventType = eventType, adId = adId))

        val current = reelsAnalyticsDao.getAnalytics() ?: ReelsAnalytics()
        val updated = when (eventType) {
            "LOADED" -> current
            "VISIBLE" -> current.copy(reelsAdViews = current.reelsAdViews + 1)
            "THRESHOLD_REACHED" -> current.copy(validWatchEvents = current.validWatchEvents + 1)
            "COMPLETED" -> current.copy(completedThresholdEvents = current.completedThresholdEvents + 1)
            "CLICKED" -> current.copy(ctaClicks = current.ctaClicks + 1)
            "SAVED" -> current.copy(savedAds = current.savedAds + 1)
            "SHARED" -> current.copy(sharedAds = current.sharedAds + 1)
            else -> current
        }
        reelsAnalyticsDao.insertOrUpdateAnalytics(updated)
    }

    suspend fun incrementReelsOpenCount() = withContext(Dispatchers.IO) {
        ensureDefaultConfigAndAnalytics()
        val current = reelsAnalyticsDao.getAnalytics() ?: ReelsAnalytics()
        reelsAnalyticsDao.insertOrUpdateAnalytics(
            current.copy(
                reelsOpenCount = current.reelsOpenCount + 1,
                totalReelsSessions = current.totalReelsSessions + 1
            )
        )
    }

    suspend fun addWatchDuration(durationMs: Long) = withContext(Dispatchers.IO) {
        val current = reelsAnalyticsDao.getAnalytics() ?: ReelsAnalytics()
        reelsAnalyticsDao.insertOrUpdateAnalytics(
            current.copy(totalWatchDurationMs = current.totalWatchDurationMs + durationMs)
        )
    }

    suspend fun addSessionDuration(durationMs: Long) = withContext(Dispatchers.IO) {
        val current = reelsAnalyticsDao.getAnalytics() ?: ReelsAnalytics()
        reelsAnalyticsDao.insertOrUpdateAnalytics(
            current.copy(totalSessionDurationMs = current.totalSessionDurationMs + durationMs)
        )
    }

    suspend fun clearAllAnalytics() = withContext(Dispatchers.IO) {
        reelsAnalyticsDao.insertOrUpdateAnalytics(ReelsAnalytics())
        reelsEventLogDao.clearLogs()
        reelsRewardHistoryDao.clearHistory()
    }
}
