package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserProfile::class,
        WatchHistory::class,
        SavedAd::class,
        RedeemHistory::class,
        CoinHistory::class,
        ImpressionCache::class,
        FeedAnalytics::class,
        FeedEventLog::class,
        FeedConfig::class,
        ReelsRewardHistory::class,
        ReelsAnalytics::class,
        ReelsEventLog::class,
        ReelsConfig::class,
        WalletEntity::class,
        RewardHistoryEntity::class,
        RedeemHistoryEntity::class,
        WithdrawalHistoryEntity::class,
        WalletActivityEntity::class,
        RewardConfigEntity::class,
        WalletAnalyticsEntity::class,
        AdStatsEntry::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun savedAdDao(): SavedAdDao
    abstract fun redeemHistoryDao(): RedeemHistoryDao
    abstract fun coinHistoryDao(): CoinHistoryDao
    abstract fun impressionCacheDao(): ImpressionCacheDao
    abstract fun feedAnalyticsDao(): FeedAnalyticsDao
    abstract fun feedEventLogDao(): FeedEventLogDao
    abstract fun feedConfigDao(): FeedConfigDao
    abstract fun reelsRewardHistoryDao(): ReelsRewardHistoryDao
    abstract fun reelsAnalyticsDao(): ReelsAnalyticsDao
    abstract fun reelsEventLogDao(): ReelsEventLogDao
    abstract fun reelsConfigDao(): ReelsConfigDao
    abstract fun walletDao(): WalletDao
    abstract fun rewardHistoryDao(): RewardHistoryDao
    abstract fun walletRedeemHistoryDao(): WalletRedeemHistoryDao
    abstract fun withdrawalHistoryDao(): WithdrawalHistoryDao
    abstract fun walletActivityDao(): WalletActivityDao
    abstract fun rewardConfigDao(): RewardConfigDao
    abstract fun walletAnalyticsDao(): WalletAnalyticsDao
    abstract fun adStatsDao(): AdStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adreels_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
