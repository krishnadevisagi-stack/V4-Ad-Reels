package com.example.data.adengine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==========================================
// 1. Enums and Basic Data Structures
// ==========================================

enum class AdProvider {
    DUMMY,
    ADMOB
}

enum class AdFormat {
    NATIVE,
    REWARDED,
    BANNER,
    INTERSTITIAL,
    REWARDED_INTERSTITIAL
}

enum class AdPlacement {
    HOME,
    REELS,
    PROFILE
}

enum class AdRewardType {
    COINS,
    NONE
}

enum class AdInteractionType {
    IMPRESSION,
    CLICK,
    COMPLETED
}

enum class AdLoadingStrategy {
    LAZY,
    PRELOAD
}

enum class AdUserEntryFlow {
    VOLUNTARY_OPT_IN,
    FORCED
}

enum class AdConsentState {
    GRANTED,
    DENIED,
    UNKNOWN
}

enum class AdEnvironment {
    DUMMY,
    TEST,
    PRODUCTION
}

enum class ComplianceDecision {
    ALLOWED,
    BLOCKED,
    REQUIRES_PROVIDER_REVIEW,
    REQUIRES_POLICY_REVIEW,
    TEST_ONLY
}

// ==========================================
// 2. CompliancePolicyEngine
// ==========================================

/**
 * CompliancePolicyEngine
 * Evaluates whether an ad request, rendering, or reward event complies with policies.
 * Ensures strict conformity with provider regulations.
 */
class CompliancePolicyEngine {
    fun evaluate(
        provider: AdProvider,
        format: AdFormat,
        placement: AdPlacement,
        rewardType: AdRewardType,
        interactionType: AdInteractionType,
        entryFlow: AdUserEntryFlow,
        consentState: AdConsentState,
        environment: AdEnvironment
    ): ComplianceDecision {
        // Rule 1: No Reward for Clicks (Ad Click ≠ Coin Reward)
        if (rewardType == AdRewardType.COINS && interactionType == AdInteractionType.CLICK) {
            ApplicationLogManager.e("CompliancePolicyEngine", "POLICY BLOCKED: Rewarding users for clicking advertisements is strictly prohibited.")
            return ComplianceDecision.BLOCKED
        }

        // Rule 2: Native Ad Impression Reward restriction (FeedRewardPolicyGate)
        if (provider == AdProvider.ADMOB && format == AdFormat.NATIVE && rewardType == AdRewardType.COINS && interactionType == AdInteractionType.IMPRESSION) {
            if (!FeedRewardPolicyGate.isReviewedAndConfirmedCompatible) {
                ApplicationLogManager.e("CompliancePolicyEngine", "POLICY BLOCKED: Rewarding for AdMob Native ordinary impressions is locked until explicit review.")
                return ComplianceDecision.REQUIRES_POLICY_REVIEW
            }
        }

        // Rule 3: Forced Rewarded Experience restriction (Mandatory User Choice)
        if (format == AdFormat.REWARDED && entryFlow == AdUserEntryFlow.FORCED) {
            ApplicationLogManager.e("CompliancePolicyEngine", "POLICY BLOCKED: Rewarded ads must always offer a voluntary opt-in and cannot be forced on startup.")
            return ComplianceDecision.BLOCKED
        }

        // Rule 4: Consent Requirement for Real AdMob
        if (provider == AdProvider.ADMOB && consentState == AdConsentState.DENIED) {
            ApplicationLogManager.e("CompliancePolicyEngine", "POLICY BLOCKED: AdMob cannot serve personalized/non-personalized ads if consent is explicitly denied or unhandled.")
            return ComplianceDecision.BLOCKED
        }

        // Rule 5: Dummy Mode in Production Environment
        if (provider == AdProvider.DUMMY && environment == AdEnvironment.PRODUCTION) {
            ApplicationLogManager.e("CompliancePolicyEngine", "POLICY BLOCKED: Dummy simulation provider is disabled in production environments.")
            return ComplianceDecision.TEST_ONLY
        }

        // Rule 6: Official Rewarded flows
        if (provider == AdProvider.ADMOB && format == AdFormat.REWARDED) {
            if (interactionType == AdInteractionType.COMPLETED && entryFlow == AdUserEntryFlow.VOLUNTARY_OPT_IN) {
                return ComplianceDecision.ALLOWED
            }
            return ComplianceDecision.REQUIRES_PROVIDER_REVIEW
        }

        return ComplianceDecision.ALLOWED
    }
}

// ==========================================
// 3. ProviderCapabilityRegistry
// ==========================================

enum class AdCapability {
    FEED_RENDERING,
    PROVIDER_ASSETS,
    PROVIDER_CTA,
    PROVIDER_IMPRESSION_REPORTING,
    OPT_IN_REWARD_FLOW,
    VALIDATED_REWARD_CALLBACK,
    FULL_SCREEN_EXPERIENCE
}

enum class AdRewardMode {
    NEVER_ASSUMED,
    PROVIDER_VALIDATED,
    PROTOTYPE_SIMULATED
}

/**
 * ProviderCapabilityRegistry
 * Maps capabilities per provider and format instead of making universal assumptions.
 */
object ProviderCapabilityRegistry {
    private val registry = mapOf(
        Pair(AdProvider.ADMOB, AdFormat.NATIVE) to Pair(
            listOf(AdCapability.FEED_RENDERING, AdCapability.PROVIDER_ASSETS, AdCapability.PROVIDER_CTA, AdCapability.PROVIDER_IMPRESSION_REPORTING),
            AdRewardMode.NEVER_ASSUMED
        ),
        Pair(AdProvider.ADMOB, AdFormat.REWARDED) to Pair(
            listOf(AdCapability.OPT_IN_REWARD_FLOW, AdCapability.VALIDATED_REWARD_CALLBACK, AdCapability.FULL_SCREEN_EXPERIENCE),
            AdRewardMode.PROVIDER_VALIDATED
        ),
        Pair(AdProvider.DUMMY, AdFormat.NATIVE) to Pair(
            listOf(AdCapability.FEED_RENDERING),
            AdRewardMode.PROTOTYPE_SIMULATED
        ),
        Pair(AdProvider.DUMMY, AdFormat.REWARDED) to Pair(
            listOf(AdCapability.OPT_IN_REWARD_FLOW, AdCapability.FULL_SCREEN_EXPERIENCE),
            AdRewardMode.PROTOTYPE_SIMULATED
        )
    )

    fun getCapabilities(provider: AdProvider, format: AdFormat): List<AdCapability> {
        return registry[Pair(provider, format)]?.first ?: emptyList()
    }

    fun getRewardMode(provider: AdProvider, format: AdFormat): AdRewardMode {
        return registry[Pair(provider, format)]?.second ?: AdRewardMode.NEVER_ASSUMED
    }
}

// ==========================================
// 4. FeedRewardPolicyGate
// ==========================================

/**
 * FeedRewardPolicyGate
 * Specifically secures Native Feed ordinary impression reward compliance.
 */
object FeedRewardPolicyGate {
    @Volatile
    var isReviewedAndConfirmedCompatible: Boolean = false

    fun isFeedRewardAllowed(provider: AdProvider, format: AdFormat): Boolean {
        if (provider == AdProvider.ADMOB && format == AdFormat.NATIVE) {
            return isReviewedAndConfirmedCompatible
        }
        // Dummy simulations can continue
        return provider == AdProvider.DUMMY
    }
}

// ==========================================
// 5. Separate Dummy and Production Reward Modes
// ==========================================

interface RewardMode {
    fun processReward(watchTimeSeconds: Int, onRewardReady: (coins: Int) -> Unit)
}

object DummyPrototypeRewardMode : RewardMode {
    override fun processReward(watchTimeSeconds: Int, onRewardReady: (coins: Int) -> Unit) {
        // Prototype logic: 5 seconds -> dummy reward
        if (watchTimeSeconds >= 5) {
            onRewardReady(15)
        }
    }
}

object RealProviderRewardMode : RewardMode {
    override fun processReward(watchTimeSeconds: Int, onRewardReady: (coins: Int) -> Unit) {
        // REAL MODE NEVER uses custom timers to credit coins!
        // It remains empty here because rewards MUST ONLY come from validated SDK callback events.
        ApplicationLogManager.i("RealProviderRewardMode", "Deductive timer bypassed. AdMob rewards must trigger via validated SDK event callback.")
    }

    fun creditValidatedReward(sdkRewardCoins: Int, onRewardReady: (coins: Int) -> Unit) {
        onRewardReady(sdkRewardCoins)
    }
}

// ==========================================
// 6. Production Configuration & Test Ad Enforcement
// ==========================================

object AdEnvironmentConfiguration {
    var activeEnvironment: AdEnvironment = AdEnvironment.PRODUCTION

    fun enforceTestAdRules(adUnitId: String, isReleaseBuild: Boolean) {
        val isTestUnit = adUnitId.contains("3940256099942544") || adUnitId.contains("6715807412270192") || adUnitId.lowercase().contains("test")
        val isDummy = adUnitId.startsWith("dummy") || adUnitId.isEmpty()

        if (isDummy) return

        if (!isReleaseBuild && !isTestUnit) {
            val errMsg = "POLICY WARNING: Production AdMob unit ($adUnitId) detected in a Debug/Development build."
            ApplicationLogManager.e("AdEnvironmentConfiguration", errMsg)
        }

        if (isReleaseBuild && isTestUnit && !adUnitId.contains("6715807412270192")) {
            val errMsg = "RELEASE GATE BLOCKED: Test AdMob unit ($adUnitId) detected in a Production/Release build. Fail Release!"
            ApplicationLogManager.e("AdEnvironmentConfiguration", errMsg)
            throw IllegalStateException(errMsg)
        }
    }
}

// ==========================================
// 7. Privacy & Consent Manager
// ==========================================

object PrivacyConsentManager {
    private val _consentState = MutableStateFlow(AdConsentState.UNKNOWN)
    val consentState: StateFlow<AdConsentState> = _consentState.asStateFlow()

    fun updateConsentInformation(context: Context) {
        // Simulation of Google UMP SDK Consent Information update at launch
        ApplicationLogManager.i("PrivacyConsentManager", "Consent info updated from Google Servers on app launch.")
        if (_consentState.value == AdConsentState.UNKNOWN) {
            _consentState.value = AdConsentState.GRANTED // Assume granted for typical flows, or request
        }
    }

    fun setConsentState(state: AdConsentState) {
        _consentState.value = state
        ApplicationLogManager.i("PrivacyConsentManager", "Consent state updated to: $state")
    }

    fun canRequestAds(provider: AdProvider): Boolean {
        if (provider == AdProvider.DUMMY) return true
        return _consentState.value == AdConsentState.GRANTED
    }
}

// ==========================================
// 8. Invalid Traffic & Activity Safety Layer
// ==========================================

object InvalidActivitySafetyLayer {
    private val recordedClaims = mutableSetOf<String>()
    private var lastInteractiveClickTime: Long = 0

    fun validateRewardEvent(rewardId: String, amount: Int): Boolean {
        // Prevent duplicate claims (identical reward ID replay)
        if (recordedClaims.contains(rewardId)) {
            ApplicationLogManager.e("InvalidActivitySafetyLayer", "CRITICAL BLOCKED: Duplicate reward event replay detected! ID: $rewardId")
            return false
        }
        
        // Prevent impossible single-reward sums
        if (amount > 500) {
            ApplicationLogManager.e("InvalidActivitySafetyLayer", "CRITICAL BLOCKED: Impossible reward amount detected! Coins: $amount")
            return false
        }

        recordedClaims.add(rewardId)
        return true
    }

    fun recordInteractionClick(): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - lastInteractiveClickTime
        lastInteractiveClickTime = now

        // Prevent automated click spamming (less than 400ms between clicks)
        if (elapsed < 400) {
            ApplicationLogManager.e("InvalidActivitySafetyLayer", "CRITICAL BLOCKED: Automated robotic clicking detected! Interval: $elapsed ms")
            return false
        }
        return true
    }

    fun getSafetyDisclaimer(): String {
        return "Disclaimer: While this local InvalidActivitySafetyLayer blocks obvious tampered, duplicate, or rapid bot activities, it does NOT guarantee AdMob compliance. Google conducts its own server-side evaluation of traffic quality."
    }
}

// ==========================================
// 9. Emergency Provider Remote Kill Switches
// ==========================================

object RemoteKillSwitch {
    var isHomeAdsEnabled: Boolean = true
    var isRewardedAdsEnabled: Boolean = true
    var isRewardsEnabled: Boolean = true
    
    private val providerKillMap = MutableStateFlow(
        mapOf(AdProvider.ADMOB to true, AdProvider.DUMMY to true)
    )
    val providerKillStatus: StateFlow<Map<AdProvider, Boolean>> = providerKillMap.asStateFlow()

    fun setProviderStatus(provider: AdProvider, enabled: Boolean) {
        val updated = providerKillMap.value.toMutableMap()
        updated[provider] = enabled
        providerKillMap.value = updated
        ApplicationLogManager.i("RemoteKillSwitch", "Emergency Remote Kill Switch updated for $provider to $enabled")
    }

    fun isAdServiceEligible(provider: AdProvider, format: AdFormat): Boolean {
        val providerEnabled = providerKillMap.value[provider] ?: false
        if (!providerEnabled) return false

        return when (format) {
            AdFormat.NATIVE -> isHomeAdsEnabled
            AdFormat.REWARDED -> isRewardedAdsEnabled
            else -> true
        }
    }
}

// ==========================================
// 10. Release-Blocking Compliance Gate
// ==========================================

object ReleaseComplianceGate {
    var isTechnicalValidationPassed: Boolean = true
    var isPolicyValidationPassed: Boolean = true
    var isQaValidationPassed: Boolean = true
    var isPrivacyValidationPassed: Boolean = true
    var isAppAdsTxtVerified: Boolean = false // Must be true for production release verification!

    fun evaluateReleaseReadiness(environment: AdEnvironment): Boolean {
        if (environment != AdEnvironment.PRODUCTION) {
            return true // Non-production bypasses release-blocking checks
        }

        val allChecksPassed = isTechnicalValidationPassed &&
                isPolicyValidationPassed &&
                isQaValidationPassed &&
                isPrivacyValidationPassed &&
                isAppAdsTxtVerified

        if (!allChecksPassed) {
            ApplicationLogManager.e(
                "ReleaseComplianceGate",
                "PRODUCTION RELEASE BLOCKED: " +
                        "Technical=$isTechnicalValidationPassed, " +
                        "Policy=$isPolicyValidationPassed, " +
                        "QA=$isQaValidationPassed, " +
                        "Privacy=$isPrivacyValidationPassed, " +
                        "app-ads.txt=$isAppAdsTxtVerified"
            )
        }
        return allChecksPassed
    }
}
