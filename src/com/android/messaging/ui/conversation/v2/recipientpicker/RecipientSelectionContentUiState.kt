package com.android.messaging.ui.conversation.v2.recipientpicker

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerListItem
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class RecipientSelectionContentUiState(
    val picker: RecipientPickerUiState = RecipientPickerUiState(),
    val primaryAction: RecipientSelectionPrimaryActionUiState? = null,
    val selectedRecipientDestinations: ImmutableSet<String> = persistentSetOf(),
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
    val recipientRowTestTag: (RecipientPickerListItem) -> String,
    val showRecipientTrailingIndicator: (RecipientPickerListItem) -> Boolean = { false },
    val trailingIndicatorTestTag: String? = null,
)
