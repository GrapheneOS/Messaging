package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ConversationListSelectionDelegate {
    val selectedIds: StateFlow<PersistentSet<String>>

    fun bind(scope: CoroutineScope, snapshotFlow: StateFlow<ConversationListSnapshot?>)
    fun toggle(conversationId: String)
    fun clear()
    fun isSelectionActive(): Boolean
    fun currentSelectedItems(): ImmutableList<ConversationListItem>
}

internal class ConversationListSelectionDelegateImpl @Inject constructor() :
    ConversationListSelectionDelegate {

    private val _selectedIds = MutableStateFlow<PersistentSet<String>>(persistentSetOf())
    override val selectedIds: StateFlow<PersistentSet<String>> = _selectedIds.asStateFlow()

    private var boundSnapshotFlow: StateFlow<ConversationListSnapshot?>? = null

    override fun bind(
        scope: CoroutineScope,
        snapshotFlow: StateFlow<ConversationListSnapshot?>,
    ) {
        if (boundSnapshotFlow != null) {
            return
        }

        boundSnapshotFlow = snapshotFlow

        scope.launch {
            snapshotFlow
                .filterNotNull()
                .collect { snapshot ->
                    pruneSelection(snapshot)
                }
        }
    }

    override fun toggle(conversationId: String) {
        _selectedIds.update { currentSelectedIds ->
            when {
                conversationId in currentSelectedIds -> currentSelectedIds.remove(conversationId)
                else -> currentSelectedIds.add(conversationId)
            }
        }
    }

    override fun clear() {
        _selectedIds.value = persistentSetOf()
    }

    override fun isSelectionActive(): Boolean {
        return _selectedIds.value.isNotEmpty()
    }

    override fun currentSelectedItems(): ImmutableList<ConversationListItem> {
        val items = boundSnapshotFlow?.value?.items ?: return persistentListOf()
        val currentSelectedIds = _selectedIds.value

        return items
            .filter { item ->
                item.conversationId in currentSelectedIds
            }
            .toImmutableList()
    }

    private fun pruneSelection(snapshot: ConversationListSnapshot) {
        if (_selectedIds.value.isEmpty()) {
            return
        }

        val visibleConversationIds = snapshot.items
            .map { item ->
                item.conversationId
            }
            .toSet()

        _selectedIds.update { currentSelectedIds ->
            currentSelectedIds
                .filter { conversationId ->
                    conversationId in visibleConversationIds
                }
                .toPersistentSet()
        }
    }
}
