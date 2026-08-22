package com.android.messaging.ui.blockedparticipants.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreen
import com.android.messaging.ui.blockedparticipants.screen.rememberBlockedParticipantsEffectHandler
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.navigation.LocalNavigator

internal fun EntryProviderScope<NavKey>.blockedParticipantsEntries() {
    entry<BlockedParticipantsNavKey>(
        content = blockedParticipantsRouteContent(),
    )
}

private fun blockedParticipantsRouteContent(): @Composable (BlockedParticipantsNavKey) -> Unit {
    return {
        val conversationNavigator = rememberConversationNavigator()
        val navigator = LocalNavigator.current
        val effectHandler = rememberBlockedParticipantsEffectHandler()

        BlockedParticipantsScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onNavigateToConversation = { conversationId ->
                conversationNavigator.replaceWithConversation(conversationId = conversationId)
            },
        )
    }
}
