package com.android.messaging.ui.conversation.messages.ui.text

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId

internal val LocalConversationTextSelection = compositionLocalOf<ConversationTextSelectionState?> {
    null
}

internal val LocalConversationTextSelectionCoordinator =
    compositionLocalOf<ConversationTextSelectionCoordinator?> { null }

@Stable
internal class ConversationTextSelectionCoordinator(
    private val onCopyText: (String) -> Unit,
) {
    private val states = mutableSetOf<ConversationTextSelectionState>()

    var active: ConversationTextSelectionState? by mutableStateOf(null)
        private set

    val hasSelection: Boolean
        get() = states.any { it.hasSelection }

    fun copyText(text: String) {
        onCopyText(text)
    }

    fun register(state: ConversationTextSelectionState) {
        states += state
    }

    fun update(state: ConversationTextSelectionState) {
        when {
            state.hasSelection -> {
                if (active !== state) active?.selection?.clear()
                active = state
            }
            active === state -> active = null
        }
    }

    fun release(state: ConversationTextSelectionState) {
        states -= state
        if (active === state) active = null
    }

    // A tap can start a selection on its down, before update() makes that message active.
    fun dismiss(): Boolean {
        val selected = states.filter { it.hasSelection }
        selected.forEach { it.selection.clear() }
        active = null
        return selected.isNotEmpty()
    }
}

@Composable
internal fun ConversationTextSelectionHost(
    conversationId: ConversationId?,
    onCopyText: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnCopyText by rememberUpdatedState(newValue = onCopyText)
    val coordinator = remember(conversationId) {
        ConversationTextSelectionCoordinator(onCopyText = { text -> currentOnCopyText(text) })
    }

    BackHandler(enabled = coordinator.active?.hasSelection == true) {
        coordinator.dismiss()
    }

    CompositionLocalProvider(LocalConversationTextSelectionCoordinator provides coordinator) {
        Box(modifier = Modifier.dismissConversationTextSelectionOnTap(coordinator = coordinator)) {
            content()
        }
    }
}

@Composable
internal fun ConversationMessageSelectionScope(
    conversationId: ConversationId,
    messageId: MessageId,
    subject: String?,
    body: String?,
    isSelectionMode: Boolean,
    isTouchExplorationEnabled: Boolean,
    onSelectMessage: () -> Unit,
    content: @Composable () -> Unit,
) {
    key(conversationId.value, messageId.value, subject, body) {
        val state = rememberSaveable(saver = ConversationTextSelectionState.Saver) {
            ConversationTextSelectionState()
        }
        val coordinator = LocalConversationTextSelectionCoordinator.current
        state.enabled = !isSelectionMode
        state.isTouchExplorationEnabled = isTouchExplorationEnabled
        state.bodyTextOffset = subject?.takeUnless { it.isBlank() }?.length ?: 0
        val currentOnSelectMessage by rememberUpdatedState(newValue = onSelectMessage)

        state.selectMessage = {
            state.selection.clear()
            currentOnSelectMessage()
        }

        LaunchedEffect(state, isSelectionMode) {
            if (isSelectionMode) state.selection.clear()
            snapshotFlow { state.hasSelection }.collect {
                state.applyPendingLinks()
                coordinator?.update(state = state)
            }
        }
        DisposableEffect(state, coordinator) {
            coordinator?.register(state = state)
            onDispose { coordinator?.release(state = state) }
        }
        CompositionLocalProvider(LocalConversationTextSelection provides state, content = content)
    }
}

private fun Modifier.dismissConversationTextSelectionOnTap(
    coordinator: ConversationTextSelectionCoordinator,
): Modifier {
    return pointerInput(coordinator) {
        var dismissingDown: PointerInputChange? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            when {
                isRepeatedTap(previousDown = dismissingDown, down = down) -> {
                    // SelectionContainer selects a word on the second down of a double tap.
                    down.consume()
                    dismissingDown = down
                }
                coordinator.hasSelection -> {
                    val up = withTimeoutOrNull(
                        timeMillis = viewConfiguration.longPressTimeoutMillis,
                    ) {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    }

                    if (up != null) {
                        up.consume()
                        coordinator.dismiss()
                        dismissingDown = down
                    }
                }
            }
        }
    }
}

private fun PointerInputScope.isRepeatedTap(
    previousDown: PointerInputChange?,
    down: PointerInputChange,
): Boolean {
    return previousDown != null &&
        down.uptimeMillis - previousDown.uptimeMillis < viewConfiguration.doubleTapTimeoutMillis &&
        (down.position - previousDown.position).getDistance() < viewConfiguration.touchSlop
}
