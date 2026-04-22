package com.android.messaging.ui.conversation.v2.composer.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.android.messaging.R

@Immutable
internal enum class ConversationSendActionButtonMode {
    Send,
    Record,
}

@Immutable
private data class ConversationSendActionButtonVisualState(
    val buttonScale: Float,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
internal fun ConversationSendActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    isRecordingActive: Boolean,
    onClick: () -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (Float) -> Unit,
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
        96.dp.toPx()
    }

    val gestureModifier = Modifier.conversationSendActionButtonGesture(
        mode = mode,
        enabled = enabled,
        cancelThresholdPx = cancelThresholdPx,
        onGestureActiveChange = { isActive ->
            isRecordGestureActive = isActive
        },
        onRecordGestureStart = onRecordGestureStart,
        onRecordGestureMove = onRecordGestureMove,
        onRecordGestureFinish = onRecordGestureFinish,
    )

    ConversationSendActionButtonLayout(
        modifier = modifier,
        isRecordingActive = isRecordingActive,
        buttonModifier = gestureModifier,
        enabled = enabled,
        mode = mode,
        onClick = onClick,
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
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "conversation_send_action_pulse_scale",
    )

    val baseButtonScale by animateFloatAsState(
        targetValue = when {
            isRecordingActive -> 1.1f
            isRecordGestureActive -> 0.95f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 180),
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
        animationSpec = tween(durationMillis = 220),
        label = "conversation_send_action_container_color",
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isRecordingActive -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 220),
        label = "conversation_send_action_content_color",
    )

    return ConversationSendActionButtonVisualState(
        buttonScale = buttonScale,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

private fun Modifier.conversationSendActionButtonGesture(
    mode: ConversationSendActionButtonMode,
    enabled: Boolean,
    cancelThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (Float) -> Unit,
    onRecordGestureFinish: (Boolean) -> Unit,
): Modifier {
    return when {
        mode == ConversationSendActionButtonMode.Record && enabled -> {
            then(
                Modifier
                    .pointerInput(mode, true, cancelThresholdPx) {
                        awaitEachGesture {
                            handleRecordGesture(
                                cancelThresholdPx = cancelThresholdPx,
                                onGestureActiveChange = onGestureActiveChange,
                                onRecordGestureStart = onRecordGestureStart,
                                onRecordGestureMove = onRecordGestureMove,
                                onRecordGestureFinish = onRecordGestureFinish,
                            )
                        }
                    },
            )
        }

        else -> this
    }
}

private suspend fun AwaitPointerEventScope.handleRecordGesture(
    cancelThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureStart: () -> Unit,
    onRecordGestureMove: (Float) -> Unit,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    val down = awaitFirstDown(requireUnconsumed = false)

    val longPressChange = awaitLongPressOrCancellation(pointerId = down.id)
        ?: return

    onGestureActiveChange(true)
    onRecordGestureStart()

    trackRecordGestureDrag(
        down = down,
        longPressChange = longPressChange,
        cancelThresholdPx = cancelThresholdPx,
        onGestureActiveChange = onGestureActiveChange,
        onRecordGestureMove = onRecordGestureMove,
        onRecordGestureFinish = onRecordGestureFinish,
    )
}

private suspend fun AwaitPointerEventScope.trackRecordGestureDrag(
    down: PointerInputChange,
    longPressChange: PointerInputChange,
    cancelThresholdPx: Float,
    onGestureActiveChange: (Boolean) -> Unit,
    onRecordGestureMove: (Float) -> Unit,
    onRecordGestureFinish: (Boolean) -> Unit,
) {
    var shouldCancel: Boolean

    longPressChange.consume()

    while (true) {
        val pointerChange = awaitRecordGestureChange(pointerId = down.id) ?: break
        val dragDistance = calculateRecordGestureDragDistance(
            down = down,
            pointerChange = pointerChange,
        )

        onRecordGestureMove(dragDistance)
        shouldCancel = dragDistance >= cancelThresholdPx
        pointerChange.consume()

        if (!pointerChange.pressed) {
            onGestureActiveChange(false)
            onRecordGestureFinish(shouldCancel)
            return
        }
    }
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

private fun calculateRecordGestureDragDistance(
    down: PointerInputChange,
    pointerChange: PointerInputChange,
): Float {
    return (down.position.x - pointerChange.position.x)
        .coerceAtLeast(minimumValue = 0f)
}

@Composable
private fun ConversationSendActionButtonLayout(
    modifier: Modifier,
    isRecordingActive: Boolean,
    buttonModifier: Modifier,
    enabled: Boolean,
    mode: ConversationSendActionButtonMode,
    onClick: () -> Unit,
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
    visualState: ConversationSendActionButtonVisualState,
) {
    FilledIconButton(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale = visualState.buttonScale)
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
            (
                fadeIn(animationSpec = tween(durationMillis = 150)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 150),
                        initialScale = 0.88f,
                    )
                ).togetherWith(
                fadeOut(animationSpec = tween(durationMillis = 120)) +
                    scaleOut(
                        animationSpec = tween(durationMillis = 120),
                        targetScale = 1.08f,
                    ),
            )
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
                    painter = painterResource(id = R.drawable.ic_mp_audio_mic),
                    contentDescription = stringResource(
                        id = R.string.audio_record_view_content_description,
                    ),
                )
            }
        }
    }
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
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2100,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "conversation_send_action_outer_pulse_scale",
    )

    val outerPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2100,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "conversation_send_action_outer_pulse_alpha",
    )

    val innerPulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2100,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(
                offsetMillis = 1050,
                offsetType = StartOffsetType.FastForward,
            ),
        ),
        label = "conversation_send_action_inner_pulse_scale",
    )

    val innerPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2100,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(
                offsetMillis = 1050,
                offsetType = StartOffsetType.FastForward,
            ),
        ),
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
