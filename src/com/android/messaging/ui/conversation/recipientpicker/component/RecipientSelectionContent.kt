@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.conversation.recipientpicker.component.simselector.NewChatSimSelectorRow
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionStrings
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun RecipientSelectionContent(
    uiState: RecipientSelectionContentUiState,
    strings: RecipientSelectionStrings,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    modifier: Modifier = Modifier,
    autoFocusQuery: Boolean = false,
    onLoadMore: () -> Unit = {},
    onPrimaryActionClick: () -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onRecipientDestinationLongClick: OnRecipientDestinationAction? = null,
    onSelectedRecipientClick: (SelectedRecipient) -> Unit = {},
    simSelectorSlot: (@Composable () -> Unit)? = null,
    topListContent: (@Composable () -> Unit)? = null,
) {
    val queryFocusRequester = remember { FocusRequester() }
    val armedDestination = rememberSaveable { mutableStateOf<String?>(null) }

    RecipientSelectionArmedRecipientResetEffect(
        selectedRecipients = uiState.selectedRecipients,
        armedDestination = armedDestination,
    )
    RecipientSelectionAutoFocusEffect(
        autoFocusQuery = autoFocusQuery,
        focusRequester = queryFocusRequester,
    )

    RecipientSelectionContentLayout(
        modifier = modifier,
        queryArea = {
            RecipientSelectionArmedQueryArea(
                uiState = uiState,
                strings = strings,
                armedDestination = armedDestination,
                queryFocusRequester = queryFocusRequester,
                simSelectorSlot = simSelectorSlot,
                onQueryChanged = onQueryChanged,
                onSelectedRecipientClick = onSelectedRecipientClick,
            )
        },
        contactsArea = {
            RecipientSelectionArmedContactsArea(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                rowDecorators = rowDecorators,
                armedDestination = armedDestination,
                topListContent = topListContent,
                onLoadMore = onLoadMore,
                onPrimaryActionClick = onPrimaryActionClick,
                onRecipientDestinationClick = onRecipientDestinationClick,
                onRecipientDestinationLongClick = onRecipientDestinationLongClick,
            )
        },
    )
}

@Composable
private fun RecipientSelectionContentLayout(
    queryArea: @Composable () -> Unit,
    contactsArea: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            queryArea()

            Spacer(modifier = Modifier.height(12.dp))
            contactsArea()
        }
    }
}

@Composable
private fun RecipientSelectionArmedQueryArea(
    uiState: RecipientSelectionContentUiState,
    strings: RecipientSelectionStrings,
    armedDestination: MutableState<String?>,
    queryFocusRequester: FocusRequester,
    simSelectorSlot: (@Composable () -> Unit)?,
    onQueryChanged: (String) -> Unit,
    onSelectedRecipientClick: (SelectedRecipient) -> Unit,
) {
    val currentOnQueryChanged = rememberUpdatedState(onQueryChanged)
    val currentOnSelectedRecipientClick = rememberUpdatedState(onSelectedRecipientClick)
    val onQueryChangedWrapped: (String) -> Unit = remember(armedDestination) {
        { query ->
            armedDestination.value = null
            currentOnQueryChanged.value(query)
        }
    }

    val onQueryFocusedWrapped: () -> Unit = remember(armedDestination) {
        { armedDestination.value = null }
    }

    val onSelectedRecipientClickWrapped: (SelectedRecipient) -> Unit = remember(
        armedDestination,
    ) {
        { recipient ->
            when {
                armedDestination.value == recipient.destination -> {
                    armedDestination.value = null
                    currentOnSelectedRecipientClick.value(recipient)
                }

                else -> {
                    armedDestination.value = recipient.destination
                }
            }
        }
    }

    val onSelectedRecipientBackspace: (SelectedRecipient) -> Unit = remember(armedDestination) {
        { recipient ->
            armedDestination.value = null
            currentOnSelectedRecipientClick.value(recipient)
        }
    }

    RecipientSelectionQueryCard(
        uiState = recipientSelectionQueryCardUiState(
            uiState = uiState,
            strings = strings,
            armedRecipientDestination = armedDestination.value,
        ),
        onQueryChanged = onQueryChangedWrapped,
        onQueryFocused = onQueryFocusedWrapped,
        onSelectedRecipientClick = onSelectedRecipientClickWrapped,
        onSelectedRecipientBackspace = onSelectedRecipientBackspace,
        focusRequester = queryFocusRequester,
        simSelectorSlot = simSelectorSlot,
    )
}

@Composable
private fun RecipientSelectionArmedContactsArea(
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    armedDestination: MutableState<String?>,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    onRecipientDestinationLongClick: OnRecipientDestinationAction?,
    onLoadMore: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    topListContent: (@Composable () -> Unit)? = null,
) {
    val currentOnPrimaryActionClick = rememberUpdatedState(onPrimaryActionClick)
    val currentOnRecipientDestinationClick = rememberUpdatedState(onRecipientDestinationClick)
    val currentOnRecipientDestinationLongClick = rememberUpdatedState(
        newValue = onRecipientDestinationLongClick,

    )
    val onPrimaryActionClickWrapped: () -> Unit = remember(armedDestination) {
        {
            armedDestination.value = null
            currentOnPrimaryActionClick.value()
        }
    }

    val onRecipientDestinationClickWrapped: OnRecipientDestinationAction = remember(
        armedDestination,
    ) {
        { item, destination ->
            armedDestination.value = null
            currentOnRecipientDestinationClick.value(item, destination)
        }
    }

    val onRecipientDestinationLongClickWrapped: OnRecipientDestinationAction = remember(
        armedDestination,
    ) {
        { item, destination ->
            armedDestination.value = null
            currentOnRecipientDestinationLongClick.value?.invoke(item, destination)
        }
    }

    RecipientSelectionContactsContent(
        modifier = modifier,
        uiState = uiState,
        rowDecorators = rowDecorators,
        onLoadMore = onLoadMore,
        onPrimaryActionClick = onPrimaryActionClickWrapped,
        onRecipientDestinationClick = onRecipientDestinationClickWrapped,
        onRecipientDestinationLongClick = onRecipientDestinationLongClickWrapped
            .takeIf { onRecipientDestinationLongClick != null },
        topListContent = topListContent,
    )
}

@Composable
private fun RecipientSelectionAutoFocusEffect(
    autoFocusQuery: Boolean,
    focusRequester: FocusRequester,
) {
    if (autoFocusQuery) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContentLoadedPreview() {
    PreviewRecipientSelectionContent(
        uiState = previewRecipientSelectionContactsLoadedState(),
    )
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContentLoadingPreview() {
    PreviewRecipientSelectionContent(
        uiState = previewRecipientSelectionContactsLoadingState(),
        onRecipientDestinationLongClick = null,
    )
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContentEmptyPreview() {
    PreviewRecipientSelectionContent(
        uiState = previewRecipientSelectionContactsEmptyState(),
        onRecipientDestinationLongClick = null,
    )
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContentSimSelectorAndTopContentPreview() {
    PreviewRecipientSelectionContent(
        uiState = previewRecipientSelectionContactsTopContentState(),
        simSelectorSlot = {
            NewChatSimSelectorRow(
                uiState = previewSimSelectorUiState(),
                onSimSelected = { _ -> },
            )
        },
        topListContent = {
            PreviewRecipientSelectionContactsTopListContent()
        },
    )
}

@PreviewLightDark
@Composable
private fun RecipientSelectionContentPrimaryActionLoadingPreview() {
    PreviewRecipientSelectionContent(
        uiState = previewRecipientSelectionContactsPrimaryActionLoadingState(),
    )
}

@Composable
private fun PreviewRecipientSelectionContent(
    uiState: RecipientSelectionContentUiState,
    modifier: Modifier = Modifier.height(height = 560.dp),
    onRecipientDestinationLongClick: OnRecipientDestinationAction? = { _, _ -> },
    simSelectorSlot: (@Composable () -> Unit)? = null,
    topListContent: (@Composable () -> Unit)? = null,
) {
    MessagingPreviewTheme {
        RecipientSelectionContent(
            modifier = modifier,
            uiState = uiState,
            strings = previewRecipientSelectionStrings(),
            rowDecorators = previewRecipientSelectionContentRowDecorators(),
            onRecipientDestinationClick = { _, _ -> },
            onRecipientDestinationLongClick = onRecipientDestinationLongClick,
            onSelectedRecipientClick = { _ -> },
            onQueryChanged = { _ -> },
            simSelectorSlot = simSelectorSlot,
            topListContent = topListContent,
        )
    }
}

private fun previewRecipientSelectionStrings(): RecipientSelectionStrings {
    return RecipientSelectionStrings(
        queryPrefixText = "To",
        queryPlaceholderText = "Name or phone number",
    )
}

private fun previewRecipientSelectionContentRowDecorators(): RecipientSelectionRowDecorators {
    return RecipientSelectionRowDecorators(
        recipientRowTestTag = { item -> item.id },
        destinationRowTestTag = { item, destination -> "${item.id}:$destination" },
    )
}
