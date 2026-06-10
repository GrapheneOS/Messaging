package com.android.messaging.ui.conversation.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.data.subscription.resolveSelectedSubscription
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.entry.model.NewChatEffect
import com.android.messaging.ui.conversation.entry.model.NewChatUiState
import com.android.messaging.ui.conversation.recipientpicker.delegate.ConversationResolutionDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerUiState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.picker.sanitizedOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface NewChatScreenModel {
    val effects: Flow<NewChatEffect>
    val uiState: StateFlow<NewChatUiState>

    fun onContactClicked(destination: String)
    fun onContactLongClicked(recipient: SelectedRecipient)
    fun onCreateGroupConfirmed()
    fun onCreateGroupRecipientClicked(recipient: SelectedRecipient)
    fun onCreateGroupRequested()
    fun onLoadMore()
    fun onNavigateBack()
    fun onQueryChanged(query: String)
    fun onSimSelected(selfParticipantId: String)
}

@HiltViewModel
internal class NewChatViewModel @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded,
    private val recipientPickerDelegate: RecipientPickerDelegate,
    private val selectedRecipientsDelegate: SelectedRecipientsDelegate,
    private val conversationResolutionDelegate: ConversationResolutionDelegate,
    private val savedStateHandle: SavedStateHandle,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    NewChatScreenModel {

    private val effectsChannel = Channel<NewChatEffect>(capacity = Channel.BUFFERED)
    private val localUiState = MutableStateFlow(restoreLocalUiState())

    private var pendingSelfParticipantId: String? = null

    override val effects = effectsChannel.receiveAsFlow()

    override val uiState: StateFlow<NewChatUiState> = combine(
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
        observeActiveSubscriptions()
        observeResolutionOutcomes()
    }

    override fun onContactClicked(destination: String) {
        if (isResolvingConversation() || localUiState.value.isCreatingGroup) {
            return
        }

        startConversationResolution(
            destinations = listOf(destination),
            recipientDestination = destination,
        )
    }

    override fun onContactLongClicked(recipient: SelectedRecipient) {
        when {
            isResolvingConversation() -> Unit
            localUiState.value.isCreatingGroup -> {
                onCreateGroupRecipientClicked(recipient = recipient)
            }

            else -> {
                startGroupWithRecipient(recipient = recipient)
            }
        }
    }

    override fun onCreateGroupConfirmed() {
        if (!isCreatingGroupEditable()) {
            return
        }

        val destinations = selectedRecipientsDelegate.state.value.map { it.destination }

        if (destinations.isEmpty() || !isRecipientCountAccepted(count = destinations.size)) {
            showTooManyParticipantsMessage()
            return
        }

        startConversationResolution(
            destinations = destinations,
            recipientDestination = null,
        )
    }

    override fun onCreateGroupRecipientClicked(recipient: SelectedRecipient) {
        if (!isCreatingGroupEditable()) {
            return
        }

        val sanitized = recipient.sanitizedOrNull() ?: return

        val outcome = selectedRecipientsDelegate.toggle(
            recipient = sanitized,
            canAdd = ::isRecipientCountAccepted,
        )

        when (outcome) {
            RecipientToggleOutcome.Added -> {
                recipientPickerDelegate.clearQuery()
            }

            RecipientToggleOutcome.OverLimit -> {
                showTooManyParticipantsMessage()
            }

            RecipientToggleOutcome.Removed -> Unit
        }
    }

    override fun onCreateGroupRequested() {
        conversationResolutionDelegate.cancel()

        if (localUiState.value.isCreatingGroup) {
            return
        }

        updateLocalUiState(
            localUiState.value.copy(isCreatingGroup = true),
        )

        selectedRecipientsDelegate.clear()
    }

    override fun onLoadMore() {
        recipientPickerDelegate.onLoadMore()
    }

    override fun onNavigateBack() {
        if (localUiState.value.isCreatingGroup) {
            cancelCreateGroup()
            return
        }

        conversationResolutionDelegate.cancel()
        sendEffect(effect = NewChatEffect.NavigateBack)
    }

    override fun onQueryChanged(query: String) {
        recipientPickerDelegate.onQueryChanged(query = query)
    }

    override fun onSimSelected(selfParticipantId: String) {
        val currentSimState = localUiState.value.simSelectorState

        val selectedSubscription = currentSimState.subscriptions
            .firstOrNull { subscription ->
                subscription.selfParticipantId == selfParticipantId
            }
            ?: return

        updateLocalUiState(
            localUiState.value.copy(
                simSelectorState = currentSimState.copy(
                    selectedSubscription = selectedSubscription,
                ),
            ),
        )
    }

    private fun buildUiState(
        localState: NewChatLocalUiState,
        recipientPickerUiState: RecipientPickerUiState,
        selectedRecipients: ImmutableList<SelectedRecipient>,
        resolutionState: ConversationResolutionState,
    ): NewChatUiState {
        val resolving = resolutionState as? ConversationResolutionState.Resolving

        return NewChatUiState(
            isCreatingGroup = localState.isCreatingGroup,
            isResolvingConversation = resolving != null,
            isResolvingConversationIndicatorVisible = resolving?.isIndicatorVisible == true,
            recipientPickerUiState = recipientPickerUiState,
            resolvingRecipientDestination = resolving?.recipientDestination,
            selectedGroupRecipients = selectedRecipients,
            simSelectorState = localState.simSelectorState,
        )
    }

    private fun observeResolutionOutcomes() {
        viewModelScope.launch(mainDispatcher) {
            conversationResolutionDelegate.outcomes.collect { outcome ->
                when (outcome) {
                    is ConversationResolutionOutcome.Resolved -> {
                        onConversationResolved(conversationId = outcome.conversationId)
                    }

                    ConversationResolutionOutcome.Failed -> {
                        showMessage(messageResId = R.string.conversation_creation_failure)
                    }
                }
            }
        }
    }

    private fun onConversationResolved(conversationId: String) {
        val pendingSelf = pendingSelfParticipantId?.takeUnless { it.isBlank() }
        pendingSelfParticipantId = null

        updateLocalUiState(
            localUiState.value.copy(isCreatingGroup = false),
        )

        selectedRecipientsDelegate.clear()

        sendEffect(
            effect = NewChatEffect.NavigateToConversation(
                conversationId = conversationId,
                selfParticipantId = pendingSelf,
            ),
        )
    }

    private fun startConversationResolution(
        destinations: List<String>,
        recipientDestination: String?,
    ) {
        pendingSelfParticipantId = selectedSelfParticipantId()
        conversationResolutionDelegate.resolve(
            destinations = destinations,
            recipientDestination = recipientDestination,
        )
    }

    private fun startGroupWithRecipient(recipient: SelectedRecipient) {
        val sanitized = recipient.sanitizedOrNull() ?: return

        if (!isRecipientCountAccepted(count = 1)) {
            showTooManyParticipantsMessage()
            return
        }

        updateLocalUiState(
            localUiState.value.copy(isCreatingGroup = true),
        )

        selectedRecipientsDelegate.replaceWith(recipient = sanitized)
        recipientPickerDelegate.clearQuery()
    }

    private fun cancelCreateGroup() {
        conversationResolutionDelegate.cancel()

        val currentState = localUiState.value
        val hasGroupStateToClear = currentState.isCreatingGroup ||
            selectedRecipientsDelegate.state.value.isNotEmpty()

        if (!hasGroupStateToClear) {
            return
        }

        updateLocalUiState(
            currentState.copy(isCreatingGroup = false),
        )

        selectedRecipientsDelegate.clear()
    }

    private fun restoreLocalUiState(): NewChatLocalUiState {
        return NewChatLocalUiState(
            isCreatingGroup = savedStateHandle[IS_CREATING_GROUP_KEY] ?: false,
        )
    }

    private fun observeActiveSubscriptions() {
        viewModelScope.launch(mainDispatcher) {
            subscriptionsRepository
                .observeActiveSubscriptions()
                .collect(::reconcileSimSelection)
        }
    }

    private fun reconcileSimSelection(subscriptions: ImmutableList<Subscription>) {
        val persistedSelfParticipantId = savedStateHandle
            .get<String>(SIM_SELECTED_SELF_PARTICIPANT_ID_KEY)

        val resolvedSelection = resolveSimSelection(
            subscriptions = subscriptions,
            persistedSelfParticipantId = persistedSelfParticipantId,
        )

        updateLocalUiState(
            localUiState.value.copy(
                simSelectorState = ConversationSimSelectorUiState(
                    subscriptions = subscriptions,
                    selectedSubscription = resolvedSelection,
                ),
            ),
        )
    }

    private fun resolveSimSelection(
        subscriptions: ImmutableList<Subscription>,
        persistedSelfParticipantId: String?,
    ): Subscription? {
        return resolveSelectedSubscription(
            subscriptions = subscriptions,
            selectedSelfParticipantId = persistedSelfParticipantId,
            defaultSmsSubscriptionId = subscriptionsRepository.getDefaultSmsSubscriptionId(),
        )
    }

    private fun selectedSelfParticipantId(): String? {
        return localUiState.value.simSelectorState.selectedSubscription?.selfParticipantId
    }

    private fun isResolvingConversation(): Boolean {
        return conversationResolutionDelegate.state.value is ConversationResolutionState.Resolving
    }

    private fun isCreatingGroupEditable(): Boolean {
        return localUiState.value.isCreatingGroup && !isResolvingConversation()
    }

    private fun isRecipientCountAccepted(count: Int): Boolean {
        return !isConversationRecipientLimitExceeded(participantCount = count)
    }

    private fun showMessage(messageResId: Int) {
        sendEffect(effect = NewChatEffect.ShowMessage(messageResId = messageResId))
    }

    private fun showTooManyParticipantsMessage() {
        showMessage(messageResId = R.string.too_many_participants)
    }

    private fun sendEffect(effect: NewChatEffect) {
        viewModelScope.launch(mainDispatcher) {
            effectsChannel.send(element = effect)
        }
    }

    private fun updateLocalUiState(uiState: NewChatLocalUiState) {
        val previousUiState = localUiState.value
        localUiState.value = uiState
        persistRestorableUiState(
            previousUiState = previousUiState,
            uiState = uiState,
        )
    }

    private fun persistRestorableUiState(
        previousUiState: NewChatLocalUiState,
        uiState: NewChatLocalUiState,
    ) {
        if (previousUiState.isCreatingGroup != uiState.isCreatingGroup) {
            savedStateHandle[IS_CREATING_GROUP_KEY] = uiState.isCreatingGroup
        }

        val persistedSelfParticipantId = savedStateHandle
            .get<String>(SIM_SELECTED_SELF_PARTICIPANT_ID_KEY)

        val selectedSelfParticipantId = uiState
            .simSelectorState
            .selectedSubscription
            ?.selfParticipantId

        if (persistedSelfParticipantId != selectedSelfParticipantId) {
            savedStateHandle[SIM_SELECTED_SELF_PARTICIPANT_ID_KEY] = selectedSelfParticipantId
        }
    }

    private companion object {
        private const val IS_CREATING_GROUP_KEY = "is_creating_group"
        private const val SIM_SELECTED_SELF_PARTICIPANT_ID_KEY = "sim_selected_self_participant_id"
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private data class NewChatLocalUiState(
    val isCreatingGroup: Boolean = false,
    val simSelectorState: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
)
