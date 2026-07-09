package com.android.messaging.ui.conversationlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.ui.conversationlist.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.model.ConversationListUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val optimisticSnapshotDelegate: ConversationListOptimisticSnapshotDelegate,
    private val debugFeaturesProvider: DebugFeaturesProvider,
) : ViewModel(),
    ConversationListScreenModel {

    private val isScrollToTopVisible = MutableStateFlow(false)
    private val isDebugEnabled = MutableStateFlow(debugFeaturesProvider.isEnabled())

    private val snapshot: StateFlow<ConversationListSnapshot?> = optimisticSnapshotDelegate.snapshot

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    override val effects: Flow<Effect> = merge(
        _effects.receiveAsFlow(),
        actionsDelegate.effects,
    )

    override val uiState: StateFlow<State> = combine(
        snapshot.filterNotNull(),
        selectionDelegate.selectedIds,
        isScrollToTopVisible,
        isDebugEnabled,
    ) { snapshot, selectedIds, isScrollToTopVisible, isDebugEnabled ->
        uiStateMapper.map(
            snapshot = snapshot,
            selectedConversationIds = selectedIds,
            isScrollToTopVisible = isScrollToTopVisible,
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
        )
        selectionDelegate.bind(
            scope = viewModelScope,
            snapshot = snapshot,
        )
        actionsDelegate.bind(
            scope = viewModelScope,
        )
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.ConfirmationAction -> onConfirmationAction(action)
            is Action.LifecycleAction -> onLifecycleAction(action)
            is Action.ListAction -> onListAction(action)
            is Action.NavigationAction -> onNavigationAction(action)
            is Action.SelectionAction -> onSelectionAction(action)
            is Action.SnackbarAction -> onSnackbarAction(action)
        }
    }

    private fun onConfirmationAction(action: Action.ConfirmationAction) {
        when (action) {
            is Action.BlockConfirmed -> {
                onBlockConfirmed(
                    conversationId = action.conversationId,
                    destination = action.destination,
                )
            }

            is Action.DeleteConfirmed -> {
                onDeleteConfirmed()
            }
        }
    }

    private fun onSnackbarAction(action: Action.SnackbarAction) {
        when (action) {
            is Action.ArchiveSnackbarDismissed -> {
                optimisticSnapshotDelegate.discardArchived(action.conversationIds)
            }

            is Action.ArchiveUndoClicked -> {
                onArchiveUndoClicked(
                    conversationIds = action.conversationIds,
                    isArchived = action.isArchived,
                )
            }

            is Action.BlockUndoClicked -> {
                actionsDelegate.unblock(
                    conversationId = action.conversationId,
                    destination = action.destination,
                )
            }
        }
    }

    private fun onBlockConfirmed(
        conversationId: String,
        destination: String,
    ) {
        actionsDelegate.block(
            conversationId = conversationId,
            destination = destination,
        )
        selectionDelegate.clear()
    }

    private fun onDeleteConfirmed() {
        val selectedItems = currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        actionsDelegate.delete(selectedItems)
        selectionDelegate.clear()
    }

    private fun onArchiveUndoClicked(
        conversationIds: List<String>,
        isArchived: Boolean,
    ) {
        when {
            isArchived -> optimisticSnapshotDelegate.restoreArchived(conversationIds)
            else -> optimisticSnapshotDelegate.archive(conversationIds)
        }

        actionsDelegate.setArchived(
            conversationIds = conversationIds,
            isArchived = !isArchived,
            shouldShowSnackbar = false,
        )
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
            is Action.AvatarMessageClicked -> {
                _effects.trySend(Effect.OpenConversation(action.conversationId))
            }

            is Action.AvatarCallClicked -> {
                _effects.trySend(Effect.PlaceCall(action.destination))
            }

            is Action.ConversationClicked -> {
                onConversationClick(action.conversationId)
            }

            is Action.ConversationLongClicked -> {
                onConversationLongClick(action.conversationId)
            }

            is Action.NewestConversationVisibilityChanged -> {
                onNewestConversationVisibilityChanged(action.isVisible)
            }

            is Action.AvatarContactClicked -> {
                onAvatarContactClick(action.avatar)
            }

            is Action.AvatarInfoClicked -> {
                _effects.trySend(Effect.OpenConversationSettings(action.conversationId))
            }

            is Action.ConversationSwipedToArchive -> {
                onConversationSwipedToArchive(action.conversationId)
            }

            is Action.ConversationSwipedToToggleRead -> {
                onConversationSwipedToToggleRead(action.conversationId)
            }
        }
    }

    private fun onConversationClick(conversationId: String) {
        when {
            currentSelectedItems().isNotEmpty() -> {
                selectionDelegate.toggle(conversationId)
            }

            else -> {
                _effects.trySend(Effect.OpenConversation(conversationId))
            }
        }
    }

    private fun onConversationLongClick(conversationId: String) {
        selectionDelegate.toggle(conversationId)
    }

    private fun onNewestConversationVisibilityChanged(isVisible: Boolean) {
        val shouldShowScrollToTop = !isVisible

        if (isScrollToTopVisible.value == shouldShowScrollToTop) {
            return
        }

        isScrollToTopVisible.value = shouldShowScrollToTop
        repository.setNewestConversationVisible(isVisible)
    }

    private fun onAvatarContactClick(avatar: ConversationListAvatarUiModel) {
        _effects.trySend(
            Effect.ShowOrAddContact(
                contactId = avatar.contactId,
                lookupKey = avatar.lookupKey,
                avatarUri = avatar.uri,
                destination = avatar.normalizedDestination,
            ),
        )
    }

    private fun onConversationSwipedToArchive(conversationId: String) {
        val conversationIds = listOf(conversationId)

        optimisticSnapshotDelegate.archive(conversationIds)
        actionsDelegate.setArchived(
            conversationIds = conversationIds,
            isArchived = true,
            shouldShowSnackbar = true,
        )
    }

    private fun onConversationSwipedToToggleRead(conversationId: String) {
        val item = itemById(conversationId) ?: return

        val shouldMarkRead = !item.latestMessage.isRead
        val conversationIds = listOf(conversationId)

        optimisticSnapshotDelegate.markRead(
            conversationIds = conversationIds,
            isRead = shouldMarkRead,
        )
        actionsDelegate.setRead(
            conversationIds = conversationIds,
            isRead = shouldMarkRead,
        )
    }

    private fun onNavigationAction(action: Action.NavigationAction) {
        when (action) {
            Action.ArchivedConversationsClicked -> {
                _effects.trySend(Effect.OpenArchivedConversations)
            }

            Action.BlockedParticipantsClicked -> {
                _effects.trySend(Effect.OpenBlockedParticipants)
            }

            Action.DebugOptionsClicked -> {
                _effects.trySend(Effect.OpenDebugOptions)
            }

            Action.ScrollToTopClicked -> {
                _effects.trySend(Effect.ScrollToTop)
            }

            Action.SettingsClicked -> {
                _effects.trySend(Effect.OpenSettings)
            }

            Action.StartChatClicked -> {
                _effects.trySend(Effect.StartChat)
            }
        }
    }

    private fun onSelectionAction(action: Action.SelectionAction) {
        when (action) {
            is Action.AddContactClicked -> {
                onAddContactClick()
            }

            is Action.ArchiveClicked -> {
                onArchiveClick()
            }

            is Action.BlockClicked -> {
                onBlockClick()
            }

            is Action.MarkReadClicked -> {
                onMarkRead(isRead = true)
            }

            is Action.MarkUnreadClicked -> {
                onMarkRead(isRead = false)
            }

            is Action.PinClicked -> {
                onPinClick(isPinned = true)
            }

            is Action.UnpinClicked -> {
                onPinClick(isPinned = false)
            }

            is Action.PinAnimationPrepared -> {
                commitPinChange(
                    conversationIds = action.conversationIds,
                    isPinned = action.isPinned,
                )
            }

            is Action.SnoozeOptionSelected -> {
                onSnoozeOptionSelected(action.option)
            }

            is Action.UnsnoozeClicked -> {
                onUnsnoozeClick()
            }

            is Action.SelectionCleared -> {
                selectionDelegate.clear()
            }
        }
    }

    private fun onAddContactClick() {
        val destination = singleSelectedDestination() ?: return

        _effects.trySend(Effect.ConfirmAddContact(destination))
        selectionDelegate.clear()
    }

    private fun onArchiveClick() {
        withSelectedIds { conversationIds ->
            optimisticSnapshotDelegate.archive(conversationIds)
            actionsDelegate.setArchived(
                conversationIds = conversationIds,
                isArchived = true,
                shouldShowSnackbar = true,
            )
            selectionDelegate.clear()
        }
    }

    private fun onBlockClick() {
        val selectedItem = singleSelectedItem() ?: return
        val destination = singleSelectedDestination() ?: return

        _effects.trySend(
            Effect.ConfirmBlock(
                conversationId = selectedItem.conversationId,
                destination = destination,
            ),
        )
    }

    private fun onMarkRead(isRead: Boolean) {
        withSelectedIds { conversationIds ->
            optimisticSnapshotDelegate.markRead(
                conversationIds = conversationIds,
                isRead = isRead,
            )
            actionsDelegate.setRead(
                conversationIds = conversationIds,
                isRead = isRead,
            )
            selectionDelegate.clear()
        }
    }

    private fun onPinClick(isPinned: Boolean) {
        withSelectedIds { conversationIds ->
            _effects.trySend(
                Effect.PreparePinAnimation(
                    conversationIds = conversationIds.toImmutableList(),
                    isPinned = isPinned,
                ),
            )
        }
    }

    private fun commitPinChange(
        conversationIds: List<String>,
        isPinned: Boolean,
    ) {
        optimisticSnapshotDelegate.pin(
            conversationIds = conversationIds,
            isPinned = isPinned,
        )
        actionsDelegate.setPinned(
            conversationIds = conversationIds,
            isPinned = isPinned,
        )
        selectionDelegate.clear()
    }

    private fun onSnoozeOptionSelected(option: SnoozeOption) {
        withSelectedIds { conversationIds ->
            actionsDelegate.snooze(
                conversationIds = conversationIds,
                option = option,
            )
            selectionDelegate.clear()
        }
    }

    private fun onUnsnoozeClick() {
        withSelectedIds { conversationIds ->
            actionsDelegate.unsnooze(conversationIds)
            selectionDelegate.clear()
        }
    }

    private inline fun withSelectedIds(block: (List<String>) -> Unit) {
        val selectedItems = currentSelectedItems()

        if (selectedItems.isEmpty()) {
            return
        }

        block(selectedItems.map(ConversationListItem::conversationId))
    }

    private fun itemById(conversationId: String): ConversationListItem? {
        return snapshot.value
            ?.items
            ?.firstOrNull { item -> item.conversationId == conversationId }
    }

    private fun singleSelectedItem(): ConversationListItem? {
        return currentSelectedItems().singleOrNull()
    }

    private fun currentSelectedItems(): List<ConversationListItem> {
        val currentSelectedIds = selectionDelegate.selectedIds.value

        return snapshot.value
            ?.items
            .orEmpty()
            .filter { item ->
                item.conversationId in currentSelectedIds
            }
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
