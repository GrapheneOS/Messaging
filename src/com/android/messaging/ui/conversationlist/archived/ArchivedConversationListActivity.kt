package com.android.messaging.ui.conversationlist.archived

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArchivedConversationListActivity : BugleComponentActivity() {

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
                    ArchivedConversationListEffectHandlerImpl(
                        activity = this,
                        hostView = hostView,
                    )
                }

                ArchivedConversationListScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
