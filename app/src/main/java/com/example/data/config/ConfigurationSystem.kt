package com.example.data.config

import androidx.compose.ui.graphics.Color

/**
 * -----------------------------------------------------------------
 * ENTERPRISE CONFIGURATION SYSTEM
 * -----------------------------------------------------------------
 * Purpose: Centrally configures all system constants, parameters, 
 *          thresholds, themes, and animations.
 * Responsibilities:
 *   - Eliminate magic numbers.
 *   - Support dynamic modification without rebuilding.
 *   - Support safe, fail-safe fallback options.
 * Dependencies: Independent from other modules.
 * Future Extension: Connect to a remote configuration engine (e.g., Firebase Remote Config)
 *                   or a localized SharedPreferences cache for on-the-fly updates.
 */

/**
 * Global App Configuration.
 * Coordinates system-wide mode controls.
 */
object AppConfig {
    const val APP_VERSION = "1.0.0"
    const val ENVIRONMENT = "PRODUCTION" // "DEVELOPMENT" or "PRODUCTION"
    const val IS_LOGGING_ENABLED = true
    const val BACKEND_URL_FALLBACK = "https://api.aistudio.com/v1"
}

/**
 * Reward Engine Configuration.
 * Configures all coin issuance parameters, watch thresholds, and milestones.
 */
object RewardConfig {
    /** Minimum watch time in seconds to qualify for a base video reward. */
    const val REELS_QUALIFIED_WATCH_SECONDS = 5
    
    /** Base coin payout issued upon satisfying the 5-second watch threshold. */
    const val REELS_BASE_REWARD_COINS = 15

    /** Extended watch duration threshold in seconds for high-tier payouts. */
    const val REELS_LONGER_WATCH_SECONDS = 10
    
    /** Premium coin payout issued upon satisfying the 10-second extended watch threshold. */
    const val REELS_LONGER_REWARD_COINS = 40

    /** Number of feed ad impressions required before awarding a milestone. */
    const val FEED_IMPRESSIONS_REQUIRED = 5
    
    /** Coin payout issued when the feed ad impressions milestone is satisfied. */
    const val FEED_BASE_REWARD_COINS = 15

    /** Initial coin reward given to new registered users. */
    const val INITIAL_SIGNON_BONUS_COINS = 5000
    
    /** Extra milestone reward coins for consistent user activity. */
    const val MILESTONE_REWARD_COINS = 50
}

/**
 * Advertisement Queue & Preloader Configuration.
 * Manages buffer sizes, retry strategies, and fetch policies.
 */
object AdvertisementConfig {
    /** The maximum buffer size for active feed advertisements. */
    const val DEFAULT_FEED_BUFFER_SIZE = 3
    
    /** Preload distance/buffer size for Reels video ads. */
    const val DEFAULT_REEL_PRELOAD_DISTANCE = 1

    /** Time-to-live duration in milliseconds for advertisement cache. */
    const val CACHE_EXPIRY_MS = 1_800_000L // 30 minutes

    /** Network connection timeout in milliseconds. */
    const val REQUEST_TIMEOUT_MS = 10_000L

    /** Maximum attempts allowed when an ad fails to load before skipping. */
    const val MAX_RETRIES = 3

    /** Backoff delay time in milliseconds between successive retries. */
    const val RETRY_DELAY_MS = 2_000L
}

/**
 * Wallet Configuration.
 * Manages currency symbols, conversion metrics, and payout limits.
 */
object WalletConfig {
    /** Exchange rate conversion multiplier from coins to USD. */
    const val COINS_TO_USD_RATIO = 0.01 // 1 Coin = 0.01 USD

    /** Exchange rate conversion multiplier from coins to INR. */
    const val COIN_TO_RUPEE_RATIO = 10.0 // 10 Coins = 1 INR

    /** The default active functional currency symbol. */
    const val DEFAULT_CURRENCY = "INR"

    /** Minimum coin count required to request a cashout. */
    const val MINIMUM_CASHOUT_COINS = 1000

    /** Determines whether transactions should compute cryptographic SHA-256 integrity hashes. */
    const val IS_CRYPTO_INTEGRITY_CHECK_ENABLED = true
}

/**
 * Performance & Memory Management Configuration.
 * Fine-tunes hardware acceleration, cache limits, and background threading parameters.
 */
object PerformanceConfig {
    /** Forces low-memory optimizations on lower-end devices. */
    const val IS_LOW_MEMORY_MODE_ENABLED = false

    /** Maximum allowed memory allocation in bytes for caching image assets. */
    const val MAX_IMAGE_MEMORY_CACHE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB

    /** Maximum disk capacity allocation in bytes for video caching. */
    const val MAX_VIDEO_DISK_CACHE_SIZE_BYTES = 50L * 1024 * 1024 // 50MB

    /** Prefetch offset count triggered before reaching the bottom of list screens. */
    const val PREFETCH_THRESHOLD_ITEMS = 1

    /** Enables verbose telemetry and diagnostics collection in development. */
    const val IS_TRACKING_DIAGNOSTICS_ENABLED = true
}

/**
 * Animation Configuration.
 * Regulates UI transitions, micro-interactions, and gold coin ripples.
 */
object AnimationConfig {
    /** Standard duration in milliseconds for common transition animations. */
    const val DEFAULT_TRANSITION_SPEED_MS = 300

    /** Duration in milliseconds for the floating coin collection animation. */
    const val COIN_ANIMATION_DURATION_MS = 800

    /** Enables hardware acceleration rendering on standard screens. */
    const val IS_HARDWARE_ACCELERATED = true
}

/**
 * Theme & Palette Configuration.
 * Configures major branding color palettes and typography metrics.
 */
object ThemeConfig {
    val PRIMARY_ACCENT_COLOR = Color(0xFFFFD700) // Premium Gold Accent
    val BACKGROUND_CARD_DARK = Color(0xFF1A1A1E) // Slate Dark
    val SURFACE_BORDER_LIGHT = Color(0x13FFFFFF) // Subtle transparent border
    
    const val DISPLAY_FONT_FAMILY_NAME = "Space Grotesk"
    const val MONOSPACED_FONT_FAMILY_NAME = "JetBrains Mono"
}
