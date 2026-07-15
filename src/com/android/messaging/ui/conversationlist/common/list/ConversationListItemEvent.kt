package com.android.messaging.ui.conversationlist.common.list

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel

@Immutable
internal sealed interface ConversationListItemEvent {

    data class Clicked(
        val conversationId: String,
    ) : ConversationListItemEvent

    data class LongClicked(
        val conversationId: String,
    ) : ConversationListItemEvent

    data class AvatarMessageClicked(
        val conversationId: String,
    ) : ConversationListItemEvent

    data class AvatarCallClicked(
        val destination: String,
    ) : ConversationListItemEvent

    data class AvatarContactClicked(
        val item: ConversationListItemUiModel,
    ) : ConversationListItemEvent

    data class AvatarInfoClicked(
        val conversationId: String,
    ) : ConversationListItemEvent

    data class Swiped(
        val conversationId: String,
        val kind: ConversationSwipeKind,
    ) : ConversationListItemEvent
}

@Immutable
internal data class ConversationListSwipeSpec(
    val startToEnd: ConversationSwipeKind,
    val endToStart: ConversationSwipeKind,
)

internal fun unsupportedSwipeKind(kind: ConversationSwipeKind): Nothing {
    error("Unsupported swipe kind on this conversation list: $kind")
}
