package com.example.data.adengine

import android.content.Context
import android.util.Log
import com.example.data.model.AdvertisementMetadata
import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.provider.AdvertisementProvider
import com.example.data.provider.ReelsAdvertisementProvider

/**
 * AdvertisementManager coordinating the load, prepare, cache, lifecycle, events,
 * and decoupling UI from lower-level provider SDK details.
 */
class AdvertisementManager(
    private val context: Context,
    private val feedProvider: AdvertisementProvider,
    private val reelsProvider: ReelsAdvertisementProvider,
    private val cache: AdvertisementCache,
    private val queueManager: AdvertisementQueueManager,
    private val loader: AdvertisementLoader
) {

    /**
     * Map clean model AdvertisementItem to standard AdvertisementMetadata.
     */
    fun mapToMetadata(item: AdvertisementItem): AdvertisementMetadata {
        return AdvertisementMetadata(
            adId = item.id,
            adType = item.type,
            brandName = item.brandName,
            productName = item.productName,
            category = item.category,
            title = item.title,
            description = item.description,
            destinationUrl = item.destinationUrl,
            cta = item.ctaText,
            sponsored = item.isSponsored,
            isRewardEligible = item.isRewardEligible,
            rewardCoins = item.rewardCoins,
            imageUrl = item.imageUrl,
            videoUrl = item.videoUrl
        )
    }

    /**
     * Map ReelAdvertisementItem to standard AdvertisementMetadata.
     */
    fun mapReelToMetadata(item: ReelAdvertisementItem): AdvertisementMetadata {
        return AdvertisementMetadata(
            adId = item.id,
            adType = "VIDEO",
            brandName = item.brandName,
            productName = item.productName,
            category = item.category,
            title = item.title,
            description = item.description,
            destinationUrl = item.destinationUrl,
            cta = item.ctaText,
            sponsored = item.isSponsored,
            isRewardEligible = item.isRewardEligible,
            rewardCoins = item.rewardCoins,
            videoUrl = item.videoUrl
        )
    }

    /**
     * Responsibility 1: Load Advertisement
     */
    suspend fun loadFeedAdvertisements(categories: List<String>): List<AdvertisementMetadata> {
        return try {
            Log.d("AdvertisementManager", "Initiating feed ads load via Loader for: $categories")
            val items = loader.loadFeedAdsWithRetry(categories)
            val metadataList = items.map { mapToMetadata(it) }
            
            // Broadcast LOADED event for each loaded advertisement
            metadataList.forEach { meta ->
                emitAdEvent(AdvertisementEventBus.Event.AdLoaded(meta))
            }
            metadataList
        } catch (e: Exception) {
            Log.e("AdvertisementManager", "Failed to load feed advertisements.", e)
            emitAdEvent(AdvertisementEventBus.Event.AdFailed("all_feed_ads", e.localizedMessage ?: "Unknown Load Error"))
            emptyList()
        }
    }

    suspend fun loadReelsAdvertisements(categories: List<String>): List<AdvertisementMetadata> {
        return try {
            Log.d("AdvertisementManager", "Initiating reels ads load via Loader for: $categories")
            val items = loader.loadReelsAdsWithRetry(categories)
            val metadataList = items.map { mapReelToMetadata(it) }
            
            // Broadcast LOADED event
            metadataList.forEach { meta ->
                emitAdEvent(AdvertisementEventBus.Event.AdLoaded(meta))
            }
            metadataList
        } catch (e: Exception) {
            Log.e("AdvertisementManager", "Failed to load reels advertisements.", e)
            emitAdEvent(AdvertisementEventBus.Event.AdFailed("all_reels_ads", e.localizedMessage ?: "Unknown Load Error"))
            emptyList()
        }
    }

    /**
     * Responsibility 2: Prepare Advertisement
     */
    fun prepareAdvertisement(metadata: AdvertisementMetadata) {
        Log.d("AdvertisementManager", "Preparing assets (prefetch, layouts, rendering pre-calculation) for ad: ${metadata.adId}")
        // Emits a PREPARED lifecycle event
        emitAdEvent(AdvertisementEventBus.Event.AdPrepared(metadata))
    }

    /**
     * Responsibility 3: Cache Advertisement
     */
    fun cacheAdvertisement(metadata: AdvertisementMetadata) {
        Log.d("AdvertisementManager", "Caching metadata/resources for ad: ${metadata.adId}")
        if (metadata.adType == "VIDEO") {
            val item = ReelAdvertisementItem(
                id = metadata.adId,
                brandName = metadata.brandName,
                productName = metadata.productName,
                category = metadata.category,
                title = metadata.title,
                description = metadata.description,
                ctaText = metadata.cta,
                destinationUrl = metadata.destinationUrl,
                videoUrl = metadata.videoUrl ?: "",
                isSponsored = metadata.sponsored,
                isRewardEligible = metadata.isRewardEligible,
                rewardCoins = metadata.rewardCoins
            )
            cache.putReelsAd(item)
        } else {
            val item = AdvertisementItem(
                id = metadata.adId,
                type = metadata.adType,
                brandName = metadata.brandName,
                productName = metadata.productName,
                category = metadata.category,
                title = metadata.title,
                description = metadata.description,
                ctaText = metadata.cta,
                destinationUrl = metadata.destinationUrl,
                imageUrl = metadata.imageUrl,
                videoUrl = metadata.videoUrl,
                isSponsored = metadata.sponsored,
                isRewardEligible = metadata.isRewardEligible,
                rewardCoins = metadata.rewardCoins
            )
            cache.putFeedAd(item)
        }
    }

    /**
     * Responsibility 4: Track Advertisement Lifecycle & Responsibility 5: Emit Events
     */
    fun trackLifecycle(adId: String, lifecycleState: String, metadata: AdvertisementMetadata? = null) {
        Log.d("AdvertisementManager", "Ad: $adId reached state: $lifecycleState")
        
        when (lifecycleState.uppercase()) {
            "VISIBLE" -> {
                metadata?.let { emitAdEvent(AdvertisementEventBus.Event.AdVisible(it)) }
                notifyAnalytics("VISIBLE", adId)
            }
            "CLICKED" -> {
                metadata?.let { emitAdEvent(AdvertisementEventBus.Event.AdClicked(it)) }
                notifyAnalytics("CLICKED", adId)
            }
            "SHARED" -> {
                metadata?.let { emitAdEvent(AdvertisementEventBus.Event.AdShared(it)) }
                notifyAnalytics("SHARED", adId)
            }
            "SAVED" -> {
                metadata?.let { emitAdEvent(AdvertisementEventBus.Event.AdSaved(it)) }
                notifyAnalytics("SAVED", adId)
            }
            "CLOSED" -> {
                emitAdEvent(AdvertisementEventBus.Event.AdClosed(adId))
            }
        }
    }

    /**
     * Responsibility 6: Notify UI (Relayed via EventBus stream or log update)
     */
    fun notifyUI(eventType: String, metadata: AdvertisementMetadata) {
        Log.d("AdvertisementManager", "Notifying UI of event: $eventType for ad: ${metadata.adId}")
        trackLifecycle(metadata.adId, eventType, metadata)
    }

    /**
     * Responsibility 7: Notify Analytics
     */
    fun notifyAnalytics(metricType: String, adId: String) {
        Log.d("AdvertisementManager", "Forwarding metric $metricType for ad $adId to Analytics engine")
        AdvertisementEventBus.emit(AdvertisementEventBus.Event.AnalyticsUpdated(metricType, adId))
    }

    /**
     * Helper to emit directly to EventBus
     */
    fun emitAdEvent(event: AdvertisementEventBus.Event) {
        AdvertisementEventBus.emit(event)
    }
}
