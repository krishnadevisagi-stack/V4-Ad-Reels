package com.example.data.usecase

import com.example.data.model.*
import com.example.data.repository.ReelsRepository
import com.example.data.utils.ReelsRewardEngine
import kotlinx.coroutines.flow.Flow

/**
 * -----------------------------------------------------------------
 * REELS INTERACTION USE CASE
 * -----------------------------------------------------------------
 * Purpose: Business-logic orchestrator for Reels short video interactions.
 * Responsibilities:
 *   - Fetch high-quality video ads based on categories.
 *   - Route playback watch seconds progress events to the reward engine for verification.
 * Dependencies:
 *   - [ReelsRepository], [ReelsRewardEngine]
 * Future Extension: Support multi-tiered reward milestone multipliers for consecutive watch sequences.
 */
class ReelsUseCase(
    private val reelsRepository: ReelsRepository,
    private val reelsRewardEngine: ReelsRewardEngine
) {
    val configFlow: Flow<ReelsConfig?> = reelsRepository.configFlow
    val analyticsFlow: Flow<ReelsAnalytics?> = reelsRepository.analyticsFlow
    val eventLogsFlow: Flow<List<ReelsEventLog>> = reelsRepository.eventLogsFlow

    suspend fun getReelsAdsForCategories(categories: List<String>): List<ReelAdvertisementItem> {
        return reelsRepository.fetchReelsAdsForCategories(categories)
    }

    suspend fun evaluateWatchProgress(
        adId: String,
        durationMs: Long,
        config: ReelsConfig
    ): ReelsRewardEngine.RewardStatus {
        return reelsRewardEngine.evaluateWatchProgress(adId, durationMs, config)
    }

    suspend fun recordEvent(eventType: String, adId: String) {
        reelsRepository.recordEvent(eventType, adId)
    }

    suspend fun incrementReelsOpenCount() {
        reelsRepository.incrementReelsOpenCount()
    }

    suspend fun addWatchDuration(durationMs: Long) {
        reelsRepository.addWatchDuration(durationMs)
    }

    suspend fun updateReelsConfig(config: ReelsConfig) {
        reelsRepository.updateReelsConfig(config)
    }

    suspend fun addSessionDuration(durationMs: Long) {
        reelsRepository.addSessionDuration(durationMs)
    }

    suspend fun clearAllAnalytics() {
        reelsRepository.clearAllAnalytics()
    }

    suspend fun hasRewarded(adId: String): Boolean {
        return reelsRepository.hasRewarded(adId)
    }
}
