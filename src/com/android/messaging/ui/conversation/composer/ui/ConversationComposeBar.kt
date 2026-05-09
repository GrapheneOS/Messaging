package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_SHAPE_CIRCLE
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSendActionButtonGestureState
import com.android.messaging.ui.conversation.composer.model.ConversationSendActionButtonMode
import com.android.messaging.ui.conversation.conversationShape

internal val AUDIO_RECORD_CANCEL_THRESHOLD = 96.dp
internal val AUDIO_RECORD_LOCK_THRESHOLD = 72.dp

private const val CONTENT_SWAP_ENTER_FADE_DURATION_MILLIS = 160
private const val CONTENT_SWAP_ENTER_SLIDE_DURATION_MILLIS = 220
private const val CONTENT_SWAP_ENTER_SLIDE_OFFSET_DIVISOR = 10
private const val CONTENT_SWAP_EXIT_FADE_DURATION_MILLIS = 120
private const val CONTENT_SWAP_EXIT_SLIDE_DURATION_MILLIS = 180
private const val CONTENT_SWAP_EXIT_SLIDE_OFFSET_DIVISOR = 12

@Composable
internal fun ConversationComposeBar(
    modifier: Modifier = Modifier,
    audioRecording: ConversationAudioRecordingUiState,
    messageText: String,
    subjectText: String,
    sendProtocol: ConversationDraftSendProtocol,
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
            onSendClick = onSendClick,
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
    onSendClick: () -> Unit,
    onAudioRecordingStartRequest: () -> Unit,
    onAudioRecordingDrag: (ConversationSendActionButtonGestureState) -> Unit,
    onAudioRecordingLock: () -> Boolean,
    onAudioRecordingFinish: (Boolean) -> Unit,
) {
    ConversationComposeSendAction(
        modifier = modifier
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
    )
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
                    isAttachmentActionEnabled = isAttachmentActionEnabled,
                    isAudioRecordActionEnabled = isRecordActionEnabled,
                    onContactAttachClick = onContactAttachClick,
                    onMediaPickerClick = onMediaPickerClick,
                    onAudioAttachClick = onLockedAudioRecordingStartRequest,
                )

                ConversationAudioRecordingContentOverlay(
                    modifier = Modifier.matchParentSize(),
                    isActiveRecording = inputState.isActiveRecording,
                    durationMillis = durationMillis,
                    cancelProgress = inputState.cancelProgress,
                    isCancellationArmed = inputState.isCancellationArmed,
                )
            }
        }
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
