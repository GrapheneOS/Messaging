package com.android.messaging.ui.appsettings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.appsettings.screen.SettingsScreen
import com.android.messaging.ui.appsettings.screen.rememberSettingsEffectHandler
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                SettingsScreen(
                    effectHandler = rememberSettingsEffectHandler(),
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
