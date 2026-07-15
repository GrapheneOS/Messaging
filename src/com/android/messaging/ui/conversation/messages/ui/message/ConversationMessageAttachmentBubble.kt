package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageContent
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.ui.attachment.ConversationMessageAttachments
import com.android.messaging.ui.conversation.messages.ui.attachment.OnConversationAttachmentClick
import com.android.messaging.ui.conversation.messages.ui.text.ConversationMessageText

private val MESSAGE_BUBBLE_MEDIA_SECTION_SPACING = 8.dp
private val MESSAGE_BUBBLE_MEDIA_TEXT_PADDING = 12.dp
private const val MESSAGE_SELECTION_MEDIA_OVERLAY_ALPHA = 0.2f

@Composable
internal fun ConversationMessageAttachmentOnlyBubble(
    modifier: Modifier,
    layout: ConversationMessageLayout,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onAttachmentClick: OnConversationAttachmentClick,
    onExternalUriClick: (String) -> Unit,
    onMessageLongClick: () -> Unit,
) {
    ConversationMessageAttachmentOnlyContainer(
        modifier = modifier,
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

@Composable
internal fun ConversationMessageAttachmentSurfaceBubble(
    modifier: Modifier,
    layout: ConversationMessageLayout,
    isSelected: Boolean,
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    onAttachmentClick: OnConversationAttachmentClick,
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
private fun ConversationMessageAttachmentBubbleContent(
    modifier: Modifier = Modifier,
    layout: ConversationMessageLayout,
    message: ConversationMessageUiModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onAttachmentClick: OnConversationAttachmentClick,
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
