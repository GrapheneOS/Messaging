@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.addparticipants

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.ui.conversation.ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.addParticipantsContactDestinationRowTestTag
import com.android.messaging.ui.conversation.addParticipantsContactRowTestTag
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsUiState
import com.android.messaging.ui.conversation.preview.previewRecipientPickerUiState
import com.android.messaging.ui.conversation.preview.previewSelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.component.RecipientSelectionContent
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.picker.toSelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionPrimaryActionUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionStrings
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.util.UiUtils
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun AddParticipantsScreen(
    conversationId: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToConversation: (String) -> Unit = {},
    screenModel: AddParticipantsScreenModel = hiltViewModel<AddParticipantsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()
    val latestOnNavigateToConversation = rememberUpdatedState(onNavigateToConversation)

    LaunchedEffect(conversationId, screenModel) {
        screenModel.onConversationIdChanged(conversationId = conversationId)
    }

    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is AddParticipantsEffect.NavigateToConversation -> {
                    latestOnNavigateToConversation.value(effect.conversationId)
                }

                is AddParticipantsEffect.ShowMessage -> {
                    UiUtils.showToastAtBottom(effect.messageResId)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                title = {
                    Text(text = stringResource(id = R.string.conversation_add_people))
                },
            )
        },
    ) { contentPadding ->
        AddParticipantsRecipientSelectionContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            uiState = uiState,
            onLoadMore = screenModel::onLoadMore,
            onQueryChanged = screenModel::onQueryChanged,
            onConfirmClick = screenModel::onConfirmClick,
            onRecipientClick = screenModel::onRecipientClicked,
        )
    }
}

@Composable
private fun AddParticipantsRecipientSelectionContent(
    uiState: AddParticipantsUiState,
    onConfirmClick: () -> Unit,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onRecipientClick: (SelectedRecipient) -> Unit,
    modifier: Modifier = Modifier,
) {
    RecipientSelectionContent(
        uiState = addParticipantsRecipientSelectionContentUiState(
            uiState = uiState,
            primaryActionText = stringResource(id = R.string.conversation_add_people),
        ),
        strings = RecipientSelectionStrings(
            queryPrefixText = stringResource(id = R.string.to_address_label),
            queryPlaceholderText = addParticipantsQueryHint(
                hasSelectedRecipients = uiState.selectedRecipients.isNotEmpty(),
            ),
        ),
        rowDecorators = RecipientSelectionRowDecorators(
            recipientRowTestTag = { item ->
                addParticipantsContactRowTestTag(contactId = item.id)
            },
            destinationRowTestTag = { item, destination ->
                addParticipantsContactDestinationRowTestTag(
                    contactId = item.id,
                    destination = destination,
                )
            },
        ),
        onRecipientDestinationClick = { item, destination ->
            onAddParticipantsRecipientDestinationClick(
                item = item,
                destination = destination,
                onRecipientClick = onRecipientClick,
            )
        },
        modifier = modifier,
        onLoadMore = onLoadMore,
        onPrimaryActionClick = onConfirmClick,
        onQueryChanged = onQueryChanged,
        onSelectedRecipientClick = onRecipientClick,
    )
}

private fun addParticipantsRecipientSelectionContentUiState(
    uiState: AddParticipantsUiState,
    primaryActionText: String,
): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = uiState.recipientPickerUiState.copy(
            isLoading = uiState.isLoadingConversationParticipants ||
                uiState.recipientPickerUiState.isLoading,
        ),
        primaryAction = addParticipantsPrimaryActionUiState(
            uiState = uiState,
            text = primaryActionText,
        ),
        selectedRecipients = uiState.selectedRecipients,
        isQueryEnabled = !uiState.isResolvingConversation &&
            !uiState.isLoadingConversationParticipants,
    )
}

private fun addParticipantsPrimaryActionUiState(
    uiState: AddParticipantsUiState,
    text: String,
): RecipientSelectionPrimaryActionUiState? {
    return when {
        uiState.selectedRecipients.isNotEmpty() -> {
            RecipientSelectionPrimaryActionUiState(
                text = text,
                isEnabled = !uiState.isLoadingConversationParticipants &&
                    !uiState.recipientPickerUiState.isLoading &&
                    !uiState.isResolvingConversation,
                isLoading = uiState.isResolvingConversation,
                testTag = ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG,
            )
        }

        else -> null
    }
}

private fun onAddParticipantsRecipientDestinationClick(
    item: RecipientPickerListItem,
    destination: String,
    onRecipientClick: (SelectedRecipient) -> Unit,
) {
    item.toSelectedRecipient(destination = destination)?.let { selectedRecipient ->
        onRecipientClick(selectedRecipient)
    }
}

@Composable
private fun addParticipantsQueryHint(
    hasSelectedRecipients: Boolean,
): String {
    return when {
        hasSelectedRecipients -> stringResource(R.string.recipient_selection_query_hint_more)
        else -> stringResource(R.string.new_chat_query_hint)
    }
}

@PreviewLightDark
@Composable
private fun AddParticipantsRecipientSelectionContentPreview() {
    MessagingPreviewTheme {
        AddParticipantsRecipientSelectionContent(
            modifier = Modifier.fillMaxSize(),
            uiState = AddParticipantsUiState(
                isLoadingConversationParticipants = false,
                recipientPickerUiState = previewRecipientPickerUiState(),
                selectedRecipients = persistentListOf(previewSelectedRecipient()),
            ),
            onConfirmClick = {},
            onLoadMore = {},
            onQueryChanged = { _ -> },
            onRecipientClick = { _ -> },
        )
    }
}
