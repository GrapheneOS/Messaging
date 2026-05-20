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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val conversationIdFlow = MutableStateFlow<String?>(rootConversationId)

    private var resolveConversationJob: Job? = null

    init {
        delegate.bind(
            conversationIdFlow = conversationIdFlow,
            scope = viewModelScope,
        )
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

            is Action.ParticipantPressed -> {
                resolveConversation(action.destination, shouldOpenChat = true)
            }

            is Action.ParticipantLongPressed -> {
                emitEffect(Effect.CopyToClipboard(action.details))
            }

            is Action.ParticipantActionPressed -> {
                resolveConversation(action.destination, shouldOpenChat = false)
            }

            is Action.SimSelected -> {
                delegate.setSelfParticipantId(action.selfParticipantId)
            }

            is Action.CallClicked -> {
                handleCallClicked()
            }

            is Action.ContactInfoClicked -> {
                handleContactInfoClicked()
            }
        }
    }

    private fun handleNotificationsClicked() {
        val state = uiState.value
        viewModelScope.launch {
            val legacyPrefs = delegate.getLegacyNotificationPrefs()
            _effects.emit(
                Effect.OpenNotificationChannelSettings(
                    conversationId = state.conversationId,
                    conversationTitle = state.conversationTitle,
                    legacyPrefs = legacyPrefs,
                ),
            )
        }
    }

    private fun handleCallClicked() {
        val participant = uiState.value.otherParticipant ?: return
        val phoneNumber = participant.normalizedDestination
            ?.takeIf { it.isNotBlank() }
            ?: return

        emitEffect(Effect.PlacePhoneCall(phoneNumber))
    }

    private fun handleContactInfoClicked() {
        val participant = uiState.value.otherParticipant ?: return

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
        if (conversationId == conversationIdFlow.value) return

        conversationIdFlow.value = conversationId
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
