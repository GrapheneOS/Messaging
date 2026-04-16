package com.android.messaging.ui.conversation.v2.recipientpicker.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class RecipientPickerUiState(
    val query: String = "",
    val contacts: ImmutableList<ConversationRecipient> = persistentListOf(),
    val canLoadMore: Boolean = false,
    val hasContactsPermission: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
)
