package com.example.data.adengine

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// ============================================================================
// 1. Future Policy and Analytics Namespaces (Chapter 9)
// ============================================================================

enum class PolicyReviewStatus {
    CURRENT,
    REVIEW_DUE,
    EXPIRED,
    BLOCKED
}

enum class RewardPolicyNamespace {
    DUMMY_POLICY,
    ADMOB_NATIVE_POLICY,
    ADMOB_REWARDED_POLICY,
    FUTURE_PROVIDER_POLICY,
    DIRECT_CAMPAIGN_POLICY
}

enum class AnalyticsNamespace {
    APP_ANALYTICS,
    PROVIDER_REPORTED_ANALYTICS,
    DIRECT_CAMPAIGN_ANALYTICS,
    WALLET_ANALYTICS,
    REWARD_ANALYTICS
}

// ============================================================================
// 2. Direct Campaign & Advertiser Support
// ============================================================================

enum class CampaignStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    PAUSED,
    EXPIRED
}

data class DirectCampaign(
    val id: String = UUID.randomUUID().toString(),
    val advertiserName: String,
    val productName: String,
    val category: String,
    val budget: Double,
    val creativeAssetUrl: String,
    val targetAudience: String,
    val rewardCoins: Int,
    val destinationUrl: String,
    var status: CampaignStatus = CampaignStatus.DRAFT,
    var impressions: Int = 0,
    var clicks: Int = 0,
    var isPolicyVerified: Boolean = false
)

// ============================================================================
// 3. Provider Adapter Contract & Registry
// ============================================================================

interface AdvertisementProviderAdapter {
    val providerId: String
    val providerName: String
    fun initialize()
    fun requestAd(placement: AdPlacement, format: AdFormat): Boolean
    fun getCapabilities(): List<AdCapability>
    fun getRewardMode(): AdRewardMode
    fun getReviewStatus(): PolicyReviewStatus
}

enum class TechnicalAvailability {
    SUPPORTED,
    UNSUPPORTED,
    REQUIRES_REVIEW,
    DISABLED,
    TEST_ONLY
}

object AdvertisementProviderRegistry {
    private val adapters = mutableMapOf<String, AdvertisementProviderAdapter>()
    private val availabilityMap = mutableMapOf<String, TechnicalAvailability>()

    init {
        // Register default simulated adapters for Phase 1 & Phase 2
        registerAdapter(object : AdvertisementProviderAdapter {
            override val providerId: String = "DUMMY"
            override val providerName: String = "Dummy Ad Platform"
            override fun initialize() {}
            override fun requestAd(placement: AdPlacement, format: AdFormat) = true
            override fun getCapabilities() = listOf(AdCapability.FEED_RENDERING, AdCapability.OPT_IN_REWARD_FLOW)
            override fun getRewardMode() = AdRewardMode.PROTOTYPE_SIMULATED
            override fun getReviewStatus() = PolicyReviewStatus.CURRENT
        }, TechnicalAvailability.TEST_ONLY)

        registerAdapter(object : AdvertisementProviderAdapter {
            override val providerId: String = "ADMOB"
            override val providerName: String = "Google AdMob"
            override fun initialize() {}
            override fun requestAd(placement: AdPlacement, format: AdFormat) = true
            override fun getCapabilities() = listOf(AdCapability.FEED_RENDERING, AdCapability.PROVIDER_ASSETS, AdCapability.PROVIDER_CTA, AdCapability.VALIDATED_REWARD_CALLBACK)
            override fun getRewardMode() = AdRewardMode.PROVIDER_VALIDATED
            override fun getReviewStatus() = PolicyReviewStatus.CURRENT
        }, TechnicalAvailability.SUPPORTED)

        registerAdapter(object : AdvertisementProviderAdapter {
            override val providerId: String = "INMOBI"
            override val providerName: String = "InMobi Platform"
            override fun initialize() {}
            override fun requestAd(placement: AdPlacement, format: AdFormat) = true
            override fun getCapabilities() = listOf(AdCapability.FEED_RENDERING, AdCapability.PROVIDER_ASSETS)
            override fun getRewardMode() = AdRewardMode.NEVER_ASSUMED
            override fun getReviewStatus() = PolicyReviewStatus.REVIEW_DUE
        }, TechnicalAvailability.REQUIRES_REVIEW)

        registerAdapter(object : AdvertisementProviderAdapter {
            override val providerId: String = "APPLOVIN"
            override val providerName: String = "AppLovin MAX"
            override fun initialize() {}
            override fun requestAd(placement: AdPlacement, format: AdFormat) = false
            override fun getCapabilities() = listOf(AdCapability.FULL_SCREEN_EXPERIENCE)
            override fun getRewardMode() = AdRewardMode.PROVIDER_VALIDATED
            override fun getReviewStatus() = PolicyReviewStatus.EXPIRED
        }, TechnicalAvailability.DISABLED)

        registerAdapter(object : AdvertisementProviderAdapter {
            override val providerId: String = "DIRECT_CAMPAIGNS"
            override val providerName: String = "Direct Advertiser Network"
            override fun initialize() {}
            override fun requestAd(placement: AdPlacement, format: AdFormat) = true
            override fun getCapabilities() = listOf(AdCapability.FEED_RENDERING, AdCapability.PROVIDER_ASSETS, AdCapability.PROVIDER_CTA)
            override fun getRewardMode() = AdRewardMode.PROVIDER_VALIDATED
            override fun getReviewStatus() = PolicyReviewStatus.CURRENT
        }, TechnicalAvailability.SUPPORTED)
    }

    fun registerAdapter(adapter: AdvertisementProviderAdapter, availability: TechnicalAvailability) {
        adapters[adapter.providerId] = adapter
        availabilityMap[adapter.providerId] = availability
    }

    fun getAdapter(providerId: String): AdvertisementProviderAdapter? = adapters[providerId]

    fun getAvailability(providerId: String): TechnicalAvailability = availabilityMap[providerId] ?: TechnicalAvailability.UNSUPPORTED

    fun getAllAdapters(): List<AdvertisementProviderAdapter> = adapters.values.toList()

    fun updateAvailability(providerId: String, availability: TechnicalAvailability) {
        availabilityMap[providerId] = availability
    }
}

// ============================================================================
// 4. Provider Selection Engine with Governance Order
// ============================================================================

object AdvertisementProviderSelector {
    
    fun selectProvider(
        placement: AdPlacement,
        format: AdFormat,
        consentState: AdConsentState,
        environment: AdEnvironment,
        logs: MutableList<String>
    ): AdvertisementProviderAdapter? {
        logs.clear()
        logs.add("🏁 Initiating capability-aware selection algorithm.")

        val adapters = AdvertisementProviderRegistry.getAllAdapters()

        for (adapter in adapters) {
            val pid = adapter.providerId
            logs.add("🔍 Evaluating provider: $pid (${adapter.providerName})")

            // Rule 1: Compliance Checking
            val availability = AdvertisementProviderRegistry.getAvailability(pid)
            if (availability == TechnicalAvailability.DISABLED) {
                logs.add("❌ $pid is explicitly marked as DISABLED in registry. Skip.")
                continue
            }
            if (availability == TechnicalAvailability.TEST_ONLY && environment == AdEnvironment.PRODUCTION) {
                logs.add("❌ $pid is marked as TEST_ONLY but active environment is PRODUCTION. Skip.")
                continue
            }

            // Rule 2: Consent Eligibility
            if (pid == "ADMOB" && consentState == AdConsentState.DENIED) {
                logs.add("❌ ADMOB requires user consent. Consent is DENIED. Skip.")
                continue
            }

            // Rule 3: Format Compatibility
            val caps = adapter.getCapabilities()
            val formatCompatible = when (format) {
                AdFormat.NATIVE -> caps.contains(AdCapability.FEED_RENDERING)
                AdFormat.REWARDED -> caps.contains(AdCapability.OPT_IN_REWARD_FLOW) || caps.contains(AdCapability.VALIDATED_REWARD_CALLBACK)
                else -> false
            }
            if (!formatCompatible) {
                logs.add("❌ $pid does not support requested format $format. Skip.")
                continue
            }

            // Rule 4: Provider Circuit Breaker Status
            val breaker = ProviderCircuitBreakerRegistry.getCircuitBreaker(pid)
            if (breaker.state == CircuitState.OPEN) {
                logs.add("❌ $pid Circuit Breaker is OPEN (tripped due to errors). Skip.")
                continue
            }

            // Rule 5: Technical Availability / Policy Status
            val reviewStatus = adapter.getReviewStatus()
            if (reviewStatus == PolicyReviewStatus.BLOCKED || reviewStatus == PolicyReviewStatus.EXPIRED) {
                logs.add("❌ $pid policy review is $reviewStatus. Skip.")
                continue
            }

            logs.add("✅ Selected compliant provider: $pid ($availability)")
            return adapter
        }

        logs.add("⚠️ No compliant provider eligible. Gracefully defaulting to Dummy Adapter.")
        return AdvertisementProviderRegistry.getAdapter("DUMMY")
    }
}

// ============================================================================
// 5. Provider Health Monitor & Circuit Breakers (Chapter 9)
// ============================================================================

enum class CircuitState {
    CLOSED,  // Normal functioning
    OPEN,    // Tripped (redirects traffic away)
    HALF_OPEN // Testing recovery
}

class ProviderCircuitBreaker(val providerId: String) {
    var state: CircuitState = CircuitState.CLOSED
    var consecutiveFailures: Int = 0
    val failureThreshold = 3

    fun recordSuccess() {
        consecutiveFailures = 0
        state = CircuitState.CLOSED
    }

    fun recordFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= failureThreshold) {
            state = CircuitState.OPEN
            Log.e("CircuitBreaker", "Circuit Breaker for $providerId has TRIPPED to OPEN state!")
        }
    }

    fun reset() {
        consecutiveFailures = 0
        state = CircuitState.CLOSED
    }
}

object ProviderCircuitBreakerRegistry {
    private val breakers = mutableMapOf<String, ProviderCircuitBreaker>()

    fun getCircuitBreaker(providerId: String): ProviderCircuitBreaker {
        return breakers.getOrPut(providerId) { ProviderCircuitBreaker(providerId) }
    }
}

object ProviderHealthMonitor {
    private val successes = mutableMapOf<String, Int>()
    private val failures = mutableMapOf<String, Int>()
    private val latencies = mutableMapOf<String, Long>()

    fun recordLoadSuccess(providerId: String, latencyMs: Long) {
        successes[providerId] = (successes[providerId] ?: 0) + 1
        latencies[providerId] = latencyMs
        ProviderCircuitBreakerRegistry.getCircuitBreaker(providerId).recordSuccess()
    }

    fun recordLoadFailure(providerId: String) {
        failures[providerId] = (failures[providerId] ?: 0) + 1
        ProviderCircuitBreakerRegistry.getCircuitBreaker(providerId).recordFailure()
    }

    fun getSuccesses(providerId: String): Int = successes[providerId] ?: 0
    fun getFailures(providerId: String): Int = failures[providerId] ?: 0
    fun getLatency(providerId: String): Long = latencies[providerId] ?: 0L

    fun resetStats(providerId: String) {
        successes[providerId] = 0
        failures[providerId] = 0
        latencies[providerId] = 0L
        ProviderCircuitBreakerRegistry.getCircuitBreaker(providerId).reset()
    }
}

// ============================================================================
// 6. Remote Configuration & Governance (Chapter 9)
// ============================================================================

object RemoteAdvertisementConfiguration {
    var configId: String = "cfg_2026_v9"
    var version: Int = 9
    var lastReviewedDate: String = "Jan 15, 2026"
    var defaultProvider: String = "ADMOB"
    var directCampaignsEnabled: Boolean = true

    fun resetToSafeLocalDefaults() {
        configId = "cfg_local_safe"
        version = 1
        lastReviewedDate = "Local Base"
        defaultProvider = "ADMOB"
        directCampaignsEnabled = false
    }
}

// ============================================================================
// 7. Server-Side Wallet Ledger & Redemption Security (Chapter 9)
// ============================================================================

data class WalletTransaction(
    val transactionId: String = "tx_" + UUID.randomUUID().toString().take(8),
    val type: String, // CREDIT or DEBIT
    val amount: Int,
    val source: String,
    val policyNamespace: RewardPolicyNamespace,
    val isServerValidated: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

object ServerSideWalletLedger {
    private val transactions = MutableStateFlow<List<WalletTransaction>>(
        listOf(
            WalletTransaction(transactionId = "tx_9011ab", type = "CREDIT", amount = 15, source = "AdMob Video Reward", policyNamespace = RewardPolicyNamespace.ADMOB_REWARDED_POLICY, isServerValidated = true),
            WalletTransaction(transactionId = "tx_9022cd", type = "CREDIT", amount = 30, source = "Premium Direct Campaign", policyNamespace = RewardPolicyNamespace.DIRECT_CAMPAIGN_POLICY, isServerValidated = true),
            WalletTransaction(transactionId = "tx_9033ef", type = "DEBIT", amount = 20, source = "UPI Cashout (Simulation)", policyNamespace = RewardPolicyNamespace.FUTURE_PROVIDER_POLICY, isServerValidated = true)
        )
    )
    val ledgerTransactions: StateFlow<List<WalletTransaction>> = transactions.asStateFlow()

    fun addTransaction(type: String, amount: Int, source: String, policyNamespace: RewardPolicyNamespace, isServerValidated: Boolean): WalletTransaction {
        val tx = WalletTransaction(
            type = type,
            amount = amount,
            source = source,
            policyNamespace = policyNamespace,
            isServerValidated = isServerValidated
        )
        val current = transactions.value.toMutableList()
        current.add(0, tx) // Insert at top
        transactions.value = current
        return tx
    }

    fun getLocalLedgerBalance(): Int {
        var balance = 0
        for (tx in transactions.value) {
            if (tx.type == "CREDIT") {
                balance += tx.amount
            } else {
                balance -= tx.amount
            }
        }
        return balance
    }

    fun verifyLedgerIntegrity(): Boolean {
        // Simulates server-side checksum and transaction order validation
        for (tx in transactions.value) {
            if (!tx.isServerValidated) return false
        }
        return true
    }
}
