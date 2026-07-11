package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationMessageAttachments
import com.android.messaging.ui.conversation.messages.ui.text.ConversationMessageText
import com.android.messaging.ui.conversation.messages.ui.text.LocalConversationMessageLinkColor
import com.android.messaging.ui.conversation.preview.previewAudioPart
import com.android.messaging.ui.conversation.preview.previewFilePart
import com.android.messaging.ui.conversation.preview.previewImagePart
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.conversation.preview.previewMmsDownloadUiModel
import com.android.messaging.ui.conversation.preview.previewOutgoingMessage
import com.android.messaging.ui.conversation.preview.previewVCardPart
import com.android.messaging.ui.conversation.preview.previewVideoPart
import kotlinx.collections.immutable.persistentListOf

private val MESSAGE_BUBBLE_MEDIA_SECTION_SPACING = 8.dp
private val MESSAGE_BUBBLE_MEDIA_TEXT_PADDING = 12.dp
private val MESSAGE_BUBBLE_TEXT_HORIZONTAL_PADDING = 16.dp
private val MESSAGE_BUBBLE_TEXT_VERTICAL_PADDING = 12.dp
private const val MESSAGE_SELECTION_MEDIA_OVERLAY_ALPHA = 0.2f

@Composable
internal fun ConversationMessageBubble(
    modifier: Modifier = Modifier,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    maxBubbleWidth: Dp,
    simDisplayName: String?,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    val bubbleModifier = Modifier
        .widthIn(max = maxBubbleWidth)
        .then(other = modifier)

    when (layout.bubbleLayoutMode) {
        ConversationMessageBubbleLayoutMode.AttachmentOnlyWithoutSurface -> {
            ConversationMessageAttachmentOnlyContainer(
                modifier = bubbleModifier,
                bubbleShape = layout.bubbleShape,
                message = message,
                isSelected = isSelected,
            ) {
                ConversationMessageAttachmentBubbleContent(
                    modifier = Modifier.fillMaxWidth(),
                    layout = layout,
                    message = message,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }
        }

        ConversationMessageBubbleLayoutMode.AttachmentsInSurface -> {
            ConversationMessageAttachmentSurfaceBubble(
                modifier = bubbleModifier,
                layout = layout,
                isSelected = isSelected,
                message = message,
                isSelectionMode = isSelectionMode,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageLongClick = onMessageLongClick,
            )
        }

        ConversationMessageBubbleLayoutMode.TextInSurface -> {
            ConversationMessageTextSurfaceBubble(
                modifier = bubbleModifier,
                layout = layout,
                isSelected = isSelected,
                message = message,
                isSelectionMode = isSelectionMode,
                simDisplayName = simDisplayName,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageLongClick = onMessageLongClick,
            )
        }
    }
}

@Composable
private fun ConversationMessageAttachmentSurfaceBubble(
    modifier: Modifier,
    layout: ConversationMessageLayout,
    isSelected: Boolean,
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    ConversationMessageBubbleSurface(
        modifier = modifier,
        isSelected = isSelected,
        message = message,
        layout = layout,
    ) {
        ConversationMessageAttachmentBubbleContent(
            layout = layout,
            message = message,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageLongClick = onMessageLongClick,
        )
    }
}

@Composable
private fun ConversationMessageTextSurfaceBubble(
    modifier: Modifier,
    layout: ConversationMessageLayout,
    isSelected: Boolean,
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    simDisplayName: String?,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    ConversationMessageBubbleSurface(
        modifier = modifier,
        isSelected = isSelected,
        message = message,
        layout = layout,
    ) {
        ConversationMessageTextBubbleContent(
            layout = layout,
            message = message,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            simDisplayName = simDisplayName,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageLongClick = onMessageLongClick,
        )
    }
}

@Composable
private fun ConversationMessageBubbleSurface(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    message: ConversationMessageUiModel,
    layout: ConversationMessageLayout,
    bubbleContent: @Composable () -> Unit,
) {
    val contentColor = messageBubbleContentColor(
        message = message,
        isSelected = isSelected,
    )

    Surface(
        color = messageBubbleColor(
            message = message,
            isSelected = isSelected,
        ),
        contentColor = contentColor,
        shape = layout.bubbleShape,
        modifier = modifier,
    ) {
        CompositionLocalProvider(
            LocalConversationMessageLinkColor provides contentColor,
        ) {
            bubbleContent()
        }
    }
}

@Composable
private fun ConversationMessageAttachmentOnlyContainer(
    modifier: Modifier = Modifier,
    bubbleShape: RoundedCornerShape,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    content: @Composable () -> Unit,
) {
    val overlayColor by animateColorAsState(
        targetValue = when {
            isSelected -> {
                messageBubbleColor(
                    message = message,
                    isSelected = true,
                ).copy(alpha = MESSAGE_SELECTION_MEDIA_OVERLAY_ALPHA)
            }

            else -> Color.Transparent
        },
        label = "conversationMessageSelectionOverlayColor",
    )

    Box(
        modifier = modifier.clip(shape = bubbleShape),
    ) {
        content()

        if (overlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = bubbleShape)
                    .background(color = overlayColor),
            )
        }
    }
}

@Composable
private fun ConversationMessageTextBubbleContent(
    layout: ConversationMessageLayout,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    simDisplayName: String?,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            horizontal = MESSAGE_BUBBLE_TEXT_HORIZONTAL_PADDING,
            vertical = MESSAGE_BUBBLE_TEXT_VERTICAL_PADDING,
        ),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        ConversationMessageSender(
            color = messageSenderColor(
                message = message,
                isSelected = isSelected,
            ),
            senderDisplayName = message.senderDisplayName,
            showSender = layout.showSender,
        )

        when {
            message.mmsDownload != null -> {
                ConversationMessageSubject(subjectText = layout.content.subjectText)

                ConversationMmsDownloadBody(
                    download = message.mmsDownload,
                    canDownloadMessage = message.canDownloadMessage,
                    isSelected = isSelected,
                    contentColor = messageBubbleContentColor(
                        message = message,
                        isSelected = isSelected,
                    ),
                    simDisplayName = simDisplayName,
                )
            }

            else -> {
                ConversationMessageBody(
                    content = layout.content,
                    isIncoming = message.isIncoming,
                    isSelectionMode = isSelectionMode,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }
        }
    }
}

@Composable
private fun ConversationMessageAttachmentBubbleContent(
    modifier: Modifier = Modifier,
    layout: ConversationMessageLayout,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    val content = layout.content
    val hasHeader = layout.showSender || !content.subjectText.isNullOrBlank()
    val hasBodyText = !content.bodyText.isNullOrBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        ConversationMessageSender(
            modifier = Modifier.padding(
                start = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                top = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                end = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                bottom = conversationMessageSenderBottomPadding(content),
            ),
            color = messageSenderColor(
                message = message,
                isSelected = isSelected,
            ),
            senderDisplayName = message.senderDisplayName,
            showSender = layout.showSender,
        )

        ConversationMessageSubject(
            modifier = Modifier.padding(
                start = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                top = conversationMessageSubjectTopPadding(showSender = layout.showSender),
                end = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                bottom = MESSAGE_BUBBLE_MEDIA_SECTION_SPACING,
            ),
            subjectText = content.subjectText,
        )

        ConversationMessageAttachments(
            attachmentSections = content.attachmentSections,
            hasTextAboveVisualAttachments = hasHeader,
            hasTextBelowVisualAttachments = hasBodyText,
            isIncoming = message.isIncoming,
            isSelectionMode = isSelectionMode,
            useStandaloneAudioAttachmentBg = false,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageLongClick = onMessageLongClick,
        )

        content.bodyText?.let { bodyText ->
            ConversationMessageText(
                modifier = Modifier.padding(
                    start = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                    top = MESSAGE_BUBBLE_MEDIA_SECTION_SPACING,
                    end = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                    bottom = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                ),
                text = bodyText,
                style = MaterialTheme.typography.bodyLarge,
                onExternalUriClick = onExternalUriClick,
                onMessageLongClick = onMessageLongClick,
            )
        }
    }
}

private fun conversationMessageSenderBottomPadding(
    content: ConversationMessageContent,
): Dp {
    return when {
        content.subjectText.isNullOrBlank() -> 6.dp
        else -> MESSAGE_BUBBLE_MEDIA_SECTION_SPACING
    }
}

private fun conversationMessageSubjectTopPadding(showSender: Boolean): Dp {
    return when {
        showSender -> 0.dp
        else -> MESSAGE_BUBBLE_MEDIA_TEXT_PADDING
    }
}

@Composable
private fun ConversationMessageBody(
    content: ConversationMessageContent,
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    ConversationMessageSubject(subjectText = content.subjectText)

    ConversationMessageAttachments(
        attachmentSections = content.attachmentSections,
        hasTextAboveVisualAttachments = false,
        hasTextBelowVisualAttachments = false,
        isIncoming = isIncoming,
        isSelectionMode = isSelectionMode,
        useStandaloneAudioAttachmentBg = true,
        onAttachmentClick = onAttachmentClick,
        onExternalUriClick = onExternalUriClick,
        onMessageLongClick = onMessageLongClick,
    )

    content.bodyText?.let { bodyText ->
        ConversationMessageText(
            text = bodyText,
            style = MaterialTheme.typography.bodyLarge,
            onExternalUriClick = onExternalUriClick,
            onMessageLongClick = onMessageLongClick,
        )
    }
}

@Composable
private fun ConversationMessageSubject(
    subjectText: String?,
    modifier: Modifier = Modifier,
) {
    if (subjectText.isNullOrBlank()) {
        return
    }

    Text(
        modifier = modifier,
        text = subjectText,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun ConversationMessageSender(
    modifier: Modifier = Modifier,
    color: Color,
    senderDisplayName: String?,
    showSender: Boolean,
) {
    if (!showSender || senderDisplayName == null) {
        return
    }

    Text(
        modifier = modifier,
        text = senderDisplayName,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun messageBubbleColor(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
): Color {
    return when {
        isSelected -> MaterialTheme.colorScheme.primary
        message.isIncoming -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

@Composable
private fun messageBubbleContentColor(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
): Color {
    return when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        message.isIncoming -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
}

@Composable
private fun messageSenderColor(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
): Color {
    return when {
        isSelected -> {
            messageBubbleContentColor(
                message = message,
                isSelected = true,
            )
        }

        message.isIncoming -> MaterialTheme.colorScheme.primary

        else -> {
            messageBubbleContentColor(
                message = message,
                isSelected = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageBubbleTextPreview() {
    ConversationMessageBubblePreviewColumn {
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-short",
                text = "Incoming text with sender.",
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-long",
                text = PREVIEW_LONG_BUBBLE_TEXT,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            showSender = false,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-link",
                text = "Outgoing text with link https://example.com/support.",
                status = Status.Outgoing.Delivered,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-overflow",
                text = PREVIEW_OVERFLOW_BUBBLE_TEXT,
                status = Status.Outgoing.Sending,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-selected",
                text = "Selected outgoing text in selection mode.",
                status = Status.Outgoing.Failed,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-selection-mode",
                text = "Incoming text while selection mode is active.",
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            isSelectionMode = true,
            simDisplayName = "Personal",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageBubbleMmsDownloadPreview() {
    ConversationMessageBubblePreviewColumn {
        conversationMessageBubbleMmsDownloadPreviewItems().forEach { item ->
            ConversationMessageBubblePreviewItem(
                message = previewIncomingMessage(
                    messageId = item.messageId,
                    text = null,
                    parts = persistentListOf(),
                    status = item.status,
                    mmsDownload = previewMmsDownloadUiModel(state = item.downloadState),
                    protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
                    canDownloadMessage = item.canDownloadMessage,
                ).copy(
                    mmsSubject = "Weekend photos",
                ),
                bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
                isSelected = item.isSelected,
                simDisplayName = "Personal",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageBubbleAttachmentSurfacePreview() {
    ConversationMessageBubblePreviewColumn {
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-media-subject",
                text = "Body text below the media with https://example.com/trip.",
                parts = persistentListOf(
                    previewImagePart(text = "Image caption"),
                    previewVideoPart(text = "Video caption"),
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ).copy(
                mmsSubject = "Trip media",
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentsInSurface,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-audio",
                text = null,
                parts = persistentListOf(previewAudioPart(text = "Voice note caption")),
                status = Status.Outgoing.Delivered,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentsInSurface,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-vcard",
                text = null,
                parts = persistentListOf(previewVCardPart()),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentsInSurface,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-file",
                text = null,
                parts = persistentListOf(previewFilePart(text = "Unsupported PDF attachment")),
                status = Status.Outgoing.Failed,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentsInSurface,
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-youtube",
                text = "Watch this: https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                status = Status.Outgoing.Delivered,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentsInSurface,
            simDisplayName = "Personal",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageBubbleAttachmentOnlyPreview() {
    ConversationMessageBubblePreviewColumn {
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-image-only",
                text = null,
                parts = persistentListOf(previewImagePart(text = null)),
                status = Status.Outgoing.Complete,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentOnlyWithoutSurface,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-image-only",
                text = null,
                parts = persistentListOf(previewImagePart(text = null)),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentOnlyWithoutSurface,
            showSender = false,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-video-only-selected",
                text = null,
                parts = persistentListOf(previewVideoPart(text = null)),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.AttachmentOnlyWithoutSurface,
            isSelected = true,
            isSelectionMode = true,
            showSender = false,
            simDisplayName = "Personal",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageBubbleClusterShapePreview() {
    ConversationMessageBubblePreviewColumn {
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-cluster-start",
                text = "Incoming cluster start.",
            ).copy(
                canClusterWithNext = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-cluster-middle",
                text = "Incoming cluster middle.",
            ).copy(
                canClusterWithPrevious = true,
                canClusterWithNext = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            showSender = false,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewIncomingMessage(
                messageId = "bubble-incoming-cluster-end",
                text = "Incoming cluster end.",
            ).copy(
                canClusterWithPrevious = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            showSender = false,
            simDisplayName = "Personal",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-cluster-start",
                text = "Outgoing cluster start.",
            ).copy(
                canClusterWithNext = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            simDisplayName = "Work",
        )
        ConversationMessageBubblePreviewItem(
            message = previewOutgoingMessage(
                messageId = "bubble-outgoing-cluster-end",
                text = "Outgoing cluster end.",
            ).copy(
                canClusterWithPrevious = true,
            ),
            bubbleLayoutMode = ConversationMessageBubbleLayoutMode.TextInSurface,
            simDisplayName = "Work",
        )
    }
}
