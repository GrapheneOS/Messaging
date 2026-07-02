package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
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
        val restoredItems = overrides.archiveById.mapNotNull { (conversationId, override) ->
            val restoring = override as? ConversationArchiveOverride.Restoring
                ?: return@mapNotNull null

            restoring.item.takeIf { conversationId !in currentIds }
        }

        val overridden = (items + restoredItems)
            .asSequence()
            .filterNot { item ->
                overrides.archiveById[item.conversationId] is ConversationArchiveOverride.Archived
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
        val archiveById = overrides.archiveById.pruneArchiveOverrides(itemsById)
        val restoringIds = archiveById
            .filterValues { override ->
                override is ConversationArchiveOverride.Restoring
            }
            .keys

        val readById = overrides.readById
            .pruneStaleOverrides(
                itemsById = itemsById,
                restoringIds = restoringIds,
                isStillPending = { item, isRead ->
                    item.latestMessage.isRead != isRead
                },
            )

        val pinnedById = overrides.pinnedById
            .pruneStaleOverrides(
                itemsById = itemsById,
                restoringIds = restoringIds,
                isStillPending = { item, isPinned ->
                    item.isPinned != isPinned
                },
            )

        return ConversationListOptimisticOverrides(
            archiveById = archiveById,
            readById = readById,
            pinnedById = pinnedById,
        )
    }

    private fun PersistentMap<String, ConversationArchiveOverride>.pruneArchiveOverrides(
        itemsById: Map<String, ConversationListItem>,
    ): PersistentMap<String, ConversationArchiveOverride> {
        return mutate { archiveOverrides ->
            forEach { (conversationId, override) ->
                when (override) {
                    is ConversationArchiveOverride.Archived -> Unit

                    is ConversationArchiveOverride.Restoring -> {
                        when {
                            conversationId !in itemsById -> {
                                archiveOverrides[conversationId] = override.copy(
                                    awaitingRemoval = false,
                                )
                            }

                            !override.awaitingRemoval -> {
                                archiveOverrides.remove(conversationId)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <V> PersistentMap<String, V>.pruneStaleOverrides(
        itemsById: Map<String, ConversationListItem>,
        restoringIds: Set<String>,
        isStillPending: (item: ConversationListItem, override: V) -> Boolean,
    ): PersistentMap<String, V> {
        return mutate { retainedOverrides ->
            forEach { (conversationId, override) ->
                val item = itemsById[conversationId]
                val shouldRetain = when {
                    item != null -> isStillPending(item, override)
                    conversationId in restoringIds -> true
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
