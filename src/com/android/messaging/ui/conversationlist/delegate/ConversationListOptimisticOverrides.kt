package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

internal data class ConversationListOptimisticOverrides(
    val archiveById: PersistentMap<String, ConversationArchiveOverride> = persistentMapOf(),
    val readById: PersistentMap<String, Boolean> = persistentMapOf(),
    val pinnedById: PersistentMap<String, Boolean> = persistentMapOf(),
) {
    val isEmpty: Boolean
        get() = archiveById.isEmpty() &&
            readById.isEmpty() &&
            pinnedById.isEmpty()
}

internal sealed interface ConversationArchiveOverride {
    val item: ConversationListItem

    data class Archived(
        override val item: ConversationListItem,
    ) : ConversationArchiveOverride

    data class Restoring(
        override val item: ConversationListItem,
        val awaitingRemoval: Boolean,
    ) : ConversationArchiveOverride
}
