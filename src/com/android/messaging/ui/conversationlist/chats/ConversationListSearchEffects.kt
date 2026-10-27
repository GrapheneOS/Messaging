package com.android.messaging.ui.conversationlist.chats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action

@Composable
internal fun ConversationListSearchEffects(
    searchState: TextFieldState,
    isBackHandlerEnabled: Boolean,
    onAction: (Action) -> Unit,
) {
    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { query ->
                currentOnAction(Action.SearchQueryChanged(query))
            }
    }

    BackHandler(enabled = isBackHandlerEnabled) {
        closeConversationListSearch(
            searchState = searchState,
            onAction = onAction,
        )
    }
}

internal fun closeConversationListSearch(
    searchState: TextFieldState,
    onAction: (Action) -> Unit,
) {
    searchState.clearText()
    onAction(Action.SearchClosed)
}
