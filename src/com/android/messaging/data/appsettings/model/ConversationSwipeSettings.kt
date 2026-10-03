package com.android.messaging.data.appsettings.model

internal data class ConversationSwipeSettings(
    val startToEnd: ConversationSwipeOption,
    val endToStart: ConversationSwipeOption,
) {
    companion object {
        val Default = ConversationSwipeSettings(
            startToEnd = ConversationSwipeOption.ToggleRead,
            endToStart = ConversationSwipeOption.Archive,
        )
    }
}
