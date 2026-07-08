package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.provider.HybridProvider
import com.example.data.repository.FeedRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.AdRepository
import com.example.data.usecase.FeedUseCase
import com.example.data.utils.RewardEngine
import com.example.data.adengine.AdvertisementEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * -----------------------------------------------------------------
 * FEED SCREEN VIEW MODEL
 * -----------------------------------------------------------------
 * Purpose: Prepares state flows and handles interactions for the personalized Feed layout.
 * Responsibilities:
 *   - Fetch and filter advertisements matching user interest preferences.
 *   - Track impression progress cycles and trigger coin awards via the [RewardEngine].
 * Dependencies:
 *   - [FeedRepository], [UserRepository], [FeedUseCase], [AdvertisementEngine]
 * Future Extension: Support server-side pagination and targeted user category segment filtering.
 */
class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val adRepository = AdRepository()

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

    // Setup our decoupled Feed layers
    private val adProvider = HybridProvider(adRepository)
    private val dummyReelsAdProvider = HybridProvider(adRepository)
    val adEngine = AdvertisementEngine.getInstance(application, adProvider, dummyReelsAdProvider)

    val feedRepository = FeedRepository(
        impressionCacheDao = db.impressionCacheDao(),
        feedAnalyticsDao = db.feedAnalyticsDao(),
        feedEventLogDao = db.feedEventLogDao(),
        feedConfigDao = db.feedConfigDao(),
        adProvider = adProvider
    )
    private val rewardEngine = RewardEngine(feedRepository)
    private val feedUseCase = FeedUseCase(feedRepository, rewardEngine)

    // UI State flows
    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val feedConfig: StateFlow<FeedConfig> = feedRepository.configFlow
        .map { it ?: FeedConfig() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedConfig())

    val feedAnalytics: StateFlow<FeedAnalytics> = feedRepository.analyticsFlow
        .map { it ?: FeedAnalytics() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedAnalytics())

    val isScrolling = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    // Complete pool of all ads matching current categories
    private val _allMatchedAds = MutableStateFlow<List<AdvertisementItem>>(emptyList())
    val allMatchedAds: StateFlow<List<AdvertisementItem>> = _allMatchedAds.asStateFlow()

    // Track highest scrolled/visible index
    val maxIndexReached = MutableStateFlow(0)

    // Currently buffered feed ads displayed to the user
    val feedAds: StateFlow<List<AdvertisementItem>> = combine(
        _allMatchedAds,
        maxIndexReached,
        feedConfig
    ) { allAds, maxIndex, config ->
        feedUseCase.applyFeedBuffering(allAds, maxIndex, config)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shared Flow for milestone reward animation trigger
    private val _rewardEvent = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val rewardEvent = _rewardEvent.asSharedFlow()

    // Keep track of which ad impressions were validated in this session to avoid repeats in memory
    private val _validatedAdIds = MutableStateFlow<Set<String>>(emptySet())
    val validatedAdIds: StateFlow<Set<String>> = _validatedAdIds.asStateFlow()

    private var sessionStartTime: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            feedRepository.ensureDefaultConfigAndAnalytics()
            feedRepository.incrementFeedOpenCount()
            
            // Listen to selected categories of logged-in user and fetch ads
            userProfile
                .map { profile ->
                    profile?.selectedCategories?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
                }
                .distinctUntilChanged()
                .collect { categories ->
                    loadAdvertisements(categories)
                }
        }
    }

    private fun loadAdvertisements(categories: List<String>) {
        viewModelScope.launch {
            isLoading.value = true
            // Avoid freezing UI using background dispatchers
            val matched = feedUseCase.loadFeedAds(categories)
            _allMatchedAds.value = matched
            maxIndexReached.value = 0
            
            // Log LOADED event for initially prepared ads
            matched.take(4).forEach { ad ->
                feedUseCase.recordAdLoaded(ad.id)
            }
            isLoading.value = false
        }
    }

    /**
     * Viewport notification handler from the LazyColumn list.
     * Keeps track of maximum scrolled item and updates viewport scroll status.
     */
    fun onUserScrolledToItem(index: Int, isUserScrolling: Boolean) {
        isScrolling.value = isUserScrolling
        
        // Prevent memory waste by lazy-loading more prepared ads when approaching threshold
        val currentMax = maxIndexReached.value
        if (index > currentMax) {
            maxIndexReached.value = index
            
            // Log that this ad is visible
            val ads = feedAds.value
            if (index < ads.size) {
                viewModelScope.launch {
                    feedUseCase.recordAdVisible(ads[index].id)
                    // Dispatch to our ad dispatcher in the background
                    adEngine.dispatcher.dispatchAdEvent(ads[index].id, "VISIBLE")
                }
            }
        }

        // Handle prefetching using AdvertisementEngine prefetch manager
        viewModelScope.launch {
            val categories = userProfile.value?.selectedCategories?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            adEngine.prefetchManager.onFeedScrollPositionChanged(
                visibleIndex = index,
                totalItemsCount = feedAds.value.size,
                isScrolling = isUserScrolling,
                categories = categories
            )
        }
    }

    /**
     * Triggered when an ad spends enough continuous duration in the viewport (validated impression).
     */
    fun onAdImpressionValidated(adId: String) {
        if (_validatedAdIds.value.contains(adId)) return
        _validatedAdIds.value = _validatedAdIds.value + adId

        viewModelScope.launch {
            val config = feedConfig.value
            val isMilestoneReached = feedUseCase.validateImpressionAndCheckReward(adId, config)
            if (isMilestoneReached) {
                val rewardAmount = com.example.data.config.AdConfig.FEED_BASE_REWARD_COINS
                userRepository.rewardCoinsForAd(
                    adId = "feed_milestone_$adId",
                    brandName = "Feed Watch Milestone Bonus",
                    coinsReward = rewardAmount,
                    isVideo = false
                )
                // Trigger flying coins animation
                _rewardEvent.emit(rewardAmount)
            }
        }
    }

    fun onAdClicked(ad: AdvertisementItem) {
        viewModelScope.launch {
            feedUseCase.recordAdClicked(ad.id)
        }
    }

    fun onAdShared(ad: AdvertisementItem) {
        viewModelScope.launch {
            feedUseCase.recordAdShared(ad.id)
        }
    }

    fun toggleSaveAd(ad: AdvertisementItem) {
        viewModelScope.launch {
            // Map AdvertisementItem back to DummyAd for compatibility with repository
            val dummy = DummyAd(
                id = ad.id,
                category = ad.category,
                brandName = ad.brandName,
                logo = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=100",
                mediaUrl = ad.imageUrl ?: ad.videoUrl ?: "",
                isVideo = ad.type == "VIDEO",
                title = ad.title,
                description = ad.description,
                rewardCoins = ad.rewardCoins,
                duration = 10,
                ctaText = ad.ctaText,
                sponsoredStatus = ad.isSponsored,
                productName = ad.productName
            )
            val isSaved = userRepository.isAdSaved(ad.id)
            if (isSaved) {
                userRepository.unsaveAd(ad.id)
            } else {
                userRepository.saveAd(dummy)
                feedUseCase.recordAdSaved(ad.id)
            }
        }
    }

    fun isAdSaved(adId: String): Flow<Boolean> {
        return userRepository.savedAds.map { list -> list.any { it.adId == adId } }
    }

    fun addScrollMetrics(delta: Float) {
        viewModelScope.launch {
            feedRepository.addScrollDistance(delta)
        }
    }

    fun updateConfigValue(newConfig: FeedConfig) {
        viewModelScope.launch {
            feedRepository.updateFeedConfig(newConfig)
        }
    }

    fun resetConfigToDefault() {
        viewModelScope.launch {
            feedRepository.updateFeedConfig(FeedConfig())
        }
    }

    fun clearAllStatsAndCache() {
        viewModelScope.launch {
            feedRepository.clearAllAnalytics()
            _validatedAdIds.value = emptySet()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Save total session duration on exit
        val duration = System.currentTimeMillis() - sessionStartTime
        viewModelScope.launch {
            feedRepository.addSessionDuration(duration)
        }
    }
}
