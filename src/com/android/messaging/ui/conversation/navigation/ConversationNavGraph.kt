package com.android.messaging.ui.conversation.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.addparticipants.AddParticipantsScreen
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.conversation.entry.ConversationEntryViewModel
import com.android.messaging.ui.conversation.entry.NewChatScreen
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import com.android.messaging.ui.conversation.messagedetails.MessageDetailsScreen
import com.android.messaging.ui.conversation.recipientpicker.RecipientPickerScreen
import com.android.messaging.ui.conversation.screen.ConversationScreen

@Composable
internal fun ConversationNavGraph(
    launchRequest: ConversationEntryLaunchRequest?,
    modifier: Modifier = Modifier,
    onConversationDetailsClick: (String) -> Unit = {},
    onFinish: () -> Unit,
    entryModel: ConversationEntryScreenModel = hiltViewModel<ConversationEntryViewModel>(),
    navigationReducer: ConversationNavigationReducer = defaultConversationNavReducer,
) {
    val entryUiState by entryModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(initialNavKey(launchRequest))
    val routeState = ConversationNavRouteState(
        backStack = backStack,
        entryModel = rememberUpdatedState(newValue = entryModel),
        entryUiState = rememberUpdatedState(newValue = entryUiState),
        isLaunchedFromBubble = rememberUpdatedState(
            newValue = launchRequest?.isLaunchedFromBubble == true,
        ),
        navigationReducer = rememberUpdatedState(newValue = navigationReducer),
        onConversationDetailsClick = rememberUpdatedState(
            newValue = onConversationDetailsClick,
        ),
        onFinish = rememberUpdatedState(newValue = onFinish),
    )
    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )
    val entryProvider = remember(backStack) {
        conversationNavEntryProvider(routeState = routeState)
    }

    ConversationNavGraphEffects(
        launchRequest = launchRequest,
        onLaunchRequest = entryModel::onLaunchRequest,
        onLaunchBackStackUpdate = { currentLaunchRequest ->
            updateBackStackForLaunch(
                backStack = backStack,
                launchRequest = currentLaunchRequest,
                navigationReducer = navigationReducer,
            )
        },
    )

    NavDisplay(
        backStack = backStack,
        modifier = modifier.background(color = MaterialTheme.colorScheme.background),
        onBack = {
            handleNavBack(
                backStack = backStack,
                navigationReducer = navigationReducer,
                onFinish = onFinish,
            )
        },
        entryDecorators = entryDecorators,
        entryProvider = entryProvider,
    )
}

private fun conversationNavEntryProvider(
    routeState: ConversationNavRouteState,
): (NavKey) -> NavEntry<NavKey> {
    return entryProvider {
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
            onConversationDetailsClick = {
                routeState.onConversationDetailsClick.value(conversationId)
            },
            onNavigateToMessageDetails = { messageId ->
                navigationReducer.navigateToMessageDetails(
                    backStack = routeState.backStack,
                    conversationId = conversationId,
                    messageId = messageId,
                )
            },
            onNavigateBack = {
                popBackStackOrFinish(
                    backStack = routeState.backStack,
                    navigationReducer = navigationReducer,
                    onFinish = routeState.onFinish.value,
                )
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

private fun newChatRouteContent(
    routeState: ConversationNavRouteState,
): @Composable (NewChatNavKey) -> Unit {
    return {
        val entryModel = routeState.entryModel.value

        NewChatScreen(
            onNavigateBack = {
                popBackStackOrFinish(
                    backStack = routeState.backStack,
                    navigationReducer = routeState.navigationReducer.value,
                    onFinish = routeState.onFinish.value,
                )
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
                popBackStackOrFinish(
                    backStack = routeState.backStack,
                    navigationReducer = routeState.navigationReducer.value,
                    onFinish = routeState.onFinish.value,
                )
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
        MessageDetailsScreen(
            conversationId = navKey.conversationId,
            messageId = navKey.messageId,
            onNavigateBack = {
                popBackStackOrFinish(
                    backStack = routeState.backStack,
                    navigationReducer = routeState.navigationReducer.value,
                    onFinish = routeState.onFinish.value,
                )
            },
        )
    }
}

@Composable
private fun ConversationNavGraphEffects(
    launchRequest: ConversationEntryLaunchRequest?,
    onLaunchRequest: (ConversationEntryLaunchRequest) -> Unit,
    onLaunchBackStackUpdate: (ConversationEntryLaunchRequest?) -> Unit,
) {
    val latestOnLaunchRequest = rememberUpdatedState(newValue = onLaunchRequest)
    val latestOnLaunchBackStackUpdate = rememberUpdatedState(
        newValue = onLaunchBackStackUpdate,
    )

    LaunchedEffect(launchRequest) {
        launchRequest?.let(latestOnLaunchRequest.value)
        latestOnLaunchBackStackUpdate.value(launchRequest)
    }
}

private fun initialNavKey(launchRequest: ConversationEntryLaunchRequest?): NavKey {
    return launchRequest
        ?.conversationId
        ?.let(::ConversationNavKey)
        ?: NewChatNavKey
}

private fun updateBackStackForLaunch(
    backStack: MutableList<NavKey>,
    launchRequest: ConversationEntryLaunchRequest?,
    navigationReducer: ConversationNavigationReducer,
) {
    val destination = initialNavKey(launchRequest = launchRequest)
    navigationReducer.resetBackStack(
        backStack = backStack,
        destination = destination,
    )
}

private fun popBackStackOrFinish(
    backStack: MutableList<NavKey>,
    navigationReducer: ConversationNavigationReducer,
    onFinish: () -> Unit,
) {
    if (navigationReducer.popBackStack(backStack = backStack)) {
        return
    }

    onFinish()
}

private fun handleNavBack(
    backStack: MutableList<NavKey>,
    navigationReducer: ConversationNavigationReducer,
    onFinish: () -> Unit,
) {
    popBackStackOrFinish(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
}

private fun pendingLaunchPayloadForConversation(
    entryUiState: ConversationEntryUiState,
    conversationId: String,
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

private class ConversationNavRouteState(
    val backStack: MutableList<NavKey>,
    val entryModel: State<ConversationEntryScreenModel>,
    val entryUiState: State<ConversationEntryUiState>,
    val isLaunchedFromBubble: State<Boolean>,
    val navigationReducer: State<ConversationNavigationReducer>,
    val onConversationDetailsClick: State<(String) -> Unit>,
    val onFinish: State<() -> Unit>,
)

private data class ConversationPendingLaunchPayload(
    val draft: ConversationDraft? = null,
    val scrollPosition: Int? = null,
    val selfParticipantId: String? = null,
    val startupAttachment: ConversationEntryStartupAttachment? = null,
)

private val defaultConversationNavReducer: ConversationNavigationReducer =
    ConversationNavigationReducerImpl()
