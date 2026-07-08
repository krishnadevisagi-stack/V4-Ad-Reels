package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.WalletRepository
import com.example.data.utils.*
import com.example.data.adengine.SecurityManager
import com.example.data.adengine.AnalyticsManager
import com.example.data.adengine.MonitoringManager
import com.example.data.adengine.ApplicationEventBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * -----------------------------------------------------------------
 * WALLET SCREEN VIEW MODEL
 * -----------------------------------------------------------------
 * Purpose: Manages local transactions state flow pipelines, vouchers, and cashouts.
 * Responsibilities:
 *   - Display active coin balances, transaction lists, and cashout history.
 *   - Process voucher redemption requests.
 *   - Listen to central event buses for ad watch awards.
 * Dependencies:
 *   - [WalletRepository], [NotificationEngine]
 * Future Extension: Connect to premium banking or stripe APIs for payout delivery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    val walletRepository = WalletRepository(
        walletDao = db.walletDao(),
        rewardHistoryDao = db.rewardHistoryDao(),
        walletRedeemHistoryDao = db.walletRedeemHistoryDao(),
        withdrawalHistoryDao = db.withdrawalHistoryDao(),
        walletActivityDao = db.walletActivityDao(),
        rewardConfigDao = db.rewardConfigDao(),
        walletAnalyticsDao = db.walletAnalyticsDao(),
        userDao = db.userDao()
    )

    val notificationEngine = NotificationEngine()
    val analyticsEngine = AnalyticsEngine(walletRepository)
    val walletEngine = WalletEngine(walletRepository, notificationEngine)
    val rewardEngine = WalletRewardEngine(
        walletRepository = walletRepository,
        walletEngine = walletEngine,
        analyticsEngine = analyticsEngine,
        isTamperedCheck = { com.example.data.adengine.SourceCodeIntegrityChecker.isTampered(application) }
    )
    val securityManager = SecurityManager(walletRepository)

    // User Profile integration from main app database
    val activeUser: StateFlow<UserProfile?> = db.userDao().getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Derived active userId
    val userIdFlow: Flow<String> = activeUser.map { it?.email ?: "guest" }.distinctUntilChanged()

    // Wallet State Flow
    val walletState: StateFlow<WalletEntity?> = userIdFlow.flatMapLatest { userId ->
        walletRepository.initializeWalletIfNeeded(userId)
        walletRepository.getWallet(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Rewards earned history
    val rewardHistoryState: StateFlow<List<RewardHistoryEntity>> = userIdFlow.flatMapLatest { userId ->
        walletRepository.getRewards(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Vouchers redeemed history
    val redeemHistoryState: StateFlow<List<RedeemHistoryEntity>> = userIdFlow.flatMapLatest { userId ->
        walletRepository.getRedemptions(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UPI Cashouts history
    val withdrawalHistoryState: StateFlow<List<WithdrawalHistoryEntity>> = userIdFlow.flatMapLatest { userId ->
        walletRepository.getWithdrawals(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unified Wallet Activity Timeline flow
    val activitiesState: StateFlow<List<WalletActivityEntity>> = userIdFlow.flatMapLatest { userId ->
        walletRepository.getActivities(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Configuration Flow
    val configState: StateFlow<RewardConfigEntity?> = walletRepository.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Wallet Analytics Flow
    val analyticsState: StateFlow<WalletAnalyticsEntity?> = userIdFlow.flatMapLatest { userId ->
        walletRepository.getAnalytics(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Search and Filters for the Activity Screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL") // "ALL", "REWARD", "REDEEM", "WITHDRAW"
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _sortByNewest = MutableStateFlow(true)
    val sortByNewest = _sortByNewest.asStateFlow()

    // Filtered Activity Timeline
    val filteredActivities: StateFlow<List<WalletActivityEntity>> = combine(
        activitiesState,
        _searchQuery,
        _selectedFilter,
        _sortByNewest
    ) { activities, query, filter, sortByNewest ->
        var list = activities.filter {
            (filter == "ALL" || it.type == filter) &&
            (it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true))
        }
        list = if (sortByNewest) {
            list.sortedByDescending { it.timestamp }
        } else {
            list.sortedBy { it.timestamp }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            walletRepository.initializeDefaultConfig()
            userIdFlow.collectLatest { userId ->
                walletRepository.initializeWalletIfNeeded(userId)
                analyticsEngine.recalculateDailyAnalytics(userId)
                com.example.data.firebase.FirebaseManager.syncPendingRequests(userId, walletRepository, viewModelScope)
            }
        }
    }

    fun refreshTransactionsAndApprovals() {
        viewModelScope.launch {
            val userId = activeUser.value?.email ?: "guest"
            com.example.data.firebase.FirebaseManager.syncPendingRequests(userId, walletRepository, viewModelScope)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun toggleSortOrder() {
        _sortByNewest.value = !_sortByNewest.value
    }

    /**
     * Updates configuration settings
     */
    fun saveRewardConfig(config: RewardConfigEntity) {
        viewModelScope.launch {
            walletRepository.saveConfig(config)
        }
    }

    /**
     * Executes cashout withdrawal flow via UPI.
     * Contains validation. Deducts coins safely.
     */
    fun cashOutUPI(
        upiIdOrMobile: String,
        amountRupees: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = activeUser.value
            val userId = user?.email ?: "guest"
            val config = walletRepository.getActiveConfig()
            val wallet = walletState.value ?: return@launch

            // Rule 1: Validation checks
            if (upiIdOrMobile.isBlank()) {
                onError("UPI details cannot be blank.")
                return@launch
            }

            val isUpiId = upiIdOrMobile.contains("@")
            if (isUpiId) {
                val parts = upiIdOrMobile.split("@")
                if (parts.size != 2 || parts[0].length < 2 || parts[1].length < 2) {
                    onError("Invalid UPI ID format. Example: name@okbank")
                    return@launch
                }
            } else {
                // Must be 10-digit mobile number
                val cleanMobile = upiIdOrMobile.filter { it.isDigit() }
                if (cleanMobile.length != 10) {
                    onError("UPI Mobile Number must be exactly 10 digits.")
                    return@launch
                }
            }

            if (amountRupees <= 0) {
                onError("Please enter an amount greater than zero.")
                return@launch
            }

            // Conversion from config (default 100 coins = 1 Rupee)
            val coinCost = (amountRupees * config.coinToRupeeRatio).toInt()

            if (wallet.redeemableCoins < coinCost) {
                onError("Insufficient balance. You need $coinCost coins to cash out ₹$amountRupees.")
                return@launch
            }

            // Security Validation Checks
            if (!securityManager.monitorRedemptionSpeed()) {
                onError("Action blocked: Too many redemption attempts in a short period.")
                return@launch
            }
            if (!securityManager.verifyWalletIntegrity(userId)) {
                onError("CRITICAL: Wallet integrity validation failed. Action blocked.")
                return@launch
            }

            val withdrawalId = "WITHDRAW_${UUID.randomUUID().toString().take(8)}"
            // Secure Deduction inside WalletEngine
            val success = walletEngine.deductCoins(
                userId = userId,
                coins = coinCost,
                activityTitle = "UPI Withdrawal Request",
                activityDesc = "Spent -$coinCost Coins for ₹$amountRupees",
                type = "WITHDRAW",
                customActivityId = withdrawalId
            )

            if (success) {
                // Insert Withdrawal history entry with "Pending" status
                val withdrawal = WithdrawalHistoryEntity(
                    withdrawalId = withdrawalId,
                    userId = userId,
                    upiId = upiIdOrMobile,
                    amountRupees = amountRupees,
                    coinsCost = coinCost,
                    status = "Pending"
                )
                walletRepository.insertWithdrawalRecord(withdrawal)
                com.example.data.firebase.FirebaseManager.createUpiRequest(withdrawal)
                onSuccess()
            } else {
                onError("Failed to process withdrawal. Please try again.")
            }
        }
    }

    /**
     * Executes gift voucher redemption flow.
     * Contains balance validation, coin deduction, and logs codes.
     */
    fun redeemGiftVoucher(
        brandName: String,
        coinsCost: Int,
        estimatedValueRupees: Double,
        onSuccess: (String) -> Unit, // passes the generated dummy code
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val user = activeUser.value
            val userId = user?.email ?: "guest"
            val wallet = walletState.value ?: return@launch

            if (wallet.redeemableCoins < coinsCost) {
                onError("Insufficient balance. You need $coinsCost coins to redeem this Swiggy/Swiggy voucher.")
                return@launch
            }

            // Security Validation Checks
            if (!securityManager.monitorRedemptionSpeed()) {
                onError("Action blocked: Too many redemption attempts in a short period.")
                return@launch
            }
            if (!securityManager.verifyWalletIntegrity(userId)) {
                onError("CRITICAL: Wallet integrity validation failed. Action blocked.")
                return@launch
            }

            val redeemId = "REDEEM_${UUID.randomUUID().toString().take(8)}"
            // Secure Deduction inside WalletEngine
            val success = walletEngine.deductCoins(
                userId = userId,
                coins = coinsCost,
                activityTitle = "Redeemed $brandName Voucher",
                activityDesc = "Spent -$coinsCost Coins for Voucher ₹$estimatedValueRupees",
                type = "REDEEM",
                customActivityId = redeemId
            )

            if (success) {
                val redeem = RedeemHistoryEntity(
                    redeemId = redeemId,
                    userId = userId,
                    brandName = brandName,
                    coinsCost = coinsCost,
                    estimatedValueRupees = estimatedValueRupees,
                    status = "Pending", // Set as Pending initially
                    voucherCode = "" // Empty voucher code initially until approved
                )
                walletRepository.insertRedeemRecord(redeem)
                com.example.data.firebase.FirebaseManager.createVoucherRequest(redeem)
                onSuccess("Pending Review")
            } else {
                onError("Redemption failed. Check your balance.")
            }
        }
    }

    /**
     * Trigger bonus or campaign reward.
     */
    fun addBonusCoins(amount: Int, title: String) {
        viewModelScope.launch {
            val user = activeUser.value
            val userId = user?.email ?: "guest"
            rewardEngine.processBonusReward(userId, amount, title)
        }
    }

    fun clearActivities() {
        viewModelScope.launch {
            val user = activeUser.value
            val userId = user?.email ?: "guest"
            walletRepository.clearAllActivity(userId)
        }
    }
}
