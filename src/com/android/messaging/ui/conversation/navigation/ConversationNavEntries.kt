package com.android.messaging.ui.conversation.navigation

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.ConversationActivity
import com.android.messaging.ui.conversation.addparticipants.AddParticipantsScreen
import com.android.messaging.ui.conversation.entry.NewChatScreen
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import com.android.messaging.ui.conversation.messagedetails.MessageDetailsScreen
import com.android.messaging.ui.conversation.recipientpicker.RecipientPickerScreen
import com.android.messaging.ui.conversation.screen.ConversationScreen
import com.android.messaging.ui.conversationsettings.ConversationSettingsActivity
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner

internal fun EntryProviderScope<NavKey>.conversationEntries(
    routeState: ConversationNavRouteState,
) {
    entry<ConversationNavKey>(
        content = conversationScreenRouteContent(routeState = routeState),
    )
    entry<NewChatNavKey>(
        content = newChatRouteContent(routeState = routeState),
    )
    entry<AddParticipantsNavKey>(
        content = addParticipantsRouteContent(routeState = routeState),
    )
    entry<MessageDetailsNavKey>(
        metadata = messageDetailsTransitionMetadata(),
        content = messageDetailsRouteContent(routeState = routeState),
    )
    entry<RecipientPickerNavKey> { navKey ->
        RecipientPickerScreen(mode = navKey.mode)
    }
}

private fun conversationScreenRouteContent(
    routeState: ConversationNavRouteState,
): @Composable (ConversationNavKey) -> Unit {
    return { navKey ->
        val conversationId = navKey.conversationId
        val entryModel = routeState.entryModel.value
        val entryUiState = routeState.entryUiState.value
        val navigationReducer = routeState.navigationReducer.value
        val pendingPayload = pendingLaunchPayloadForConversation(
            entryUiState = entryUiState,
            conversationId = conversationId,
        )
        val openConversationDetails = rememberConversationDetailsLauncher(routeState = routeState)

        ConversationScreen(
            conversationId = conversationId,
            launchGeneration = entryUiState.launchGeneration,
            cancelIncomingNotification = !routeState.isLaunchedFromBubble.value,
            onAddPeopleClick = {
                navigationReducer.navigateToAddParticipants(
                    backStack = routeState.backStack,
                    conversationId = conversationId,
                )
            },
            onConversationDetailsClick = { openConversationDetails(conversationId) },
            onNavigateToMessageDetails = { messageId ->
                navigationReducer.navigateToMessageDetails(
                    backStack = routeState.backStack,
                    conversationId = conversationId,
                    messageId = messageId,
                )
            },
            onNavigateBack = {
                popConversationBackStackOrFinish(routeState = routeState)
            },
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

@Composable
private fun rememberConversationDetailsLauncher(
    routeState: ConversationNavRouteState,
): (ConversationId) -> Unit {
    val activity = checkNotNull(LocalActivity.current)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == ConversationActivity.FINISH_RESULT_CODE) {
            popConversationBackStackOrFinish(routeState = routeState)
        }
    }

    return { conversationId ->
        val intent = Intent(activity, ConversationSettingsActivity::class.java)
        intent.putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, conversationId.value)
        launcher.launch(intent)
    }
}

private fun newChatRouteContent(
    routeState: ConversationNavRouteState,
): @Composable (NewChatNavKey) -> Unit {
    return {
        val entryModel = routeState.entryModel.value

        NewChatScreen(
            onNavigateBack = {
                popConversationBackStackOrFinish(routeState = routeState)
            },
            onNavigateToConversation = { conversationId, selfParticipantId ->
                entryModel.onConversationNavigationRequested(
                    conversationId = conversationId,
                    pendingSelfParticipantId = selfParticipantId,
                )
                routeState.navigationReducer.value.navigateToConversation(
                    backStack = routeState.backStack,
                    conversationId = conversationId,
                )
            },
        )
    }
}

private fun addParticipantsRouteContent(
    routeState: ConversationNavRouteState,
): @Composable (AddParticipantsNavKey) -> Unit {
    return { navKey ->
        AddParticipantsScreen(
            conversationId = navKey.conversationId,
            onNavigateBack = {
                popConversationBackStackOrFinish(routeState = routeState)
            },
            onNavigateToConversation = { resolvedConversationId ->
                routeState.navigationReducer.value.replaceCurrentConversation(
                    backStack = routeState.backStack,
                    conversationId = resolvedConversationId,
                )
            },
        )
    }
}

private fun messageDetailsRouteContent(
    routeState: ConversationNavRouteState,
): @Composable (MessageDetailsNavKey) -> Unit {
    return { navKey ->
        val defaultArgs = remember(navKey) {
            messageDetailsDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            MessageDetailsScreen(
                onNavigateBack = {
                    popConversationBackStackOrFinish(routeState = routeState)
                },
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

private fun popConversationBackStackOrFinish(
    routeState: ConversationNavRouteState,
) {
    if (routeState.navigationReducer.value.popBackStack(backStack = routeState.backStack)) {
        return
    }

    routeState.onFinish.value()
}

private data class ConversationPendingLaunchPayload(
    val draft: ConversationDraft? = null,
    val scrollPosition: Int? = null,
    val selfParticipantId: ParticipantId? = null,
    val startupAttachment: ConversationEntryStartupAttachment? = null,
)
