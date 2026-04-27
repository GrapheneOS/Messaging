package com.android.messaging.ui.conversation.v2.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.conversation.mapper.ConversationMessageDataDraftMapper
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.IsConversationRecipientLimitExceeded
import com.android.messaging.domain.conversation.usecase.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.model.ResolveConversationIdResult
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryEffect
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.v2.entry.model.ConversationEntryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface ConversationEntryModel {
    val effects: Flow<ConversationEntryEffect>
    val uiState: StateFlow<ConversationEntryUiState>

    fun onCreateGroupRequested()
    fun onCreateGroupCanceled()
    fun onCreateGroupRecipientClicked(destination: String)
    fun onCreateGroupConfirmed()

    fun onLaunchRequest(launchRequest: ConversationEntryLaunchRequest)

    fun onNewChatRecipientLongPressed(destination: String)
    fun onNewChatRecipientSelected(destination: String)

    fun onDraftPayloadConsumed(conversationId: String)

    fun onScrollPositionConsumed(conversationId: String)

    fun onStartupAttachmentConsumed(conversationId: String)

    fun navigateBack()
    fun navigateToConversation(conversationId: String)

    fun showMessage(messageResId: Int)
}

internal const val RESOLVING_CONVERSATION_INDICATOR_DELAY_MILLIS = 200L

@HiltViewModel
internal class ConversationEntryViewModel @Inject constructor(
    private val conversationMessageDataDraftMapper: ConversationMessageDataDraftMapper,
    private val isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded,
    private val resolveConversationId: ResolveConversationId,
    private val savedStateHandle: SavedStateHandle,
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    ConversationEntryModel {

    private val _effects = MutableSharedFlow<ConversationEntryEffect>(
        extraBufferCapacity = 1,
    )
    private val _uiState = MutableStateFlow(
        value = restoreUiState(),
    )
    private var resolveConversationJob: Job? = null

    override val effects = _effects.asSharedFlow()
    override val uiState = _uiState.asStateFlow()

    override fun onCreateGroupRequested() {
        // Re-entering group creation should also abandon any in-flight resolution.
        cancelConversationResolution()
        val currentUiState = _uiState.value

        if (currentUiState.isCreatingGroup) {
            return
        }

        updateUiState(
            currentUiState.copy(
                isCreatingGroup = true,
                selectedGroupRecipientDestinations = persistentListOf(),
            ),
        )
    }

    override fun onCreateGroupCanceled() {
        cancelConversationResolution()
        val currentUiState = _uiState.value

        val hasGroupStateToClear = currentUiState.isCreatingGroup ||
            currentUiState.selectedGroupRecipientDestinations.isNotEmpty()

        if (!hasGroupStateToClear) {
            return
        }

        updateUiState(
            currentUiState.copy(
                isCreatingGroup = false,
                selectedGroupRecipientDestinations = persistentListOf(),
            ),
        )
    }

    override fun onCreateGroupRecipientClicked(destination: String) {
        val state = editableGroupStateOrNull() ?: return
        val current = state.selectedGroupRecipientDestinations
        val trimmed = destination.trim()

        val updatedDestinations = when {
            trimmed.isEmpty() -> {
                return
            }

            trimmed in current -> {
                current - trimmed
            }

            canAcceptRecipientCount(count = current.size + 1) -> {
                current + trimmed
            }

            else -> {
                return
            }
        }

        updateUiState(
            state.copy(
                selectedGroupRecipientDestinations = updatedDestinations.toImmutableList(),
            ),
        )
    }

    override fun onCreateGroupConfirmed() {
        val state = editableGroupStateOrNull() ?: return
        val destinations = state.selectedGroupRecipientDestinations

        val isSelectionValid = destinations.isNotEmpty() &&
            canAcceptRecipientCount(count = destinations.size)

        if (isSelectionValid) {
            resolveConversation(
                destinations = destinations,
                resolvingRecipientDestination = null,
            )
        }
    }

    override fun onNewChatRecipientLongPressed(destination: String) {
        val state = _uiState.value

        if (state.isResolvingConversation) {
            return
        }

        if (state.isCreatingGroup) {
            onCreateGroupRecipientClicked(destination = destination)
            return
        }

        val trimmed = destination.trim()
        val canSeedGroup = trimmed.isNotEmpty() && canAcceptRecipientCount(count = 1)

        if (canSeedGroup) {
            updateUiState(
                state.copy(
                    isCreatingGroup = true,
                    selectedGroupRecipientDestinations = persistentListOf(trimmed),
                ),
            )
        }
    }

    override fun onLaunchRequest(launchRequest: ConversationEntryLaunchRequest) {
        // Each new launch should supersede any in-flight resolution from the previous one.
        cancelConversationResolution()

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
                pendingScrollPosition = launchRequest.messagePosition,
                pendingStartupAttachment = buildStartupAttachmentOrNull(
                    contentUri = launchRequest.startupAttachmentUri,
                    contentType = launchRequest.startupAttachmentType,
                ),
            ),
        )
        savedStateHandle[PENDING_DRAFT_DATA_KEY] = launchRequest.draftData
        savedStateHandle[PENDING_SCROLL_POSITION_KEY] = launchRequest.messagePosition
        savedStateHandle[PROCESSED_LAUNCH_GENERATION_KEY] = launchRequest.launchGeneration
    }

    override fun onNewChatRecipientSelected(destination: String) {
        val currentUiState = _uiState.value

        if (currentUiState.isResolvingConversation || currentUiState.isCreatingGroup) {
            return
        }

        resolveConversation(
            destinations = listOf(destination),
            resolvingRecipientDestination = destination,
        )
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

    override fun onScrollPositionConsumed(conversationId: String) {
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

    override fun onStartupAttachmentConsumed(conversationId: String) {
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

    override fun navigateBack() {
        cancelConversationResolution()
        _effects.tryEmit(ConversationEntryEffect.NavigateBack)
    }

    override fun navigateToConversation(conversationId: String) {
        updateUiState(
            _uiState.value.copy(
                conversationId = conversationId,
                isCreatingGroup = false,
                isResolvingConversation = false,
                isResolvingConversationIndicatorVisible = false,
                resolvingRecipientDestination = null,
                selectedGroupRecipientDestinations = persistentListOf(),
            ),
        )

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
            isCreatingGroup = savedStateHandle[IS_CREATING_GROUP_KEY] ?: false,
            isResolvingConversation = false,
            isResolvingConversationIndicatorVisible = false,
            pendingDraft = pendingDraftData?.let { messageData ->
                conversationMessageDataDraftMapper.map(messageData = messageData)
            },
            pendingScrollPosition = savedStateHandle[PENDING_SCROLL_POSITION_KEY],
            pendingStartupAttachment = buildStartupAttachmentOrNull(
                contentUri = startupAttachmentUri,
                contentType = startupAttachmentType,
            ),
            resolvingRecipientDestination = null,
            selectedGroupRecipientDestinations = savedStateHandle
                .get<ArrayList<String>>(SELECTED_GROUP_RECIPIENT_DESTINATIONS_KEY)
                ?.toImmutableList()
                ?: persistentListOf(),
        )
    }

    private fun clearConversationResolutionState() {
        updateUiState(
            _uiState.value.copy(
                isResolvingConversation = false,
                isResolvingConversationIndicatorVisible = false,
                resolvingRecipientDestination = null,
            ),
        )
    }

    private fun editableGroupStateOrNull(): ConversationEntryUiState? {
        return _uiState.value.takeIf { state ->
            state.isCreatingGroup && !state.isResolvingConversation
        }
    }

    private fun canAcceptRecipientCount(count: Int): Boolean {
        if (isConversationRecipientLimitExceeded(count)) {
            showMessage(messageResId = R.string.too_many_participants)
            return false
        }

        return true
    }

    private fun cancelConversationResolution() {
        val currentResolveConversationJob = resolveConversationJob
        val currentUiState = _uiState.value

        resolveConversationJob = null
        currentResolveConversationJob?.cancel()

        val shouldClearConversationResolutionState = currentUiState.isResolvingConversation ||
            currentUiState.isResolvingConversationIndicatorVisible ||
            currentUiState.resolvingRecipientDestination != null

        if (shouldClearConversationResolutionState) {
            clearConversationResolutionState()
        }
    }

    private fun showConversationResolutionIndicator() {
        val currentUiState = _uiState.value

        val shouldShowIndicator = currentUiState.isResolvingConversation &&
            !currentUiState.isResolvingConversationIndicatorVisible

        if (shouldShowIndicator) {
            updateUiState(
                currentUiState.copy(
                    isResolvingConversationIndicatorVisible = true,
                ),
            )
        }
    }

    private fun startConversationResolution(resolvingRecipientDestination: String?) {
        updateUiState(
            _uiState.value.copy(
                isResolvingConversation = true,
                isResolvingConversationIndicatorVisible = false,
                resolvingRecipientDestination = resolvingRecipientDestination,
            ),
        )
    }

    private fun resolveConversation(
        destinations: List<String>,
        resolvingRecipientDestination: String?,
    ) {
        resolveConversationJob = viewModelScope.launch(mainDispatcher) {
            startConversationResolution(resolvingRecipientDestination)

            val showIndicatorJob = launchDelayedResolutionIndicator()

            try {
                resolveConversationId(destinations)
                    .let(::handleResolveConversationIdResult)
            } finally {
                showIndicatorJob.cancel()
                resolveConversationJob = null
            }
        }
    }

    private fun CoroutineScope.launchDelayedResolutionIndicator(): Job {
        return launch(mainDispatcher) {
            delay(timeMillis = RESOLVING_CONVERSATION_INDICATOR_DELAY_MILLIS)
            showConversationResolutionIndicator()
        }
    }

    private fun handleResolveConversationIdResult(result: ResolveConversationIdResult) {
        when (result) {
            is ResolveConversationIdResult.Resolved -> {
                navigateToConversation(conversationId = result.conversationId)
            }

            ResolveConversationIdResult.EmptyDestinations,
            ResolveConversationIdResult.NotResolved,
            -> {
                clearConversationResolutionState()
                showMessage(messageResId = R.string.conversation_creation_failure)
            }
        }
    }

    private fun updateUiState(uiState: ConversationEntryUiState) {
        _uiState.value = uiState

        savedStateHandle[LAUNCH_GENERATION_KEY] = uiState.launchGeneration
        savedStateHandle[CONVERSATION_ID_KEY] = uiState.conversationId
        savedStateHandle[IS_CREATING_GROUP_KEY] = uiState.isCreatingGroup
        savedStateHandle[PENDING_STARTUP_ATTACHMENT_TYPE_KEY] = uiState
            .pendingStartupAttachment
            ?.contentType

        savedStateHandle[PENDING_STARTUP_ATTACHMENT_URI_KEY] = uiState
            .pendingStartupAttachment
            ?.contentUri
        savedStateHandle[SELECTED_GROUP_RECIPIENT_DESTINATIONS_KEY] = ArrayList(
            uiState.selectedGroupRecipientDestinations,
        )
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
        private const val IS_CREATING_GROUP_KEY = "is_creating_group"
        private const val LAUNCH_GENERATION_KEY = "launch_generation"
        private const val PENDING_DRAFT_DATA_KEY = "pending_draft_data"
        private const val PENDING_SCROLL_POSITION_KEY = "pending_scroll_position"
        private const val PENDING_STARTUP_ATTACHMENT_TYPE_KEY = "pending_startup_attachment_type"
        private const val PENDING_STARTUP_ATTACHMENT_URI_KEY = "pending_startup_attachment_uri"

        // Tracks the last launch request handled by this ViewModel even when the
        // same launch generation remains in uiState for downstream side effects
        private const val PROCESSED_LAUNCH_GENERATION_KEY = "processed_launch_generation"
        private const val SELECTED_GROUP_RECIPIENT_DESTINATIONS_KEY =
            "selected_group_recipient_destinations"
    }
}
