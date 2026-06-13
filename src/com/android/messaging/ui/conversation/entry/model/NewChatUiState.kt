package com.android.messaging.ui.conversation.entry.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class NewChatUiState(
    val isCreatingGroup: Boolean = false,
    val isResolvingConversation: Boolean = false,
    val isResolvingConversationIndicatorVisible: Boolean = false,
    val recipientPickerUiState: RecipientPickerUiState = RecipientPickerUiState(),
    val resolvingRecipientDestination: String? = null,
    val selectedGroupRecipients: ImmutableList<SelectedRecipient> = persistentListOf(),
    val simSelectorState: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
)
