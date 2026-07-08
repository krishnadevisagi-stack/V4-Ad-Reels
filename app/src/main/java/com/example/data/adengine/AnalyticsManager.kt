package com.example.data.adengine

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AnalyticsManager
 * Responsibilities:
 *  - Subscribes to ApplicationEventBus.
 *  - Stores Events locally in memory.
 *  - Aggregates Statistics (Feed Analytics, Reels Analytics, Wallet Analytics, User Activity).
 *  - Updates Dashboard state dynamically.
 *  - Generates analytical reports.
 */
object AnalyticsManager {

    private val scope = CoroutineScope(Dispatchers.Default)

    // Aggregate stats state flows for the UI to observe
    private val _feedOpenCount = MutableStateFlow(0)
    val feedOpenCount = _feedOpenCount.asStateFlow()

    private val _feedCloseCount = MutableStateFlow(0)
    val feedCloseCount = _feedCloseCount.asStateFlow()

    private val _feedSessionTimeMs = MutableStateFlow(0L)
    val feedSessionTimeMs = _feedSessionTimeMs.asStateFlow()

    private val _feedScrollDistance = MutableStateFlow(0)
    val feedScrollDistance = _feedScrollDistance.asStateFlow()

    private val _feedScrollSpeed = MutableStateFlow(0f)
    val feedScrollSpeed = _feedScrollSpeed.asStateFlow()

    private val _feedLoadingTimeMs = MutableStateFlow(0L)
    val feedLoadingTimeMs = _feedLoadingTimeMs.asStateFlow()

    private val _feedRefreshCount = MutableStateFlow(0)
    val feedRefreshCount = _feedRefreshCount.asStateFlow()

    private val _categoryUsage = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryUsage = _categoryUsage.asStateFlow()

    private val _reelsOpenCount = MutableStateFlow(0)
    val reelsOpenCount = _reelsOpenCount.asStateFlow()

    private val _reelsCloseCount = MutableStateFlow(0)
    val reelsCloseCount = _reelsCloseCount.asStateFlow()

    private val _reelsSessionTimeMs = MutableStateFlow(0L)
    val reelsSessionTimeMs = _reelsSessionTimeMs.asStateFlow()

    private val _rewardedAdAttempts = MutableStateFlow(0)
    val rewardedAdAttempts = _rewardedAdAttempts.asStateFlow()

    private val _rewardedAdCompletions = MutableStateFlow(0)
    val rewardedAdCompletions = _rewardedAdCompletions.asStateFlow()

    private val _rewardEventsCount = MutableStateFlow(0)
    val rewardEventsCount = _rewardEventsCount.asStateFlow()

    // Wallet Analytics
    private val _coinsEarned = MutableStateFlow(0)
    val coinsEarned = _coinsEarned.asStateFlow()

    private val _coinsRedeemed = MutableStateFlow(0)
    val coinsRedeemed = _coinsRedeemed.asStateFlow()

    private val _walletBalance = MutableStateFlow(0)
    val walletBalance = _walletBalance.asStateFlow()

    private val _pendingRequestsCount = MutableStateFlow(0)
    val pendingRequestsCount = _pendingRequestsCount.asStateFlow()

    private val _withdrawalRequestsCount = MutableStateFlow(0)
    val withdrawalRequestsCount = _withdrawalRequestsCount.asStateFlow()

    private val _voucherRequestsCount = MutableStateFlow(0)
    val voucherRequestsCount = _voucherRequestsCount.asStateFlow()

    private val _dailyCoinsEarned = MutableStateFlow(0)
    val dailyCoinsEarned = _dailyCoinsEarned.asStateFlow()

    private val _weeklyCoinsEarned = MutableStateFlow(0)
    val weeklyCoinsEarned = _weeklyCoinsEarned.asStateFlow()

    private val _lifetimeCoinsEarned = MutableStateFlow(0)
    val lifetimeCoinsEarned = _lifetimeCoinsEarned.asStateFlow()

    // Timeline of all important user activity events
    private val _userActivityTimeline = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val userActivityTimeline = _userActivityTimeline.asStateFlow()

    data class ActivityRecord(
        val title: String,
        val details: String,
        val timestamp: Long
    )

    init {
        // Start listening to the Event Bus
        scope.launch {
            ApplicationEventBus.events.collect { event ->
                processEvent(event)
            }
        }
    }

    private fun processEvent(event: ApplicationEventBus.Event) {
        val now = System.currentTimeMillis()
        when (event) {
            is ApplicationEventBus.Event.FeedOpened -> {
                _feedOpenCount.value += 1
                addTimelineRecord("Feed Opened", "User accessed Feed list", now)
            }
            is ApplicationEventBus.Event.FeedClosed -> {
                _feedCloseCount.value += 1
                _feedSessionTimeMs.value += event.sessionDurationMs
                addTimelineRecord("Feed Closed", "Duration: ${event.sessionDurationMs / 1000}s", now)
            }
            is ApplicationEventBus.Event.ReelsOpened -> {
                _reelsOpenCount.value += 1
                addTimelineRecord("Reels Opened", "User accessed AdReels player", now)
            }
            is ApplicationEventBus.Event.WalletUpdated -> {
                _walletBalance.value = event.currentBalance
                if (event.delta > 0) {
                    _coinsEarned.value += event.delta
                    _dailyCoinsEarned.value += event.delta
                    _weeklyCoinsEarned.value += event.delta
                    _lifetimeCoinsEarned.value += event.delta
                } else if (event.delta < 0) {
                    _coinsRedeemed.value += (-event.delta)
                }
                addTimelineRecord("Wallet Updated", "Delta: ${event.delta} coins via ${event.source}", now)
            }
            is ApplicationEventBus.Event.RewardAdded -> {
                _rewardEventsCount.value += 1
                _rewardedAdCompletions.value += 1
                addTimelineRecord("Reward Earned", "Awarded ${event.amountCoins} coins from ${event.source}", now)
            }
            is ApplicationEventBus.Event.RedeemRequested -> {
                if (event.redeemType == "UPI") {
                    _withdrawalRequestsCount.value += 1
                } else {
                    _voucherRequestsCount.value += 1
                }
                addTimelineRecord("Redeem Requested", "Requested ${event.redeemType} for ${event.amountCoins} coins", now)
            }
            is ApplicationEventBus.Event.SettingsChanged -> {
                addTimelineRecord("Setting Changed", "${event.settingKey} changed to ${event.value}", now)
            }
            is ApplicationEventBus.Event.CategoryChanged -> {
                val current = _categoryUsage.value.toMutableMap()
                val count = current.getOrDefault(event.category, 0)
                current[event.category] = if (event.enabled) count + 1 else (count - 1).coerceAtLeast(0)
                _categoryUsage.value = current
                addTimelineRecord("Category Updated", "Interests in ${event.category} set to ${event.enabled}", now)
            }
            is ApplicationEventBus.Event.Login -> {
                addTimelineRecord("User Login", "User ID ${event.userId} successfully logged in", now)
            }
            is ApplicationEventBus.Event.Logout -> {
                addTimelineRecord("User Logout", "User ID ${event.userId} successfully logged out", now)
            }
            is ApplicationEventBus.Event.PerformanceMetric -> {
                if (event.metricName == "FEED_LOAD_TIME") {
                    _feedLoadingTimeMs.value = event.valueMs
                }
            }
            is ApplicationEventBus.Event.ErrorLogged -> {
                // Ignore for general analytics dashboard
            }
        }
    }

    private fun addTimelineRecord(title: String, details: String, timestamp: Long) {
        val record = ActivityRecord(title, details, timestamp)
        val current = _userActivityTimeline.value.toMutableList()
        current.add(0, record) // Newest first
        if (current.size > 50) {
            current.removeAt(current.size - 1) // Cap at 50 logs
        }
        _userActivityTimeline.value = current
    }

    fun recordScroll(distance: Int, speed: Float) {
        _feedScrollDistance.value += distance
        _feedScrollSpeed.value = speed
    }

    fun recordRefresh() {
        _feedRefreshCount.value += 1
    }

    fun recordAdAttempt() {
        _rewardedAdAttempts.value += 1
    }

    fun getAnalyticsSummaryReport(): String {
        return """
            --- Application Analytics Summary ---
            Feed Sessions: Open Count: ${_feedOpenCount.value}, Close Count: ${_feedCloseCount.value}
            Feed Active Duration: ${_feedSessionTimeMs.value / 1000} seconds
            Feed Total Scroll: ${_feedScrollDistance.value} dp
            Ad Reels Sessions: Open Count: ${_reelsOpenCount.value}
            Rewarded Ad Completion Rate: ${_rewardedAdCompletions.value}/${_rewardedAdAttempts.value}
            Lifetime Coins Accrued: ${_lifetimeCoinsEarned.value} coins
            Lifetime Coins Cashed out: ${_coinsRedeemed.value} coins
            Current Balance: ${_walletBalance.value} coins
        """.trimIndent()
    }

    fun resetAllAnalytics() {
        _feedOpenCount.value = 0
        _feedCloseCount.value = 0
        _feedSessionTimeMs.value = 0L
        _feedScrollDistance.value = 0
        _feedScrollSpeed.value = 0f
        _feedLoadingTimeMs.value = 0L
        _feedRefreshCount.value = 0
        _categoryUsage.value = emptyMap()
        _reelsOpenCount.value = 0
        _reelsCloseCount.value = 0
        _reelsSessionTimeMs.value = 0L
        _rewardedAdAttempts.value = 0
        _rewardedAdCompletions.value = 0
        _rewardEventsCount.value = 0
        _coinsEarned.value = 0
        _coinsRedeemed.value = 0
        _walletBalance.value = 0
        _pendingRequestsCount.value = 0
        _withdrawalRequestsCount.value = 0
        _voucherRequestsCount.value = 0
        _dailyCoinsEarned.value = 0
        _weeklyCoinsEarned.value = 0
        _lifetimeCoinsEarned.value = 0
        _userActivityTimeline.value = emptyList()
    }
}
