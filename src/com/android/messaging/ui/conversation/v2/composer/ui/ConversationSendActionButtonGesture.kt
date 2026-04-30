package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.android.messaging.ui.conversation.v2.composer.model.ConversationSendActionButtonGestureState
import com.android.messaging.ui.conversation.v2.composer.model.ConversationSendActionButtonMode

@Composable
internal fun Modifier.conversationSendActionButtonGesture(
    mode: ConversationSendActionButtonMode,
    enabled: Boolean,
    cancelThresholdPx: Float,
    lockThresholdPx: Float,
    isRecordingActive: Boolean,
    isRecordingLocked: Boolean,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
    onLockedStopClick: () -> Unit,
): Modifier {
    val currentIsRecordingActive by rememberUpdatedState(newValue = isRecordingActive)
    val currentIsRecordingLocked by rememberUpdatedState(newValue = isRecordingLocked)
    val currentOnGestureActiveChange by rememberUpdatedState(newValue = onGestureActiveChange)
    val currentOnRecordGestureStart by rememberUpdatedState(newValue = onRecordGestureStart)
    val currentOnRecordGestureMove by rememberUpdatedState(newValue = onRecordGestureMove)
    val currentOnRecordGestureLock by rememberUpdatedState(newValue = onRecordGestureLock)
    val currentOnRecordGestureFinish by rememberUpdatedState(newValue = onRecordGestureFinish)
    val currentOnLockedStopClick by rememberUpdatedState(newValue = onLockedStopClick)

    if (mode == ConversationSendActionButtonMode.Send || !enabled) {
        return this
    }

    return pointerInput(
        mode,
        cancelThresholdPx,
        lockThresholdPx,
    ) {
        awaitEachGesture {
            if (currentIsRecordingActive && currentIsRecordingLocked) {
                handleLockedRecordGesture(
                    cancelThresholdPx = cancelThresholdPx,
                    onGestureActiveChange = currentOnGestureActiveChange,
                    onRecordGestureMove = currentOnRecordGestureMove,
                    onRecordGestureFinish = currentOnRecordGestureFinish,
                    onLockedStopClick = currentOnLockedStopClick,
                )
            } else {
                handleRecordGesture(
                    cancelThresholdPx = cancelThresholdPx,
                    lockThresholdPx = lockThresholdPx,
                    onGestureActiveChange = currentOnGestureActiveChange,
                    onRecordGestureStart = currentOnRecordGestureStart,
                    onRecordGestureMove = currentOnRecordGestureMove,
                    onRecordGestureLock = currentOnRecordGestureLock,
                    onRecordGestureFinish = currentOnRecordGestureFinish,
                )
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.handleRecordGesture(
    cancelThresholdPx: Float,
    lockThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    val initialDown = awaitFirstDown(requireUnconsumed = false)

    val longPressChange = awaitLongPressOrCancellation(pointerId = initialDown.id)
        ?: return

    onGestureActiveChange(true)
    onRecordGestureStart()

    trackRecordGestureDrag(
        initialDown = initialDown,
        longPressChange = longPressChange,
        cancelThresholdPx = cancelThresholdPx,
        lockThresholdPx = lockThresholdPx,
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
        onRecordGestureLock = onRecordGestureLock,
        onRecordGestureFinish = onRecordGestureFinish,
    )
}

private suspend fun AwaitPointerEventScope.trackRecordGestureDrag(
    initialDown: PointerInputChange,
    longPressChange: PointerInputChange,
    cancelThresholdPx: Float,
    lockThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    var isRecordingLocked = false

    longPressChange.consume()

    val releaseGestureState = awaitRecordGestureRelease(initialDown = initialDown) { gestureState ->
        isRecordingLocked = updateRecordGestureLockState(
            gestureState = gestureState,
            isRecordingLocked = isRecordingLocked,
            lockThresholdPx = lockThresholdPx,
            onRecordGestureMove = onRecordGestureMove,
            onRecordGestureLock = onRecordGestureLock,
        )
    }

    resetRecordGestureDragUi(
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
    )

    if (releaseGestureState != null && !isRecordingLocked) {
        onRecordGestureFinish(releaseGestureState.cancelDragDistancePx >= cancelThresholdPx)
    }
}

private fun updateRecordGestureLockState(
    gestureState: ConversationSendActionButtonGestureState,
    isRecordingLocked: Boolean,
    lockThresholdPx: Float,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
): Boolean {
    var updatedIsRecordingLocked = isRecordingLocked

    if (!updatedIsRecordingLocked) {
        onRecordGestureMove(gestureState)

        if (gestureState.lockDragDistancePx >= lockThresholdPx) {
            updatedIsRecordingLocked = onRecordGestureLock()

            if (updatedIsRecordingLocked) {
                onRecordGestureMove(ConversationSendActionButtonGestureState())
            }
        }
    }

    return updatedIsRecordingLocked
}

private suspend fun AwaitPointerEventScope.handleLockedRecordGesture(
    cancelThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureFinish: (Boolean) -> Unit,
    onLockedStopClick: () -> Unit,
) {
    val initialDown = awaitFirstDown(requireUnconsumed = false)

    onGestureActiveChange(true)
    initialDown.consume()

    val releaseGestureState = awaitRecordGestureRelease(initialDown = initialDown) { gestureState ->
        onRecordGestureMove(
            ConversationSendActionButtonGestureState(
                cancelDragDistancePx = gestureState.cancelDragDistancePx,
            ),
        )
    }

    resetRecordGestureDragUi(
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
    )

    if (releaseGestureState != null) {
        handleLockedRecordGestureRelease(
            gestureState = releaseGestureState,
            cancelThresholdPx = cancelThresholdPx,
            onRecordGestureFinish = onRecordGestureFinish,
            onLockedStopClick = onLockedStopClick,
        )
    }
}

private fun handleLockedRecordGestureRelease(
    gestureState: ConversationSendActionButtonGestureState,
    cancelThresholdPx: Float,
    onRecordGestureFinish: (Boolean) -> Unit,
    onLockedStopClick: () -> Unit,
) {
    when {
        gestureState.cancelDragDistancePx >= cancelThresholdPx -> {
            onRecordGestureFinish(true)
        }

        else -> {
            onLockedStopClick()
        }
    }
}

private fun resetRecordGestureDragUi(
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
) {
    onGestureActiveChange(false)
    onRecordGestureMove(ConversationSendActionButtonGestureState())
}

private suspend fun AwaitPointerEventScope.awaitRecordGestureRelease(
    initialDown: PointerInputChange,
    onGestureChange: (ConversationSendActionButtonGestureState) -> Unit,
): ConversationSendActionButtonGestureState? {
    var releaseGestureState: ConversationSendActionButtonGestureState? = null
    var isTrackingGesture = true

    while (isTrackingGesture) {
        val pointerChange = awaitRecordGestureChange(pointerId = initialDown.id)

        if (pointerChange == null) {
            isTrackingGesture = false
        } else {
            val gestureState = calculateRecordGestureState(
                initialDown = initialDown,
                pointerChange = pointerChange,
            )

            onGestureChange(gestureState)
            pointerChange.consume()

            if (!pointerChange.pressed) {
                releaseGestureState = gestureState
                isTrackingGesture = false
            }
        }
    }

    return releaseGestureState
}

private suspend fun AwaitPointerEventScope.awaitRecordGestureChange(
    pointerId: PointerId,
): PointerInputChange? {
    return awaitPointerEvent()
        .changes
        .firstOrNull { change ->
            change.id == pointerId
        }
}

private fun calculateRecordGestureState(
    initialDown: PointerInputChange,
    pointerChange: PointerInputChange,
): ConversationSendActionButtonGestureState {
    val cancelDragDistancePx = (initialDown.position.x - pointerChange.position.x)
        .coerceAtLeast(minimumValue = 0f)

    val lockDragDistancePx = (initialDown.position.y - pointerChange.position.y)
        .coerceAtLeast(minimumValue = 0f)

    return ConversationSendActionButtonGestureState(
        cancelDragDistancePx = cancelDragDistancePx,
        lockDragDistancePx = lockDragDistancePx,
    )
}
