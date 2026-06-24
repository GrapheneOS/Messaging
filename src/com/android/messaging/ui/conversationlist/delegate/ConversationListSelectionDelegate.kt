package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal interface ConversationListSelectionDelegate {
    val selectedIds: StateFlow<ImmutableList<String>>

    fun bind(scope: CoroutineScope, snapshot: StateFlow<ConversationListSnapshot?>)
    fun toggle(conversationId: String)
    fun clear()
}

internal class ConversationListSelectionDelegateImpl @Inject constructor() :
    ConversationListSelectionDelegate {

    private val _selectedIds = MutableStateFlow<PersistentList<String>>(persistentListOf())
    override val selectedIds: StateFlow<ImmutableList<String>> = _selectedIds.asStateFlow()

    private var isBound = false

    override fun bind(
        scope: CoroutineScope,
        snapshot: StateFlow<ConversationListSnapshot?>,
    ) {
        if (isBound) {
            return
        }

        isBound = true

        snapshot
            .filterNotNull()
            .onEach { currentSnapshot ->
                if (_selectedIds.value.isEmpty()) {
                    return@onEach
                }

                val knownIds = currentSnapshot.items.mapTo(
                    destination = HashSet(currentSnapshot.items.size),
                    transform = ConversationListItem::conversationId,
                )

                _selectedIds.update { currentSelectedIds ->
                    currentSelectedIds.retainAll(knownIds)
                }
            }
            .launchIn(scope)
    }

    override fun toggle(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        _selectedIds.update { currentSelectedIds ->
            when {
                resolvedConversationId in currentSelectedIds -> {
                    currentSelectedIds.remove(resolvedConversationId)
                }

                else -> {
                    currentSelectedIds.add(resolvedConversationId)
                }
            }
        }
    }

    override fun clear() {
        _selectedIds.value = persistentListOf()
    }
}
