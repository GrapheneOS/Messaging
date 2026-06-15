package com.android.messaging.ui.conversation.messages.model.attachment

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel

@Immutable
internal sealed interface ConversationMessageAttachment {
    val key: String

    @Immutable
    data class Media(
        override val key: String,
        val part: ConversationMessagePartUiModel.Attachment,
    ) : ConversationMessageAttachment

    @Immutable
    data class Unsupported(
        override val key: String,
        val part: ConversationMessagePartUiModel.Attachment,
    ) : ConversationMessageAttachment
}
