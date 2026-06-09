package com.android.messaging.data.conversationlist.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

internal data class ConversationListSnapshot(
    val items: ImmutableList<ConversationListItem>,
    val blockedDestinations: ImmutableSet<String>,
    val hasFirstSyncCompleted: Boolean,
)
