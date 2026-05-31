package com.android.messaging.ui.conversation.entry.newchat

import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.ui.conversation.entry.model.NewChatEffect
import com.android.messaging.ui.recipientselection.model.picker.ConversationResolutionState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class NewChatViewModelGroupConfirmationTest : BaseNewChatViewModelTest() {

    @Test
    fun onCreateGroupConfirmed_withAcceptedRecipients_resolvesAllDestinationsAsGroup() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selected = createSelectedRecipientsDelegateMock(
                recipients = persistentListOf(
                    selectedRecipient(destination = DESTINATION),
                    selectedRecipient(destination = DESTINATION_2),
                ),
            )
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.onCreateGroupConfirmed()
            advanceUntilIdle()

            verify(exactly = 1) {
                resolution.mock.resolve(
                    destinations = listOf(DESTINATION, DESTINATION_2),
                    recipientDestination = null,
                )
            }
        }
    }

    @Test
    fun onCreateGroupConfirmed_withNoRecipients_showsMessageAndDoesNotResolve() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selected = createSelectedRecipientsDelegateMock(recipients = persistentListOf())
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.effects.test {
                viewModel.onCreateGroupConfirmed()
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.ShowMessage(messageResId = R.string.too_many_participants),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 0) {
                resolution.mock.resolve(destinations = any(), recipientDestination = any())
            }
        }
    }

    @Test
    fun onCreateGroupConfirmed_whenRecipientLimitExceeded_showsMessageAndDoesNotResolve() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selected = createSelectedRecipientsDelegateMock(
                recipients = persistentListOf(selectedRecipient(destination = DESTINATION)),
            )
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(
                isConversationRecipientLimitExceeded = createRecipientLimitExceeded(
                    exceeded = true
                ),
                selectedRecipientsDelegate = selected.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.effects.test {
                viewModel.onCreateGroupConfirmed()
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.ShowMessage(messageResId = R.string.too_many_participants),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 0) {
                resolution.mock.resolve(destinations = any(), recipientDestination = any())
            }
        }
    }

    @Test
    fun onCreateGroupConfirmed_whenNotCreatingGroup_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selected = createSelectedRecipientsDelegateMock(
                recipients = persistentListOf(selectedRecipient(destination = DESTINATION)),
            )
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()

            viewModel.onCreateGroupConfirmed()
            advanceUntilIdle()

            verify(exactly = 0) {
                resolution.mock.resolve(destinations = any(), recipientDestination = any())
            }
        }
    }

    @Test
    fun onCreateGroupConfirmed_whileResolving_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selected = createSelectedRecipientsDelegateMock(
                recipients = persistentListOf(selectedRecipient(destination = DESTINATION)),
            )
            val resolution = createResolutionDelegateMock(
                state = ConversationResolutionState.Resolving(
                    recipientDestination = null,
                    isIndicatorVisible = false,
                ),
            )
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selected.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.onCreateGroupConfirmed()
            advanceUntilIdle()

            verify(exactly = 0) {
                resolution.mock.resolve(destinations = any(), recipientDestination = any())
            }
        }
    }
}
