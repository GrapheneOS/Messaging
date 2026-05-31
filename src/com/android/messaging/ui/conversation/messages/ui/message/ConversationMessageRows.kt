package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.preview.previewAudioPart
import com.android.messaging.ui.conversation.preview.previewFilePart
import com.android.messaging.ui.conversation.preview.previewImagePart
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.conversation.preview.previewMmsDownloadUiModel
import com.android.messaging.ui.conversation.preview.previewOutgoingMessage
import com.android.messaging.ui.conversation.preview.previewVCardPart
import com.android.messaging.ui.conversation.preview.previewVideoPart
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ConversationMessageBubbleRow(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    maxBubbleWidth: Dp,
    simDisplayName: String?,
    onAttachmentClick: (contentType: String, contentUri: String) -> Unit,
    onExternalUriClick: (String) -> Unit,
    onMessageClick: () -> Unit,
    onMessageAvatarClick: () -> Unit,
    onMessageDownloadClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
) {
    ConversationMessageBubbleRowContainer(
        message = message,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        layout = layout,
        onMessageClick = onMessageClick,
        onMessageAvatarClick = onMessageAvatarClick,
        onMessageLongClick = onMessageLongClick,
    ) {
        ConversationMessageBubble(
            modifier = Modifier.conversationMessageBubbleInteractionModifier(
                message = message,
                isSelectionMode = isSelectionMode,
                layout = layout,
                onMessageDownloadClick = onMessageDownloadClick,
                onMessageLongClick = onMessageLongClick,
                onMessageResendClick = onMessageResendClick,
            ),
            message = message,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            layout = layout,
            maxBubbleWidth = maxBubbleWidth,
            simDisplayName = simDisplayName,
            onAttachmentClick = { contentType, contentUri ->
                when {
                    isSelectionMode -> onMessageClick()
                    message.canDownloadMessage -> onMessageDownloadClick()
                    message.canResendMessage -> onMessageResendClick()
                    else -> onAttachmentClick(contentType, contentUri)
                }
            },
            onExternalUriClick = { uri ->
                when {
                    isSelectionMode -> onMessageClick()
                    message.canDownloadMessage -> onMessageDownloadClick()
                    message.canResendMessage -> onMessageResendClick()
                    else -> onExternalUriClick(uri)
                }
            },
            onMessageLongClick = onMessageLongClick,
        )
    }
}

@Composable
private fun ConversationMessageBubbleRowContainer(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    onMessageClick: () -> Unit,
    onMessageAvatarClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(
                tag = conversationMessageSelectionRowTestTag(
                    messageId = message.messageId,
                ),
            )
            .conversationMessageSelectionModeRowModifier(
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationMessageSelectionIndicator(
            visible = isSelectionMode,
            isSelected = isSelected,
            expandFrom = Alignment.Start,
            shrinkTowards = Alignment.Start,
        )

        Row(
            modifier = Modifier.weight(weight = 1f),
            horizontalArrangement = conversationMessageRowHorizontalArrangement(
                message = message,
            ),
            verticalAlignment = Alignment.Bottom,
        ) {
            ConversationMessageAvatarGutter(
                message = message,
                isSelectionMode = isSelectionMode,
                layout = layout,
                onAvatarClick = onMessageAvatarClick,
                onMessageClick = onMessageClick,
                onMessageLongClick = onMessageLongClick,
            )

            content()
        }
    }
}

@Composable
private fun ConversationMessageAvatarGutter(
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    onAvatarClick: () -> Unit,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
) {
    if (isSelectionMode || !layout.showAvatarGutter) {
        return
    }

    Box(
        modifier = Modifier
            .width(width = CONVERSATION_MESSAGE_AVATAR_GUTTER_WIDTH),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (layout.showAvatar) {
            ConversationMessageAvatar(
                message = message,
                onClick = {
                    when {
                        isSelectionMode -> onMessageClick()
                        else -> onAvatarClick()
                    }
                },
                onLongClick = onMessageLongClick,
            )
        }
    }
}

private fun conversationMessageRowHorizontalArrangement(
    message: ConversationMessageUiModel,
): Arrangement.Horizontal {
    return when {
        message.isIncoming -> Arrangement.Start
        else -> Arrangement.End
    }
}

@Composable
private fun Modifier.conversationMessageSelectionModeRowModifier(
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    return when {
        !isSelectionMode -> this

        else -> {
            this
                .semantics {
                    role = Role.Checkbox
                    selected = isSelected
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = true,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onMessageClick()
                    },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMessageLongClick()
                    },
                )
        }
    }
}

@Composable
private fun Modifier.conversationMessageBubbleInteractionModifier(
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    onMessageDownloadClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    val bubbleModifier = this
        .testTag(
            tag = conversationMessageBubbleTestTag(
                messageId = message.messageId,
            ),
        )
        .clip(shape = layout.bubbleShape)

    return when {
        isSelectionMode -> bubbleModifier

        else -> {
            bubbleModifier.combinedClickable(
                enabled = true,
                onClick = {
                    when {
                        message.canDownloadMessage -> onMessageDownloadClick()
                        message.canResendMessage -> onMessageResendClick()
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMessageLongClick()
                },
            )
        }
    }
}

@Composable
internal fun ConversationMessageMetadataRow(
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    maxBubbleWidth: Dp,
    simDisplayName: String?,
    onSimSelectorClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ConversationMessageSelectionIndicatorOffset(
            visible = isSelectionMode,
            expandFrom = Alignment.Start,
            shrinkTowards = Alignment.Start,
        )

        Row(
            modifier = Modifier.weight(weight = 1f),
            horizontalArrangement = conversationMessageRowHorizontalArrangement(
                message = message,
            ),
        ) {
            ConversationMessageAvatarMetadataOffset(
                isSelectionMode = isSelectionMode,
                layout = layout,
            )

            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                horizontalAlignment = when {
                    message.isIncoming -> Alignment.Start
                    else -> Alignment.End
                },
            ) {
                ConversationMessageMetadata(
                    message = message,
                    metadataText = layout.metadataText,
                    simDisplayName = simDisplayName,
                    onSimSelectorClick = onSimSelectorClick,
                )
            }
        }
    }
}

@Composable
private fun ConversationMessageAvatarMetadataOffset(
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
) {
    if (isSelectionMode || !layout.showAvatarGutter) {
        return
    }

    Box(
        modifier = Modifier
            .width(width = CONVERSATION_MESSAGE_AVATAR_GUTTER_WIDTH),
    )
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsOutgoingStatusPreview() {
    ConversationMessageRowsPreviewColumn {
        conversationMessageRowsOutgoingStatusPreviewItems().forEach { item ->
            ConversationMessageRowsPreviewItem(
                message = previewOutgoingMessage(
                    messageId = item.messageId,
                    text = item.text,
                    status = item.status,
                ),
                simDisplayName = "Work",
                metadataText = item.metadataText,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsIncomingStatusPreview() {
    ConversationMessageRowsPreviewColumn {
        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-incoming-complete",
                text = "Incoming complete row with avatar, sender, and timestamp.",
                status = Status.Incoming.Complete,
            ),
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-incoming-unknown",
                text = "Incoming row with unknown protocol and status.",
                status = Status.Unknown,
                protocol = ConversationMessageUiModel.Protocol.UNKNOWN,
            ),
            simDisplayName = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsDirectionMetadataPreview() {
    ConversationMessageRowsPreviewColumn {
        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-incoming",
                text = PREVIEW_ROWS_LONG_TEXT,
            ),
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-outgoing-delivered",
                text = "Delivered outgoing row with right alignment and SIM metadata.",
                status = Status.Outgoing.Delivered,
            ),
            simDisplayName = "Work",
            metadataText = "18:05 \u2022 Delivered",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-outgoing-failed",
                text = "Failed row shows retry interaction on the bubble and error metadata.",
                status = Status.Outgoing.Failed,
            ),
            simDisplayName = "Personal",
            metadataText = "18:06 \u2022 Failed",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-hidden-identity",
                text = "Incoming row with participant identity hidden keeps the bubble flush left.",
            ),
            showIncomingParticipantIdentity = false,
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-overflow",
                text = PREVIEW_ROWS_OVERFLOW_TEXT,
                status = Status.Outgoing.Complete,
            ),
            simDisplayName = null,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsSelectionPreview() {
    ConversationMessageRowsPreviewColumn {
        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-selection-incoming-unselected",
                text = "Selection mode, incoming row, not selected.",
            ),
            isSelected = false,
            isSelectionMode = true,
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-selection-incoming-selected",
                text = "Selection mode, incoming row, selected.",
            ),
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-selection-outgoing-unselected",
                text = "Selection mode, outgoing row, not selected.",
                status = Status.Outgoing.Sending,
            ),
            isSelected = false,
            isSelectionMode = true,
            simDisplayName = "Work",
            metadataText = "18:07 \u2022 Sending",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-selection-outgoing-selected",
                text = "Selected outgoing failed row keeps retry state visible " +
                    "inside selection mode.",
                status = Status.Outgoing.Failed,
            ),
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Work",
            metadataText = "18:08 \u2022 Failed",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsMmsDownloadPreview() {
    ConversationMessageRowsPreviewColumn {
        conversationMessageRowsMmsDownloadPreviewItems().forEach { item ->
            ConversationMessageRowsPreviewItem(
                message = previewIncomingMessage(
                    messageId = item.messageId,
                    text = null,
                    status = item.status,
                    parts = persistentListOf(),
                    mmsDownload = previewMmsDownloadUiModel(state = item.downloadState),
                    protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
                    canDownloadMessage = item.canDownloadMessage,
                ),
                isSelected = item.isSelected,
                isSelectionMode = item.isSelectionMode,
                simDisplayName = null,
                metadataText = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsAttachmentPreview() {
    ConversationMessageRowsPreviewColumn {
        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-attachments-incoming-gallery",
                text = "Photo and video from the site visit.",
                parts = persistentListOf(
                    previewImagePart(text = "North entrance"),
                    previewVideoPart(text = "Walkthrough clip"),
                ),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ).copy(
                mmsSubject = "Site visit",
            ),
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-attachments-outgoing-audio",
                text = "Voice memo attached.",
                parts = persistentListOf(previewAudioPart(text = "Two minute update")),
                status = Status.Outgoing.Complete,
            ).copy(
                mmsSubject = "Audio update",
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            simDisplayName = "Work",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-attachments-vcard",
                text = null,
                parts = persistentListOf(previewVCardPart()),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-attachments-image-only",
                text = null,
                parts = persistentListOf(previewImagePart(text = null)),
                status = Status.Outgoing.Complete,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-attachments-file-failed",
                text = "Document did not send.",
                parts = persistentListOf(previewFilePart(text = "Quarterly report.pdf")),
                status = Status.Outgoing.Failed,
            ).copy(
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Work",
            metadataText = "18:11 \u2022 Failed",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-attachments-youtube-preview",
                text = "Reference clip: https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                status = Status.Outgoing.Delivered,
            ),
            simDisplayName = "Personal",
            metadataText = "18:12 \u2022 Delivered",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMessageRowsClusterPreview() {
    ConversationMessageRowsPreviewColumn {
        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-incoming-cluster-start",
                text = "Cluster start shows sender but no avatar.",
            ).copy(
                canClusterWithNext = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-incoming-cluster-middle",
                text = "Cluster middle suppresses sender, avatar, and metadata.",
            ).copy(
                canClusterWithPrevious = true,
                canClusterWithNext = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = "rows-incoming-cluster-end",
                text = "Cluster end restores the avatar and timestamp.",
            ).copy(
                canClusterWithPrevious = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-outgoing-cluster-start",
                text = "Outgoing cluster start hides metadata until the last grouped message.",
            ).copy(
                canClusterWithNext = true,
            ),
            simDisplayName = "Work",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = "rows-outgoing-cluster-end",
                text = "Outgoing cluster end shows right aligned metadata.",
                status = Status.Outgoing.Delivered,
            ).copy(
                canClusterWithPrevious = true,
            ),
            simDisplayName = "Work",
            metadataText = "18:16 \u2022 Delivered",
        )
    }
}

@Composable
private fun ConversationMessageRowsPreviewColumn(content: @Composable () -> Unit) {
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
private fun ConversationMessageRowsPreviewItem(
    message: ConversationMessageUiModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    showIncomingParticipantIdentity: Boolean = true,
    simDisplayName: String? = null,
    metadataText: String? = "18:04",
) {
    val layout = previewConversationMessageRowsLayout(
        message = message,
        showIncomingParticipantIdentity = showIncomingParticipantIdentity,
        metadataText = metadataText,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = conversationMessageRowsPreviewHorizontalAlignment(
            message = message,
        ),
    ) {
        ConversationMessageBubbleRow(
            message = message,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            layout = layout,
            maxBubbleWidth = 320.dp,
            simDisplayName = simDisplayName,
            onAttachmentClick = { _, _ -> },
            onExternalUriClick = {},
            onMessageClick = {},
            onMessageAvatarClick = {},
            onMessageDownloadClick = {},
            onMessageLongClick = {},
            onMessageResendClick = {},
        )
        ConversationMessageMetadataRow(
            message = message,
            isSelectionMode = isSelectionMode,
            layout = layout,
            maxBubbleWidth = 320.dp,
            simDisplayName = simDisplayName,
            onSimSelectorClick = {},
        )
    }
}
