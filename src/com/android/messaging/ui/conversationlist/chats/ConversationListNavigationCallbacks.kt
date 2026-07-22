package com.android.messaging.ui.conversationlist.chats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.chats.model.ConversationListNavEvent as NavEvent
import kotlinx.coroutines.flow.Flow

@Stable
internal class ConversationListNavigationCallbacks(
    val onNavigateToConversation: (ConversationId) -> Unit,
    val onNavigateToNewChat: () -> Unit,
    val onNavigateToConversationSettings: (ConversationId) -> Unit,
    val onNavigateToArchivedConversations: () -> Unit,
    val onNavigateToBlockedParticipants: () -> Unit,
    val onNavigateToSettings: () -> Unit,
)

@Composable
internal fun ConversationListNavEvents(
    navigationEvents: Flow<NavEvent>,
    navigation: ConversationListNavigationCallbacks,
) {
    val currentNavigation by rememberUpdatedState(navigation)

    LaunchedEffect(navigationEvents) {
        navigationEvents.collect { event ->
            with(currentNavigation) {
                when (event) {
                    NavEvent.OpenNewChat -> onNavigateToNewChat()
                    NavEvent.OpenArchivedConversations -> onNavigateToArchivedConversations()
                    NavEvent.OpenBlockedParticipants -> onNavigateToBlockedParticipants()
                    NavEvent.OpenSettings -> onNavigateToSettings()

                    is NavEvent.OpenConversation -> {
                        onNavigateToConversation(event.conversationId)
                    }

                    is NavEvent.OpenConversationSettings -> {
                        onNavigateToConversationSettings(event.conversationId)
                    }
                }
            }
        }
    }
}
