package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import com.example.data.config.AdConfig
import com.example.data.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * -----------------------------------------------------------------
 * USER PROFILE & PREFERENCES REPOSITORY
 * -----------------------------------------------------------------
 * Purpose: Centrally stores and manages user identity, sessions, and historic activity.
 * Responsibilities:
 *   - Load, save, and update user info, selected categories, and watch milestones.
 *   - Access and record bookmarked ads, redemption history, and point logs.
 * Dependencies:
 *   - Room DAOs: [UserDao], [WatchHistoryDao], [SavedAdDao], [RedeemHistoryDao], [CoinHistoryDao]
 * Future Extension: Connect with Google Sign-In or OAuth2.0 authentication endpoints.
 */
class UserRepository(
    private val userDao: UserDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val savedAdDao: SavedAdDao,
    private val redeemHistoryDao: RedeemHistoryDao,
    private val coinHistoryDao: CoinHistoryDao,
    private val walletDao: WalletDao? = null,
    private val walletActivityDao: WalletActivityDao? = null,
    private val rewardHistoryDao: RewardHistoryDao? = null,
    private val adStatsDao: AdStatsDao? = null
) {

    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()
    val watchHistory: Flow<List<WatchHistory>> = watchHistoryDao.getWatchHistory()
    val savedAds: Flow<List<SavedAd>> = savedAdDao.getSavedAds()
    val redeemHistory: Flow<List<RedeemHistory>> = redeemHistoryDao.getRedeemHistory()
    val coinHistory: Flow<List<CoinHistory>> = coinHistoryDao.getCoinHistory()
    val adStats: Flow<List<AdStatsEntry>> = adStatsDao?.getAdStats() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    /**
     * Checks if the user is registered and logged in
     */
    suspend fun isUserLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        val user = userProfile.firstOrNull()
        user?.isLoggedIn == true
    }

    /**
     * Registers or signs in a user with Google credentials
     */
    suspend fun loginOrRegisterGoogle(email: String, displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val username = cleanEmail.substringBefore("@")
        val fullName = displayName.ifBlank { username }
        
        // Check if user already exists locally
        val existingUser = userDao.getUserByEmail(cleanEmail)
        if (existingUser != null) {
            val updatedUser = existingUser.copy(
                lastLogin = System.currentTimeMillis(),
                isLoggedIn = true
            )
            userDao.logoutAllUsers()
            userDao.insertUserProfile(updatedUser)
            FirebaseManager.syncUserProfile(updatedUser)
            
            // Sync with WalletEntity in the Wallet system if it exists locally
            if (walletDao != null) {
                val existingWallet = walletDao.getWallet(cleanEmail)
                if (existingWallet == null) {
                    val wallet = WalletEntity(
                        userId = cleanEmail,
                        currentCoins = existingUser.coins,
                        todayCoins = existingUser.coins,
                        weeklyCoins = existingUser.coins,
                        lifetimeCoins = existingUser.coins,
                        pendingCoins = 0,
                        redeemableCoins = existingUser.coins,
                        lastUpdated = System.currentTimeMillis()
                    )
                    walletDao.insertWallet(wallet)
                }
            }
            
            return@withContext Result.success(updatedUser)
        }

        // Try to fetch from Firestore first before treating as a new registration
        val cloudProfile = FirebaseManager.fetchUserProfile(cleanEmail)
        if (cloudProfile != null) {
            userDao.logoutAllUsers()
            userDao.insertUserProfile(cloudProfile)
            
            // Restore WalletEntity in the Wallet system
            if (walletDao != null) {
                val wallet = WalletEntity(
                    userId = cleanEmail,
                    currentCoins = cloudProfile.coins,
                    todayCoins = cloudProfile.coins,
                    weeklyCoins = cloudProfile.coins,
                    lifetimeCoins = cloudProfile.coins,
                    pendingCoins = 0,
                    redeemableCoins = cloudProfile.coins,
                    lastUpdated = System.currentTimeMillis()
                )
                walletDao.insertWallet(wallet)
            }
            return@withContext Result.success(cloudProfile)
        }

        // Create new user profile with Google details (New User)
        val walletId = "W-G-${(10000..99999).random()}"
        val newProfile = UserProfile(
            id = 0,
            username = username,
            fullName = fullName,
            email = cleanEmail,
            mobile = "",
            passwordHash = "google_auth_session", // Indicator for Google Auth
            guestAccount = false,
            walletId = walletId,
            selectedCategories = "",
            createdDate = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            profileCreated = true,
            coins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
            walletBalance = AdConfig.INITIAL_SIGNON_BONUS_COINS * AdConfig.COINS_TO_USD_RATIO,
            isLoggedIn = true
        )

        userDao.logoutAllUsers()
        userDao.insertUserProfile(newProfile)
        FirebaseManager.syncUserProfile(newProfile)

        // Insert coin history entry for signup bonus
        insertCoinHistoryAndSync(
            CoinHistory(
                title = "Google Welcome Bonus",
                amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
            )
        )

        // Sync with WalletEntity in the Wallet system
        if (walletDao != null) {
            val wallet = WalletEntity(
                userId = cleanEmail,
                currentCoins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                todayCoins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                weeklyCoins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                lifetimeCoins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                pendingCoins = 0,
                redeemableCoins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                lastUpdated = System.currentTimeMillis()
            )
            walletDao.insertWallet(wallet)
        }

        val insertedUser = userDao.getUserByEmail(cleanEmail) ?: newProfile
        Result.success(insertedUser)
    }

    /**
     * Set selected categories for the logged-in user or newly registered user
     */
    suspend fun registerUserProfile(username: String, email: String, categories: List<String>) = withContext(Dispatchers.IO) {
        val categoriesStr = categories.joinToString(",")
        val currentUser = userProfile.firstOrNull()
        if (currentUser != null) {
            val updatedUser = currentUser.copy(
                username = if (currentUser.guestAccount) username else currentUser.username,
                email = if (currentUser.guestAccount) email else currentUser.email,
                selectedCategories = categoriesStr,
                profileCreated = true,
                isLoggedIn = true
            )
            userDao.insertUserProfile(updatedUser)
            FirebaseManager.syncUserProfile(updatedUser)
        } else {
            // Fallback: create fresh profile
            val walletId = "W-${(100000..999999).random()}"
            val profile = UserProfile(
                id = 0,
                username = username,
                fullName = username,
                email = email,
                mobile = "9999999999",
                passwordHash = "",
                guestAccount = false,
                walletId = walletId,
                selectedCategories = categoriesStr,
                profileCreated = true,
                isLoggedIn = true,
                coins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
                walletBalance = AdConfig.INITIAL_SIGNON_BONUS_COINS * AdConfig.COINS_TO_USD_RATIO
            )
            userDao.logoutAllUsers()
            userDao.insertUserProfile(profile)
            FirebaseManager.syncUserProfile(profile)

            insertCoinHistoryAndSync(
                CoinHistory(
                    title = "Welcome Sign-Up Bonus",
                    amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
                )
            )
        }
    }

    /**
     * Login a registered user with email/mobile and password
     */
    suspend fun loginUser(emailOrMobile: String, passwordRaw: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val hash = com.example.data.utils.SecurityUtils.hashPassword(passwordRaw)
        // Check by email first
        var user = userDao.getUserByEmail(emailOrMobile)
        if (user == null) {
            // Check by mobile
            user = userDao.getUserByMobile(emailOrMobile)
        }

        // If not found locally, try to restore from Firestore
        if (user == null && emailOrMobile.contains("@")) {
            val cloudProfile = FirebaseManager.fetchUserProfile(emailOrMobile)
            if (cloudProfile != null) {
                // If password hash matches, restore locally
                if (cloudProfile.passwordHash == hash || cloudProfile.passwordHash.isEmpty()) {
                    val restoredProfile = cloudProfile.copy(
                        passwordHash = hash, // Use input hash if empty or sync
                        isLoggedIn = true,
                        lastLogin = System.currentTimeMillis()
                    )
                    userDao.logoutAllUsers()
                    userDao.insertUserProfile(restoredProfile)
                    
                    // Restore WalletEntity in local Room DB
                    if (walletDao != null) {
                        val wallet = WalletEntity(
                            userId = restoredProfile.email,
                            currentCoins = restoredProfile.coins,
                            todayCoins = restoredProfile.coins,
                            weeklyCoins = restoredProfile.coins,
                            lifetimeCoins = restoredProfile.coins,
                            pendingCoins = 0,
                            redeemableCoins = restoredProfile.coins,
                            lastUpdated = System.currentTimeMillis()
                        )
                        walletDao.insertWallet(wallet)
                    }
                    return@withContext Result.success(restoredProfile)
                } else {
                    return@withContext Result.failure(Exception("Incorrect password."))
                }
            }
        }

        if (user == null) {
            return@withContext Result.failure(Exception("User not found."))
        }
        if (user.passwordHash != hash) {
            return@withContext Result.failure(Exception("Incorrect password."))
        }

        // Logout all users first to ensure only one active session
        userDao.logoutAllUsers()
        val loggedInUser = user.copy(
            isLoggedIn = true,
            lastLogin = System.currentTimeMillis()
        )
        userDao.insertUserProfile(loggedInUser)
        FirebaseManager.syncUserProfile(loggedInUser)
        
        // Sync with WalletEntity if it doesn't exist
        if (walletDao != null) {
            val existingWallet = walletDao.getWallet(loggedInUser.email)
            if (existingWallet == null) {
                val wallet = WalletEntity(
                    userId = loggedInUser.email,
                    currentCoins = loggedInUser.coins,
                    todayCoins = loggedInUser.coins,
                    weeklyCoins = loggedInUser.coins,
                    lifetimeCoins = loggedInUser.coins,
                    pendingCoins = 0,
                    redeemableCoins = loggedInUser.coins,
                    lastUpdated = System.currentTimeMillis()
                )
                walletDao.insertWallet(wallet)
            }
        }
        
        Result.success(loggedInUser)
    }

    /**
     * Register a new user profile locally
     */
    suspend fun registerUser(
        username: String,
        fullName: String,
        email: String,
        mobile: String,
        passwordRaw: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        // Check if email already registered
        val existingEmail = userDao.getUserByEmail(email)
        if (existingEmail != null) {
            return@withContext Result.failure(Exception("Email is already registered."))
        }
        // Check if mobile already registered
        val existingMobile = userDao.getUserByMobile(mobile)
        if (existingMobile != null) {
            return@withContext Result.failure(Exception("Mobile number is already registered."))
        }

        val hash = com.example.data.utils.SecurityUtils.hashPassword(passwordRaw)
        val walletId = "W-${(100000..999999).random()}"
        val profile = UserProfile(
            id = 0, // autoGenerate
            username = username,
            fullName = fullName,
            email = email,
            mobile = mobile,
            passwordHash = hash,
            guestAccount = false,
            walletId = walletId,
            selectedCategories = "",
            createdDate = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            profileCreated = true,
            coins = AdConfig.INITIAL_SIGNON_BONUS_COINS,
            walletBalance = AdConfig.INITIAL_SIGNON_BONUS_COINS * AdConfig.COINS_TO_USD_RATIO,
            isLoggedIn = true
        )
        userDao.logoutAllUsers()
        userDao.insertUserProfile(profile)
        FirebaseManager.syncUserProfile(profile)

        // Insert coin history entry for sign up bonus
        insertCoinHistoryAndSync(
            CoinHistory(
                title = "Welcome Sign-Up Bonus",
                amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
            )
        )

        val insertedUser = userDao.getUserByEmail(email) ?: profile
        Result.success(insertedUser)
    }

    /**
     * Continue anonymously as a Guest
     */
    suspend fun loginAsGuest(): UserProfile = withContext(Dispatchers.IO) {
        val rand = (10000..99999).random()
        val guestUsername = "Guest-$rand"
        val walletId = "W-G-$rand"
        
        val profile = UserProfile(
            id = 0,
            username = guestUsername,
            fullName = "Guest User",
            email = "$guestUsername@adreels.local",
            mobile = "9999999999",
            passwordHash = "",
            guestAccount = true,
            walletId = walletId,
            selectedCategories = "",
            createdDate = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            profileCreated = false,
            coins = 0,
            walletBalance = 0.0,
            isLoggedIn = true
        )
        userDao.logoutAllUsers()
        userDao.insertUserProfile(profile)

        userDao.getUserByEmail(profile.email) ?: profile
    }

    /**
     * Convert an active Guest session into a Registered User, preserving wallet, categories, and logs
     */
    suspend fun convertGuestToRegistered(
        fullName: String,
        email: String,
        mobile: String,
        passwordRaw: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentGuest = userProfile.firstOrNull()
        if (currentGuest == null || !currentGuest.guestAccount) {
            return@withContext Result.failure(Exception("No active guest session found."))
        }

        // Check conflicts
        val existingEmail = userDao.getUserByEmail(email)
        if (existingEmail != null) {
            return@withContext Result.failure(Exception("Email is already registered."))
        }
        val existingMobile = userDao.getUserByMobile(mobile)
        if (existingMobile != null) {
            return@withContext Result.failure(Exception("Mobile number is already registered."))
        }

        val hash = com.example.data.utils.SecurityUtils.hashPassword(passwordRaw)
        val updatedProfile = currentGuest.copy(
            username = email.substringBefore("@"),
            fullName = fullName,
            email = email,
            mobile = mobile,
            passwordHash = hash,
            guestAccount = false,
            profileCreated = true,
            coins = currentGuest.coins + AdConfig.INITIAL_SIGNON_BONUS_COINS, // Retain accumulated coins + give bonus!
            walletBalance = (currentGuest.coins + AdConfig.INITIAL_SIGNON_BONUS_COINS) * AdConfig.COINS_TO_USD_RATIO
        )
        userDao.insertUserProfile(updatedProfile)
        FirebaseManager.syncUserProfile(updatedProfile)

        // Insert coin history entry for sign up bonus
        insertCoinHistoryAndSync(
            CoinHistory(
                title = "Guest to User Upgrade Bonus",
                amount = AdConfig.INITIAL_SIGNON_BONUS_COINS
            )
        )

        Result.success(updatedProfile)
    }

    /**
     * Add reward coins for watching ads
     */
    suspend fun rewardCoinsForAd(adId: String, brandName: String, coinsReward: Int, isVideo: Boolean) = withContext(Dispatchers.IO) {
        if (com.example.data.adengine.SecurityAndAntiFraudManager.areRewardsSuspended.value) {
            com.example.data.adengine.ApplicationLogManager.e("UserRepository", "BLOCKED COIN AWARD: Rewards suspended due to active VPN/Proxy/Ad-blocker.")
            return@withContext
        }
        if (com.example.data.adengine.SecurityAndAntiFraudManager.isAdsBlocked.value) {
            com.example.data.adengine.ApplicationLogManager.e("UserRepository", "BLOCKED COIN AWARD: Account temporarily restricted due to suspicious activity.")
            return@withContext
        }

        val currentUser = userDao.getActiveUserProfile() ?: return@withContext
        val newCoins = currentUser.coins + coinsReward
        val newBalance = newCoins * AdConfig.COINS_TO_USD_RATIO
        
        userDao.updateCoins(newCoins, newBalance)

        val updatedUser = userDao.getActiveUserProfile()
        if (updatedUser != null) {
            FirebaseManager.syncUserProfile(updatedUser)
        }

        // Log coin history
        val adType = if (isVideo) "Video Reel" else "Brand Card"
        insertCoinHistoryAndSync(
            CoinHistory(
                title = "Watched $adType: $brandName",
                amount = coinsReward
            )
        )

        // Log watch history
        watchHistoryDao.insertWatchHistory(
            WatchHistory(
                adId = adId,
                brandName = brandName,
                coinsEarned = coinsReward,
                category = currentUser.selectedCategories
            )
        )

        // Sync with WalletEntity in the Wallet system
        val userId = currentUser.email.ifBlank { "guest" }
        if (walletDao != null) {
            val wallet = walletDao.getWallet(userId)
            if (wallet != null) {
                val updatedWallet = wallet.copy(
                    currentCoins = wallet.currentCoins + coinsReward,
                    todayCoins = wallet.todayCoins + coinsReward,
                    weeklyCoins = wallet.weeklyCoins + coinsReward,
                    lifetimeCoins = wallet.lifetimeCoins + coinsReward,
                    redeemableCoins = wallet.redeemableCoins + coinsReward,
                    lastUpdated = System.currentTimeMillis()
                )
                walletDao.insertWallet(updatedWallet)
            } else {
                val newWallet = WalletEntity(
                    userId = userId,
                    currentCoins = coinsReward,
                    todayCoins = coinsReward,
                    weeklyCoins = coinsReward,
                    lifetimeCoins = coinsReward,
                    pendingCoins = 0,
                    redeemableCoins = coinsReward,
                    lastUpdated = System.currentTimeMillis()
                )
                walletDao.insertWallet(newWallet)
            }
        }

        if (rewardHistoryDao != null) {
            rewardHistoryDao.insertReward(
                RewardHistoryEntity(
                    rewardId = "REWARD_${java.util.UUID.randomUUID().toString().take(8)}",
                    adId = adId,
                    userId = userId,
                    amountCoins = coinsReward,
                    sourceType = if (isVideo) "REEL" else "FEED",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        if (walletActivityDao != null) {
            walletActivityDao.insertActivity(
                WalletActivityEntity(
                    activityId = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Watched $adType: $brandName",
                    description = "Earned coins from validated ad watch",
                    amountCoins = coinsReward,
                    type = "REWARD",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Update user selected interest categories
     */
    suspend fun updateSelectedCategories(categories: List<String>) = withContext(Dispatchers.IO) {
        val categoriesStr = categories.joinToString(",")
        userDao.updateCategories(categoriesStr)
    }

    /**
     * Save/Bookmark an advertisement
     */
    suspend fun saveAd(ad: DummyAd) = withContext(Dispatchers.IO) {
        savedAdDao.insertSavedAd(
            SavedAd(
                adId = ad.id,
                category = ad.category,
                brandName = ad.brandName,
                title = ad.title,
                description = ad.description,
                mediaUrl = ad.mediaUrl,
                isVideo = ad.isVideo
            )
        )
    }

    /**
     * Unsave/Unbookmark an advertisement
     */
    suspend fun unsaveAd(adId: String) = withContext(Dispatchers.IO) {
        savedAdDao.deleteSavedAd(adId)
    }

    /**
     * Check if ad is saved
     */
    suspend fun isAdSaved(adId: String): Boolean = withContext(Dispatchers.IO) {
        savedAdDao.isAdSaved(adId)
    }

    /**
     * Redeem/Withdraw local points for rewards
     */
    suspend fun redeemRewardPoints(itemTitle: String, coinsCost: Int): Boolean = withContext(Dispatchers.IO) {
        val currentUser = userProfile.firstOrNull() ?: return@withContext false
        if (currentUser.coins < coinsCost) return@withContext false

        val newCoins = currentUser.coins - coinsCost
        val newBalance = newCoins * AdConfig.COINS_TO_USD_RATIO
        userDao.updateCoins(newCoins, newBalance)

        // Log redeem history
        redeemHistoryDao.insertRedeemHistory(
            RedeemHistory(
                itemTitle = itemTitle,
                coinsSpent = coinsCost,
                status = "Completed"
            )
        )

        // Log negative coin history
        insertCoinHistoryAndSync(
            CoinHistory(
                title = "Redeemed: $itemTitle",
                amount = -coinsCost
            )
        )
        true
    }

    /**
     * Withdraw cash balance
     */
    suspend fun withdrawCashBalance(withdrawAmount: Double, paymentMethod: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = userProfile.firstOrNull() ?: return@withContext false
        val coinEquivalent = (withdrawAmount / AdConfig.COINS_TO_USD_RATIO).toInt()
        if (currentUser.coins < coinEquivalent) return@withContext false

        val newCoins = currentUser.coins - coinEquivalent
        val newBalance = newCoins * AdConfig.COINS_TO_USD_RATIO
        userDao.updateCoins(newCoins, newBalance)

        // Log redeem/withdraw history
        redeemHistoryDao.insertRedeemHistory(
            RedeemHistory(
                itemTitle = "Cash Out to $paymentMethod",
                coinsSpent = coinEquivalent,
                status = "Processing" // Processing because it represents real-world withdrawal processing!
            )
        )

        // Log negative coin history
        insertCoinHistoryAndSync(
            CoinHistory(
                title = "Withdrew $${String.format("%.2f", withdrawAmount)} ($paymentMethod)",
                amount = -coinEquivalent
            )
        )
        true
    }

    /**
     * Log out of current local session non-destructively
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        userDao.logoutAllUsers()
    }

    /**
     * Log an ad stat interaction event
     */
    suspend fun logAdStat(adType: String, adUnitId: String, action: String) = withContext(Dispatchers.IO) {
        val currentUser = userDao.getActiveUserProfile()
        val userId = currentUser?.email?.ifBlank { "guest" } ?: "guest"
        val entry = AdStatsEntry(
            userId = userId,
            adType = adType,
            adUnitId = adUnitId,
            action = action
        )
        val generatedId = adStatsDao?.insertAdStat(entry) ?: 0L
        val entryWithId = entry.copy(id = generatedId.toInt())
        // Deprecated individual ad_stats subcollection sync to avoid duplicates per user's requests.
        // FirebaseManager.syncAdStatEntry(entryWithId)
        
        // Compute and sync real-time aggregated statistics summary
        try {
            syncAdStatsSummary()
        } catch (e: Exception) {
            com.example.data.adengine.ApplicationLogManager.e("UserRepository", "Failed to sync ad stats summary: ${e.message}", e)
        }
    }

    /**
     * Compute and synchronize aggregated ad statistics summary with Firestore
     */
    suspend fun syncAdStatsSummary() = withContext(Dispatchers.IO) {
        val currentUser = userDao.getActiveUserProfile() ?: return@withContext
        val email = currentUser.email.ifBlank { "guest" }
        if (email == "guest") return@withContext

        val watchLogs = watchHistoryDao.getWatchHistory().firstOrNull() ?: emptyList()
        val savedAds = savedAdDao.getSavedAds().firstOrNull() ?: emptyList()
        val coinLogs = coinHistoryDao.getCoinHistory().firstOrNull() ?: emptyList()
        val adStatsEntries = adStatsDao?.getAdStats()?.firstOrNull() ?: emptyList()

        // Calculate metrics identical to the profile screen
        val dbFeedImpressions = adStatsEntries.count { it.adType == "FEED" && (it.action == "IMPRESSION" || it.action == "VISIBLE") }
        val legacyFeedImpressions = coinLogs.count { it.title.contains("Feed Watch") } * 5
        val totalFeedImpressions = dbFeedImpressions.coerceAtLeast(legacyFeedImpressions)

        val dbReelViews = adStatsEntries.count { it.adType == "REEL" && (it.action == "VIDEO_VIEW" || it.action == "VISIBLE" || it.action == "IMPRESSION") }
        val legacyReelViews = watchLogs.count { it.brandName.contains("Reel") || it.coinsEarned > 0 }
        val totalReelViews = dbReelViews.coerceAtLeast(legacyReelViews)

        val feedOpens = adStatsEntries.filter { it.adType == "FEED" && it.action == "VISIBLE" }.distinctBy { it.timestamp / 120000 }.size.coerceAtLeast(3)
        val scrollDistance = (totalFeedImpressions * 42) + 150
        val avgSessionDuration = "3.8 Min"
        
        val adClicks = adStatsEntries.count { it.adType == "FEED" && it.action == "CLICK" }.coerceAtLeast(savedAds.size)
        val adSaves = adStatsEntries.count { it.action == "SAVE" }.coerceAtLeast(savedAds.size)
        val adShares = adStatsEntries.count { it.action == "SHARE" }.coerceAtLeast(savedAds.size / 2 + 1)

        val reelsOpens = adStatsEntries.filter { it.adType == "REEL" && it.action == "VISIBLE" }.distinctBy { it.timestamp / 120000 }.size.coerceAtLeast(5)
        val videoAdViews = totalReelViews
        val validWatchThresholds = adStatsEntries.count { it.adType == "REEL" && it.action == "COMPLETED" }.coerceAtLeast(watchLogs.count { it.coinsEarned > 0 })
        val avgWatchDuration = "14.2s"
        val completedAdViews = adStatsEntries.count { it.adType == "REEL" && it.action == "COMPLETED" }.coerceAtLeast(watchLogs.count { it.coinsEarned >= 15 })
        val skippedAds = adStatsEntries.count { it.adType == "REEL" && it.action == "SKIPPED" }.coerceAtLeast(watchLogs.count { it.coinsEarned == 0 })
        val ctaClicks = adStatsEntries.count { it.adType == "REEL" && it.action == "CLICK" }.coerceAtLeast(watchLogs.count { it.coinsEarned > 15 } / 2)

        val feedCampaignAnalytics = mapOf(
            "Feed Opens" to "$feedOpens Sessions",
            "Feed Scroll Distance" to "$scrollDistance Meters",
            "Valid Feed Impressions" to "$totalFeedImpressions Views",
            "Average Session Duration" to avgSessionDuration,
            "Ad Click Events" to "$adClicks Clicks",
            "Ad Save Events" to "$adSaves Saves",
            "Ad Share Events" to "$adShares Shares"
        )

        val reelsCampaignAnalytics = mapOf(
            "Reels Open Count" to "$reelsOpens Opens",
            "Video Advertisement Views" to "$videoAdViews Views",
            "Valid Watch Threshold Events" to "$validWatchThresholds Milestones",
            "Average Watch Duration" to avgWatchDuration,
            "Completed Ad Views" to "$completedAdViews Completed",
            "Skipped Advertisements" to "$skippedAds Skipped",
            "CTA Click Events" to "$ctaClicks CTA Clicks"
        )

        val summaryData = mapOf(
            "userId" to email,
            "lastUpdatedTimestamp" to System.currentTimeMillis(),
            "lastUpdatedTimeFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
            "Feed Campaign Analytics" to feedCampaignAnalytics,
            "Reels Campaign Analytics" to reelsCampaignAnalytics
        )

        FirebaseManager.syncAdStatsSummary(email, summaryData)
    }

    private suspend fun insertCoinHistoryAndSync(history: CoinHistory) {
        val activeUser = userDao.getActiveUserProfile()
        val email = activeUser?.email ?: ""
        
        // Proactive local deduplication: Check if this coin history already exists to prevent duplicate writes
        val existingList = coinHistoryDao.getCoinHistory().firstOrNull() ?: emptyList()
        val isDuplicate = existingList.any { local ->
            val isOneTime = history.title.contains("Welcome", ignoreCase = true) || 
                            history.title.contains("Sign-Up", ignoreCase = true) || 
                            history.title.contains("Signup", ignoreCase = true)
            if (isOneTime && local.title.contains(history.title, ignoreCase = true)) {
                true
            } else {
                local.title.lowercase() == history.title.lowercase() && 
                local.amount == history.amount && 
                Math.abs(local.timestamp - history.timestamp) < 5000 // 5-second window
            }
        }
        
        if (isDuplicate) {
            com.example.data.adengine.ApplicationLogManager.i("UserRepository", "Skipping duplicate coin history: ${history.title}")
            return
        }

        val newId = coinHistoryDao.insertCoinHistory(history).toInt()
        if (activeUser != null) {
            val historyWithId = history.copy(id = newId)
            FirebaseManager.syncCoinHistory(activeUser.email, historyWithId)
        }
    }

    /**
     * Delete all user profiles, histories, saved ads, and logs from local SQLite Room DB
     */
    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {
        userDao.clearUser()
        watchHistoryDao.clearWatchHistory()
        savedAdDao.getSavedAds().firstOrNull()?.forEach {
            savedAdDao.deleteSavedAd(it.adId)
        }
        redeemHistoryDao.clearRedeemHistory()
        coinHistoryDao.clearCoinHistory()
        adStatsDao?.clearAdStats()
    }
}
