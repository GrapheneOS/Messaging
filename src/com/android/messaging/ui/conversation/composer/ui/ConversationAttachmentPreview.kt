package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.android.messaging.ui.common.components.composer.AttachmentPreviewRemoveButton
import com.android.messaging.ui.common.components.composer.AttachmentPreviewRow
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.attachment.ui.ConversationMediaThumbnail
import com.android.messaging.ui.conversation.attachment.ui.ConversationVCardAttachmentCardContent
import com.android.messaging.ui.conversation.audio.formatConversationAudioDuration
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ATTACHMENT_PREVIEW_CARD_HEIGHT = 88.dp
private val ATTACHMENT_PREVIEW_CARD_WIDTH = 220.dp
private val ATTACHMENT_PREVIEW_AUDIO_REMOVE_BUTTON_MARGIN = 4.dp
private val ATTACHMENT_PREVIEW_AUDIO_REMOVE_BUTTON_SIZE = 24.dp
private const val ATTACHMENT_PREVIEW_SIZE_PX = 256

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
                    attachmentKey = attachment.key,
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
private fun PendingAttachmentPreviewItem(
    attachmentKey: String,
    onRemoveClick: () -> Unit,
) {
    AttachmentPreviewItemContainer(
        modifier = Modifier.size(88.dp),
        attachmentKey = attachmentKey,
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
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
        }

        RemoveAttachmentButton(
            attachmentKey = attachmentKey,
            onClick = onRemoveClick,
        )
    }
}

@Composable
private fun PendingAudioAttachmentPreviewItem(
    attachmentKey: String,
) {
    AttachmentPreviewItemContainer(
        modifier = Modifier
            .height(height = ATTACHMENT_PREVIEW_CARD_HEIGHT)
            .wrapContentWidth(),
        attachmentKey = attachmentKey,
        onClick = {},
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(height = ATTACHMENT_PREVIEW_CARD_HEIGHT)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp),
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

@Composable
private fun ResolvedAttachmentPreviewItem(
    attachment: ComposerAttachmentUiModel.Resolved,
    attachmentKey: String,
    onAttachmentClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    when (attachment) {
        is ComposerAttachmentUiModel.Resolved.VCard -> {
            ConversationVCardAttachmentPreviewItem(
                attachmentKey = attachmentKey,
                uiModel = attachment.vCardUiModel,
                onAttachmentClick = onAttachmentClick,
                onRemoveClick = onRemoveClick,
            )
        }

        is ComposerAttachmentUiModel.Resolved.Audio -> {
            ConversationAudioAttachmentPreviewItem(
                attachmentKey = attachmentKey,
                durationMillis = attachment.durationMillis,
                onAttachmentClick = onAttachmentClick,
                onRemoveClick = onRemoveClick,
            )
        }

        is ComposerAttachmentUiModel.Resolved.File,
        is ComposerAttachmentUiModel.Resolved.VisualMedia.Image,
        is ComposerAttachmentUiModel.Resolved.VisualMedia.Video,
        -> {
            ConversationResolvedAttachmentThumbnailPreviewItem(
                attachment = attachment,
                attachmentKey = attachmentKey,
                onAttachmentClick = onAttachmentClick,
                onRemoveClick = onRemoveClick,
            )
        }
    }
}

@Composable
private fun ConversationResolvedAttachmentThumbnailPreviewItem(
    attachment: ComposerAttachmentUiModel.Resolved,
    attachmentKey: String,
    onAttachmentClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val thumbnailSize = IntSize(
        width = ATTACHMENT_PREVIEW_SIZE_PX,
        height = ATTACHMENT_PREVIEW_SIZE_PX,
    )

    AttachmentPreviewItemContainer(
        modifier = Modifier.size(90.dp),
        attachmentKey = attachmentKey,
        onClick = onAttachmentClick,
    ) {
        ConversationMediaThumbnail(
            modifier = Modifier.fillMaxSize(),
            contentUri = attachment.contentUri,
            contentType = attachment.contentType,
            size = thumbnailSize,
        )

        if (attachment is ComposerAttachmentUiModel.Resolved.VisualMedia.Video) {
            VideoAttachmentOverlay()
        }

        RemoveAttachmentButton(
            attachmentKey = attachmentKey,
            onClick = onRemoveClick,
        )
    }
}

@Composable
private fun VideoAttachmentOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ConversationAudioAttachmentPreviewItem(
    attachmentKey: String,
    durationMillis: Long,
    onAttachmentClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    AttachmentPreviewItemContainer(
        modifier = Modifier
            .height(height = ATTACHMENT_PREVIEW_CARD_HEIGHT)
            .wrapContentWidth(),
        attachmentKey = attachmentKey,
        onClick = onAttachmentClick,
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(height = ATTACHMENT_PREVIEW_CARD_HEIGHT)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )

            Text(
                modifier = Modifier.wrapContentWidth(),
                text = formatConversationAudioDuration(durationMillis = durationMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

            Box(
                modifier = Modifier
                    .width(
                        width = ATTACHMENT_PREVIEW_AUDIO_REMOVE_BUTTON_SIZE +
                            ATTACHMENT_PREVIEW_AUDIO_REMOVE_BUTTON_MARGIN,
                    )
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                InlineAudioRemoveAttachmentButton(
                    attachmentKey = attachmentKey,
                    onClick = onRemoveClick,
                )
            }
        }
    }
}

@Composable
private fun ConversationVCardAttachmentPreviewItem(
    attachmentKey: String,
    uiModel: ConversationVCardAttachmentUiModel,
    onAttachmentClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    AttachmentPreviewItemContainer(
        modifier = Modifier.size(
            width = ATTACHMENT_PREVIEW_CARD_WIDTH,
            height = ATTACHMENT_PREVIEW_CARD_HEIGHT,
        ),
        attachmentKey = attachmentKey,
        onClick = onAttachmentClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            ConversationVCardAttachmentCardContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(alignment = Alignment.CenterStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                type = uiModel.type,
                avatarUri = uiModel.avatarUri,
                titleText = uiModel.titleText,
                titleTextResId = uiModel.titleTextResId,
                subtitleText = uiModel.subtitleText,
                subtitleTextResId = uiModel.subtitleTextResId,
            )

            RemoveAttachmentButton(
                attachmentKey = attachmentKey,
                onClick = onRemoveClick,
            )
        }
    }
}

@Composable
private fun AttachmentPreviewItemContainer(
    modifier: Modifier = Modifier,
    attachmentKey: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(
                conversationAttachmentPreviewItemTestTag(
                    attachmentKey = attachmentKey,
                ),
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(content = content)
    }
}

@Composable
private fun BoxScope.RemoveAttachmentButton(
    attachmentKey: String,
    onClick: () -> Unit,
) {
    AttachmentPreviewRemoveButton(
        onClick = onClick,
        size = 28.dp,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .testTag(
                conversationAttachmentPreviewRemoveButtonTestTag(
                    attachmentKey = attachmentKey,
                ),
            ),
    )
}

@Composable
private fun InlineAudioRemoveAttachmentButton(
    attachmentKey: String,
    onClick: () -> Unit,
) {
    AttachmentPreviewRemoveButton(
        onClick = onClick,
        size = ATTACHMENT_PREVIEW_AUDIO_REMOVE_BUTTON_SIZE,
        modifier = Modifier.testTag(
            conversationAttachmentPreviewRemoveButtonTestTag(
                attachmentKey = attachmentKey,
            ),
        ),
    )
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
