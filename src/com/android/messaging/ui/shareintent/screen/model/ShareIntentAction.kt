package com.android.messaging.ui.shareintent.screen.model

internal sealed interface ShareIntentAction {

    data class TargetClicked(
        val conversationId: String,
    ) : ShareIntentAction

    data class TargetLongPressed(
        val conversationId: String,
    ) : ShareIntentAction

    data class SelectionToggled(
        val conversationId: String,
    ) : ShareIntentAction

    data object SelectionCleared : ShareIntentAction

    data object SendToSelectedClicked : ShareIntentAction

    data object NewMessageClicked : ShareIntentAction

    data object SearchOpened : ShareIntentAction

    data object SearchClosed : ShareIntentAction

    data class SearchQueryChanged(
        val query: String,
    ) : ShareIntentAction
}
