package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.messaging.R

@Immutable
internal enum class ConversationSendActionButtonMode {
    Send,
    Record,
    Stop,
}

@Immutable
internal data class ConversationSendActionButtonGestureState(
    val cancelDragDistancePx: Float = 0f,
    val lockDragDistancePx: Float = 0f,
)

@Immutable
private data class ConversationSendActionButtonVisualState(
    val buttonScale: Float,
    val containerColor: Color,
    val contentColor: Color,
)

private val SEND_ACTION_BUTTON_PULSE_SCALE_ANIMATION_SPEC = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 1000,
        easing = FastOutSlowInEasing,
    ),
    repeatMode = RepeatMode.Reverse,
)

private val SEND_ACTION_BUTTON_BASE_SCALE_ANIMATION_SPEC = tween<Float>(durationMillis = 180)
private val SEND_ACTION_BUTTON_COLOR_ANIMATION_SPEC = tween<Color>(durationMillis = 220)
private val SEND_ACTION_BUTTON_ICON_ENTER_ANIMATION_SPEC = tween<Float>(durationMillis = 150)
private val SEND_ACTION_BUTTON_ICON_EXIT_ANIMATION_SPEC = tween<Float>(durationMillis = 120)

private val SEND_ACTION_BUTTON_BACKDROP_PULSE_ANIMATION_SPEC = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 2100,
        easing = FastOutSlowInEasing,
    ),
    repeatMode = RepeatMode.Restart,
)

private val SEND_ACTION_BUTTON_BACKDROP_DELAYED_PULSE_ANIMATION_SPEC = infiniteRepeatable<Float>(
    animation = tween(
        durationMillis = 2100,
        easing = FastOutSlowInEasing,
    ),
    repeatMode = RepeatMode.Restart,
    initialStartOffset = StartOffset(
        offsetMillis = 1050,
        offsetType = StartOffsetType.FastForward,
    ),
)

@Composable
internal fun ConversationSendActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    isRecordingActive: Boolean,
    isRecordingLocked: Boolean,
    onClick: () -> Unit,
    onLockedStopClick: () -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
    onRecordGestureLock: () -> Boolean,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    var isRecordGestureActive by remember(mode, enabled) {
        mutableStateOf(value = false)
    }

    val visualState = animateConversationSendActionButtonVisualState(
        isRecordingActive = isRecordingActive,
        isRecordGestureActive = isRecordGestureActive,
    )

    val cancelThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_CANCEL_THRESHOLD.toPx()
    }
    val lockThresholdPx = with(LocalDensity.current) {
        AUDIO_RECORD_LOCK_THRESHOLD.toPx()
    }

    val gestureModifier = Modifier.conversationSendActionButtonGesture(
        mode = mode,
        enabled = enabled,
        cancelThresholdPx = cancelThresholdPx,
        lockThresholdPx = lockThresholdPx,
        isRecordingActive = isRecordingActive,
        isRecordingLocked = isRecordingLocked,
        onGestureActiveChange = { isActive ->
            isRecordGestureActive = isActive
        },
        onRecordGestureStart = onRecordGestureStart,
        onRecordGestureMove = onRecordGestureMove,
        onRecordGestureLock = onRecordGestureLock,
        onRecordGestureFinish = onRecordGestureFinish,
        onLockedStopClick = onLockedStopClick,
    )

    ConversationSendActionButtonLayout(
        modifier = modifier,
        isRecordingActive = isRecordingActive,
        buttonModifier = gestureModifier,
        enabled = enabled,
        mode = mode,
        onClick = onClick,
        onLockedStopClick = onLockedStopClick,
        visualState = visualState,
    )
}

@Composable
private fun animateConversationSendActionButtonVisualState(
    isRecordingActive: Boolean,
    isRecordGestureActive: Boolean,
): ConversationSendActionButtonVisualState {
    val pulseAnimation = rememberInfiniteTransition(
        label = "conversation_send_action_pulse",
    )

    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = SEND_ACTION_BUTTON_PULSE_SCALE_ANIMATION_SPEC,
        label = "conversation_send_action_pulse_scale",
    )

    val baseButtonScale by animateFloatAsState(
        targetValue = when {
            isRecordingActive -> 1.1f
            isRecordGestureActive -> 0.95f
            else -> 1f
        },
        animationSpec = SEND_ACTION_BUTTON_BASE_SCALE_ANIMATION_SPEC,
        label = "conversation_send_action_base_scale",
    )

    val buttonScale = when {
        isRecordingActive -> baseButtonScale * pulseScale
        else -> baseButtonScale
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isRecordingActive -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = SEND_ACTION_BUTTON_COLOR_ANIMATION_SPEC,
        label = "conversation_send_action_container_color",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isRecordingActive -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = SEND_ACTION_BUTTON_COLOR_ANIMATION_SPEC,
        label = "conversation_send_action_content_color",
    )

    return ConversationSendActionButtonVisualState(
        buttonScale = buttonScale,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
private fun Modifier.conversationSendActionButtonGesture(
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

    return when {
        mode != ConversationSendActionButtonMode.Send && enabled -> {
            pointerInput(
                mode,
                enabled,
                cancelThresholdPx,
                lockThresholdPx,
            ) {
                awaitEachGesture {
                    when {
                        currentIsRecordingActive && currentIsRecordingLocked -> {
                            handleLockedRecordGesture(
                                cancelThresholdPx = cancelThresholdPx,
                                onGestureActiveChange = currentOnGestureActiveChange,
                                onRecordGestureMove = currentOnRecordGestureMove,
                                onRecordGestureFinish = currentOnRecordGestureFinish,
                                onLockedStopClick = currentOnLockedStopClick,
                            )
                        }

                        else -> {
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
        }

        else -> this
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

    while (true) {
        val pointerChange = awaitRecordGestureChange(pointerId = initialDown.id) ?: break
        val gestureState = calculateRecordGestureState(
            initialDown = initialDown,
            pointerChange = pointerChange,
        )

        if (!isRecordingLocked) {
            onRecordGestureMove(gestureState)

            if (gestureState.lockDragDistancePx >= lockThresholdPx) {
                isRecordingLocked = onRecordGestureLock()

                if (isRecordingLocked) {
                    onRecordGestureMove(ConversationSendActionButtonGestureState())
                }
            }
        }

        pointerChange.consume()

        if (pointerChange.pressed) {
            continue
        }

        resetRecordGestureDragUi(
            onGestureActiveChange = onGestureActiveChange,
            onRecordGestureMove = onRecordGestureMove,
        )

        if (!isRecordingLocked) {
            onRecordGestureFinish(gestureState.cancelDragDistancePx >= cancelThresholdPx)
        }

        return
    }

    resetRecordGestureDragUi(
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
    )
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

    while (true) {
        val pointerChange = awaitRecordGestureChange(pointerId = initialDown.id) ?: break
        val gestureState = calculateRecordGestureState(
            initialDown = initialDown,
            pointerChange = pointerChange,
        )

        onRecordGestureMove(
            ConversationSendActionButtonGestureState(
                cancelDragDistancePx = gestureState.cancelDragDistancePx,
            ),
        )
        pointerChange.consume()

        if (!pointerChange.pressed) {
            resetRecordGestureDragUi(
                onGestureActiveChange = onGestureActiveChange,
                onRecordGestureMove = onRecordGestureMove,
            )
            when {
                gestureState.cancelDragDistancePx >= cancelThresholdPx -> {
                    onRecordGestureFinish(true)
                }

                else -> {
                    onLockedStopClick()
                }
            }
            return
        }
    }

    resetRecordGestureDragUi(
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
    )
}

private fun resetRecordGestureDragUi(
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (ConversationSendActionButtonGestureState) -> Unit,
) {
    onGestureActiveChange(false)
    onRecordGestureMove(ConversationSendActionButtonGestureState())
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
    return ConversationSendActionButtonGestureState(
        cancelDragDistancePx = (initialDown.position.x - pointerChange.position.x)
            .coerceAtLeast(minimumValue = 0f),
        lockDragDistancePx = (initialDown.position.y - pointerChange.position.y)
            .coerceAtLeast(minimumValue = 0f),
    )
}

@Composable
private fun ConversationSendActionButtonLayout(
    modifier: Modifier,
    isRecordingActive: Boolean,
    buttonModifier: Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    onClick: () -> Unit,
    onLockedStopClick: () -> Unit,
    visualState: ConversationSendActionButtonVisualState,
) {
    Box(
        modifier = modifier.size(size = 56.dp),
    ) {
        ConversationSendActionButtonPulseBackdrop(
            isVisible = isRecordingActive,
        )

        ConversationSendActionButtonContent(
            modifier = buttonModifier,
            enabled = enabled,
            mode = mode,
            onClick = onClick,
            onLockedStopClick = onLockedStopClick,
            visualState = visualState,
        )
    }
}

@Composable
private fun ConversationSendActionButtonContent(
    modifier: Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    onClick: () -> Unit,
    onLockedStopClick: () -> Unit,
    visualState: ConversationSendActionButtonVisualState,
) {
    val stopContentDescription = stringResource(
        id = R.string.audio_record_stop_content_description,
    )
    val stopSemanticsModifier = when (mode) {
        ConversationSendActionButtonMode.Stop -> {
            Modifier.semantics {
                onClick(label = stopContentDescription) {
                    onLockedStopClick()
                    true
                }
            }
        }

        else -> Modifier
    }

    FilledIconButton(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale = visualState.buttonScale)
            .then(stopSemanticsModifier)
            .then(modifier),
        onClick = {
            if (mode == ConversationSendActionButtonMode.Send) {
                onClick()
            }
        },
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = visualState.containerColor,
            contentColor = visualState.contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        ConversationSendActionButtonIcon(
            mode = mode,
        )
    }
}

@Composable
private fun ConversationSendActionButtonIcon(mode: ConversationSendActionButtonMode) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            conversationSendActionButtonIconContentTransform()
        },
        label = "conversation_send_action_icon",
    ) { currentMode ->
        when (currentMode) {
            ConversationSendActionButtonMode.Send -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(
                        id = R.string.sendButtonContentDescription,
                    ),
                )
            }

            ConversationSendActionButtonMode.Record -> {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = stringResource(
                        id = R.string.audio_record_view_content_description,
                    ),
                )
            }

            ConversationSendActionButtonMode.Stop -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mp_capture_stop_large_light),
                    contentDescription = stringResource(
                        id = R.string.audio_record_stop_content_description,
                    ),
                )
            }
        }
    }
}

private fun conversationSendActionButtonIconContentTransform(): ContentTransform {
    val fadeInTransition = fadeIn(
        animationSpec = SEND_ACTION_BUTTON_ICON_ENTER_ANIMATION_SPEC,
    )
    val scaleInTransition = scaleIn(
        animationSpec = SEND_ACTION_BUTTON_ICON_ENTER_ANIMATION_SPEC,
        initialScale = 0.9f,
    )
    val enterTransition = fadeInTransition + scaleInTransition

    val fadeOutTransition = fadeOut(
        animationSpec = SEND_ACTION_BUTTON_ICON_EXIT_ANIMATION_SPEC,
    )
    val scaleOutTransition = scaleOut(
        animationSpec = SEND_ACTION_BUTTON_ICON_EXIT_ANIMATION_SPEC,
        targetScale = 1.1f,
    )
    val exitTransition = fadeOutTransition + scaleOutTransition

    return enterTransition.togetherWith(exitTransition)
}

@Composable
private fun ConversationSendActionButtonPulseBackdrop(
    isVisible: Boolean,
) {
    if (!isVisible) {
        return
    }

    val pulseTransition = rememberInfiniteTransition(
        label = "conversation_send_action_backdrop",
    )

    val outerPulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.9f,
        animationSpec = SEND_ACTION_BUTTON_BACKDROP_PULSE_ANIMATION_SPEC,
        label = "conversation_send_action_outer_pulse_scale",
    )

    val outerPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0f,
        animationSpec = SEND_ACTION_BUTTON_BACKDROP_PULSE_ANIMATION_SPEC,
        label = "conversation_send_action_outer_pulse_alpha",
    )

    val innerPulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = SEND_ACTION_BUTTON_BACKDROP_DELAYED_PULSE_ANIMATION_SPEC,
        label = "conversation_send_action_inner_pulse_scale",
    )

    val innerPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0f,
        animationSpec = SEND_ACTION_BUTTON_BACKDROP_DELAYED_PULSE_ANIMATION_SPEC,
        label = "conversation_send_action_inner_pulse_alpha",
    )

    ConversationSendActionPulseCircle(
        scale = outerPulseScale,
        alpha = outerPulseAlpha,
    )

    ConversationSendActionPulseCircle(
        scale = innerPulseScale,
        alpha = innerPulseAlpha,
    )
}

@Composable
private fun ConversationSendActionPulseCircle(
    scale: Float,
    alpha: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale = scale)
            .alpha(alpha = alpha)
            .background(
                color = MaterialTheme.colorScheme.error,
                shape = CircleShape,
            ),
    )
}
