package com.android.messaging.ui.conversationlist.redesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.domain.conversationlist.model.ConversationListActionTarget
import com.android.messaging.domain.conversationlist.usecase.DeleteConversations
import com.android.messaging.domain.conversationlist.usecase.SetConversationArchived
import com.android.messaging.domain.conversationlist.usecase.SetConversationBlocked
import com.android.messaging.ui.conversationlist.redesign.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListUiState as State
import com.android.messaging.ui.conversationlist.redesign.model.SelectedConversationUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private val deleteConversations: DeleteConversations,
    private val setConversationArchived: SetConversationArchived,
    private val setConversationBlocked: SetConversationBlocked,
) : ViewModel(),
    ConversationListScreenModel {

    private val selectedConversationIds = MutableStateFlow<PersistentSet<String>>(
        persistentSetOf(),
    )
    private val isScrollUpVisible = MutableStateFlow(false)

    private var isNewestConversationVisible = true

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = combine(
        repository.observeInboxSnapshot().onEach(::pruneSelection),
        selectedConversationIds,
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
            Action.BlockConfirmed -> onBlockConfirmed()
            Action.DeleteConfirmed -> onDeleteConfirmed()
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
            Action.ArchivedConversationsClicked -> onArchivedConversationsClick()
            Action.BlockedParticipantsClicked -> onBlockedParticipantsClick()
            Action.ScrollUpClicked -> onScrollUpClick()
            Action.SettingsClicked -> onSettingsClick()
            Action.StartChatClicked -> onStartChatClick()
        }
    }

    private fun onSelectionAction(action: Action.SelectionAction) {
        when (action) {
            Action.AddContactClicked -> onAddContactClick()
            Action.ArchiveClicked -> onArchiveClick()
            Action.BlockClicked -> onBlockClick()
            Action.SelectionCleared -> onSelectionCleared()
            Action.UnarchiveClicked -> onUnarchiveClick()
        }
    }

    private fun onConversationClick(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        when {
            uiState.value.isSelectionMode -> {
                toggleSelection(resolvedConversationId)
            }

            else -> {
                _effects.tryEmit(Effect.OpenConversation(resolvedConversationId))
            }
        }
    }

    private fun onConversationLongClick(conversationId: String) {
        val resolvedConversationId = conversationId.takeIf(String::isNotBlank) ?: return

        toggleSelection(resolvedConversationId)
    }

    private fun onSelectionCleared() {
        selectedConversationIds.value = persistentSetOf()
    }

    private fun onStartChatClick() {
        _effects.tryEmit(Effect.StartChat)
    }

    private fun onArchivedConversationsClick() {
        _effects.tryEmit(Effect.OpenArchivedConversations)
    }

    private fun onBlockedParticipantsClick() {
        _effects.tryEmit(Effect.OpenBlockedParticipants)
    }

    private fun onSettingsClick() {
        _effects.tryEmit(Effect.OpenSettings)
    }

    private fun onScrollUpClick() {
        _effects.tryEmit(Effect.ScrollToTop)
    }

    private fun onNewestConversationVisibilityChanged(isVisible: Boolean) {
        if (isNewestConversationVisible == isVisible) {
            return
        }

        isNewestConversationVisible = isVisible
        isScrollUpVisible.value = !isVisible
        repository.setNewestConversationVisible(isVisible)
    }

    private fun onAddContactClick() {
        val selectedConversation = singleSelectedConversation() ?: return

        _effects.tryEmit(Effect.ConfirmAddContact(selectedConversation))
    }

    private fun onBlockClick() {
        val selectedConversation = singleSelectedConversation() ?: return

        _effects.tryEmit(Effect.ConfirmBlock(selectedConversation))
    }

    private fun onArchiveClick() {
        updateSelectedArchiveStatus(isArchived = true)
    }

    private fun onUnarchiveClick() {
        updateSelectedArchiveStatus(isArchived = false)
    }

    private fun onDeleteConfirmed() {
        val selectedConversations = uiState.value.selection.selectedConversations

        if (selectedConversations.isEmpty()) {
            return
        }

        deleteConversations(
            selectedConversations.map { conversation ->
                ConversationListActionTarget(
                    conversationId = conversation.conversationId,
                    cutoffTimestampMillis = conversation.timestampMillis,
                )
            },
        )

        onSelectionCleared()
    }

    private fun onBlockConfirmed() {
        val selectedConversation = singleSelectedConversation() ?: return
        val destination = selectedConversation.normalizedDestination ?: return

        viewModelScope.launch {
            val success = setConversationBlocked(
                destination = destination,
                conversationId = selectedConversation.conversationId,
                isBlocked = true,
            )

            _effects.emit(
                Effect.ConversationBlocked(
                    destination = destination,
                    success = success,
                ),
            )

            onSelectionCleared()
        }
    }

    private fun updateSelectedArchiveStatus(isArchived: Boolean) {
        val selectedConversations = uiState.value.selection.selectedConversations
        val conversationIds = selectedConversations
            .map { conversation ->
                conversation.conversationId
            }
            .toSet()

        if (conversationIds.isEmpty()) {
            return
        }

        setConversationArchived(
            conversationIds = conversationIds,
            isArchived = isArchived,
        )

        _effects.tryEmit(
            Effect.ConversationsArchived(
                count = conversationIds.size,
                isArchived = isArchived,
            ),
        )

        onSelectionCleared()
    }

    private fun toggleSelection(conversationId: String) {
        selectedConversationIds.update { currentSelectedIds ->
            when {
                conversationId in currentSelectedIds -> currentSelectedIds.remove(conversationId)
                else -> currentSelectedIds.add(conversationId)
            }
        }
    }

    private fun pruneSelection(snapshot: ConversationListSnapshot) {
        val visibleConversationIds = snapshot.items
            .asSequence()
            .map { item ->
                item.conversationId
            }
            .toSet()

        selectedConversationIds.update { currentSelectedIds ->
            currentSelectedIds
                .filter { conversationId ->
                    conversationId in visibleConversationIds
                }
                .toPersistentSet()
        }
    }

    private fun singleSelectedConversation(): SelectedConversationUiModel? {
        return uiState.value.selection.selectedConversations.singleOrNull()
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
