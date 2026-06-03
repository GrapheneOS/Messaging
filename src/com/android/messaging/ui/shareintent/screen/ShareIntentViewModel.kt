package com.android.messaging.ui.shareintent.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.shareintent.screen.delegate.ShareDraftDelegate
import com.android.messaging.ui.shareintent.screen.delegate.ShareTargetsDelegate
import com.android.messaging.ui.shareintent.screen.model.ShareIntentAction as Action
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal interface ShareIntentScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class ShareIntentViewModel @Inject constructor(
    private val targetsDelegate: ShareTargetsDelegate,
    private val draftDelegate: ShareDraftDelegate,
) : ViewModel(),
    ShareIntentScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = combine(
        targetsDelegate.state,
        draftDelegate.state,
    ) { targetsState, draftState ->
        val hasDraftContent = draftState.text.isNotBlank() ||
            draftState.subjectText.isNotBlank() ||
            draftState.attachments.isNotEmpty()

        State(
            isLoading = targetsState.isLoading || draftState.isLoading,
            targets = targetsState.targets,
            isSearchActive = targetsState.isSearchActive,
            selectedConversationIds = targetsState.selectedConversationIds,
            selectedTargets = targetsState.selectedTargets,
            isReviewing = draftState.isReviewing,
            draftText = draftState.text,
            draftSubject = draftState.subjectText,
            draftAttachments = draftState.attachments,
            isSendEnabled = hasDraftContent && targetsState.selectedConversationIds.isNotEmpty(),
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
                _effects.tryEmit(Effect.OpenConversation(action.conversationId))
            }

            is Action.TargetLongPressed -> {
                targetsDelegate.toggleSelection(action.conversationId)
            }

            is Action.SelectionToggled -> {
                targetsDelegate.toggleSelection(action.conversationId)
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
                        conversationIds = targetsDelegate.currentSelection(),
                        draft = draftDelegate.currentDraft(),
                    ),
                )
            }
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
