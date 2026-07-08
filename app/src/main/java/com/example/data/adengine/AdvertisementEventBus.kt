package com.example.data.adengine

import com.example.data.model.AdvertisementMetadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log

/**
 * Event-driven communication hub for Advertisement, Reward, Wallet, and Analytics modules.
 * Ensures strict decoupling by broadcasting standard lifecycle and system events.
 */
object AdvertisementEventBus {

    sealed class Event {
        data class AdLoaded(val metadata: AdvertisementMetadata) : Event()
        data class AdPrepared(val metadata: AdvertisementMetadata) : Event()
        data class AdVisible(val metadata: AdvertisementMetadata) : Event()
        data class AdClosed(val adId: String) : Event()
        data class AdFailed(val adId: String, val error: String) : Event()
        data class AdClicked(val metadata: AdvertisementMetadata) : Event()
        data class AdShared(val metadata: AdvertisementMetadata) : Event()
        data class AdSaved(val metadata: AdvertisementMetadata) : Event()
        data class RewardEligible(val adId: String, val coins: Int, val isVideo: Boolean) : Event()
        data class WalletUpdated(val currentCoins: Int, val deltaCoins: Int) : Event()
        data class AnalyticsUpdated(val metricName: String, val newValue: String) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 10,
        extraBufferCapacity = 128
    )
    val events = _events.asSharedFlow()

    fun emit(event: Event) {
        Log.d("AdEventBus", "Broadcasting event: ${event.javaClass.simpleName} to event bus.")
        val succeeded = _events.tryEmit(event)
        if (!succeeded) {
            Log.w("AdEventBus", "Buffer overflow, event discarded: $event")
        }
    }
}
