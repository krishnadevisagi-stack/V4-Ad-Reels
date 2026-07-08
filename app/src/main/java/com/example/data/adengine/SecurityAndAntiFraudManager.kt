package com.example.data.adengine

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

object SecurityAndAntiFraudManager {
    private const val PREFS_NAME = "antifraud_prefs"
    private const val KEY_BLOCKED_UNTIL = "blocked_until"
    private const val KEY_SUSPICIOUS_COUNT = "suspicious_count"
    private const val KEY_BLOCK_DURATION_MINUTES = "block_duration_minutes"

    private lateinit var prefs: SharedPreferences
    private var isInitialized = false

    // Cache of watch times (adId -> elapsed millisecond progress)
    private val adWatchTimes = ConcurrentHashMap<String, Long>()

    // For click rate-limiting (preventing rapid double/triple clicks on any ad)
    private var lastClickTime: Long = 0
    private var recentClickCount = 0

    private val _isAdsBlocked = MutableStateFlow(false)
    val isAdsBlocked: StateFlow<Boolean> = _isAdsBlocked.asStateFlow()

    private val _suspiciousWarningMsg = MutableStateFlow<String?>(null)
    val suspiciousWarningMsg: StateFlow<String?> = _suspiciousWarningMsg.asStateFlow()

    private val _fraudBlockMsg = MutableStateFlow<String?>(null)
    val fraudBlockMsg: StateFlow<String?> = _fraudBlockMsg.asStateFlow()

    // VPN / Proxy / AdBlock state
    private val _areRewardsSuspended = MutableStateFlow(false)
    val areRewardsSuspended: StateFlow<Boolean> = _areRewardsSuspended.asStateFlow()

    private val _rewardsSuspensionDetails = MutableStateFlow<String?>(null)
    val rewardsSuspensionDetails: StateFlow<String?> = _rewardsSuspensionDetails.asStateFlow()

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isInitialized = true
        checkBlockStatus()
    }

    fun checkBlockStatus() {
        if (!isInitialized) return
        val blockedUntil = prefs.getLong(KEY_BLOCKED_UNTIL, 0L)
        val isBlocked = System.currentTimeMillis() < blockedUntil
        _isAdsBlocked.value = isBlocked
        if (isBlocked) {
            val remainingMs = blockedUntil - System.currentTimeMillis()
            val minutes = (remainingMs / 60000) + 1
            _fraudBlockMsg.value = "Your account has been temporarily restricted due to suspicious click activity. You will not see ads or earn rewards for the next $minutes minute(s)."
        } else {
            _fraudBlockMsg.value = null
        }
    }

    /**
     * Set customizable ban/block duration in minutes from administrative settings (Disabled - logic is automatic)
     */
    fun setBlockDurationMinutes(minutes: Int) {
        // Disabled: Anti-fraud blocking duration is handled automatically inside backend logic to prevent tampering
        Log.d("AntiFraudManager", "setBlockDurationMinutes is disabled. Security blocking is fully automatic.")
    }

    fun getBlockDurationMinutes(): Int {
        if (!isInitialized) return 15 // Default 15 minutes for demonstration (or can be 1440 for 1 day)
        return prefs.getInt(KEY_BLOCK_DURATION_MINUTES, 15)
    }

    /**
     * Records watch progress of an ad (Feed or Reel)
     */
    fun recordWatchProgress(adId: String, elapsedMs: Long) {
        adWatchTimes[adId] = elapsedMs
    }

    fun getWatchProgress(adId: String): Long {
        return adWatchTimes[adId] ?: 0L
    }

    /**
     * Clear warning/suspicious messages so they don't keep popping up
     */
    fun clearWarning() {
        _suspiciousWarningMsg.value = null
    }

    fun clearBlockMessage() {
        _fraudBlockMsg.value = null
    }

    /**
     * Resets the entire anti-fraud status (for administrative control)
     */
    fun resetAntiFraudStatus() {
        if (!isInitialized) return
        prefs.edit()
            .putLong(KEY_BLOCKED_UNTIL, 0L)
            .putInt(KEY_SUSPICIOUS_COUNT, 0)
            .apply()
        _isAdsBlocked.value = false
        _fraudBlockMsg.value = null
        _suspiciousWarningMsg.value = null
        recentClickCount = 0
        adWatchTimes.clear()
    }

    /**
     * Updates VPN/Proxy/AdBlock rewards suspension state based on active threats
     */
    fun updateSecurityStatus(threats: SecurityThreats) {
        val suspended = threats.hasVpn || threats.hasProxy || threats.detectedAdBlockers.isNotEmpty() || threats.dnsAdBlockActive
        _areRewardsSuspended.value = suspended
        if (suspended) {
            val details = buildString {
                if (threats.hasVpn) append("Active VPN connection. ")
                if (threats.hasProxy) append("Network proxy server. ")
                if (threats.detectedAdBlockers.isNotEmpty()) append("Ad-blocking tools. ")
                if (threats.dnsAdBlockActive) append("Local DNS ad-blocker / Pi-hole. ")
            }.trim()
            _rewardsSuspensionDetails.value = "Rewards are temporarily suspended on this device due to: $details\n\nAds will still display, but no reward coins will be generated or credited."
        } else {
            _rewardsSuspensionDetails.value = null
        }
    }

    /**
     * Registers a click on an ad. Evaluates if the click is suspicious or spammy.
     * Returns true if the user is allowed to proceed, false if blocked.
     */
    fun registerAndEvaluateClick(adId: String, adBrandName: String): Boolean {
        if (!isInitialized) return true

        // 1. If already blocked, reject
        checkBlockStatus()
        if (_isAdsBlocked.value) {
            return false
        }

        val now = System.currentTimeMillis()

        // 2. Check for continuous rapid click spam (clicking again within 3 seconds)
        val timeSinceLastClick = now - lastClickTime
        lastClickTime = now

        if (timeSinceLastClick < 3000L) {
            recentClickCount++
            Log.w("AntiFraudManager", "Rapid clicking detected! Recent click count: $recentClickCount")
        } else {
            recentClickCount = 1
        }

        // 3. Retrieve watch progress before click
        val watchProgressMs = getWatchProgress(adId)
        val requiredWatchTimeMs = 3000L // 3 seconds minimum watch time to qualify as genuine

        var isSuspicious = false
        var reason = ""

        if (recentClickCount >= 3) {
            isSuspicious = true
            reason = "Multiple rapid clicks detected without delay"
        } else if (watchProgressMs < requiredWatchTimeMs) {
            isSuspicious = true
            reason = "Instant click without sufficient view progress ($watchProgressMs ms watched, minimum 3s required)"
        }

        if (isSuspicious) {
            val currentSuspiciousCount = prefs.getInt(KEY_SUSPICIOUS_COUNT, 0) + 1
            prefs.edit().putInt(KEY_SUSPICIOUS_COUNT, currentSuspiciousCount).apply()

            Log.w("AntiFraudManager", "Suspicious Activity: $reason. Suspicious Count: $currentSuspiciousCount")

            if (currentSuspiciousCount >= 3) {
                // Impose account ban/limitation. Detected automatically based on suspicion severity:
                // 1st block: 15 minutes
                // 2nd block: 120 minutes (2 hours)
                // 3rd+ block: 1440 minutes (24 hours / 1 day)
                val durationMinutes = when {
                    currentSuspiciousCount == 3 -> 15
                    currentSuspiciousCount == 4 -> 120
                    else -> 1440
                }
                val blockUntil = now + (durationMinutes * 60000L)
                prefs.edit().putLong(KEY_BLOCKED_UNTIL, blockUntil).apply()
                _isAdsBlocked.value = true
                
                val durationText = if (durationMinutes >= 1440) "24 hours" else "$durationMinutes minutes"
                _fraudBlockMsg.value = "Your account is temporarily restricted due to suspicious click activity ($reason). You will not see ads or earn rewards for the next $durationText."
                return false
            } else {
                // Warn user about suspicious activity
                _suspiciousWarningMsg.value = "Warning: Suspicious click activity detected ($reason). Please watch ads sufficiently before clicking. Repeated violations will result in temporary suspension."
            }
        }

        return true
    }
}
