package com.android.messaging.ui.conversation.v2.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryEffect
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal interface ConversationEntryModel {
    val effects: Flow<ConversationEntryEffect>
    val uiState: StateFlow<ConversationEntryUiState>

    fun onLaunchRequest(launchRequest: ConversationEntryLaunchRequest)
    fun onDraftPayloadConsumed(conversationId: String)
    fun onStartupAttachmentConsumed(conversationId: String)
    fun navigateBack()
    fun navigateToConversation(conversationId: String)
    fun showMessage(messageResId: Int)
}

@HiltViewModel
internal class ConversationEntryViewModel @Inject constructor(
    private val conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), ConversationEntryModel {

    private val _effects = MutableSharedFlow<ConversationEntryEffect>(
        extraBufferCapacity = 1,
    )
    private val _uiState = MutableStateFlow(
        value = restoreUiState(),
    )

    override val effects = _effects.asSharedFlow()
    override val uiState = _uiState.asStateFlow()

    override fun onLaunchRequest(launchRequest: ConversationEntryLaunchRequest) {
        val processedLaunchGeneration = savedStateHandle.get<Int>(
            PROCESSED_LAUNCH_GENERATION_KEY,
        )

        if (processedLaunchGeneration == launchRequest.launchGeneration) {
            return
        }

        updateUiState(
            ConversationEntryUiState(
                launchGeneration = launchRequest.launchGeneration,
                conversationId = launchRequest.conversationId,
                pendingDraft = launchRequest.draftData?.let { messageData ->
                    conversationMessageDataDraftMapper.map(messageData = messageData)
                },
                pendingStartupAttachment = launchRequest.toStartupAttachmentOrNull(),
            ),
        )
        savedStateHandle[PENDING_DRAFT_DATA_KEY] = launchRequest.draftData
        savedStateHandle[PROCESSED_LAUNCH_GENERATION_KEY] = launchRequest.launchGeneration
    }

    override fun onDraftPayloadConsumed(conversationId: String) {
        val currentUiState = _uiState.value

        if (currentUiState.conversationId == conversationId &&
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

    override fun onStartupAttachmentConsumed(conversationId: String) {
        val currentUiState = _uiState.value

        if (currentUiState.conversationId == conversationId &&
            currentUiState.pendingStartupAttachment != null
        ) {
            updateUiState(
                currentUiState.copy(
                    pendingStartupAttachment = null,
                ),
            )
        }
    }

    override fun navigateBack() {
        _effects.tryEmit(ConversationEntryEffect.NavigateBack)
    }

    override fun navigateToConversation(conversationId: String) {
        _effects.tryEmit(
            ConversationEntryEffect.NavigateToConversation(
                conversationId = conversationId,
            ),
        )
    }

    override fun showMessage(messageResId: Int) {
        _effects.tryEmit(
            ConversationEntryEffect.ShowMessage(
                messageResId = messageResId,
            ),
        )
    }

    private fun restoreUiState(): ConversationEntryUiState {
        val pendingDraftData = savedStateHandle.get<MessageData>(
            PENDING_DRAFT_DATA_KEY,
        )
        val startupAttachmentUri = savedStateHandle.get<String>(
            PENDING_STARTUP_ATTACHMENT_URI_KEY,
        )
        val startupAttachmentType = savedStateHandle.get<String>(
            PENDING_STARTUP_ATTACHMENT_TYPE_KEY,
        )

        return ConversationEntryUiState(
            launchGeneration = savedStateHandle[LAUNCH_GENERATION_KEY],
            conversationId = savedStateHandle[CONVERSATION_ID_KEY],
            pendingDraft = pendingDraftData?.let { messageData ->
                conversationMessageDataDraftMapper.map(messageData = messageData)
            },
            pendingStartupAttachment = when {
                startupAttachmentUri != null && startupAttachmentType != null -> {
                    ConversationEntryStartupAttachment(
                        contentType = startupAttachmentType,
                        contentUri = startupAttachmentUri,
                    )
                }

                else -> null
            },
        )
    }

    private fun updateUiState(uiState: ConversationEntryUiState) {
        _uiState.value = uiState

        savedStateHandle[LAUNCH_GENERATION_KEY] = uiState.launchGeneration
        savedStateHandle[CONVERSATION_ID_KEY] = uiState.conversationId
        savedStateHandle[PENDING_STARTUP_ATTACHMENT_TYPE_KEY] = uiState
            .pendingStartupAttachment
            ?.contentType

        savedStateHandle[PENDING_STARTUP_ATTACHMENT_URI_KEY] = uiState
            .pendingStartupAttachment
            ?.contentUri
    }

    private fun ConversationEntryLaunchRequest.toStartupAttachmentOrNull():
        ConversationEntryStartupAttachment? {
        return when {
            startupAttachmentUri != null && startupAttachmentType != null -> {
                ConversationEntryStartupAttachment(
                    contentType = startupAttachmentType,
                    contentUri = startupAttachmentUri,
                )
            }
            else -> null
        }
    }

    private companion object {
        private const val CONVERSATION_ID_KEY = "conversation_id"
        private const val LAUNCH_GENERATION_KEY = "launch_generation"
        private const val PENDING_DRAFT_DATA_KEY = "pending_draft_data"
        private const val PENDING_STARTUP_ATTACHMENT_TYPE_KEY = "pending_startup_attachment_type"
        private const val PENDING_STARTUP_ATTACHMENT_URI_KEY = "pending_startup_attachment_uri"
        private const val PROCESSED_LAUNCH_GENERATION_KEY = "processed_launch_generation"
    }
}
