package com.android.messaging.ui.conversation.entry.newchat

import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.entry.model.NewChatEffect
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class NewChatViewModelConversationResolutionTest : BaseNewChatViewModelTest() {

    @Test
    fun onContactClicked_whenIdle_resolvesSingleDestination() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)
            advanceUntilIdle()

            viewModel.onContactClicked(destination = DESTINATION)

            verify(exactly = 1) {
                resolution.mock.resolve(
                    destinations = listOf(DESTINATION),
                    recipientDestination = DESTINATION,
                )
            }
        }
    }

    @Test
    fun onContactClicked_whileResolving_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock(
                state = ConversationResolutionState.Resolving(
                    recipientDestination = DESTINATION,
                    isIndicatorVisible = false,
                ),
            )
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)
            advanceUntilIdle()

            viewModel.onContactClicked(destination = DESTINATION)

            verify(exactly = 0) {
                resolution.mock.resolve(destinations = any(), recipientDestination = any())
            }
        }
    }

    @Test
    fun onContactClicked_whileCreatingGroup_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()

            viewModel.onContactClicked(destination = DESTINATION)

            verify(exactly = 0) {
                resolution.mock.resolve(destinations = any(), recipientDestination = any())
            }
        }
    }

    @Test
    fun resolvedOutcome_navigatesToConversationWithoutPendingSelfParticipant() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)
            advanceUntilIdle()

            viewModel.effects.test {
                resolution.outcomes.emit(
                    ConversationResolutionOutcome.Resolved(conversationId = CONVERSATION_ID),
                )
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.NavigateToConversation(
                        conversationId = CONVERSATION_ID,
                        selfParticipantId = null,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun resolvedOutcome_clearsSelectedRecipients() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val selected = createSelectedRecipientsDelegateMock()
            createViewModel(
                conversationResolutionDelegate = resolution.mock,
                selectedRecipientsDelegate = selected.mock,
            )
            advanceUntilIdle()

            resolution.outcomes.emit(
                ConversationResolutionOutcome.Resolved(conversationId = CONVERSATION_ID),
            )
            advanceUntilIdle()

            verify(exactly = 1) { selected.mock.clear() }
        }
    }

    @Test
    fun resolvedOutcome_exitsGroupCreationMode() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onCreateGroupRequested()
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isCreatingGroup)

                resolution.outcomes.emit(
                    ConversationResolutionOutcome.Resolved(conversationId = CONVERSATION_ID),
                )
                advanceUntilIdle()
                assertFalse(expectMostRecentItem().isCreatingGroup)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun resolvedOutcome_afterContactClick_usesSelectedSimSelfParticipantId() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(subscription(selfParticipantId = "self-1")),
            )
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(
                subscriptionsRepository = repository,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onContactClicked(destination = DESTINATION)
                advanceUntilIdle()
                resolution.outcomes.emit(
                    ConversationResolutionOutcome.Resolved(conversationId = CONVERSATION_ID),
                )
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.NavigateToConversation(
                        conversationId = CONVERSATION_ID,
                        selfParticipantId = "self-1",
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun resolvedOutcome_afterContactClick_withBlankSelfParticipantId_navigatesWithNull() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repository = createSubscriptionsRepositoryMock(
                subscriptions = persistentListOf(subscription(selfParticipantId = "")),
            )
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(
                subscriptionsRepository = repository,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onContactClicked(destination = DESTINATION)
                advanceUntilIdle()
                resolution.outcomes.emit(
                    ConversationResolutionOutcome.Resolved(conversationId = CONVERSATION_ID),
                )
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.NavigateToConversation(
                        conversationId = CONVERSATION_ID,
                        selfParticipantId = null,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun failedOutcome_emitsConversationCreationFailureMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)
            advanceUntilIdle()

            viewModel.effects.test {
                resolution.outcomes.emit(ConversationResolutionOutcome.Failed)
                advanceUntilIdle()
                assertEquals(
                    NewChatEffect.ShowMessage(
                        messageResId = R.string.conversation_creation_failure,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
