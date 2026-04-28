package com.scaevo.ui.settings

import androidx.lifecycle.ViewModel
import com.scaevo.data.settings.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {
    val darkModeEnabled = settingsRepository.darkModeEnabled
    val includeHomeScreen = settingsRepository.includeHomeScreen

    fun setDarkModeEnabled(enabled: Boolean) {
        settingsRepository.setDarkModeEnabled(enabled)
    }

    fun setIncludeHomeScreen(enabled: Boolean) {
        settingsRepository.setIncludeHomeScreen(enabled)
    }
}
