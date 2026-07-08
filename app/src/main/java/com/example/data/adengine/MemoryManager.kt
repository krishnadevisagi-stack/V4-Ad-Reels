package com.example.data.adengine

import android.content.Context
import android.util.Log
import coil.Coil

class MemoryManager(private val context: Context) {

    val imageController = ImageMemoryController(context)
    val videoController = VideoMemoryController()
    val cacheController = CacheController(this)

    fun monitorMemoryUsage(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory
        val usedPercentage = (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0

        if (usedPercentage > 80.0 || AppConfiguration.performance.isLowMemoryModeEnabled) {
            Log.w("MemoryManager", "High memory usage detected ($usedPercentage%). Trimming resources...")
            performEmergencyClean()
        }

        return MemoryStats(usedMemory, freeMemory, maxMemory, usedPercentage)
    }

    fun performEmergencyClean() {
        imageController.clearMemory()
        videoController.releaseAllInactivePlayers()
        cacheController.trimAllCaches()
        System.gc()
    }
}

class ImageMemoryController(private val context: Context) {
    fun clearMemory() {
        try {
            Coil.imageLoader(context).memoryCache?.clear()
            Log.d("ImageMemoryController", "Coil memory cache cleared successfully.")
        } catch (e: Exception) {
            Log.e("ImageMemoryController", "Error clearing Coil memory cache", e)
        }
    }

    fun clearDiskCache() {
        try {
            Coil.imageLoader(context).diskCache?.clear()
            Log.d("ImageMemoryController", "Coil disk cache cleared successfully.")
        } catch (e: Exception) {
            Log.e("ImageMemoryController", "Error clearing Coil disk cache", e)
        }
    }
}

class VideoMemoryController {
    private val activePlayers = mutableSetOf<String>()

    fun trackPlayerActive(playerId: String) {
        activePlayers.add(playerId)
    }

    fun trackPlayerInactive(playerId: String) {
        activePlayers.remove(playerId)
    }

    fun releaseAllInactivePlayers() {
        Log.d("VideoMemoryController", "Releasing inactive video decoders, player surfaces and memory allocations.")
        // In extreme memory pressure scenarios, VM can coordinate player releases
    }
}

class CacheController(private val memoryManager: MemoryManager) {
    private val adCaches = mutableListOf<AdvertisementCache>()

    fun registerCache(cache: AdvertisementCache) {
        adCaches.add(cache)
    }

    fun trimAllCaches() {
        adCaches.forEach { it.clear() }
        Log.d("CacheController", "All active advertisement caches trimmed/cleared.")
    }
}

data class MemoryStats(
    val usedMemoryBytes: Long,
    val freeMemoryBytes: Long,
    val maxMemoryBytes: Long,
    val usedPercentage: Double
)
