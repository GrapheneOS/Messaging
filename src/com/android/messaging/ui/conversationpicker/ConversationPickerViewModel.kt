package com.android.messaging.ui.conversationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.ui.conversationpicker.delegate.DraftDelegate
import com.android.messaging.ui.conversationpicker.delegate.TargetsDelegate
import com.android.messaging.ui.conversationpicker.mapper.ContactTargetMapper
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.ConversationPickerEffect as Effect
import com.android.messaging.ui.conversationpicker.model.ConversationPickerUiState as State
import com.android.messaging.ui.conversationpicker.model.DraftUiState
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.conversationpicker.model.TargetsUiState
import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegate
import com.android.messaging.ui.subscription.delegate.SimSelectionDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface ConversationPickerScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ConversationPickerViewModel @Inject constructor(
    private val targetsDelegate: TargetsDelegate,
    private val recipientPickerDelegate: RecipientPickerDelegate,
    private val draftDelegate: DraftDelegate,
    private val simSelectionDelegate: SimSelectionDelegate,
    private val resolveConversationId: ResolveConversationId,
    private val contactTargetMapper: ContactTargetMapper,
) : ViewModel(),
    ConversationPickerScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = combine(
        targetsDelegate.state,
        recipientPickerDelegate.state,
        draftDelegate.state,
        simSelectionDelegate.state,
    ) { targetsState, contactsState, draftState, simState ->
        State(
            targets = targetsState,
            contacts = contactsState,
            draft = draftState,
            sim = simState,
            isSendEnabled = isSendEnabled(targetsState, draftState),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
        ),
        initialValue = State(),
    )

    init {
        targetsDelegate.bind(viewModelScope)
        recipientPickerDelegate.bind(viewModelScope)
        draftDelegate.bind(viewModelScope, targetsDelegate.selectedIds)
        simSelectionDelegate.bind(viewModelScope)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.TargetsAction -> onTargetsAction(action)
            is Action.DraftAction -> onDraftAction(action)
            is Action.SimSelected -> simSelectionDelegate.select(action.selfParticipantId)
        }
    }

    private fun onTargetsAction(action: Action.TargetsAction) {
        when (action) {
            is Action.TargetClicked -> {
                onTargetClicked(action.target)
            }

            is Action.SelectionToggled -> {
                targetsDelegate.toggleSelection(action.target)
            }

            is Action.ContactDestinationClicked -> {
                openContactConversation(action.destination)
            }

            is Action.ContactDestinationToggled -> {
                val target = contactTargetMapper.map(
                    item = action.item,
                    destination = action.destination,
                )
                targetsDelegate.toggleSelection(target)
            }

            Action.SelectionCleared -> {
                targetsDelegate.clearSelection()
            }

            Action.ProceedToReviewClicked -> {
                draftDelegate.enterReview()
            }

            Action.SearchOpened -> {
                targetsDelegate.setSearchActive(true)
            }

            Action.SearchClosed -> {
                targetsDelegate.setSearchActive(false)
                recipientPickerDelegate.clearQuery()
            }

            is Action.SearchQueryChanged -> {
                targetsDelegate.setSearchQuery(action.query)
                recipientPickerDelegate.onQueryChanged(action.query)
            }

            Action.LoadMoreContacts -> {
                recipientPickerDelegate.onLoadMore()
            }

            Action.LoadMoreRecent -> {
                targetsDelegate.loadMoreRecent()
            }

            Action.CollapseRecent -> {
                targetsDelegate.collapseRecent()
            }

            Action.ContactsPermissionGranted -> {
                recipientPickerDelegate.refresh()
            }
        }
    }

    private fun onDraftAction(action: Action.DraftAction) {
        when (action) {
            is Action.DraftResolved -> {
                draftDelegate.resolveDraft(action.draft)
            }

            is Action.DraftTextChanged -> {
                draftDelegate.setDraftText(action.text)
            }

            is Action.DraftAttachmentRemoved -> {
                draftDelegate.removeDraftAttachment(action.id)
            }

            is Action.DraftAttachmentClicked -> {
                openAttachmentPreview(action.id)
            }

            Action.DraftSubjectCleared -> {
                draftDelegate.clearDraftSubject()
            }

            Action.ReviewDismissed -> {
                draftDelegate.exitReview()
            }

            Action.SendClicked -> {
                val selfParticipantId = simSelectionDelegate
                    .currentSelectedSelfParticipantId()
                    .orEmpty()

                _effects.tryEmit(
                    Effect.SendToSelected(
                        targets = currentSendTargets(),
                        draft = draftDelegate.currentDraft().copy(
                            selfParticipantId = selfParticipantId,
                        ),
                    ),
                )
            }
        }
    }

    private fun openAttachmentPreview(id: String) {
        val attachment = draftDelegate.currentDraft().attachments
            .firstOrNull { attachment -> attachment.contentUri == id }
            ?: return

        _effects.tryEmit(
            Effect.OpenAttachmentPreview(
                contentUri = attachment.contentUri,
                contentType = attachment.contentType,
            ),
        )
    }

    private fun isSendEnabled(
        targets: TargetsUiState,
        draft: DraftUiState,
    ): Boolean {
        val hasDraftContent = draft.text.isNotBlank() ||
            draft.subjectText.isNotBlank() ||
            draft.attachments.isNotEmpty()

        return hasDraftContent && targets.selection.selectedIds.isNotEmpty()
    }

    private fun onTargetClicked(target: TargetUiState) {
        when (target) {
            is TargetUiState.Conversation -> {
                _effects.tryEmit(Effect.OpenConversation(target.conversationId))
            }

            is TargetUiState.Contact -> {
                openContactConversation(target.destination)
            }
        }
    }

    private fun openContactConversation(destination: String) {
        viewModelScope.launch {
            val effect = when (val result = resolveConversationId(listOf(destination))) {
                is ResolveConversationIdResult.Resolved -> {
                    Effect.OpenConversation(result.conversationId)
                }

                else -> Effect.OpenConversationFailed
            }

            _effects.tryEmit(effect)
        }
    }

    private fun currentSendTargets(): ImmutableSet<SendTarget> {
        return targetsDelegate.currentSelectedTargets
            .map(::toSendTarget)
            .toImmutableSet()
    }

    private fun toSendTarget(target: TargetUiState): SendTarget {
        return when (target) {
            is TargetUiState.Conversation -> {
                SendTarget.Conversation(target.conversationId)
            }

            is TargetUiState.Contact -> {
                SendTarget.Contact(target.destination)
            }
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
