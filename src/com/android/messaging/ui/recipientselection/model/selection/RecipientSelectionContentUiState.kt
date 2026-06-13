package com.android.messaging.ui.recipientselection.model.selection

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal typealias OnRecipientDestinationAction =
    (item: RecipientPickerListItem, destination: String) -> Unit

internal typealias RecipientRowTestTagProvider =
    (item: RecipientPickerListItem) -> String

internal typealias RecipientDestinationTestTagProvider =
    (item: RecipientPickerListItem, destination: String) -> String

internal typealias ShouldShowRecipientTrailingIndicator =
    (item: RecipientPickerListItem, destination: String) -> Boolean

@Immutable
internal data class RecipientSelectionContentUiState(
    val picker: RecipientPickerUiState = RecipientPickerUiState(),
    val primaryAction: RecipientSelectionPrimaryActionUiState? = null,
    val selectedRecipients: ImmutableList<SelectedRecipient> = persistentListOf(),
    val isQueryEnabled: Boolean = true,
)

@Immutable
internal data class RecipientSelectionPrimaryActionUiState(
    val text: String,
    val isEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val testTag: String? = null,
)

@Immutable
internal data class RecipientSelectionStrings(
    val queryPrefixText: String,
    val queryPlaceholderText: String,
)

internal data class RecipientSelectionRowDecorators(
    val recipientRowTestTag: RecipientRowTestTagProvider,
    val destinationRowTestTag: RecipientDestinationTestTagProvider =
        { item, _ -> recipientRowTestTag(item) },
    val showRecipientTrailingIndicator: ShouldShowRecipientTrailingIndicator = { _, _ -> false },
    val trailingIndicatorTestTag: String? = null,
)
