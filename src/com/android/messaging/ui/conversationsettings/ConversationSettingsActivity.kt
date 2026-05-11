package com.android.messaging.ui.conversationsettings

import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsEffectHandlerImpl
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConversationSettingsActivity : ComponentActivity() {

    @Inject
    lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val effectHandler = ConversationSettingsEffectHandlerImpl(
            activity = this,
            clipboardManager = clipboardManager,
        )

        setContent {
            AppTheme {
                ConversationSettingsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
