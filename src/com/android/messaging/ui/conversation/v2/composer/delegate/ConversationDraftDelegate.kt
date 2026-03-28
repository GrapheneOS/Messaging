package com.android.messaging.ui.conversation.v2.composer.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.SendConversationDraft
import com.android.messaging.ui.conversation.v2.common.ConversationScreenDelegate
import com.android.messaging.util.LogUtil
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface ConversationDraftDelegate : ConversationScreenDelegate<ConversationDraft> {
    val effects: Flow<ConversationDraftEffect>

    fun onMessageTextChanged(messageText: String)

    fun onAttachmentClick()

    fun onSendClick()

    fun persistDraft()

    fun flushDraft()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class ConversationDraftDelegateImpl @Inject constructor(
    @param:ApplicationCoroutineScope
    private val applicationScope: CoroutineScope,
    private val conversationDraftsRepository: ConversationDraftsRepository,
    private val sendConversationDraft: SendConversationDraft,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationDraftDelegate {

    private val _effects = MutableSharedFlow<ConversationDraftEffect>(
        extraBufferCapacity = 1,
    )
    private val _state = MutableStateFlow(ConversationDraft())
    override val effects = _effects.asSharedFlow()
    override val state = _state.asStateFlow()

    private val draftEditorState = MutableStateFlow(DraftEditorState())
    private val draftSaveMutex = Mutex()

    private var boundScope: CoroutineScope? = null

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
    }

    override fun onMessageTextChanged(messageText: String) {
        updateDraftEditorState { currentDraftEditorState ->
            return@updateDraftEditorState currentDraftEditorState.withMessageText(
                messageText = messageText,
            )
        }
    }

    override fun onAttachmentClick() {
        val scope = boundScope ?: return

        launchDraftOperation(scope = scope) {
            createAttachmentClickFlow()
        }
    }

    override fun onSendClick() {
        val scope = boundScope ?: return
        val sendRequest = markSendingAndCreateSendRequestOrNull() ?: return

        launchDraftOperation(scope = scope) {
            createSendDraftFlow(
                sendRequest = sendRequest,
            )
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
                return@updateDraftEditorState currentDraftEditorState.markPersistedIfUnchanged(
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

    private suspend fun resetDraftEditorState(conversationId: String?) {
        val previousDraftEditorState = draftEditorState.value
        updateDraftEditorState(
            draftEditorState = DraftEditorState(
                conversationId = conversationId,
            ),
        )

        previousDraftEditorState
            .toSaveRequestOrNull()
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

    private fun createAttachmentClickFlow(): Flow<Unit> {
        return runDraftOperationBoundary(
            operationName = "launch attachment chooser",
            conversationId = draftEditorState.value.conversationId,
        ) {
            unitFlow {
                val currentDraftEditorState = draftEditorState.value
                if (!currentDraftEditorState.canLaunchAttachmentChooser()) {
                    return@unitFlow
                }

                val saveRequest = currentDraftEditorState.toSaveRequestOrNull()
                if (saveRequest != null) {
                    saveDraft(
                        saveRequest = saveRequest,
                        shouldMarkCurrentDraftAsPersisted = true,
                        shouldSkipIfRequestIsStale = true,
                    )
                }

                val conversationId = draftEditorState.value.conversationId ?: return@unitFlow
                _effects.emit(
                    value = ConversationDraftEffect.LaunchAttachmentChooser(
                        conversationId = conversationId,
                    ),
                )
            }
        }
    }

    private fun createSendDraftFlow(sendRequest: DraftSendRequest): Flow<Unit> {
        var didClearDraftAfterSend = false

        return runDraftOperationBoundary(
            operationName = "send draft",
            conversationId = sendRequest.conversationId,
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

    private fun updateDraftEditorState(draftEditorState: DraftEditorState) {
        this.draftEditorState.value = draftEditorState
        _state.value = draftEditorState.visibleDraft
    }

    private fun updateDraftEditorState(transform: (DraftEditorState) -> DraftEditorState) {
        draftEditorState.update { currentDraftEditorState ->
            val updatedDraftEditorState = transform(currentDraftEditorState)
            _state.value = updatedDraftEditorState.visibleDraft

            updatedDraftEditorState
        }
    }

    private fun markConversationDraftAsIdle(conversationId: String) {
        updateDraftEditorState { currentDraftEditorState ->
            if (currentDraftEditorState.conversationId != conversationId) {
                return@updateDraftEditorState currentDraftEditorState
            }

            return@updateDraftEditorState currentDraftEditorState.markIdle()
        }
    }

    private fun clearConversationDraftAfterSend(sendRequest: DraftSendRequest) {
        updateDraftEditorState { latestDraftEditorState ->
            if (latestDraftEditorState.conversationId != sendRequest.conversationId) {
                return@updateDraftEditorState latestDraftEditorState
            }

            return@updateDraftEditorState latestDraftEditorState.clearDraftAfterSend(
                sentDraft = sendRequest.draft,
            )
        }
    }

    private fun markSendingAndCreateSendRequestOrNull(): DraftSendRequest? {
        var sendRequest: DraftSendRequest? = null

        updateDraftEditorState { currentDraftEditorState ->
            if (!currentDraftEditorState.canSendDraft()) {
                return@updateDraftEditorState currentDraftEditorState
            }

            val conversationId = currentDraftEditorState
                .conversationId
                ?: return@updateDraftEditorState currentDraftEditorState

            sendRequest = DraftSendRequest(
                conversationId = conversationId,
                draft = currentDraftEditorState.effectiveDraft,
            )

            currentDraftEditorState.markSending()
        }

        return sendRequest
    }

    private fun <T> runDraftOperationBoundary(
        operationName: String,
        conversationId: String?,
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
        }
    }

    private companion object {
        private const val TAG = "ConversationDraftDelegate"

        private const val DRAFT_AUTOSAVE_DELAY_MILLIS = 300L
    }
}

private data class DraftEditorState(
    val conversationId: String? = null,
    val persistedDraft: ConversationDraft = ConversationDraft(),
    val localEdits: ConversationDraftEdits = ConversationDraftEdits(),
    val isLoaded: Boolean = false,
    val isSending: Boolean = false,
    val pendingSentDraft: ConversationDraft? = null,
) {
    val effectiveDraft: ConversationDraft
        get() = localEdits.applyTo(baseDraft = persistedDraft)

    val visibleDraft: ConversationDraft
        get() {
            if (conversationId == null) {
                return ConversationDraft()
            }

            return effectiveDraft.copy(
                isCheckingDraft = !isLoaded,
                isSending = isSending,
            )
        }

    fun withPersistedDraft(persistedDraft: ConversationDraft): DraftEditorState {
        pendingSentDraft?.let { draft ->
            return withPersistedDraftWhileAwaitingSentDraftClear(
                persistedDraft = persistedDraft,
                sentDraftAwaitingClear = draft,
            )
        }

        return copy(
            persistedDraft = persistedDraft,
            localEdits = localEdits.normalizedAgainst(
                baseDraft = persistedDraft,
            ),
            isLoaded = true,
        )
    }

    fun withMessageText(messageText: String): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        return copy(
            localEdits = localEdits
                .copy(messageText = messageText)
                .normalizedAgainst(baseDraft = persistedDraft),
        )
    }

    fun toSaveRequestOrNull(): DraftSaveRequest? {
        val currentConversationId = conversationId ?: return null

        if (!isLoaded || isSending || !localEdits.hasChanges) {
            return null
        }

        return DraftSaveRequest(
            conversationId = currentConversationId,
            draft = effectiveDraft,
        )
    }

    fun canLaunchAttachmentChooser(): Boolean {
        return conversationId != null &&
            isLoaded &&
            !isSending
    }

    fun canSendDraft(): Boolean {
        return conversationId != null &&
            isLoaded &&
            !isSending &&
            effectiveDraft.hasContent
    }

    fun markPersistedIfUnchanged(saveRequest: DraftSaveRequest): DraftEditorState {
        if (conversationId != saveRequest.conversationId) {
            return this
        }

        if (effectiveDraft != saveRequest.draft) {
            return this
        }

        return copy(
            persistedDraft = saveRequest.draft,
            localEdits = ConversationDraftEdits(),
            isLoaded = true,
            pendingSentDraft = null,
        )
    }

    fun matchesSaveRequest(saveRequest: DraftSaveRequest): Boolean {
        return toSaveRequestOrNull() == saveRequest
    }

    fun markSending(): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        return copy(isSending = true)
    }

    fun markIdle(): DraftEditorState {
        return copy(isSending = false)
    }

    fun clearDraftAfterSend(sentDraft: ConversationDraft): DraftEditorState {
        val latestEffectiveDraft = effectiveDraft

        val clearedDraft = createClearedDraftForSentDraft(
            sentDraft = sentDraft,
        )

        val visibleDraftAfterSend = when {
            latestEffectiveDraft == sentDraft -> clearedDraft

            // Preserve edits made while the send is enqueued
            else -> latestEffectiveDraft.copy(
                selfParticipantId = sentDraft.selfParticipantId,
            )
        }

        return copy(
            persistedDraft = clearedDraft,
            localEdits = createConversationDraftEdits(
                baseDraft = clearedDraft,
                targetDraft = visibleDraftAfterSend,
            ),
            isLoaded = true,
            isSending = false,
            pendingSentDraft = sentDraft,
        )
    }

    private fun withPersistedDraftWhileAwaitingSentDraftClear(
        persistedDraft: ConversationDraft,
        sentDraftAwaitingClear: ConversationDraft,
    ): DraftEditorState {
        if (persistedDraft == sentDraftAwaitingClear) {
            return rebaseVisibleDraftOnPersistedDraft(
                persistedDraft = persistedDraft,
                shouldKeepPendingSentDraft = true,
            )
        }

        val clearedDraft = createClearedDraftForSentDraft(
            sentDraft = sentDraftAwaitingClear,
        )
        if (effectiveDraft == clearedDraft) {
            return copy(
                persistedDraft = persistedDraft,
                localEdits = ConversationDraftEdits(),
                isLoaded = true,
                pendingSentDraft = null,
            )
        }

        return rebaseVisibleDraftOnPersistedDraft(
            persistedDraft = persistedDraft,
            shouldKeepPendingSentDraft = false,
        )
    }

    private fun rebaseVisibleDraftOnPersistedDraft(
        persistedDraft: ConversationDraft,
        shouldKeepPendingSentDraft: Boolean,
    ): DraftEditorState {
        val visibleDraft = effectiveDraft

        return copy(
            persistedDraft = persistedDraft,
            localEdits = createConversationDraftEdits(
                baseDraft = persistedDraft,
                targetDraft = visibleDraft,
            ),
            isLoaded = true,
            pendingSentDraft = pendingSentDraft.takeIf { shouldKeepPendingSentDraft },
        )
    }
}

private fun createClearedDraftForSentDraft(
    sentDraft: ConversationDraft,
): ConversationDraft {
    return ConversationDraft(
        selfParticipantId = sentDraft.selfParticipantId,
    )
}

private data class ConversationDraftEdits(
    val messageText: String? = null,
    val subjectText: String? = null,
    val selfParticipantId: String? = null,
    val attachments: List<ConversationDraftAttachment>? = null,
) {
    val hasChanges: Boolean
        get() {
            return messageText != null ||
                subjectText != null ||
                selfParticipantId != null ||
                attachments != null
        }

    fun applyTo(baseDraft: ConversationDraft): ConversationDraft {
        return baseDraft.copy(
            messageText = messageText ?: baseDraft.messageText,
            subjectText = subjectText ?: baseDraft.subjectText,
            selfParticipantId = selfParticipantId ?: baseDraft.selfParticipantId,
            attachments = attachments ?: baseDraft.attachments,
        )
    }

    fun normalizedAgainst(baseDraft: ConversationDraft): ConversationDraftEdits {
        return ConversationDraftEdits(
            messageText = messageText?.takeUnless { value ->
                value == baseDraft.messageText
            },
            subjectText = subjectText?.takeUnless { value ->
                value == baseDraft.subjectText
            },
            selfParticipantId = selfParticipantId?.takeUnless { value ->
                value == baseDraft.selfParticipantId
            },
            attachments = attachments?.takeUnless { value ->
                value == baseDraft.attachments
            },
        )
    }
}

private fun createConversationDraftEdits(
    baseDraft: ConversationDraft,
    targetDraft: ConversationDraft,
): ConversationDraftEdits {
    return ConversationDraftEdits(
        messageText = targetDraft.messageText.takeUnless { value ->
            value == baseDraft.messageText
        },
        subjectText = targetDraft.subjectText.takeUnless { value ->
            value == baseDraft.subjectText
        },
        selfParticipantId = targetDraft.selfParticipantId.takeUnless { value ->
            value == baseDraft.selfParticipantId
        },
        attachments = targetDraft.attachments.takeUnless { value ->
            value == baseDraft.attachments
        },
    )
}

private data class DraftSaveRequest(val conversationId: String, val draft: ConversationDraft)

private data class DraftSendRequest(val conversationId: String, val draft: ConversationDraft)

private data class PersistedDraftUpdate(
    val conversationId: String,
    val persistedDraft: ConversationDraft,
)
