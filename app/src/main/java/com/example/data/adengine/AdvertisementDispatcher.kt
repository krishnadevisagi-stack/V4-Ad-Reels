package com.example.data.adengine

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AdvertisementDispatcher(
    private val scope: CoroutineScope
) {

    // Event type constants
    companion object {
        const val EVENT_LOADED = "LOADED"
        const val EVENT_VISIBLE = "VISIBLE"
        const val EVENT_THRESHOLD_REACHED = "THRESHOLD_REACHED"
        const val EVENT_COMPLETED = "COMPLETED"
        const val EVENT_CLICKED = "CLICKED"
        const val EVENT_SAVED = "SAVED"
        const val EVENT_SHARED = "SHARED"
    }

    data class AdEvent(
        val adId: String,
        val eventType: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class RewardCreditEvent(
        val adId: String,
        val coins: Int,
        val isVideo: Boolean
    )

    private val _adEventFlow = MutableSharedFlow<AdEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val adEventFlow = _adEventFlow.asSharedFlow()

    private val _rewardCreditFlow = MutableSharedFlow<RewardCreditEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val rewardCreditFlow = _rewardCreditFlow.asSharedFlow()

    fun dispatchAdEvent(adId: String, eventType: String) {
        scope.launch {
            Log.d("AdDispatcher", "Dispatching ad event: $eventType for $adId in background.")
            _adEventFlow.emit(AdEvent(adId, eventType))
        }
    }

    fun dispatchRewardCredit(adId: String, coins: Int, isVideo: Boolean) {
        scope.launch {
            Log.d("AdDispatcher", "Dispatching reward credit: +$coins coins for ad $adId.")
            _rewardCreditFlow.emit(RewardCreditEvent(adId, coins, isVideo))
        }
    }
}
