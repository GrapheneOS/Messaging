package com.android.messaging.ui.conversation.messages.ui.message

import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
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
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import com.android.messaging.ui.conversation.screen.model.availableMessageActions
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
    bubbleRipple: ConversationMessageBubbleRipple,
    onAttachmentClick: OnConversationAttachmentClick,
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
            modifier = Modifier.conversationMessageBubbleModifier(
                message = message,
                isSelectionMode = isSelectionMode,
                layout = layout,
                bubbleRipple = bubbleRipple,
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
            onAttachmentClick = { contentType, contentUri, partId ->
                when {
                    isSelectionMode -> onMessageClick()
                    message.canDownloadMessage -> onMessageDownloadClick()
                    message.canResendMessage -> onMessageResendClick()
                    else -> onAttachmentClick(contentType, contentUri, partId)
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
        modifier = Modifier.fillMaxWidth(),
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

/**
 * Handles clicks for the whole message, bubble and metadata, so that a screen reader reads them as
 * one item that offers the message actions. The ripple is still drawn on the bubble only.
 */
@Composable
internal fun Modifier.conversationMessageInteractionModifier(
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    bubbleRipple: ConversationMessageBubbleRipple,
    onMessageClick: () -> Unit,
    onMessageDownloadClick: () -> Unit,
    onMessageActionClick: (ConversationMessageAction) -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
): Modifier {
    val messageActions = message.availableMessageActions().map { action ->
        CustomAccessibilityAction(
            label = stringResource(id = action.labelRes),
        ) {
            onMessageActionClick(action)
            true
        }
    }
    // A screen reader presses the middle of the message, which can be beside the bubble
    val isWholeMessageTouchable = rememberIsTouchExplorationEnabled() || isSelectionMode

    return this
        .testTag(
            tag = conversationMessageSelectionRowTestTag(
                messageId = message.messageId,
            ),
        )
        .onPlaced { coordinates ->
            bubbleRipple.messageCoordinates = coordinates
        }
        .semantics {
            // The attachments, links and avatar inside the message come first: TalkBack drops a
            // node's link to its own descendant, which leaves the message out of order in the
            // reversed list
            traversalIndex = 1f

            when {
                isSelectionMode -> {
                    role = Role.Checkbox
                    selected = isSelected
                }

                else -> customActions = messageActions
            }
        }
        .conversationMessageClickable(
            message = message,
            isSelectionMode = isSelectionMode,
            isWholeMessageTouchable = isWholeMessageTouchable,
            interactionSource = bubbleRipple.messageInteractionSource,
            onMessageClick = onMessageClick,
            onMessageDownloadClick = onMessageDownloadClick,
            onMessageLongClick = onMessageLongClick,
            onMessageResendClick = onMessageResendClick,
        )
}

@Composable
private fun rememberIsTouchExplorationEnabled(): Boolean {
    val accessibilityManager = LocalContext.current
        .getSystemService(AccessibilityManager::class.java)
    var isTouchExplorationEnabled by remember(accessibilityManager) {
        mutableStateOf(accessibilityManager.isTouchExplorationEnabled)
    }

    DisposableEffect(accessibilityManager) {
        val listener = TouchExplorationStateChangeListener { isEnabled ->
            isTouchExplorationEnabled = isEnabled
        }
        accessibilityManager.addTouchExplorationStateChangeListener(listener)
        isTouchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled

        onDispose {
            accessibilityManager.removeTouchExplorationStateChangeListener(listener)
        }
    }

    return isTouchExplorationEnabled
}

@Composable
private fun Modifier.conversationMessageBubbleModifier(
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    layout: ConversationMessageLayout,
    bubbleRipple: ConversationMessageBubbleRipple,
    onMessageDownloadClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
): Modifier {
    val senderAnnouncement = conversationMessageSenderAnnouncement(
        message = message,
        isSenderLabelVisible = layout.showSender,
    )
    val bubbleModifier = this
        .testTag(
            tag = conversationMessageBubbleTestTag(
                messageId = message.messageId,
            ),
        )
        .conversationMessageSenderSemantics(announcement = senderAnnouncement)
        .clip(shape = layout.bubbleShape)
        .onPlaced { coordinates ->
            bubbleRipple.bubbleCoordinates = coordinates
        }

    return when {
        isSelectionMode -> bubbleModifier

        else -> {
            bubbleModifier
                .indication(
                    interactionSource = bubbleRipple.bubbleInteractionSource,
                    indication = LocalIndication.current,
                )
                .conversationMessageBubbleClickable(
                    interactionSource = bubbleRipple.bubbleInteractionSource,
                    onClick = message.conversationMessageTapOrNull(
                        onMessageDownloadClick = onMessageDownloadClick,
                        onMessageResendClick = onMessageResendClick,
                    ),
                    onLongClick = onMessageLongClick,
                )
        }
    }
}

@Composable
internal fun rememberConversationMessageBubbleRipple(): ConversationMessageBubbleRipple {
    val bubbleRipple = remember { ConversationMessageBubbleRipple() }

    LaunchedEffect(bubbleRipple) {
        bubbleRipple.replayPressesOnBubble()
    }

    return bubbleRipple
}

/**
 * Replays the interactions of the message's click handler, such as keyboard focus and presses, on
 * the bubble, with press positions moved into the bubble's coordinates. Presses outside the bubble
 * show no ripple. Touch presses on the bubble go to [bubbleInteractionSource] directly.
 */
@Stable
internal class ConversationMessageBubbleRipple {
    val messageInteractionSource = MutableInteractionSource()
    val bubbleInteractionSource = MutableInteractionSource()
    var messageCoordinates: LayoutCoordinates? = null
    var bubbleCoordinates: LayoutCoordinates? = null

    suspend fun replayPressesOnBubble() {
        var bubblePress: PressInteraction.Press? = null

        messageInteractionSource.interactions.collect { interaction ->
            val bubbleInteraction = when (interaction) {
                is PressInteraction.Press -> {
                    bubblePress = bubblePositionOrNull(messagePosition = interaction.pressPosition)
                        ?.let(PressInteraction::Press)
                    bubblePress
                }

                is PressInteraction.Release -> {
                    bubblePress?.let(PressInteraction::Release)
                }

                is PressInteraction.Cancel -> {
                    bubblePress?.let(PressInteraction::Cancel)
                }

                else -> interaction
            }

            bubbleInteraction?.let { bubbleInteractionSource.emit(interaction = it) }
        }
    }

    /** Returns [messagePosition] in the bubble's coordinates, or null when it's beside the bubble. */
    fun bubblePositionOrNull(messagePosition: Offset): Offset? {
        val message = messageCoordinates?.takeIf(LayoutCoordinates::isAttached)
        val bubble = bubbleCoordinates?.takeIf(LayoutCoordinates::isAttached)
        if (message == null || bubble == null) {
            return null
        }

        val bubblePosition = bubble.localPositionOf(
            sourceCoordinates = message,
            relativeToSource = messagePosition,
        )
        val bubbleBounds = Rect(offset = Offset.Zero, size = bubble.size.toSize())

        return bubblePosition.takeIf { bubbleBounds.contains(offset = it) }
    }
}

@Composable
private fun conversationMessageSenderAnnouncement(
    message: ConversationMessageUiModel,
    isSenderLabelVisible: Boolean,
): String? {
    return when {
        !message.isIncoming -> stringResource(id = R.string.outgoing_sender_content_description)
        isSenderLabelVisible -> null

        else -> {
            stringResource(
                id = R.string.incoming_sender_content_description,
                message
                    .senderDisplayName
                    ?.takeIf(String::isNotBlank)
                    ?: stringResource(id = R.string.unknown_sender),
            )
        }
    }
}

private fun Modifier.conversationMessageSenderSemantics(announcement: String?): Modifier {
    return when {
        announcement == null -> this
        else -> {
            semantics {
                text = AnnotatedString(text = announcement)
            }
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
                messageId = MessageId("rows-incoming-complete"),
                text = "Incoming complete row with avatar, sender, and timestamp.",
                status = Status.Incoming.Complete,
            ),
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = MessageId("rows-incoming-unknown"),
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
                messageId = MessageId("rows-incoming"),
                text = PREVIEW_ROWS_LONG_TEXT,
            ),
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-outgoing-delivered"),
                text = "Delivered outgoing row with right alignment and SIM metadata.",
                status = Status.Outgoing.Delivered,
            ),
            simDisplayName = "Work",
            metadataText = "18:05 \u2022 Delivered",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-outgoing-failed"),
                text = "Failed row shows retry interaction on the bubble and error metadata.",
                status = Status.Outgoing.Failed,
            ),
            simDisplayName = "Personal",
            metadataText = "18:06 \u2022 Failed",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = MessageId("rows-hidden-identity"),
                text = "Incoming row with participant identity hidden keeps the bubble flush left.",
            ),
            showIncomingParticipantIdentity = false,
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-overflow"),
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
                messageId = MessageId("rows-selection-incoming-unselected"),
                text = "Selection mode, incoming row, not selected.",
            ),
            isSelected = false,
            isSelectionMode = true,
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = MessageId("rows-selection-incoming-selected"),
                text = "Selection mode, incoming row, selected.",
            ),
            isSelected = true,
            isSelectionMode = true,
            simDisplayName = "Personal",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-selection-outgoing-unselected"),
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
                messageId = MessageId("rows-selection-outgoing-selected"),
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
                messageId = MessageId("rows-attachments-incoming-gallery"),
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
                messageId = MessageId("rows-attachments-outgoing-audio"),
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
                messageId = MessageId("rows-attachments-vcard"),
                text = null,
                parts = persistentListOf(previewVCardPart()),
                protocol = ConversationMessageUiModel.Protocol.MMS,
                canSaveAttachments = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-attachments-image-only"),
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
                messageId = MessageId("rows-attachments-file-failed"),
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
                messageId = MessageId("rows-attachments-youtube-preview"),
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
                messageId = MessageId("rows-incoming-cluster-start"),
                text = "Cluster start shows sender but no avatar.",
            ).copy(
                canClusterWithNext = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = MessageId("rows-incoming-cluster-middle"),
                text = "Cluster middle suppresses sender, avatar, and metadata.",
            ).copy(
                canClusterWithPrevious = true,
                canClusterWithNext = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewIncomingMessage(
                messageId = MessageId("rows-incoming-cluster-end"),
                text = "Cluster end restores the avatar and timestamp.",
            ).copy(
                canClusterWithPrevious = true,
            ),
            simDisplayName = null,
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-outgoing-cluster-start"),
                text = "Outgoing cluster start hides metadata until the last grouped message.",
            ).copy(
                canClusterWithNext = true,
            ),
            simDisplayName = "Work",
        )

        ConversationMessageRowsPreviewItem(
            message = previewOutgoingMessage(
                messageId = MessageId("rows-outgoing-cluster-end"),
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
            bubbleRipple = rememberConversationMessageBubbleRipple(),
            onAttachmentClick = { _, _, _ -> },
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
