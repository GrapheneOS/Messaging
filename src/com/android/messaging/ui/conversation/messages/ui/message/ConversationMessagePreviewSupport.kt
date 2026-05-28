package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn

internal data class MessageStatusPreviewItem(
    val messageId: String,
    val text: String,
    val status: Status,
)

internal data class MessageMmsDownloadPreviewItem(
    val messageId: String,
    val status: Status.Incoming,
    val downloadState: MmsDownloadUiModel.State,
    val canDownloadMessage: Boolean,
)

@Composable
internal fun ConversationMessagePreviewColumn(content: @Composable () -> Unit) {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun ConversationMessagePreviewItem(
    message: ConversationMessageUiModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    showIncomingParticipantIdentity: Boolean = true,
    simDisplayName: String? = null,
) {
    ConversationMessage(
        message = message,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        showIncomingParticipantIdentity = showIncomingParticipantIdentity,
        simDisplayName = simDisplayName,
    )
}

internal fun outgoingStatusPreviewItems(): List<MessageStatusPreviewItem> {
    return listOf(
        MessageStatusPreviewItem(
            messageId = "outgoing-complete",
            text = "Outgoing complete message.",
            status = Status.Outgoing.Complete,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-delivered",
            text = "Outgoing delivered message.",
            status = Status.Outgoing.Delivered,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-draft",
            text = "Outgoing draft message.",
            status = Status.Outgoing.Draft,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-yet-to-send",
            text = "Outgoing queued message.",
            status = Status.Outgoing.YetToSend,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-sending",
            text = "Outgoing sending message.",
            status = Status.Outgoing.Sending,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-resending",
            text = "Outgoing retrying send.",
            status = Status.Outgoing.Resending,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-awaiting-retry",
            text = "Outgoing awaiting retry.",
            status = Status.Outgoing.AwaitingRetry,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-failed",
            text = "Outgoing failed and can be resent.",
            status = Status.Outgoing.Failed,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-failed-emergency",
            text = "Outgoing failed emergency-number message.",
            status = Status.Outgoing.FailedEmergencyNumber,
        ),
        MessageStatusPreviewItem(
            messageId = "outgoing-unknown",
            text = "Outgoing message with unknown status.",
            status = Status.Unknown,
        ),
    )
}

internal fun mmsDownloadStatusPreviewItems(): List<MessageMmsDownloadPreviewItem> {
    return listOf(
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-yet-to-manual-download",
            status = Status.Incoming.YetToManualDownload,
            downloadState = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = true,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-yet-to-manual-download-disabled",
            status = Status.Incoming.YetToManualDownload,
            downloadState = MmsDownloadUiModel.State.AwaitingManualDownload,
            canDownloadMessage = false,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-retrying-manual-download",
            status = Status.Incoming.RetryingManualDownload,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-manual-downloading",
            status = Status.Incoming.ManualDownloading,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-retrying-auto-download",
            status = Status.Incoming.RetryingAutoDownload,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-auto-downloading",
            status = Status.Incoming.AutoDownloading,
            downloadState = MmsDownloadUiModel.State.Downloading,
            canDownloadMessage = false,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-download-failed",
            status = Status.Incoming.DownloadFailed,
            downloadState = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = true,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-download-failed-disabled",
            status = Status.Incoming.DownloadFailed,
            downloadState = MmsDownloadUiModel.State.DownloadFailed,
            canDownloadMessage = false,
        ),
        MessageMmsDownloadPreviewItem(
            messageId = "incoming-expired",
            status = Status.Incoming.ExpiredOrNotAvailable,
            downloadState = MmsDownloadUiModel.State.ExpiredOrUnavailable,
            canDownloadMessage = false,
        ),
    )
}
