package com.android.messaging.ui.conversationlist.redesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.redesign.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.redesign.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAvatarUiModel
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val debugFeaturesProvider: DebugFeaturesProvider,
) : ViewModel(),
    ConversationListScreenModel {

    private val isScrollUpVisible = MutableStateFlow(false)
    private val isDebugEnabled = MutableStateFlow(debugFeaturesProvider.isEnabled())

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
        isDebugEnabled,
    ) { snapshot, selectedIds, isScrollUpVisible, isDebugEnabled ->
        uiStateMapper.map(
            snapshot = snapshot,
            selectedConversationIds = selectedIds,
            isScrollUpVisible = isScrollUpVisible,
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
        selectionDelegate.bind(
            scope = viewModelScope,
            snapshotFlow = snapshot,
        )
        actionsDelegate.bind(scope = viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.DialogAction -> onDialogAction(action)
            is Action.LifecycleAction -> onLifecycleAction(action)
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

            is Action.BlockUndoClicked -> actionsDelegate.unblock(
                conversationId = action.conversationId,
                destination = action.destination,
            )
        }
    }

    private fun onLifecycleAction(action: Action.LifecycleAction) {
        when (action) {
            Action.ScreenResumed -> {
                isDebugEnabled.value = debugFeaturesProvider.isEnabled()
                repository.refresh()
            }
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

            is Action.AvatarMessageClicked -> {
                _effects.tryEmit(Effect.OpenConversation(action.conversationId))
            }

            is Action.AvatarCallClicked -> {
                _effects.tryEmit(Effect.PlaceCall(action.destination))
            }

            is Action.AvatarContactClicked -> {
                onAvatarContactClick(action.avatar)
            }

            is Action.ConversationSwipedToArchive -> {
                onConversationSwipedToArchive(action.conversationId)
            }

            is Action.ConversationSwipedToToggleRead -> {
                onConversationSwipedToToggleRead(action.conversationId)
            }
        }
    }

    private fun onAvatarContactClick(avatar: ConversationListAvatarUiModel) {
        _effects.tryEmit(
            Effect.ShowOrAddContact(
                contactId = avatar.contactId,
                lookupKey = avatar.lookupKey,
                avatarUri = avatar.uri,
                destination = avatar.normalizedDestination,
            ),
        )
    }

    private fun onConversationSwipedToArchive(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        actionsDelegate.setArchived(
            conversationIds = listOf(resolvedConversationId),
            isArchived = true,
            shouldShowSnackbar = true,
        )
    }

    private fun onConversationSwipedToToggleRead(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        val item = snapshot.value
            ?.items
            ?.firstOrNull { it.conversationId == resolvedConversationId }
            ?: return

        when {
            item.latestMessage.isRead -> actionsDelegate.markUnread(resolvedConversationId)
            else -> actionsDelegate.markRead(resolvedConversationId)
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
            Action.MarkReadClicked -> onMarkRead(isRead = true)
            Action.MarkUnreadClicked -> onMarkRead(isRead = false)
            Action.SelectionCleared -> selectionDelegate.clear()
            Action.UnsnoozeClicked -> onUnsnoozeClick()
            is Action.SnoozeOptionSelected -> onSnoozeOptionSelected(action.option)
        }
    }

    private fun onMarkRead(isRead: Boolean) {
        val selectedItems = selectionDelegate.currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        selectedItems.forEach { item ->
            when {
                isRead -> actionsDelegate.markRead(item.conversationId)
                else -> actionsDelegate.markUnread(item.conversationId)
            }
        }

        selectionDelegate.clear()
    }

    private fun onUnsnoozeClick() {
        val selectedItems = selectionDelegate.currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        selectedItems.forEach { item ->
            repository.clearSnooze(item.conversationId)
        }

        selectionDelegate.clear()
    }

    private fun onSnoozeOptionSelected(option: SnoozeOption) {
        val selectedItems = selectionDelegate.currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        selectedItems.forEach { item ->
            repository.snooze(item.conversationId, option)
        }

        selectionDelegate.clear()
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

        if (!isArchived || isScrollUpVisible.value) {
            return
        }

        viewModelScope.launch {
            snapshot.filterNotNull().first { restoredSnapshot ->
                restoredSnapshot.items.any { item -> item.conversationId in conversationIds }
            }

            _effects.tryEmit(Effect.ScrollToTop)
        }
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
