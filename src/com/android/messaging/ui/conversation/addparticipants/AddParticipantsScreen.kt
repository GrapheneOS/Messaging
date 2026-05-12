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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.ui.conversation.ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.addParticipantsContactDestinationRowTestTag
import com.android.messaging.ui.conversation.addParticipantsContactRowTestTag
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsUiState
import com.android.messaging.ui.conversation.recipientpicker.RecipientSelectionContent
import com.android.messaging.ui.conversation.recipientpicker.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.RecipientSelectionPrimaryActionUiState
import com.android.messaging.ui.conversation.recipientpicker.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversation.recipientpicker.RecipientSelectionStrings
import com.android.messaging.util.UiUtils
import kotlinx.collections.immutable.toImmutableSet

@Composable
internal fun AddParticipantsScreen(
    conversationId: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToConversation: (String) -> Unit = {},
    screenModel: AddParticipantsModel = hiltViewModel<AddParticipantsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId, screenModel) {
        screenModel.onConversationIdChanged(conversationId = conversationId)
    }

    LaunchedEffect(screenModel, onNavigateToConversation) {
        screenModel.effects.collect { effect ->
            when (effect) {
                is AddParticipantsEffect.NavigateToConversation -> {
                    onNavigateToConversation(effect.conversationId)
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
    onRecipientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryAction = when {
        uiState.selectedRecipientDestinations.isNotEmpty() -> {
            RecipientSelectionPrimaryActionUiState(
                text = stringResource(id = R.string.conversation_add_people),
                isEnabled = !uiState.isLoadingConversationParticipants &&
                    !uiState.recipientPickerUiState.isLoading &&
                    !uiState.isResolvingConversation,
                isLoading = uiState.isResolvingConversation,
                testTag = ADD_PARTICIPANTS_CONFIRM_BUTTON_TEST_TAG,
            )
        }

        else -> null
    }

    RecipientSelectionContent(
        uiState = RecipientSelectionContentUiState(
            picker = uiState.recipientPickerUiState.copy(
                isLoading = uiState.isLoadingConversationParticipants ||
                    uiState.recipientPickerUiState.isLoading,
            ),
            primaryAction = primaryAction,
            selectedRecipientDestinations = uiState.selectedRecipientDestinations.toImmutableSet(),
            isQueryEnabled = !uiState.isResolvingConversation &&
                !uiState.isLoadingConversationParticipants,
        ),
        strings = RecipientSelectionStrings(
            queryPrefixText = stringResource(id = R.string.to_address_label),
            queryPlaceholderText = stringResource(id = R.string.new_chat_query_hint),
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
        onRecipientDestinationClick = { _, destination ->
            onRecipientClick(destination)
        },
        modifier = modifier,
        onLoadMore = onLoadMore,
        onPrimaryActionClick = onConfirmClick,
        onQueryChanged = onQueryChanged,
    )
}
