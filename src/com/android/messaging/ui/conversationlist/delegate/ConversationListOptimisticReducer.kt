package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.ui.conversationlist.delegate.ConversationRemovalOverride.Kind
import dagger.Reusable
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toImmutableList

@Reusable
internal class ConversationListOptimisticReducer @Inject constructor() {

    fun apply(
        items: ImmutableList<ConversationListItem>,
        overrides: ConversationListOptimisticOverrides,
    ): ImmutableList<ConversationListItem> {
        if (overrides.isEmpty) {
            return items
        }

        val currentIds = items.mapTo(mutableSetOf()) { it.conversationId }
        val restoredItems = overrides.removalById.mapNotNull { (conversationId, override) ->
            override.item.takeIf {
                override.kind == Kind.Restored && conversationId !in currentIds
            }
        }

        val overridden = (items + restoredItems)
            .asSequence()
            .filterNot { item ->
                overrides.removalById[item.conversationId]?.kind == Kind.Removed
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
        val removalById = overrides.removalById.pruneRemovalOverrides(itemsById)
        val restoredIds = removalById
            .filterValues { override ->
                override.kind == Kind.Restored
            }
            .keys

        val readById = overrides.readById
            .pruneStaleOverrides(
                itemsById = itemsById,
                restoredIds = restoredIds,
                isStillPending = { item, isRead ->
                    item.latestMessage.isRead != isRead
                },
            )

        val pinnedById = overrides.pinnedById
            .pruneStaleOverrides(
                itemsById = itemsById,
                restoredIds = restoredIds,
                isStillPending = { item, isPinned ->
                    item.isPinned != isPinned
                },
            )

        return ConversationListOptimisticOverrides(
            removalById = removalById,
            readById = readById,
            pinnedById = pinnedById,
        )
    }

    private fun PersistentMap<ConversationId, ConversationRemovalOverride>.pruneRemovalOverrides(
        itemsById: Map<ConversationId, ConversationListItem>,
    ): PersistentMap<ConversationId, ConversationRemovalOverride> {
        return mutate { removalOverrides ->
            forEach { (conversationId, override) ->
                when {
                    conversationId !in itemsById -> {
                        removalOverrides[conversationId] = override.copy(
                            awaitingRemoval = false,
                        )
                    }

                    !override.awaitingRemoval -> {
                        removalOverrides.remove(conversationId)
                    }
                }
            }
        }
    }

    private fun <V> PersistentMap<ConversationId, V>.pruneStaleOverrides(
        itemsById: Map<ConversationId, ConversationListItem>,
        restoredIds: Set<ConversationId>,
        isStillPending: (item: ConversationListItem, override: V) -> Boolean,
    ): PersistentMap<ConversationId, V> {
        return mutate { retainedOverrides ->
            forEach { (conversationId, override) ->
                val item = itemsById[conversationId]
                val shouldRetain = when {
                    item != null -> isStillPending(item, override)
                    conversationId in restoredIds -> true
                    else -> false
                }

                if (!shouldRetain) {
                    retainedOverrides.remove(conversationId)
                }
            }
        }
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
