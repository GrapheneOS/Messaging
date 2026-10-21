package com.android.messaging.ui.conversationlist.chats.mapper

import com.android.messaging.data.appsettings.model.ConversationSwipeOption
import com.android.messaging.data.appsettings.model.ConversationSwipeSettings
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.list.ConversationListSwipeSpec

internal fun ConversationSwipeSettings.toSwipeSpec(): ConversationListSwipeSpec {
    return ConversationListSwipeSpec(
        startToEnd = startToEnd.toSwipeKind(),
        endToStart = endToStart.toSwipeKind(),
    )
}

private fun ConversationSwipeOption.toSwipeKind(): ConversationSwipeKind {
    return when (this) {
        ConversationSwipeOption.ToggleRead -> ConversationSwipeKind.ToggleRead
        ConversationSwipeOption.Archive -> ConversationSwipeKind.Archive
        ConversationSwipeOption.Delete -> ConversationSwipeKind.Delete
    }
}
