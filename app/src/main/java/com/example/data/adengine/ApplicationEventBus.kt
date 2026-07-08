package com.example.data.adengine

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * ApplicationEventBus
 * Every important action in the application creates an Event and passes through this bus.
 * Direct communication between modules is discouraged.
 */
object ApplicationEventBus {

    sealed class Event {
        data class FeedOpened(val timestamp: Long) : Event()
        data class FeedClosed(val sessionDurationMs: Long) : Event()
        data class ReelsOpened(val timestamp: Long) : Event()
        data class WalletUpdated(val userId: String, val currentBalance: Int, val delta: Int, val source: String) : Event()
        data class RewardAdded(val userId: String, val rewardId: String, val amountCoins: Int, val source: String) : Event()
        data class RedeemRequested(val userId: String, val redeemType: String, val amountCoins: Int, val details: String) : Event()
        data class SettingsChanged(val settingKey: String, val value: String) : Event()
        data class CategoryChanged(val userId: String, val category: String, val enabled: Boolean) : Event()
        data class Login(val userId: String, val timestamp: Long) : Event()
        data class Logout(val userId: String, val timestamp: Long) : Event()
        data class PerformanceMetric(val metricName: String, val valueMs: Long) : Event()
        data class ErrorLogged(val errorType: String, val message: String) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 20,
        extraBufferCapacity = 256
    )
    val events = _events.asSharedFlow()

    fun emit(event: Event) {
        Log.d("AppEventBus", "Emitting Event: ${event.javaClass.simpleName} -> $event")
        _events.tryEmit(event)
    }
}
