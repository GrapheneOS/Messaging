package com.android.messaging.ui.conversation.messagedetails.mapper

import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.ui.conversation.messagedetails.model.MessageDetailsUiState
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import javax.inject.Inject

internal interface MessageDetailsUiStateMapper {
    fun map(
        message: ConversationMessageData?,
        details: ConversationMessageDetails?,
    ): MessageDetailsUiState
}

internal class MessageDetailsUiStateMapperImpl @Inject constructor(
    private val conversationMessageUiModelMapper: ConversationMessageUiModelMapper,
) : MessageDetailsUiStateMapper {

    override fun map(
        message: ConversationMessageData?,
        details: ConversationMessageDetails?,
    ): MessageDetailsUiState {
        val preview = message?.let(conversationMessageUiModelMapper::map)

        if (preview == null || details == null) {
            return MessageDetailsUiState.Unavailable
        }

        return MessageDetailsUiState.Content(
            preview = preview,
            details = details,
        )
    }
}
