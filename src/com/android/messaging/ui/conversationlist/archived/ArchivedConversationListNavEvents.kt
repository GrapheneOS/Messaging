package com.android.messaging.ui.conversationlist.archived

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListNavEvent as NavEvent
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ArchivedConversationListNavEvents(
    navigationEvents: Flow<NavEvent>,
    onNavigateToConversation: (ConversationId) -> Unit,
    onNavigateToConversationSettings: (ConversationId) -> Unit,
) {
    val currentOnNavigateToConversation by rememberUpdatedState(onNavigateToConversation)
    val currentOnNavigateToConversationSettings by rememberUpdatedState(
        onNavigateToConversationSettings,
    )

    LaunchedEffect(navigationEvents) {
        navigationEvents.collect { event ->
            when (event) {
                is NavEvent.OpenConversation -> {
                    currentOnNavigateToConversation(event.conversationId)
                }

                is NavEvent.OpenConversationSettings -> {
                    currentOnNavigateToConversationSettings(event.conversationId)
                }
            }
        }
    }
}
