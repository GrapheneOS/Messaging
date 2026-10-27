package com.android.messaging.data.conversationlist.model

import kotlinx.collections.immutable.ImmutableList

internal data class ConversationSearchMatch(
    val item: ConversationListItem,
    val titleMatchRanges: ImmutableList<IntRange>,
)
