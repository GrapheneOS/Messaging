@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.KeyboardActionHandler
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.conversation.recipientpicker.component.simselector.NewChatSimSelectorRow
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.recipientselection.component.PreviewRecipientSelectionContactsTopListContent
import com.android.messaging.ui.recipientselection.component.RecipientSelectionContactsContent
import com.android.messaging.ui.recipientselection.component.previewRecipientSelectionContactsEmptyState
import com.android.messaging.ui.recipientselection.component.previewRecipientSelectionContactsLoadedState
import com.android.messaging.ui.recipientselection.component.previewRecipientSelectionContactsLoadingState
import com.android.messaging.ui.recipientselection.component.previewRecipientSelectionContactsPrimaryActionLoadingState
import com.android.messaging.ui.recipientselection.component.previewRecipientSelectionContactsTopContentState
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RecipientSelectionContent(
    uiState: RecipientSelectionContentUiState,
    strings: RecipientSelectionStrings,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientDestinationClick: OnRecipientDestinationAction,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    autoFocusQuery: Boolean = false,
    onLoadMore: () -> Unit = {},
    onPrimaryActionClick: () -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onRecipientDestinationLongClick: OnRecipientDestinationAction? = null,
    onSelectedRecipientClick: (SelectedRecipient) -> Unit = {},
    pinnedTopContent: (@Composable () -> Unit)? = null,
    simSelectorSlot: (@Composable () -> Unit)? = null,
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
        contentPadding = contentPadding,
        pinnedTopContent = pinnedTopContent,
        queryArea = {
            RecipientSelectionArmedQueryArea(
                uiState = uiState,
                strings = strings,
                armedDestination = armedDestination,
                queryFocusRequester = queryFocusRequester,
                simSelectorSlot = simSelectorSlot,
                onQueryChanged = onQueryChanged,
                onRecipientDestinationClick = onRecipientDestinationClick,
                onSelectedRecipientClick = onSelectedRecipientClick,
            )
        },
        contactsArea = {
            RecipientSelectionArmedContactsArea(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = maxOf(
                        contentPadding.calculateBottomPadding(),
                        WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
                    ),
                ),
                uiState = uiState,
                rowDecorators = rowDecorators,
                armedDestination = armedDestination,
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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    pinnedTopContent: (@Composable () -> Unit)? = null,
) {
    val layoutDirection = LocalLayoutDirection.current

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            queryArea()

            Spacer(modifier = Modifier.height(12.dp))
            pinnedTopContent?.invoke()

            Box(modifier = Modifier.weight(weight = 1f)) {
                contactsArea()
            }
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
    onRecipientDestinationClick: OnRecipientDestinationAction,
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
        onKeyboardAction = rememberRecipientSelectionKeyboardActionHandler(
            uiState = uiState,
            armedDestination = armedDestination,
            onRecipientDestinationClick = onRecipientDestinationClick,
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
private fun rememberRecipientSelectionKeyboardActionHandler(
    uiState: RecipientSelectionContentUiState,
    armedDestination: MutableState<String?>,
    onRecipientDestinationClick: OnRecipientDestinationAction,
): KeyboardActionHandler {
    val currentUiState = rememberUpdatedState(uiState)
    val currentOnRecipientDestinationClick = rememberUpdatedState(onRecipientDestinationClick)

    return remember(armedDestination) {
        KeyboardActionHandler { performDefaultAction ->
            val currentState = currentUiState.value
            val item = recipientSelectionSoleResultOrNull(uiState = currentState)
            val destination = item?.let {
                recipientSelectionUnselectedDestinationOrNull(
                    item = item,
                    selectedRecipients = currentState.selectedRecipients,
                )
            }

            when {
                item == null || destination == null -> performDefaultAction()

                else -> {
                    armedDestination.value = null
                    currentOnRecipientDestinationClick.value(item, destination)
                }
            }
        }
    }
}

private fun recipientSelectionSoleResultOrNull(
    uiState: RecipientSelectionContentUiState,
): RecipientPickerListItem? {
    val picker = uiState.picker
    val isSettled = picker.query.isNotBlank() &&
        picker.itemsQuery == picker.query &&
        !picker.isLoading &&
        !picker.canLoadMore &&
        uiState.primaryAction?.isLoading != true

    return picker.items
        .singleOrNull()
        .takeIf { isSettled }
}

private fun recipientSelectionUnselectedDestinationOrNull(
    item: RecipientPickerListItem,
    selectedRecipients: ImmutableList<SelectedRecipient>,
): String? {
    val destination = when (item) {
        is RecipientPickerListItem.Contact -> item.destinations.singleOrNull()?.normalizedValue
        is RecipientPickerListItem.SyntheticPhone -> item.normalizedDestination
    }

    return destination.takeIf {
        selectedRecipients.none { recipient -> recipient.destination == destination }
    }
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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
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
        contentPadding = contentPadding,
        uiState = uiState,
        rowDecorators = rowDecorators,
        onLoadMore = onLoadMore,
        onPrimaryActionClick = onPrimaryActionClickWrapped,
        onRecipientDestinationClick = onRecipientDestinationClickWrapped,
        onRecipientDestinationLongClick = onRecipientDestinationLongClickWrapped
            .takeIf { onRecipientDestinationLongClick != null },
        emptyStateText = R.string.contact_list_empty_text,
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
        pinnedTopContent = {
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
    pinnedTopContent: (@Composable () -> Unit)? = null,
    simSelectorSlot: (@Composable () -> Unit)? = null,
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
            pinnedTopContent = pinnedTopContent,
            simSelectorSlot = simSelectorSlot,
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
