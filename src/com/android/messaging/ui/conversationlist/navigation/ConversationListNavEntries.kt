package com.android.messaging.ui.conversationlist.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.conversationlist.chats.ConversationListEffectHandlerImpl
import com.android.messaging.ui.conversationlist.chats.ConversationListScreen

internal fun EntryProviderScope<NavKey>.conversationListEntry() {
    entry<ConversationListNavKey>(
        content = conversationListRouteContent(),
    )
}

private fun conversationListRouteContent(): @Composable (ConversationListNavKey) -> Unit {
    return {
        val activity = checkNotNull(LocalActivity.current)
        val hostView = LocalView.current
        val navigator = rememberConversationNavigator()
        val effectHandler = remember(activity, hostView) {
            ConversationListEffectHandlerImpl(
                activity = activity,
                hostView = hostView,
            )
        }

        ConversationListScreen(
            effectHandler = effectHandler,
            onNavigateToConversation = { conversationId ->
                navigator.navigateToConversation(conversationId = conversationId)
            },
            onNavigateToNewChat = navigator::navigateToNewChat,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
