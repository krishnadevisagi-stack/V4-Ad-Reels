package com.example.data.adengine

import android.util.LruCache
import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem

class AdvertisementCache(maxSize: Int = 100) {

    // LRU Cache for Feed items
    private val feedAdLruCache = LruCache<String, AdvertisementItem>(maxSize)

    // LRU Cache for Reels items
    private val reelsAdLruCache = LruCache<String, ReelAdvertisementItem>(maxSize)

    // Tracking timestamps of when items were fetched/cached
    private val timestampCache = mutableMapOf<String, Long>()

    @Synchronized
    fun putFeedAd(ad: AdvertisementItem) {
        feedAdLruCache.put(ad.id, ad)
        timestampCache[ad.id] = System.currentTimeMillis()
    }

    @Synchronized
    fun getFeedAd(id: String): AdvertisementItem? {
        return feedAdLruCache.get(id)
    }

    @Synchronized
    fun removeFeedAd(id: String) {
        feedAdLruCache.remove(id)
        timestampCache.remove(id)
    }

    @Synchronized
    fun putReelsAd(ad: ReelAdvertisementItem) {
        reelsAdLruCache.put(ad.id, ad)
        timestampCache[ad.id] = System.currentTimeMillis()
    }

    @Synchronized
    fun getReelsAd(id: String): ReelAdvertisementItem? {
        return reelsAdLruCache.get(id)
    }

    @Synchronized
    fun removeReelsAd(id: String) {
        reelsAdLruCache.remove(id)
        timestampCache.remove(id)
    }

    @Synchronized
    fun evictExpired(expiryDurationMs: Long) {
        val now = System.currentTimeMillis()
        val toRemove = mutableListOf<String>()
        timestampCache.forEach { (id, time) ->
            if (now - time > expiryDurationMs) {
                toRemove.add(id)
            }
        }
        toRemove.forEach { id ->
            feedAdLruCache.remove(id)
            reelsAdLruCache.remove(id)
            timestampCache.remove(id)
        }
    }

    @Synchronized
    fun clear() {
        feedAdLruCache.evictAll()
        reelsAdLruCache.evictAll()
        timestampCache.clear()
    }

    @Synchronized
    fun size(): Int {
        return feedAdLruCache.size() + reelsAdLruCache.size()
    }
}
