package com.android.messaging.ui.conversationsettings.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.conversation.repository.ConversationNotificationRepository
import com.android.messaging.di.core.MainDispatcher
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
import kotlinx.coroutines.CoroutineDispatcher
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
    private val notificationRepository: ConversationNotificationRepository,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
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
    private var currentConversationId: String = ""

    init {
        setConversationId(rootConversationId)
        delegate.bind(scope = viewModelScope)
    }

    override fun refreshState() {
        delegate.refresh()
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.NotificationsClicked -> {
                val state = uiState.value
                val legacyPrefs = notificationRepository.getLegacyNotificationPrefs(
                    conversationId = state.conversationId,
                )

                emitEffect(
                    Effect.OpenNotificationChannelSettings(
                        conversationId = state.conversationId,
                        conversationTitle = state.conversationTitle,
                        legacyPrefs = legacyPrefs,
                    ),
                )
            }

            is Action.SnoozeOptionSelected -> {
                notificationRepository.snooze(
                    conversationId = uiState.value.conversationId,
                    option = action.option,
                )
                delegate.refresh()
            }

            is Action.UnsnoozeClicked -> {
                notificationRepository.clearSnooze(uiState.value.conversationId)
                delegate.refresh()
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
                emitNavigationEvent(NavEvent.CloseAfterBlock)
            }

            is Action.ParticipantPressed -> {
                resolveConversation(
                    destination = action.destination,
                    shouldOpenChat = true,
                )
            }

            is Action.ParticipantLongPressed -> {
                emitEffect(Effect.CopyToClipboard(action.details))
            }

            is Action.ParticipantActionPressed -> {
                resolveConversation(
                    destination = action.destination,
                    shouldOpenChat = false,
                )
            }
        }
    }

    override fun setConversationId(conversationId: String) {
        if (conversationId == currentConversationId) return

        currentConversationId = conversationId
        delegate.setConversationId(conversationId)
        delegate.refresh()
    }

    private fun resolveConversation(
        destination: String,
        shouldOpenChat: Boolean,
    ) {
        resolveConversationJob?.cancel()
        resolveConversationJob = viewModelScope.launch(mainDispatcher) {
            try {
                val result = resolveConversationId.invoke(listOf(destination))
                handleResolveConversationIdResult(result, shouldOpenChat)
            } finally {
                resolveConversationJob = null
            }
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
