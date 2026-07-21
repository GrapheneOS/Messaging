package com.android.messaging.ui.conversationlist.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.blockedparticipants.navigation.BlockedParticipantsNavKey
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.conversationlist.archived.ArchivedConversationListEffectHandlerImpl
import com.android.messaging.ui.conversationlist.archived.ArchivedConversationListScreen
import com.android.messaging.ui.conversationlist.chats.ConversationListEffectHandlerImpl
import com.android.messaging.ui.conversationlist.chats.ConversationListNavigation
import com.android.messaging.ui.conversationlist.chats.ConversationListScreen
import com.android.messaging.ui.navigation.LocalNavigator

internal fun EntryProviderScope<NavKey>.conversationListEntries() {
    entry<ConversationListNavKey>(
        content = conversationListRouteContent(),
    )
    entry<ArchivedConversationListNavKey>(
        content = archivedConversationListRouteContent(),
    )
}

private fun conversationListRouteContent(): @Composable (ConversationListNavKey) -> Unit {
    return {
        val activity = checkNotNull(LocalActivity.current)
        val hostView = LocalView.current
        val navigator = rememberConversationNavigator()
        val appNavigator = LocalNavigator.current
        val effectHandler = remember(activity, hostView) {
            ConversationListEffectHandlerImpl(
                activity = activity,
                hostView = hostView,
            )
        }

        val navigation = remember(navigator, appNavigator) {
            ConversationListNavigation(
                onNavigateToConversation = { conversationId ->
                    navigator.navigateToConversation(conversationId = conversationId)
                },
                onNavigateToNewChat = navigator::navigateToNewChat,
                onNavigateToConversationSettings = { conversationId ->
                    navigator.navigateToConversationSettings(conversationId = conversationId)
                },
                onNavigateToArchivedConversations = {
                    appNavigator.push(destination = ArchivedConversationListNavKey)
                },
                onNavigateToBlockedParticipants = {
                    appNavigator.push(destination = BlockedParticipantsNavKey)
                },
            )
        }

        ConversationListScreen(
            effectHandler = effectHandler,
            navigation = navigation,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun archivedConversationListRouteContent():
    @Composable (ArchivedConversationListNavKey) -> Unit {
    return {
        val activity = checkNotNull(LocalActivity.current)
        val hostView = LocalView.current
        val conversationNavigator = rememberConversationNavigator()
        val navigator = LocalNavigator.current
        val effectHandler = remember(activity, hostView) {
            ArchivedConversationListEffectHandlerImpl(
                activity = activity,
                hostView = hostView,
            )
        }

        ArchivedConversationListScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onNavigateToConversation = { conversationId ->
                conversationNavigator.navigateToConversation(conversationId = conversationId)
            },
            onNavigateToConversationSettings = { conversationId ->
                conversationNavigator.navigateToConversationSettings(
                    conversationId = conversationId,
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
