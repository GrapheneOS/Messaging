package com.android.messaging.ui.conversationlist.chats

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationListActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val hostView = LocalView.current
                val effectHandler = remember(hostView) {
                    ConversationListEffectHandlerImpl(
                        activity = this,
                        hostView = hostView,
                    )
                }

                ConversationListScreen(
                    effectHandler = effectHandler,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
