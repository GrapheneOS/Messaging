package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.attachment.AttachmentPreviewCard
import com.android.messaging.ui.common.components.attachment.AttachmentPreviewRemoveButton
import com.android.messaging.ui.common.components.attachment.AttachmentPreviewRow
import com.android.messaging.ui.common.components.attachment.AudioAttachmentCell
import com.android.messaging.ui.common.components.attachment.MediaAttachmentCell
import com.android.messaging.ui.common.components.attachment.VCardAttachmentCell
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG
import com.android.messaging.ui.conversation.attachment.ui.resolveLtrVCardText
import com.android.messaging.ui.conversation.attachment.ui.toVCardAttachmentKind
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.conversationAttachmentPreviewItemTestTag
import com.android.messaging.ui.conversation.conversationAttachmentPreviewRemoveButtonTestTag
import com.android.messaging.ui.conversation.preview.previewPendingAttachment
import com.android.messaging.ui.conversation.preview.previewPendingAudioAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedAudioAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedFileAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedImageAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedVCardAttachment
import com.android.messaging.ui.conversation.preview.previewResolvedVideoAttachment
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.vcard.rememberVCardAvatarImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val PENDING_ATTACHMENT_CARD_SIZE = 88.dp
private val PENDING_AUDIO_CARD_HEIGHT = 88.dp
private val PENDING_REMOVE_BUTTON_SIZE = 28.dp
private val PENDING_REMOVE_BUTTON_PADDING = 6.dp

@Composable
internal fun ConversationAttachmentPreview(
    modifier: Modifier = Modifier,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    onPendingAttachmentRemove: (String) -> Unit,
    onResolvedAttachmentClick: (ComposerAttachmentUiModel.Resolved) -> Unit,
    onResolvedAttachmentRemove: (String) -> Unit,
) {
    AttachmentPreviewRow(
        attachments = attachments,
        key = { attachment -> attachment.key },
        modifier = modifier.testTag(CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG),
    ) { attachment ->
        when (attachment) {
            is ComposerAttachmentUiModel.Pending.AudioFinalizing -> {
                PendingAudioAttachmentPreviewItem(
                    attachmentKey = attachment.key,
                )
            }

            is ComposerAttachmentUiModel.Pending.Generic -> {
                PendingAttachmentPreviewItem(
                    attachmentKey = attachment.key,
                    onRemoveClick = {
                        onPendingAttachmentRemove(attachment.key)
                    },
                )
            }

            is ComposerAttachmentUiModel.Resolved -> {
                ResolvedAttachmentPreviewItem(
                    attachment = attachment,
                    onAttachmentClick = {
                        onResolvedAttachmentClick(attachment)
                    },
                    onRemoveClick = {
                        onResolvedAttachmentRemove(attachment.contentUri)
                    },
                )
            }
        }
    }
}

@Composable
private fun ResolvedAttachmentPreviewItem(
    attachment: ComposerAttachmentUiModel.Resolved,
    onAttachmentClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val itemTestTag = conversationAttachmentPreviewItemTestTag(
        attachmentKey = attachment.key
    )
    val removeTestTag = conversationAttachmentPreviewRemoveButtonTestTag(
        attachmentKey = attachment.key,
    )

    when (attachment) {
        is ComposerAttachmentUiModel.Resolved.VCard -> {
            val avatarImage = rememberVCardAvatarImage(attachment.vCardUiModel.avatarPhoto)

            VCardAttachmentCell(
                modifier = Modifier.testTag(itemTestTag),
                kind = attachment.vCardUiModel.type.toVCardAttachmentKind(),
                avatarImage = avatarImage,
                displayName = attachment.vCardUiModel.titleText,
                normalizedDestination = attachment.vCardUiModel.normalizedDestination,
                title = resolveLtrVCardText(
                    text = attachment.vCardUiModel.titleText,
                    textResId = attachment.vCardUiModel.titleTextResId,
                ).orEmpty(),
                subtitle = resolveLtrVCardText(
                    text = attachment.vCardUiModel.subtitleText,
                    textResId = attachment.vCardUiModel.subtitleTextResId,
                ),
                onClick = onAttachmentClick,
                onRemove = onRemoveClick,
                removeButtonTestTag = removeTestTag,
            )
        }

        is ComposerAttachmentUiModel.Resolved.Audio -> {
            AudioAttachmentCell(
                modifier = Modifier.testTag(itemTestTag),
                durationMillis = attachment.durationMillis,
                onClick = onAttachmentClick,
                onRemove = onRemoveClick,
                removeButtonTestTag = removeTestTag,
            )
        }

        is ComposerAttachmentUiModel.Resolved.File,
        is ComposerAttachmentUiModel.Resolved.VisualMedia.Image,
        is ComposerAttachmentUiModel.Resolved.VisualMedia.Video,
        -> {
            MediaAttachmentCell(
                modifier = Modifier.testTag(itemTestTag),
                contentUri = attachment.contentUri,
                contentType = attachment.contentType,
                isVideo = attachment is ComposerAttachmentUiModel.Resolved.VisualMedia.Video,
                onClick = onAttachmentClick,
                onRemove = onRemoveClick,
                removeButtonTestTag = removeTestTag,
            )
        }
    }
}

@Composable
private fun PendingAttachmentPreviewItem(
    attachmentKey: String,
    onRemoveClick: () -> Unit,
) {
    AttachmentPreviewCard(
        modifier = Modifier
            .size(size = PENDING_ATTACHMENT_CARD_SIZE)
            .testTag(conversationAttachmentPreviewItemTestTag(attachmentKey = attachmentKey)),
        onClick = {},
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CircularProgressIndicator(
                modifier = Modifier.size(size = 28.dp),
                strokeWidth = 2.dp,
            )
        }

        AttachmentPreviewRemoveButton(
            onClick = onRemoveClick,
            size = PENDING_REMOVE_BUTTON_SIZE,
            modifier = Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(all = PENDING_REMOVE_BUTTON_PADDING)
                .testTag(
                    conversationAttachmentPreviewRemoveButtonTestTag(
                        attachmentKey = attachmentKey,
                    ),
                ),
        )
    }
}

@Composable
private fun PendingAudioAttachmentPreviewItem(attachmentKey: String) {
    AttachmentPreviewCard(
        modifier = Modifier
            .height(height = PENDING_AUDIO_CARD_HEIGHT)
            .wrapContentWidth()
            .testTag(conversationAttachmentPreviewItemTestTag(attachmentKey = attachmentKey)),
        onClick = {},
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(height = PENDING_AUDIO_CARD_HEIGHT)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(size = 40.dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = 20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Text(
                modifier = Modifier.wrapContentWidth(),
                text = stringResource(id = R.string.audio_recording_finalizing_attachment_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationAttachmentPreviewPreview() {
    MessagingPreviewTheme {
        ConversationAttachmentPreview(
            attachments = persistentListOf(
                previewPendingAttachment(),
                previewPendingAudioAttachment(),
                previewResolvedImageAttachment(),
                previewResolvedVideoAttachment(),
                previewResolvedAudioAttachment(),
                previewResolvedFileAttachment(),
                previewResolvedVCardAttachment(),
            ),
            onPendingAttachmentRemove = { _ -> },
            onResolvedAttachmentClick = { _ -> },
            onResolvedAttachmentRemove = { _ -> },
        )
    }
}
