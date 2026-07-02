package com.android.messaging.ui.conversation.metadata.delegate

import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.blockedparticipants.usecase.SetDestinationBlocked
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal interface ConversationMetadataDelegate :
    ConversationScreenDelegate<ConversationMetadataUiState> {
    val effects: Flow<ConversationScreenEffect>
    val isDeleteConversationConfirmationVisible: StateFlow<Boolean>

    fun onArchiveConversationClick()
    fun onUnarchiveConversationClick()
    fun onUnblockConversationClick()
    fun onAddContactClick()
    fun onDeleteConversationClick()
    fun confirmDeleteConversation()
    fun dismissDeleteConversationConfirmation()
}

internal class ConversationMetadataDelegateImpl @Inject constructor(
    private val checkConversationActionRequirements: CheckConversationActionRequirements,
    private val conversationsRepository: ConversationsRepository,
    private val conversationMetadataUiStateMapper: ConversationMetadataUiStateMapper,
    private val setDestinationBlocked: SetDestinationBlocked,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationMetadataDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val _state = MutableStateFlow<ConversationMetadataUiState>(
        value = ConversationMetadataUiState.Loading,
    )
    private val _isDeleteConversationConfirmationVisible = MutableStateFlow(value = false)

    override val effects = _effects.asSharedFlow()
    override val state = _state.asStateFlow()
    override val isDeleteConversationConfirmationVisible =
        _isDeleteConversationConfirmationVisible.asStateFlow()

    private var boundScope: CoroutineScope? = null
    private var boundConversationIdFlow: StateFlow<String?>? = null
    private var latestMetadata: ConversationMetadata? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope
        boundConversationIdFlow = conversationIdFlow

        scope.launch(defaultDispatcher) {
            conversationIdFlow.collectLatest { conversationId ->
                _state.value = ConversationMetadataUiState.Loading
                _isDeleteConversationConfirmationVisible.value = false
                latestMetadata = null

                if (conversationId == null) {
                    return@collectLatest
                }

                conversationsRepository
                    .getConversationMetadata(conversationId = conversationId)
                    .onEach { metadata -> latestMetadata = metadata }
                    .map { metadata ->
                        when {
                            metadata != null -> {
                                conversationMetadataUiStateMapper.map(metadata = metadata)
                            }
                            else -> ConversationMetadataUiState.Unavailable
                        }
                    }
                    .flowOn(defaultDispatcher)
                    .collect { currentMetadataState ->
                        _state.value = currentMetadataState
                    }
            }
        }
    }

    override fun onArchiveConversationClick() {
        val conversationId = currentConversationId ?: return

        boundScope?.launch(defaultDispatcher) {
            conversationsRepository.archiveConversation(conversationId = conversationId)
            _effects.emit(ConversationScreenEffect.CloseConversation)
        }
    }

    override fun onUnarchiveConversationClick() {
        val conversationId = currentConversationId ?: return

        boundScope?.launch(defaultDispatcher) {
            conversationsRepository.unarchiveConversation(conversationId = conversationId)
        }
    }

    override fun onUnblockConversationClick() {
        val conversationId = currentConversationId ?: return
        val normalizedDestination = latestMetadata
            ?.otherParticipantNormalizedDestination
            ?.takeIf { it.isNotBlank() }
            ?: return

        setDestinationBlocked(
            normalizedDestination = normalizedDestination,
            blocked = false,
            conversationId = conversationId,
        )
    }

    override fun onAddContactClick() {
        val destination = (_state.value as? ConversationMetadataUiState.Present)
            ?.otherParticipantPhoneNumber
            ?.takeIf { it.isNotBlank() }
            ?: return

        boundScope?.launch(defaultDispatcher) {
            _effects.emit(
                ConversationScreenEffect.LaunchAddContactFlow(destination = destination),
            )
        }
    }

    override fun onDeleteConversationClick() {
        if (currentConversationId == null) {
            return
        }

        when (checkConversationActionRequirements()) {
            ConversationActionRequirementsResult.Ready -> {
                _isDeleteConversationConfirmationVisible.value = true
            }

            ConversationActionRequirementsResult.SmsNotCapable -> {
                emitEffect(
                    effect = ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.sms_disabled,
                    ),
                )
            }

            ConversationActionRequirementsResult.NoPreferredSmsSim -> {
                emitEffect(
                    effect = ConversationScreenEffect.ShowMessage(
                        messageResId = R.string.no_preferred_sim_selected,
                    ),
                )
            }

            ConversationActionRequirementsResult.MissingDefaultSmsRole -> {
                emitEffect(
                    effect = ConversationScreenEffect.RequestDefaultSmsRole(
                        isSending = false,
                    ),
                )
            }
        }
    }

    private fun emitEffect(effect: ConversationScreenEffect) {
        boundScope?.launch(defaultDispatcher) {
            _effects.emit(effect)
        }
    }

    override fun confirmDeleteConversation() {
        val conversationId = currentConversationId ?: return
        val cutoffTimestamp = latestMetadata?.sortTimestamp ?: System.currentTimeMillis()

        _isDeleteConversationConfirmationVisible.value = false

        boundScope?.launch(defaultDispatcher) {
            conversationsRepository.deleteConversation(
                conversationId = conversationId,
                cutoffTimestamp = cutoffTimestamp,
            )
            _effects.emit(ConversationScreenEffect.CloseConversation)
        }
    }

    override fun dismissDeleteConversationConfirmation() {
        _isDeleteConversationConfirmationVisible.value = false
    }

    private val currentConversationId: String?
        get() {
            return boundConversationIdFlow
                ?.value
                ?.takeIf { it.isNotBlank() }
        }
}
