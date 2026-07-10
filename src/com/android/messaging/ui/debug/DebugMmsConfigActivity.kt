package com.android.messaging.ui.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.debug.screen.DebugMmsConfigScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugMmsConfigActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                DebugMmsConfigScreen(onNavigateBack = ::finish)
            }
        }
    }
}
