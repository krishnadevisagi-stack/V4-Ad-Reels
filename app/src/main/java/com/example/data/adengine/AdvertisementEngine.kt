package com.example.data.adengine

import android.content.Context
import com.example.data.provider.AdvertisementProvider
import com.example.data.provider.ReelsAdvertisementProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AdvertisementEngine(
    val context: Context,
    val feedProvider: AdvertisementProvider,
    val reelsProvider: ReelsAdvertisementProvider
) {
    // Background execution scope
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Sub-modules
    val cache = AdvertisementCache()
    val loader = AdvertisementLoader(feedProvider, reelsProvider)
    val queueManager = AdvertisementQueueManager()
    val manager = AdvertisementManager(context, feedProvider, reelsProvider, cache, queueManager, loader)
    val prefetchManager = AdvertisementPrefetchManager(cache, loader, queueManager)
    val dispatcher = AdvertisementDispatcher(engineScope)
    val memoryManager = MemoryManager(context).apply {
        cacheController.registerCache(cache)
    }

    companion object {
        @Volatile
        private var INSTANCE: AdvertisementEngine? = null

        fun getInstance(
            context: Context,
            feedProvider: AdvertisementProvider,
            reelsProvider: ReelsAdvertisementProvider
        ): AdvertisementEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdvertisementEngine(
                    context.applicationContext,
                    feedProvider,
                    reelsProvider
                ).also { INSTANCE = it }
            }
        }
    }
}
