package com.android.messaging.ui.conversation.addparticipants

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsUiState
import com.android.messaging.ui.conversation.recipientpicker.delegate.ConversationResolutionDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegate
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.model.picker.sanitizedOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface AddParticipantsScreenModel {
    val effects: Flow<AddParticipantsEffect>
    val uiState: StateFlow<AddParticipantsUiState>

    fun onConversationIdChanged(conversationId: String?)
    fun onLoadMore()
    fun onQueryChanged(query: String)
    fun onRecipientClicked(recipient: SelectedRecipient)
    fun onConfirmClick()
}

@HiltViewModel
internal class AddParticipantsViewModel @Inject constructor(
    private val contactDestinationFormatter: ContactDestinationFormatter,
    private val conversationParticipantsRepository: ConversationParticipantsRepository,
    private val isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded,
    private val recipientPickerDelegate: RecipientPickerDelegate,
    private val selectedRecipientsDelegate: SelectedRecipientsDelegate,
    private val conversationResolutionDelegate: ConversationResolutionDelegate,
    private val savedStateHandle: SavedStateHandle,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    AddParticipantsScreenModel {

    private val conversationIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        key = CONVERSATION_ID_KEY,
        initialValue = null,
    )
    private val effectsChannel = Channel<AddParticipantsEffect>(
        capacity = Channel.BUFFERED,
    )
    private val localUiState = MutableStateFlow(
        value = LocalAddParticipantsUiState(),
    )

    override val effects = effectsChannel.receiveAsFlow()

    override val uiState: StateFlow<AddParticipantsUiState> = combine(
        localUiState,
        recipientPickerDelegate.state,
        selectedRecipientsDelegate.state,
        conversationResolutionDelegate.state,
        ::buildUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = buildUiState(
            localState = localUiState.value,
            recipientPickerUiState = recipientPickerDelegate.state.value,
            selectedRecipients = selectedRecipientsDelegate.state.value,
            resolutionState = conversationResolutionDelegate.state.value,
        ),
    )

    init {
        recipientPickerDelegate.bind(scope = viewModelScope)
        conversationResolutionDelegate.bind(scope = viewModelScope)
        bindConversationParticipants()
        observeResolutionOutcomes()
    }

    private fun bindConversationParticipants() {
        viewModelScope.launch(mainDispatcher) {
            var isFirstEmission = true

            conversationIdFlow.collectLatest { conversationId ->
                if (!isFirstEmission) {
                    conversationResolutionDelegate.cancel()
                    selectedRecipientsDelegate.clear()
                }

                isFirstEmission = false

                localUiState.value = localUiState.value.copy(
                    existingParticipants = persistentEmptyParticipants(),
                    existingParticipantCanonicalDestinations = persistentSetOf(),
                    isLoadingConversationParticipants = conversationId != null,
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
                        val canonicalDestinations = participants
                            .asSequence()
                            .map { participant ->
                                contactDestinationFormatter.canonicalize(
                                    value = participant.destination,
                                )
                            }
                            .toImmutableSet()

                        selectedRecipientsDelegate.removeWhere { recipient ->
                            recipient.destination in canonicalDestinations
                        }

                        localUiState.value = localUiState.value.copy(
                            existingParticipants = participants,
                            existingParticipantCanonicalDestinations = canonicalDestinations,
                            isLoadingConversationParticipants = false,
                        )

                        recipientPickerDelegate.onExcludedDestinationsChanged(
                            destinations = participants
                                .asSequence()
                                .map { participant -> participant.destination }
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

    override fun onRecipientClicked(recipient: SelectedRecipient) {
        val sanitized = recipient.sanitizedOrNull() ?: return
        val currentUiState = localUiState.value

        val shouldIgnoreRecipientClick = currentUiState.isLoadingConversationParticipants ||
            isResolvingConversation() ||
            sanitized.destination in currentUiState.existingParticipantCanonicalDestinations

        if (shouldIgnoreRecipientClick) {
            return
        }

        val outcome = selectedRecipientsDelegate.toggle(
            recipient = sanitized,
            canAdd = { true },
        )

        if (outcome is RecipientToggleOutcome.Added) {
            recipientPickerDelegate.clearQuery()
        }
    }

    override fun onConfirmClick() {
        val currentUiState = localUiState.value
        val selectedRecipients = selectedRecipientsDelegate.state.value

        val shouldIgnoreConfirmClick = currentUiState.isLoadingConversationParticipants ||
            isResolvingConversation() ||
            selectedRecipients.isEmpty()

        if (shouldIgnoreConfirmClick) {
            return
        }

        val existingDestinations = currentUiState.existingParticipants.map { it.destination }
        val selectedDestinations = selectedRecipients.map { it.destination }
        val allDestinations = (existingDestinations + selectedDestinations).distinct()

        if (isConversationRecipientLimitExceeded(participantCount = allDestinations.size)) {
            showMessage(messageResId = R.string.too_many_participants)
            return
        }

        conversationResolutionDelegate.resolve(destinations = allDestinations)
    }

    private fun buildUiState(
        localState: LocalAddParticipantsUiState,
        recipientPickerUiState: RecipientPickerUiState,
        selectedRecipients: ImmutableList<SelectedRecipient>,
        resolutionState: ConversationResolutionState,
    ): AddParticipantsUiState {
        return AddParticipantsUiState(
            existingParticipants = localState.existingParticipants,
            isLoadingConversationParticipants = localState.isLoadingConversationParticipants,
            isResolvingConversation = resolutionState is ConversationResolutionState.Resolving,
            recipientPickerUiState = recipientPickerUiState,
            selectedRecipients = selectedRecipients,
        )
    }

    private fun observeResolutionOutcomes() {
        viewModelScope.launch(mainDispatcher) {
            conversationResolutionDelegate
                .outcomes
                .collect { outcome ->
                    when (outcome) {
                        is ConversationResolutionOutcome.Resolved -> {
                            selectedRecipientsDelegate.clear()
                            sendEffect(
                                effect = AddParticipantsEffect.NavigateToConversation(
                                    conversationId = outcome.conversationId,
                                ),
                            )
                        }

                        ConversationResolutionOutcome.Failed -> {
                            showMessage(messageResId = R.string.conversation_creation_failure)
                        }
                    }
                }
        }
    }

    private fun isResolvingConversation(): Boolean {
        return conversationResolutionDelegate.state.value is ConversationResolutionState.Resolving
    }

    private fun showMessage(messageResId: Int) {
        sendEffect(
            effect = AddParticipantsEffect.ShowMessage(
                messageResId = messageResId,
            ),
        )
    }

    private fun sendEffect(effect: AddParticipantsEffect) {
        viewModelScope.launch(mainDispatcher) {
            effectsChannel.send(element = effect)
        }
    }

    private fun persistentEmptyParticipants(): PersistentList<ConversationRecipient> {
        return persistentListOf()
    }

    private data class LocalAddParticipantsUiState(
        val existingParticipants: ImmutableList<ConversationRecipient> = persistentListOf(),
        val existingParticipantCanonicalDestinations: ImmutableSet<String> = persistentSetOf(),
        val isLoadingConversationParticipants: Boolean = true,
    )

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
