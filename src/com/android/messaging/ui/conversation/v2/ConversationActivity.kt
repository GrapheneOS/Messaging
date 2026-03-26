package com.android.messaging.ui.conversation.v2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.v2.screen.ConversationScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ConversationActivity : ComponentActivity() {

    private var conversationId: String? by mutableStateOf(value = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        conversationId = extractConversationId(intent = intent)

        enableEdgeToEdge()

        setContent {
            AppTheme {
                ConversationScreen(
                    conversationId = conversationId,
                    onNavigateBack = ::finish,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        conversationId = extractConversationId(intent = intent)
    }

    private fun extractConversationId(intent: Intent?): String? {
        return intent?.getStringExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID)
    }
}
