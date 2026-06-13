package com.android.messaging.ui.conversationpicker.viewmodel

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveConversationIdResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONTACT_DESTINATION
import com.android.messaging.testutil.TEST_RESOLVED_CONVERSATION_ID
import com.android.messaging.ui.conversationpicker.ConversationPickerViewModel
import com.android.messaging.ui.conversationpicker.delegate.DraftDelegate
import com.android.messaging.ui.conversationpicker.delegate.TargetsDelegate
import com.android.messaging.ui.conversationpicker.formatter.TargetTextFormatter
import com.android.messaging.ui.conversationpicker.mapper.ContactTargetMapperImpl
import com.android.messaging.ui.conversationpicker.model.DraftUiState
import com.android.messaging.ui.conversationpicker.model.SelectionUiState
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.conversationpicker.model.TargetsUiState
import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegate
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
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
internal abstract class BaseConversationPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected val targetsState = MutableStateFlow(TargetsUiState())
    protected val contactsState = MutableStateFlow(RecipientPickerUiState())
    protected val draftState = MutableStateFlow(DraftUiState())
    protected val selectedIds = MutableStateFlow(persistentSetOf<String>())

    protected val targetsDelegate = mockk<TargetsDelegate>(relaxed = true) {
        every { state } returns targetsState
        every {
            this@mockk.selectedIds
        } returns this@BaseConversationPickerViewModelTest.selectedIds
        every { currentSelectedTargets() } returns persistentListOf()
    }

    protected val recipientPickerDelegate = mockk<RecipientPickerDelegate>(relaxed = true) {
        every { state } returns contactsState
    }

    protected val textFormatter = mockk<TargetTextFormatter> {
        every { wrap(any()) } answers { firstArg() }
        every { detailsOrNull(any(), any()) } answers { secondArg() }
    }

    protected val contactTargetMapper = ContactTargetMapperImpl(textFormatter)

    protected val draftDelegate = mockk<DraftDelegate>(relaxed = true) {
        every { state } returns draftState
        every { currentDraft() } returns ConversationDraft()
    }

    protected val resolveConversationId = mockk<ResolveConversationId>()

    protected fun createViewModel(): ConversationPickerViewModel {
        return ConversationPickerViewModel(
            targetsDelegate = targetsDelegate,
            recipientPickerDelegate = recipientPickerDelegate,
            draftDelegate = draftDelegate,
            resolveConversationId = resolveConversationId,
            contactTargetMapper = contactTargetMapper,
        )
    }

    protected fun givenSelectedTargets(targets: List<TargetUiState>) {
        every { targetsDelegate.currentSelectedTargets() } returns targets.toImmutableList()
        selectedIds.value = targets.map { it.selectionId }.toImmutableSet().let {
            persistentSetOf<String>().addAll(it)
        }
    }

    protected fun givenResolvedConversation(
        destination: String = TEST_CONTACT_DESTINATION,
        conversationId: String = TEST_RESOLVED_CONVERSATION_ID,
    ) {
        coEvery { resolveConversationId(listOf(destination)) } returns
            ResolveConversationIdResult.Resolved(conversationId)
    }

    protected fun givenUnresolvedConversation(
        destination: String = TEST_CONTACT_DESTINATION,
        result: ResolveConversationIdResult = ResolveConversationIdResult.NotResolved,
    ) {
        coEvery { resolveConversationId(listOf(destination)) } returns result
    }

    protected fun setSelection(selectedTargets: List<TargetUiState>) {
        targetsState.value = targetsState.value.copy(
            selection = SelectionUiState(
                selectedIds = selectedTargets.map { it.selectionId }
                    .toImmutableSet()
                    .let { persistentSetOf<String>().addAll(it) },
                selectedTargets = selectedTargets.toImmutableList(),
            ),
        )
    }
}
