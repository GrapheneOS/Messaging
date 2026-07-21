package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.addparticipants.AddParticipantsScreen
import com.android.messaging.ui.conversation.entry.NewChatScreen
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import com.android.messaging.ui.conversation.messagedetails.MessageDetailsScreen
import com.android.messaging.ui.conversation.recipientpicker.RecipientPickerScreen
import com.android.messaging.ui.conversation.screen.ConversationScreen
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
        metadata = messageDetailsTransitionMetadata(),
        content = messageDetailsRouteContent(),
    )
    entry<RecipientPickerNavKey> { navKey ->
        RecipientPickerScreen(mode = navKey.mode)
    }
}

private fun conversationScreenRouteContent(): @Composable (ConversationNavKey) -> Unit {
    return { navKey ->
        val conversationId = navKey.conversationId
        val entryNavState = LocalConversationEntryNavState.current
        val entryModel = entryNavState.model
        val entryUiState by entryModel.uiState.collectAsStateWithLifecycle()
        val navigator = rememberConversationNavigator()
        val pendingPayload = pendingLaunchPayloadForConversation(
            entryUiState = entryUiState,
            conversationId = conversationId,
        )

        ConversationScreen(
            conversationId = conversationId,
            launchGeneration = entryUiState.launchGeneration,
            cancelIncomingNotification = !entryNavState.isLaunchedFromBubble,
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
            onNavigateBack = navigator::back,
            pendingDraft = pendingPayload.draft,
            pendingScrollPosition = pendingPayload.scrollPosition,
            pendingSelfParticipantId = pendingPayload.selfParticipantId,
            pendingStartupAttachment = pendingPayload.startupAttachment,
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
}

private fun newChatRouteContent(): @Composable (NewChatNavKey) -> Unit {
    return {
        val entryModel = LocalConversationEntryNavState.current.model
        val navigator = rememberConversationNavigator()

        NewChatScreen(
            onNavigateBack = navigator::back,
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

        AddParticipantsScreen(
            conversationId = navKey.conversationId,
            onNavigateBack = navigator::back,
            onNavigateToConversation = { resolvedConversationId ->
                navigator.replaceCurrentConversation(conversationId = resolvedConversationId)
            },
        )
    }
}

private fun messageDetailsRouteContent(): @Composable (MessageDetailsNavKey) -> Unit {
    return { navKey ->
        val navigator = rememberConversationNavigator()
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

private data class ConversationPendingLaunchPayload(
    val draft: ConversationDraft? = null,
    val scrollPosition: Int? = null,
    val selfParticipantId: ParticipantId? = null,
    val startupAttachment: ConversationEntryStartupAttachment? = null,
)
