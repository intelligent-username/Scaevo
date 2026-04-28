package com.scaevo.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _darkModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val _includeHomeScreen = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_HOME, false))
    val includeHomeScreen: StateFlow<Boolean> = _includeHomeScreen.asStateFlow()

    private val listener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                KEY_DARK_MODE -> _darkModeEnabled.value = sharedPreferences.getBoolean(KEY_DARK_MODE, true)
                KEY_INCLUDE_HOME -> _includeHomeScreen.value = sharedPreferences.getBoolean(KEY_INCLUDE_HOME, false)
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun setIncludeHomeScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_HOME, enabled).apply()
    }

    fun readIncludeHomeScreen(): Boolean = prefs.getBoolean(KEY_INCLUDE_HOME, false)

    companion object {
        const val PREFS_NAME = "scaevo_settings"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_INCLUDE_HOME = "include_home_screen"
    }
}
