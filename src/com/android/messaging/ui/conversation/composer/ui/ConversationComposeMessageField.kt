package com.android.messaging.ui.conversation.composer.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_MMS_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_TEXT_FIELD_TEST_TAG

@Composable
internal fun rememberConversationComposeBarPresentation(): ConversationComposeBarPresentation {
    val fieldColors = conversationComposeBarTextFieldColors()

    return remember(fieldColors) {
        ConversationComposeBarPresentation(
            fieldShape = RoundedCornerShape(size = 28.dp),
            fieldColors = fieldColors,
        )
    }
}

@Composable
private fun conversationComposeBarTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun ConversationComposeMessageField(
    modifier: Modifier = Modifier,
    value: String,
    enabled: Boolean,
    sendProtocol: ConversationDraftSendProtocol,
    isVisuallyHidden: Boolean,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    isAttachmentActionEnabled: Boolean,
    isAudioRecordActionEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onAudioAttachClick: () -> Unit,
) {
    val focusRequesterModifier = messageFieldFocusRequester
        ?.let(Modifier::focusRequester)
        ?: Modifier

    val recordingVisibilityModifier = when {
        isVisuallyHidden -> {
            Modifier
                .alpha(alpha = 0f)
                .clearAndSetSemantics {}
        }

        else -> Modifier
    }

    val mmsText = stringResource(id = R.string.mms_text)
    val sendProtocolSemanticsModifier = when (sendProtocol) {
        ConversationDraftSendProtocol.MMS -> {
            Modifier.semantics {
                stateDescription = mmsText
            }
        }

        ConversationDraftSendProtocol.SMS -> Modifier
    }

    TextField(
        modifier = modifier
            .then(focusRequesterModifier)
            .testTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .heightIn(min = 56.dp)
            .then(sendProtocolSemanticsModifier)
            .then(recordingVisibilityModifier),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        shape = presentation.fieldShape,
        colors = presentation.fieldColors,
        placeholder = ::ConversationComposePlaceholder,
        leadingIcon = {
            ConversationComposeAttachmentMenu(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG),
                enabled = isAttachmentActionEnabled,
                isAudioRecordActionEnabled = isAudioRecordActionEnabled,
                onContactAttachClick = onContactAttachClick,
                onMediaPickerClick = onMediaPickerClick,
                onAudioAttachClick = onAudioAttachClick,
            )
        },
        trailingIcon = when (sendProtocol) {
            ConversationDraftSendProtocol.MMS -> {
                {
                    MmsIndicator()
                }
            }

            ConversationDraftSendProtocol.SMS -> null
        },
        minLines = 1,
        maxLines = 4,
    )
}

@Composable
private fun MmsIndicator() {
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
private fun ConversationComposePlaceholder() {
    Text(
        text = stringResource(id = R.string.compose_message_view_hint_text),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ConversationComposeAttachmentMenu(
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
            shape = RoundedCornerShape(size = 24.dp),
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

internal data class ConversationComposeBarPresentation(
    val fieldShape: RoundedCornerShape,
    val fieldColors: TextFieldColors,
)
