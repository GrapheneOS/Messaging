package com.android.messaging.ui.conversationpicker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationpicker.host.forward.ForwardMessageScreen
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner
import com.android.messaging.ui.navigation.paneTitleMetadata

internal fun EntryProviderScope<NavKey>.forwardMessageEntries() {
    entry<ForwardMessageNavKey>(
        metadata = paneTitleMetadata(R.string.forward_message_activity_title),
        content = forwardMessageRouteContent(),
    )
}

private fun forwardMessageRouteContent(): @Composable (ForwardMessageNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current
        val defaultArgs = remember(navKey) {
            forwardMessageDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            ForwardMessageScreen(
                onOpenConversation = { conversationId ->
                    navigator.reset(
                        destinations = listOf(
                            ConversationListNavKey,
                            ConversationNavKey(conversationId = conversationId),
                        ),
                    )
                },
                onClose = {
                    navigator.reset(
                        destinations = listOf(ConversationListNavKey),
                    )
                },
                onNavigateBack = navigator::back,
            )
        }
    }
}
