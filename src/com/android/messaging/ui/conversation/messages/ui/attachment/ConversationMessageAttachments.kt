package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.preview.previewAttachmentSections
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun ConversationMessageAttachments(
    modifier: Modifier = Modifier,
    attachmentSections: ConversationAttachmentSections,
    hasTextAboveVisualAttachments: Boolean,
    hasTextBelowVisualAttachments: Boolean,
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    useStandaloneAudioAttachmentBg: Boolean,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    val hasGalleryVisualAttachments = attachmentSections.galleryVisualAttachments.isNotEmpty()
    val hasTrailingItems = attachmentSections.trailingItems.isNotEmpty()

    if (!hasGalleryVisualAttachments && !hasTrailingItems) {
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        if (hasGalleryVisualAttachments) {
            ConversationGalleryVisualAttachments(
                attachments = attachmentSections.galleryVisualAttachments,
                hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageLongClick = onMessageLongClick,
            )
        }

        attachmentSections.trailingItems.forEach { trailingItem ->
            when (trailingItem) {
                is ConversationAttachmentItem.Inline -> {
                    ConversationInlineAttachmentRow(
                        attachment = trailingItem.attachment,
                        isIncoming = isIncoming,
                        isSelectionMode = isSelectionMode,
                        useStandaloneAudioAttachmentBackground = useStandaloneAudioAttachmentBg,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onLongClick = onMessageLongClick,
                    )
                }

                is ConversationAttachmentItem.StandaloneVisual -> {
                    ConversationStandaloneVisualAttachment(
                        attachment = trailingItem.attachment,
                        hasTextAboveVisualAttachments = hasTextAboveVisualAttachments,
                        hasTextBelowVisualAttachments = hasTextBelowVisualAttachments,
                        onAttachmentClick = onAttachmentClick,
                        onExternalUriClick = onExternalUriClick,
                        onMessageLongClick = onMessageLongClick,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageAttachmentsPreview() {
    MessagingPreviewColumn {
        ConversationMessageAttachments(
            attachmentSections = previewAttachmentSections(),
            hasTextAboveVisualAttachments = true,
            hasTextBelowVisualAttachments = true,
            isIncoming = true,
            isSelectionMode = false,
            useStandaloneAudioAttachmentBg = true,
            onAttachmentClick = { _, _, _ -> },
            onExternalUriClick = {},
            onMessageLongClick = {},
        )
    }
}
