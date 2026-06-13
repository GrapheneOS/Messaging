@file:Suppress("TooManyFunctions")

package com.android.messaging.ui.recipientselection.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.model.selection.OnRecipientDestinationAction
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionPrimaryActionUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionRowDecorators
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val PREVIEW_PRIMARY_ACTION_TEST_TAG = "preview-primary-action"
private const val PREVIEW_TRAILING_INDICATOR_TEST_TAG = "preview-recipient-loading"

@Composable
internal fun PreviewRecipientSelectionContactsContent(
    uiState: RecipientSelectionContentUiState,
    modifier: Modifier = Modifier,
    loadingDestination: String? = null,
    onRecipientDestinationLongClick: OnRecipientDestinationAction? = { _, _ -> },
    topListContent: (@Composable () -> Unit)? = null,
) {
    RecipientSelectionContactsContent(
        modifier = modifier,
        uiState = uiState,
        rowDecorators = previewRecipientSelectionContactsRowDecorators(
            loadingDestination = loadingDestination,
        ),
        onLoadMore = {},
        onPrimaryActionClick = {},
        onRecipientDestinationClick = { _, _ -> },
        onRecipientDestinationLongClick = onRecipientDestinationLongClick,
        topListContent = topListContent,
    )
}

@Composable
internal fun PreviewRecipientSelectionContactsTopListContent() {
    Text(
        modifier = Modifier.padding(bottom = 12.dp),
        text = previewRecipientSelectionContactsSelectedRecipients()
            .joinToString { recipient -> recipient.label },
    )
}

internal fun previewRecipientSelectionContactsLoadedState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = previewRecipientSelectionContactsDefaultItems(),
        ),
        primaryAction = previewRecipientSelectionPrimaryActionUiState(
            isEnabled = true,
        ),
        selectedRecipients = previewRecipientSelectionContactsSelectedRecipients(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsLoadingState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = RecipientPickerUiState(
            query = "Ada",
            items = persistentListOf(),
            canLoadMore = false,
            hasContactsPermission = true,
            isLoading = true,
            isLoadingMore = false,
        ),
        primaryAction = null,
        selectedRecipients = persistentListOf(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsEmptyState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = RecipientPickerUiState(
            query = "No matches",
            items = persistentListOf(),
            canLoadMore = false,
            hasContactsPermission = true,
            isLoading = false,
            isLoadingMore = false,
        ),
        primaryAction = null,
        selectedRecipients = persistentListOf(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsLoadingMoreState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = previewRecipientSelectionContactsDefaultItems(),
            canLoadMore = true,
            isLoadingMore = true,
        ),
        primaryAction = previewRecipientSelectionPrimaryActionUiState(
            isEnabled = true,
        ),
        selectedRecipients = previewRecipientSelectionContactsSelectedRecipients(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsPrimaryActionDisabledState():
    RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = previewRecipientSelectionContactsDefaultItems(),
        ),
        primaryAction = previewRecipientSelectionPrimaryActionUiState(
            isEnabled = false,
        ),
        selectedRecipients = persistentListOf(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsPrimaryActionLoadingState():
    RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = previewRecipientSelectionContactsDefaultItems(),
        ),
        primaryAction = previewRecipientSelectionPrimaryActionUiState(
            isEnabled = true,
            isLoading = true,
        ),
        selectedRecipients = previewRecipientSelectionContactsSelectedRecipients(),
        isQueryEnabled = false,
    )
}

internal fun previewRecipientSelectionContactsNoPrimaryActionState():
    RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = previewRecipientSelectionContactsDefaultItems(),
        ),
        primaryAction = null,
        selectedRecipients = persistentListOf(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsTopContentState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = previewRecipientSelectionContactsDefaultItems(),
        ),
        primaryAction = previewRecipientSelectionPrimaryActionUiState(
            isEnabled = true,
        ),
        selectedRecipients = previewRecipientSelectionContactsSelectedRecipients(),
        isQueryEnabled = true,
    )
}

internal fun previewRecipientSelectionContactsLongTextState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = previewRecipientSelectionContactsPickerState(
            items = persistentListOf<RecipientPickerListItem>()
                .add(previewRecipientSelectionLongSingleDestinationContactItem())
                .add(previewRecipientSelectionLongMultiDestinationContactItem())
                .add(previewRecipientSelectionLongSyntheticPhoneItem()),
        ),
        primaryAction = previewRecipientSelectionPrimaryActionUiState(
            isEnabled = true,
        ),
        selectedRecipients = persistentListOf(
            SelectedRecipient(
                destination = RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
                label = "Alexandria Cassandra Montgomery-Washington",
                displayDestination = RECIPIENT_ROW_PREVIEW_LONG_PHONE_DESTINATION,
                photoUri = null,
            ),
        ),
        isQueryEnabled = true,
    )
}

private fun previewRecipientSelectionContactsDefaultItems():
    ImmutableList<RecipientPickerListItem> {
    return persistentListOf<RecipientPickerListItem>()
        .add(previewRecipientSelectionSingleDestinationContactItem())
        .add(previewRecipientSelectionSyntheticPhoneItem())
        .add(previewRecipientSelectionMultiDestinationContactItem())
        .add(previewRecipientSelectionSingleEmailDestinationContactItem())
}

private fun previewRecipientSelectionContactsPickerState(
    items: ImmutableList<RecipientPickerListItem>,
    canLoadMore: Boolean = false,
    isLoadingMore: Boolean = false,
): RecipientPickerUiState {
    return RecipientPickerUiState(
        query = "Ada",
        items = items,
        canLoadMore = canLoadMore,
        hasContactsPermission = true,
        isLoading = false,
        isLoadingMore = isLoadingMore,
    )
}

private fun previewRecipientSelectionPrimaryActionUiState(
    isEnabled: Boolean,
    isLoading: Boolean = false,
): RecipientSelectionPrimaryActionUiState {
    return RecipientSelectionPrimaryActionUiState(
        text = "Start chat",
        isEnabled = isEnabled,
        isLoading = isLoading,
        testTag = PREVIEW_PRIMARY_ACTION_TEST_TAG,
    )
}

private fun previewRecipientSelectionContactsSelectedRecipients():
    ImmutableList<SelectedRecipient> {
    return persistentListOf(
        SelectedRecipient(
            destination = RECIPIENT_ROW_PREVIEW_PRIMARY_DESTINATION,
            label = "Ada Lovelace",
            displayDestination = "+31 6 2222 3333",
            photoUri = null,
        ),
        SelectedRecipient(
            destination = RECIPIENT_ROW_PREVIEW_SYNTHETIC_DESTINATION,
            label = "+31 6 5555 0199",
            displayDestination = "Mobile",
            photoUri = null,
        ),
    )
}

private fun previewRecipientSelectionContactsRowDecorators(
    loadingDestination: String?,
): RecipientSelectionRowDecorators {
    return RecipientSelectionRowDecorators(
        recipientRowTestTag = { item -> item.id },
        destinationRowTestTag = { item, destination -> "${item.id}:$destination" },
        showRecipientTrailingIndicator = { _, destination ->
            destination == loadingDestination
        },
        trailingIndicatorTestTag = PREVIEW_TRAILING_INDICATOR_TEST_TAG,
    )
}
