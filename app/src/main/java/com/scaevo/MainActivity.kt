package com.scaevo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scaevo.ui.nav.AppNavGraph
import com.scaevo.ui.theme.AppTheme
import com.scaevo.data.settings.UserSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userSettingsRepository: UserSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = userSettingsRepository.darkModeEnabled
                .collectAsStateWithLifecycle().value

            AppTheme(darkTheme = darkTheme) {
                AppNavGraph()
            }
        }
    }
}
