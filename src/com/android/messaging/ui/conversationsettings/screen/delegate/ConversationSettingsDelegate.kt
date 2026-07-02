package com.android.messaging.ui.conversationsettings.screen.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepository
import com.android.messaging.data.conversationsettings.repository.ConversationSettingsRepository
import com.android.messaging.data.subscription.repository.ConversationSimSelectionRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.domain.conversationsettings.usecase.SetConversationSelfParticipantId
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsScreenDelegate
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapper
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal interface ConversationSettingsDelegate :
    ConversationSettingsScreenDelegate<ConversationSettingsUiState> {
    fun setDestinationBlocked(blocked: Boolean)
    fun setArchived(archived: Boolean)
    fun setSelfParticipantId(selfParticipantId: String)
    fun snooze(option: SnoozeOption)
    fun unsnooze()
}

internal class ConversationSettingsDelegateImpl @Inject constructor(
    private val repository: ConversationSettingsRepository,
    private val notificationRepository: ConversationNotificationRepository,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val simSelectionRepository: ConversationSimSelectionRepository,
    private val mapper: ConversationSettingsUiStateMapper,
    private val conversationsRepository: ConversationsRepository,
    private val blockedParticipantsRepository: BlockedParticipantsRepository,
    private val setConversationSelfParticipantId: SetConversationSelfParticipantId,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ConversationSettingsDelegate {

    private val _state = MutableStateFlow(ConversationSettingsUiState())
    override val state: StateFlow<ConversationSettingsUiState> = _state.asStateFlow()

    private val refreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val conversationIdFlow = MutableStateFlow<String?>(null)
    private var isBound = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        conversationIdFlow
            .filterNotNull()
            .flatMapLatest(::observeUiState)
            .onEach { _state.value = it }
            .launchIn(scope)
    }

    override fun setConversationId(conversationId: String) {
        if (conversationIdFlow.value == conversationId) return
        conversationIdFlow.value = conversationId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUiState(id: String): Flow<ConversationSettingsUiState> {
        val settings = refreshTriggers
            .onStart { emit(Unit) }
            .flatMapLatest { repository.getConversationSettings(id) }

        return combine(
            settings,
            subscriptionsRepository.observeActiveSubscriptions(),
            simSelectionRepository.observe(id),
        ) { data, subscriptions, selfIdOverride ->
            mapper.map(
                data = data,
                subscriptions = subscriptions,
                selfIdOverride = selfIdOverride,
            )
        }
    }

    override fun refresh() {
        ParticipantRefresh.refreshParticipantsIfNeeded()
        refreshTriggers.tryEmit(Unit)
    }

    override fun setDestinationBlocked(blocked: Boolean) {
        val normalizedDestination = _state.value.otherParticipant?.normalizedDestination
        val conversationId = currentConversationId()

        if (normalizedDestination == null || conversationId == null) return

        applicationScope.launch {
            blockedParticipantsRepository.setDestinationBlocked(
                destination = normalizedDestination,
                conversationId = conversationId,
                isBlocked = blocked,
            )
        }
    }

    override fun setArchived(archived: Boolean) {
        val conversationId = currentConversationId() ?: return

        applicationScope.launch {
            when {
                archived -> conversationsRepository.archiveConversation(conversationId)
                else -> conversationsRepository.unarchiveConversation(conversationId)
            }
        }
    }

    override fun setSelfParticipantId(selfParticipantId: String) {
        if (_state.value.selfParticipantId == selfParticipantId) return
        val conversationId = currentConversationId() ?: return

        applicationScope.launch {
            setConversationSelfParticipantId(
                conversationId = conversationId,
                selfParticipantId = selfParticipantId,
            )
        }
    }

    override fun snooze(option: SnoozeOption) {
        val conversationId = currentConversationId() ?: return
        notificationRepository.snooze(conversationId, option)
        refresh()
    }

    override fun unsnooze() {
        val conversationId = currentConversationId() ?: return
        notificationRepository.clearSnooze(conversationId)
        refresh()
    }

    private fun currentConversationId(): String? {
        return conversationIdFlow.value?.takeIf(String::isNotBlank)
    }
}
