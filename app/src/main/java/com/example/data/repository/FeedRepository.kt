package com.example.data.repository

import android.util.LruCache
import com.example.data.database.*
import com.example.data.model.*
import com.example.data.provider.AdvertisementProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * -----------------------------------------------------------------
 * FEED ADVERTISEMENT DATA REPOSITORY
 * -----------------------------------------------------------------
 * Purpose: Acts as a gateway to store, update, and manage Feed advertisement interactions.
 * Responsibilities:
 *   - Load, cache, and record impressions/events for feed ads.
 *   - Feed-specific local databases sync, LRU memory caches.
 *   - Decouple lower database and ad provider frameworks from presenter layer.
 * Dependencies:
 *   - Room DAOs: [ImpressionCacheDao], [FeedAnalyticsDao], [FeedEventLogDao], [FeedConfigDao]
 *   - Provider Interface: [AdvertisementProvider]
 * Future Extension: Support offline queue syncing with backends when a network becomes available.
 */
class FeedRepository(
    private val impressionCacheDao: ImpressionCacheDao,
    private val feedAnalyticsDao: FeedAnalyticsDao,
    private val feedEventLogDao: FeedEventLogDao,
    private val feedConfigDao: FeedConfigDao,
    private val adProvider: AdvertisementProvider
) {
    // In-memory LRU Cache for Metadata
    private val metadataCache = LruCache<String, AdvertisementItem>(50)

    // Flow of configs, analytics, and event logs
    val configFlow: Flow<FeedConfig?> = feedConfigDao.getConfigFlow()
    val analyticsFlow: Flow<FeedAnalytics?> = feedAnalyticsDao.getAnalyticsFlow()
    val eventLogsFlow: Flow<List<FeedEventLog>> = feedEventLogDao.getEventLogsFlow()
    val impressionCacheFlow: Flow<List<ImpressionCache>> = impressionCacheDao.getAllCachedImpressionsFlow()

    suspend fun ensureDefaultConfigAndAnalytics() = withContext(Dispatchers.IO) {
        if (feedConfigDao.getConfig() == null) {
            feedConfigDao.insertOrUpdateConfig(FeedConfig())
        }
        if (feedAnalyticsDao.getAnalytics() == null) {
            feedAnalyticsDao.insertOrUpdateAnalytics(FeedAnalytics())
        }
    }

    suspend fun getFeedConfig(): FeedConfig = withContext(Dispatchers.IO) {
        ensureDefaultConfigAndAnalytics()
        feedConfigDao.getConfig() ?: FeedConfig()
    }

    suspend fun updateFeedConfig(config: FeedConfig) = withContext(Dispatchers.IO) {
        feedConfigDao.insertOrUpdateConfig(config)
    }

    suspend fun fetchAdsForCategories(categories: List<String>): List<AdvertisementItem> = withContext(Dispatchers.IO) {
        val ads = adProvider.getAdsForCategories(categories)
        // Store in LRU Cache to avoid duplicate loading
        ads.forEach { ad ->
            metadataCache.put(ad.id, ad)
        }
        ads
    }

    // Load from cache or provider fallback
    fun getCachedAd(adId: String): AdvertisementItem? {
        return metadataCache.get(adId)
    }

    suspend fun getAnalyticsDirectly(): FeedAnalytics = withContext(Dispatchers.IO) {
        ensureDefaultConfigAndAnalytics()
        feedAnalyticsDao.getAnalytics() ?: FeedAnalytics()
    }

    // Impression check
    suspend fun hasImpression(adId: String): Boolean = withContext(Dispatchers.IO) {
        impressionCacheDao.hasImpression(adId)
    }

    // Record Event log (Loaded, Visible, Clicked, Saved, Shared)
    suspend fun recordEvent(eventType: String, adId: String) = withContext(Dispatchers.IO) {
        feedEventLogDao.insertLog(FeedEventLog(eventType = eventType, adId = adId))
        
        // Update corresponding aggregated analytics counts
        val currentStats = feedAnalyticsDao.getAnalytics() ?: FeedAnalytics()
        val updatedStats = when (eventType) {
            "LOADED" -> currentStats
            "VISIBLE" -> currentStats
            "IMPRESSION_COUNTED" -> {
                // Record local impression cache so we only count once
                impressionCacheDao.insertImpression(ImpressionCache(adId = adId))
                currentStats.copy(feedImpressions = currentStats.feedImpressions + 1)
            }
            "CLICKED" -> currentStats.copy(adClicks = currentStats.adClicks + 1)
            "SAVED" -> currentStats.copy(adSaves = currentStats.adSaves + 1)
            "SHARED" -> currentStats.copy(adShares = currentStats.adShares + 1)
            else -> currentStats
        }
        feedAnalyticsDao.insertOrUpdateAnalytics(updatedStats)
    }

    // Increment Feed Sessions or Open Counts
    suspend fun incrementFeedOpenCount() = withContext(Dispatchers.IO) {
        ensureDefaultConfigAndAnalytics()
        val current = feedAnalyticsDao.getAnalytics() ?: FeedAnalytics()
        feedAnalyticsDao.insertOrUpdateAnalytics(
            current.copy(
                feedOpenCount = current.feedOpenCount + 1,
                totalFeedSessions = current.totalFeedSessions + 1
            )
        )
    }

    // Add scroll distance
    suspend fun addScrollDistance(distance: Float) = withContext(Dispatchers.IO) {
        val current = feedAnalyticsDao.getAnalytics() ?: FeedAnalytics()
        feedAnalyticsDao.insertOrUpdateAnalytics(
            current.copy(feedScrollDistance = current.feedScrollDistance + distance)
        )
    }

    // Add session duration
    suspend fun addSessionDuration(durationMs: Long) = withContext(Dispatchers.IO) {
        val current = feedAnalyticsDao.getAnalytics() ?: FeedAnalytics()
        feedAnalyticsDao.insertOrUpdateAnalytics(
            current.copy(totalSessionDurationMs = current.totalSessionDurationMs + durationMs)
        )
    }

    suspend fun clearAllAnalytics() = withContext(Dispatchers.IO) {
        feedAnalyticsDao.insertOrUpdateAnalytics(FeedAnalytics())
        feedEventLogDao.clearLogs()
        impressionCacheDao.clearCache()
    }
}
