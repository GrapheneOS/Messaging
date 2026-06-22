package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import javax.inject.Inject
import kotlinx.collections.immutable.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ConversationListOptimisticSnapshotDelegate {
    val snapshot: StateFlow<ConversationListSnapshot?>

    fun bind(scope: CoroutineScope)

    fun archive(conversationIds: List<String>)
    fun restoreArchived(conversationIds: List<String>)
    fun markRead(conversationIds: List<String>, isRead: Boolean)
    fun pin(conversationIds: List<String>, isPinned: Boolean)
}

internal class ConversationListOptimisticSnapshotDelegateImpl @Inject constructor(
    private val repository: ConversationListRepository,
    private val reducer: ConversationListOptimisticReducer,
) : ConversationListOptimisticSnapshotDelegate {

    private val overrides = MutableStateFlow(ConversationListOptimisticOverrides())

    private val _snapshot = MutableStateFlow<ConversationListSnapshot?>(null)
    override val snapshot: StateFlow<ConversationListSnapshot?> = _snapshot.asStateFlow()

    private var rawSnapshot: StateFlow<ConversationListSnapshot?> = MutableStateFlow(null)
    private var boundScope: CoroutineScope? = null

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
        rawSnapshot = repository.observeInboxSnapshot().stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = null,
        )

        emitEffectiveSnapshots(scope)
        pruneOverridesOnFreshData(scope)
    }

    override fun archive(conversationIds: List<String>) {
        val archivedItemsById = _snapshot.value
            ?.items
            .orEmpty()
            .filter { item ->
                item.conversationId in conversationIds
            }
            .associateBy(ConversationListItem::conversationId)

        overrides.update { overrides ->
            overrides.copy(
                archivedIds = overrides.archivedIds.addAll(conversationIds),
                archivedItemsById = overrides.archivedItemsById.putAll(archivedItemsById),
                restoringById = overrides.restoringById.mutate { restoring ->
                    conversationIds.forEach(restoring::remove)
                },
            )
        }
    }

    override fun restoreArchived(conversationIds: List<String>) {
        val rawItemsById = rawSnapshot.value
            ?.items
            .orEmpty()
            .associateBy(ConversationListItem::conversationId)

        overrides.update { overrides ->
            val archivedItemsById = overrides.archivedItemsById.putAll(
                rawItemsById.filterKeys { conversationId ->
                    conversationId in conversationIds
                },
            )

            overrides.copy(
                archivedIds = overrides.archivedIds.removeAll(conversationIds.toSet()),
                archivedItemsById = archivedItemsById,
                restoringById = overrides.restoringById.mutate { restoring ->
                    conversationIds.forEach { conversationId ->
                        val item = archivedItemsById[conversationId] ?: return@forEach

                        restoring[conversationId] = RestoringConversation(
                            item = item,
                            hasObservedArchivedSnapshot = conversationId !in rawItemsById,
                        )
                    }
                },
            )
        }
    }

    override fun markRead(
        conversationIds: List<String>,
        isRead: Boolean,
    ) {
        val readById = conversationIds.associateWith { isRead }

        overrides.update { overrides ->
            overrides.copy(
                readById = overrides.readById.putAll(readById),
            )
        }
    }

    override fun pin(
        conversationIds: List<String>,
        isPinned: Boolean,
    ) {
        val pinnedById = conversationIds.associateWith { isPinned }

        overrides.update { overrides ->
            overrides.copy(
                pinnedById = overrides.pinnedById.putAll(pinnedById),
            )
        }
    }

    private fun emitEffectiveSnapshots(scope: CoroutineScope) {
        scope.launch {
            combine(rawSnapshot, overrides) { snapshot, overrides ->
                snapshot?.copy(
                    items = reducer.apply(
                        items = snapshot.items,
                        overrides = overrides,
                    ),
                )
            }.collect { effectiveSnapshot ->
                _snapshot.value = effectiveSnapshot
            }
        }
    }

    private fun pruneOverridesOnFreshData(scope: CoroutineScope) {
        scope.launch {
            rawSnapshot.filterNotNull().collect { snapshot ->
                overrides.update { overrides ->
                    reducer.prune(
                        items = snapshot.items,
                        overrides = overrides,
                    )
                }
            }
        }
    }
}
