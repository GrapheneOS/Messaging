package com.android.messaging.ui.conversation.messages.ui.message

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.sms.cleanseMmsSubject
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status

private const val MESSAGE_BUBBLE_MAX_WIDTH_DP = 360
private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.8f
private const val MESSAGE_BUBBLE_CORNER_RADIUS_DP = 24
private const val MESSAGE_BUBBLE_CONNECTED_CORNER_RADIUS_DP = 6

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
    onMessageResendClick: () -> Unit = {},
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
                onMessageResendClick = onMessageResendClick,
            )
        }
    }
}

@Immutable
internal data class ConversationMessageLayout(
    val bubbleShape: RoundedCornerShape,
    val bubbleLayoutMode: ConversationMessageBubbleLayoutMode,
    val content: ConversationMessageContent,
    val metadataText: String?,
    val showSender: Boolean,
)

internal enum class ConversationMessageBubbleLayoutMode {
    AttachmentOnlyWithoutSurface,
    AttachmentsInSurface,
    TextInSurface,
}

@Composable
private fun rememberConversationMessageLayout(
    message: ConversationMessageUiModel,
): ConversationMessageLayout {
    val bubbleShape = remember(
        message.canClusterWithPrevious,
        message.canClusterWithNext,
    ) {
        messageBubbleShape(message = message)
    }

    val content = rememberConversationMessageContent(message = message)
    val metadataText = rememberConversationMessageMetadataText(message = message)

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

@Composable
private fun rememberConversationMessageContent(
    message: ConversationMessageUiModel,
): ConversationMessageContent {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
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

    return remember(
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
}

@Composable
private fun rememberConversationMessageMetadataText(
    message: ConversationMessageUiModel,
): String? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val statusTextResourceId = remember(message.status) {
        messageStatusTextResourceId(status = message.status)
    }
    val statusText = statusTextResourceId?.let { stringResource(it) }

    return remember(
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
    onMessageResendClick: () -> Unit,
) {
    val bubbleInteractionModifier = conversationMessageBubbleInteractionModifier(
        message = message,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        layout = layout,
        onMessageClick = onMessageClick,
        onMessageLongClick = onMessageLongClick,
        onMessageResendClick = onMessageResendClick,
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

                    message.canResendMessage -> {
                        onMessageResendClick()
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

                    message.canResendMessage -> {
                        onMessageResendClick()
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
private fun conversationMessageBubbleInteractionModifier(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    return Modifier
        .clip(shape = layout.bubbleShape)
        .semantics {
            selected = isSelected
        }
        .combinedClickable(
            enabled = true,
            onClick = {
                when {
                    isSelectionMode -> {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onMessageClick()
                    }

                    message.canResendMessage -> {
                        onMessageResendClick()
                    }
                }
            },
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onMessageLongClick()
            },
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
    return when {
        !clustersWithAdjacent -> defaultRadius
        useFreeSide -> defaultRadius
        else -> MESSAGE_BUBBLE_CONNECTED_CORNER_RADIUS_DP.dp
    }
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
    return when {
        canClusterWithNext -> null
        timestamp <= 0L -> statusText

        else -> {
            val formattedTime = DateUtils.formatDateTime(
                context,
                timestamp,
                DateUtils.FORMAT_SHOW_TIME,
            )

            buildTimestampMetadataText(
                formattedTime = formattedTime,
                statusText = statusText,
            )
        }
    }
}

private fun buildTimestampMetadataText(
    formattedTime: String,
    statusText: String?,
): String {
    return when (statusText) {
        null -> formattedTime
        else -> "$formattedTime \u2022 $statusText"
    }
}

@Suppress("CyclomaticComplexMethod")
private fun messageStatusTextResourceId(status: Status): Int? {
    return when (status) {
        Status.Outgoing.Delivered -> R.string.delivered_status_content_description
        Status.Outgoing.Sending -> R.string.message_status_sending
        Status.Outgoing.Resending -> R.string.message_status_send_retrying
        Status.Outgoing.AwaitingRetry -> R.string.message_status_failed
        Status.Outgoing.Failed -> R.string.message_status_send_failed
        Status.Outgoing.FailedEmergencyNumber -> {
            R.string.message_status_send_failed_emergency_number
        }
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
