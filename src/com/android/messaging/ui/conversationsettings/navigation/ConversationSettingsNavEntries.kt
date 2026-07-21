package com.android.messaging.ui.conversationsettings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreen
import com.android.messaging.ui.conversationsettings.screen.rememberConversationSettingsEffectHandler
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner

internal fun EntryProviderScope<NavKey>.conversationSettingsEntries() {
    entry<ConversationSettingsNavKey>(
        content = conversationSettingsRouteContent(),
    )
}

private fun conversationSettingsRouteContent(): @Composable (ConversationSettingsNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current
        val conversationNavigator = rememberConversationNavigator()
        val effectHandler = rememberConversationSettingsEffectHandler()
        val defaultArgs = remember(navKey) {
            conversationSettingsDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            ConversationSettingsScreen(
                effectHandler = effectHandler,
                onNavigateBack = navigator::back,
                onCloseAfterArchive = {
                    navigator.closeConversation(conversationId = navKey.conversationId)
                },
                onNavigateToConversation = { conversationId ->
                    conversationNavigator.navigateToConversation(conversationId = conversationId)
                },
            )
        }
    }
}
