package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AdRepository
import com.example.data.repository.UserRepository
import com.example.data.adengine.SecurityChecker
import com.example.data.adengine.SecurityThreats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * -----------------------------------------------------------------
 * AD ADAPTER & CAMPAIGN VIEW MODEL
 * -----------------------------------------------------------------
 * Purpose: Manages state flows for personalized category choices and ad preference updates.
 * Responsibilities:
 *   - Display and update user category filters and ad preferences.
 *   - Log campaign interactions and support bookmarking of advertisements.
 * Dependencies:
 *   - [UserRepository], [AdRepository]
 * Future Extension: Sync interest profiles to remote databases for server-side segmentation.
 */
class AdViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    
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
    
    val adRepository = AdRepository()

    val settingsManager = com.example.data.config.AppSettingsManager.getInstance(application)
    val isDarkMode: StateFlow<Boolean> = settingsManager.isDarkMode
    val autoPlayOption: StateFlow<String> = settingsManager.autoPlayOption
    val autoMuteOption: StateFlow<Boolean> = settingsManager.autoMuteOption
    val pushNotificationsEnabled: StateFlow<Boolean> = settingsManager.pushNotificationsEnabled
    val rewardAnimationsEnabled: StateFlow<Boolean> = settingsManager.rewardAnimationsEnabled
    val selectedLanguage: StateFlow<String> = settingsManager.selectedLanguage

    fun setDarkMode(enabled: Boolean) {
        settingsManager.setDarkMode(enabled)
    }

    fun setAutoPlayOption(option: String) {
        settingsManager.setAutoPlayOption(option)
    }

    fun setAutoMuteOption(enabled: Boolean) {
        settingsManager.setAutoMuteOption(enabled)
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        settingsManager.setPushNotificationsEnabled(enabled)
    }

    fun setRewardAnimationsEnabled(enabled: Boolean) {
        settingsManager.setRewardAnimationsEnabled(enabled)
    }

    fun setSelectedLanguage(language: String) {
        settingsManager.setSelectedLanguage(language)
    }

    // UI States
    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val watchHistory: StateFlow<List<WatchHistory>> = userRepository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedAds: StateFlow<List<SavedAd>> = userRepository.savedAds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coinHistory: StateFlow<List<CoinHistory>> = userRepository.coinHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val redeemHistory: StateFlow<List<RedeemHistory>> = userRepository.redeemHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adStats: StateFlow<List<AdStatsEntry>> = userRepository.adStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category lists derived from active user profile interests
    val homeAds: StateFlow<List<DummyAd>> = userProfile
        .map { profile ->
            val categories = profile?.selectedCategories?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            adRepository.getHomeFeedAds(categories)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reelsAds: StateFlow<List<DummyAd>> = userProfile
        .map { profile ->
            val categories = profile?.selectedCategories?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            adRepository.getReelsAds(categories)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keep track of watched ads to avoid multiple payouts in the same session
    private val _rewardedAdIds = MutableStateFlow<Set<String>>(emptySet())
    val rewardedAdIds: StateFlow<Set<String>> = _rewardedAdIds.asStateFlow()

    // NEW: Feed views impression tracker (5 impressions required)
    private val _feedViewProgress = MutableStateFlow(0)
    val feedViewProgress: StateFlow<Int> = _feedViewProgress.asStateFlow()

    // NEW: Reels milestones rewarded trackers
    private val _reels5sRewardedAdIds = MutableStateFlow<Set<String>>(emptySet())
    val reels5sRewardedAdIds: StateFlow<Set<String>> = _reels5sRewardedAdIds.asStateFlow()

    private val _reels15sRewardedAdIds = MutableStateFlow<Set<String>>(emptySet())
    val reels15sRewardedAdIds: StateFlow<Set<String>> = _reels15sRewardedAdIds.asStateFlow()

    // NEW: Coin Fly Animation events
    data class CoinAnimationTrigger(
        val id: Long = System.currentTimeMillis() + (0..1000).random(),
        val amount: Int
    )

    private val _coinAnimEvent = MutableSharedFlow<CoinAnimationTrigger>(extraBufferCapacity = 8)
    val coinAnimEvent = _coinAnimEvent.asSharedFlow()

    // Central Security Threat State Flow
    private val _securityStatus = MutableStateFlow<SecurityThreats?>(null)
    val securityStatus: StateFlow<SecurityThreats?> = _securityStatus.asStateFlow()

    fun runSecurityCheck() {
        viewModelScope.launch {
            val result = SecurityChecker.checkSecurity(getApplication())
            _securityStatus.value = result
            if (!result.isSecure()) {
                com.example.data.adengine.ApplicationLogManager.e("AdViewModel", "Security threat detected! Blocking operations. Details: ${result.details}")
            }
        }
    }

    init {
        // Run security checks on initialization
        runSecurityCheck()
    }

    fun logAdStat(adType: String, adUnitId: String, action: String) {
        viewModelScope.launch {
            userRepository.logAdStat(adType, adUnitId, action)
        }
    }

    fun syncAdStatsSummary() {
        viewModelScope.launch {
            userRepository.syncAdStatsSummary()
        }
    }

    fun registerUser(username: String, email: String, interests: List<String>) {
        viewModelScope.launch {
            userRepository.registerUserProfile(username, email, interests)
        }
    }

    fun loginUser(emailOrMobile: String, passwordRaw: String, onSuccess: (UserProfile) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            userRepository.loginUser(emailOrMobile, passwordRaw)
                .onSuccess { profile ->
                    // Return onSuccess immediately to display local cache/host data first
                    onSuccess(profile)
                    // Trigger deep cloud sync silently in the background
                    com.example.data.firebase.FirebaseManager.syncCloudDataToLocal(profile.email, viewModelScope) {
                        // Silently updated local Room DB; StateFlows will automatically propagate fresh values
                    }
                }
                .onFailure { onError(it.message ?: "Login failed.") }
        }
    }

    fun registerNewUser(
        username: String,
        fullName: String,
        email: String,
        mobile: String,
        passwordRaw: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            userRepository.registerUser(username, fullName, email, mobile, passwordRaw)
                .onSuccess { profile ->
                    // Return onSuccess immediately to display local cache/host data first
                    onSuccess(profile)
                    // Trigger deep cloud sync silently in the background
                    com.example.data.firebase.FirebaseManager.syncCloudDataToLocal(profile.email, viewModelScope) {
                        // Silently updated local Room DB; StateFlows will automatically propagate fresh values
                    }
                }
                .onFailure { onError(it.message ?: "Registration failed.") }
        }
    }

    fun signInWithGoogleUser(
        email: String,
        displayName: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            userRepository.loginOrRegisterGoogle(email, displayName)
                .onSuccess { profile ->
                    // Return onSuccess immediately to display local cache/host data first
                    onSuccess(profile)
                    // Trigger deep cloud sync silently in the background
                    com.example.data.firebase.FirebaseManager.syncCloudDataToLocal(profile.email, viewModelScope) {
                        // Silently updated local Room DB; StateFlows will automatically propagate fresh values
                    }
                }
                .onFailure { onError(it.message ?: "Google Sign-In failed.") }
        }
    }

    fun continueAsGuest(onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            val guestProfile = userRepository.loginAsGuest()
            onSuccess(guestProfile)
        }
    }

    fun upgradeGuestToUser(
        fullName: String,
        email: String,
        mobile: String,
        passwordRaw: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            userRepository.convertGuestToRegistered(fullName, email, mobile, passwordRaw)
                .onSuccess(onSuccess)
                .onFailure { onError(it.message ?: "Upgrade failed.") }
        }
    }

    // NEW: Generalized coin rewarding method
    fun rewardUserCoins(coinsReward: Int, title: String, isVideo: Boolean, adId: String = "generic_reward_${System.currentTimeMillis()}") {
        viewModelScope.launch {
            // HIGH SECURITY CHECK: If threats exist or the app has been modified/tampered, prevent ad reward coin allocation.
            val currentSecurity = _securityStatus.value ?: SecurityChecker.checkSecurity(getApplication())
            _securityStatus.value = currentSecurity
            if (!currentSecurity.isSecure()) {
                com.example.data.adengine.ApplicationLogManager.e("AdViewModel", "SECURITY ALERT: Blocked rewarding $coinsReward coins for '$title' due to active security threat: ${currentSecurity.details}")
                return@launch
            }

            userRepository.rewardCoinsForAd(
                adId = adId,
                brandName = title,
                coinsReward = coinsReward,
                isVideo = isVideo
            )
            // Trigger flying coin animation
            _coinAnimEvent.emit(CoinAnimationTrigger(amount = coinsReward))
        }
    }

    // NEW: Called when feed ad completes 100% (4 seconds watched)
    fun onFeedAdWatchedComplete(ad: DummyAd) {
        if (_rewardedAdIds.value.contains(ad.id)) return
        _rewardedAdIds.value = _rewardedAdIds.value + ad.id

        val currentProgress = _feedViewProgress.value + 1
        if (currentProgress >= com.example.data.config.AdConfig.FEED_IMPRESSIONS_REQUIRED) {
            _feedViewProgress.value = 0
            rewardUserCoins(
                coinsReward = com.example.data.config.AdConfig.FEED_BASE_REWARD_COINS,
                title = "Feed Watch Milestone Bonus",
                isVideo = false,
                adId = "feed_milestone_${ad.id}"
            )
        } else {
            _feedViewProgress.value = currentProgress
        }
    }

    // NEW: Called dynamically during reels watch playback
    fun onReelsWatchDuration(ad: DummyAd, playbackMs: Long) {
        if (playbackMs >= 10000) {
            if (!_reels15sRewardedAdIds.value.contains(ad.id)) {
                _reels15sRewardedAdIds.value = _reels15sRewardedAdIds.value + ad.id
                rewardUserCoins(
                    coinsReward = com.example.data.config.AdConfig.REELS_LONGER_REWARD_COINS,
                    title = "${ad.brandName} (10s Watch)",
                    isVideo = true,
                    adId = "reels_10s_${ad.id}"
                )
            }
        } else if (playbackMs >= 5000) {
            if (!_reels5sRewardedAdIds.value.contains(ad.id)) {
                _reels5sRewardedAdIds.value = _reels5sRewardedAdIds.value + ad.id
                rewardUserCoins(
                    coinsReward = com.example.data.config.AdConfig.REELS_BASE_REWARD_COINS,
                    title = "${ad.brandName} (5s Watch)",
                    isVideo = true,
                    adId = "reels_5s_${ad.id}"
                )
            }
        }
    }

    // Fallback/Legacy support method (not used directly, but kept for compatibility)
    fun rewardUser(ad: DummyAd) {
        if (_rewardedAdIds.value.contains(ad.id)) return
        _rewardedAdIds.value = _rewardedAdIds.value + ad.id
        rewardUserCoins(
            coinsReward = ad.rewardCoins,
            title = ad.brandName,
            isVideo = ad.isVideo,
            adId = ad.id
        )
    }

    fun toggleSaveAd(ad: DummyAd) {
        viewModelScope.launch {
            val saved = userRepository.isAdSaved(ad.id)
            if (saved) {
                userRepository.unsaveAd(ad.id)
            } else {
                userRepository.saveAd(ad)
            }
        }
    }

    fun isAdSaved(adId: String): Flow<Boolean> {
        return savedAds.map { list -> list.any { it.adId == adId } }
    }

    fun updateInterests(interests: List<String>) {
        viewModelScope.launch {
            userRepository.updateSelectedCategories(interests)
        }
    }

    fun redeemReward(itemTitle: String, coinsCost: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = userRepository.redeemRewardPoints(itemTitle, coinsCost)
            if (success) {
                onSuccess()
            } else {
                onError("Insufficient coins in your wallet.")
            }
        }
    }

    fun withdrawCash(amount: Double, method: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = userRepository.withdrawCashBalance(amount, method)
            if (success) {
                onSuccess()
            } else {
                onError("Insufficient balance or error processing withdrawal.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _rewardedAdIds.value = emptySet()
        }
    }

    fun clearAppDataAndLogs(context: android.content.Context) {
        viewModelScope.launch {
            userRepository.clearAllLocalData()
            _rewardedAdIds.value = emptySet()
            android.widget.Toast.makeText(context, "Database, logs and cache cleared successfully!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun resetAllAnalyticsLogsAndCache() {
        viewModelScope.launch {
            // 1. Clear AdStats entries
            db.adStatsDao().clearAdStats()
            
            // 2. Clear Feed Analytics and event logs
            db.feedAnalyticsDao().insertOrUpdateAnalytics(FeedAnalytics())
            db.feedEventLogDao().clearLogs()
            db.impressionCacheDao().clearCache()
            
            // 3. Clear Reels Analytics and event logs
            db.reelsAnalyticsDao().insertOrUpdateAnalytics(ReelsAnalytics())
            db.reelsEventLogDao().clearLogs()
            db.reelsRewardHistoryDao().clearHistory()
            
            // 4. Reset in-memory AnalyticsManager flows
            com.example.data.adengine.AnalyticsManager.resetAllAnalytics()
        }
    }
}
