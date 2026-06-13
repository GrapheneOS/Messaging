package com.android.messaging.ui.conversation.recipientpicker.model.selection

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class RecipientSelectionQueryCardUiState(
    val text: RecipientSelectionQueryTextUiState,
    val chips: RecipientSelectionQueryChipsUiState,
)

@Immutable
internal data class RecipientSelectionQueryTextUiState(
    val query: String,
    val enabled: Boolean,
    val prefixText: String,
    val placeholderText: String,
)

@Immutable
internal data class RecipientSelectionQueryChipsUiState(
    val recipients: ImmutableList<SelectedRecipient>,
    val armedRecipientDestination: String?,
    val enabled: Boolean,
)
