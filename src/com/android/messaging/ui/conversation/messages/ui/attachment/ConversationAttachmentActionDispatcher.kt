package com.android.messaging.ui.conversation.messages.ui.attachment

import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment

internal fun dispatchConversationAttachmentOpenAction(
    action: ConversationAttachmentOpenAction,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
) {
    when (action) {
        is ConversationAttachmentOpenAction.OpenContent -> {
            onAttachmentClick(
                action.contentType,
                action.contentUri,
                action.partId,
            )
        }

        is ConversationAttachmentOpenAction.OpenExternal -> {
            onExternalUriClick(action.uri)
        }
    }
}

internal fun ConversationMessageAttachment.toConversationAttachmentOpenActionOrNull():
    ConversationAttachmentOpenAction? {
    return when (this) {
        is ConversationMessageAttachment.Media -> {
            ConversationAttachmentOpenAction.OpenContent(
                contentType = part.contentType,
                contentUri = part.contentUri.toString(),
                partId = part.partId,
            )
        }

        is ConversationMessageAttachment.Unsupported -> {
            part.contentUri?.let { contentUri ->
                ConversationAttachmentOpenAction.OpenContent(
                    contentType = part.contentType,
                    contentUri = contentUri.toString(),
                    partId = part.partId,
                )
            }
        }

        is ConversationMessageAttachment.YouTubePreview -> {
            ConversationAttachmentOpenAction.OpenExternal(
                uri = sourceUrl,
            )
        }
    }
}
