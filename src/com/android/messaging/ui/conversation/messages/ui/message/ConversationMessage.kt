package com.android.messaging.ui.conversation.messages.ui.message

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.sms.cleanseMmsSubject
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.ui.attachment.OnConversationAttachmentClick
import com.android.messaging.ui.conversation.preview.previewAudioPart
import com.android.messaging.ui.conversation.preview.previewFilePart
import com.android.messaging.ui.conversation.preview.previewImagePart
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.conversation.preview.previewMmsDownloadUiModel
import com.android.messaging.ui.conversation.preview.previewOutgoingMessage
import com.android.messaging.ui.conversation.preview.previewVCardPart
import com.android.messaging.ui.conversation.preview.previewVideoPart
import kotlinx.collections.immutable.persistentListOf

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
    showIncomingParticipantIdentity: Boolean = true,
    simDisplayName: String? = null,
    onAttachmentClick: OnConversationAttachmentClick =
        { _, _, _ -> },
    onExternalUriClick: (String) -> Unit = {},
    onMessageClick: () -> Unit = {},
    onMessageAvatarClick: () -> Unit = {},
    onMessageDownloadClick: () -> Unit = {},
    onMessageLongClick: () -> Unit = {},
    onMessageResendClick: () -> Unit = {},
    onSimSelectorClick: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        val layout = rememberConversationMessageLayout(
            message = message,
            showIncomingParticipantIdentity = showIncomingParticipantIdentity,
        )

        val maxBubbleWidth = remember(maxWidth) {
            (maxWidth * MESSAGE_BUBBLE_WIDTH_FRACTION)
                .coerceAtMost(MESSAGE_BUBBLE_MAX_WIDTH_DP.dp)
        }

        val maxAdjustedBubbleWidth = remember(
            maxBubbleWidth,
            layout.showAvatarGutter,
        ) {
            conversationMessageMaxBubbleWidth(
                maxBubbleWidth = maxBubbleWidth,
                showAvatarGutter = layout.showAvatarGutter,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = messageHorizontalArrangement(message = message),
        ) {
            ConversationMessageContent(
                message = message,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                layout = layout,
                maxBubbleWidth = maxAdjustedBubbleWidth,
                simDisplayName = simDisplayName,
                onAttachmentClick = onAttachmentClick,
                onExternalUriClick = onExternalUriClick,
                onMessageClick = onMessageClick,
                onMessageAvatarClick = onMessageAvatarClick,
                onMessageDownloadClick = onMessageDownloadClick,
                onMessageLongClick = onMessageLongClick,
                onMessageResendClick = onMessageResendClick,
                onSimSelectorClick = onSimSelectorClick,
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
    val showAvatarGutter: Boolean,
    val showAvatar: Boolean,
)

internal enum class ConversationMessageBubbleLayoutMode {
    AttachmentOnlyWithoutSurface,
    AttachmentsInSurface,
    TextInSurface,
}

@Composable
private fun rememberConversationMessageLayout(
    message: ConversationMessageUiModel,
    showIncomingParticipantIdentity: Boolean,
): ConversationMessageLayout {
    val bubbleShape = remember(
        message.canClusterWithPrevious,
        message.canClusterWithNext,
    ) {
        messageBubbleShape(message = message)
    }

    val content = rememberConversationMessageContent(message = message)
    val metadataText = rememberConversationMessageMetadataText(message = message)

    val showSender = message.isIncoming &&
        showIncomingParticipantIdentity &&
        !message.senderDisplayName.isNullOrBlank() &&
        !message.canClusterWithPrevious

    val showAvatarGutter = message.isIncoming && showIncomingParticipantIdentity

    val showAvatar = showAvatarGutter && !message.canClusterWithNext

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
        showAvatarGutter,
        showAvatar,
    ) {
        ConversationMessageLayout(
            bubbleShape = bubbleShape,
            bubbleLayoutMode = bubbleLayoutMode,
            content = content,
            metadataText = metadataText,
            showSender = showSender,
            showAvatarGutter = showAvatarGutter,
            showAvatar = showAvatar,
        )
    }
}

private fun conversationMessageMaxBubbleWidth(
    maxBubbleWidth: Dp,
    showAvatarGutter: Boolean,
): Dp {
    return when {
        showAvatarGutter -> {
            (maxBubbleWidth - CONVERSATION_MESSAGE_AVATAR_GUTTER_WIDTH)
                .coerceAtLeast(0.dp)
        }

        else -> maxBubbleWidth
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
        message.canResendMessage,
        message.text,
        message.mmsSubject,
        message.parts,
        message.isYouTubePreviewEnabled,
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
    if (message.mmsDownload != null) {
        return null
    }

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
    simDisplayName: String?,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: () -> Unit,
    onMessageAvatarClick: () -> Unit,
    onMessageDownloadClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
    onSimSelectorClick: () -> Unit,
) {
    Column(
        horizontalAlignment = messageContentHorizontalAlignment(message = message),
    ) {
        ConversationMessageBubbleRow(
            message = message,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            layout = layout,
            maxBubbleWidth = maxBubbleWidth,
            simDisplayName = simDisplayName,
            onAttachmentClick = onAttachmentClick,
            onExternalUriClick = onExternalUriClick,
            onMessageClick = onMessageClick,
            onMessageAvatarClick = onMessageAvatarClick,
            onMessageDownloadClick = onMessageDownloadClick,
            onMessageLongClick = onMessageLongClick,
            onMessageResendClick = onMessageResendClick,
        )

        ConversationMessageMetadataRow(
            message = message,
            isSelectionMode = isSelectionMode,
            layout = layout,
            maxBubbleWidth = maxBubbleWidth,
            simDisplayName = simDisplayName,
            onSimSelectorClick = onSimSelectorClick,
        )
    }
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

private fun messageStatusTextResourceId(status: Status): Int? {
    return when (status) {
        Status.Outgoing.Delivered -> R.string.delivered_status_content_description
        Status.Outgoing.YetToSend -> R.string.message_status_sending
        Status.Outgoing.Sending -> R.string.message_status_sending
        Status.Outgoing.Resending -> R.string.message_status_send_retrying
        Status.Outgoing.AwaitingRetry -> R.string.message_status_failed
        Status.Outgoing.Failed -> R.string.message_status_send_failed
        Status.Outgoing.FailedEmergencyNumber -> {
            R.string.message_status_send_failed_emergency_number
        }
        else -> null
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageOutgoingStatusPreview() {
    ConversationMessagePreviewColumn {
        outgoingStatusPreviewItems().forEach { item ->
            ConversationMessagePreviewItem(
                message = previewOutgoingMessage(
                    messageId = item.messageId,
                    text = item.text,
                    status = item.status,
                ),
                simDisplayName = "Personal",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageIncomingStatusPreview() {
    ConversationMessagePreviewColumn {
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-complete",
                text = "Incoming complete message.",
                status = Status.Incoming.Complete,
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-unknown",
                text = "Incoming message with unknown status.",
                status = Status.Unknown,
                protocol = ConversationMessageUiModel.Protocol.UNKNOWN,
            ),
            simDisplayName = "Personal",
        )
        mmsDownloadStatusPreviewItems().forEach { item ->
            ConversationMessagePreviewItem(
                message = previewIncomingMessage(
                    messageId = item.messageId,
                    text = null,
                    parts = persistentListOf(),
                    status = item.status,
                    mmsDownload = previewMmsDownloadUiModel(state = item.downloadState),
                    protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
                    canDownloadMessage = item.canDownloadMessage,
                ),
                simDisplayName = "Personal",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageAttachmentContentPreview() {
    ConversationMessagePreviewColumn {
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-image-only",
                text = null,
                parts = persistentListOf(previewImagePart(text = null)),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-image-only-selected",
                text = null,
                parts = persistentListOf(previewImagePart(text = null)),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-media-with-subject",
                text = "Long body text below the gallery with a URL " +
                    "https://example.com/trip-notes.",
                parts = persistentListOf(
                    previewImagePart(text = "Image caption"),
                    previewVideoPart(text = "Video caption"),
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ).copy(
                mmsSubject = "Trip media",
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewOutgoingMessage(
                messageId = "outgoing-audio",
                text = null,
                parts = persistentListOf(previewAudioPart(text = "Voice note caption")),
                status = Status.Outgoing.Delivered,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            simDisplayName = "Work",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-vcard",
                text = null,
                parts = persistentListOf(previewVCardPart()),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewOutgoingMessage(
                messageId = "outgoing-file-unsupported",
                text = null,
                parts = persistentListOf(previewFilePart(text = "Unsupported PDF attachment")),
                status = Status.Outgoing.Complete,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            simDisplayName = "Work",
        )
        ConversationMessagePreviewItem(
            message = previewOutgoingMessage(
                messageId = "outgoing-youtube-preview",
                text = "Watch this: https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                status = Status.Outgoing.Delivered,
            ),
            simDisplayName = "Personal",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageClusterSelectionPreview() {
    ConversationMessagePreviewColumn {
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-cluster-start",
                text = "Incoming cluster starts here.",
            ).copy(
                canClusterWithNext = true,
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-cluster-middle",
                text = "Incoming cluster middle.",
            ).copy(
                canClusterWithPrevious = true,
                canClusterWithNext = true,
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-cluster-end",
                text = "Incoming cluster ends here.",
            ).copy(
                canClusterWithPrevious = true,
            ),
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewOutgoingMessage(
                messageId = "outgoing-cluster-start",
                text = "Outgoing cluster starts here.",
            ).copy(
                canClusterWithNext = true,
            ),
            simDisplayName = "Work",
        )
        ConversationMessagePreviewItem(
            message = previewOutgoingMessage(
                messageId = "outgoing-cluster-end",
                text = "Outgoing cluster ends here.",
            ).copy(
                canClusterWithPrevious = true,
            ),
            simDisplayName = "Work",
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-identity-hidden",
                text = "Incoming without participant identity.",
            ),
            showIncomingParticipantIdentity = false,
            simDisplayName = null,
        )
        ConversationMessagePreviewItem(
            message = previewIncomingMessage(
                messageId = "incoming-selection-unselected",
                text = "Selection mode, not selected.",
            ),
            isSelectionMode = true,
            simDisplayName = "Personal",
        )
        ConversationMessagePreviewItem(
            message = previewOutgoingMessage(
                messageId = "outgoing-selection-selected",
                text = "Selection mode, selected.",
                status = Status.Outgoing.Failed,
            ),
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Work",
        )
    }
}
