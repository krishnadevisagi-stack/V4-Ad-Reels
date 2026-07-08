package com.example.data.provider

import com.example.data.model.AdvertisementItem

/**
 * High-level abstract provider interface.
 * The Feed communicates only with this, never with direct implementations.
 */
interface AdvertisementProvider {
    suspend fun getAdsForCategories(categories: List<String>): List<AdvertisementItem>
}
