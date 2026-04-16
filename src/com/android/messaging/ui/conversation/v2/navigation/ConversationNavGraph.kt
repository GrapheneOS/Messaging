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
    onFinish: () -> Unit,
    entryModel: ConversationEntryModel = hiltViewModel<ConversationEntryViewModel>(),
) {
    val entryUiState by entryModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(initialNavKey(launchRequest = launchRequest))
    val latestEntryModel = rememberUpdatedState(newValue = entryModel)
    val latestEntryUiState = rememberUpdatedState(newValue = entryUiState)
    val latestOnFinish = rememberUpdatedState(newValue = onFinish)

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
                    onNavigateBack = {
                        popBackStackOrFinish(
                            backStack = backStack,
                            onFinish = currentOnFinish,
                        )
                    },
                    pendingDraft = pendingDraftForConversation(
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
                            onFinish = latestOnFinish.value,
                        )
                    },
                    resolvingRecipientDestination = currentEntryUiState
                        .resolvingRecipientDestination,
                    selectedGroupRecipientDestinations = currentEntryUiState
                        .selectedGroupRecipientDestinations,
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
        )
    }

    LaunchedEffect(entryModel, onFinish) {
        entryModel.effects.collect { effect ->
            handleEntryEffect(
                backStack = backStack,
                effect = effect,
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
) {
    val destination = initialNavKey(launchRequest = launchRequest)

    if (backStack.size == 1 && backStack.firstOrNull() == destination) {
        return
    }

    backStack.clear()
    backStack.add(destination)
}

private fun popBackStackOrFinish(
    backStack: MutableList<NavKey>,
    onFinish: () -> Unit,
) {
    if (backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
        return
    }

    onFinish()
}

private fun handleNavBack(
    backStack: MutableList<NavKey>,
    entryModel: ConversationEntryModel,
    entryUiState: ConversationEntryUiState,
    onFinish: () -> Unit,
) {
    if (backStack.lastOrNull() == NewChatNavKey && entryUiState.isCreatingGroup) {
        entryModel.onCreateGroupCanceled()
        return
    }

    popBackStackOrFinish(
        backStack = backStack,
        onFinish = onFinish,
    )
}

private fun handleNewChatBack(
    entryModel: ConversationEntryModel,
    entryUiState: ConversationEntryUiState,
    backStack: MutableList<NavKey>,
    onFinish: () -> Unit,
) {
    if (entryUiState.isCreatingGroup) {
        entryModel.onCreateGroupCanceled()
        return
    }

    popBackStackOrFinish(
        backStack = backStack,
        onFinish = onFinish,
    )
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
    onFinish: () -> Unit,
) {
    when (effect) {
        is ConversationEntryEffect.NavigateBack -> {
            popBackStackOrFinish(
                backStack = backStack,
                onFinish = onFinish,
            )
        }

        is ConversationEntryEffect.NavigateToConversation -> {
            navigateToConversation(
                backStack = backStack,
                conversationId = effect.conversationId,
            )
        }

        is ConversationEntryEffect.NavigateToRecipientPicker -> {
            navigateToRecipientPicker(
                backStack = backStack,
                mode = effect.mode,
            )
        }

        is ConversationEntryEffect.ShowMessage -> {
            UiUtils.showToastAtBottom(effect.messageResId)
        }
    }
}

private fun navigateToConversation(
    backStack: MutableList<NavKey>,
    conversationId: String,
) {
    ConversationNavKey(conversationId = conversationId)
        .takeIf { it != backStack.lastOrNull() }
        ?.let(backStack::add)
}

private fun navigateToRecipientPicker(
    backStack: MutableList<NavKey>,
    mode: RecipientPickerMode,
) {
    RecipientPickerNavKey(mode = mode)
        .takeIf { it != backStack.lastOrNull() }
        ?.let(backStack::add)
}
