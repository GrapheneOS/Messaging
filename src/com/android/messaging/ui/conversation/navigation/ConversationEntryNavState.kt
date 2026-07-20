package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.compositionLocalOf
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel

internal data class ConversationEntryNavState(
    val model: ConversationEntryScreenModel,
    val isLaunchedFromBubble: Boolean,
)

internal val LocalConversationEntryNavState = compositionLocalOf<ConversationEntryNavState> {
    error("No ConversationEntryNavState was provided")
}
