package com.android.messaging.ui.conversation.v2.messages.model.attachment

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ConversationAttachmentSections(
    val galleryVisualAttachments: ImmutableList<ConversationMessageAttachment>,
    val trailingItems: ImmutableList<ConversationAttachmentItem>,
)

@Immutable
internal sealed interface ConversationAttachmentItem {
    val key: String

    @Immutable
    data class StandaloneVisual(
        override val key: String,
        val attachment: ConversationMessageAttachment,
    ) : ConversationAttachmentItem

    @Immutable
    data class Inline(
        override val key: String,
        val attachment: ConversationInlineAttachment,
    ) : ConversationAttachmentItem
}
