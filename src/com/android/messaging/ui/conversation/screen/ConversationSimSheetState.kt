package com.android.messaging.ui.conversation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
internal class ConversationSimSheetState {
    var isVisible: Boolean by mutableStateOf(value = false)
        private set

    fun show() {
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
    }

    companion object {
        val Saver: Saver<ConversationSimSheetState, Boolean> = Saver(
            save = { state -> state.isVisible },
            restore = { restoredIsVisible ->
                ConversationSimSheetState().apply {
                    isVisible = restoredIsVisible
                }
            },
        )
    }
}

@Composable
internal fun rememberConversationSimSheetState(
    isAvailable: Boolean,
): ConversationSimSheetState {
    val state = rememberSaveable(saver = ConversationSimSheetState.Saver) {
        ConversationSimSheetState()
    }

    LaunchedEffect(isAvailable) {
        if (!isAvailable) {
            state.dismiss()
        }
    }

    return state
}
