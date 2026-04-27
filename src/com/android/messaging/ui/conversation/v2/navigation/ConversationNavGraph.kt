package com.android.messaging.ui.conversation.v2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.ui.conversation.v2.addparticipants.AddParticipantsScreen
import com.android.messaging.ui.conversation.v2.entry.ConversationEntryModel
import com.android.messaging.ui.conversation.v2.entry.ConversationEntryViewModel
import com.android.messaging.ui.conversation.v2.entry.NewChatScreen
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryEffect
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryUiState
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerScreen
import com.android.messaging.ui.conversation.v2.screen.ConversationScreen
import com.android.messaging.util.UiUtils

@Composable
internal fun ConversationNavGraph(
    launchRequest: ConversationEntryLaunchRequest?,
    modifier: Modifier = Modifier,
    onConversationDetailsClick: (String) -> Unit = {},
    onFinish: () -> Unit,
    entryModel: ConversationEntryModel = hiltViewModel<ConversationEntryViewModel>(),
    navigationReducer: ConversationNavigationReducer = defaultConversationNavReducer,
) {
    val entryUiState by entryModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(initialNavKey(launchRequest))
    val latestEntryModel = rememberUpdatedState(entryModel)
    val latestEntryUiState = rememberUpdatedState(entryUiState)
    val latestNavigationReducer = rememberUpdatedState(navigationReducer)
    val latestOnConversationDetailsClick = rememberUpdatedState(onConversationDetailsClick)
    val latestOnFinish = rememberUpdatedState(onFinish)
    val latestIsLaunchedFromBubble = rememberUpdatedState(
        launchRequest?.isLaunchedFromBubble == true,
    )

    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator<NavKey>(),
    )

    val entryProvider = remember(
        backStack,
    ) {
        entryProvider {
            entry<ConversationNavKey> { navKey ->
                val currentEntryUiState = latestEntryUiState.value
                val currentEntryModel = latestEntryModel.value
                val currentOnFinish = latestOnFinish.value

                ConversationScreen(
                    conversationId = navKey.conversationId,
                    launchGeneration = currentEntryUiState.launchGeneration,
                    cancelIncomingNotification = !latestIsLaunchedFromBubble.value,
                    onAddPeopleClick = {
                        latestNavigationReducer.value.navigateToAddParticipants(
                            backStack = backStack,
                            conversationId = navKey.conversationId,
                        )
                    },
                    onConversationDetailsClick = {
                        latestOnConversationDetailsClick.value(navKey.conversationId)
                    },
                    onNavigateBack = {
                        popBackStackOrFinish(
                            backStack = backStack,
                            navigationReducer = latestNavigationReducer.value,
                            onFinish = currentOnFinish,
                        )
                    },
                    pendingDraft = pendingDraftForConversation(
                        entryUiState = currentEntryUiState,
                        conversationId = navKey.conversationId,
                    ),
                    pendingScrollPosition = pendingScrollPositionForConversation(
                        entryUiState = currentEntryUiState,
                        conversationId = navKey.conversationId,
                    ),
                    pendingStartupAttachment = pendingStartupAttachmentForConversation(
                        entryUiState = currentEntryUiState,
                        conversationId = navKey.conversationId,
                    ),
                    onPendingDraftConsumed = {
                        currentEntryModel.onDraftPayloadConsumed(
                            conversationId = navKey.conversationId,
                        )
                    },
                    onPendingScrollPositionConsumed = {
                        currentEntryModel.onScrollPositionConsumed(
                            conversationId = navKey.conversationId,
                        )
                    },
                    onPendingStartupAttachmentConsumed = {
                        currentEntryModel.onStartupAttachmentConsumed(
                            conversationId = navKey.conversationId,
                        )
                    },
                )
            }

            entry<NewChatNavKey> {
                val currentEntryUiState = latestEntryUiState.value
                val currentEntryModel = latestEntryModel.value

                NewChatScreen(
                    isCreatingGroup = currentEntryUiState.isCreatingGroup,
                    isResolvingConversation = currentEntryUiState.isResolvingConversation,
                    isResolvingConversationIndicatorVisible = currentEntryUiState
                        .isResolvingConversationIndicatorVisible,
                    onContactClick = currentEntryModel::onNewChatRecipientSelected,
                    onContactLongClick = currentEntryModel::onNewChatRecipientLongPressed,
                    onCreateGroupClick = currentEntryModel::onCreateGroupRequested,
                    onCreateGroupConfirmed = currentEntryModel::onCreateGroupConfirmed,
                    onCreateGroupRecipientClick = currentEntryModel::onCreateGroupRecipientClicked,
                    onNavigateBack = {
                        handleNewChatBack(
                            entryModel = currentEntryModel,
                            entryUiState = currentEntryUiState,
                            backStack = backStack,
                            navigationReducer = latestNavigationReducer.value,
                            onFinish = latestOnFinish.value,
                        )
                    },
                    resolvingRecipientDestination = currentEntryUiState
                        .resolvingRecipientDestination,
                    selectedGroupRecipientDestinations = currentEntryUiState
                        .selectedGroupRecipientDestinations,
                )
            }

            entry<AddParticipantsNavKey> { navKey ->
                AddParticipantsScreen(
                    conversationId = navKey.conversationId,
                    onNavigateBack = {
                        popBackStackOrFinish(
                            backStack = backStack,
                            navigationReducer = latestNavigationReducer.value,
                            onFinish = latestOnFinish.value,
                        )
                    },
                    onNavigateToConversation = { resolvedConversationId ->
                        latestNavigationReducer.value.replaceCurrentConversation(
                            backStack = backStack,
                            conversationId = resolvedConversationId,
                        )
                    },
                )
            }

            entry<RecipientPickerNavKey> { navKey ->
                RecipientPickerScreen(mode = navKey.mode)
            }
        }
    }

    LaunchedEffect(launchRequest) {
        launchRequest?.let(entryModel::onLaunchRequest)
        updateBackStackForLaunch(
            backStack = backStack,
            launchRequest = launchRequest,
            navigationReducer = latestNavigationReducer.value,
        )
    }

    LaunchedEffect(entryModel, onFinish) {
        entryModel.effects.collect { effect ->
            handleEntryEffect(
                backStack = backStack,
                effect = effect,
                navigationReducer = latestNavigationReducer.value,
                onFinish = onFinish,
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            handleNavBack(
                backStack = backStack,
                entryModel = latestEntryModel.value,
                entryUiState = latestEntryUiState.value,
                navigationReducer = latestNavigationReducer.value,
                onFinish = latestOnFinish.value,
            )
        },
        entryDecorators = entryDecorators,
        entryProvider = entryProvider,
    )
}

private fun initialNavKey(launchRequest: ConversationEntryLaunchRequest?): NavKey {
    return launchRequest
        ?.conversationId
        ?.let(::ConversationNavKey)
        ?: NewChatNavKey
}

private fun pendingDraftForConversation(
    entryUiState: ConversationEntryUiState,
    conversationId: String,
): ConversationDraft? {
    return when {
        entryUiState.conversationId == conversationId -> {
            entryUiState.pendingDraft
        }
        else -> null
    }
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
    entryModel: ConversationEntryModel,
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
    entryModel: ConversationEntryModel,
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

private fun pendingScrollPositionForConversation(
    entryUiState: ConversationEntryUiState,
    conversationId: String,
): Int? {
    return when {
        entryUiState.conversationId == conversationId -> entryUiState.pendingScrollPosition
        else -> null
    }
}

private fun pendingStartupAttachmentForConversation(
    entryUiState: ConversationEntryUiState,
    conversationId: String,
): ConversationEntryStartupAttachment? {
    return when {
        entryUiState.conversationId == conversationId -> {
            entryUiState.pendingStartupAttachment
        }
        else -> null
    }
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
