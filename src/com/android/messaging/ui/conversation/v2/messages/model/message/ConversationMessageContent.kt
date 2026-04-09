package com.android.messaging.ui.conversation.v2.messages.model.message

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.v2.messages.model.attachment.ConversationMessageAttachment
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ConversationMessageContent(
    val subjectText: String?,
    val bodyText: String?,
    val attachments: ImmutableList<ConversationMessageAttachment>,
    val attachmentSections: ConversationAttachmentSections,
    val isAttachmentOnly: Boolean,
)
