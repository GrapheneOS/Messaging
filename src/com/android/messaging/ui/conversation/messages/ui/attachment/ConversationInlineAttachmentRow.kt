package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.preview.previewInlineAudioAttachment
import com.android.messaging.ui.conversation.preview.previewInlineFileAttachment
import com.android.messaging.ui.conversation.preview.previewInlineVCardAttachment
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun ConversationInlineAttachmentRow(
    attachment: ConversationInlineAttachment,
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    useStandaloneAudioAttachmentBackground: Boolean,
    onAttachmentClick: OnConversationAttachmentClick,
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

@PreviewLightDark
@Composable
private fun ConversationInlineAttachmentRowPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            listOf(
                previewInlineAudioAttachment(),
                previewInlineVCardAttachment(),
                previewInlineFileAttachment(),
            ).forEach { attachment ->
                ConversationInlineAttachmentRow(
                    attachment = attachment,
                    isIncoming = true,
                    isSelectionMode = false,
                    useStandaloneAudioAttachmentBackground = true,
                    onAttachmentClick = { _, _, _ -> },
                    onExternalUriClick = {},
                )
            }
        }
    }
}
