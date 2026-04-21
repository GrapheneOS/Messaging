package com.android.messaging.ui.conversation.v2.messages.ui.message

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.sms.cleanseMmsSubject
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.v2.messages.ui.attachment.ConversationMessageAttachments
import com.android.messaging.ui.conversation.v2.messages.ui.text.ConversationMessageText

private const val MESSAGE_BUBBLE_MAX_WIDTH_DP = 360
private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.8f
private const val MESSAGE_BUBBLE_CORNER_RADIUS_DP = 24
private const val MESSAGE_BUBBLE_CONNECTED_CORNER_RADIUS_DP = 6
private val MESSAGE_BUBBLE_MEDIA_SECTION_SPACING = 8.dp
private val MESSAGE_BUBBLE_MEDIA_TEXT_PADDING = 12.dp
private val MESSAGE_BUBBLE_TEXT_HORIZONTAL_PADDING = 16.dp
private val MESSAGE_BUBBLE_TEXT_VERTICAL_PADDING = 12.dp
private const val MESSAGE_SELECTION_MEDIA_OVERLAY_ALPHA = 0.2f

@Composable
internal fun ConversationMessage(
    modifier: Modifier = Modifier,
    message: ConversationMessageUiModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit = { _, _ -> },
    onExternalUriClick: (String) -> Unit = {},
    onMessageClick: () -> Unit = {},
    onMessageLongClick: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        val maxBubbleWidth = remember(maxWidth) {
            (maxWidth * MESSAGE_BUBBLE_WIDTH_FRACTION)
                .coerceAtMost(MESSAGE_BUBBLE_MAX_WIDTH_DP.dp)
        }
        val layout = rememberConversationMessageLayout(message = message)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = messageHorizontalArrangement(message = message),
        ) {
            ConversationMessageContent(
                message = message,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                layout = layout,
                maxBubbleWidth = maxBubbleWidth,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
            )
        }
    }
}

@Immutable
private data class ConversationMessageLayout(
    val bubbleShape: RoundedCornerShape,
    val bubbleLayoutMode: ConversationMessageBubbleLayoutMode,
    val content: ConversationMessageContent,
    val metadataText: String?,
    val showSender: Boolean,
)

private enum class ConversationMessageBubbleLayoutMode {
    AttachmentOnlyWithoutSurface,
    AttachmentsInSurface,
    TextInSurface,
}

@Composable
private fun rememberConversationMessageLayout(
    message: ConversationMessageUiModel,
): ConversationMessageLayout {
    val context = LocalContext.current
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current

    val bubbleShape = remember(
        message.canClusterWithPrevious,
        message.canClusterWithNext,
    ) {
        messageBubbleShape(message = message)
    }

    val subjectText = remember(
        resources,
        configuration,
        message.mmsSubject,
    ) {
        cleanseMmsSubject(
            resources = resources,
            subject = message.mmsSubject,
        )
    }

    val content = remember(
        message.text,
        message.mmsSubject,
        message.parts,
        subjectText,
    ) {
        buildConversationMessageContent(
            message = message,
            subjectText = subjectText,
        )
    }

    val statusTextResourceId = remember(message.status) {
        messageStatusTextResourceId(status = message.status)
    }
    val statusText = statusTextResourceId?.let { stringResource(it) }

    val metadataText = remember(
        context,
        configuration,
        message.canClusterWithNext,
        message.displayTimestamp,
        statusText,
    ) {
        buildMessageMetadataText(
            context = context,
            canClusterWithNext = message.canClusterWithNext,
            timestamp = message.displayTimestamp,
            statusText = statusText,
        )
    }

    val showSender = remember(
        message.isIncoming,
        message.senderDisplayName,
        message.canClusterWithPrevious,
    ) {
        message.isIncoming &&
            !message.senderDisplayName.isNullOrBlank() &&
            !message.canClusterWithPrevious
    }

    val bubbleLayoutMode = remember(
        content,
        showSender,
    ) {
        buildConversationMessageBubbleLayoutMode(
            content = content,
            showSender = showSender,
        )
    }

    return remember(
        bubbleShape,
        bubbleLayoutMode,
        content,
        metadataText,
        showSender,
    ) {
        ConversationMessageLayout(
            bubbleShape = bubbleShape,
            bubbleLayoutMode = bubbleLayoutMode,
            content = content,
            metadataText = metadataText,
            showSender = showSender,
        )
    }
}

private fun messageHorizontalArrangement(
    message: ConversationMessageUiModel,
): Arrangement.Horizontal {
    return when {
        message.isIncoming -> Arrangement.Start
        else -> Arrangement.End
    }
}

@Composable
private fun ConversationMessageContent(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    maxBubbleWidth: Dp,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val bubbleInteractionModifier = Modifier
        .clip(shape = layout.bubbleShape)
        .semantics {
            selected = isSelected
        }
        .combinedClickable(
            enabled = true,
            onClick = {
                if (isSelectionMode) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onMessageClick()
                }
            },
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onMessageLongClick()
            },
        )

    Column(
        modifier = Modifier.widthIn(max = maxBubbleWidth),
        horizontalAlignment = messageContentHorizontalAlignment(message = message),
    ) {
        ConversationMessageBubble(
            modifier = bubbleInteractionModifier,
            message = message,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            layout = layout,
            maxBubbleWidth = maxBubbleWidth,
            onAttachmentClick = { contentType, contentUri ->
                when {
                    isSelectionMode -> {
                        onMessageClick()
                    }

                    else -> {
                        onAttachmentClick(contentType, contentUri)
                    }
                }
            },
            onExternalUriClick = { uri ->
                when {
                    isSelectionMode -> {
                        onMessageClick()
                    }

                    else -> {
                        onExternalUriClick(uri)
                    }
                }
            },
            onMessageLongClick = onMessageLongClick,
        )

        ConversationMessageMetadata(
            message = message,
            metadataText = layout.metadataText,
        )
    }
}

@Composable
private fun ConversationMessageBubble(
    modifier: Modifier = Modifier,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    maxBubbleWidth: Dp,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    when (layout.bubbleLayoutMode) {
        ConversationMessageBubbleLayoutMode.AttachmentOnlyWithoutSurface -> {
            ConversationMessageAttachmentOnlyContainer(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .then(other = modifier),
                bubbleShape = layout.bubbleShape,
                message = message,
                isSelected = isSelected,
            ) {
                ConversationMessageAttachmentBubbleContent(
                    modifier = Modifier
                        .fillMaxWidth(),
                    content = layout.content,
                    message = message,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    senderDisplayName = message.senderDisplayName,
                    showSender = layout.showSender,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }
        }

        ConversationMessageBubbleLayoutMode.AttachmentsInSurface -> {
            ConversationMessageBubbleSurface(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .then(other = modifier),
                isSelected = isSelected,
                message = message,
                layout = layout,
            ) {
                ConversationMessageAttachmentBubbleContent(
                    content = layout.content,
                    message = message,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    senderDisplayName = message.senderDisplayName,
                    showSender = layout.showSender,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }
        }

        ConversationMessageBubbleLayoutMode.TextInSurface -> {
            ConversationMessageBubbleSurface(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .then(other = modifier),
                isSelected = isSelected,
                message = message,
                layout = layout,
            ) {
                ConversationMessageTextBubbleContent(
                    content = layout.content,
                    message = message,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    senderDisplayName = message.senderDisplayName,
                    showSender = layout.showSender,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageLongClick = onMessageLongClick,
                )
            }
        }
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
    Surface(
        color = messageBubbleColor(
            message = message,
            isSelected = isSelected,
        ),
        contentColor = messageBubbleContentColor(
            message = message,
            isSelected = isSelected,
        ),
        shape = layout.bubbleShape,
        modifier = modifier,
    ) {
        bubbleContent()
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
    content: ConversationMessageContent,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    senderDisplayName: String?,
    showSender: Boolean,
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
            senderDisplayName = senderDisplayName,
            showSender = showSender,
        )

        ConversationMessageBody(
            content = content,
            isIncoming = message.isIncoming,
            isSelectionMode = isSelectionMode,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageLongClick = onMessageLongClick,
        )
    }
}

@Composable
private fun ConversationMessageAttachmentBubbleContent(
    modifier: Modifier = Modifier,
    content: ConversationMessageContent,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    senderDisplayName: String?,
    showSender: Boolean,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    val hasHeader = showSender || !content.subjectText.isNullOrBlank()
    val hasBodyText = !content.bodyText.isNullOrBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        ConversationMessageSender(
            modifier = Modifier.padding(
                start = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                top = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                end = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                bottom = when {
                    content.subjectText.isNullOrBlank() -> 6.dp
                    else -> MESSAGE_BUBBLE_MEDIA_SECTION_SPACING
                },
            ),
            color = messageSenderColor(
                message = message,
                isSelected = isSelected,
            ),
            senderDisplayName = senderDisplayName,
            showSender = showSender,
        )

        content.subjectText?.let { subjectText ->
            Text(
                modifier = Modifier.padding(
                    start = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                    end = MESSAGE_BUBBLE_MEDIA_TEXT_PADDING,
                    bottom = MESSAGE_BUBBLE_MEDIA_SECTION_SPACING,
                ),
                text = subjectText,
                style = MaterialTheme.typography.titleSmall,
            )
        }

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
            )
        }
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
    content.subjectText?.let { subjectText ->
        Text(
            text = subjectText,
            style = MaterialTheme.typography.titleSmall,
        )
    }

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
        )
    }
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
private fun ConversationMessageMetadata(
    message: ConversationMessageUiModel,
    metadataText: String?,
) {
    if (metadataText == null) {
        return
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        text = metadataText,
        style = MaterialTheme.typography.labelSmall,
        color = messageMetadataColor(message = message),
        textAlign = messageMetadataTextAlign(message = message),
    )
}

private fun messageContentHorizontalAlignment(
    message: ConversationMessageUiModel,
): Alignment.Horizontal {
    return when {
        message.isIncoming -> Alignment.Start
        else -> Alignment.End
    }
}

private fun messageMetadataTextAlign(message: ConversationMessageUiModel): TextAlign {
    return when {
        message.isIncoming -> TextAlign.Start
        else -> TextAlign.End
    }
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

private fun messageBubbleShape(message: ConversationMessageUiModel): RoundedCornerShape {
    val cornerRadius = MESSAGE_BUBBLE_CORNER_RADIUS_DP.dp

    val topStartCornerRadius = clusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithPrevious,
    )
    val topEndCornerRadius = clusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithPrevious,
        useFreeSide = true,
        defaultRadius = cornerRadius,
    )
    val bottomStartCornerRadius = clusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithNext,
    )
    val bottomEndCornerRadius = clusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithNext,
        useFreeSide = true,
        defaultRadius = cornerRadius,
    )

    return RoundedCornerShape(
        topStart = if (message.isIncoming) topStartCornerRadius else topEndCornerRadius,
        topEnd = if (message.isIncoming) topEndCornerRadius else topStartCornerRadius,
        bottomStart = if (message.isIncoming) bottomStartCornerRadius else bottomEndCornerRadius,
        bottomEnd = if (message.isIncoming) bottomEndCornerRadius else bottomStartCornerRadius,
    )
}

private fun clusteredCornerRadius(
    clustersWithAdjacent: Boolean,
    useFreeSide: Boolean = false,
    defaultRadius: Dp = MESSAGE_BUBBLE_CORNER_RADIUS_DP.dp,
): Dp {
    if (!clustersWithAdjacent) {
        return defaultRadius
    }

    if (useFreeSide) {
        return defaultRadius
    }

    return MESSAGE_BUBBLE_CONNECTED_CORNER_RADIUS_DP.dp
}

private fun buildConversationMessageBubbleLayoutMode(
    content: ConversationMessageContent,
    showSender: Boolean,
): ConversationMessageBubbleLayoutMode {
    val hasAttachments = content.attachments.isNotEmpty()
    if (!hasAttachments) {
        return ConversationMessageBubbleLayoutMode.TextInSurface
    }

    val hasAttachmentHeaderOrFooter = showSender ||
        !content.subjectText.isNullOrBlank() ||
        !content.bodyText.isNullOrBlank()

    return when {
        content.isAttachmentOnly && !hasAttachmentHeaderOrFooter -> {
            ConversationMessageBubbleLayoutMode.AttachmentOnlyWithoutSurface
        }
        else -> ConversationMessageBubbleLayoutMode.AttachmentsInSurface
    }
}

private fun buildMessageMetadataText(
    context: Context,
    canClusterWithNext: Boolean,
    timestamp: Long,
    statusText: String?,
): String? {
    if (canClusterWithNext) {
        return null
    }

    if (timestamp <= 0L) {
        return statusText
    }

    val formattedTime = DateUtils.formatDateTime(
        context,
        timestamp,
        DateUtils.FORMAT_SHOW_TIME,
    )

    if (statusText == null) {
        return formattedTime
    }

    return "$formattedTime \u2022 $statusText"
}

private fun messageStatusTextResourceId(status: Status): Int? {
    return when (status) {
        Status.Outgoing.Delivered -> R.string.delivered_status_content_description
        Status.Outgoing.Sending -> R.string.message_status_sending
        Status.Outgoing.Resending -> R.string.message_status_send_retrying
        Status.Outgoing.AwaitingRetry -> R.string.message_status_failed
        Status.Outgoing.Failed -> R.string.message_status_failed
        Status.Outgoing.FailedEmergencyNumber -> R.string.message_status_failed
        Status.Incoming.YetToManualDownload -> R.string.message_status_download
        Status.Incoming.RetryingManualDownload -> R.string.message_status_downloading
        Status.Incoming.ManualDownloading -> R.string.message_status_downloading
        Status.Incoming.RetryingAutoDownload -> R.string.message_status_downloading
        Status.Incoming.AutoDownloading -> R.string.message_status_downloading
        Status.Incoming.DownloadFailed -> R.string.message_status_download_failed
        Status.Incoming.ExpiredOrNotAvailable -> R.string.message_status_download_error
        else -> null
    }
}

@Composable
private fun messageMetadataColor(
    message: ConversationMessageUiModel,
): Color {
    return when (message.status) {
        Status.Outgoing.AwaitingRetry,
        Status.Outgoing.Failed,
        Status.Outgoing.FailedEmergencyNumber,
        Status.Incoming.DownloadFailed,
        Status.Incoming.ExpiredOrNotAvailable,
        -> MaterialTheme.colorScheme.error

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
