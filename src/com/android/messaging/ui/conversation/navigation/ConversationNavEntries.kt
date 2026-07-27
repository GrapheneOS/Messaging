package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.contact.navigation.navigateToAddContact
import com.android.messaging.ui.conversation.addparticipants.AddParticipantsScreen
import com.android.messaging.ui.conversation.addparticipants.rememberAddParticipantsEffectHandler
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.conversation.entry.NewChatScreen
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import com.android.messaging.ui.conversation.entry.rememberNewChatEffectHandler
import com.android.messaging.ui.conversation.messagedetails.MessageDetailsScreen
import com.android.messaging.ui.conversation.screen.ConversationScreen
import com.android.messaging.ui.conversation.screen.model.ConversationPendingLaunchPayload
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner

internal fun EntryProviderScope<NavKey>.conversationEntries() {
    entry<ConversationNavKey>(
        content = conversationScreenRouteContent(),
    )
    entry<NewChatNavKey>(
        content = newChatRouteContent(),
    )
    entry<AddParticipantsNavKey>(
        content = addParticipantsRouteContent(),
    )
    entry<MessageDetailsNavKey>(
        content = messageDetailsRouteContent(),
    )
}

private fun conversationScreenRouteContent(): @Composable (ConversationNavKey) -> Unit {
    return { navKey ->
        val entryNavState = LocalConversationEntryNavState.current
        val entryUiState by entryNavState.model.uiState.collectAsStateWithLifecycle()

        ConversationRoute(
            conversationId = navKey.conversationId,
            isLaunchedFromBubble = entryNavState.isLaunchedFromBubble,
            entryModel = entryNavState.model,
            entryUiState = entryUiState,
        )
    }
}

@Composable
private fun ConversationRoute(
    conversationId: ConversationId,
    isLaunchedFromBubble: Boolean,
    entryModel: ConversationEntryScreenModel,
    entryUiState: ConversationEntryUiState,
) {
    val navigator = rememberConversationNavigator()
    val appNavigator = LocalNavigator.current
    val pendingPayload = pendingLaunchPayloadForConversation(
        entryUiState = entryUiState,
        conversationId = conversationId,
    )

    ConversationScreen(
        conversationId = conversationId,
        cancelIncomingNotification = !isLaunchedFromBubble,
        onAddPeopleClick = {
            navigator.navigateToAddParticipants(conversationId = conversationId)
        },
        onConversationDetailsClick = {
            navigator.navigateToConversationSettings(conversationId = conversationId)
        },
        onNavigateToMessageDetails = { messageId ->
            navigator.navigateToMessageDetails(
                conversationId = conversationId,
                messageId = messageId,
            )
        },
        onNavigateToVCardDetail = { uri ->
            navigator.navigateToVCardDetail(uri = uri)
        },
        onNavigateToPhotoViewer = { launchRequest ->
            navigator.navigateToPhotoViewer(
                conversationId = conversationId,
                launchRequest = launchRequest,
            )
        },
        onNavigateToAddContact = { request ->
            appNavigator.navigateToAddContact(request = request)
        },
        onNavigateToForward = { messageId ->
            navigator.navigateToForward(
                conversationId = conversationId,
                messageId = messageId,
            )
        },
        onNavigateBack = appNavigator::back,
        pendingLaunchPayload = pendingPayload,
        onPendingDraftConsumed = {
            entryModel.onDraftPayloadConsumed(conversationId = conversationId)
        },
        onPendingScrollPositionConsumed = {
            entryModel.onScrollPositionConsumed(conversationId = conversationId)
        },
        onPendingSelfParticipantIdConsumed = {
            entryModel.onPendingSelfParticipantIdConsumed(conversationId = conversationId)
        },
        onPendingStartupAttachmentConsumed = {
            entryModel.onStartupAttachmentConsumed(conversationId = conversationId)
        },
    )
}

private fun newChatRouteContent(): @Composable (NewChatNavKey) -> Unit {
    return {
        val entryModel = LocalConversationEntryNavState.current.model
        val navigator = rememberConversationNavigator()
        val appNavigator = LocalNavigator.current

        NewChatScreen(
            effectHandler = rememberNewChatEffectHandler(),
            onNavigateBack = appNavigator::back,
            onNavigateToConversation = { conversationId, selfParticipantId ->
                entryModel.onConversationNavigationRequested(
                    conversationId = conversationId,
                    pendingSelfParticipantId = selfParticipantId,
                )
                navigator.navigateToConversation(conversationId = conversationId)
            },
        )
    }
}

private fun addParticipantsRouteContent(): @Composable (AddParticipantsNavKey) -> Unit {
    return { navKey ->
        val navigator = rememberConversationNavigator()
        val appNavigator = LocalNavigator.current

        AddParticipantsScreen(
            effectHandler = rememberAddParticipantsEffectHandler(),
            conversationId = navKey.conversationId,
            onNavigateBack = appNavigator::back,
            onNavigateToConversation = { resolvedConversationId ->
                navigator.replaceCurrentConversation(conversationId = resolvedConversationId)
            },
        )
    }
}

private fun messageDetailsRouteContent(): @Composable (MessageDetailsNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current
        val defaultArgs = remember(navKey) {
            messageDetailsDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            MessageDetailsScreen(
                onNavigateBack = navigator::back,
            )
        }
    }
}

private fun pendingLaunchPayloadForConversation(
    entryUiState: ConversationEntryUiState,
    conversationId: ConversationId,
): ConversationPendingLaunchPayload {
    if (entryUiState.conversationId != conversationId) {
        return ConversationPendingLaunchPayload()
    }

    return ConversationPendingLaunchPayload(
        draft = entryUiState.pendingDraft,
        scrollPosition = entryUiState.pendingScrollPosition,
        selfParticipantId = entryUiState.pendingSelfParticipantId,
        startupAttachment = entryUiState.pendingStartupAttachment,
    )
}
