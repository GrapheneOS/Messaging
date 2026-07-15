package com.android.messaging.data.conversationlist.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

internal data class ConversationListSnapshot(
    val items: ImmutableList<ConversationListItem>,
    val blockedDestinations: ImmutableSet<String>,
    val hasFirstSyncCompleted: Boolean,
    val restoredConversationIds: ImmutableSet<String> = persistentSetOf(),
)
