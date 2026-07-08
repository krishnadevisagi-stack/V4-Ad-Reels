package com.example.data.firebase

import android.content.Context
import com.example.data.adengine.ApplicationLogManager
import com.example.data.model.*
import com.example.data.repository.WalletRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var appContext: Context? = null

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(applicationContext)
                ApplicationLogManager.i(TAG, "Firebase initialized manually via init")
            }
        } catch (e: Exception) {
            ApplicationLogManager.e(TAG, "Manual Firebase initialization error: ${e.message}", e)
        }
    }

    private fun showToast(message: String) {
        appContext?.let { context ->
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                ApplicationLogManager.e(TAG, "Error showing toast: ${e.message}", e)
            }
        }
    }

    // Checks if Firebase services are successfully initialized with google-services.json
    val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }

    fun getAuth(): FirebaseAuth? {
        return if (isFirebaseAvailable) {
            FirebaseAuth.getInstance()
        } else {
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isFirebaseAvailable) {
            FirebaseFirestore.getInstance()
        } else {
            null
        }
    }

    /**
     * Synchronize user profile and coin balance with Firebase Firestore
     */
    fun syncUserProfile(userProfile: UserProfile) {
        val firestore = getFirestore() ?: run {
            ApplicationLogManager.i(TAG, "Firebase not initialized. Simulating user profile sync for ${userProfile.email}")
            return
        }

        val userDoc = firestore.collection("users").document(userProfile.email)
        val data = mapOf(
            "email" to userProfile.email,
            "username" to userProfile.username,
            "fullName" to userProfile.fullName,
            "mobile" to userProfile.mobile,
            "coins" to userProfile.coins,
            "walletBalance" to userProfile.walletBalance,
            "walletId" to userProfile.walletId,
            "guestAccount" to userProfile.guestAccount,
            "lastLogin" to userProfile.lastLogin,
            "updatedAt" to System.currentTimeMillis()
        )

        userDoc.set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully synced user profile to Firebase: ${userProfile.email}")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.e(TAG, "Failed to sync user profile to Firebase: ${e.message}", e)
            }
    }

    /**
     * Fetch user profile from Firebase Firestore by email
     */
    suspend fun fetchUserProfile(email: String): UserProfile? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        try {
            val doc = firestore.collection("users").document(email).get().await()
            if (doc.exists()) {
                val coinsVal = doc.getLong("coins")?.toInt() ?: 0
                val walletBalanceVal = doc.getDouble("walletBalance") ?: 0.0
                val walletIdVal = doc.getString("walletId") ?: "W-${(100000..999999).random()}"
                val usernameVal = doc.getString("username") ?: email.substringBefore("@")
                val fullNameVal = doc.getString("fullName") ?: usernameVal
                val mobileVal = doc.getString("mobile") ?: ""
                val guestAccountVal = doc.getBoolean("guestAccount") ?: false
                
                UserProfile(
                    id = 0,
                    username = usernameVal,
                    fullName = fullNameVal,
                    email = email,
                    mobile = mobileVal,
                    passwordHash = doc.getString("passwordHash") ?: "google_auth_session",
                    guestAccount = guestAccountVal,
                    walletId = walletIdVal,
                    selectedCategories = "",
                    coins = coinsVal,
                    walletBalance = walletBalanceVal,
                    isLoggedIn = true,
                    profileCreated = true,
                    createdDate = doc.getLong("createdDate") ?: System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            ApplicationLogManager.e(TAG, "Error fetching user profile from Firestore: ${e.message}", e)
            null
        }
    }

    /**
     * Creates a new UPI cashout withdrawal request on Firebase Firestore
     */
    fun createUpiRequest(withdrawal: WithdrawalHistoryEntity, showToast: Boolean = true) {
        val firestore = getFirestore() ?: run {
            ApplicationLogManager.i(TAG, "Firebase not initialized. Simulated UPI Cashout uploaded to WebAdmin (Pending)")
            return
        }

        val data = mapOf(
            "activityId" to withdrawal.withdrawalId,
            "requestId" to withdrawal.withdrawalId,
            "userId" to withdrawal.userId,
            "upiId" to withdrawal.upiId,
            "amountRupees" to withdrawal.amountRupees,
            "coinsCost" to withdrawal.coinsCost,
            "amountCoins" to -withdrawal.coinsCost,
            "title" to "UPI Withdrawal Request",
            "description" to "Spent -${withdrawal.coinsCost} Coins for ₹${withdrawal.amountRupees}",
            "type" to "WITHDRAWAL",
            "status" to withdrawal.status, // "Pending"
            "adminRemark" to withdrawal.adminRemark,
            "remark" to withdrawal.adminRemark, // Both remark and adminRemark
            "timestamp" to withdrawal.timestamp,
            "timestampFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(withdrawal.timestamp))
        )

        // Write EXACTLY ONE document in logs_activity subcollection with ID equal to the withdrawalId
        firestore.collection("users").document(withdrawal.userId).collection("logs_activity").document(withdrawal.withdrawalId).set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "UPI withdrawal request successfully uploaded as logs_activity: ${withdrawal.withdrawalId}")
                if (showToast) {
                    showToast("UPI withdrawal submitted successfully!")
                }
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.e(TAG, "Failed to upload UPI request as logs_activity: ${e.message}", e)
                if (showToast) {
                    showToast("Failed to upload UPI request: ${e.localizedMessage}")
                }
            }
    }

    /**
     * Creates a new Voucher redemption request on Firebase Firestore
     */
    fun createVoucherRequest(redeem: RedeemHistoryEntity, showToast: Boolean = true) {
        val firestore = getFirestore() ?: run {
            ApplicationLogManager.i(TAG, "Firebase not initialized. Simulated Voucher redemption uploaded to WebAdmin (Pending)")
            return
        }

        val data = mapOf(
            "activityId" to redeem.redeemId,
            "requestId" to redeem.redeemId,
            "userId" to redeem.userId,
            "brandName" to redeem.brandName,
            "coinsCost" to redeem.coinsCost,
            "amountCoins" to -redeem.coinsCost,
            "estimatedValueRupees" to redeem.estimatedValueRupees,
            "title" to "Voucher Redeem: ${redeem.brandName}",
            "description" to "Spent -${redeem.coinsCost} Coins for Voucher ₹${redeem.estimatedValueRupees}",
            "type" to "REDEMPTION",
            "status" to redeem.status, // "Pending"
            "voucherCode" to redeem.voucherCode, // Empty initially
            "redeemCode" to redeem.voucherCode, // Both voucherCode and redeemCode
            "adminRemark" to redeem.adminRemark,
            "remark" to redeem.adminRemark, // Both remark and adminRemark
            "timestamp" to redeem.timestamp,
            "timestampFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(redeem.timestamp))
        )

        // Write EXACTLY ONE document in logs_activity subcollection with ID equal to the redeemId
        firestore.collection("users").document(redeem.userId).collection("logs_activity").document(redeem.redeemId).set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Voucher redemption request successfully uploaded as logs_activity: ${redeem.redeemId}")
                if (showToast) {
                    showToast("Voucher request submitted successfully!")
                }
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.e(TAG, "Failed to upload Voucher request as logs_activity: ${e.message}", e)
                if (showToast) {
                    showToast("Failed to upload Voucher request: ${e.localizedMessage}")
                }
            }
    }

    /**
     * Syncs request approvals and deep user state from Firestore back into local SQLite Room database.
     * Delegates to deep dual-sync syncCloudDataToLocal.
     */
    fun syncPendingRequests(
        userId: String,
        walletRepository: WalletRepository,
        scope: CoroutineScope,
        onComplete: () -> Unit = {}
    ) {
        val firestore = getFirestore()
        if (firestore == null) {
            // Firebase not configured yet, run offline-simulation of admin approval flow
            simulateOfflineApproval(userId, walletRepository, scope)
            onComplete()
            return
        }
        syncCloudDataToLocal(userId, scope) {
            onComplete()
        }
    }

    /**
     * Deep Cloud-to-Local Sync: Fetches user profile, coin histories, watch histories,
     * wallet activities, UPI withdrawals, and voucher redemptions from Firestore
     * and saves them to local SQLite Room database.
     */
    fun syncCloudDataToLocal(
        email: String,
        scope: CoroutineScope,
        isSilent: Boolean = true,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val firestore = getFirestore()
        val context = appContext
        if (firestore == null || context == null) {
            ApplicationLogManager.e(TAG, "Firebase or Context not ready for deep sync")
            onComplete(false)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.database.AppDatabase.getDatabase(context)
                
                // 1. Fetch User Profile
                val userDoc = firestore.collection("users").document(email).get().await()
                if (userDoc.exists()) {
                    val coinsVal = userDoc.getLong("coins")?.toInt() ?: 0
                    val walletBalanceVal = userDoc.getDouble("walletBalance") ?: 0.0
                    val walletIdVal = userDoc.getString("walletId") ?: "W-${(100000..999999).random()}"
                    val usernameVal = userDoc.getString("username") ?: email.substringBefore("@")
                    val fullNameVal = userDoc.getString("fullName") ?: usernameVal
                    val mobileVal = userDoc.getString("mobile") ?: ""
                    val guestAccountVal = userDoc.getBoolean("guestAccount") ?: false
                    
                    val existingLocalUser = db.userDao().getUserByEmail(email)
                    val updatedProfile = if (existingLocalUser != null) {
                        existingLocalUser.copy(
                            coins = coinsVal,
                            walletBalance = walletBalanceVal,
                            walletId = walletIdVal,
                            username = usernameVal,
                            fullName = fullNameVal,
                            mobile = mobileVal,
                            guestAccount = guestAccountVal,
                            isLoggedIn = true,
                            lastLogin = System.currentTimeMillis()
                        )
                    } else {
                        UserProfile(
                            id = 0,
                            username = usernameVal,
                            fullName = fullNameVal,
                            email = email,
                            mobile = mobileVal,
                            passwordHash = userDoc.getString("passwordHash") ?: "google_auth_session",
                            guestAccount = guestAccountVal,
                            walletId = walletIdVal,
                            selectedCategories = "",
                            coins = coinsVal,
                            walletBalance = walletBalanceVal,
                            isLoggedIn = true,
                            profileCreated = true,
                            createdDate = userDoc.getLong("createdDate") ?: System.currentTimeMillis(),
                            lastLogin = System.currentTimeMillis()
                        )
                    }
                    db.userDao().insertUserProfile(updatedProfile)
                    
                    // Sync local WalletEntity
                    val existingWallet = db.walletDao().getWallet(email)
                    if (existingWallet != null) {
                        val updatedWallet = existingWallet.copy(
                            currentCoins = coinsVal,
                            redeemableCoins = coinsVal,
                            lastUpdated = System.currentTimeMillis()
                        )
                        db.walletDao().insertWallet(updatedWallet)
                    } else {
                        val wallet = WalletEntity(
                            userId = email,
                            currentCoins = coinsVal,
                            todayCoins = coinsVal,
                            weeklyCoins = coinsVal,
                            lifetimeCoins = coinsVal,
                            pendingCoins = 0,
                            redeemableCoins = coinsVal,
                            lastUpdated = System.currentTimeMillis()
                        )
                        db.walletDao().insertWallet(wallet)
                    }
                }

                // 2. Sync subcollection coin_histories
                val coinHistDocs = firestore.collection("users").document(email).collection("coin_histories").get().await()
                for (doc in coinHistDocs.documents) {
                    val id = doc.getLong("id")?.toInt() ?: continue
                    if (id <= 0) continue
                    val title = doc.getString("title") ?: "Reward"
                    val amount = doc.getLong("amount")?.toInt() ?: 0
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    val history = CoinHistory(
                        id = id,
                        title = title,
                        amount = amount,
                        timestamp = timestamp
                    )
                    db.coinHistoryDao().insertCoinHistory(history)
                }

                // 3. Sync subcollection watch_histories
                val watchHistDocs = firestore.collection("users").document(email).collection("watch_histories").get().await()
                for (doc in watchHistDocs.documents) {
                    val id = doc.getLong("id")?.toInt() ?: continue
                    if (id <= 0) continue
                    val adId = doc.getString("adId") ?: ""
                    val brandName = doc.getString("brandName") ?: ""
                    val coinsEarned = doc.getLong("coinsEarned")?.toInt() ?: 0
                    val category = doc.getString("category") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    val watch = WatchHistory(
                        id = id,
                        adId = adId,
                        brandName = brandName,
                        coinsEarned = coinsEarned,
                        category = category,
                        timestamp = timestamp
                    )
                    db.watchHistoryDao().insertWatchHistory(watch)
                }

                // 4. Sync subcollection logs_activity to WalletActivity (using logs_activity instead of wallet_activities)
                val logsDocsList = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                try {
                    val logsDocs = firestore.collection("users").document(email).collection("logs_activity").get().await()
                    logsDocsList.addAll(logsDocs.documents)
                } catch (e: Exception) {
                    ApplicationLogManager.e(TAG, "logs_activity subcollection fetch error: ${e.message}")
                }

                for (doc in logsDocsList) {
                    val activityId = doc.getString("activityId") ?: doc.id
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    val amountCoins = doc.getLong("amountCoins")?.toInt() ?: 0
                    val type = doc.getString("type") ?: "REWARD"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    val act = WalletActivityEntity(
                        activityId = activityId,
                        userId = email,
                        title = title,
                        description = description,
                        amountCoins = amountCoins,
                        type = type,
                        timestamp = timestamp
                    )
                    db.walletActivityDao().insertActivity(act)
                }

                // 5. Sync subcollection reward_histories
                val rewardHistDocs = firestore.collection("users").document(email).collection("reward_histories").get().await()
                for (doc in rewardHistDocs.documents) {
                    val rewardId = doc.getString("rewardId") ?: continue
                    val adId = doc.getString("adId") ?: ""
                    val amountCoins = doc.getLong("amountCoins")?.toInt() ?: 0
                    val sourceType = doc.getString("sourceType") ?: "FEED"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    val reward = RewardHistoryEntity(
                        rewardId = rewardId,
                        adId = adId,
                        userId = email,
                        amountCoins = amountCoins,
                        sourceType = sourceType,
                        timestamp = timestamp
                    )
                    db.rewardHistoryDao().insertReward(reward)
                }

                // 6. Sync requests_upi (Withdrawals) - Fetch from logs_activity!
                val upiDocsList = logsDocsList.filter { doc ->
                    val type = doc.getString("type") ?: ""
                    val docId = doc.id
                    type == "WITHDRAW" || type == "WITHDRAWAL" || docId.startsWith("WITHDRAW_")
                }

                for (doc in upiDocsList) {
                    val requestId = doc.getString("activityId") ?: doc.getString("requestId") ?: doc.id
                    val upiId = doc.getString("upiId") ?: ""
                    val amountRupees = doc.getDouble("amountRupees") ?: 0.0
                    val coinsCost = doc.getLong("coinsCost")?.toInt() ?: doc.getLong("amountCoins")?.toInt()?.let { -it } ?: 0
                    
                    val rawStatus = doc.getString("status") ?: "Pending"
                    val adminRemark = doc.getString("adminRemark")
                        ?: doc.getString("remark")
                        ?: doc.getString("admin_remark")
                        ?: ""
                        
                    val adminRemarkLower = adminRemark.lowercase(java.util.Locale.getDefault())
                    val isSuccessRemark = adminRemarkLower.contains("successful") || adminRemarkLower.contains("success") || adminRemarkLower.contains("approved") || adminRemarkLower.contains("completed")
                    
                    val resolvedStatus = if (isSuccessRemark || rawStatus == "Approved" || rawStatus == "Success" || rawStatus == "Completed" || rawStatus == "Successful") {
                        "Completed"
                    } else {
                        rawStatus
                    }
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    val withdrawal = WithdrawalHistoryEntity(
                        withdrawalId = requestId,
                        userId = email,
                        upiId = upiId,
                        amountRupees = amountRupees,
                        coinsCost = coinsCost,
                        status = resolvedStatus,
                        adminRemark = adminRemark,
                        timestamp = timestamp
                    )
                    db.withdrawalHistoryDao().insertWithdrawal(withdrawal)
                }

                // 7. Sync requests_vouchers (Redemptions) - Fetch from logs_activity!
                val voucherDocsList = logsDocsList.filter { doc ->
                    val type = doc.getString("type") ?: ""
                    val docId = doc.id
                    type == "REDEEM" || type == "REDEMPTION" || docId.startsWith("REDEEM_")
                }

                for (doc in voucherDocsList) {
                    val requestId = doc.getString("activityId") ?: doc.getString("requestId") ?: doc.id
                    val brandName = doc.getString("brandName") ?: doc.getString("title")?.removePrefix("Voucher Redeem: ") ?: ""
                    val coinsCost = doc.getLong("coinsCost")?.toInt() ?: doc.getLong("amountCoins")?.toInt()?.let { -it } ?: 0
                    val estimatedValueRupees = doc.getDouble("estimatedValueRupees") ?: 0.0
                    
                    val rawStatus = doc.getString("status") ?: "Pending"
                    val voucherCode = doc.getString("voucherCode")
                        ?: doc.getString("voucher_code")
                        ?: doc.getString("redeemCode")
                        ?: doc.getString("redeem_code")
                        ?: doc.getString("redeem code")
                        ?: doc.getString("code")
                        ?: ""
                    val adminRemark = doc.getString("adminRemark")
                        ?: doc.getString("remark")
                        ?: doc.getString("admin_remark")
                        ?: ""
                        
                    val adminRemarkLower = adminRemark.lowercase(java.util.Locale.getDefault())
                    val isSuccessRemark = adminRemarkLower.contains("successful") || adminRemarkLower.contains("success") || adminRemarkLower.contains("approved") || adminRemarkLower.contains("completed")
                    
                    val resolvedStatus = if (voucherCode.isNotEmpty() || isSuccessRemark || rawStatus == "Approved" || rawStatus == "Success" || rawStatus == "Completed" || rawStatus == "Successful") {
                        "Completed"
                    } else {
                        rawStatus
                    }
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    
                    val redemption = RedeemHistoryEntity(
                        redeemId = requestId,
                        userId = email,
                        brandName = brandName,
                        coinsCost = coinsCost,
                        estimatedValueRupees = estimatedValueRupees,
                        status = resolvedStatus,
                        voucherCode = voucherCode,
                        adminRemark = adminRemark,
                        timestamp = timestamp
                    )
                    db.walletRedeemHistoryDao().insertRedeem(redemption)
                }

                ApplicationLogManager.i(TAG, "Completed deep cloud-to-local sync for $email")
                if (!isSilent) {
                    withContext(Dispatchers.Main) {
                        showToast("Sync completed! Wallet activity & history restored.")
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                ApplicationLogManager.e(TAG, "Deep sync error: ${e.message}", e)
                if (!isSilent) {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to restore history: ${e.localizedMessage}")
                    }
                }
                onComplete(false)
            }
        }
    }

    /**
     * Simulates admin approval for local development if Firebase is not linked yet.
     * Approves "Pending" requests that are older than 15 seconds to demonstrate the real-world flow.
     */
    private fun simulateOfflineApproval(userId: String, walletRepository: WalletRepository, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            
            // 1. Simulate UPI Cashout Approvals
            val localWithdrawals = walletRepository.getWithdrawals(userId).first()
            for (w in localWithdrawals) {
                if (w.status == "Pending" && (now - w.timestamp) > 15000) {
                    val approved = w.copy(
                        status = "Completed",
                        adminRemark = "Paid successfully via UPI IMPS. Transaction ID: UPI${(100000..999999).random()}AXIS"
                    )
                    walletRepository.insertWithdrawalRecord(approved)


                    ApplicationLogManager.i(TAG, "Simulated Approval: UPI Cashout ${w.withdrawalId} marked Approved.")
                }
            }

            // 2. Simulate Voucher Approvals
            val localRedemptions = walletRepository.getRedemptions(userId).first()
            for (r in localRedemptions) {
                if (r.status == "Pending" && (now - r.timestamp) > 15000) {
                    val randCode = "${r.brandName.take(3).uppercase()}-${(10000..99999).random()}-${(1000..9999).random()}"
                    val approved = r.copy(
                        status = "Completed", 
                        voucherCode = randCode,
                        adminRemark = "Gift voucher activated successfully. Apply code during checkout."
                    )
                    walletRepository.insertRedeemRecord(approved)


                    ApplicationLogManager.i(TAG, "Simulated Approval: Voucher ${r.redeemId} marked Approved with code: $randCode.")
                }
            }
        }
    }

    /**
     * Synchronize a wallet activity with Firestore
     */
    fun syncWalletActivity(activity: WalletActivityEntity) {
        val firestore = getFirestore() ?: return
         val data = mapOf(
            "activityId" to activity.activityId,
            "userId" to activity.userId,
            "title" to activity.title,
            "description" to activity.description,
            "amountCoins" to activity.amountCoins,
            "type" to activity.type,
            "timestamp" to activity.timestamp,
            "timestampFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp))
        )

        // Sync ONLY to user select user by clicking on email -> logs activity subcollection
        firestore.collection("users").document(activity.userId).collection("logs_activity").document(activity.activityId).set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully synced wallet activity to logs_activity subcollection: ${activity.activityId}")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to sync wallet activity to logs_activity: ${e.message}")
            }
    }

    /**
     * Synchronize a coin history entry with Firestore
     */
    fun syncCoinHistory(userId: String, history: CoinHistory) {
        val firestore = getFirestore() ?: return
        val id = "COIN_HIST_${history.id}_${history.timestamp}"
        val data = mapOf(
            "id" to history.id,
            "userId" to userId,
            "title" to history.title,
            "amount" to history.amount,
            "timestamp" to history.timestamp
        )
        // 1. Root collection (DEPRECATED - Removed global collection write to avoid duplicates)
        // 2. Isolated user document subcollection
        firestore.collection("users").document(userId).collection("coin_histories").document(id).set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully synced coin history to user subcollection: $id")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to sync coin history to user subcollection: ${e.message}")
            }

        // 3. Standardize and write to logs_activity subcollection
        val formattedLog = mapOf(
            "activityId" to id,
            "userId" to userId,
            "title" to history.title,
            "description" to "Earned +${history.amount} Coins",
            "amountCoins" to history.amount,
            "type" to "COIN_HISTORY",
            "timestamp" to history.timestamp,
            "timestampFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(history.timestamp))
        )
        firestore.collection("users").document(userId).collection("logs_activity").document(id).set(formattedLog)
    }

    /**
     * Synchronize a reward history record with Firestore
     */
    fun syncRewardHistory(reward: RewardHistoryEntity) {
        val firestore = getFirestore() ?: return
        val data = mapOf(
            "rewardId" to reward.rewardId,
            "adId" to reward.adId,
            "userId" to reward.userId,
            "amountCoins" to reward.amountCoins,
            "sourceType" to reward.sourceType,
            "timestamp" to reward.timestamp
        )
        // 1. Root collection (DEPRECATED - Removed global collection write to avoid duplicates)
        // 2. Isolated user document subcollection
        firestore.collection("users").document(reward.userId).collection("reward_histories").document(reward.rewardId).set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully synced reward history to user subcollection: ${reward.rewardId}")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to sync reward history to user subcollection: ${e.message}")
            }

        // 3. Standardize and write to logs_activity subcollection
        val formattedLog = mapOf(
            "activityId" to reward.rewardId,
            "userId" to reward.userId,
            "title" to "Reward: ${reward.sourceType}",
            "description" to "Earned +${reward.amountCoins} Coins",
            "amountCoins" to reward.amountCoins,
            "type" to reward.sourceType,
            "timestamp" to reward.timestamp,
            "timestampFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(reward.timestamp))
        )
        firestore.collection("users").document(reward.userId).collection("logs_activity").document(reward.rewardId).set(formattedLog)
    }

    /**
     * Synchronize watch history with Firestore
     */
    fun syncWatchHistory(userId: String, watch: WatchHistory) {
        val firestore = getFirestore() ?: return
        val id = "WATCH_${watch.id}_${watch.timestamp}"
        val data = mapOf(
            "id" to watch.id,
            "userId" to userId,
            "adId" to watch.adId,
            "brandName" to watch.brandName,
            "coinsEarned" to watch.coinsEarned,
            "category" to watch.category,
            "timestamp" to watch.timestamp
        )
        // 1. Root collection (DEPRECATED - Removed global collection write to avoid duplicates)
        // 2. Isolated user document subcollection
        firestore.collection("users").document(userId).collection("watch_histories").document(id).set(data)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully synced watch history to user subcollection: $id")
            }
            .addOnFailureListener { e ->
                ApplicationLogManager.d(TAG, "Failed to sync watch history to user subcollection: ${e.message}")
            }

        // 3. Standardize and write to logs_activity subcollection
        val formattedLog = mapOf(
            "activityId" to id,
            "userId" to userId,
            "title" to "Watched Ad: ${watch.brandName}",
            "description" to "Earned +${watch.coinsEarned} Coins",
            "amountCoins" to watch.coinsEarned,
            "type" to "WATCH_HISTORY",
            "timestamp" to watch.timestamp,
            "timestampFormatted" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(watch.timestamp))
        )
        firestore.collection("users").document(userId).collection("logs_activity").document(id).set(formattedLog)
    }

    /**
     * Synchronize individual ad stat entries with Firestore
     */
    fun syncAdStatEntry(entry: AdStatsEntry) {
        // DEPRECATED: Individual ad_stats entries are redundant and removed per user's requests to avoid duplicate data and clutter.
    }

    /**
     * Synchronize aggregated ad statistics summary with Firestore
     */
    fun syncAdStatsSummary(userId: String, summaryData: Map<String, Any>) {
        val firestore = getFirestore() ?: return
        
        // 1. Root collection of summaries (DEPRECATED - Removed global collection write to avoid duplicates)

        // 2. Embedded directly within the user document for unified profile view - THIS IS THE AUTHORITATIVE METRIC PATH
        firestore.collection("users").document(userId).update("adStatsSummary", summaryData)
            .addOnSuccessListener {
                ApplicationLogManager.i(TAG, "Successfully updated adStatsSummary within user profile document: $userId")
            }
            .addOnFailureListener { e ->
                // If update fails (e.g. document doesn't exist yet, try to merge instead)
                firestore.collection("users").document(userId).set(mapOf("adStatsSummary" to summaryData), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        ApplicationLogManager.i(TAG, "Successfully merged adStatsSummary to user profile document: $userId")
                    }
                    .addOnFailureListener { err ->
                        ApplicationLogManager.e(TAG, "Failed to merge adStatsSummary within user document: ${err.message}", err)
                    }
            }

        // 3. User→select user by clicking on email (DEPRECATED - Subcollection ad_stats removed per user's requests to avoid duplicate views and nested complexity)
    }

    /**
     * Complete dual-sync to upload all local records to the new highly structured Firestore hierarchy.
     */
    fun syncLocalDataToCloud(context: Context, email: String, scope: CoroutineScope, onComplete: (Boolean) -> Unit = {}) {
        val firestore = getFirestore() ?: run {
            onComplete(false)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.database.AppDatabase.getDatabase(context)
                
                // 1. Fetch local user profile and sync
                val localUser = db.userDao().getUserByEmail(email)
                if (localUser != null) {
                    syncUserProfile(localUser)
                }

                // 2. Fetch all local coin histories, deduplicate, and sync to logs_activity
                val coinHistories = db.coinHistoryDao().getCoinHistory().first()
                val seenKeys = mutableSetOf<String>()
                for (history in coinHistories) {
                    val isOneTime = history.title.contains("Welcome", ignoreCase = true) || 
                                    history.title.contains("Sign-Up", ignoreCase = true) || 
                                    history.title.contains("Signup", ignoreCase = true)
                    val key = if (isOneTime) {
                        history.title.lowercase()
                    } else {
                        "${history.title.lowercase()}_${history.amount}_${history.timestamp / 5000}" // 5-second bucket
                    }
                    
                    if (seenKeys.contains(key)) {
                        db.coinHistoryDao().deleteCoinHistoryById(history.id)
                        val docId = "COIN_HIST_${history.id}_${history.timestamp}"
                        firestore.collection("users").document(email).collection("coin_histories").document(docId).delete()
                        firestore.collection("users").document(email).collection("logs_activity").document(docId).delete()
                    } else {
                        seenKeys.add(key)
                        syncCoinHistory(email, history)
                    }
                }

                // Proactively clean up any duplicate records or "COIN_HIST_0_..." leftover records in Firestore
                try {
                    val coinHistDocs = firestore.collection("users").document(email).collection("coin_histories").get().await()
                    val seenCloudKeys = mutableSetOf<String>()
                    for (doc in coinHistDocs.documents) {
                        val docId = doc.id
                        val title = doc.getString("title") ?: ""
                        val amount = doc.getLong("amount")?.toInt() ?: 0
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val docIdValue = doc.getLong("id")?.toInt() ?: -1
                        
                        val isOneTime = title.contains("Welcome", ignoreCase = true) || 
                                        title.contains("Sign-Up", ignoreCase = true) || 
                                        title.contains("Signup", ignoreCase = true)
                        val key = if (isOneTime) {
                            title.lowercase()
                        } else {
                            "${title.lowercase()}_${amount}_${timestamp / 5000}"
                        }
                        
                        if (docIdValue <= 0 || docId.startsWith("COIN_HIST_0_") || seenCloudKeys.contains(key)) {
                            firestore.collection("users").document(email).collection("coin_histories").document(docId).delete()
                            firestore.collection("users").document(email).collection("logs_activity").document(docId).delete()
                        } else {
                            seenCloudKeys.add(key)
                        }
                    }
                } catch (e: Exception) {
                    ApplicationLogManager.d(TAG, "Cleanup of stale Firestore coin_histories failed: ${e.message}")
                }

                // 3. Fetch all local watch histories, deduplicate, and sync to logs_activity
                val watchHistories = db.watchHistoryDao().getWatchHistory().first()
                val seenWatchKeys = mutableSetOf<String>()
                for (watch in watchHistories) {
                    val key = "${watch.adId}_${watch.coinsEarned}_${watch.timestamp / 5000}" // 5-second bucket
                    if (seenWatchKeys.contains(key)) {
                        db.watchHistoryDao().deleteWatchHistoryById(watch.id)
                        val docId = "WATCH_HIST_${watch.id}_${watch.timestamp}"
                        firestore.collection("users").document(email).collection("watch_histories").document(docId).delete()
                        firestore.collection("users").document(email).collection("logs_activity").document(docId).delete()
                    } else {
                        seenWatchKeys.add(key)
                        syncWatchHistory(email, watch)
                    }
                }

                // Proactively clean up any duplicate records or "WATCH_HIST_0_..." leftover records in Firestore
                try {
                    val watchHistDocs = firestore.collection("users").document(email).collection("watch_histories").get().await()
                    val seenCloudWatchKeys = mutableSetOf<String>()
                    for (doc in watchHistDocs.documents) {
                        val docId = doc.id
                        val adId = doc.getString("adId") ?: ""
                        val coinsEarned = doc.getLong("coinsEarned")?.toInt() ?: 0
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val docIdValue = doc.getLong("id")?.toInt() ?: -1
                        
                        val key = "${adId}_${coinsEarned}_${timestamp / 5000}"
                        if (docIdValue <= 0 || docId.startsWith("WATCH_HIST_0_") || seenCloudWatchKeys.contains(key)) {
                            firestore.collection("users").document(email).collection("watch_histories").document(docId).delete()
                            firestore.collection("users").document(email).collection("logs_activity").document(docId).delete()
                        } else {
                            seenCloudWatchKeys.add(key)
                        }
                    }
                } catch (e: Exception) {
                    ApplicationLogManager.d(TAG, "Cleanup of stale Firestore watch_histories failed: ${e.message}")
                }

                // 4. Fetch all local wallet activities and sync to logs_activity
                val walletActivities = db.walletActivityDao().getActivityFlow(email).first()
                for (activity in walletActivities) {
                    syncWalletActivity(activity)
                }

                // 5. Fetch all local reward histories and sync to logs_activity
                val rewards = db.rewardHistoryDao().getRewardsFlow(email).first()
                for (reward in rewards) {
                    syncRewardHistory(reward)
                }

                // 6. Fetch all local UPI withdrawals and sync
                val withdrawals = db.withdrawalHistoryDao().getWithdrawalFlow(email).first()
                for (withdrawal in withdrawals) {
                    // Sync individual withdrawal directly to logs_activity without showing Toast
                    createUpiRequest(withdrawal, showToast = false)
                }

                // 7. Fetch all local Voucher redemptions and sync
                val redemptions = db.walletRedeemHistoryDao().getRedeemFlow(email).first()
                for (redeem in redemptions) {
                    // Sync individual voucher redemption directly to logs_activity without showing Toast
                    createVoucherRequest(redeem, showToast = false)
                }

                // 8. Proactively delete any leftover ad_stats subcollection documents in Firestore to completely clean up per user's request
                try {
                    val adStatsDocs = firestore.collection("users").document(email).collection("ad_stats").get().await()
                    for (doc in adStatsDocs.documents) {
                        firestore.collection("users").document(email).collection("ad_stats").document(doc.id).delete()
                    }
                } catch (e: Exception) {
                    ApplicationLogManager.d(TAG, "Cleanup of legacy subcollection ad_stats failed: ${e.message}")
                }

                onComplete(true)
            } catch (e: Exception) {
                ApplicationLogManager.e(TAG, "Failed to sync local data to cloud: ${e.message}", e)
                onComplete(false)
            }
        }
    }
}
