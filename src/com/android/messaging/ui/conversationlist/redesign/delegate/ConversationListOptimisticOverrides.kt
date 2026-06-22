package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

internal data class ConversationListOptimisticOverrides(
    val archivedIds: PersistentSet<String> = persistentSetOf(),
    val archivedItemsById: PersistentMap<String, ConversationListItem> = persistentMapOf(),
    val restoringById: PersistentMap<String, RestoringConversation> = persistentMapOf(),
    val readById: PersistentMap<String, Boolean> = persistentMapOf(),
    val pinnedById: PersistentMap<String, Boolean> = persistentMapOf(),
) {
    val isEmpty: Boolean
        get() = archivedIds.isEmpty() &&
            restoringById.isEmpty() &&
            readById.isEmpty() &&
            pinnedById.isEmpty()
}

internal data class RestoringConversation(
    val item: ConversationListItem,
    val hasObservedArchivedSnapshot: Boolean,
)
