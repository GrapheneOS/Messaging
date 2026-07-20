package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.State
import androidx.navigation3.runtime.NavKey
import com.android.messaging.ui.conversation.entry.ConversationEntryScreenModel
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState

internal class ConversationNavRouteState(
    val backStack: MutableList<NavKey>,
    val entryModel: State<ConversationEntryScreenModel>,
    val entryUiState: State<ConversationEntryUiState>,
    val isLaunchedFromBubble: State<Boolean>,
    val navigationReducer: State<ConversationNavigationReducer>,
    val onFinish: State<() -> Unit>,
)
