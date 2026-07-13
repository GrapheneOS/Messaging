package com.android.messaging.ui.conversation.messagedetails.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel

@Immutable
internal sealed interface MessageDetailsUiState {

    data object Loading : MessageDetailsUiState

    data object Unavailable : MessageDetailsUiState

    @Immutable
    data class Content(
        val preview: ConversationMessageUiModel,
        val details: ConversationMessageDetails,
        val youTubeLinkPreviewsEnabled: Boolean = false,
    ) : MessageDetailsUiState
}
