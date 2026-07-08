package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.provider.HybridProvider
import com.example.data.adengine.AdvertisementEngine
import com.example.data.repository.AdRepository
import com.example.data.repository.ReelsRepository
import com.example.data.repository.UserRepository
import com.example.data.usecase.ReelsUseCase
import com.example.data.utils.ReelsRewardEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * -----------------------------------------------------------------
 * REELS SCREEN VIEW MODEL
 * -----------------------------------------------------------------
 * Purpose: Manages state flow pipelines and interactive playback tracking for video Reels.
 * Responsibilities:
 *   - Fetch high-performance video advertisements matching user categories.
 *   - Coordinate video watch duration intervals, logging qualified checkpoints.
 *   - Trigger reward validation logic in [ReelsRewardEngine] to add coins.
 * Dependencies:
 *   - [ReelsRepository], [ReelsUseCase], [ReelsRewardEngine], [AdvertisementEngine]
 * Future Extension: Integrate with secure hardware keystores for request signing.
 */
class ReelsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val adRepository = AdRepository() // Shared model pool

    val userRepository = UserRepository(
        userDao = db.userDao(),
        watchHistoryDao = db.watchHistoryDao(),
        savedAdDao = db.savedAdDao(),
        redeemHistoryDao = db.redeemHistoryDao(),
        coinHistoryDao = db.coinHistoryDao(),
        walletDao = db.walletDao(),
        walletActivityDao = db.walletActivityDao(),
        rewardHistoryDao = db.rewardHistoryDao(),
        adStatsDao = db.adStatsDao()
    )

    private val reelsAdProvider = HybridProvider(adRepository)
    private val feedAdProvider = HybridProvider(adRepository)

    val adEngine = AdvertisementEngine.getInstance(application, feedAdProvider, reelsAdProvider)

    private val reelsRepository = ReelsRepository(
        reelsRewardHistoryDao = db.reelsRewardHistoryDao(),
        reelsAnalyticsDao = db.reelsAnalyticsDao(),
        reelsEventLogDao = db.reelsEventLogDao(),
        reelsConfigDao = db.reelsConfigDao(),
        adProvider = reelsAdProvider
    )

    private val reelsRewardEngine = ReelsRewardEngine(reelsRepository)
    private val reelsUseCase = ReelsUseCase(reelsRepository, reelsRewardEngine)

    // Observables
    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val configState: StateFlow<ReelsConfig> = reelsUseCase.configFlow
        .map { it ?: ReelsConfig() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReelsConfig())

    val analyticsState: StateFlow<ReelsAnalytics> = reelsUseCase.analyticsFlow
        .map { it ?: ReelsAnalytics() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReelsAnalytics())

    val eventLogs: StateFlow<List<ReelsEventLog>> = reelsUseCase.eventLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of active Reels
    private val _reelsAds = MutableStateFlow<List<ReelAdvertisementItem>>(emptyList())
    val reelsAds: StateFlow<List<ReelAdvertisementItem>> = _reelsAds.asStateFlow()

    // Loading indicator
    val isLoading = MutableStateFlow(false)

    // Current and Next indices for memory strategy
    val activeIndex = MutableStateFlow(0)
    val preparedIndex = MutableStateFlow<Int?>(null)

    // Track if ad was already rewarded
    private val _rewardedAdIds = MutableStateFlow<Set<String>>(emptySet())
    val rewardedAdIds: StateFlow<Set<String>> = _rewardedAdIds.asStateFlow()

    // Animation events
    data class ReelsCoinAnimationTrigger(
        val id: Long = System.currentTimeMillis() + (0..1000).random(),
        val amount: Int
    )
    private val _coinAnimEvent = MutableSharedFlow<ReelsCoinAnimationTrigger>(extraBufferCapacity = 8)
    val coinAnimEvent = _coinAnimEvent.asSharedFlow()

    private var sessionStartTime = System.currentTimeMillis()

    // Live watch progress map to accumulate duration cleanly
    private val activeWatchTimes = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            reelsRepository.ensureDefaultConfigAndAnalytics()
            reelsUseCase.incrementReelsOpenCount()

            userProfile
                .filterNotNull()
                .map { it.selectedCategories }
                .distinctUntilChanged()
                .collect { categoriesStr ->
                    val categories = categoriesStr.split(",").filter { it.isNotEmpty() }
                    loadReelsAds(categories)
                }
        }
    }

    fun loadReelsAds(categories: List<String>) {
        viewModelScope.launch {
            isLoading.value = true
            val ads = reelsUseCase.getReelsAdsForCategories(categories)
            _reelsAds.value = ads
            activeIndex.value = 0
            preparedIndex.value = null

            // Prefetch rewarded cache for these ads to update state
            val rewarded = mutableSetOf<String>()
            ads.forEach { ad ->
                if (reelsUseCase.hasRewarded(ad.id)) {
                    rewarded.add(ad.id)
                }
            }
            _rewardedAdIds.value = rewarded

            // Log LOADED events
            ads.forEach { ad ->
                reelsUseCase.recordEvent("LOADED", ad.id)
            }
            
            if (ads.isNotEmpty()) {
                reelsUseCase.recordEvent("VISIBLE", ads[0].id)
            }
            isLoading.value = false
        }
    }

    /**
     * Handled on VerticalPager scroll
     */
    fun onPageSelected(index: Int) {
        val ads = reelsAds.value
        if (index < 0 || index >= ads.size) return

        val previousIndex = activeIndex.value
        activeIndex.value = index
        // When swiping to a new page, clear the prepared index until watch threshold is reached
        preparedIndex.value = null

        viewModelScope.launch {
            // Track view analytics
            val ad = ads[index]
            reelsUseCase.recordEvent("VISIBLE", ad.id)

            // Accumulate watch time of previous ad before switching
            if (previousIndex < ads.size) {
                val prevAd = ads[previousIndex]
                val prevWatchTime = activeWatchTimes[prevAd.id] ?: 0L
                if (prevWatchTime > 0) {
                    reelsUseCase.addWatchDuration(prevWatchTime)
                    activeWatchTimes[prevAd.id] = 0L // Reset
                }
            }
        }
    }

    /**
     * Called during video playback to update elapsed milliseconds
     */
    fun updatePlaybackProgress(adId: String, currentPositionMs: Long) {
        val ads = reelsAds.value
        val index = activeIndex.value
        if (index >= ads.size || ads[index].id != adId) return

        // Save current progress in map
        activeWatchTimes[adId] = currentPositionMs
        com.example.data.adengine.SecurityAndAntiFraudManager.recordWatchProgress(adId, currentPositionMs)

        val config = configState.value
        val thresholdMs = config.minimumWatchSeconds * 1000L

        // Trigger preparation of next video if threshold is reached using our AdvertisementEngine
        val hasMetThreshold = adEngine.prefetchManager.shouldPrepareNextVideo(
            currentPositionMs = currentPositionMs,
            thresholdMs = thresholdMs,
            currentIndex = index,
            totalReels = ads.size
        )
        if (hasMetThreshold && preparedIndex.value == null) {
            val nextIndex = index + 1
            preparedIndex.value = nextIndex
            viewModelScope.launch {
                adEngine.prefetchManager.prefetchNextReel(nextIndex, ads)
                adEngine.dispatcher.dispatchAdEvent(ads[nextIndex].id, "LOADED")
            }
        }

        // Evaluate reward eligibility
        viewModelScope.launch {
            val status = reelsUseCase.evaluateWatchProgress(adId, currentPositionMs, config)
            if (status is ReelsRewardEngine.RewardStatus.Awarded) {
                _rewardedAdIds.value = _rewardedAdIds.value + adId

                val activeAd = ads.find { it.id == adId }
                val title = activeAd?.brandName ?: "Reels Watch Reward"
                userRepository.rewardCoinsForAd(
                    adId = adId,
                    brandName = title,
                    coinsReward = status.coins,
                    isVideo = true
                )

                _coinAnimEvent.emit(ReelsCoinAnimationTrigger(amount = status.coins))
                adEngine.dispatcher.dispatchRewardCredit(adId, status.coins, true)
            }
        }
    }

    fun onCtaClicked(ad: ReelAdvertisementItem) {
        viewModelScope.launch {
            reelsUseCase.recordEvent("CLICKED", ad.id)
        }
    }

    fun onAdShared(ad: ReelAdvertisementItem) {
        viewModelScope.launch {
            reelsUseCase.recordEvent("SHARED", ad.id)
        }
    }

    fun onAdSaved(ad: ReelAdvertisementItem, saveAction: () -> Unit) {
        viewModelScope.launch {
            reelsUseCase.recordEvent("SAVED", ad.id)
            saveAction()
        }
    }

    fun updateConfig(config: ReelsConfig) {
        viewModelScope.launch {
            reelsUseCase.updateReelsConfig(config)
        }
    }

    fun clearAllStats() {
        viewModelScope.launch {
            reelsUseCase.clearAllAnalytics()
            _rewardedAdIds.value = emptySet()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Save final session duration
        val duration = System.currentTimeMillis() - sessionStartTime
        viewModelScope.launch {
            reelsUseCase.addSessionDuration(duration)
            
            // Collect any residual active watch durations
            activeWatchTimes.forEach { (_, watchTime) ->
                if (watchTime > 0) {
                    reelsUseCase.addWatchDuration(watchTime)
                }
            }
        }
    }
}
