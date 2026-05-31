@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.ui.conversation.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_NAVIGATE_BACK_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_TOP_APP_BAR_TITLE_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.entry.model.NewChatEffect
import com.android.messaging.ui.conversation.entry.model.NewChatUiState
import com.android.messaging.ui.conversation.newChatContactDestinationRowTestTag
import com.android.messaging.ui.conversation.newChatContactRowTestTag
import com.android.messaging.ui.conversation.preview.previewRecipientPickerUiState
import com.android.messaging.ui.conversation.preview.previewSelectedRecipient
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.conversation.recipientpicker.component.RecipientSelectionContent
import com.android.messaging.ui.conversation.recipientpicker.component.simselector.NewChatSimSelectorRow
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerUiState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.picker.toSelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionPrimaryActionUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionStrings
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.util.UiUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private typealias NewChatNavigateToConversation = (
    conversationId: String,
    selfParticipantId: String?,
) -> Unit

@Composable
internal fun NewChatScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToConversation: NewChatNavigateToConversation = { _, _ -> },
    screenModel: NewChatScreenModel = hiltViewModel<NewChatViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    NewChatScreenEffects(
        uiState = uiState,
        screenModel = screenModel,
        onNavigateBack = onNavigateBack,
        onNavigateToConversation = onNavigateToConversation,
    )

    NewChatScreenContent(
        modifier = modifier,
        isCreatingGroup = uiState.isCreatingGroup,
        isResolvingConversation = uiState.isResolvingConversation,
        isResolvingConversationIndicatorVisible = uiState
            .isResolvingConversationIndicatorVisible,
        onContactClick = screenModel::onContactClicked,
        onContactLongClick = screenModel::onContactLongClicked,
        onCreateGroupClick = screenModel::onCreateGroupRequested,
        onCreateGroupConfirmed = screenModel::onCreateGroupConfirmed,
        onCreateGroupRecipientClick = screenModel::onCreateGroupRecipientClicked,
        onLoadMore = screenModel::onLoadMore,
        onNavigateBack = screenModel::onNavigateBack,
        onQueryChanged = screenModel::onQueryChanged,
        onSimSelected = screenModel::onSimSelected,
        pickerUiState = uiState.recipientPickerUiState,
        resolvingRecipientDestination = uiState.resolvingRecipientDestination,
        selectedGroupRecipients = uiState.selectedGroupRecipients,
        simSelectorUiState = uiState.simSelectorState,
    )
}

@Composable
private fun NewChatScreenEffects(
    uiState: NewChatUiState,
    screenModel: NewChatScreenModel,
    onNavigateBack: () -> Unit,
    onNavigateToConversation: NewChatNavigateToConversation,
) {
    val latestOnNavigateBack = rememberUpdatedState(newValue = onNavigateBack)
    val latestOnNavigateToConversation = rememberUpdatedState(
        newValue = onNavigateToConversation,
    )

    BackHandler(enabled = uiState.isCreatingGroup || uiState.isResolvingConversation) {
        screenModel.onNavigateBack()
    }

    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            when (effect) {
                NewChatEffect.NavigateBack -> {
                    latestOnNavigateBack.value()
                }

                is NewChatEffect.NavigateToConversation -> {
                    latestOnNavigateToConversation.value(
                        effect.conversationId,
                        effect.selfParticipantId,
                    )
                }

                is NewChatEffect.ShowMessage -> {
                    UiUtils.showToastAtBottom(effect.messageResId)
                }
            }
        }
    }
}

@Composable
private fun NewChatScreenContent(
    modifier: Modifier = Modifier,
    isCreatingGroup: Boolean = false,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    onContactClick: (String) -> Unit = {},
    onContactLongClick: (SelectedRecipient) -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onCreateGroupConfirmed: () -> Unit = {},
    onCreateGroupRecipientClick: (SelectedRecipient) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onSimSelected: (String) -> Unit = {},
    pickerUiState: RecipientPickerUiState = RecipientPickerUiState(),
    resolvingRecipientDestination: String? = null,
    selectedGroupRecipients: ImmutableList<SelectedRecipient> = persistentListOf(),
    simSelectorUiState: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
) {
    val screenContainerColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        modifier = modifier,
        containerColor = screenContainerColor,
        topBar = {
            NewChatTopAppBar(
                containerColor = screenContainerColor,
                isCreatingGroup = isCreatingGroup,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { contentPadding ->
        NewChatRecipientSelectionContent(
            contentPadding = contentPadding,
            isCreatingGroup = isCreatingGroup,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onCreateGroupClick = onCreateGroupClick,
            onCreateGroupConfirmed = onCreateGroupConfirmed,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
            onLoadMore = onLoadMore,
            onQueryChanged = onQueryChanged,
            onSimSelected = onSimSelected,
            pickerUiState = pickerUiState,
            resolvingRecipientDestination = resolvingRecipientDestination,
            selectedGroupRecipients = selectedGroupRecipients,
            simSelectorUiState = simSelectorUiState,
        )
    }
}

@Composable
private fun NewChatTopAppBar(
    containerColor: Color,
    isCreatingGroup: Boolean,
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .testTag(tag = NEW_CHAT_NAVIGATE_BACK_BUTTON_TEST_TAG),
                onClick = onNavigateBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                )
            }
        },
        title = {
            Text(
                modifier = Modifier
                    .testTag(tag = NEW_CHAT_TOP_APP_BAR_TITLE_TEST_TAG),
                text = newChatTitle(isCreatingGroup = isCreatingGroup),
            )
        },
    )
}

@Composable
private fun NewChatRecipientSelectionContent(
    contentPadding: PaddingValues,
    isCreatingGroup: Boolean,
    isResolvingConversation: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    onContactClick: (String) -> Unit,
    onContactLongClick: (SelectedRecipient) -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateGroupConfirmed: () -> Unit,
    onCreateGroupRecipientClick: (SelectedRecipient) -> Unit,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSimSelected: (String) -> Unit,
    pickerUiState: RecipientPickerUiState,
    resolvingRecipientDestination: String?,
    selectedGroupRecipients: ImmutableList<SelectedRecipient>,
    simSelectorUiState: ConversationSimSelectorUiState,
) {
    RecipientSelectionContent(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = contentPadding),
        uiState = newChatRecipientSelectionContentUiState(
            pickerUiState = pickerUiState,
            isCreatingGroup = isCreatingGroup,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            selectedGroupRecipients = selectedGroupRecipients,
        ),
        strings = newChatRecipientSelectionStrings(
            hasSelectedRecipients = selectedGroupRecipients.isNotEmpty(),
        ),
        rowDecorators = newChatRecipientSelectionRowDecorators(
            isCreatingGroup = isCreatingGroup,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            resolvingRecipientDestination = resolvingRecipientDestination,
        ),
        onRecipientDestinationClick = newChatRecipientDestinationAction(
            isCreatingGroup = isCreatingGroup,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
            onContactRecipientClick = { selectedRecipient ->
                onContactClick(selectedRecipient.destination)
            },
        ),
        autoFocusQuery = true,
        onLoadMore = onLoadMore,
        onPrimaryActionClick = onCreateGroupConfirmed,
        onQueryChanged = onQueryChanged,
        onRecipientDestinationLongClick = newChatRecipientDestinationAction(
            isCreatingGroup = isCreatingGroup,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
            onContactRecipientClick = onContactLongClick,
        ),
        onSelectedRecipientClick = onCreateGroupRecipientClick,
        simSelectorSlot = {
            NewChatSimSelectorRow(
                uiState = simSelectorUiState,
                onSimSelected = onSimSelected,
            )
        },
        topListContent = {
            NewChatRecipientSelectionTopListContent(
                isCreatingGroup = isCreatingGroup,
                onCreateGroupClick = onCreateGroupClick,
            )
        },
    )
}

private fun newChatRecipientDestinationAction(
    isCreatingGroup: Boolean,
    onCreateGroupRecipientClick: (SelectedRecipient) -> Unit,
    onContactRecipientClick: (SelectedRecipient) -> Unit,
): OnRecipientDestinationAction {
    return { item, destination ->
        item.toSelectedRecipient(destination = destination)?.let { selectedRecipient ->
            when {
                isCreatingGroup -> {
                    onCreateGroupRecipientClick(selectedRecipient)
                }

                else -> {
                    onContactRecipientClick(selectedRecipient)
                }
            }
        }
    }
}

private fun newChatRecipientSelectionRowDecorators(
    isCreatingGroup: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    resolvingRecipientDestination: String?,
): RecipientSelectionRowDecorators {
    return RecipientSelectionRowDecorators(
        recipientRowTestTag = { item ->
            newChatContactRowTestTag(contactId = item.id)
        },
        destinationRowTestTag = { item, destination ->
            newChatContactDestinationRowTestTag(
                contactId = item.id,
                destination = destination,
            )
        },
        showRecipientTrailingIndicator = { _, destination ->
            !isCreatingGroup &&
                isResolvingConversationIndicatorVisible &&
                resolvingRecipientDestination == destination
        },
        trailingIndicatorTestTag = NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG,
    )
}

@Composable
private fun newChatRecipientSelectionContentUiState(
    pickerUiState: RecipientPickerUiState,
    isCreatingGroup: Boolean,
    isResolvingConversation: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    selectedGroupRecipients: ImmutableList<SelectedRecipient>,
): RecipientSelectionContentUiState {
    val primaryAction = when {
        isCreatingGroup && selectedGroupRecipients.isNotEmpty() -> {
            RecipientSelectionPrimaryActionUiState(
                text = stringResource(id = R.string.next),
                isEnabled = !pickerUiState.isLoading && !isResolvingConversation,
                isLoading = isResolvingConversationIndicatorVisible,
                testTag = NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG,
            )
        }

        else -> null
    }

    return RecipientSelectionContentUiState(
        picker = pickerUiState,
        primaryAction = primaryAction,
        selectedRecipients = when {
            isCreatingGroup -> selectedGroupRecipients
            else -> persistentListOf()
        },
        isQueryEnabled = !isResolvingConversation,
    )
}

@Composable
private fun newChatRecipientSelectionStrings(
    hasSelectedRecipients: Boolean,
): RecipientSelectionStrings {
    return RecipientSelectionStrings(
        queryPrefixText = stringResource(id = R.string.new_chat_recipient_prefix),
        queryPlaceholderText = newChatQueryHint(
            hasSelectedRecipients = hasSelectedRecipients,
        ),
    )
}

@Composable
private fun newChatQueryHint(
    hasSelectedRecipients: Boolean,
): String {
    return when {
        hasSelectedRecipients -> stringResource(R.string.recipient_selection_query_hint_more)
        else -> stringResource(R.string.new_chat_query_hint)
    }
}

@Composable
private fun newChatTitle(
    isCreatingGroup: Boolean,
): String {
    return when {
        isCreatingGroup -> stringResource(R.string.conversation_new_group)
        else -> stringResource(R.string.start_new_conversation)
    }
}

@PreviewLightDark
@Composable
private fun NewChatScreenContentPreview() {
    MessagingPreviewTheme {
        NewChatScreenContent(
            modifier = Modifier.fillMaxSize(),
            pickerUiState = previewRecipientPickerUiState(),
            simSelectorUiState = previewSimSelectorUiState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun NewChatScreenContentCreatingGroupPreview() {
    MessagingPreviewTheme {
        NewChatScreenContent(
            modifier = Modifier.fillMaxSize(),
            isCreatingGroup = true,
            pickerUiState = previewRecipientPickerUiState(),
            selectedGroupRecipients = persistentListOf(previewSelectedRecipient()),
            simSelectorUiState = previewSimSelectorUiState(),
        )
    }
}
