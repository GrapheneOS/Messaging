package com.android.messaging.ui.conversation.addparticipants

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsUiState
import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface AddParticipantsModel {
    val effects: Flow<AddParticipantsEffect>
    val uiState: StateFlow<AddParticipantsUiState>

    fun onConversationIdChanged(conversationId: String?)
    fun onLoadMore()
    fun onQueryChanged(query: String)
    fun onRecipientClicked(destination: String)
    fun onConfirmClick()
}

@HiltViewModel
internal class AddParticipantsViewModel @Inject constructor(
    private val conversationParticipantsRepository: ConversationParticipantsRepository,
    private val isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded,
    private val recipientPickerDelegate: RecipientPickerDelegate,
    private val resolveConversationId: ResolveConversationId,
    private val savedStateHandle: SavedStateHandle,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    AddParticipantsModel {

    private val conversationIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = CONVERSATION_ID_KEY,
        initialValue = null,
    )
    private val _effects = MutableSharedFlow<AddParticipantsEffect>(
        extraBufferCapacity = 1,
    )
    private val localUiState = MutableStateFlow(
        value = LocalAddParticipantsUiState(),
    )

    override val effects = _effects.asSharedFlow()

    override val uiState: StateFlow<AddParticipantsUiState> = combine(
        localUiState,
        recipientPickerDelegate.state,
    ) { localState, recipientPickerUiState ->
        AddParticipantsUiState(
            existingParticipants = localState.existingParticipants,
            isLoadingConversationParticipants = localState.isLoadingConversationParticipants,
            isResolvingConversation = localState.isResolvingConversation,
            recipientPickerUiState = recipientPickerUiState,
            selectedRecipientDestinations = localState.selectedRecipientDestinations,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = AddParticipantsUiState(
            existingParticipants = localUiState.value.existingParticipants,
            isLoadingConversationParticipants = localUiState
                .value
                .isLoadingConversationParticipants,
            isResolvingConversation = localUiState.value.isResolvingConversation,
            recipientPickerUiState = recipientPickerDelegate.state.value,
            selectedRecipientDestinations = localUiState.value.selectedRecipientDestinations,
        ),
    )

    init {
        recipientPickerDelegate.bind(scope = viewModelScope)
        bindConversationParticipants()
    }

    private fun bindConversationParticipants() {
        viewModelScope.launch(mainDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                updateLocalUiState(
                    localUiState.value.copy(
                        existingParticipants = persistentEmptyParticipants(),
                        isLoadingConversationParticipants = conversationId != null,
                        isResolvingConversation = false,
                        selectedRecipientDestinations = persistentEmptyDestinations(),
                    ),
                )
                recipientPickerDelegate.onExcludedDestinationsChanged(
                    destinations = emptySet(),
                )

                if (conversationId == null) {
                    return@collectLatest
                }

                conversationParticipantsRepository
                    .getParticipants(conversationId = conversationId)
                    .collect { participants ->
                        val selectedDestinations = localUiState.value
                            .selectedRecipientDestinations
                            .filterNot { selectedDestination ->
                                participants.any { participant ->
                                    participant.destination == selectedDestination
                                }
                            }
                            .toImmutableList()

                        updateLocalUiState(
                            localUiState.value.copy(
                                existingParticipants = participants,
                                isLoadingConversationParticipants = false,
                                selectedRecipientDestinations = selectedDestinations,
                            ),
                        )
                        recipientPickerDelegate.onExcludedDestinationsChanged(
                            destinations = participants
                                .map { participant ->
                                    participant.destination
                                }
                                .toSet(),
                        )
                    }
            }
        }
    }

    override fun onConversationIdChanged(conversationId: String?) {
        if (conversationId != conversationIdFlow.value) {
            savedStateHandle[CONVERSATION_ID_KEY] = conversationId
        }
    }

    override fun onLoadMore() {
        recipientPickerDelegate.onLoadMore()
    }

    override fun onQueryChanged(query: String) {
        recipientPickerDelegate.onQueryChanged(query = query)
    }

    override fun onRecipientClicked(destination: String) {
        val trimmedDestination = destination.trim()
        val currentUiState = localUiState.value

        val shouldIgnoreRecipientClick = trimmedDestination.isEmpty() ||
            currentUiState.isLoadingConversationParticipants ||
            currentUiState.isResolvingConversation ||
            currentUiState.existingParticipants.any { participant ->
                participant.destination == trimmedDestination
            }

        if (shouldIgnoreRecipientClick) {
            return
        }

        val nextSelectedDestinations = when {
            trimmedDestination in currentUiState.selectedRecipientDestinations -> {
                currentUiState.selectedRecipientDestinations - trimmedDestination
            }

            else -> {
                currentUiState.selectedRecipientDestinations + trimmedDestination
            }
        }

        updateLocalUiState(
            currentUiState.copy(
                selectedRecipientDestinations = nextSelectedDestinations.toImmutableList(),
            ),
        )
    }

    override fun onConfirmClick() {
        val currentUiState = localUiState.value

        val shouldIgnoreConfirmClick = currentUiState.isLoadingConversationParticipants ||
            currentUiState.isResolvingConversation ||
            currentUiState.selectedRecipientDestinations.isEmpty()

        if (shouldIgnoreConfirmClick) {
            return
        }

        val allDestinations = (
            currentUiState.existingParticipants.map { participant ->
                participant.destination
            } + currentUiState.selectedRecipientDestinations
            ).distinct()

        if (isConversationRecipientLimitExceeded(participantCount = allDestinations.size)) {
            showMessage(messageResId = R.string.too_many_participants)
            return
        }

        viewModelScope.launch(mainDispatcher) {
            updateLocalUiState(
                currentUiState.copy(
                    isResolvingConversation = true,
                ),
            )

            when (val result = resolveConversationId(destinations = allDestinations)) {
                is ResolveConversationIdResult.Resolved -> {
                    updateLocalUiState(
                        localUiState.value.copy(
                            isResolvingConversation = false,
                            selectedRecipientDestinations = persistentEmptyDestinations(),
                        ),
                    )
                    _effects.tryEmit(
                        AddParticipantsEffect.NavigateToConversation(
                            conversationId = result.conversationId,
                        ),
                    )
                }

                ResolveConversationIdResult.EmptyDestinations,
                ResolveConversationIdResult.NotResolved,
                -> {
                    updateLocalUiState(
                        localUiState.value.copy(
                            isResolvingConversation = false,
                        ),
                    )
                    showMessage(messageResId = R.string.conversation_creation_failure)
                }
            }
        }
    }

    private fun showMessage(messageResId: Int) {
        _effects.tryEmit(
            AddParticipantsEffect.ShowMessage(
                messageResId = messageResId,
            ),
        )
    }

    private fun updateLocalUiState(uiState: LocalAddParticipantsUiState) {
        localUiState.value = uiState
    }

    private fun persistentEmptyParticipants(): PersistentList<ConversationRecipient> {
        return persistentListOf()
    }

    private fun persistentEmptyDestinations(): PersistentList<String> {
        return persistentListOf()
    }

    private data class LocalAddParticipantsUiState(
        val existingParticipants: ImmutableList<ConversationRecipient> = persistentListOf(),
        val isLoadingConversationParticipants: Boolean = true,
        val isResolvingConversation: Boolean = false,
        val selectedRecipientDestinations: ImmutableList<String> = persistentListOf(),
    )

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
