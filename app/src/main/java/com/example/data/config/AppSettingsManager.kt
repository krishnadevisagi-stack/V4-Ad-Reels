package com.example.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _autoPlayOption = MutableStateFlow(prefs.getString("autoplay_option", "Always") ?: "Always")
    val autoPlayOption: StateFlow<String> = _autoPlayOption.asStateFlow()

    private val _autoMuteOption = MutableStateFlow(prefs.getBoolean("automute_option", true))
    val autoMuteOption: StateFlow<Boolean> = _autoMuteOption.asStateFlow()

    private val _pushNotificationsEnabled = MutableStateFlow(prefs.getBoolean("push_notifications", true))
    val pushNotificationsEnabled: StateFlow<Boolean> = _pushNotificationsEnabled.asStateFlow()

    private val _rewardAnimationsEnabled = MutableStateFlow(prefs.getBoolean("reward_animations", true))
    val rewardAnimationsEnabled: StateFlow<Boolean> = _rewardAnimationsEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(prefs.getString("selected_language", "English") ?: "English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setAutoPlayOption(option: String) {
        prefs.edit().putString("autoplay_option", option).apply()
        _autoPlayOption.value = option
    }

    fun setAutoMuteOption(enabled: Boolean) {
        prefs.edit().putBoolean("automute_option", enabled).apply()
        _autoMuteOption.value = enabled
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("push_notifications", enabled).apply()
        _pushNotificationsEnabled.value = enabled
    }

    fun setRewardAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reward_animations", enabled).apply()
        _rewardAnimationsEnabled.value = enabled
    }

    fun setSelectedLanguage(language: String) {
        prefs.edit().putString("selected_language", language).apply()
        _selectedLanguage.value = language
    }

    companion object {
        @Volatile
        private var INSTANCE: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AppSettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
