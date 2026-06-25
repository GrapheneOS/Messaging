package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface ConversationListOptimisticSnapshotDelegate {
    val snapshot: StateFlow<ConversationListSnapshot?>

    fun bind(scope: CoroutineScope)

    fun archive(conversationIds: List<String>)
    fun discardArchived(conversationIds: List<String>)
    fun restoreArchived(conversationIds: List<String>)
    fun markRead(conversationIds: List<String>, isRead: Boolean)
    fun pin(conversationIds: List<String>, isPinned: Boolean)
}

internal class ConversationListOptimisticSnapshotDelegateImpl @Inject constructor(
    private val repository: ConversationListRepository,
    private val reducer: ConversationListOptimisticReducer,
) : ConversationListOptimisticSnapshotDelegate {

    private val _snapshot = MutableStateFlow<ConversationListSnapshot?>(null)
    override val snapshot: StateFlow<ConversationListSnapshot?> = _snapshot.asStateFlow()

    private var rawSnapshot: ConversationListSnapshot? = null
    private var overrides = ConversationListOptimisticOverrides()
    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch {
            repository.observeInboxSnapshot()
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

    override fun archive(conversationIds: List<String>) {
        val requestedIds = conversationIds.toSet()
        val archivedItems = _snapshot.value
            ?.items
            .orEmpty()
            .filter { item ->
                item.conversationId in requestedIds
            }
            .associate { item ->
                item.conversationId to ConversationArchiveOverride.Archived(item)
            }

        if (archivedItems.isEmpty()) {
            return
        }

        overrides = overrides.copy(
            archiveById = overrides.archiveById.putAll(archivedItems),
        )
        publishSnapshot()
    }

    override fun discardArchived(conversationIds: List<String>) {
        var archiveById = overrides.archiveById

        conversationIds.forEach { conversationId ->
            if (archiveById[conversationId] is ConversationArchiveOverride.Archived) {
                archiveById = archiveById.remove(conversationId)
            }
        }

        overrides = overrides.copy(archiveById = archiveById)
        publishSnapshot()
    }

    override fun restoreArchived(conversationIds: List<String>) {
        var archiveById = overrides.archiveById
        val rawItemsById = rawSnapshot
            ?.items
            .orEmpty()
            .associateBy(ConversationListItem::conversationId)

        conversationIds.forEach { conversationId ->
            val item = archiveById[conversationId]?.item
                ?: rawItemsById[conversationId]
                ?: return@forEach

            archiveById = archiveById.put(
                key = conversationId,
                value = ConversationArchiveOverride.Restoring(
                    item = item,
                    awaitingRemoval = conversationId in rawItemsById,
                ),
            )
        }

        overrides = overrides.copy(archiveById = archiveById)
        publishSnapshot()
    }

    override fun markRead(
        conversationIds: List<String>,
        isRead: Boolean,
    ) {
        val effectiveIds = _snapshot.value
            ?.items
            .orEmpty()
            .mapTo(mutableSetOf()) { item -> item.conversationId }
        val readOverrides = conversationIds
            .filter(effectiveIds::contains)
            .associateWith { isRead }

        if (readOverrides.isEmpty()) {
            return
        }

        overrides = overrides.copy(
            readById = overrides.readById.putAll(readOverrides),
        )
        publishSnapshot()
    }

    override fun pin(
        conversationIds: List<String>,
        isPinned: Boolean,
    ) {
        val effectiveIds = _snapshot.value
            ?.items
            .orEmpty()
            .mapTo(mutableSetOf()) { item -> item.conversationId }
        val pinOverrides = conversationIds
            .filter(effectiveIds::contains)
            .associateWith { isPinned }

        if (pinOverrides.isEmpty()) {
            return
        }

        overrides = overrides.copy(
            pinnedById = overrides.pinnedById.putAll(pinOverrides),
        )
        publishSnapshot()
    }

    private fun publishSnapshot() {
        val snapshot = rawSnapshot ?: return

        val restoredConversationIds = overrides.archiveById
            .filterValues { override -> override is ConversationArchiveOverride.Restoring }
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
