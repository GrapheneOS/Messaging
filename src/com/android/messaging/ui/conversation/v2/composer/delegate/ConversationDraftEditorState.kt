package com.android.messaging.ui.conversation.v2.composer.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.ui.conversation.v2.composer.model.ConversationDraftState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class DraftEditorState(
    val conversationId: String? = null,
    val persistedDraft: ConversationDraft = ConversationDraft(),
    private val localEdits: ConversationDraftEdits = ConversationDraftEdits(),
    val isLoaded: Boolean = false,
    val isSending: Boolean = false,
    val pendingAttachments: List<ConversationDraftPendingAttachment> = emptyList(),
    val pendingSentDraft: ConversationDraft? = null,
) {
    val effectiveDraft: ConversationDraft
        get() = localEdits.applyTo(baseDraft = persistedDraft)

    val visibleState: ConversationDraftState
        get() {
            return when {
                conversationId == null -> ConversationDraftState()

                else -> {
                    ConversationDraftState(
                        draft = effectiveDraft.copy(
                            isCheckingDraft = !isLoaded,
                            isSending = isSending,
                        ),
                        pendingAttachments = pendingAttachments,
                    )
                }
            }
        }

    fun withPersistedDraft(persistedDraft: ConversationDraft): DraftEditorState {
        return when {
            pendingSentDraft != null -> {
                withPersistedDraftWhileAwaitingSentDraftClear(
                    persistedDraft = persistedDraft,
                    sentDraftAwaitingClear = pendingSentDraft,
                )
            }

            else -> {
                copy(
                    persistedDraft = persistedDraft,
                    localEdits = localEdits.normalizedAgainst(
                        baseDraft = persistedDraft,
                    ),
                    isLoaded = true,
                )
            }
        }
    }

    fun withMessageText(messageText: String): DraftEditorState {
        return when {
            conversationId == null -> this
            effectiveDraft.messageText == messageText -> this

            else -> copyWithNormalizedLocalEdits(
                updatedLocalEdits = localEdits.copy(messageText = messageText),
            )
        }
    }

    fun withSeededDraft(draft: ConversationDraft): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        val normalizedDraft = draft.copy(
            selfParticipantId = when {
                draft.selfParticipantId.isBlank() -> persistedDraft.selfParticipantId
                else -> draft.selfParticipantId
            },
        )

        return copyWithNormalizedLocalEdits(
            updatedLocalEdits = ConversationDraftEdits(
                messageText = normalizedDraft.messageText,
                subjectText = normalizedDraft.subjectText,
                selfParticipantId = normalizedDraft.selfParticipantId,
                attachments = normalizedDraft.attachments,
            ),
        )
    }

    fun toSaveRequestOrNull(): DraftSaveRequest? {
        return when {
            conversationId == null -> null
            !isLoaded || isSending || !localEdits.hasChanges -> null

            else -> {
                DraftSaveRequest(
                    conversationId = conversationId,
                    draft = effectiveDraft,
                )
            }
        }
    }

    fun withAttachmentsAdded(
        attachments: Collection<ConversationDraftAttachment>,
    ): DraftEditorState {
        if (conversationId == null || attachments.isEmpty()) {
            return this
        }

        val mergedAttachments = mergeDraftAttachments(
            baseAttachments = effectiveDraft.attachments,
            attachmentsToAdd = attachments,
        )

        return when {
            mergedAttachments == effectiveDraft.attachments -> this

            else -> {
                copyWithNormalizedLocalEdits(
                    updatedLocalEdits = localEdits.copy(
                        attachments = mergedAttachments,
                    ),
                )
            }
        }
    }

    fun withAttachmentRemoved(contentUri: String): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        val attachmentIndex = effectiveDraft.attachments.indexOfFirst { attachment ->
            attachment.contentUri == contentUri
        }

        if (attachmentIndex == -1) {
            return this
        }

        val updatedAttachments = effectiveDraft.attachments.toMutableList().apply {
            removeAt(attachmentIndex)
        }.toImmutableList()

        return copyWithNormalizedLocalEdits(
            updatedLocalEdits = localEdits.copy(attachments = updatedAttachments),
        )
    }

    fun withAttachmentCaption(
        contentUri: String,
        captionText: String,
    ): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        val currentAttachments = effectiveDraft.attachments
        val attachmentIndex = currentAttachments.indexOfFirst { attachment ->
            attachment.contentUri == contentUri
        }
        if (attachmentIndex == -1) {
            return this
        }

        val currentAttachment = currentAttachments[attachmentIndex]
        if (currentAttachment.captionText == captionText) {
            return this
        }

        val updatedAttachments = currentAttachments.toMutableList().apply {
            this[attachmentIndex] = currentAttachment.copy(captionText = captionText)
        }.toImmutableList()

        return copyWithNormalizedLocalEdits(
            updatedLocalEdits = localEdits.copy(attachments = updatedAttachments),
        )
    }

    fun withPendingAttachmentAdded(
        pendingAttachment: ConversationDraftPendingAttachment,
    ): DraftEditorState {
        if (conversationId == null) {
            return this
        }

        val updatedPendingAttachments = pendingAttachments + pendingAttachment

        return copy(pendingAttachments = updatedPendingAttachments)
    }

    fun withPendingAttachmentRemoved(pendingAttachmentId: String): DraftEditorState {
        val pendingAttachmentIndex = pendingAttachments.indexOfFirst { pendingAttachment ->
            pendingAttachment.pendingAttachmentId == pendingAttachmentId
        }

        if (pendingAttachmentIndex == -1) {
            return this
        }

        val updatedPendingAttachments = pendingAttachments.toMutableList().apply {
            removeAt(pendingAttachmentIndex)
        }

        return copy(pendingAttachments = updatedPendingAttachments)
    }

    fun withPendingAttachmentResolved(
        pendingAttachmentId: String,
        attachment: ConversationDraftAttachment,
    ): DraftEditorState {
        val updatedState = withPendingAttachmentRemoved(pendingAttachmentId)

        return updatedState.withAttachmentsAdded(listOf(attachment))
    }

    fun canSendDraft(): Boolean {
        return conversationId != null &&
            isLoaded &&
            !isSending &&
            pendingAttachments.isEmpty() &&
            effectiveDraft.hasContent
    }

    fun markPersistedIfUnchanged(saveRequest: DraftSaveRequest): DraftEditorState {
        return when {
            conversationId != saveRequest.conversationId -> this

            effectiveDraft != saveRequest.draft -> this

            else -> copy(
                persistedDraft = saveRequest.draft,
                localEdits = ConversationDraftEdits(),
                isLoaded = true,
                pendingSentDraft = null,
            )
        }
    }

    fun matchesSaveRequest(saveRequest: DraftSaveRequest): Boolean {
        return when {
            conversationId != saveRequest.conversationId -> false
            !isLoaded || isSending || !localEdits.hasChanges -> false
            else -> effectiveDraft == saveRequest.draft
        }
    }

    fun markSending(): DraftEditorState {
        return when {
            conversationId == null -> this
            isSending -> this
            else -> copy(isSending = true)
        }
    }

    fun markIdle(): DraftEditorState {
        if (!isSending) {
            return this
        }

        return copy(isSending = false)
    }

    fun clearDraftAfterSend(sentDraft: ConversationDraft): DraftEditorState {
        val latestEffectiveDraft = effectiveDraft

        val clearedDraft = createClearedDraftForSentDraft(sentDraft)

        val visibleDraftAfterSend = when {
            latestEffectiveDraft == sentDraft -> clearedDraft

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
        val currentEffectiveDraft = effectiveDraft

        if (persistedDraft == sentDraftAwaitingClear) {
            return rebaseVisibleDraftOnPersistedDraft(
                persistedDraft = persistedDraft,
                shouldKeepPendingSentDraft = true,
            )
        }

        val clearedDraft = createClearedDraftForSentDraft(sentDraftAwaitingClear)
        if (currentEffectiveDraft == clearedDraft) {
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
        return copy(
            persistedDraft = persistedDraft,
            localEdits = createConversationDraftEdits(
                baseDraft = persistedDraft,
                targetDraft = effectiveDraft,
            ),
            isLoaded = true,
            pendingSentDraft = pendingSentDraft.takeIf { shouldKeepPendingSentDraft },
        )
    }

    private fun copyWithNormalizedLocalEdits(
        updatedLocalEdits: ConversationDraftEdits,
    ): DraftEditorState {
        return copy(
            localEdits = updatedLocalEdits.normalizedAgainst(
                baseDraft = persistedDraft,
            ),
        )
    }
}

internal data class DraftSaveRequest(
    val conversationId: String,
    val draft: ConversationDraft,
)

internal data class DraftSendRequest(
    val conversationId: String,
    val draft: ConversationDraft,
)

internal data class PersistedDraftUpdate(
    val conversationId: String,
    val persistedDraft: ConversationDraft,
)

internal data class ConversationDraftEdits(
    val messageText: String? = null,
    val subjectText: String? = null,
    val selfParticipantId: String? = null,
    val attachments: ImmutableList<ConversationDraftAttachment>? = null,
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
            messageText = messageText?.takeIf { it != baseDraft.messageText },
            subjectText = subjectText?.takeIf { it != baseDraft.subjectText },
            selfParticipantId = selfParticipantId?.takeIf { it != baseDraft.selfParticipantId },
            attachments = attachments?.takeIf { it != baseDraft.attachments },
        )
    }
}

private fun mergeDraftAttachments(
    baseAttachments: ImmutableList<ConversationDraftAttachment>,
    attachmentsToAdd: Collection<ConversationDraftAttachment>,
): ImmutableList<ConversationDraftAttachment> {
    if (attachmentsToAdd.isEmpty()) {
        return baseAttachments
    }

    val seenContentUris = baseAttachments
        .asSequence()
        .map { attachment -> attachment.contentUri }
        .toHashSet()

    val attachmentsToAppend = attachmentsToAdd.filter { attachment ->
        seenContentUris.add(attachment.contentUri)
    }

    return when {
        attachmentsToAppend.isEmpty() -> baseAttachments
        else -> (baseAttachments + attachmentsToAppend).toImmutableList()
    }
}

private fun createClearedDraftForSentDraft(
    sentDraft: ConversationDraft,
): ConversationDraft {
    return ConversationDraft(
        selfParticipantId = sentDraft.selfParticipantId,
    )
}

private fun createConversationDraftEdits(
    baseDraft: ConversationDraft,
    targetDraft: ConversationDraft,
): ConversationDraftEdits {
    return ConversationDraftEdits(
        messageText = targetDraft.messageText.takeIf { it != baseDraft.messageText },
        subjectText = targetDraft.subjectText.takeIf { it != baseDraft.subjectText },
        selfParticipantId = targetDraft.selfParticipantId.takeIf {
            it != baseDraft.selfParticipantId
        },
        attachments = targetDraft.attachments.takeIf { it != baseDraft.attachments },
    )
}
