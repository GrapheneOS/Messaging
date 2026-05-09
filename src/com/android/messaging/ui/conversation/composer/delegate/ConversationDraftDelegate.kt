package com.android.messaging.ui.conversation.composer.delegate

import android.app.Activity
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.conversation.usecase.action.CheckConversationActionRequirements
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationSimNotReadyException
import com.android.messaging.domain.conversation.usecase.draft.exception.MessageLimitExceededException
import com.android.messaging.domain.conversation.usecase.draft.exception.SendConversationDraftException
import com.android.messaging.domain.conversation.usecase.draft.exception.TooManyVideoAttachmentsException
import com.android.messaging.domain.conversation.usecase.draft.exception.UnknownConversationRecipientException
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface ConversationDraftDelegate : ConversationScreenDelegate<ConversationDraftState> {
    val effects: Flow<ConversationScreenEffect>
    val attachmentLimitWarning: StateFlow<ConversationAttachmentLimitWarning?>
    val isSubjectDialogVisible: StateFlow<Boolean>

    fun onMessageTextChanged(messageText: String)

    fun onSubjectTextChanged(subjectText: String)

    fun showSubjectDialog()

    fun dismissSubjectDialog()

    fun confirmSubjectDialog(subjectText: String)

    fun onSelfParticipantIdChanged(selfParticipantId: String)

    fun seedDraft(
        conversationId: String,
        draft: ConversationDraft,
    )

    fun addAttachments(
        attachments: Collection<ConversationDraftAttachment>,
    ): List<ConversationDraftAttachment>

    fun tryStartAddingAttachment(): Boolean

    fun addPendingAttachment(pendingAttachment: ConversationDraftPendingAttachment)

    fun removeAttachment(contentUri: String)

    fun removePendingAttachment(pendingAttachmentId: String)

    fun resolvePendingAttachment(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ): Boolean

    fun updateAttachmentCaption(
        contentUri: String,
        captionText: String,
    )

    fun onSendClick()

    fun dismissAttachmentLimitWarning()

    fun sendAnywayAfterAttachmentLimitWarning()

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
    private val conversationDraftEditorDelegate: ConversationDraftEditorDelegate,
    private val sendConversationDraft: SendConversationDraft,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationDraftDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(
        extraBufferCapacity = 1,
    )
    private val _attachmentLimitWarning = MutableStateFlow<ConversationAttachmentLimitWarning?>(
        value = null,
    )
    private val _isSubjectDialogVisible = MutableStateFlow(value = false)

    override val effects = _effects.asSharedFlow()
    override val attachmentLimitWarning = _attachmentLimitWarning.asStateFlow()
    override val isSubjectDialogVisible = _isSubjectDialogVisible.asStateFlow()
    override val state: StateFlow<ConversationDraftState> = conversationDraftEditorDelegate.state

    private val draftSaveMutex = Mutex()

    private var boundScope: CoroutineScope? = null
    private var pendingDefaultSmsRoleSendRequest: DraftSendRequest? = null
    private var pendingMessageLimitSendRequest: DraftSendRequest? = null

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
        conversationDraftEditorDelegate.onMessageTextChanged(messageText = messageText)
    }

    override fun onSubjectTextChanged(subjectText: String) {
        conversationDraftEditorDelegate.onSubjectTextChanged(subjectText = subjectText)
    }

    override fun showSubjectDialog() {
        _isSubjectDialogVisible.value = true
    }

    override fun dismissSubjectDialog() {
        _isSubjectDialogVisible.value = false
    }

    override fun confirmSubjectDialog(subjectText: String) {
        conversationDraftEditorDelegate.onSubjectTextChanged(subjectText = subjectText)
        _isSubjectDialogVisible.value = false
    }

    override fun onSelfParticipantIdChanged(selfParticipantId: String) {
        conversationDraftEditorDelegate.onSelfParticipantIdChanged(
            selfParticipantId = selfParticipantId,
        )
    }

    override fun seedDraft(
        conversationId: String,
        draft: ConversationDraft,
    ) {
        conversationDraftEditorDelegate.seedDraft(
            conversationId = conversationId,
            draft = draft,
        )
    }

    override fun addAttachments(
        attachments: Collection<ConversationDraftAttachment>,
    ): List<ConversationDraftAttachment> {
        val attachmentLimitResult = conversationDraftEditorDelegate.addAttachments(
            attachments = attachments,
        )

        if (attachmentLimitResult.didDropAttachments) {
            showComposingAttachmentLimitWarning()
        }

        return attachmentLimitResult.attachmentsToAdd
    }

    override fun tryStartAddingAttachment(): Boolean {
        val canStartAddingAttachment = conversationDraftEditorDelegate.tryStartAddingAttachment()

        if (!canStartAddingAttachment) {
            showComposingAttachmentLimitWarning()
        }

        return canStartAddingAttachment
    }

    override fun addPendingAttachment(pendingAttachment: ConversationDraftPendingAttachment) {
        conversationDraftEditorDelegate.addPendingAttachment(
            pendingAttachment = pendingAttachment,
        )
    }

    override fun removeAttachment(contentUri: String) {
        conversationDraftEditorDelegate.removeAttachment(contentUri = contentUri)
    }

    override fun removePendingAttachment(pendingAttachmentId: String) {
        conversationDraftEditorDelegate.removePendingAttachment(
            pendingAttachmentId = pendingAttachmentId,
        )
    }

    override fun resolvePendingAttachment(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ): Boolean {
        val resolution = conversationDraftEditorDelegate.resolvePendingAttachment(
            pendingAttachmentId = pendingAttachmentId,
            attachment = attachment,
        )

        if (resolution.didDropAttachments) {
            showComposingAttachmentLimitWarning()
        }

        return resolution.didResolveAttachment
    }

    override fun updateAttachmentCaption(
        contentUri: String,
        captionText: String,
    ) {
        conversationDraftEditorDelegate.updateAttachmentCaption(
            contentUri = contentUri,
            captionText = captionText,
        )
    }

    override fun onSendClick() {
        conversationDraftEditorDelegate.createSendRequestOrNull()
            ?.let(::sendDraftWhenActionRequirementsSatisfied)
    }

    override fun dismissAttachmentLimitWarning() {
        _attachmentLimitWarning.value = null
    }

    private fun showComposingAttachmentLimitWarning() {
        _attachmentLimitWarning.value = ConversationAttachmentLimitWarning
            .ComposingAttachmentLimitReached
    }

    override fun sendAnywayAfterAttachmentLimitWarning() {
        val sendRequest = pendingMessageLimitSendRequest ?: return
        pendingMessageLimitSendRequest = null
        _attachmentLimitWarning.value = null

        sendDraftWhenActionRequirementsSatisfied(
            sendRequest = sendRequest.copy(ignoreMessageSizeLimit = true),
        )
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
        val saveRequest = conversationDraftEditorDelegate.currentSaveRequest ?: return

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
        val saveRequest = conversationDraftEditorDelegate.currentSaveRequest ?: return

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
                !conversationDraftEditorDelegate.matchesSaveRequest(
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

            conversationDraftEditorDelegate.applyPersistedSaveResult(saveRequest = saveRequest)
        }
    }

    private fun bindConversationDraftObservation(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        scope.launch(defaultDispatcher) {
            observeConversationDraftUpdates(conversationIdFlow = conversationIdFlow)
                .collect { persistedDraftUpdate ->
                    conversationDraftEditorDelegate.applyPersistedDraftUpdate(
                        persistedDraftUpdate = persistedDraftUpdate,
                    )
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
            conversationDraftEditorDelegate.sendProtocolUpdates.collect { sendProtocol ->
                conversationDraftEditorDelegate.applySendProtocol(sendProtocol = sendProtocol)
            }
        }
    }

    private suspend fun resetDraftEditorState(conversationId: String?) {
        pendingMessageLimitSendRequest = null
        _attachmentLimitWarning.value = null
        _isSubjectDialogVisible.value = false

        val previousSaveRequest = conversationDraftEditorDelegate.reset(
            conversationId = conversationId,
        )

        previousSaveRequest
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

        if (conversationDraftEditorDelegate.markSendingForSendRequest(sendRequest = sendRequest)) {
            launchDraftOperation(scope = scope) {
                createSendDraftFlow(sendRequest = sendRequest)
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
            onFailure = { exception ->
                handleSendDraftFailure(
                    exception = exception,
                    sendRequest = sendRequest,
                )
            },
        ) {
            sendConversationDraft(
                conversationId = sendRequest.conversationId,
                draft = sendRequest.draft,
                ignoreMessageSizeLimit = sendRequest.ignoreMessageSizeLimit,
            ).onEach {
                conversationDraftEditorDelegate.clearConversationDraftAfterSend(
                    sendRequest = sendRequest,
                )
                didClearDraftAfterSend = true
            }.onCompletion { throwable ->
                if (throwable != null || !didClearDraftAfterSend) {
                    conversationDraftEditorDelegate.markConversationDraftAsIdle(
                        conversationId = sendRequest.conversationId,
                    )
                }
            }
        }
    }

    private fun handleSendDraftFailure(
        exception: Throwable,
        sendRequest: DraftSendRequest,
    ) {
        // TODO: Add an extension that properly skip CancellationException manual handling

        val wasAttachmentLimitFailure = handleAttachmentLimitFailure(
            exception = exception,
            sendRequest = sendRequest,
        )

        if (!wasAttachmentLimitFailure && exception !is CancellationException) {
            emitEffect(
                effect = ConversationScreenEffect.ShowMessage(
                    messageResId = resolveSendDraftFailureMessageResId(exception = exception),
                ),
            )
        }
    }

    private fun handleAttachmentLimitFailure(
        exception: Throwable,
        sendRequest: DraftSendRequest,
    ): Boolean {
        return when (exception) {
            is TooManyVideoAttachmentsException -> {
                _attachmentLimitWarning.value = ConversationAttachmentLimitWarning
                    .SendingVideoAttachmentLimitReached
                true
            }

            is MessageLimitExceededException -> {
                pendingMessageLimitSendRequest = sendRequest
                _attachmentLimitWarning.value = ConversationAttachmentLimitWarning
                    .SendingMessageLimitReached
                true
            }

            else -> false
        }
    }

    private fun resolveSendDraftFailureMessageResId(exception: Throwable): Int {
        return when (exception) {
            is ConversationSimNotReadyException -> {
                R.string.cant_send_message_without_active_subscription
            }

            is UnknownConversationRecipientException -> R.string.unknown_sender
            is SendConversationDraftException -> R.string.send_message_failure
            else -> R.string.send_message_failure
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
            conversationDraftEditorDelegate
                .saveRequests
                .distinctUntilChanged()
                .debounce(timeoutMillis = DRAFT_AUTOSAVE_DELAY_MILLIS)
                .filterNotNull()
        }
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
    }
}
