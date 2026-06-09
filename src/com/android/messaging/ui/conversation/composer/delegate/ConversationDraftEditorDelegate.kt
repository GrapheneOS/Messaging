package com.android.messaging.ui.conversation.composer.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.domain.conversation.usecase.draft.ResolveConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.ResolveDraftAttachmentsWithinLimit
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.domain.conversation.usecase.draft.model.DraftAttachmentLimitResult
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update

internal interface ConversationDraftEditorDelegate {
    val state: StateFlow<ConversationDraftState>
    val saveRequests: Flow<DraftSaveRequest?>
    val sendProtocolUpdates: Flow<ConversationDraftSendProtocol>
    val currentSaveRequest: DraftSaveRequest?

    fun onMessageTextChanged(messageText: String)

    fun onSubjectTextChanged(subjectText: String)

    fun onSelfParticipantIdChanged(
        conversationId: String,
        selfParticipantId: String,
    )

    fun seedDraft(
        conversationId: String,
        draft: ConversationDraft,
    )

    fun addAttachments(
        attachments: Collection<ConversationDraftAttachment>,
    ): DraftAttachmentLimitResult

    fun tryStartAddingAttachment(): Boolean

    fun addPendingAttachment(pendingAttachment: ConversationDraftPendingAttachment)

    fun removeAttachment(contentUri: String)

    fun removePendingAttachment(pendingAttachmentId: String)

    fun resolvePendingAttachment(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ): DraftPendingAttachmentResolution

    fun updateAttachmentCaption(
        contentUri: String,
        captionText: String,
    )

    fun reset(conversationId: String?): DraftSaveRequest?

    fun applyPersistedDraftUpdate(persistedDraftUpdate: PersistedDraftUpdate)

    fun matchesSaveRequest(saveRequest: DraftSaveRequest): Boolean

    fun applyPersistedSaveResult(saveRequest: DraftSaveRequest)

    fun applySendProtocol(sendProtocol: ConversationDraftSendProtocol)

    fun createSendRequestOrNull(): DraftSendRequest?

    fun markSendingForSendRequest(sendRequest: DraftSendRequest): Boolean

    fun markConversationDraftAsIdle(conversationId: String)

    fun clearConversationDraftAfterSend(sendRequest: DraftSendRequest)
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class ConversationDraftEditorDelegateImpl @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val resolveConversationDraftSendProtocol: ResolveConversationDraftSendProtocol,
    private val resolveDraftAttachmentsWithinLimit: ResolveDraftAttachmentsWithinLimit,
) : ConversationDraftEditorDelegate {

    private val _state = MutableStateFlow(ConversationDraftState())
    private val draftEditorState = MutableStateFlow(DraftEditorState())

    override val state: StateFlow<ConversationDraftState> = _state.asStateFlow()
    override val saveRequests: Flow<DraftSaveRequest?> = draftEditorState
        .map { currentDraftEditorState ->
            currentDraftEditorState.toSaveRequestOrNull()
        }
    override val sendProtocolUpdates: Flow<ConversationDraftSendProtocol> = draftEditorState
        .map { currentDraftEditorState ->
            DraftSendProtocolRequest(
                conversationId = currentDraftEditorState.conversationId,
                draft = currentDraftEditorState.effectiveDraft,
            )
        }
        .distinctUntilChanged()
        .debounce(timeoutMillis = DRAFT_SEND_PROTOCOL_DEBOUNCE_MILLIS)
        .mapLatest { request ->
            resolveConversationDraftSendProtocol(
                conversationId = request.conversationId,
                draft = request.draft,
            )
        }
        .distinctUntilChanged()
    override val currentSaveRequest: DraftSaveRequest?
        get() {
            return draftEditorState.value.toSaveRequestOrNull()
        }

    private var pendingDraftSeed: PendingDraftSeed? = null
    private var pendingSelfParticipantId: PendingSelfParticipantId? = null

    override fun onMessageTextChanged(messageText: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withMessageText(messageText)
        }
    }

    override fun onSubjectTextChanged(subjectText: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withSubjectText(subjectText)
        }
    }

    override fun onSelfParticipantIdChanged(
        conversationId: String,
        selfParticipantId: String,
    ) {
        pendingSelfParticipantId = PendingSelfParticipantId(
            conversationId = conversationId,
            selfParticipantId = selfParticipantId,
        )
        applyPendingSelfParticipantIdIfPossible()
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

    override fun addAttachments(
        attachments: Collection<ConversationDraftAttachment>,
    ): DraftAttachmentLimitResult {
        if (attachments.isEmpty()) {
            return DraftAttachmentLimitResult(
                attachmentsToAdd = emptyList(),
                didDropAttachments = false,
            )
        }

        val attachmentLimitResult = resolveDraftAttachmentsWithinLimit(
            currentAttachments = draftEditorState.value.effectiveDraft.attachments,
            attachmentsToAdd = attachments,
        )
        val attachmentsToAdd = attachmentLimitResult.attachmentsToAdd

        if (attachmentsToAdd.isNotEmpty()) {
            updateDraftEditorState { currentDraftEditorState ->
                currentDraftEditorState.withAttachmentsAdded(attachments = attachmentsToAdd)
            }
        }

        return attachmentLimitResult
    }

    override fun tryStartAddingAttachment(): Boolean {
        val currentDraftEditorState = draftEditorState.value
        val currentAttachmentCount = currentDraftEditorState.effectiveDraft.attachments.size +
            currentDraftEditorState.pendingAttachments.size

        return currentAttachmentCount < subscriptionsRepository.resolveAttachmentLimit()
    }

    override fun addPendingAttachment(pendingAttachment: ConversationDraftPendingAttachment) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withPendingAttachmentAdded(pendingAttachment)
        }
    }

    override fun removeAttachment(contentUri: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withAttachmentRemoved(contentUri = contentUri)
        }
    }

    override fun removePendingAttachment(pendingAttachmentId: String) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withPendingAttachmentRemoved(
                pendingAttachmentId = pendingAttachmentId,
            )
        }
    }

    override fun resolvePendingAttachment(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ): DraftPendingAttachmentResolution {
        var resolution = DraftPendingAttachmentResolution(
            didResolveAttachment = false,
            didDropAttachments = false,
        )

        updateDraftEditorState { currentDraftEditorState ->
            val resolutionState = resolvePendingAttachmentState(
                currentDraftEditorState = currentDraftEditorState,
                pendingAttachmentId = pendingAttachmentId,
                attachment = attachment,
            )

            resolution = DraftPendingAttachmentResolution(
                didResolveAttachment = resolutionState.didResolveAttachment,
                didDropAttachments = resolutionState.didDropAttachments,
            )

            resolutionState.draftEditorState
        }

        return resolution
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

    override fun reset(conversationId: String?): DraftSaveRequest? {
        val saveRequest = draftEditorState.value.toSaveRequestOrNull()

        updateDraftEditorState {
            DraftEditorState(conversationId = conversationId)
        }
        applyPendingDraftSeedIfPossible()
        // A seeded draft replaces all local edits wholesale, so the pending SIM
        // selection must apply after it to survive.
        applyPendingSelfParticipantIdIfPossible()

        return saveRequest
    }

    override fun applyPersistedDraftUpdate(persistedDraftUpdate: PersistedDraftUpdate) {
        updateDraftEditorState { currentDraftEditorState ->
            when {
                currentDraftEditorState.conversationId != persistedDraftUpdate.conversationId -> {
                    currentDraftEditorState
                }

                else -> {
                    currentDraftEditorState.withPersistedDraft(
                        persistedDraft = persistedDraftUpdate.persistedDraft,
                    )
                }
            }
        }
        applyPendingDraftSeedIfPossible()
    }

    override fun matchesSaveRequest(saveRequest: DraftSaveRequest): Boolean {
        return draftEditorState.value.matchesSaveRequest(saveRequest = saveRequest)
    }

    override fun applyPersistedSaveResult(saveRequest: DraftSaveRequest) {
        updateDraftEditorState { currentDraftEditorState ->
            currentDraftEditorState.withPersistedSaveResult(saveRequest = saveRequest)
        }
    }

    override fun applySendProtocol(sendProtocol: ConversationDraftSendProtocol) {
        _state.update { currentState ->
            currentState.copy(
                sendProtocol = when {
                    currentState.draft.hasContent -> sendProtocol
                    else -> ConversationDraftSendProtocol.SMS
                },
            )
        }
    }

    override fun createSendRequestOrNull(): DraftSendRequest? {
        val currentDraftEditorState = draftEditorState.value
        val conversationId = currentDraftEditorState.conversationId

        return when {
            conversationId == null -> null
            !currentDraftEditorState.canSendDraft() -> null

            else -> {
                DraftSendRequest(
                    conversationId = conversationId,
                    draft = currentDraftEditorState.effectiveDraft,
                )
            }
        }
    }

    override fun markSendingForSendRequest(sendRequest: DraftSendRequest): Boolean {
        var didMarkSending = false

        updateDraftEditorState { currentDraftEditorState ->
            val isSameConversation = currentDraftEditorState.conversationId ==
                sendRequest.conversationId
            val canMarkSending = isSameConversation && !currentDraftEditorState.isSending

            if (!canMarkSending) {
                return@updateDraftEditorState currentDraftEditorState
            }

            didMarkSending = true
            currentDraftEditorState.markSending()
        }

        return didMarkSending
    }

    override fun markConversationDraftAsIdle(conversationId: String) {
        updateDraftEditorState { currentDraftEditorState ->
            if (currentDraftEditorState.conversationId != conversationId) {
                return@updateDraftEditorState currentDraftEditorState
            }

            currentDraftEditorState.markIdle()
        }
    }

    override fun clearConversationDraftAfterSend(sendRequest: DraftSendRequest) {
        updateDraftEditorState { currentDraftEditorState ->
            if (currentDraftEditorState.conversationId != sendRequest.conversationId) {
                return@updateDraftEditorState currentDraftEditorState
            }

            currentDraftEditorState.clearDraftAfterSend(sentDraft = sendRequest.draft)
        }
    }

    private fun resolvePendingAttachmentState(
        currentDraftEditorState: DraftEditorState,
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ): PendingAttachmentResolutionState {
        val hasPendingAttachment = currentDraftEditorState
            .pendingAttachments
            .any { pendingAttachment ->
                pendingAttachment.pendingAttachmentId == pendingAttachmentId
            }

        if (!hasPendingAttachment) {
            return PendingAttachmentResolutionState(
                draftEditorState = currentDraftEditorState,
                didResolveAttachment = false,
                didDropAttachments = false,
            )
        }

        val draftEditorStateWithoutPendingAttachment = currentDraftEditorState
            .withPendingAttachmentRemoved(pendingAttachmentId = pendingAttachmentId)

        val attachmentLimitResult = resolveDraftAttachmentsWithinLimit(
            currentAttachments = draftEditorStateWithoutPendingAttachment
                .effectiveDraft
                .attachments,
            attachmentsToAdd = listOf(attachment),
        )
        val attachmentsToAdd = attachmentLimitResult.attachmentsToAdd
        val updatedDraftEditorState = draftEditorStateWithoutPendingAttachment
            .withAttachmentsAdded(attachments = attachmentsToAdd)

        return PendingAttachmentResolutionState(
            draftEditorState = updatedDraftEditorState,
            didResolveAttachment = attachmentsToAdd.any { acceptedAttachment ->
                acceptedAttachment.contentUri == attachment.contentUri
            },
            didDropAttachments = attachmentLimitResult.didDropAttachments,
        )
    }

    private fun updateDraftEditorState(transform: (DraftEditorState) -> DraftEditorState) {
        draftEditorState.update { currentDraftEditorState ->
            val updatedDraftEditorState = transform(currentDraftEditorState)
            val visibleState = updatedDraftEditorState.visibleState
            val visibleSendProtocol = resolveVisibleSendProtocol(
                previousState = _state.value,
                visibleState = visibleState,
            )

            _state.value = visibleState.copy(
                sendProtocol = visibleSendProtocol,
            )

            updatedDraftEditorState
        }
    }

    private fun resolveVisibleSendProtocol(
        previousState: ConversationDraftState,
        visibleState: ConversationDraftState,
    ): ConversationDraftSendProtocol {
        val visibleDraft = visibleState.draft
        val previousDraft = previousState.draft

        return when {
            !visibleDraft.hasContent -> ConversationDraftSendProtocol.SMS
            visibleDraft.isMms -> ConversationDraftSendProtocol.MMS
            previousDraft.isMms -> ConversationDraftSendProtocol.SMS
            else -> previousState.sendProtocol
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

    private fun applyPendingSelfParticipantIdIfPossible() {
        val pendingSelfParticipantId = pendingSelfParticipantId ?: return

        updateDraftEditorState { currentDraftEditorState ->
            val isBoundToTargetConversation = currentDraftEditorState.conversationId ==
                pendingSelfParticipantId.conversationId

            if (!isBoundToTargetConversation) {
                return@updateDraftEditorState currentDraftEditorState
            }

            this.pendingSelfParticipantId = null
            currentDraftEditorState.withSelfParticipantId(
                selfParticipantId = pendingSelfParticipantId.selfParticipantId,
            )
        }
    }

    private companion object {
        private const val DRAFT_SEND_PROTOCOL_DEBOUNCE_MILLIS = 250L
    }
}

private data class DraftSendProtocolRequest(
    val conversationId: String?,
    val draft: ConversationDraft,
)

internal data class DraftPendingAttachmentResolution(
    val didResolveAttachment: Boolean,
    val didDropAttachments: Boolean,
)

private data class PendingDraftSeed(
    val conversationId: String,
    val draft: ConversationDraft,
)

private data class PendingSelfParticipantId(
    val conversationId: String,
    val selfParticipantId: String,
)

private data class PendingAttachmentResolutionState(
    val draftEditorState: DraftEditorState,
    val didResolveAttachment: Boolean,
    val didDropAttachments: Boolean,
)
