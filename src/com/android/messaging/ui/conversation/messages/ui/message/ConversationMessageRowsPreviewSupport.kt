package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel

private val PREVIEW_ROWS_BUBBLE_CONNECTED_CORNER_RADIUS = 6.dp
private val PREVIEW_ROWS_BUBBLE_CORNER_RADIUS = 24.dp

internal const val PREVIEW_ROWS_LONG_TEXT =
    "This row preview uses a longer body so the row has to preserve alignment, " +
        "bubble width, metadata placement, and avatar spacing while the content wraps " +
        "across several lines inside the message bubble."

internal const val PREVIEW_ROWS_OVERFLOW_TEXT =
    "SupercalifragilisticexpialidociousRowsPreviewTokenWithoutNaturalBreaks1234567890"

internal data class RowsStatusPreviewItem(
    val messageId: String,
    val text: String,
    val status: Status,
    val metadataText: String?,
)

internal data class RowsMmsDownloadPreviewItem(
    val messageId: String,
    val status: Status.Incoming,
    val downloadState: MmsDownloadUiModel.State,
    val canDownloadMessage: Boolean,
    val isSelected: Boolean = false,
    val isSelectionMode: Boolean = false,
)

@Suppress("LongMethod")
internal fun conversationMessageRowsOutgoingStatusPreviewItems(): List<RowsStatusPreviewItem> {
    return listOf(
        RowsStatusPreviewItem(
            messageId = "rows-status-complete",
            text = "Complete outgoing row.",
            status = Status.Outgoing.Complete,
            metadataText = "18:01",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-delivered",
            text = "Delivered outgoing row.",
            status = Status.Outgoing.Delivered,
            metadataText = "18:02 \u2022 Delivered",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-draft",
            text = "Draft outgoing row.",
            status = Status.Outgoing.Draft,
            metadataText = "18:03",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-yet-to-send",
            text = "Queued outgoing row.",
            status = Status.Outgoing.YetToSend,
            metadataText = "18:04 \u2022 Sending",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-sending",
            text = "Sending outgoing row.",
            status = Status.Outgoing.Sending,
            metadataText = "18:05 \u2022 Sending",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-resending",
            text = "Retrying outgoing row.",
            status = Status.Outgoing.Resending,
            metadataText = "18:06 \u2022 Retrying",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-awaiting-retry",
            text = "Awaiting retry outgoing row.",
            status = Status.Outgoing.AwaitingRetry,
            metadataText = "18:07 \u2022 Failed",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-failed",
            text = "Failed outgoing row.",
            status = Status.Outgoing.Failed,
            metadataText = "18:08 \u2022 Failed",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-failed-emergency",
            text = "Emergency-number failure row.",
            status = Status.Outgoing.FailedEmergencyNumber,
            metadataText = "18:09 \u2022 Failed",
        ),
        RowsStatusPreviewItem(
            messageId = "rows-status-unknown",
            text = "Unknown outgoing row.",
            status = Status.Unknown,
            metadataText = "18:10",
        ),
    )
}

internal fun conversationMessageRowsMmsDownloadPreviewItems(): List<RowsMmsDownloadPreviewItem> {
    return listOf(
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-awaiting",
            status = Status.Incoming.YetToManualDownload,
            downloadState = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = true,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-awaiting-disabled",
            status = Status.Incoming.YetToManualDownload,
            downloadState = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = false,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-downloading",
            status = Status.Incoming.ManualDownloading,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
            isSelected = true,
            isSelectionMode = true,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-auto-downloading",
            status = Status.Incoming.AutoDownloading,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-retrying",
            status = Status.Incoming.RetryingManualDownload,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-auto-retrying",
            status = Status.Incoming.RetryingAutoDownload,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-failed",
            status = Status.Incoming.DownloadFailed,
            downloadState = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = true,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-failed-disabled",
            status = Status.Incoming.DownloadFailed,
            downloadState = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = false,
        ),
        RowsMmsDownloadPreviewItem(
            messageId = "rows-mms-expired",
            status = Status.Incoming.ExpiredOrNotAvailable,
            downloadState = MmsDownloadUiModel.State.ExpiredOrUnavailable,
            canDownloadMessage = false,
        ),
    )
}

internal fun conversationMessageRowsPreviewHorizontalAlignment(
    message: ConversationMessageUiModel,
): Alignment.Horizontal {
    return when {
        message.isIncoming -> Alignment.Start
        else -> Alignment.End
    }
}

internal fun previewConversationMessageRowsLayout(
    message: ConversationMessageUiModel,
    showIncomingParticipantIdentity: Boolean,
    metadataText: String?,
): ConversationMessageLayout {
    val showSender = message.isIncoming &&
        showIncomingParticipantIdentity &&
        !message.senderDisplayName.isNullOrBlank() &&
        !message.canClusterWithPrevious
    val showAvatarGutter = message.isIncoming && showIncomingParticipantIdentity
    val showAvatar = showAvatarGutter && !message.canClusterWithNext
    val content = buildConversationMessageContent(
        message = message,
        subjectText = message.mmsSubject,
    )

    return ConversationMessageLayout(
        bubbleShape = previewConversationMessageRowsBubbleShape(message = message),
        bubbleLayoutMode = previewConversationMessageRowsBubbleLayoutMode(
            content = content,
            showSender = showSender,
        ),
        content = content,
        metadataText = previewConversationMessageRowsMetadataText(
            message = message,
            metadataText = metadataText,
        ),
        showSender = showSender,
        showAvatarGutter = showAvatarGutter,
        showAvatar = showAvatar,
    )
}

private fun previewConversationMessageRowsBubbleLayoutMode(
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

private fun previewConversationMessageRowsMetadataText(
    message: ConversationMessageUiModel,
    metadataText: String?,
): String? {
    return when {
        message.mmsDownload != null -> null
        message.canClusterWithNext -> null
        else -> metadataText
    }
}

private fun previewConversationMessageRowsBubbleShape(
    message: ConversationMessageUiModel,
): RoundedCornerShape {
    val topStartCornerRadius = previewConversationMessageRowsClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithPrevious,
    )
    val topEndCornerRadius = previewConversationMessageRowsClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithPrevious,
        useFreeSide = true,
    )
    val bottomStartCornerRadius = previewConversationMessageRowsClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithNext,
    )
    val bottomEndCornerRadius = previewConversationMessageRowsClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithNext,
        useFreeSide = true,
    )

    return RoundedCornerShape(
        topStart = if (message.isIncoming) topStartCornerRadius else topEndCornerRadius,
        topEnd = if (message.isIncoming) topEndCornerRadius else topStartCornerRadius,
        bottomStart = if (message.isIncoming) bottomStartCornerRadius else bottomEndCornerRadius,
        bottomEnd = if (message.isIncoming) bottomEndCornerRadius else bottomStartCornerRadius,
    )
}

private fun previewConversationMessageRowsClusteredCornerRadius(
    clustersWithAdjacent: Boolean,
    useFreeSide: Boolean = false,
): Dp {
    return when {
        !clustersWithAdjacent -> PREVIEW_ROWS_BUBBLE_CORNER_RADIUS
        useFreeSide -> PREVIEW_ROWS_BUBBLE_CORNER_RADIUS
        else -> PREVIEW_ROWS_BUBBLE_CONNECTED_CORNER_RADIUS
    }
}
