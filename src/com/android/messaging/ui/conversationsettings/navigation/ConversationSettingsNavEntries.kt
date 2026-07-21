package com.android.messaging.ui.conversationsettings.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsScreen
import com.android.messaging.ui.conversationsettings.screen.rememberConversationSettingsEffectHandler
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner

internal fun EntryProviderScope<NavKey>.conversationSettingsEntry() {
    entry<ConversationSettingsNavKey>(
        content = conversationSettingsRouteContent(),
    )
}

private fun conversationSettingsRouteContent(): @Composable (ConversationSettingsNavKey) -> Unit {
    return { navKey ->
        val activity = checkNotNull(LocalActivity.current)
        val hostView = LocalView.current
        val navigator = rememberConversationNavigator()
        val effectHandler = rememberConversationSettingsEffectHandler(
            activity = activity,
            hostView = hostView,
        )
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
            )
        }
    }
}
