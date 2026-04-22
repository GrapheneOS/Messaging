package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.conversationShape
import com.android.messaging.ui.core.AppTheme

private val AUDIO_RECORD_CANCEL_THRESHOLD = 96.dp

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    shouldShowRecordAction: Boolean,
    messageFieldFocusRequester: FocusRequester? = null,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingCancel: () -> Unit,
    onSendClick: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()

    var recordingDragDistancePx by remember {
        mutableFloatStateOf(value = 0f)
    }

    LaunchedEffect(audioRecording.isRecording) {
        if (!audioRecording.isRecording) {
            recordingDragDistancePx = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .testTag(CONVERSATION_COMPOSE_BAR_TEST_TAG),
    ) {
        ConversationComposeInputContent(
            audioRecording = audioRecording,
            messageText = messageText,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            recordingDragDistancePx = recordingDragDistancePx,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = {
                recordingDragDistancePx = 0f
                onAudioRecordingStartRequest()
            },
            onAudioRecordingDrag = { dragDistancePx ->
                recordingDragDistancePx = dragDistancePx
            },
            onAudioRecordingFinish = { shouldCancelRecording ->
                recordingDragDistancePx = 0f
                when {
                    shouldCancelRecording -> onAudioRecordingCancel()
                    else -> onAudioRecordingFinish()
                }
            },
            onSendClick = onSendClick,
        )
    }
}

@Composable
private fun rememberConversationComposeBarPresentation(): ConversationComposeBarPresentation {
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
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
private fun ConversationComposeInputContent(
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
    shouldShowRecordAction: Boolean,
    recordingDragDistancePx: Float,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingDrag: (Float) -> Unit,
    onAudioRecordingFinish: (Boolean) -> Unit,
    onSendClick: () -> Unit,
) {
    val cancelThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_CANCEL_THRESHOLD.toPx()
    }
    val cancelProgress = (recordingDragDistancePx / cancelThresholdPx)
        .coerceIn(minimumValue = 0f, maximumValue = 1f)

    val isCancellationArmed = cancelProgress >= 1f
    val isRecordMode = shouldShowRecordAction || audioRecording.isRecording

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
        ),
        verticalAlignment = Alignment.Bottom,
    ) {
        AnimatedContent(
            modifier = Modifier
                .weight(weight = 1f),
            targetState = audioRecording.isRecording,
            transitionSpec = {
                contentSwapTransition()
            },
            label = "conversation_compose_content",
        ) { isRecording ->
            when {
                isRecording -> {
                    ConversationAudioRecordingBar(
                        durationMillis = audioRecording.durationMillis,
                        cancelProgress = cancelProgress,
                        isCancellationArmed = isCancellationArmed,
                    )
                }

                else -> {
                    ConversationComposeMessageField(
                        modifier = Modifier,
                        value = messageText,
                        onValueChange = onMessageTextChange,
                        enabled = isMessageFieldEnabled,
                        messageFieldFocusRequester = messageFieldFocusRequester,
                        presentation = presentation,
                        isAttachmentActionEnabled = isAttachmentActionEnabled,
                        onContactAttachClick = onContactAttachClick,
                        onMediaPickerClick = onMediaPickerClick,
                    )
                }
            }
        }

        ConversationComposeSendAction(
            modifier = Modifier
                .testTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
                .semantics {
                    conversationShape = CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
                },
            enabled = when {
                isRecordMode -> isRecordActionEnabled
                else -> isSendActionEnabled
            },
            mode = when {
                isRecordMode -> ConversationSendActionButtonMode.Record
                else -> ConversationSendActionButtonMode.Send
            },
            isRecordingActive = audioRecording.isRecording,
            onClick = onSendClick,
            onRecordGestureStart = onAudioRecordingStartRequest,
            onRecordGestureMove = onAudioRecordingDrag,
            onRecordGestureFinish = onAudioRecordingFinish,
        )
    }
}

@Composable
private fun ConversationComposeMessageField(
    modifier: Modifier = Modifier,
    value: String,
    enabled: Boolean,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    isAttachmentActionEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
) {
    val focusRequesterModifier = messageFieldFocusRequester
        ?.let(Modifier::focusRequester)
        ?: Modifier

    TextField(
        modifier = modifier
            .then(focusRequesterModifier)
            .testTag(CONVERSATION_TEXT_FIELD_TEST_TAG)
            .heightIn(min = 56.dp),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        shape = presentation.fieldShape,
        colors = presentation.fieldColors,
        placeholder = ::ConversationComposePlaceholder,
        leadingIcon = {
            ConversationComposeAttachmentMenu(
                modifier = Modifier
                    .testTag(CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG),
                enabled = isAttachmentActionEnabled,
                onContactAttachClick = onContactAttachClick,
                onMediaPickerClick = onMediaPickerClick,
            )
        },
        minLines = 1,
        maxLines = 4,
    )
}

@Composable
private fun ConversationComposePlaceholder() {
    Text(
        text = stringResource(id = R.string.compose_message_view_hint_text),
        style = MaterialTheme.typography.bodyLarge,
    )
}

private fun contentSwapTransition(): ContentTransform {
    return (
        fadeIn(animationSpec = tween(durationMillis = 160)) +
            slideInHorizontally(
                animationSpec = tween(durationMillis = 220),
                initialOffsetX = { fullWidth ->
                    fullWidth / 10
                },
            )
        ).togetherWith(
        fadeOut(animationSpec = tween(durationMillis = 120)) +
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 180),
                targetOffsetX = { fullWidth ->
                    -(fullWidth / 12)
                },
            ),
    )
}

@Composable
private fun ConversationComposeAttachmentMenu(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isExpanded by rememberSaveable {
        mutableStateOf(value = false)
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
            offset = DpOffset(
                x = 0.dp,
                y = (-8).dp,
            ),
        ) {
            DropdownMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG),
                text = {
                    Text(text = stringResource(id = R.string.mediapicker_gallery_title))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                    )
                },
                onClick = {
                    isExpanded = false
                    onMediaPickerClick()
                },
            )
            DropdownMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG),
                text = {
                    Text(text = stringResource(id = R.string.mediapicker_contact_title))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                    )
                },
                onClick = {
                    isExpanded = false
                    onContactAttachClick()
                },
            )
        }
    }
}

@Composable
private fun ConversationComposeSendAction(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    isRecordingActive: Boolean,
    onClick: () -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (Float) -> Unit,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    ConversationSendActionButton(
        modifier = modifier,
        enabled = enabled,
        mode = mode,
        isRecordingActive = isRecordingActive,
        onClick = onClick,
        onRecordGestureStart = onRecordGestureStart,
        onRecordGestureMove = onRecordGestureMove,
        onRecordGestureFinish = onRecordGestureFinish,
    )
}

private data class ConversationComposeBarPresentation(
    val fieldShape: RoundedCornerShape,
    val fieldColors: TextFieldColors,
)

@Composable
private fun ConversationComposeBarPreviewContainer(
    content: @Composable () -> Unit,
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .padding(vertical = 24.dp),
        ) {
            content()
        }
    }
}
