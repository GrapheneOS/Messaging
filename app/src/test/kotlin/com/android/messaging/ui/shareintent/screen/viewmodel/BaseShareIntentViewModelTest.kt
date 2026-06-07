package com.android.messaging.ui.shareintent.screen.viewmodel

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.shareintent.screen.ShareIntentViewModel
import com.android.messaging.ui.shareintent.screen.delegate.ShareDraftDelegate
import com.android.messaging.ui.shareintent.screen.delegate.ShareTargetsDelegate
import com.android.messaging.ui.shareintent.screen.model.ShareDraftUiState
import com.android.messaging.ui.shareintent.screen.model.ShareSelectionUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetsUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseShareIntentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected val targetsState = MutableStateFlow(ShareTargetsUiState())
    protected val draftState = MutableStateFlow(ShareDraftUiState())
    protected val selectedIds = MutableStateFlow(persistentSetOf<String>())

    protected val targetsDelegate = mockk<ShareTargetsDelegate>(relaxed = true) {
        every { state } returns targetsState
        every { this@mockk.selectedIds } returns this@BaseShareIntentViewModelTest.selectedIds
        every { currentSelectedTargets() } returns persistentListOf()
    }

    protected val draftDelegate = mockk<ShareDraftDelegate>(relaxed = true) {
        every { state } returns draftState
        every { currentDraft() } returns ConversationDraft()
    }

    protected val resolveConversationId = mockk<ResolveConversationId>()

    protected fun createViewModel(): ShareIntentViewModel {
        return ShareIntentViewModel(
            targetsDelegate = targetsDelegate,
            draftDelegate = draftDelegate,
            resolveConversationId = resolveConversationId,
        )
    }

    protected fun givenSelectedTargets(targets: List<ShareTargetUiState>) {
        every { targetsDelegate.currentSelectedTargets() } returns targets.toImmutableList()
        selectedIds.value = targets.map { it.selectionId }.toImmutableSet().let {
            persistentSetOf<String>().addAll(it)
        }
    }

    protected fun givenResolvedConversation(
        destination: String,
        conversationId: String,
    ) {
        coEvery { resolveConversationId(listOf(destination)) } returns
            ResolveConversationIdResult.Resolved(conversationId)
    }

    protected fun givenUnresolvedConversation(
        destination: String,
        result: ResolveConversationIdResult = ResolveConversationIdResult.NotResolved,
    ) {
        coEvery { resolveConversationId(listOf(destination)) } returns result
    }

    protected fun setSelection(selectedTargets: List<ShareTargetUiState>) {
        targetsState.value = targetsState.value.copy(
            selection = ShareSelectionUiState(
                selectedIds = selectedTargets.map { it.selectionId }
                    .toImmutableSet()
                    .let { persistentSetOf<String>().addAll(it) },
                selectedTargets = selectedTargets.toImmutableList(),
            ),
        )
    }

    protected fun conversationTarget(
        conversationId: String,
        normalizedDestination: String? = null,
    ): ShareTargetUiState.Conversation {
        return ShareTargetUiState.Conversation(
            conversationId = conversationId,
            normalizedDestination = normalizedDestination,
            displayName = "Conversation $conversationId",
            details = null,
            avatarUri = null,
            isGroup = false,
        )
    }

    protected fun contactTarget(
        contactId: Long,
        destination: String,
    ): ShareTargetUiState.Contact {
        return ShareTargetUiState.Contact(
            contactId = contactId,
            destination = destination,
            normalizedDestination = destination,
            displayName = "Contact $contactId",
            details = null,
            avatarUri = null,
        )
    }
}
