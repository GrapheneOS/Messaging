package com.android.messaging.ui.conversationsettings.screen.delegate

import com.android.messaging.data.conversationsettings.repository.ConversationSettingsRepository
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.domain.conversationsettings.usecase.SetConversationArchived
import com.android.messaging.domain.conversationsettings.usecase.SetConversationDestinationBlocked
import com.android.messaging.domain.conversationsettings.usecase.SetConversationSelfParticipantId
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsScreenDelegate
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapper
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface ConversationSettingsDelegate :
    ConversationSettingsScreenDelegate<ConversationSettingsUiState> {
    fun setConversationId(conversationId: String)
    fun setDestinationBlocked(blocked: Boolean)
    fun setArchived(archived: Boolean)
    fun setSelfParticipantId(selfParticipantId: String)
}

internal class ConversationSettingsDelegateImpl @Inject constructor(
    private val repository: ConversationSettingsRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val simSelectionRepository: ConversationSimSelectionRepository,
    private val mapper: ConversationSettingsUiStateMapper,
    private val setConversationArchived: SetConversationArchived,
    private val setConversationDestinationBlocked: SetConversationDestinationBlocked,
    private val setConversationSelfParticipantId: SetConversationSelfParticipantId,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ConversationSettingsDelegate {

    private val _state = MutableStateFlow(ConversationSettingsUiState())
    override val state: StateFlow<ConversationSettingsUiState> = _state.asStateFlow()

    private val refreshTriggers: Channel<Unit> = Channel(Channel.CONFLATED)
    private val conversationId = MutableStateFlow("")

    private var isBound = false

    override fun setConversationId(conversationId: String) {
        this.conversationId.value = conversationId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        conversationId
            .flatMapLatest(::observeUiState)
            .onEach { _state.value = it }
            .launchIn(scope)
    }

    private fun observeUiState(id: String): Flow<ConversationSettingsUiState> {
        val triggers = merge(
            repository.observeConversationChanges(id),
            refreshTriggers.receiveAsFlow(),
        ).onStart { emit(Unit) }

        return combine(
            triggers,
            subscriptionsRepository.observeActiveSubscriptions(),
            simSelectionRepository.observe(id),
        ) { _, subscriptions, storedOverride ->
            val data = repository.getConversationSettings(id)

            mapper.map(
                data = data,
                subscriptions = subscriptions,
                selfIdOverride = storedOverride,
            )
        }
    }

    override fun refresh() {
        ParticipantRefresh.refreshParticipantsIfNeeded()
        refreshTriggers.trySend(Unit)
    }

    override fun setDestinationBlocked(blocked: Boolean) {
        val participant = _state.value.otherParticipant ?: return
        val normalizedDestination = participant.normalizedDestination ?: return

        setConversationDestinationBlocked(
            conversationId = conversationId.value,
            normalizedDestination = normalizedDestination,
            blocked = blocked,
        )
    }

    override fun setArchived(archived: Boolean) {
        setConversationArchived(
            conversationId = conversationId.value,
            archived = archived,
        )
    }

    override fun setSelfParticipantId(selfParticipantId: String) {
        if (_state.value.selfParticipantId == selfParticipantId) return

        applicationScope.launch {
            setConversationSelfParticipantId(
                conversationId = conversationId.value,
                selfParticipantId = selfParticipantId,
            )
        }
    }
}
