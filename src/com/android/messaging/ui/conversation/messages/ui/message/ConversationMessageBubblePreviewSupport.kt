package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn

private val PREVIEW_BUBBLE_CONNECTED_CORNER_RADIUS = 6.dp
private val PREVIEW_BUBBLE_CORNER_RADIUS = 24.dp

internal const val PREVIEW_LONG_BUBBLE_TEXT = "This longer message wraps across several lines " +
    "inside the bubble so typography, line height, and horizontal padding are visible in the " +
    "preview. It also includes https://example.com/details for link styling."
internal const val PREVIEW_OVERFLOW_BUBBLE_TEXT =
    "SupercalifragilisticexpialidociousPreviewTokenWithoutNaturalBreaks1234567890"

internal data class BubbleMmsDownloadPreviewItem(
    val messageId: String,
    val status: Status.Incoming,
    val downloadState: MmsDownloadUiModel.State,
    val canDownloadMessage: Boolean,
    val isSelected: Boolean = false,
)

@Composable
internal fun ConversationMessageBubblePreviewColumn(content: @Composable () -> Unit) {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun ConversationMessageBubblePreviewItem(
    message: ConversationMessageUiModel,
    bubbleLayoutMode: ConversationMessageBubbleLayoutMode,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    showSender: Boolean = message.isIncoming && !message.canClusterWithPrevious,
    simDisplayName: String? = null,
) {
    ConversationMessageBubble(
        message = message,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        layout = previewConversationMessageLayout(
            message = message,
            bubbleLayoutMode = bubbleLayoutMode,
            showSender = showSender,
        ),
        maxBubbleWidth = 320.dp,
        simDisplayName = simDisplayName,
        onAttachmentClick = { _, _, _ -> },
        onExternalUriClick = {},
        onMessageLongClick = {},
    )
}

internal fun conversationMessageBubbleMmsDownloadPreviewItems():
    List<BubbleMmsDownloadPreviewItem> {
    return listOf(
        BubbleMmsDownloadPreviewItem(
            messageId = "bubble-mms-awaiting",
            status = Status.Incoming.YetToManualDownload,
            downloadState = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = true,
        ),
        BubbleMmsDownloadPreviewItem(
            messageId = "bubble-mms-awaiting-disabled",
            status = Status.Incoming.YetToManualDownload,
            downloadState = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = false,
        ),
        BubbleMmsDownloadPreviewItem(
            messageId = "bubble-mms-downloading",
            status = Status.Incoming.ManualDownloading,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
            isSelected = true,
        ),
        BubbleMmsDownloadPreviewItem(
            messageId = "bubble-mms-failed",
            status = Status.Incoming.DownloadFailed,
            downloadState = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = true,
        ),
        BubbleMmsDownloadPreviewItem(
            messageId = "bubble-mms-failed-disabled",
            status = Status.Incoming.DownloadFailed,
            downloadState = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = false,
        ),
        BubbleMmsDownloadPreviewItem(
            messageId = "bubble-mms-expired",
            status = Status.Incoming.ExpiredOrNotAvailable,
            downloadState = MmsDownloadUiModel.State.ExpiredOrUnavailable,
            canDownloadMessage = false,
        ),
    )
}

internal fun previewConversationMessageLayout(
    message: ConversationMessageUiModel,
    bubbleLayoutMode: ConversationMessageBubbleLayoutMode,
    showSender: Boolean,
): ConversationMessageLayout {
    return ConversationMessageLayout(
        bubbleShape = previewConversationMessageBubbleShape(message = message),
        bubbleLayoutMode = bubbleLayoutMode,
        content = buildConversationMessageContent(
            message = message,
            subjectText = message.mmsSubject,
        ),
        metadataText = "18:04",
        showSender = showSender,
        showAvatarGutter = message.isIncoming,
        showAvatar = message.isIncoming && !message.canClusterWithNext,
    )
}

private fun previewConversationMessageBubbleShape(
    message: ConversationMessageUiModel,
): RoundedCornerShape {
    val topStartCornerRadius = previewClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithPrevious,
    )
    val topEndCornerRadius = previewClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithPrevious,
        useFreeSide = true,
    )
    val bottomStartCornerRadius = previewClusteredCornerRadius(
        clustersWithAdjacent = message.canClusterWithNext,
    )
    val bottomEndCornerRadius = previewClusteredCornerRadius(
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

private fun previewClusteredCornerRadius(
    clustersWithAdjacent: Boolean,
    useFreeSide: Boolean = false,
): Dp {
    return when {
        !clustersWithAdjacent -> PREVIEW_BUBBLE_CORNER_RADIUS
        useFreeSide -> PREVIEW_BUBBLE_CORNER_RADIUS
        else -> PREVIEW_BUBBLE_CONNECTED_CORNER_RADIUS
    }
}
