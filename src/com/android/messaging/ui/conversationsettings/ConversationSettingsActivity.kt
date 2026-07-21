package com.android.messaging.ui.conversationsettings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreen
import com.android.messaging.ui.conversationsettings.screen.rememberConversationSettingsEffectHandler
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationSettingsActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val effectHandler = rememberConversationSettingsEffectHandler(
                    activity = this,
                    hostView = LocalView.current,
                )

                ConversationSettingsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    onCloseAfterArchive = ::finish,
                )
            }
        }
    }
}
