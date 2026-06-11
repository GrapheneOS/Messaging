package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SEGMENT_COUNTER_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSegmentCounterUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSendActionButtonGestureState
import com.android.messaging.ui.conversation.composer.model.ConversationSendActionButtonMode
import com.android.messaging.ui.conversation.conversationShape
import com.android.messaging.ui.conversation.preview.previewComposerUiState
import com.android.messaging.ui.conversation.preview.previewRecordingComposer
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.core.MessagingPreviewTheme

internal val AUDIO_RECORD_CANCEL_THRESHOLD = 96.dp
internal val AUDIO_RECORD_LOCK_THRESHOLD = 72.dp

private const val CONTENT_SWAP_ENTER_FADE_DURATION_MILLIS = 160
private const val CONTENT_SWAP_ENTER_SLIDE_DURATION_MILLIS = 220
private const val CONTENT_SWAP_ENTER_SLIDE_OFFSET_DIVISOR = 10
private const val CONTENT_SWAP_EXIT_FADE_DURATION_MILLIS = 120
private const val CONTENT_SWAP_EXIT_SLIDE_DURATION_MILLIS = 180
private const val CONTENT_SWAP_EXIT_SLIDE_OFFSET_DIVISOR = 12

private const val PREVIEW_MULTILINE_MESSAGE_TEXT = "Can you review these before tonight?\n" +
    "First pass is enough for the meeting.\n" +
    "I will send the final attachment after dinner."
private const val PREVIEW_OVERFLOW_MESSAGE_TEXT =
    "PreviewOverflowTokenWithoutBreaksABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" +
        "PreviewOverflowTokenWithoutBreaksABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private const val PREVIEW_MMS_BODY_TEXT =
    "Photo attached. I am adding the complete notes from the trip, " +
        "including the hotel confirmation, train times, restaurant address, " +
        "and a few reminders about what " +
        "still needs to be booked before everyone arrives tomorrow afternoon."

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    subjectText: String,
    sendProtocol: ConversationDraftSendProtocol,
    segmentCounter: ConversationSegmentCounterUiState?,
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
    onSendActionLongClick: () -> Unit,
    onSubjectChipClick: () -> Unit,
    onSubjectChipClear: () -> Unit,
) {
    val presentation = rememberConversationComposeBarPresentation()
    val recordingGestureController = rememberConversationAudioRecordingGestureController(
        audioRecording = audioRecording,
        onAudioRecordingStartRequest = onAudioRecordingStartRequest,
        onAudioRecordingFinish = onAudioRecordingFinish,
        onAudioRecordingLock = onAudioRecordingLock,
        onAudioRecordingCancel = onAudioRecordingCancel,
    )

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
            subjectText = subjectText,
            sendProtocol = sendProtocol,
            segmentCounter = segmentCounter,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            isSendActionEnabled = isSendActionEnabled,
            shouldShowRecordAction = shouldShowRecordAction,
            recordingGestureState = recordingGestureController.recordingGestureState,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onAudioRecordingStartRequest = recordingGestureController.onAudioRecordingStartRequest,
            onAudioRecordingDrag = recordingGestureController.onAudioRecordingDrag,
            onAudioRecordingLock = recordingGestureController.onAudioRecordingLock,
            onAudioRecordingFinish = recordingGestureController.onAudioRecordingFinish,
            onSendClick = onSendClick,
            onSendActionLongClick = onSendActionLongClick,
            onSubjectChipClick = onSubjectChipClick,
            onSubjectChipClear = onSubjectChipClear,
        )
    }
}

@Composable
private fun rememberConversationAudioRecordingGestureController(
    audioRecording: ConversationAudioRecordingUiState,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingFinish: () -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingCancel: () -> Unit,
): ConversationAudioRecordingGestureController {
    val hapticFeedback = LocalHapticFeedback.current

    var recordingGestureState by remember {
        mutableStateOf(ConversationSendActionButtonGestureState())
    }

    LaunchedEffect(audioRecording.phase) {
        if (audioRecording.phase != ConversationAudioRecordingPhase.Recording) {
            recordingGestureState = ConversationSendActionButtonGestureState()
        }
    }

    return ConversationAudioRecordingGestureController(
        recordingGestureState = recordingGestureState,
        onAudioRecordingStartRequest = {
            recordingGestureState = ConversationSendActionButtonGestureState()
            onAudioRecordingStartRequest()
        },
        onAudioRecordingDrag = { gestureState ->
            recordingGestureState = gestureState
        },
        onAudioRecordingLock = {
            when {
                audioRecording.isLocked -> false

                else -> {
                    recordingGestureState = ConversationSendActionButtonGestureState()
                    val didLockRecording = onAudioRecordingLock()
                    if (didLockRecording) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    }
                    didLockRecording
                }
            }
        },
        onAudioRecordingFinish = { shouldCancelRecording ->
            recordingGestureState = ConversationSendActionButtonGestureState()
            when {
                shouldCancelRecording -> onAudioRecordingCancel()
                else -> onAudioRecordingFinish()
            }
        },
    )
}

@Composable
internal fun ConversationComposeInputContent(
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    subjectText: String,
    sendProtocol: ConversationDraftSendProtocol,
    segmentCounter: ConversationSegmentCounterUiState?,
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
    onSendActionLongClick: () -> Unit,
    onSubjectChipClick: () -> Unit,
    onSubjectChipClear: () -> Unit,
) {
    val inputState = conversationComposeInputState(
        audioRecording = audioRecording,
        recordingGestureState = recordingGestureState,
        shouldShowRecordAction = shouldShowRecordAction,
        isRecordActionEnabled = isRecordActionEnabled,
        isSendActionEnabled = isSendActionEnabled,
    )

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
        ConversationComposeMessageRecordingContent(
            modifier = Modifier.weight(weight = 1f),
            messageText = messageText,
            subjectText = subjectText,
            sendProtocol = sendProtocol,
            durationMillis = audioRecording.durationMillis,
            inputState = inputState,
            isMessageFieldEnabled = isMessageFieldEnabled,
            isAttachmentActionEnabled = isAttachmentActionEnabled,
            isRecordActionEnabled = isRecordActionEnabled,
            messageFieldFocusRequester = messageFieldFocusRequester,
            presentation = presentation,
            onContactAttachClick = onContactAttachClick,
            onMediaPickerClick = onMediaPickerClick,
            onLockedAudioRecordingStartRequest = onLockedAudioRecordingStartRequest,
            onMessageTextChange = onMessageTextChange,
            onSubjectChipClick = onSubjectChipClick,
            onSubjectChipClear = onSubjectChipClear,
        )

        ConversationComposeInputSendAction(
            audioRecording = audioRecording,
            inputState = inputState,
            segmentCounter = segmentCounter,
            onSendClick = onSendClick,
            onSendActionLongClick = onSendActionLongClick,
            onAudioRecordingStartRequest = onAudioRecordingStartRequest,
            onAudioRecordingDrag = onAudioRecordingDrag,
            onAudioRecordingLock = onAudioRecordingLock,
            onAudioRecordingFinish = onAudioRecordingFinish,
        )
    }
}

@Composable
private fun ConversationComposeInputSendAction(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    inputState: ConversationComposeInputState,
    segmentCounter: ConversationSegmentCounterUiState?,
    onSendClick: () -> Unit,
    onSendActionLongClick: () -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingDrag: (ConversationSendActionButtonGestureState) -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingFinish: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (segmentCounter != null && !inputState.isActiveRecording) {
            SegmentCounterIndicator(state = segmentCounter)
        }

        ConversationComposeSendAction(
            modifier = Modifier
                .testTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
                .semantics {
                    conversationShape = CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
                },
            enabled = inputState.isRecordingControlEnabled,
            mode = conversationComposeSendActionMode(
                isRecordMode = inputState.isRecordMode,
                isRecordingLocked = audioRecording.isLocked,
            ),
            isRecordingActive = inputState.isActiveRecording,
            isRecordingLocked = audioRecording.isLocked,
            shouldShowLockAffordance = inputState.isActiveRecording && !audioRecording.isLocked,
            lockProgress = inputState.lockProgress,
            onClick = onSendClick,
            onLockedStopClick = {
                onAudioRecordingFinish(false)
            },
            onRecordGestureStart = onAudioRecordingStartRequest,
            onRecordGestureMove = onAudioRecordingDrag,
            onRecordGestureLock = onAudioRecordingLock,
            onRecordGestureFinish = onAudioRecordingFinish,
            onSendActionLongClick = onSendActionLongClick,
        )
    }
}

@Composable
private fun conversationComposeInputState(
    audioRecording: ConversationAudioRecordingUiState,
    recordingGestureState: ConversationSendActionButtonGestureState,
    shouldShowRecordAction: Boolean,
    isRecordActionEnabled: Boolean,
    isSendActionEnabled: Boolean,
): ConversationComposeInputState {
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
    val isActiveRecording = audioRecording.phase == ConversationAudioRecordingPhase.Recording
    val isRecordMode = shouldShowRecordAction || isActiveRecording

    val isRecordingControlEnabled = when {
        isActiveRecording -> true
        isRecordMode -> isRecordActionEnabled
        else -> isSendActionEnabled
    }

    return ConversationComposeInputState(
        cancelProgress = cancelProgress,
        lockProgress = lockProgress,
        isCancellationArmed = cancelProgress >= 1f,
        isActiveRecording = isActiveRecording,
        isRecordMode = isRecordMode,
        isRecordingControlEnabled = isRecordingControlEnabled,
    )
}

@Composable
private fun ConversationComposeMessageRecordingContent(
    modifier: Modifier = Modifier,
    messageText: String,
    subjectText: String,
    sendProtocol: ConversationDraftSendProtocol,
    durationMillis: Long,
    inputState: ConversationComposeInputState,
    isMessageFieldEnabled: Boolean,
    isAttachmentActionEnabled: Boolean,
    isRecordActionEnabled: Boolean,
    messageFieldFocusRequester: FocusRequester?,
    presentation: ConversationComposeBarPresentation,
    onContactAttachClick: () -> Unit,
    onMediaPickerClick: () -> Unit,
    onLockedAudioRecordingStartRequest: () -> Unit,
    onMessageTextChange: (String) -> Unit,
    onSubjectChipClick: () -> Unit,
    onSubjectChipClear: () -> Unit,
) {
    val isInputActionEnabled = !inputState.isActiveRecording

    Surface(
        modifier = modifier,
        shape = presentation.fieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            if (!subjectText.isBlank()) {
                ConversationSubjectChip(
                    subjectText = subjectText,
                    onClick = onSubjectChipClick,
                    onClear = onSubjectChipClear,
                )
            }

            Box {
                ConversationComposeMessageField(
                    modifier = Modifier.fillMaxWidth(),
                    value = messageText,
                    onValueChange = { updatedMessageText ->
                        if (!inputState.isActiveRecording) {
                            onMessageTextChange(updatedMessageText)
                        }
                    },
                    enabled = isMessageFieldEnabled,
                    sendProtocol = sendProtocol,
                    isVisuallyHidden = inputState.isActiveRecording,
                    messageFieldFocusRequester = messageFieldFocusRequester,
                    presentation = presentation,
                    isAttachmentActionEnabled = isAttachmentActionEnabled && isInputActionEnabled,
                    isAudioRecordActionEnabled = isRecordActionEnabled && isInputActionEnabled,
                    onContactAttachClick = onContactAttachClick,
                    onMediaPickerClick = onMediaPickerClick,
                    onAudioAttachClick = onLockedAudioRecordingStartRequest,
                )

                ConversationAudioRecordingContentOverlay(
                    modifier = Modifier
                        .matchParentSize()
                        .consumeRecordingInputTouches(
                            isActiveRecording = inputState.isActiveRecording,
                        ),
                    isActiveRecording = inputState.isActiveRecording,
                    durationMillis = durationMillis,
                    cancelProgress = inputState.cancelProgress,
                    isCancellationArmed = inputState.isCancellationArmed,
                )
            }
        }
    }
}

private fun Modifier.consumeRecordingInputTouches(isActiveRecording: Boolean): Modifier {
    return when {
        isActiveRecording -> {
            pointerInput(Unit) {
                awaitEachGesture {
                    val downChange = awaitFirstDown(requireUnconsumed = false)
                    downChange.consume()

                    var isAnyPointerPressed: Boolean

                    do {
                        val event = awaitPointerEvent()

                        event.changes.forEach { change ->
                            change.consume()
                        }

                        isAnyPointerPressed = event.changes.any { change ->
                            change.pressed
                        }
                    } while (isAnyPointerPressed)
                }
            }
        }

        else -> this
    }
}

@Composable
private fun ConversationAudioRecordingContentOverlay(
    modifier: Modifier = Modifier,
    isActiveRecording: Boolean,
    durationMillis: Long,
    cancelProgress: Float,
    isCancellationArmed: Boolean,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = isActiveRecording,
        transitionSpec = {
            contentSwapTransition()
        },
        label = "conversation_compose_content",
    ) { isRecording ->
        when {
            isRecording -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    ConversationAudioRecordingBar(
                        durationMillis = durationMillis,
                        cancelProgress = cancelProgress,
                        isCancellationArmed = isCancellationArmed,
                    )
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun conversationComposeSendActionMode(
    isRecordMode: Boolean,
    isRecordingLocked: Boolean,
): ConversationSendActionButtonMode {
    return when {
        isRecordMode && isRecordingLocked -> ConversationSendActionButtonMode.Stop
        isRecordMode -> ConversationSendActionButtonMode.Record
        else -> ConversationSendActionButtonMode.Send
    }
}

private fun contentSwapTransition(): ContentTransform {
    val enterTransition = fadeIn(
        animationSpec = tween(durationMillis = CONTENT_SWAP_ENTER_FADE_DURATION_MILLIS),
    ) + slideInHorizontally(
        animationSpec = tween(durationMillis = CONTENT_SWAP_ENTER_SLIDE_DURATION_MILLIS),
        initialOffsetX = { fullWidth ->
            fullWidth / CONTENT_SWAP_ENTER_SLIDE_OFFSET_DIVISOR
        },
    )

    val exitTransition = fadeOut(
        animationSpec = tween(durationMillis = CONTENT_SWAP_EXIT_FADE_DURATION_MILLIS),
    ) + slideOutHorizontally(
        animationSpec = tween(durationMillis = CONTENT_SWAP_EXIT_SLIDE_DURATION_MILLIS),
        targetOffsetX = { fullWidth ->
            -(fullWidth / CONTENT_SWAP_EXIT_SLIDE_OFFSET_DIVISOR)
        },
    )

    return enterTransition.togetherWith(exitTransition)
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
    onSendActionLongClick: () -> Unit,
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
            onSendActionLongClick = onSendActionLongClick,
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

@Composable
private fun SegmentCounterIndicator(
    state: ConversationSegmentCounterUiState,
) {
    val displayText = when {
        state.messageCount > 1 -> stringResource(
            id = R.string.conversation_segment_counter_multi,
            state.codePointsRemainingInCurrentMessage,
            state.messageCount,
        )

        else -> state.codePointsRemainingInCurrentMessage.toString()
    }

    val accessibilityDescription = when {
        state.messageCount > 1 -> pluralStringResource(
            id = R.plurals.conversation_segment_counter_content_description,
            count = state.codePointsRemainingInCurrentMessage,
            state.codePointsRemainingInCurrentMessage,
            state.messageCount,
        )

        else -> pluralStringResource(
            id = R.plurals.conversation_segment_counter_single_content_description,
            count = state.codePointsRemainingInCurrentMessage,
            state.codePointsRemainingInCurrentMessage,
        )
    }

    Text(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clearAndSetSemantics {
                testTag = CONVERSATION_SEGMENT_COUNTER_TEST_TAG
                contentDescription = accessibilityDescription
                text = AnnotatedString(text = displayText)
            },
        text = displayText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class ConversationComposeInputState(
    val cancelProgress: Float,
    val lockProgress: Float,
    val isCancellationArmed: Boolean,
    val isActiveRecording: Boolean,
    val isRecordMode: Boolean,
    val isRecordingControlEnabled: Boolean,
)

private data class ConversationAudioRecordingGestureController(
    val recordingGestureState: ConversationSendActionButtonGestureState,
    val onAudioRecordingStartRequest: () -> Unit,
    val onAudioRecordingDrag: (ConversationSendActionButtonGestureState) -> Unit,
    val onAudioRecordingLock: () -> Boolean,
    val onAudioRecordingFinish: (Boolean) -> Unit,
)

@PreviewLightDark
@Composable
private fun ConversationComposeBarTextPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            PreviewConversationComposeBar(
                uiState = previewComposerUiState(),
            )
            PreviewConversationComposeBar(
                uiState = previewComposerUiState(messageText = PREVIEW_MULTILINE_MESSAGE_TEXT),
            )
            PreviewConversationComposeBar(
                uiState = previewComposerUiState(messageText = PREVIEW_OVERFLOW_MESSAGE_TEXT),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationComposeBarSubjectPreview() {
    MessagingPreviewTheme {
        PreviewConversationComposeBar(
            uiState = previewComposerUiState(
                messageText = PREVIEW_MMS_BODY_TEXT,
                subjectText = "Trip updates",
            ).copy(sendProtocol = ConversationDraftSendProtocol.MMS),
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationComposeBarRecordingPreview() {
    MessagingPreviewTheme {
        PreviewConversationComposeBar(
            uiState = previewRecordingComposer(isLocked = false),
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationComposeBarLockedRecordingPreview() {
    MessagingPreviewTheme {
        PreviewConversationComposeBar(
            uiState = previewRecordingComposer(isLocked = true),
        )
    }
}

@Composable
private fun PreviewConversationComposeBar(uiState: ConversationComposerUiState) {
    ConversationComposeBar(
        audioRecording = uiState.audioRecording,
        messageText = uiState.messageText,
        subjectText = uiState.subjectText,
        sendProtocol = uiState.sendProtocol,
        segmentCounter = uiState.segmentCounter,
        isMessageFieldEnabled = uiState.isMessageFieldEnabled,
        isAttachmentActionEnabled = uiState.isAttachmentActionEnabled,
        isRecordActionEnabled = uiState.isRecordActionEnabled,
        isSendActionEnabled = uiState.isSendEnabled,
        shouldShowRecordAction = uiState.shouldShowRecordAction,
        onContactAttachClick = {},
        onMediaPickerClick = {},
        onLockedAudioRecordingStartRequest = {},
        onMessageTextChange = { _ -> },
        onAudioRecordingStartRequest = {},
        onAudioRecordingFinish = {},
        onAudioRecordingLock = { true },
        onAudioRecordingCancel = {},
        onSendClick = {},
        onSendActionLongClick = {},
        onSubjectChipClick = {},
        onSubjectChipClear = {},
    )
}
