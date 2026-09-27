package com.android.messaging.ui.conversation.messages.ui.message

import android.view.ViewConfiguration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Tap and long press of a message. The tap toggles the selection in selection mode, otherwise it
 * downloads or resends the message, or does nothing, in which case no tap is offered at all. Touch
 * is taken on the whole message when [isWholeMessageTouchable], otherwise it's left to
 * [conversationMessageBubbleClickable].
 */
@Composable
internal fun Modifier.conversationMessageClickable(
    message: ConversationMessageUiModel,
    isSelectionMode: Boolean,
    isWholeMessageTouchable: Boolean,
    interactionSource: MutableInteractionSource,
    onMessageClick: () -> Unit,
    onMessageDownloadClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onMessageResendClick: () -> Unit,
): Modifier {
    val hapticFeedback = LocalHapticFeedback.current

    val clickLabel = when {
        isSelectionMode -> null
        message.canDownloadMessage -> stringResource(id = R.string.action_download)
        message.canResendMessage -> stringResource(id = R.string.action_send)
        else -> null
    }

    val longClickLabel = when {
        isSelectionMode -> null
        else -> stringResource(id = R.string.conversation_message_select)
    }

    val onClick = when {
        isSelectionMode -> {
            {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                onMessageClick()
            }
        }

        else -> {
            message.conversationMessageTapOrNull(
                onMessageDownloadClick = onMessageDownloadClick,
                onMessageResendClick = onMessageResendClick,
            )
        }
    }

    return this
        .conversationMessageNonTouchClickable(
            interactionSource = interactionSource,
            onClickLabel = clickLabel,
            onClick = onClick,
            onLongClickLabel = longClickLabel,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onMessageLongClick()
            },
        )
        .then(
            when {
                isWholeMessageTouchable -> {
                    Modifier
                        .conversationMessageBubbleClickable(
                            interactionSource = interactionSource,
                            onClick = onClick,
                            onLongClick = onMessageLongClick,
                        )
                        .then(
                            when (onClick) {
                                null -> Modifier.consumeTapsBeforeContent()
                                else -> Modifier
                            },
                        )
                }

                else -> Modifier
            },
        )
}

/**
 * The tap of a message outside selection mode: downloads or resends it, or null when it does
 * nothing.
 */
internal fun ConversationMessageUiModel.conversationMessageTapOrNull(
    onMessageDownloadClick: () -> Unit,
    onMessageResendClick: () -> Unit,
): (() -> Unit)? {
    return when {
        canDownloadMessage -> onMessageDownloadClick
        canResendMessage -> onMessageResendClick
        else -> null
    }
}

/**
 * [combinedClickable] without touch, and without the tap when [onClick] is null: a screen reader
 * always offers the tap of [combinedClickable] as "double-tap to activate", and switching to it on
 * gaining the tap, as on entering selection mode, would take keyboard focus away from the message.
 * Holding an enter key long presses it as with [combinedClickable].
 */
@Composable
internal fun Modifier.conversationMessageNonTouchClickable(
    interactionSource: MutableInteractionSource,
    onClickLabel: String?,
    onClick: (() -> Unit)?,
    onLongClickLabel: String?,
    onLongClick: () -> Unit,
): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val inputModeManager = LocalInputModeManager.current
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    val coroutineScope = rememberCoroutineScope()
    var keyLongPressJob by remember { mutableStateOf<Job?>(null) }

    return this
        .semantics(mergeDescendants = true) {
            if (onClick != null) {
                onClick(label = onClickLabel) {
                    currentOnClick?.invoke()
                    true
                }
            }
            onLongClick(label = onLongClickLabel) {
                currentOnLongClick()
                true
            }
        }
        .onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                keyLongPressJob?.cancel()
                keyLongPressJob = null
            }
        }
        .onKeyEvent { event ->
            when {
                event.key !in CONVERSATION_MESSAGE_ENTER_KEYS -> false

                event.type == KeyEventType.KeyDown -> {
                    if (keyLongPressJob == null) {
                        keyLongPressJob = coroutineScope.launch {
                            delay(timeMillis = longPressTimeoutMillis)
                            currentOnLongClick()
                        }
                    }
                    true
                }

                event.type == KeyEventType.KeyUp -> {
                    val pressJob = keyLongPressJob
                    keyLongPressJob = null
                    if (pressJob?.isActive == true) {
                        pressJob.cancel()
                        currentOnClick?.invoke()
                    }
                    pressJob != null
                }

                else -> false
            }
        }
        .focusProperties { canFocus = inputModeManager.inputMode != InputMode.Touch }
        .focusable(interactionSource = interactionSource)
}

/**
 * Touch of the bubble outside selection mode, so that a press leaving the bubble is canceled, or of
 * the whole message in selection mode and for a screen reader, which presses the middle of the
 * message.
 */
@Composable
internal fun Modifier.conversationMessageBubbleClickable(
    interactionSource: MutableInteractionSource,
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
): Modifier {
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    return pointerInput(interactionSource) {
        detectTapGestures(
            onPress = { position ->
                emitPress(position = position, interactionSource = interactionSource)
            },
            onTap = { currentOnClick?.invoke() },
            onLongPress = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnLongClick()
            },
        )
    }
}

/**
 * Keeps taps from the links and attachments of a message without a tap that's touched as a whole,
 * which only a screen reader does: its double-tap on the message taps the middle of it. Links and
 * attachments have double-taps of their own.
 */
private fun Modifier.consumeTapsBeforeContent(): Modifier {
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            withTimeoutOrNull(timeMillis = viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
            }?.consume()
        }
    }
}

/**
 * Emits the press as [combinedClickable] does in a scrolling list: at once for a tap, otherwise only
 * after the tap timeout, so that a scroll starting on a message doesn't flash the ripple.
 */
private suspend fun PressGestureScope.emitPress(
    position: Offset,
    interactionSource: MutableInteractionSource,
) {
    val releasedBeforeTimeout = withTimeoutOrNull(
        timeMillis = ViewConfiguration.getTapTimeout().toLong(),
    ) {
        tryAwaitRelease()
    }

    if (releasedBeforeTimeout == false) {
        return
    }

    val press = PressInteraction.Press(pressPosition = position)
    interactionSource.tryEmit(interaction = press)

    var released = releasedBeforeTimeout == true
    try {
        if (releasedBeforeTimeout == null) {
            released = tryAwaitRelease()
        }
    } finally {
        interactionSource.tryEmit(
            interaction = when {
                released -> PressInteraction.Release(press = press)
                else -> PressInteraction.Cancel(press = press)
            },
        )
    }
}

private val CONVERSATION_MESSAGE_ENTER_KEYS = setOf(
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter,
    Key.Spacebar,
)
