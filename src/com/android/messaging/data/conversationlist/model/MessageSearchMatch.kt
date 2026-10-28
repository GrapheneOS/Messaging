package com.android.messaging.data.conversationlist.model

import kotlinx.collections.immutable.ImmutableList

internal data class MessageSearchMatch(
    val item: ConversationListItem,
    val message: SearchableMessage,
    val textMatchRanges: ImmutableList<IntRange>,
)
