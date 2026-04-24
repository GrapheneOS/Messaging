package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.v2.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.CONVERSATION_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.v2.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.v2.conversationShape
import com.android.messaging.ui.core.AppTheme

internal val AUDIO_RECORD_CANCEL_THRESHOLD = 96.dp
internal val AUDIO_RECORD_LOCK_THRESHOLD = 72.dp

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
    onLockedAudioRecordingStartRequest: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingCancel: () -> Unit,
    onSendClick: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()
    val hapticFeedback = LocalHapticFeedback.current

    var recordingGestureState by remember {
        mutableStateOf(ConversationSendActionButtonGestureState())
    }

    LaunchedEffect(audioRecording.phase) {
        if (audioRecording.phase != ConversationAudioRecordingPhase.Recording) {
            recordingGestureState = ConversationSendActionButtonGestureState()
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
            recordingGestureState = recordingGestureState,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = {
                recordingGestureState = ConversationSendActionButtonGestureState()
                onAudioRecordingStartRequest()
            },
            onAudioRecordingDrag = { gestureState ->
                recordingGestureState = gestureState
            },
            onAudioRecordingLock = {
                if (audioRecording.isLocked) {
                    return@ConversationComposeInputContent false
                }

                recordingGestureState = ConversationSendActionButtonGestureState()
                val didLockRecording = onAudioRecordingLock()
                if (didLockRecording) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                didLockRecording
            },
            onAudioRecordingFinish = { shouldCancelRecording ->
                recordingGestureState = ConversationSendActionButtonGestureState()
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
    recordingGestureState: ConversationSendActionButtonGestureState,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingDrag: (ConversationSendActionButtonGestureState) -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingFinish: (Boolean) -> Unit,
    onSendClick: () -> Unit,
) {
    val cancelThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_CANCEL_THRESHOLD.toPx()
    }
    val lockThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_LOCK_THRESHOLD.toPx()
    }
    val cancelProgress = (recordingGestureState.cancelDragDistancePx / cancelThresholdPx)
        .coerceIn(minimumValue = 0f, maximumValue = 1f)

    val lockProgress = when {
        audioRecording.isLocked -> 1f

        else -> {
            (recordingGestureState.lockDragDistancePx / lockThresholdPx)
                .coerceIn(minimumValue = 0f, maximumValue = 1f)
        }
    }

    val isCancellationArmed = cancelProgress >= 1f
    val isActiveRecording = audioRecording.phase == ConversationAudioRecordingPhase.Recording
    val isRecordMode = shouldShowRecordAction || isActiveRecording
    val isRecordingControlEnabled = when {
        isActiveRecording -> true
        isRecordMode -> isRecordActionEnabled
        else -> isSendActionEnabled
    }

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
            modifier = Modifier.weight(weight = 1f),
            targetState = isActiveRecording,
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
                        isAudioRecordActionEnabled = isRecordActionEnabled,
                        onContactAttachClick = onContactAttachClick,
                        onMediaPickerClick = onMediaPickerClick,
                        onAudioAttachClick = onLockedAudioRecordingStartRequest,
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
            enabled = isRecordingControlEnabled,
            mode = when {
                isRecordMode && audioRecording.isLocked -> ConversationSendActionButtonMode.Stop
                isRecordMode -> ConversationSendActionButtonMode.Record
                else -> ConversationSendActionButtonMode.Send
            },
            isRecordingActive = isActiveRecording,
            isRecordingLocked = audioRecording.isLocked,
            shouldShowLockAffordance = isActiveRecording && !audioRecording.isLocked,
            lockProgress = lockProgress,
            onClick = onSendClick,
            onLockedStopClick = {
                onAudioRecordingFinish(false)
            },
            onRecordGestureStart = onAudioRecordingStartRequest,
            onRecordGestureMove = onAudioRecordingDrag,
            onRecordGestureLock = onAudioRecordingLock,
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
    isAudioRecordActionEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onAudioAttachClick: () -> Unit,
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
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG),
                enabled = isAttachmentActionEnabled,
                isAudioRecordActionEnabled = isAudioRecordActionEnabled,
                onContactAttachClick = onContactAttachClick,
                onMediaPickerClick = onMediaPickerClick,
                onAudioAttachClick = onAudioAttachClick,
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
        ) {
            ConversationComposeAttachmentMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG),
                imageVector = Icons.Rounded.Image,
                textResId = R.string.mediapicker_gallery_title,
                onClick = {
                    closeMenuAndRun(action = onMediaPickerClick)
                },
            )
            ConversationComposeAttachmentMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG),
                imageVector = Icons.Rounded.Mic,
                textResId = R.string.mediapicker_audio_title,
                enabled = isAudioRecordActionEnabled,
                onClick = {
                    closeMenuAndRun(action = onAudioAttachClick)
                },
            )

            ConversationComposeAttachmentMenuItem(
                modifier = Modifier.testTag(CONVERSATION_ATTACHMENT_CONTACT_MENU_ITEM_TEST_TAG),
                imageVector = Icons.Rounded.Person,
                textResId = R.string.mediapicker_contact_title,
                onClick = {
                    closeMenuAndRun(action = onContactAttachClick)
                },
            )
        }
    }
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

@Composable
private fun ConversationComposeSendAction(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    isRecordingActive: Boolean,
    isRecordingLocked: Boolean,
    shouldShowLockAffordance: Boolean,
    lockProgress: Float,
    onClick: () -> Unit,
    onLockedStopClick: () -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.heightIn(
            min = 56.dp,
            max = 56.dp,
        ),
    ) {
        ConversationSendActionButton(
            modifier = modifier,
            enabled = enabled,
            mode = mode,
            isRecordingActive = isRecordingActive,
            isRecordingLocked = isRecordingLocked,
            onClick = onClick,
            onLockedStopClick = onLockedStopClick,
            onRecordGestureStart = onRecordGestureStart,
            onRecordGestureMove = onRecordGestureMove,
            onRecordGestureLock = onRecordGestureLock,
            onRecordGestureFinish = onRecordGestureFinish,
        )

        if (shouldShowLockAffordance) {
            ConversationAudioRecordingLockAffordance(
                modifier = Modifier
                    .align(alignment = Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .offset(y = (-74).dp),
                lockProgress = lockProgress,
            )
        }
    }
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
