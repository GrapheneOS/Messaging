package com.android.messaging.ui.conversation.v2.composer.delegate

import android.app.Activity
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.draft.GetConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationSimNotReadyException
import com.android.messaging.domain.conversation.usecase.draft.exception.SendConversationDraftException
import com.android.messaging.domain.conversation.usecase.draft.exception.TooManyVideoAttachmentsException
import com.android.messaging.domain.conversation.usecase.draft.exception.UnknownConversationRecipientException
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.v2.screen.model.ConversationScreenEffect
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface ConversationDraftDelegate : ConversationScreenDelegate<ConversationDraftState> {
    val effects: Flow<ConversationScreenEffect>

    fun onMessageTextChanged(messageText: String)

    fun onSelfParticipantIdChanged(selfParticipantId: String)

    fun seedDraft(
        conversationId: String,
        draft: ConversationDraft,
    )

    fun addAttachments(attachments: Collection<ConversationDraftAttachment>)

    fun addPendingAttachment(pendingAttachment: ConversationDraftPendingAttachment)

    fun removeAttachment(contentUri: String)

    fun removePendingAttachment(pendingAttachmentId: String)

    fun resolvePendingAttachment(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    )

    fun updateAttachmentCaption(
        contentUri: String,
        captionText: String,
    )

    fun onSendClick()

    fun onDefaultSmsRoleRequestResult(resultCode: Int): Boolean

    fun persistDraft()

    fun flushDraft()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class ConversationDraftDelegateImpl @Inject constructor(
    @param:ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
    private val checkConversationActionRequirements: CheckConversationActionRequirements,
    private val conversationDraftsRepository: ConversationDraftsRepository,
    private val conversationsRepository: ConversationsRepository,
    private val getConversationDraftSendProtocol: GetConversationDraftSendProtocol,
    private val sendConversationDraft: SendConversationDraft,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationDraftDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val _state = MutableStateFlow(ConversationDraftState())
    override val effects = _effects.asSharedFlow()
    override val state = _state.asStateFlow()

    private val draftEditorState = MutableStateFlow(DraftEditorState())
    private val draftSaveMutex = Mutex()

    private var boundScope: CoroutineScope? = null
    private var pendingDraftSeed: PendingDraftSeed? = null
    private var pendingDefaultSmsRoleSendRequest: DraftSendRequest? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        bindConversationDraftObservation(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )
        bindDraftAutosave(scope = scope)
        bindDraftSendProtocol(scope = scope)
    }

    override fun onMessageTextChanged(messageText: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withMessageText(messageText)
        }
    }

    override fun onSelfParticipantIdChanged(selfParticipantId: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withSelfParticipantId(selfParticipantId = selfParticipantId)
        }
    }

    override fun seedDraft(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        pendingDraftSeed = PendingDraftSeed(
            conversationId = conversationId,
            draft = draft,
        )
        applyPendingDraftSeedIfPossible()
    }

    override fun addAttachments(attachments: Collection<ConversationDraftAttachment>) {
        if (attachments.isEmpty()) {
            return
        }

        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withAttachmentsAdded(attachments)
        }
    }

    override fun addPendingAttachment(pendingAttachment: ConversationDraftPendingAttachment) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withPendingAttachmentAdded(pendingAttachment)
        }
    }

    override fun removeAttachment(contentUri: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withAttachmentRemoved(contentUri)
        }
    }

    override fun removePendingAttachment(pendingAttachmentId: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withPendingAttachmentRemoved(pendingAttachmentId)
        }
    }

    override fun resolvePendingAttachment(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withPendingAttachmentResolved(
                pendingAttachmentId = pendingAttachmentId,
                attachment = attachment,
            )
        }
    }

    override fun updateAttachmentCaption(
        contentUri: String,
        captionText: String,
    ) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withAttachmentCaption(
                contentUri = contentUri,
                captionText = captionText,
            )
        }
    }

    override fun onSendClick() {
        createSendRequestOrNull()
            ?.let(::sendDraftWhenActionRequirementsSatisfied)
    }

    override fun onDefaultSmsRoleRequestResult(resultCode: Int): Boolean {
        val sendRequest = pendingDefaultSmsRoleSendRequest ?: return false

        pendingDefaultSmsRoleSendRequest = null

        return when (resultCode) {
            Activity.RESULT_OK -> {
                sendDraftWhenActionRequirementsSatisfied(sendRequest = sendRequest)
                true
            }

            else -> false
        }
    }

    override fun persistDraft() {
        val scope = boundScope ?: return
        val saveRequest = draftEditorState.value.toSaveRequestOrNull() ?: return

        launchDraftOperation(scope = scope) {
            createSaveDraftOperationFlow(
                operationName = "persist draft",
                saveRequest = saveRequest,
                shouldMarkCurrentDraftAsPersisted = true,
                shouldSkipIfRequestIsStale = true,
            )
        }
    }

    override fun flushDraft() {
        val saveRequest = draftEditorState.value.toSaveRequestOrNull() ?: return

        launchDraftOperation(scope = applicationScope) {
            createSaveDraftOperationFlow(
                operationName = "flush draft",
                saveRequest = saveRequest,
                shouldMarkCurrentDraftAsPersisted = false,
                shouldSkipIfRequestIsStale = false,
                shouldRunNonCancellable = true,
            )
        }
    }

    private suspend fun saveDraft(
        saveRequest: DraftSaveRequest,
        shouldMarkCurrentDraftAsPersisted: Boolean,
        shouldSkipIfRequestIsStale: Boolean,
    ) {
        draftSaveMutex.withLock {
            // Ignore debounced or queued saves that no longer reflect the current working draft
            if (shouldSkipIfRequestIsStale &&
                !draftEditorState.value.matchesSaveRequest(
                    saveRequest = saveRequest,
                )
            ) {
                return@withLock
            }

            conversationDraftsRepository.saveDraft(
                conversationId = saveRequest.conversationId,
                draft = saveRequest.draft,
            )

            if (!shouldMarkCurrentDraftAsPersisted) {
                return@withLock
            }

            updateDraftEditorState { currentDraftEditorState ->
                currentDraftEditorState.markPersistedIfUnchanged(
                    saveRequest = saveRequest,
                )
            }
        }
    }

    private fun bindConversationDraftObservation(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        scope.launch(defaultDispatcher) {
            observeConversationDraftUpdates(conversationIdFlow = conversationIdFlow)
                .collect { persistedDraftUpdate ->
                    updateDraftEditorState { currentDraftEditorState ->
                        if (currentDraftEditorState.conversationId !=
                            persistedDraftUpdate.conversationId
                        ) {
                            currentDraftEditorState
                        } else {
                            currentDraftEditorState.withPersistedDraft(
                                persistedDraft = persistedDraftUpdate.persistedDraft,
                            )
                        }
                    }
                    applyPendingDraftSeedIfPossible()
                }
        }
    }

    private fun bindDraftAutosave(scope: CoroutineScope) {
        scope.launch(defaultDispatcher) {
            observeDraftAutosaveRequests().collect { saveRequest ->
                createSaveDraftOperationFlow(
                    operationName = "autosave draft",
                    saveRequest = saveRequest,
                    shouldMarkCurrentDraftAsPersisted = true,
                    shouldSkipIfRequestIsStale = true,
                ).collect()
            }
        }
    }

    private fun bindDraftSendProtocol(scope: CoroutineScope) {
        scope.launch(defaultDispatcher) {
            observeDraftSendProtocol().collect { sendProtocol ->
                _state.update { currentState ->
                    currentState.copy(
                        sendProtocol = when {
                            currentState.draft.hasContent -> sendProtocol
                            else -> ConversationDraftSendProtocol.SMS
                        },
                    )
                }
            }
        }
    }

    private suspend fun resetDraftEditorState(conversationId: String?) {
        var previousDraftEditorState: DraftEditorState? = null

        updateDraftEditorState { currentDraftEditorState ->
            previousDraftEditorState = currentDraftEditorState
            DraftEditorState(conversationId = conversationId)
        }
        applyPendingDraftSeedIfPossible()

        previousDraftEditorState
            ?.toSaveRequestOrNull()
            ?.let { saveRequest ->
                createSaveDraftOperationFlow(
                    operationName = "flush previous draft",
                    saveRequest = saveRequest,
                    shouldMarkCurrentDraftAsPersisted = false,
                    shouldSkipIfRequestIsStale = false,
                    shouldRunNonCancellable = true,
                ).collect()
            }
    }

    private fun launchDraftOperation(
        scope: CoroutineScope,
        createOperationFlow: () -> Flow<Unit>,
    ) {
        scope.launch(defaultDispatcher) {
            createOperationFlow().collect()
        }
    }

    private fun sendDraftWhenActionRequirementsSatisfied(sendRequest: DraftSendRequest) {
        when (checkConversationActionRequirements()) {
            ConversationActionRequirementsResult.Ready -> {
                sendDraft(sendRequest = sendRequest)
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
                pendingDefaultSmsRoleSendRequest = sendRequest
                emitEffect(
                    effect = ConversationScreenEffect.RequestDefaultSmsRole(
                        isSending = true,
                    ),
                )
            }
        }
    }

    private fun sendDraft(sendRequest: DraftSendRequest) {
        val scope = boundScope ?: return

        if (markSendingForSendRequest(sendRequest = sendRequest)) {
            launchDraftOperation(scope = scope) {
                createSendDraftFlow(sendRequest)
            }
        }
    }

    private fun emitEffect(effect: ConversationScreenEffect) {
        boundScope?.launch(defaultDispatcher) {
            _effects.emit(effect)
        }
    }

    private fun createSendDraftFlow(sendRequest: DraftSendRequest): Flow<Unit> {
        var didClearDraftAfterSend = false

        return runDraftOperationBoundary(
            operationName = "send draft",
            conversationId = sendRequest.conversationId,
            onFailure = ::handleSendDraftFailure,
        ) {
            sendConversationDraft(
                conversationId = sendRequest.conversationId,
                draft = sendRequest.draft,
            ).onEach {
                clearConversationDraftAfterSend(sendRequest = sendRequest)
                didClearDraftAfterSend = true
            }.onCompletion { throwable ->
                if (throwable != null || !didClearDraftAfterSend) {
                    markConversationDraftAsIdle(conversationId = sendRequest.conversationId)
                }
            }
        }
    }

    private fun handleSendDraftFailure(exception: Throwable) {
        // TODO: Add an extension that properly skip CancellationException manual handling

        val messageResId = when (exception) {
            is CancellationException -> return

            is ConversationSimNotReadyException -> {
                R.string.cant_send_message_without_active_subscription
            }

            is TooManyVideoAttachmentsException -> {
                R.string.cant_send_message_with_multiple_videos
            }

            is UnknownConversationRecipientException -> R.string.unknown_sender
            is SendConversationDraftException -> R.string.send_message_failure
            else -> R.string.send_message_failure
        }

        emitEffect(
            effect = ConversationScreenEffect.ShowMessage(
                messageResId = messageResId,
            ),
        )
    }

    private fun createSaveDraftOperationFlow(
        operationName: String,
        saveRequest: DraftSaveRequest,
        shouldMarkCurrentDraftAsPersisted: Boolean,
        shouldSkipIfRequestIsStale: Boolean,
        shouldRunNonCancellable: Boolean = false,
    ): Flow<Unit> {
        return runDraftOperationBoundary(
            operationName = operationName,
            conversationId = saveRequest.conversationId,
        ) {
            unitFlow {
                if (shouldRunNonCancellable) {
                    withContext(context = NonCancellable) {
                        saveDraft(
                            saveRequest = saveRequest,
                            shouldMarkCurrentDraftAsPersisted = shouldMarkCurrentDraftAsPersisted,
                            shouldSkipIfRequestIsStale = shouldSkipIfRequestIsStale,
                        )
                    }

                    return@unitFlow
                }

                saveDraft(
                    saveRequest = saveRequest,
                    shouldMarkCurrentDraftAsPersisted = shouldMarkCurrentDraftAsPersisted,
                    shouldSkipIfRequestIsStale = shouldSkipIfRequestIsStale,
                )
            }
        }
    }

    private fun observeConversationDraftUpdates(
        conversationIdFlow: StateFlow<String?>,
    ): Flow<PersistedDraftUpdate> {
        return runDraftOperationBoundary(
            operationName = "observe drafts",
            conversationId = null,
        ) {
            conversationIdFlow.transformLatest { conversationId ->
                resetDraftEditorState(conversationId = conversationId)

                if (conversationId == null) {
                    return@transformLatest
                }

                emitAll(createPersistedDraftUpdatesFlow(conversationId = conversationId))
            }
        }
    }

    private fun createPersistedDraftUpdatesFlow(
        conversationId: String,
    ): Flow<PersistedDraftUpdate> {
        return conversationDraftsRepository
            .observeConversationDraft(conversationId = conversationId)
            .map { persistedDraft ->
                PersistedDraftUpdate(
                    conversationId = conversationId,
                    persistedDraft = persistedDraft,
                )
            }
            .catch { exception ->
                LogUtil.e(
                    TAG,
                    "Failed to observe draft for conversation $conversationId",
                    exception,
                )

                emit(
                    PersistedDraftUpdate(
                        conversationId = conversationId,
                        persistedDraft = ConversationDraft(),
                    ),
                )
            }
    }

    private fun observeDraftAutosaveRequests(): Flow<DraftSaveRequest> {
        return runDraftOperationBoundary(
            operationName = "bind draft autosave",
            conversationId = null,
        ) {
            draftEditorState
                .map { currentDraftEditorState ->
                    currentDraftEditorState.toSaveRequestOrNull()
                }
                .distinctUntilChanged()
                .debounce(timeoutMillis = DRAFT_AUTOSAVE_DELAY_MILLIS)
                .filterNotNull()
        }
    }

    private fun observeDraftSendProtocol(): Flow<ConversationDraftSendProtocol> {
        return draftEditorState
            .map { currentDraftEditorState ->
                currentDraftEditorState.conversationId to currentDraftEditorState.effectiveDraft
            }
            .distinctUntilChanged()
            .debounce(timeoutMillis = DRAFT_SEND_PROTOCOL_DEBOUNCE_MILLIS)
            .mapLatest { (conversationId, draft) ->
                resolveDraftSendProtocol(
                    conversationId = conversationId,
                    draft = draft,
                )
            }
            .distinctUntilChanged()
    }

    private suspend fun resolveDraftSendProtocol(
        conversationId: String?,
        draft: ConversationDraft,
    ): ConversationDraftSendProtocol {
        return try {
            val resolvedConversationId = conversationId?.takeIf { it.isNotBlank() }
            val sendData = when {
                draft.hasContent && resolvedConversationId != null -> {
                    withContext(ioDispatcher) {
                        conversationsRepository.getConversationSendData(
                            conversationId = resolvedConversationId,
                            requestedSelfParticipantId = draft.selfParticipantId,
                        )
                    }
                }

                else -> null
            }

            when (sendData) {
                null -> fallbackDraftSendProtocol(draft = draft)
                else -> {
                    getConversationDraftSendProtocol(
                        draft = draft,
                        sendData = sendData,
                    )
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            LogUtil.e(
                TAG,
                "Failed to resolve draft send protocol for conversation $conversationId",
                exception,
            )

            fallbackDraftSendProtocol(draft = draft)
        }
    }

    private fun fallbackDraftSendProtocol(
        draft: ConversationDraft,
    ): ConversationDraftSendProtocol {
        return when {
            draft.isMms -> ConversationDraftSendProtocol.MMS
            else -> ConversationDraftSendProtocol.SMS
        }
    }

    private fun updateDraftEditorState(transform: (DraftEditorState) -> DraftEditorState) {
        draftEditorState.update { currentDraftEditorState ->
            val updatedDraftEditorState = transform(currentDraftEditorState)
            val visibleState = updatedDraftEditorState.visibleState
            val visibleSendProtocol = when {
                visibleState.draft.hasContent -> _state.value.sendProtocol
                else -> ConversationDraftSendProtocol.SMS
            }

            _state.value = visibleState.copy(
                sendProtocol = visibleSendProtocol,
            )

            updatedDraftEditorState
        }
    }

    private fun applyPendingDraftSeedIfPossible() {
        val pendingDraftSeed = pendingDraftSeed ?: return

        updateDraftEditorState { currentDraftEditorState ->
            if (currentDraftEditorState.conversationId != pendingDraftSeed.conversationId) {
                return@updateDraftEditorState currentDraftEditorState
            }

            this.pendingDraftSeed = null
            currentDraftEditorState.withSeededDraft(draft = pendingDraftSeed.draft)
        }
    }

    private fun markConversationDraftAsIdle(conversationId: String) {
        updateDraftEditorState { currentDraftEditorState ->
            if (currentDraftEditorState.conversationId != conversationId) {
                return@updateDraftEditorState currentDraftEditorState
            }

            currentDraftEditorState.markIdle()
        }
    }

    private fun clearConversationDraftAfterSend(sendRequest: DraftSendRequest) {
        updateDraftEditorState { latestDraftEditorState ->
            if (latestDraftEditorState.conversationId != sendRequest.conversationId) {
                return@updateDraftEditorState latestDraftEditorState
            }

            latestDraftEditorState.clearDraftAfterSend(
                sentDraft = sendRequest.draft,
            )
        }
    }

    private fun createSendRequestOrNull(): DraftSendRequest? {
        val currentDraftEditorState = draftEditorState.value
        val conversationId = currentDraftEditorState.conversationId

        return when {
            !currentDraftEditorState.canSendDraft() -> null
            conversationId == null -> null

            else -> {
                DraftSendRequest(
                    conversationId = conversationId,
                    draft = currentDraftEditorState.effectiveDraft,
                )
            }
        }
    }

    private fun markSendingForSendRequest(sendRequest: DraftSendRequest): Boolean {
        var didMarkSending = false

        updateDraftEditorState { state ->
            val isSameConversation = state.conversationId == sendRequest.conversationId

            val canMarkSending = isSameConversation && !state.isSending

            if (!canMarkSending) {
                return@updateDraftEditorState state
            }

            didMarkSending = true
            state.markSending()
        }

        return didMarkSending
    }

    private fun <T> runDraftOperationBoundary(
        operationName: String,
        conversationId: String?,
        onFailure: ((Throwable) -> Unit)? = null,
        createFlow: () -> Flow<T>,
    ): Flow<T> {
        return flow {
            emitAll(createFlow())
        }.catch { exception ->
            LogUtil.e(
                TAG,
                "Failed to $operationName for conversation $conversationId",
                exception,
            )
            onFailure?.invoke(exception)
        }
    }

    private companion object {
        private const val TAG = "ConversationDraftDelegate"

        private const val DRAFT_AUTOSAVE_DELAY_MILLIS = 300L
        private const val DRAFT_SEND_PROTOCOL_DEBOUNCE_MILLIS = 250L
    }
}

private data class PendingDraftSeed(
    val conversationId: String,
    val draft: ConversationDraft,
)
