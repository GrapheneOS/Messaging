package com.android.messaging.ui.conversationlist.chats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.chats.model.ConversationListNavEvent as NavEvent
import com.android.messaging.ui.core.CollectEvents
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
    CollectEvents(events = navigationEvents) { event ->
        with(navigation) {
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
