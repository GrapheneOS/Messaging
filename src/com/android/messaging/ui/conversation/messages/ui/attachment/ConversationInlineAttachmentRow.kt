package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.runtime.Composable
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment

@Composable
internal fun ConversationInlineAttachmentRow(
    attachment: ConversationInlineAttachment,
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    useStandaloneAudioAttachmentBackground: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onLongClick: () -> Unit = {},
) {
    when (attachment) {
        is ConversationInlineAttachment.Audio -> {
            ConversationInlineAudioAttachmentRow(
                attachment = attachment,
                isIncoming = isIncoming,
                isSelectionMode = isSelectionMode,
                useStandaloneAudioAttachmentBackground = useStandaloneAudioAttachmentBackground,
                onLongClick = onLongClick,
            )
        }

        is ConversationInlineAttachment.VCard -> {
            ConversationVCardInlineAttachmentRow(
                attachment = attachment,
                isSelectionMode = isSelectionMode,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onLongClick = onLongClick,
            )
        }

        is ConversationInlineAttachment.File -> {
            ConversationGenericInlineAttachmentRow(
                attachment = attachment,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onLongClick = onLongClick,
            )
        }
    }
}
