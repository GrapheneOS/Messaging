package com.android.messaging.ui.conversation.entry.newchat

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.ui.conversation.entry.model.NewChatEffect
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class NewChatViewModelGroupCreationTest : BaseNewChatViewModelTest() {

    @Test
    fun onCreateGroupRequested_entersGroupModeCancelsResolutionAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(
                conversationResolutionDelegate = resolution.mock,
                selectedRecipientsDelegate = selected.mock,
            )

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onCreateGroupRequested()
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isCreatingGroup)
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { resolution.mock.cancel() }
            verify(exactly = 1) { selected.mock.clear() }
        }
    }

    @Test
    fun onCreateGroupRequested_whenAlreadyCreatingGroup_cancelsButDoesNotClearSelectionAgain() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(
                conversationResolutionDelegate = resolution.mock,
                selectedRecipientsDelegate = selected.mock,
            )
            advanceUntilIdle()

            viewModel.onCreateGroupRequested()
            viewModel.onCreateGroupRequested()
            advanceUntilIdle()

            verify(exactly = 1) { selected.mock.clear() }
            verify(exactly = 2) { resolution.mock.cancel() }
        }
    }

    @Test
    fun onContactLongClicked_whenNotCreatingGroup_startsGroupWithRecipient() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock()
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                recipientPickerDelegate = recipientPicker.mock,
            )

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onContactLongClicked(recipient = recipient)
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isCreatingGroup)
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { selected.mock.replaceWith(recipient = recipient) }
            verify(exactly = 1) { recipientPicker.mock.clearQuery() }
        }
    }

    @Test
    fun onContactLongClicked_whenNotCreatingGroup_withBlankDestination_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val blankRecipient = selectedRecipient(destination = "   ")
            val selected = createSelectedRecipientsDelegateMock()
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                recipientPickerDelegate = recipientPicker.mock,
            )
            advanceUntilIdle()

            viewModel.onContactLongClicked(recipient = blankRecipient)
            advanceUntilIdle()

            verify(exactly = 0) { selected.mock.replaceWith(recipient = any()) }
            verify(exactly = 0) { recipientPicker.mock.clearQuery() }
        }
    }

    @Test
    fun onContactLongClicked_whenStartingGroupExceedsLimit_showsMessageAndDoesNotStartGroup() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(
                isConversationRecipientLimitExceeded = createRecipientLimitExceeded(
                    exceeded = true
                ),
                selectedRecipientsDelegate = selected.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onContactLongClicked(recipient = recipient)
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.ShowMessage(messageResId = R.string.too_many_participants),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 0) { selected.mock.replaceWith(recipient = any()) }
        }
    }

    @Test
    fun onContactLongClicked_whileResolving_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock()
            val resolution = createResolutionDelegateMock(
                state = ConversationResolutionState.Resolving(
                    recipientDestination = DESTINATION,
                    isIndicatorVisible = false,
                ),
            )
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()

            viewModel.onContactLongClicked(recipient = recipient)
            advanceUntilIdle()

            verify(exactly = 0) { selected.mock.replaceWith(recipient = any()) }
            verify(exactly = 0) { selected.mock.toggle(recipient = any(), canAdd = any()) }
        }
    }

    @Test
    fun onContactLongClicked_whileCreatingGroup_togglesRecipient() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock(
                toggleOutcome = RecipientToggleOutcome.Added,
            )
            val viewModel = createViewModel(selectedRecipientsDelegate = selected.mock)
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.onContactLongClicked(recipient = recipient)
            advanceUntilIdle()

            verify(exactly = 1) { selected.mock.toggle(recipient = recipient, canAdd = any()) }
        }
    }

    @Test
    fun onCreateGroupRecipientClicked_whenRecipientAdded_clearsRecipientPickerQuery() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock(
                toggleOutcome = RecipientToggleOutcome.Added,
            )
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                recipientPickerDelegate = recipientPicker.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.onCreateGroupRecipientClicked(recipient = recipient)
            advanceUntilIdle()

            verify(exactly = 1) { selected.mock.toggle(recipient = recipient, canAdd = any()) }
            verify(exactly = 1) { recipientPicker.mock.clearQuery() }
        }
    }

    @Test
    fun onCreateGroupRecipientClicked_whenOverLimit_showsMessageAndKeepsQuery() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock(
                toggleOutcome = RecipientToggleOutcome.OverLimit,
            )
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                recipientPickerDelegate = recipientPicker.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.effects.test {
                viewModel.onCreateGroupRecipientClicked(recipient = recipient)
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.ShowMessage(messageResId = R.string.too_many_participants),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 0) { recipientPicker.mock.clearQuery() }
        }
    }

    @Test
    fun onCreateGroupRecipientClicked_whenRecipientRemoved_doesNotClearQueryOrShowMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock(
                toggleOutcome = RecipientToggleOutcome.Removed,
            )
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                recipientPickerDelegate = recipientPicker.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.effects.test {
                viewModel.onCreateGroupRecipientClicked(recipient = recipient)
                advanceUntilIdle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 0) { recipientPicker.mock.clearQuery() }
        }
    }

    @Test
    fun onCreateGroupRecipientClicked_whenNotCreatingGroup_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipient = selectedRecipient(destination = DESTINATION)
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(selectedRecipientsDelegate = selected.mock)
            advanceUntilIdle()

            viewModel.onCreateGroupRecipientClicked(recipient = recipient)
            advanceUntilIdle()

            verify(exactly = 0) { selected.mock.toggle(recipient = any(), canAdd = any()) }
        }
    }

    @Test
    fun onCreateGroupRecipientClicked_withBlankDestination_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val blankRecipient = selectedRecipient(destination = "   ")
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(selectedRecipientsDelegate = selected.mock)
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.onCreateGroupRecipientClicked(recipient = blankRecipient)
            advanceUntilIdle()

            verify(exactly = 0) { selected.mock.toggle(recipient = any(), canAdd = any()) }
        }
    }

    @Test
    fun isCreatingGroupState_survivesViewModelRecreationViaSavedStateHandle() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val savedStateHandle = SavedStateHandle()
            val firstViewModel = createViewModel(savedStateHandle = savedStateHandle)
            advanceUntilIdle()
            firstViewModel.onCreateGroupRequested()
            advanceUntilIdle()

            val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

            recreatedViewModel.uiState.test {
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isCreatingGroup)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
