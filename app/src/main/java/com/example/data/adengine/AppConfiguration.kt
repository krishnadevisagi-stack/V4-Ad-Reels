package com.example.data.adengine

object AppConfiguration {
    var reward = RewardConfiguration()
    var advertisement = AdvertisementConfiguration()
    var animation = AnimationConfiguration()
    var wallet = WalletConfiguration()
    var performance = PerformanceConfiguration()
}

data class RewardConfiguration(
    val initialSignOnBonus: Int = 200,
    val feedImpressionsRequired: Int = 5,
    val feedBaseRewardCoins: Int = 15,
    val reelsQualifiedWatchSeconds: Int = 5,
    val reelsBaseRewardCoins: Int = 15,
    val reelsLongerWatchSeconds: Int = 10,
    val reelsLongerRewardCoins: Int = 40,
    val coinsToUsdRatio: Double = 0.01,
    val milestoneRewardCoins: Int = 50
)

data class AdvertisementConfiguration(
    val feedBufferSize: Int = 3,
    val reelsPreloadDistance: Int = 1,
    val cacheExpiryMs: Long = 1800000, // 30 minutes
    val requestTimeoutMs: Long = 10000,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 2000
)

data class AnimationConfiguration(
    val animationSpeedMs: Int = 300,
    val coinAnimationDurationMs: Int = 800,
    val isHardwareAccelerated: Boolean = true
)

data class WalletConfiguration(
    val isCryptoHashingEnabled: Boolean = true,
    val defaultCurrency: String = "USD",
    val minimumCashoutCoins: Int = 1000
)

data class PerformanceConfiguration(
    val isLowMemoryModeEnabled: Boolean = false,
    val maxImageMemoryCacheSizeBytes: Long = 10 * 1024 * 1024, // 10MB
    val maxVideoCacheSizeBytes: Long = 50 * 1024 * 1024, // 50MB
    val prefetchThresholdItems: Int = 1,
    val isTrackingDiagnosticsEnabled: Boolean = true
)
