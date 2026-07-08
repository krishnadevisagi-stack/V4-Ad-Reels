package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUserProfile(): UserProfile?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserProfile?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: UserProfile)

    @Query("UPDATE users SET coins = :coins, walletBalance = :balance WHERE isLoggedIn = 1")
    suspend fun updateCoins(coins: Int, balance: Double)

    @Query("UPDATE users SET selectedCategories = :categories WHERE isLoggedIn = 1")
    suspend fun updateCategories(categories: String)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    @Query("DELETE FROM users")
    suspend fun clearUser()
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_histories ORDER BY timestamp DESC")
    fun getWatchHistory(): Flow<List<WatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistory)

    @Query("DELETE FROM watch_histories WHERE id = :id")
    suspend fun deleteWatchHistoryById(id: Int)

    @Query("DELETE FROM watch_histories")
    suspend fun clearWatchHistory()
}

@Dao
interface SavedAdDao {
    @Query("SELECT * FROM saved_ads ORDER BY timestamp DESC")
    fun getSavedAds(): Flow<List<SavedAd>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAd(ad: SavedAd)

    @Query("DELETE FROM saved_ads WHERE adId = :adId")
    suspend fun deleteSavedAd(adId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_ads WHERE adId = :adId)")
    suspend fun isAdSaved(adId: String): Boolean
}

@Dao
interface RedeemHistoryDao {
    @Query("SELECT * FROM redeem_histories ORDER BY timestamp DESC")
    fun getRedeemHistory(): Flow<List<RedeemHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedeemHistory(redeem: RedeemHistory)

    @Query("DELETE FROM redeem_histories")
    suspend fun clearRedeemHistory()
}

@Dao
interface CoinHistoryDao {
    @Query("SELECT * FROM coin_histories ORDER BY timestamp DESC")
    fun getCoinHistory(): Flow<List<CoinHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoinHistory(history: CoinHistory): Long

    @Query("DELETE FROM coin_histories WHERE id = :id")
    suspend fun deleteCoinHistoryById(id: Int)

    @Query("DELETE FROM coin_histories")
    suspend fun clearCoinHistory()
}

@Dao
interface AdStatsDao {
    @Query("SELECT * FROM ad_stats_entries ORDER BY timestamp DESC")
    fun getAdStats(): Flow<List<AdStatsEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdStat(entry: AdStatsEntry): Long

    @Query("DELETE FROM ad_stats_entries")
    suspend fun clearAdStats()
}
