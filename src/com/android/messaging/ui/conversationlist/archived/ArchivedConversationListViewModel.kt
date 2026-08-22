package com.android.messaging.ui.conversationlist.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMode
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.ui.conversationlist.archived.mapper.ArchivedConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListAction as Action
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListNavEvent as NavEvent
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState as State
import com.android.messaging.ui.conversationlist.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface ArchivedConversationListScreenModel {
    val effects: Flow<Effect>
    val navigationEvents: Flow<NavEvent>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ArchivedConversationListViewModel @Inject constructor(
    private val actionsDelegate: ConversationListActionsDelegate,
    private val optimisticSnapshotDelegate: ConversationListOptimisticSnapshotDelegate,
    private val selectionDelegate: ConversationListSelectionDelegate,
    private val uiStateMapper: ArchivedConversationListUiStateMapper,
    debugFeaturesProvider: DebugFeaturesProvider,
) : ViewModel(),
    ArchivedConversationListScreenModel {

    private val snapshot: StateFlow<ConversationListSnapshot?> = optimisticSnapshotDelegate.snapshot

    private val isDebugEnabled = debugFeaturesProvider.isEnabled()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val _navigationEvents = Channel<NavEvent>(Channel.BUFFERED)
    override val navigationEvents: Flow<NavEvent> = _navigationEvents.receiveAsFlow()

    override val uiState: StateFlow<State> = combine(
        snapshot.filterNotNull(),
        selectionDelegate.selectedIds,
    ) { snapshot, selectedIds ->
        uiStateMapper.map(
            snapshot = snapshot,
            selectedConversationIds = selectedIds,
            isDebugEnabled = isDebugEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = State(),
    )

    init {
        optimisticSnapshotDelegate.bind(
            scope = viewModelScope,
            mode = ConversationListMode.Archived
        )
        selectionDelegate.bind(
            scope = viewModelScope,
            snapshot = snapshot
        )
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.ConfirmationAction -> onConfirmationAction(action)
            is Action.ListAction -> onListAction(action)
            is Action.NavigationAction -> onNavigationAction(action)
            is Action.SelectionAction -> onSelectionAction(action)
            is Action.SnackbarAction -> onSnackbarAction(action)
        }
    }

    private fun onConfirmationAction(action: Action.ConfirmationAction) {
        when (action) {
            Action.DeleteSelectedConfirmed -> onDeleteSelected()
        }
    }

    private fun onListAction(action: Action.ListAction) {
        when (action) {
            is Action.ConversationClicked -> {
                onConversationClicked(action.conversationId)
            }

            is Action.ConversationLongClicked -> {
                selectionDelegate.toggle(action.conversationId)
            }

            is Action.ConversationSwipedToUnarchive -> {
                onConversationSwipedToUnarchive(action.conversationId)
            }

            is Action.AvatarMessageClicked -> {
                _navigationEvents.trySend(NavEvent.OpenConversation(action.conversationId))
            }

            is Action.AvatarCallClicked -> {
                _effects.trySend(Effect.PlaceCall(action.destination))
            }

            is Action.AvatarContactClicked -> {
                onAvatarContactClicked(action.avatar)
            }

            is Action.AvatarInfoClicked -> {
                _navigationEvents.trySend(NavEvent.OpenConversationSettings(action.conversationId))
            }
        }
    }

    private fun onNavigationAction(action: Action.NavigationAction) {
        when (action) {
            Action.DebugOptionsClicked -> {
                _effects.trySend(Effect.OpenDebugOptions)
            }
        }
    }

    private fun onSelectionAction(action: Action.SelectionAction) {
        when (action) {
            Action.UnarchiveSelectedClicked -> {
                onUnarchiveSelected()
            }

            Action.SelectionCleared -> {
                selectionDelegate.clear()
            }
        }
    }

    private fun onSnackbarAction(action: Action.SnackbarAction) {
        when (action) {
            is Action.UnarchiveUndoClicked -> {
                onUnarchiveUndo(action.conversationIds)
            }

            is Action.UnarchiveSnackbarDismissed -> {
                optimisticSnapshotDelegate.discardRemoval(action.conversationIds)
            }
        }
    }

    private fun onConversationClicked(conversationId: ConversationId) {
        when {
            selectionDelegate.selectedIds.value.isNotEmpty() -> {
                selectionDelegate.toggle(conversationId)
            }

            else -> {
                _navigationEvents.trySend(NavEvent.OpenConversation(conversationId))
            }
        }
    }

    private fun onConversationSwipedToUnarchive(conversationId: ConversationId) {
        unarchive(listOf(conversationId))
    }

    private fun onUnarchiveSelected() {
        withSelectedIds { conversationIds ->
            unarchive(conversationIds)
            selectionDelegate.clear()
        }
    }

    private fun unarchive(conversationIds: List<ConversationId>) {
        if (conversationIds.isEmpty()) {
            return
        }

        optimisticSnapshotDelegate.remove(conversationIds)
        viewModelScope.launch {
            actionsDelegate.setArchived(
                conversationIds = conversationIds,
                isArchived = false,
            )
        }
        _effects.trySend(Effect.ConversationsUnarchived(conversationIds.toImmutableList()))
    }

    private fun onUnarchiveUndo(conversationIds: List<ConversationId>) {
        if (conversationIds.isEmpty()) {
            return
        }

        optimisticSnapshotDelegate.restore(conversationIds)
        viewModelScope.launch {
            actionsDelegate.setArchived(
                conversationIds = conversationIds,
                isArchived = true,
            )
        }
    }

    private fun onDeleteSelected() {
        val selectedItems = currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        actionsDelegate.delete(selectedItems)
        selectionDelegate.clear()
    }

    private fun onAvatarContactClicked(avatar: ConversationListAvatarUiModel) {
        _effects.trySend(
            Effect.ShowOrAddContact(
                contactId = avatar.contactId,
                lookupKey = avatar.lookupKey,
                avatarUri = avatar.uri,
                destination = avatar.normalizedDestination,
            ),
        )
    }

    private inline fun withSelectedIds(block: (List<ConversationId>) -> Unit) {
        val selectedItems = currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        block(selectedItems.map(ConversationListItem::conversationId))
    }

    private fun currentSelectedItems(): List<ConversationListItem> {
        val selectedIds = selectionDelegate.selectedIds.value

        return snapshot.value
            ?.items
            .orEmpty()
            .filter { item -> item.conversationId in selectedIds }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
