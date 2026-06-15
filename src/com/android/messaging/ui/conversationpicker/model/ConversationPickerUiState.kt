package com.android.messaging.ui.conversationpicker.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.subscription.model.SimSelectionUiState

@Immutable
internal data class ConversationPickerUiState(
    val targets: TargetsUiState = TargetsUiState(),
    val contacts: RecipientPickerUiState = RecipientPickerUiState(),
    val draft: DraftUiState = DraftUiState(),
    val sim: SimSelectionUiState = SimSelectionUiState(),
    val isSendEnabled: Boolean = false,
) {
    val isLoading: Boolean
        get() = targets.isLoading || draft.isLoading
}
