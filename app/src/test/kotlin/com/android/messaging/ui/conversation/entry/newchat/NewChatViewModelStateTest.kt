package com.android.messaging.ui.conversation.entry.newchat

import app.cash.turbine.test
import com.android.messaging.ui.conversation.entry.model.NewChatEffect
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class NewChatViewModelStateTest : BaseNewChatViewModelTest() {

    @Test
    fun init_bindsRecipientPickerAndResolutionDelegatesToTheSameScope() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPicker = createRecipientPickerDelegateMock()
            val resolution = createResolutionDelegateMock()

            createViewModel(
                recipientPickerDelegate = recipientPicker.mock,
                conversationResolutionDelegate = resolution.mock,
            )
            advanceUntilIdle()

            assertEquals(1, recipientPicker.bindScopes.size)
            assertEquals(1, resolution.bindScopes.size)
            assertSame(
                recipientPicker.bindScopes.single(),
                resolution.bindScopes.single(),
            )
        }
    }

    @Test
    fun uiState_whenResolutionIdle_projectsRecipientPickerAndSelectedRecipients() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPicker = createRecipientPickerDelegateMock(
                state = RecipientPickerUiState(query = "Ada", isLoading = true),
            )
            val selected = createSelectedRecipientsDelegateMock(
                recipients = persistentListOf(selectedRecipient(destination = DESTINATION)),
            )
            val viewModel = createViewModel(
                recipientPickerDelegate = recipientPicker.mock,
                selectedRecipientsDelegate = selected.mock,
            )

            viewModel.uiState.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertFalse(state.isCreatingGroup)
                assertFalse(state.isResolvingConversation)
                assertFalse(state.isResolvingConversationIndicatorVisible)
                assertEquals(null, state.resolvingRecipientDestination)
                assertEquals(
                    RecipientPickerUiState(query = "Ada", isLoading = true),
                    state.recipientPickerUiState,
                )
                assertEquals(
                    persistentListOf(selectedRecipient(destination = DESTINATION)),
                    state.selectedGroupRecipients,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun uiState_whenResolving_exposesResolvingFlagsAndIndicatorTransition() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock(
                state = ConversationResolutionState.Resolving(
                    recipientDestination = DESTINATION,
                    isIndicatorVisible = false,
                ),
            )
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)

            viewModel.uiState.test {
                advanceUntilIdle()
                val resolving = expectMostRecentItem()
                assertTrue(resolving.isResolvingConversation)
                assertFalse(resolving.isResolvingConversationIndicatorVisible)
                assertEquals(DESTINATION, resolving.resolvingRecipientDestination)

                resolution.stateFlow.value = ConversationResolutionState.Resolving(
                    recipientDestination = DESTINATION,
                    isIndicatorVisible = true,
                )
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isResolvingConversationIndicatorVisible)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun uiState_reflectsRecipientPickerAndSelectedRecipientUpdates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPicker = createRecipientPickerDelegateMock()
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(
                recipientPickerDelegate = recipientPicker.mock,
                selectedRecipientsDelegate = selected.mock,
            )

            viewModel.uiState.test {
                advanceUntilIdle()
                assertEquals(
                    persistentListOf<SelectedRecipient>(),
                    expectMostRecentItem().selectedGroupRecipients,
                )

                selected.stateFlow.value =
                    persistentListOf(selectedRecipient(destination = DESTINATION))
                advanceUntilIdle()
                assertEquals(
                    persistentListOf(selectedRecipient(destination = DESTINATION)),
                    expectMostRecentItem().selectedGroupRecipients,
                )

                recipientPicker.stateFlow.value = RecipientPickerUiState(query = "Bob")
                advanceUntilIdle()
                assertEquals("Bob", expectMostRecentItem().recipientPickerUiState.query)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onLoadMore_forwardsToRecipientPickerDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(recipientPickerDelegate = recipientPicker.mock)
            advanceUntilIdle()

            viewModel.onLoadMore()

            verify(exactly = 1) { recipientPicker.mock.onLoadMore() }
        }
    }

    @Test
    fun onQueryChanged_forwardsToRecipientPickerDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPicker = createRecipientPickerDelegateMock()
            val viewModel = createViewModel(recipientPickerDelegate = recipientPicker.mock)
            advanceUntilIdle()

            viewModel.onQueryChanged(query = "Ada")

            verify(exactly = 1) { recipientPicker.mock.onQueryChanged(query = "Ada") }
        }
    }

    @Test
    fun onNavigateBack_whenNotCreatingGroup_cancelsResolutionAndEmitsNavigateBack() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolution = createResolutionDelegateMock()
            val viewModel = createViewModel(conversationResolutionDelegate = resolution.mock)
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onNavigateBack()
                advanceUntilIdle()
                assertEquals(NewChatEffect.NavigateBack, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { resolution.mock.cancel() }
        }
    }

    @Test
    fun onNavigateBack_whileCreatingGroup_exitsGroupModeAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selected = createSelectedRecipientsDelegateMock()
            val viewModel = createViewModel(selectedRecipientsDelegate = selected.mock)

            viewModel.uiState.test {
                advanceUntilIdle()
                viewModel.onCreateGroupRequested()
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isCreatingGroup)

                viewModel.onNavigateBack()
                advanceUntilIdle()
                assertFalse(expectMostRecentItem().isCreatingGroup)
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 2) { selected.mock.clear() }
        }
    }

    @Test
    fun onNavigateBack_whileCreatingGroup_doesNotEmitNavigateBack() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onCreateGroupRequested()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onNavigateBack()
                advanceUntilIdle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
