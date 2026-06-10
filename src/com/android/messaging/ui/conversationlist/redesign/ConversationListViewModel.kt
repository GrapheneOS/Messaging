package com.android.messaging.ui.conversationlist.redesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.redesign.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

internal interface ConversationListScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ConversationListViewModel @Inject constructor(
    private val repository: ConversationListRepository,
    private val uiStateMapper: ConversationListUiStateMapper,
    private val selectionDelegate: ConversationListSelectionDelegate,
    private val actionsDelegate: ConversationListActionsDelegate,
) : ViewModel(),
    ConversationListScreenModel {

    private val isScrollUpVisible = MutableStateFlow(false)

    private val snapshot: StateFlow<ConversationListSnapshot?> = repository
        .observeInboxSnapshot()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null,
        )

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = merge(
        _effects.asSharedFlow(),
        actionsDelegate.effects,
    )

    override val uiState: StateFlow<State> = combine(
        snapshot.filterNotNull(),
        selectionDelegate.selectedIds,
        isScrollUpVisible,
    ) { snapshot, selectedIds, isScrollUpVisible ->
        uiStateMapper.map(
            snapshot = snapshot,
            selectedConversationIds = selectedIds,
            isScrollUpVisible = isScrollUpVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = State(),
    )

    init {
        selectionDelegate.bind(
            scope = viewModelScope,
            snapshotFlow = snapshot,
        )
        actionsDelegate.bind(scope = viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.DialogAction -> onDialogAction(action)
            is Action.ListAction -> onListAction(action)
            is Action.NavigationAction -> onNavigationAction(action)
            is Action.SelectionAction -> onSelectionAction(action)
        }
    }

    private fun onDialogAction(action: Action.DialogAction) {
        when (action) {
            is Action.AddContactConfirmed -> onAddContactConfirmed(action.destination)
            Action.BlockConfirmed -> onBlockConfirmed()
            Action.DeleteConfirmed -> onDeleteConfirmed()

            is Action.ArchiveUndoClicked -> onArchiveUndoClicked(
                conversationIds = action.conversationIds,
                isArchived = action.isArchived,
            )
        }
    }

    private fun onListAction(action: Action.ListAction) {
        when (action) {
            is Action.ConversationClicked -> {
                onConversationClick(action.conversationId)
            }

            is Action.ConversationLongClicked -> {
                onConversationLongClick(action.conversationId)
            }

            is Action.NewestConversationVisibilityChanged -> {
                onNewestConversationVisibilityChanged(action.isVisible)
            }
        }
    }

    private fun onNavigationAction(action: Action.NavigationAction) {
        when (action) {
            Action.ArchivedConversationsClicked -> {
                _effects.tryEmit(Effect.OpenArchivedConversations)
            }

            Action.BlockedParticipantsClicked -> {
                _effects.tryEmit(Effect.OpenBlockedParticipants)
            }

            Action.DebugOptionsClicked -> {
                _effects.tryEmit(Effect.OpenDebugOptions)
            }

            Action.ScrollUpClicked -> {
                _effects.tryEmit(Effect.ScrollToTop)
            }

            Action.SettingsClicked -> {
                _effects.tryEmit(Effect.OpenSettings)
            }

            Action.StartChatClicked -> {
                _effects.tryEmit(Effect.StartChat)
            }
        }
    }

    private fun onSelectionAction(action: Action.SelectionAction) {
        when (action) {
            Action.AddContactClicked -> onAddContactClick()
            Action.ArchiveClicked -> onArchiveClick(isArchived = true)
            Action.UnarchiveClicked -> onArchiveClick(isArchived = false)
            Action.BlockClicked -> onBlockClick()
            Action.SelectionCleared -> selectionDelegate.clear()
        }
    }

    private fun onConversationClick(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        when {
            selectionDelegate.isSelectionActive() -> {
                selectionDelegate.toggle(resolvedConversationId)
            }

            else -> {
                _effects.tryEmit(Effect.OpenConversation(resolvedConversationId))
            }
        }
    }

    private fun onConversationLongClick(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        selectionDelegate.toggle(resolvedConversationId)
    }

    private fun onNewestConversationVisibilityChanged(isVisible: Boolean) {
        if (isScrollUpVisible.value == !isVisible) {
            return
        }

        isScrollUpVisible.value = !isVisible
        repository.setNewestConversationVisible(isVisible)
    }

    private fun onArchiveClick(isArchived: Boolean) {
        val selectedItems = selectionDelegate.currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        actionsDelegate.setArchived(
            conversationIds = selectedItems.map(ConversationListItem::conversationId),
            isArchived = isArchived,
            shouldShowSnackbar = true
        )
        selectionDelegate.clear()
    }

    private fun onAddContactClick() {
        val destination = singleSelectedDestination() ?: return

        _effects.tryEmit(Effect.ConfirmAddContact(destination))
    }

    private fun onAddContactConfirmed(destination: String) {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return

        _effects.tryEmit(Effect.OpenAddContact(resolvedDestination))
        selectionDelegate.clear()
    }

    private fun onArchiveUndoClicked(
        conversationIds: List<String>,
        isArchived: Boolean,
    ) {
        actionsDelegate.setArchived(
            conversationIds = conversationIds,
            isArchived = !isArchived,
            shouldShowSnackbar = false,
        )
    }

    private fun onBlockClick() {
        val selectedItem = singleSelectedItem() ?: return
        val destination = singleSelectedDestination() ?: return

        _effects.tryEmit(
            Effect.ConfirmBlock(
                conversationId = selectedItem.conversationId,
                destination = destination,
            ),
        )
    }

    private fun onBlockConfirmed() {
        val selectedItem = singleSelectedItem() ?: return

        actionsDelegate.block(selectedItem)
        selectionDelegate.clear()
    }

    private fun onDeleteConfirmed() {
        val selectedItems = selectionDelegate.currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        actionsDelegate.delete(selectedItems)
        selectionDelegate.clear()
    }

    private fun singleSelectedItem(): ConversationListItem? {
        return selectionDelegate.currentSelectedItems().singleOrNull()
    }

    private fun singleSelectedDestination(): String? {
        return singleSelectedItem()
            ?.participant
            ?.otherNormalizedDestination
            ?.takeIf(String::isNotBlank)
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
