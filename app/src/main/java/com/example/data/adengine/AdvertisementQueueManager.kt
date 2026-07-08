package com.example.data.adengine

import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import java.util.LinkedList
import java.util.Queue

class AdvertisementQueueManager {

    private val feedQueue: Queue<AdvertisementItem> = LinkedList()
    private val reelsQueue: Queue<ReelAdvertisementItem> = LinkedList()

    @Synchronized
    fun enqueueFeedAd(ad: AdvertisementItem) {
        if (!feedQueue.contains(ad)) {
            feedQueue.offer(ad)
        }
    }

    @Synchronized
    fun dequeueFeedAd(): AdvertisementItem? {
        return feedQueue.poll()
    }

    @Synchronized
    fun peekFeedAd(): AdvertisementItem? {
        return feedQueue.peek()
    }

    @Synchronized
    fun getFeedQueueSize(): Int = feedQueue.size

    @Synchronized
    fun clearFeedQueue() {
        feedQueue.clear()
    }

    @Synchronized
    fun getFeedSnapshot(): List<AdvertisementItem> {
        return feedQueue.toList()
    }

    @Synchronized
    fun enqueueReelsAd(ad: ReelAdvertisementItem) {
        if (!reelsQueue.contains(ad)) {
            reelsQueue.offer(ad)
        }
    }

    @Synchronized
    fun dequeueReelsAd(): ReelAdvertisementItem? {
        return reelsQueue.poll()
    }

    @Synchronized
    fun peekReelsAd(): ReelAdvertisementItem? {
        return reelsQueue.peek()
    }

    @Synchronized
    fun getReelsQueueSize(): Int = reelsQueue.size

    @Synchronized
    fun clearReelsQueue() {
        reelsQueue.clear()
    }

    @Synchronized
    fun getReelsSnapshot(): List<ReelAdvertisementItem> {
        return reelsQueue.toList()
    }
}
