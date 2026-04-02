package com.android.messaging.ui.conversation.v2.messages.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessageUiModel
import com.android.messaging.ui.conversation.v2.messages.model.ConversationMessageUiModel.Status

private const val MESSAGE_BUBBLE_MAX_WIDTH_DP = 360
private const val MESSAGE_BUBBLE_WIDTH_FRACTION = 0.8f
private const val MESSAGE_BUBBLE_CORNER_RADIUS_DP = 24
private const val MESSAGE_BUBBLE_CONNECTED_CORNER_RADIUS_DP = 6

@Composable
internal fun ConversationMessage(
    modifier: Modifier = Modifier,
    message: ConversationMessageUiModel,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val maxBubbleWidth = remember(maxWidth) {
            (maxWidth * MESSAGE_BUBBLE_WIDTH_FRACTION)
                .coerceAtMost(MESSAGE_BUBBLE_MAX_WIDTH_DP.dp)
        }
        val presentation = rememberConversationMessagePresentation(message = message)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = messageHorizontalArrangement(message = message),
        ) {
            ConversationMessageContent(
                message = message,
                presentation = presentation,
                maxBubbleWidth = maxBubbleWidth,
            )
        }
    }
}

@Immutable
private data class ConversationMessagePresentation(
    val bubbleShape: RoundedCornerShape,
    val bodyText: String,
    val metadataText: String?,
    val showSender: Boolean,
)

@Composable
private fun rememberConversationMessagePresentation(
    message: ConversationMessageUiModel,
): ConversationMessagePresentation {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val bubbleShape = remember(
        message.canClusterWithPrevious,
        message.canClusterWithNext,
    ) {
        messageBubbleShape(message = message)
    }

    val bodyText = remember(
        message.text,
        message.mmsSubject,
        message.parts,
    ) {
        buildMessageBody(message = message)
    }

    val statusTextResourceId = remember(message.status) {
        messageStatusTextResourceId(status = message.status)
    }
    val statusText = statusTextResourceId?.let { stringResource(id = it) }

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

    return remember(
        bubbleShape,
        bodyText,
        metadataText,
        showSender,
    ) {
        ConversationMessagePresentation(
            bubbleShape = bubbleShape,
            bodyText = bodyText,
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
    presentation: ConversationMessagePresentation,
    maxBubbleWidth: Dp,
) {
    Column(
        modifier = Modifier.widthIn(max = maxBubbleWidth),
        horizontalAlignment = messageContentHorizontalAlignment(message = message),
    ) {
        ConversationMessageBubble(
            message = message,
            presentation = presentation,
            maxBubbleWidth = maxBubbleWidth,
        )

        ConversationMessageMetadata(
            message = message,
            metadataText = presentation.metadataText,
        )
    }
}

@Composable
private fun ConversationMessageBubble(
    message: ConversationMessageUiModel,
    presentation: ConversationMessagePresentation,
    maxBubbleWidth: Dp,
) {
    Surface(
        color = messageBubbleColor(message = message),
        contentColor = messageBubbleContentColor(message = message),
        shape = presentation.bubbleShape,
        modifier = Modifier.widthIn(max = maxBubbleWidth),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            ConversationMessageSender(
                senderDisplayName = message.senderDisplayName,
                showSender = presentation.showSender,
            )

            Text(
                text = presentation.bodyText,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ConversationMessageSender(
    senderDisplayName: String?,
    showSender: Boolean,
) {
    if (!showSender || senderDisplayName == null) {
        return
    }

    Text(
        text = senderDisplayName,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
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
        text = metadataText,
        style = MaterialTheme.typography.labelSmall,
        color = messageMetadataColor(message = message),
        textAlign = messageMetadataTextAlign(message = message),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
private fun messageBubbleColor(message: ConversationMessageUiModel): Color {
    return when {
        message.isIncoming -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

@Composable
private fun messageBubbleContentColor(message: ConversationMessageUiModel): Color {
    return when {
        message.isIncoming -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
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

private fun buildMessageBody(message: ConversationMessageUiModel): String {
    message
        .text
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    message
        .mmsSubject
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    message
        .parts
        .firstNotNullOfOrNull { part ->
            part.text?.takeIf { it.isNotBlank() }
        }
        ?.let { return it }

    return message.parts.firstOrNull()?.contentType.orEmpty()
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
private fun messageMetadataColor(message: ConversationMessageUiModel): Color {
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
