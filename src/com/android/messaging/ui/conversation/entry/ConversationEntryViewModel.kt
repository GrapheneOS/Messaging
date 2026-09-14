package com.android.messaging.ui.conversation.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.entry.model.ConversationEntryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface ConversationEntryScreenModel {
    val uiState: StateFlow<ConversationEntryUiState>

    fun onConversationNavigationRequested(
        conversationId: ConversationId,
        pendingSelfParticipantId: ParticipantId?,
    )

    fun onDraftPayloadConsumed(conversationId: ConversationId)

    fun onScrollPositionConsumed(conversationId: ConversationId)

    fun onPendingSelfParticipantIdConsumed(conversationId: ConversationId)

    fun onStartupAttachmentConsumed(conversationId: ConversationId)
}

@HiltViewModel
internal class ConversationEntryViewModel @Inject constructor(
    private val conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper,
    private val savedStateHandle: SavedStateHandle,
    launchStore: ConversationLaunchStore,
) : ViewModel(),
    ConversationEntryScreenModel {

    private val _uiState = MutableStateFlow(restoreUiState())
    override val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launchStore.requests.collect(::onLaunchRequest)
        }
    }

    override fun onConversationNavigationRequested(
        conversationId: ConversationId,
        pendingSelfParticipantId: ParticipantId?,
    ) {
        updateUiState(
            _uiState.value.copy(
                conversationId = conversationId,
                pendingSelfParticipantId = pendingSelfParticipantId,
            ),
        )
    }

    private fun onLaunchRequest(launchRequest: ConversationEntryLaunchRequest) {
        updateUiState(
            ConversationEntryUiState(
                conversationId = launchRequest.conversationId,
                pendingDraft = launchRequest.draftData?.let { messageData ->
                    conversationMessageDataDraftMapper.map(messageData = messageData)
                },
                pendingScrollPosition = launchRequest.messagePosition,
                pendingStartupAttachment = buildStartupAttachmentOrNull(
                    contentUri = launchRequest.startupAttachmentUri,
                    contentType = launchRequest.startupAttachmentType,
                ),
            ),
        )
        savedStateHandle[PENDING_DRAFT_DATA_KEY] = launchRequest.draftData
        savedStateHandle[PENDING_SCROLL_POSITION_KEY] = launchRequest.messagePosition
    }

    override fun onDraftPayloadConsumed(conversationId: ConversationId) {
        val currentUiState = _uiState.value

        if (
            currentUiState.conversationId == conversationId &&
            currentUiState.pendingDraft != null
        ) {
            updateUiState(
                currentUiState.copy(
                    pendingDraft = null,
                ),
            )

            savedStateHandle[PENDING_DRAFT_DATA_KEY] = null
        }
    }

    override fun onScrollPositionConsumed(conversationId: ConversationId) {
        val currentUiState = _uiState.value

        val hasPendingScrollPosition = currentUiState.pendingScrollPosition != null

        if (currentUiState.conversationId == conversationId && hasPendingScrollPosition) {
            updateUiState(
                currentUiState.copy(
                    pendingScrollPosition = null,
                ),
            )

            savedStateHandle[PENDING_SCROLL_POSITION_KEY] = null
        }
    }

    override fun onStartupAttachmentConsumed(conversationId: ConversationId) {
        val currentUiState = _uiState.value

        val hasPendingStartupAttachment = currentUiState.pendingStartupAttachment != null

        if (currentUiState.conversationId == conversationId && hasPendingStartupAttachment) {
            updateUiState(
                currentUiState.copy(
                    pendingStartupAttachment = null,
                ),
            )
        }
    }

    override fun onPendingSelfParticipantIdConsumed(conversationId: ConversationId) {
        val currentUiState = _uiState.value

        val hasPendingSelfParticipantId = currentUiState.pendingSelfParticipantId != null

        if (currentUiState.conversationId == conversationId && hasPendingSelfParticipantId) {
            updateUiState(
                currentUiState.copy(
                    pendingSelfParticipantId = null,
                ),
            )
        }
    }

    private fun restoreUiState(): ConversationEntryUiState {
        val pendingDraftData = savedStateHandle.get<MessageData>(PENDING_DRAFT_DATA_KEY)
        val startupAttachmentUri = savedStateHandle.get<String>(
            PENDING_STARTUP_ATTACHMENT_URI_KEY,
        )
        val startupAttachmentType = savedStateHandle.get<String>(
            PENDING_STARTUP_ATTACHMENT_TYPE_KEY,
        )

        return ConversationEntryUiState(
            conversationId = ConversationId.fromOrNull(savedStateHandle[CONVERSATION_ID_KEY]),
            pendingDraft = pendingDraftData?.let(conversationMessageDataDraftMapper::map),
            pendingScrollPosition = savedStateHandle[PENDING_SCROLL_POSITION_KEY],
            pendingSelfParticipantId = ParticipantId.fromOrNull(
                savedStateHandle[PENDING_SELF_PARTICIPANT_ID_KEY],
            ),
            pendingStartupAttachment = buildStartupAttachmentOrNull(
                contentUri = startupAttachmentUri,
                contentType = startupAttachmentType,
            ),
        )
    }

    private fun updateUiState(uiState: ConversationEntryUiState) {
        val previousUiState = _uiState.value

        _uiState.value = uiState

        persistRestorableUiState(
            previousUiState = previousUiState,
            uiState = uiState,
        )
    }

    private fun persistRestorableUiState(
        previousUiState: ConversationEntryUiState,
        uiState: ConversationEntryUiState,
    ) {
        if (previousUiState.conversationId != uiState.conversationId) {
            savedStateHandle[CONVERSATION_ID_KEY] = uiState.conversationId?.value
        }

        if (previousUiState.pendingSelfParticipantId != uiState.pendingSelfParticipantId) {
            savedStateHandle[PENDING_SELF_PARTICIPANT_ID_KEY] =
                uiState.pendingSelfParticipantId?.value
        }

        if (
            previousUiState.pendingStartupAttachment?.contentType !=
            uiState.pendingStartupAttachment?.contentType
        ) {
            savedStateHandle[PENDING_STARTUP_ATTACHMENT_TYPE_KEY] = uiState
                .pendingStartupAttachment
                ?.contentType
        }

        if (
            previousUiState.pendingStartupAttachment?.contentUri !=
            uiState.pendingStartupAttachment?.contentUri
        ) {
            savedStateHandle[PENDING_STARTUP_ATTACHMENT_URI_KEY] = uiState
                .pendingStartupAttachment
                ?.contentUri
        }
    }

    private fun buildStartupAttachmentOrNull(
        contentUri: String?,
        contentType: String?,
    ): ConversationEntryStartupAttachment? {
        return when {
            contentUri == null || contentType == null -> null

            else -> {
                ConversationEntryStartupAttachment(
                    contentType = contentType,
                    contentUri = contentUri,
                )
            }
        }
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val PENDING_DRAFT_DATA_KEY = "pending_draft_data"
        private const val PENDING_SCROLL_POSITION_KEY = "pending_scroll_position"
        private const val PENDING_SELF_PARTICIPANT_ID_KEY = "pending_self_participant_id"
        private const val PENDING_STARTUP_ATTACHMENT_TYPE_KEY = "pending_startup_attachment_type"
        private const val PENDING_STARTUP_ATTACHMENT_URI_KEY = "pending_startup_attachment_uri"
    }
}
