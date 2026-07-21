package com.android.messaging.ui.blockedparticipants.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsEffectHandlerImpl
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreen
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.navigation.LocalNavigator

internal fun EntryProviderScope<NavKey>.blockedParticipantsEntry() {
    entry<BlockedParticipantsNavKey>(
        content = blockedParticipantsRouteContent(),
    )
}

private fun blockedParticipantsRouteContent(): @Composable (BlockedParticipantsNavKey) -> Unit {
    return {
        val activity = checkNotNull(LocalActivity.current)
        val hostView = LocalView.current
        val conversationNavigator = rememberConversationNavigator()
        val navigator = LocalNavigator.current
        val effectHandler = remember(activity, hostView) {
            BlockedParticipantsEffectHandlerImpl(
                activity = activity,
                hostView = hostView,
            )
        }

        BlockedParticipantsScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onNavigateToConversation = { conversationId ->
                conversationNavigator.replaceWithConversation(conversationId = conversationId)
            },
        )
    }
}
