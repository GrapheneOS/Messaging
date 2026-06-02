package com.android.messaging.ui.conversation.composer.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_MMS_INDICATOR_TEST_TAG

internal fun conversationComposeMmsSlot(
    sendProtocol: ConversationDraftSendProtocol,
): (@Composable () -> Unit)? {
    return when (sendProtocol) {
        ConversationDraftSendProtocol.MMS -> {
            {
                MmsIndicator()
            }
        }

        ConversationDraftSendProtocol.SMS -> null
    }
}

@Composable
internal fun MmsIndicator() {
    Text(
        modifier = Modifier
            .padding(end = 12.dp)
            .clearAndSetSemantics {
                testTag = CONVERSATION_MMS_INDICATOR_TEST_TAG
            },
        text = stringResource(id = R.string.mms_text),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
internal fun ConversationComposeAttachmentMenu(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isAudioRecordActionEnabled: Boolean,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onAudioAttachClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isExpanded by rememberSaveable {
        mutableStateOf(value = false)
    }

    fun closeMenuAndRun(action: () -> Unit) {
        isExpanded = false
        action()
    }

    Box(
        modifier = modifier,
    ) {
        IconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                isExpanded = true
            },
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = stringResource(
                    id = R.string.attachMediaButtonContentDescription,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            offset = DpOffset(
                x = 0.dp,
                y = (-8).dp,
            ),
            properties = PopupProperties(
                focusable = false,
                clippingEnabled = false,
            ),
        ) {
            ConversationComposeAttachmentMenuContent(
                isAudioRecordActionEnabled = isAudioRecordActionEnabled,
                onMediaPickerClick = {
                    closeMenuAndRun(action = onMediaPickerClick)
                },
                onAudioAttachClick = {
                    closeMenuAndRun(action = onAudioAttachClick)
                },
                onContactAttachClick = {
                    closeMenuAndRun(action = onContactAttachClick)
                },
            )
        }
    }
}

@Composable
private fun ConversationComposeAttachmentMenuContent(
    isAudioRecordActionEnabled: Boolean,
    onMediaPickerClick: () -> Unit,
    onAudioAttachClick: () -> Unit,
    onContactAttachClick: () -> Unit,
) {
    ConversationComposeAttachmentMenuItem(
        modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG),
        imageVector = Icons.Rounded.Image,
        textResId = R.string.mediapicker_gallery_title,
        onClick = onMediaPickerClick,
    )
    ConversationComposeAttachmentMenuItem(
        modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG),
        imageVector = Icons.Rounded.Mic,
        textResId = R.string.mediapicker_audio_title,
        enabled = isAudioRecordActionEnabled,
        onClick = onAudioAttachClick,
    )

    ConversationComposeAttachmentMenuItem(
        modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG),
        imageVector = Icons.Rounded.Person,
        textResId = R.string.mediapicker_contact_title,
        onClick = onContactAttachClick,
    )
}

@Composable
private fun ConversationComposeAttachmentMenuItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    @StringRes textResId: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = modifier,
        text = {
            Text(text = stringResource(id = textResId))
        },
        leadingIcon = {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(size = 24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@PreviewLightDark
@Composable
private fun ConversationComposeMessageFieldPreview() {
    MessagingPreviewColumn {
        Column {
            ConversationComposeMessageField(
                value = "",
                enabled = true,
                sendProtocol = ConversationDraftSendProtocol.SMS,
                isVisuallyHidden = false,
                messageFieldFocusRequester = null,
                presentation = rememberConversationComposeBarPresentation(),
                isAttachmentActionEnabled = true,
                isAudioRecordActionEnabled = true,
                onValueChange = { _ -> },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onAudioAttachClick = {},
            )
            ConversationComposeMessageField(
                value = "Photo attached",
                enabled = true,
                sendProtocol = ConversationDraftSendProtocol.MMS,
                isVisuallyHidden = false,
                messageFieldFocusRequester = null,
                presentation = rememberConversationComposeBarPresentation(),
                isAttachmentActionEnabled = true,
                isAudioRecordActionEnabled = false,
                onValueChange = { _ -> },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onAudioAttachClick = {},
            )
            ConversationComposeMessageField(
                value = "Disabled conversation",
                enabled = false,
                sendProtocol = ConversationDraftSendProtocol.SMS,
                isVisuallyHidden = false,
                messageFieldFocusRequester = null,
                presentation = rememberConversationComposeBarPresentation(),
                isAttachmentActionEnabled = false,
                isAudioRecordActionEnabled = false,
                onValueChange = { _ -> },
                onContactAttachClick = {},
                onMediaPickerClick = {},
                onAudioAttachClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationComposeAttachmentMenuContentPreview() {
    MessagingPreviewColumn {
        ConversationComposeAttachmentMenuContent(
            isAudioRecordActionEnabled = false,
            onMediaPickerClick = {},
            onAudioAttachClick = {},
            onContactAttachClick = {},
        )
    }
}
