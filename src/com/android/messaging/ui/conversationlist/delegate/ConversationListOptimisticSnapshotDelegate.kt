package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMode
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.ui.conversationlist.delegate.ConversationRemovalOverride.Kind
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface ConversationListOptimisticSnapshotDelegate {
    val snapshot: StateFlow<ConversationListSnapshot?>
    val hasRawItems: Boolean

    fun bind(scope: CoroutineScope, mode: ConversationListMode)

    fun remove(conversationIds: List<ConversationId>)
    fun discardRemoval(conversationIds: List<ConversationId>)
    fun restore(conversationIds: List<ConversationId>)
    fun markRead(conversationIds: List<ConversationId>, isRead: Boolean)
    fun pin(conversationIds: List<ConversationId>, isPinned: Boolean)
}

internal class ConversationListOptimisticSnapshotDelegateImpl @Inject constructor(
    private val repository: ConversationListRepository,
    private val reducer: ConversationListOptimisticReducer,
) : ConversationListOptimisticSnapshotDelegate {

    private val _snapshot = MutableStateFlow<ConversationListSnapshot?>(null)
    override val snapshot: StateFlow<ConversationListSnapshot?> = _snapshot.asStateFlow()

    override val hasRawItems: Boolean
        get() = rawSnapshot?.items?.isNotEmpty() == true

    private var rawSnapshot: ConversationListSnapshot? = null
    private var overrides = ConversationListOptimisticOverrides()
    private var isBound = false

    override fun bind(
        scope: CoroutineScope,
        mode: ConversationListMode,
    ) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch {
            repository.observeSnapshot(mode)
                .collect { snapshot ->
                    rawSnapshot = snapshot
                    overrides = reducer.prune(
                        items = snapshot.items,
                        overrides = overrides,
                    )
                    publishSnapshot()
                }
        }
    }

    override fun remove(conversationIds: List<ConversationId>) {
        val rawItemsById = rawItemsById()
        val removedItems = effectiveItems(conversationIds).associate { item ->
            item.conversationId to ConversationRemovalOverride(
                item = item,
                kind = Kind.Removed,
                awaitingRemoval = item.conversationId in rawItemsById,
            )
        }

        if (removedItems.isEmpty()) {
            return
        }

        overrides = overrides.copy(
            removalById = overrides.removalById.putAll(removedItems),
        )
        publishSnapshot()
    }

    override fun discardRemoval(conversationIds: List<ConversationId>) {
        var removalById = overrides.removalById

        conversationIds.forEach { conversationId ->
            if (removalById[conversationId]?.kind == Kind.Removed) {
                removalById = removalById.remove(conversationId)
            }
        }

        overrides = overrides.copy(removalById = removalById)
        publishSnapshot()
    }

    override fun restore(conversationIds: List<ConversationId>) {
        var removalById = overrides.removalById
        val rawItemsById = rawItemsById()

        conversationIds.forEach { conversationId ->
            val item = removalById[conversationId]?.item
                ?: rawItemsById[conversationId]
                ?: return@forEach

            removalById = removalById.put(
                key = conversationId,
                value = ConversationRemovalOverride(
                    item = item,
                    kind = Kind.Restored,
                    awaitingRemoval = conversationId in rawItemsById,
                ),
            )
        }

        overrides = overrides.copy(removalById = removalById)
        publishSnapshot()
    }

    override fun markRead(
        conversationIds: List<ConversationId>,
        isRead: Boolean,
    ) {
        val readOverrides = effectiveItems(conversationIds)
            .associate { item -> item.conversationId to isRead }

        if (readOverrides.isEmpty()) {
            return
        }

        overrides = overrides.copy(
            readById = overrides.readById.putAll(readOverrides),
        )
        publishSnapshot()
    }

    override fun pin(
        conversationIds: List<ConversationId>,
        isPinned: Boolean,
    ) {
        val pinOverrides = effectiveItems(conversationIds)
            .associate { item -> item.conversationId to isPinned }

        if (pinOverrides.isEmpty()) {
            return
        }

        overrides = overrides.copy(
            pinnedById = overrides.pinnedById.putAll(pinOverrides),
        )
        publishSnapshot()
    }

    private fun effectiveItems(
        conversationIds: List<ConversationId>,
    ): List<ConversationListItem> {
        val requestedIds = conversationIds.toSet()

        return _snapshot.value
            ?.items
            .orEmpty()
            .filter { item -> item.conversationId in requestedIds }
    }

    private fun rawItemsById(): Map<ConversationId, ConversationListItem> {
        return rawSnapshot
            ?.items
            .orEmpty()
            .associateBy(ConversationListItem::conversationId)
    }

    private fun publishSnapshot() {
        val snapshot = rawSnapshot ?: return

        val restoredConversationIds = overrides.removalById
            .filterValues { override -> override.kind == Kind.Restored }
            .keys
            .toImmutableSet()

        _snapshot.value = snapshot.copy(
            items = reducer.apply(
                items = snapshot.items,
                overrides = overrides,
            ),
            restoredConversationIds = restoredConversationIds,
        )
    }
}
