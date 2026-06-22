package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import dagger.Reusable
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet

@Reusable
internal class ConversationListOptimisticReducer @Inject constructor() {

    fun apply(
        items: ImmutableList<ConversationListItem>,
        overrides: ConversationListOptimisticOverrides,
    ): ImmutableList<ConversationListItem> {
        if (overrides.isEmpty) {
            return items
        }

        val restoredItems = overrides.restoringById
            .filterKeys { conversationId ->
                items.none { it.conversationId == conversationId }
            }
            .values
            .map(RestoringConversation::item)

        val overridden = (items + restoredItems)
            .asSequence()
            .filterNot { item ->
                item.conversationId in overrides.archivedIds
            }
            .map { item ->
                item.withOverrides(overrides)
            }
            .toList()

        val ordered = when {
            overrides.pinnedById.isEmpty() && restoredItems.isEmpty() -> overridden
            else -> overridden.sortedWith(sortComparator)
        }

        return ordered.toImmutableList()
    }

    fun prune(
        items: ImmutableList<ConversationListItem>,
        overrides: ConversationListOptimisticOverrides,
    ): ConversationListOptimisticOverrides {
        if (overrides.isEmpty) {
            return overrides
        }

        val itemsById = items.associateBy(ConversationListItem::conversationId)
        val restoringById = overrides.restoringById.pruneRestoring(itemsById)

        val readById = overrides.readById
            .pruneStaleOverrides(
                itemsById = itemsById,
                restoringById = restoringById,
            ) { item, isRead ->
                item.latestMessage.isRead != isRead
            }

        val archivedIds = overrides.archivedIds
            .filter { conversationId ->
                conversationId in itemsById
            }
            .toPersistentSet()

        val archivedItemsById = overrides.archivedItemsById
            .filterKeys { conversationId ->
                conversationId in archivedIds || conversationId in restoringById
            }
            .toPersistentMap()

        val pinnedById = overrides.pinnedById
            .pruneStaleOverrides(
                itemsById = itemsById,
                restoringById = restoringById,
            ) { item, isPinned ->
                item.isPinned != isPinned
            }

        return ConversationListOptimisticOverrides(
            archivedIds = archivedIds,
            archivedItemsById = archivedItemsById,
            restoringById = restoringById,
            readById = readById,
            pinnedById = pinnedById,
        )
    }

    private fun PersistentMap<String, RestoringConversation>.pruneRestoring(
        itemsById: Map<String, ConversationListItem>,
    ): PersistentMap<String, RestoringConversation> {
        return mapNotNull { (conversationId, restoring) ->
            when {
                conversationId !in itemsById -> {
                    conversationId to restoring.copy(
                        hasObservedArchivedSnapshot = true,
                    )
                }

                restoring.hasObservedArchivedSnapshot -> null

                else -> conversationId to restoring
            }
        }.toMap().toPersistentMap()
    }

    private fun <V> PersistentMap<String, V>.pruneStaleOverrides(
        itemsById: Map<String, ConversationListItem>,
        restoringById: PersistentMap<String, RestoringConversation>,
        isStillPending: (item: ConversationListItem, override: V) -> Boolean,
    ): PersistentMap<String, V> {
        return filter { (conversationId, override) ->
            val item = itemsById[conversationId]

            when {
                item != null -> isStillPending(item, override)
                conversationId in restoringById -> true
                else -> false
            }
        }.toPersistentMap()
    }

    private fun ConversationListItem.withOverrides(
        overrides: ConversationListOptimisticOverrides,
    ): ConversationListItem {
        val isRead = overrides.readById[conversationId]
        val isPinned = overrides.pinnedById[conversationId]

        if (isRead == null && isPinned == null) {
            return this
        }

        return copy(
            isPinned = isPinned ?: this.isPinned,
            latestMessage = isRead?.let { latestMessage.copy(isRead = it) } ?: latestMessage,
        )
    }

    private val sortComparator = compareByDescending<ConversationListItem> { it.isPinned }
        .thenByDescending { it.latestMessage.timestamp }
}
