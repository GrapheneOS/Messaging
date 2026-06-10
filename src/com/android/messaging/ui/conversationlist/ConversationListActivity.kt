package com.android.messaging.ui.conversationlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.android.messaging.ui.conversationlist.redesign.ui.ConversationListScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val effectHandler = ConversationListActivityEffectHandler(
            activity = this,
        )

        setContent {
            AppTheme {
                ConversationListScreen(
                    effectHandler = effectHandler,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
