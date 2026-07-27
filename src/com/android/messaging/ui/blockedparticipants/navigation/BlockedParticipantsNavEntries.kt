package com.android.messaging.ui.blockedparticipants.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreen
import com.android.messaging.ui.blockedparticipants.screen.rememberBlockedParticipantsEffectHandler
import com.android.messaging.ui.contact.navigation.navigateToAddContact
import com.android.messaging.ui.conversation.navigation.rememberConversationNavigator
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.paneTitleMetadata

internal fun EntryProviderScope<NavKey>.blockedParticipantsEntries() {
    entry<BlockedParticipantsNavKey>(
        metadata = paneTitleMetadata(R.string.blocked_contacts_title),
        content = blockedParticipantsRouteContent(),
    )
}

private fun blockedParticipantsRouteContent(): @Composable (BlockedParticipantsNavKey) -> Unit {
    return {
        val conversationNavigator = rememberConversationNavigator()
        val navigator = LocalNavigator.current
        val effectHandler = rememberBlockedParticipantsEffectHandler(
            onNavigateToAddContact = { request ->
                navigator.navigateToAddContact(request = request)
            },
        )

        BlockedParticipantsScreen(
            effectHandler = effectHandler,
            onNavigateBack = navigator::back,
            onNavigateToConversation = { conversationId ->
                conversationNavigator.replaceWithConversation(conversationId = conversationId)
            },
        )
    }
}
