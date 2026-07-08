package com.example.data.provider

import com.example.data.model.ReelAdvertisementItem

/**
 * High-level abstract Reels provider interface.
 * The Reels Screen and Reels UseCase communicate only with this.
 */
interface ReelsAdvertisementProvider {
    suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem>
}
