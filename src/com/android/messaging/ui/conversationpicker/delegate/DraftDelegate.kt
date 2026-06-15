package com.android.messaging.ui.conversationpicker.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversationpicker.model.DraftUiState
import com.android.messaging.ui.conversationpicker.model.toAttachmentUiModel
import com.android.messaging.ui.conversationpicker.model.toMediaPreviewItem
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface DraftDelegate {
    val state: StateFlow<DraftUiState>
    fun bind(scope: CoroutineScope, selectedIds: Flow<ImmutableSet<String>>)
    fun resolveDraft(draft: ConversationDraft?)
    fun setDraftText(text: String)
    fun clearDraftSubject()
    fun removeDraftAttachment(id: String)
    fun enterReview()
    fun exitReview()
    fun currentDraft(): ConversationDraft
}

internal class DraftDelegateImpl @Inject constructor(
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : DraftDelegate {

    private val draft = MutableStateFlow(ConversationDraft())
    private val isReviewing = MutableStateFlow(false)
    private val isDraftLoading = MutableStateFlow(true)

    private var isBound = false

    private val _state = MutableStateFlow(DraftUiState())
    override val state: StateFlow<DraftUiState> = _state.asStateFlow()

    override fun bind(
        scope: CoroutineScope,
        selectedIds: Flow<ImmutableSet<String>>,
    ) {
        if (isBound) return
        isBound = true

        scope.launch(defaultDispatcher) {
            combine(
                draft,
                isReviewing,
                isDraftLoading,
            ) { current, reviewing, loading ->
                DraftUiState(
                    isLoading = loading,
                    isReviewing = reviewing,
                    text = current.messageText,
                    subjectText = current.subjectText,
                    attachments = current.attachments
                        .map(ConversationDraftAttachment::toAttachmentUiModel)
                        .toImmutableList(),
                    backgroundItems = current.attachments
                        .map(ConversationDraftAttachment::toMediaPreviewItem)
                        .toImmutableList(),
                )
            }.collect { state ->
                _state.value = state
            }
        }

        scope.launch(defaultDispatcher) {
            selectedIds.collect { selected ->
                if (selected.isEmpty()) {
                    exitReview()
                }
            }
        }
    }

    override fun resolveDraft(draft: ConversationDraft?) {
        if (!isDraftLoading.value) {
            return
        }

        if (draft != null) {
            this.draft.value = draft
        }

        isDraftLoading.value = false
    }

    override fun setDraftText(text: String) {
        draft.update { current ->
            current.copy(messageText = text)
        }
    }

    override fun clearDraftSubject() {
        draft.update { current ->
            current.copy(subjectText = "")
        }
    }

    override fun removeDraftAttachment(id: String) {
        draft.update { current ->
            current.copy(
                attachments = current.attachments
                    .filterNot { attachment ->
                        attachment.contentUri == id
                    }
                    .toImmutableList(),
            )
        }
    }

    override fun enterReview() {
        isReviewing.value = true
    }

    override fun exitReview() {
        isReviewing.value = false
    }

    override fun currentDraft(): ConversationDraft {
        return draft.value
    }
}
