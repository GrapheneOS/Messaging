package com.android.messaging.ui.conversationsettings

import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsEffectHandlerImpl
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConversationSettingsActivity : ComponentActivity() {

    @Inject
    internal lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val hostView = LocalView.current
                val effectHandler = remember(hostView) {
                    ConversationSettingsEffectHandlerImpl(
                        activity = this,
                        hostView = hostView,
                        clipboardManager = clipboardManager,
                    )
                }

                ConversationSettingsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = { code ->
                        code?.let(::setResult)
                        finish()
                    },
                )
            }
        }
    }
}
