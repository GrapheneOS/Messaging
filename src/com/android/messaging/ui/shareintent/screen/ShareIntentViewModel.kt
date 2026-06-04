package com.android.messaging.ui.shareintent.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import com.android.messaging.ui.shareintent.screen.delegate.ShareDraftDelegate
import com.android.messaging.ui.shareintent.screen.delegate.ShareTargetsDelegate
import com.android.messaging.ui.shareintent.screen.model.ShareDraftUiState
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetsUiState
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

internal interface ShareIntentScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ShareIntentViewModel @Inject constructor(
    private val targetsDelegate: ShareTargetsDelegate,
    private val draftDelegate: ShareDraftDelegate,
    private val resolveConversationId: ResolveConversationId,
) : ViewModel(),
    ShareIntentScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = combine(
        targetsDelegate.state,
        draftDelegate.state,
    ) { targetsState, draftState ->
        State(
            targets = targetsState,
            draft = draftState,
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
        draftDelegate.bind(viewModelScope, targetsDelegate.selectedIds)
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.TargetsAction -> onTargetsAction(action)
            is Action.DraftAction -> onDraftAction(action)
        }
    }

    private fun onTargetsAction(action: Action.TargetsAction) {
        when (action) {
            is Action.TargetClicked -> {
                onTargetClicked(action.target)
            }

            is Action.TargetLongPressed -> {
                targetsDelegate.toggleSelection(action.target)
            }

            is Action.SelectionToggled -> {
                targetsDelegate.toggleSelection(action.target)
            }

            Action.SelectionCleared -> {
                targetsDelegate.clearSelection()
            }

            Action.SendToSelectedClicked -> {
                draftDelegate.enterReview()
            }

            Action.NewMessageClicked -> {
                _effects.tryEmit(Effect.CreateNewConversation)
            }

            Action.SearchOpened -> {
                targetsDelegate.setSearchActive(true)
            }

            Action.SearchClosed -> {
                targetsDelegate.setSearchActive(false)
            }

            is Action.SearchQueryChanged -> {
                targetsDelegate.setSearchQuery(action.query)
            }

            Action.LoadMoreContacts -> {
                targetsDelegate.loadMoreContacts()
            }

            Action.ContactsPermissionGranted -> {
                targetsDelegate.onContactsPermissionGranted()
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

            Action.DraftSubjectCleared -> {
                draftDelegate.clearDraftSubject()
            }

            Action.ReviewDismissed -> {
                draftDelegate.exitReview()
            }

            Action.ConfirmSendClicked -> {
                _effects.tryEmit(
                    Effect.SendToSelected(
                        targets = currentSendTargets(),
                        draft = draftDelegate.currentDraft(),
                    ),
                )
            }
        }
    }

    private fun isSendEnabled(
        targets: ShareTargetsUiState,
        draft: ShareDraftUiState,
    ): Boolean {
        val hasDraftContent = draft.text.isNotBlank() ||
            draft.subjectText.isNotBlank() ||
            draft.attachments.isNotEmpty()

        return hasDraftContent && targets.selectedIds.isNotEmpty()
    }

    private fun onTargetClicked(target: ShareTargetUiState) {
        when (target) {
            is ShareTargetUiState.Conversation -> {
                _effects.tryEmit(Effect.OpenConversation(target.conversationId))
            }

            is ShareTargetUiState.Contact -> {
                openContactConversation(destination = target.destination)
            }
        }
    }

    private fun openContactConversation(destination: String) {
        viewModelScope.launch {
            val result = resolveConversationId(destinations = listOf(destination))

            if (result is ResolveConversationIdResult.Resolved) {
                _effects.tryEmit(Effect.OpenConversation(result.conversationId))
            }
        }
    }

    private fun currentSendTargets(): ImmutableSet<ShareSendTarget> {
        return targetsDelegate.currentSelectedTargets()
            .map(::toSendTarget)
            .toImmutableSet()
    }

    private fun toSendTarget(target: ShareTargetUiState): ShareSendTarget {
        return when (target) {
            is ShareTargetUiState.Conversation -> {
                ShareSendTarget.Conversation(conversationId = target.conversationId)
            }

            is ShareTargetUiState.Contact -> {
                ShareSendTarget.Contact(destination = target.destination)
            }
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
