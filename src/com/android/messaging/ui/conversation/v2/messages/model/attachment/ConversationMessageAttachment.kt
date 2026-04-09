package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessagePartUiModel

@Immutable
internal sealed interface ConversationMessageAttachment {
    val key: String

    @Immutable
    data class Media(
        override val key: String,
        val part: ConversationMessagePartUiModel,
    ) : ConversationMessageAttachment

    @Immutable
    data class Unsupported(
        override val key: String,
        val part: ConversationMessagePartUiModel,
    ) : ConversationMessageAttachment

    @Immutable
    data class YouTubePreview(
        override val key: String,
        val sourceUrl: String,
        val thumbnailUrl: String,
    ) : ConversationMessageAttachment
}
