package com.android.messaging.ui.conversationsettings.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegate
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavEvent as NavEvent
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantConversationSettingsAction as ParticipantAction
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal interface ConversationSettingsScreenModel {
    val effects: Flow<Effect>
    val navigationEvents: Flow<NavEvent>
    val uiState: StateFlow<State>
    val rootConversationId: String

    fun refreshState()
    fun onAction(action: Action)

    fun setConversationId(conversationId: String)
}

@HiltViewModel
internal class ConversationSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val delegate: ConversationSettingsDelegate,
    private val resolveConversationId: ResolveConversationId,
) : ViewModel(),
    ConversationSettingsScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    private val _navigationEvents = MutableSharedFlow<NavEvent>(extraBufferCapacity = 1)
    override val navigationEvents: Flow<NavEvent> = _navigationEvents.asSharedFlow()

    override val uiState: StateFlow<State> = delegate.state

    override val rootConversationId: String = requireNotNull(
        savedStateHandle[UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID],
    ) { "conversationId is required" }

    private var resolveConversationJob: Job? = null

    init {
        delegate.setConversationId(rootConversationId)
        delegate.bind(viewModelScope)
    }

    override fun refreshState() {
        delegate.refresh()
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.NotificationsClicked -> {
                handleNotificationsClicked()
            }

            is Action.SnoozeOptionSelected -> {
                delegate.snooze(action.option)
            }

            is Action.UnsnoozeClicked -> {
                delegate.unsnooze()
            }

            is Action.UnarchiveClicked -> {
                delegate.setArchived(false)
            }

            is Action.ArchiveClicked -> {
                delegate.setArchived(true)
                emitNavigationEvent(NavEvent.CloseAfterArchive)
            }

            is Action.UnblockClicked -> {
                delegate.setDestinationBlocked(false)
            }

            is Action.BlockConfirmed -> {
                delegate.setDestinationBlocked(true)
            }

            is Action.SimSelected -> {
                delegate.setSelfParticipantId(action.selfParticipantId)
            }

            is ParticipantAction -> {
                handleParticipantAction(action)
            }
        }
    }

    private fun handleParticipantAction(action: ParticipantAction) {
        when (action) {
            is ParticipantAction.ParticipantPressed -> {
                resolveConversation(
                    action.destination,
                    shouldOpenChat = true,
                )
            }

            is ParticipantAction.ParticipantLongPressed -> {
                emitEffect(Effect.CopyToClipboard(action.details))
            }

            is ParticipantAction.ParticipantActionPressed -> {
                resolveConversation(
                    action.destination,
                    shouldOpenChat = false,
                )
            }

            is ParticipantAction.ParticipantCallClicked -> {
                emitEffect(Effect.PlacePhoneCall(action.destination))
            }

            is ParticipantAction.ParticipantContactInfoClicked -> {
                showOrAddContact(action.participant)
            }
        }
    }

    private fun handleNotificationsClicked() {
        val state = uiState.value
        emitEffect(
            Effect.OpenNotificationChannelSettings(
                conversationId = state.conversationId,
                conversationTitle = state.conversationTitle,
            ),
        )
    }

    private fun showOrAddContact(participant: ParticipantUiState) {
        emitEffect(
            Effect.ShowOrAddContact(
                contactId = participant.contactId,
                contactLookupKey = participant.lookupKey,
                avatarUri = participant.avatarUri,
                normalizedDestination = participant.normalizedDestination,
            ),
        )
    }

    override fun setConversationId(conversationId: String) {
        delegate.setConversationId(conversationId)
    }

    private fun resolveConversation(
        destination: String,
        shouldOpenChat: Boolean,
    ) {
        resolveConversationJob?.cancel()
        resolveConversationJob = viewModelScope.launch {
            val result = resolveConversationId.invoke(listOf(destination))
            handleResolveConversationIdResult(result, shouldOpenChat)
        }
    }

    private fun handleResolveConversationIdResult(
        result: ResolveConversationIdResult,
        shouldOpenChat: Boolean,
    ) {
        when (result) {
            is ResolveConversationIdResult.Resolved -> {
                if (shouldOpenChat) {
                    emitEffect(Effect.OpenParticipantChat(result.conversationId))
                } else {
                    emitNavigationEvent(NavEvent.OpenParticipantInfo(result.conversationId))
                }
            }

            ResolveConversationIdResult.EmptyDestinations,
            ResolveConversationIdResult.NotResolved,
            -> {
                emitEffect(Effect.ShowMessage(R.string.conversation_creation_failure))
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    private fun emitNavigationEvent(event: NavEvent) {
        viewModelScope.launch {
            _navigationEvents.emit(event)
        }
    }
}
