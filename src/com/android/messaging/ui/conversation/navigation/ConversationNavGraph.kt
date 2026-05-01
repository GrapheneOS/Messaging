package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import com.android.messaging.ui.conversation.entry.model.ConversationEntryEffect
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import com.android.messaging.ui.conversation.recipientpicker.RecipientPickerScreen
import com.android.messaging.ui.conversation.screen.ConversationScreen
import com.android.messaging.util.UiUtils

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
    val effectState = remember(backStack, entryModel) {
        conversationNavEffectState(routeState = routeState, entryModel = entryModel)
    }

    ConversationNavGraphEffects(
        launchRequest = launchRequest,
        effectState = effectState,
    )

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            handleNavBack(
                backStack = backStack,
                entryModel = entryModel,
                entryUiState = entryUiState,
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
            onNavigateBack = {
                popBackStackOrFinish(
                    backStack = routeState.backStack,
                    navigationReducer = navigationReducer,
                    onFinish = routeState.onFinish.value,
                )
            },
            pendingDraft = pendingPayload.draft,
            pendingScrollPosition = pendingPayload.scrollPosition,
            pendingStartupAttachment = pendingPayload.startupAttachment,
            onPendingDraftConsumed = {
                entryModel.onDraftPayloadConsumed(conversationId = conversationId)
            },
            onPendingScrollPositionConsumed = {
                entryModel.onScrollPositionConsumed(conversationId = conversationId)
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
        val entryUiState = routeState.entryUiState.value

        NewChatScreen(
            isCreatingGroup = entryUiState.isCreatingGroup,
            isResolvingConversation = entryUiState.isResolvingConversation,
            isResolvingConversationIndicatorVisible = entryUiState
                .isResolvingConversationIndicatorVisible,
            onContactClick = entryModel::onNewChatRecipientSelected,
            onContactLongClick = entryModel::onNewChatRecipientLongPressed,
            onCreateGroupClick = entryModel::onCreateGroupRequested,
            onCreateGroupConfirmed = entryModel::onCreateGroupConfirmed,
            onCreateGroupRecipientClick = entryModel::onCreateGroupRecipientClicked,
            onNavigateBack = {
                handleNewChatBack(
                    entryModel = entryModel,
                    entryUiState = entryUiState,
                    backStack = routeState.backStack,
                    navigationReducer = routeState.navigationReducer.value,
                    onFinish = routeState.onFinish.value,
                )
            },
            resolvingRecipientDestination = entryUiState.resolvingRecipientDestination,
            selectedGroupRecipientDestinations = entryUiState.selectedGroupRecipientDestinations,
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

@Composable
private fun ConversationNavGraphEffects(
    launchRequest: ConversationEntryLaunchRequest?,
    effectState: ConversationNavEffectState,
) {
    val latestEffectState = rememberUpdatedState(newValue = effectState)

    LaunchedEffect(launchRequest) {
        launchRequest?.let(latestEffectState.value.onLaunchRequest)
        latestEffectState.value.onLaunchBackStackUpdate(launchRequest)
    }

    LaunchedEffect(effectState.collectEntryEffects) {
        effectState.collectEntryEffects { effect ->
            latestEffectState.value.onEntryEffect(effect)
        }
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
    entryModel: ConversationEntryScreenModel,
    entryUiState: ConversationEntryUiState,
    navigationReducer: ConversationNavigationReducer,
    onFinish: () -> Unit,
) {
    if (backStack.lastOrNull() == NewChatNavKey && entryUiState.isCreatingGroup) {
        entryModel.onCreateGroupCanceled()
        return
    }

    popBackStackOrFinish(
        backStack = backStack,
        navigationReducer = navigationReducer,
        onFinish = onFinish,
    )
}

private fun handleNewChatBack(
    entryModel: ConversationEntryScreenModel,
    entryUiState: ConversationEntryUiState,
    backStack: MutableList<NavKey>,
    navigationReducer: ConversationNavigationReducer,
    onFinish: () -> Unit,
) {
    if (entryUiState.isCreatingGroup) {
        entryModel.onCreateGroupCanceled()
        return
    }

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

private typealias ConversationEntryEffectCollector =
    suspend ((ConversationEntryEffect) -> Unit) -> Unit

@Immutable
private data class ConversationNavEffectState(
    val onLaunchRequest: (ConversationEntryLaunchRequest) -> Unit,
    val onLaunchBackStackUpdate: (ConversationEntryLaunchRequest?) -> Unit,
    val collectEntryEffects: ConversationEntryEffectCollector,
    val onEntryEffect: (ConversationEntryEffect) -> Unit,
)

private data class ConversationPendingLaunchPayload(
    val draft: ConversationDraft? = null,
    val scrollPosition: Int? = null,
    val startupAttachment: ConversationEntryStartupAttachment? = null,
)

private fun conversationNavEffectState(
    routeState: ConversationNavRouteState,
    entryModel: ConversationEntryScreenModel,
): ConversationNavEffectState {
    return ConversationNavEffectState(
        onLaunchRequest = entryModel::onLaunchRequest,
        onLaunchBackStackUpdate = { launchRequest ->
            updateBackStackForLaunch(
                backStack = routeState.backStack,
                launchRequest = launchRequest,
                navigationReducer = routeState.navigationReducer.value,
            )
        },
        collectEntryEffects = { onEffect ->
            entryModel.effects.collect { effect ->
                onEffect(effect)
            }
        },
        onEntryEffect = { effect ->
            handleEntryEffect(
                backStack = routeState.backStack,
                effect = effect,
                navigationReducer = routeState.navigationReducer.value,
                onFinish = routeState.onFinish.value,
            )
        },
    )
}

private fun handleEntryEffect(
    backStack: MutableList<NavKey>,
    effect: ConversationEntryEffect,
    navigationReducer: ConversationNavigationReducer,
    onFinish: () -> Unit,
) {
    when (effect) {
        is ConversationEntryEffect.NavigateBack -> {
            popBackStackOrFinish(
                backStack = backStack,
                navigationReducer = navigationReducer,
                onFinish = onFinish,
            )
        }

        is ConversationEntryEffect.NavigateToConversation -> {
            navigationReducer.navigateToConversation(
                backStack = backStack,
                conversationId = effect.conversationId,
            )
        }

        is ConversationEntryEffect.NavigateToRecipientPicker -> {
            navigationReducer.navigateToRecipientPicker(
                backStack = backStack,
                mode = effect.mode,
            )
        }

        is ConversationEntryEffect.ShowMessage -> {
            UiUtils.showToastAtBottom(effect.messageResId)
        }
    }
}

private val defaultConversationNavReducer: ConversationNavigationReducer =
    ConversationNavigationReducerImpl()
