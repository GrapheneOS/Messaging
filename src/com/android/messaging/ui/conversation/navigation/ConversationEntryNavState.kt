package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.conversation.entry.ConversationEntryViewModel

internal data class ConversationEntryNavState(
    val model: ConversationEntryScreenModel,
    val isLaunchedFromBubble: Boolean,
)

@Composable
internal fun ProvideConversationEntryNavState(
    isLaunchedFromBubble: Boolean,
    content: @Composable () -> Unit,
) {
    val entryModel: ConversationEntryScreenModel = hiltViewModel<ConversationEntryViewModel>()
    val entryNavState = ConversationEntryNavState(
        model = entryModel,
        isLaunchedFromBubble = isLaunchedFromBubble,
    )

    CompositionLocalProvider(
        LocalConversationEntryNavState provides entryNavState,
    ) {
        content()
    }
}

internal val LocalConversationEntryNavState = compositionLocalOf<ConversationEntryNavState> {
    error("No ConversationEntryNavState was provided")
}
