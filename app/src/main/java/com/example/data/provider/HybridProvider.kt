package com.example.data.provider

import com.example.data.model.AdvertisementItem
import com.example.data.model.ReelAdvertisementItem
import com.example.data.repository.AdRepository
import com.example.data.adengine.RemoteAdvertisementConfiguration
import com.example.data.adengine.AdEnvironmentConfiguration
import com.example.data.adengine.AdEnvironment
import android.util.Log

/**
 * Hybrid Provider that manages fallback, waterfall or split ratio distribution 
 * of advertisements across other configured active providers (e.g. AdMob vs Direct vs Dummy).
 */
class HybridProvider(
    private val adRepository: AdRepository,
    private val adMobProvider: AdMobProvider = AdMobProvider(adRepository),
    private val directProvider: DirectAdvertiserProvider = DirectAdvertiserProvider(adRepository),
    private val dummyProvider: DummyAdvertisementProvider = DummyAdvertisementProvider(adRepository),
    private val dummyReelsProvider: DummyReelsAdvertisementProvider = DummyReelsAdvertisementProvider(adRepository)
) : AdvertisementProvider, ReelsAdvertisementProvider {

    override suspend fun getAdsForCategories(categories: List<String>): List<AdvertisementItem> {
        val env = AdEnvironmentConfiguration.activeEnvironment
        Log.d("HybridProvider", "getAdsForCategories - Active Environment: $env")

        if (env == AdEnvironment.DUMMY) {
            Log.d("HybridProvider", "Using local Dummy provider for sandbox/development.")
            return dummyProvider.getAdsForCategories(categories)
        }

        // If environment is TEST or PRODUCTION, run the official AdMob / Direct path
        val selected = RemoteAdvertisementConfiguration.defaultProvider
        Log.d("HybridProvider", "Interrogating waterfall configuration. Default Provider: $selected")
        return try {
            val ads = when (selected) {
                "DIRECT_CAMPAIGNS" -> {
                    val direct = directProvider.getAdsForCategories(categories)
                    if (direct.isNotEmpty()) direct else adMobProvider.getAdsForCategories(categories)
                }
                else -> {
                    adMobProvider.getAdsForCategories(categories)
                }
            }
            if (ads.isEmpty() && env == AdEnvironment.PRODUCTION) {
                Log.e("HybridProvider", "PRODUCTION ERROR: AdMob native ad path returned no ads. Fail state - no fallback to dummy!")
                emptyList()
            } else ads
        } catch (e: Exception) {
            Log.e("HybridProvider", "AdMob native ad path execution failed.", e)
            if (env == AdEnvironment.PRODUCTION) {
                Log.e("HybridProvider", "PRODUCTION ERROR: AdMob path threw exception. Fail state - no fallback to dummy!")
                emptyList()
            } else {
                adMobProvider.getAdsForCategories(categories)
            }
        }
    }

    override suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> {
        val env = AdEnvironmentConfiguration.activeEnvironment
        Log.d("HybridProvider", "getReelsAdsForCategories - Active Environment: $env")

        if (env == AdEnvironment.DUMMY) {
            Log.d("HybridProvider", "Using local Dummy reels provider for sandbox/development.")
            return dummyReelsProvider.getReelsAdsForCategories(categories)
        }

        // If environment is TEST or PRODUCTION, run the official AdMob / Direct path
        val selected = RemoteAdvertisementConfiguration.defaultProvider
        Log.d("HybridProvider", "Selecting high-yield video ads using split hybrid logic. Default Provider: $selected")
        return try {
            val ads = when (selected) {
                "DIRECT_CAMPAIGNS" -> {
                    val direct = directProvider.getReelsAdsForCategories(categories)
                    if (direct.isNotEmpty()) direct else adMobProvider.getReelsAdsForCategories(categories)
                }
                else -> {
                    adMobProvider.getReelsAdsForCategories(categories)
                }
            }
            if (ads.isEmpty() && env == AdEnvironment.PRODUCTION) {
                Log.e("HybridProvider", "PRODUCTION ERROR: AdMob Reels returned no reels. Fail state - no fallback to dummy!")
                emptyList()
            } else ads
        } catch (e: Exception) {
            Log.e("HybridProvider", "AdMob Reels path execution failed.", e)
            if (env == AdEnvironment.PRODUCTION) {
                Log.e("HybridProvider", "PRODUCTION ERROR: AdMob Reels path threw exception. Fail state - no fallback to dummy!")
                emptyList()
            } else {
                adMobProvider.getReelsAdsForCategories(categories)
            }
        }
    }
}
