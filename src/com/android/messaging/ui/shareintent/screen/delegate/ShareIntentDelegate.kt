package com.android.messaging.ui.shareintent.screen.delegate

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.shareintent.repository.ShareTargetsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.shareintent.screen.mapper.ShareTargetUiStateMapper
import com.android.messaging.ui.shareintent.screen.model.ShareAttachmentUiModel
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.ui.shareintent.screen.model.toShareAttachmentUiModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ShareIntentScreenDelegate {
    val state: Flow<State>
    fun bind(scope: CoroutineScope)
    fun setSearchActive(active: Boolean)
    fun setSearchQuery(query: String)
    fun toggleSelection(conversationId: String)
    fun clearSelection()
    fun currentSelection(): ImmutableSet<String>
    fun resolveDraft(draft: ConversationDraft?)
    fun setDraftText(text: String)
    fun removeDraftAttachment(id: String)
    fun enterReview()
    fun exitReview()
    fun currentDraft(): ConversationDraft
}

internal class ShareIntentScreenDelegateImpl @Inject constructor(
    private val repository: ShareTargetsRepository,
    private val mapper: ShareTargetUiStateMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ShareIntentScreenDelegate {

    private val targets = MutableStateFlow<ImmutableList<ShareTargetUiState>?>(null)
    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<PersistentSet<String>>(persistentSetOf())
    private val draft = MutableStateFlow(ConversationDraft())
    private val isReviewing = MutableStateFlow(false)
    private val isDraftLoading = MutableStateFlow(true)

    private var isBound = false

    private val draftState: Flow<DraftState> =
        combine(draft, isReviewing, isDraftLoading) { current, reviewing, loading ->
            DraftState(
                text = current.messageText,
                attachments = current.attachments
                    .map(ConversationDraftAttachment::toShareAttachmentUiModel)
                    .toImmutableList(),
                isReviewing = reviewing,
                isLoading = loading,
            )
        }

    override val state: Flow<State> = combine(
        targets,
        isSearchActive,
        searchQuery,
        selectedIds,
        draftState,
    ) { allTargets, active, query, selected, draftBundle ->
        val isSendEnabled = (draftBundle.text.isNotBlank() || draftBundle.attachments.isNotEmpty()) &&
            selected.isNotEmpty()

        when (allTargets) {
            null -> State(
                isSearchActive = active,
                isReviewing = draftBundle.isReviewing,
                draftText = draftBundle.text,
                draftAttachments = draftBundle.attachments,
                isSendEnabled = isSendEnabled,
            )

            else -> State(
                isLoading = draftBundle.isLoading,
                targets = filterTargets(allTargets, query),
                isSearchActive = active,
                selectedConversationIds = selected,
                selectedTargets = selectedTargets(allTargets, selected),
                isReviewing = draftBundle.isReviewing,
                draftText = draftBundle.text,
                draftAttachments = draftBundle.attachments,
                isSendEnabled = isSendEnabled,
            )
        }
    }.flowOn(defaultDispatcher)

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch(defaultDispatcher) {
            repository.observeShareTargets()
                .map(mapper::map)
                .collect { mapped ->
                    targets.value = mapped
                    pruneSelection(mapped)
                }
        }
    }

    override fun setSearchActive(active: Boolean) {
        isSearchActive.value = active

        if (!active) {
            searchQuery.value = ""
        }
    }

    override fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    override fun toggleSelection(conversationId: String) {
        selectedIds.update { selected ->
            when (conversationId) {
                in selected -> selected.remove(conversationId)
                else -> selected.add(conversationId)
            }
        }
        exitReviewIfSelectionEmpty()
    }

    override fun clearSelection() {
        selectedIds.value = persistentSetOf()
        exitReviewIfSelectionEmpty()
    }

    override fun currentSelection(): ImmutableSet<String> {
        return selectedIds.value
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

    override fun removeDraftAttachment(id: String) {
        draft.update { current ->
            current.copy(
                attachments = current.attachments
                    .filterNot { attachment -> attachment.contentUri == id }
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

    private fun pruneSelection(available: ImmutableList<ShareTargetUiState>) {
        if (selectedIds.value.isEmpty()) {
            return
        }

        val availableIds = available.mapTo(HashSet(available.size)) { it.conversationId }
        selectedIds.update { selected ->
            selected.retainAll(availableIds)
        }
        exitReviewIfSelectionEmpty()
    }

    private fun exitReviewIfSelectionEmpty() {
        if (selectedIds.value.isEmpty()) {
            isReviewing.value = false
        }
    }

    private fun selectedTargets(
        targets: ImmutableList<ShareTargetUiState>,
        selected: ImmutableSet<String>,
    ): ImmutableList<ShareTargetUiState> {
        if (selected.isEmpty()) {
            return persistentListOf()
        }

        val targetsById = targets.associateBy { it.conversationId }
        return selected
            .mapNotNull { targetsById[it] }
            .toImmutableList()
    }

    private fun filterTargets(
        targets: ImmutableList<ShareTargetUiState>,
        query: String,
    ): ImmutableList<ShareTargetUiState> {
        if (query.isBlank()) {
            return targets
        }

        return targets
            .filter { target ->
                target.matches(query)
            }
            .toImmutableList()
    }

    private fun ShareTargetUiState.matches(query: String): Boolean {
        return displayName.contains(query, ignoreCase = true) ||
            details?.contains(query, ignoreCase = true) == true
    }

    private data class DraftState(
        val text: String,
        val attachments: ImmutableList<ShareAttachmentUiModel>,
        val isReviewing: Boolean,
        val isLoading: Boolean,
    )
}
