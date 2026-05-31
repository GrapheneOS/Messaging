package com.android.messaging.ui.conversation.addparticipants

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.recipientpicker.delegate.ConversationResolutionDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerUiState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddParticipantsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_bindsDelegates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPickerDelegate = createRecipientPickerDelegate()
            val resolutionDelegate = createResolutionDelegate()

            createViewModel(
                recipientPickerDelegate = recipientPickerDelegate,
                conversationResolutionDelegate = resolutionDelegate.mock,
            )
            advanceUntilIdle()

            verify(exactly = 1) {
                recipientPickerDelegate.bind(scope = any())
            }
            verify(exactly = 1) {
                resolutionDelegate.mock.bind(scope = any())
            }
        }
    }

    @Test
    fun conversationIdChanged_loadsExistingParticipantsAndExcludesThemFromPicker() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val recipientPickerDelegate = createRecipientPickerDelegate()
            val participant = participant(destination = "+1 555 0100")
            val viewModel = createViewModel(
                conversationParticipantsRepository = createParticipantsRepository(
                    participants = persistentListOf(participant),
                ),
                recipientPickerDelegate = recipientPickerDelegate,
            )

            viewModel.uiState.test {
                awaitItem()

                viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.isLoadingConversationParticipants)
                assertEquals(listOf(participant), state.existingParticipants)
                verify {
                    recipientPickerDelegate.onExcludedDestinationsChanged(
                        destinations = setOf("+1 555 0100"),
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun recipientClicked_togglesRecipientAndClearsPickerQueryWhenAdded() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selectedRecipientsDelegate = createSelectedRecipientsDelegate(
                toggleOutcome = RecipientToggleOutcome.Added,
            )
            val recipientPickerDelegate = createRecipientPickerDelegate()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selectedRecipientsDelegate,
                recipientPickerDelegate = recipientPickerDelegate,
            )
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)
            advanceUntilIdle()

            viewModel.onRecipientClicked(
                recipient = selectedRecipient(destination = " +1 555 0101 "),
            )

            verify(exactly = 1) {
                selectedRecipientsDelegate.toggle(
                    recipient = selectedRecipient(destination = "+1 555 0101"),
                    canAdd = any(),
                )
            }
            verify(exactly = 1) {
                recipientPickerDelegate.clearQuery()
            }
        }
    }

    @Test
    fun confirmClick_resolvesExistingAndSelectedDestinations() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolutionDelegate = createResolutionDelegate()
            val selectedRecipientsDelegate = createSelectedRecipientsDelegate(
                selectedRecipients = persistentListOf(
                    selectedRecipient(destination = "+1 555 0101"),
                ),
            )
            val viewModel = createViewModel(
                conversationParticipantsRepository = createParticipantsRepository(
                    participants = persistentListOf(
                        participant(destination = "+1 555 0100"),
                    ),
                ),
                selectedRecipientsDelegate = selectedRecipientsDelegate,
                conversationResolutionDelegate = resolutionDelegate.mock,
            )
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)
            advanceUntilIdle()

            viewModel.onConfirmClick()

            assertEquals(
                listOf(listOf("+1 555 0100", "+1 555 0101")),
                resolutionDelegate.resolvedDestinations,
            )
        }
    }

    @Test
    fun confirmClick_overLimitShowsMessageInsteadOfResolving() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val resolutionDelegate = createResolutionDelegate()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = createSelectedRecipientsDelegate(
                    selectedRecipients = persistentListOf(
                        selectedRecipient(destination = "+1 555 0101"),
                    ),
                ),
                conversationResolutionDelegate = resolutionDelegate.mock,
                isRecipientLimitExceeded = true,
            )
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onConfirmClick()
                assertEquals(
                    AddParticipantsEffect.ShowMessage(
                        messageResId = R.string.too_many_participants,
                    ),
                    awaitItem(),
                )
                assertTrue(resolutionDelegate.resolvedDestinations.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun resolvedOutcome_clearsSelectionAndNavigates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val selectedRecipientsDelegate = createSelectedRecipientsDelegate()
            val resolutionDelegate = createResolutionDelegate()
            val viewModel = createViewModel(
                selectedRecipientsDelegate = selectedRecipientsDelegate,
                conversationResolutionDelegate = resolutionDelegate.mock,
            )

            viewModel.effects.test {
                advanceUntilIdle()
                resolutionDelegate.outcomesSource.emit(
                    ConversationResolutionOutcome.Resolved(
                        conversationId = "conversation-2",
                    ),
                )

                assertEquals(
                    AddParticipantsEffect.NavigateToConversation(
                        conversationId = "conversation-2",
                    ),
                    awaitItem(),
                )
                verify(exactly = 1) {
                    selectedRecipientsDelegate.clear()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun createViewModel(
        contactDestinationFormatter: ContactDestinationFormatter = createFormatter(),
        conversationParticipantsRepository: ConversationParticipantsRepository =
            createParticipantsRepository(),
        isRecipientLimitExceeded: Boolean = false,
        recipientPickerDelegate: RecipientPickerDelegate = createRecipientPickerDelegate(),
        selectedRecipientsDelegate: SelectedRecipientsDelegate = createSelectedRecipientsDelegate(),
        conversationResolutionDelegate: ConversationResolutionDelegate =
            createResolutionDelegate().mock,
    ): AddParticipantsViewModel {
        return AddParticipantsViewModel(
            contactDestinationFormatter = contactDestinationFormatter,
            conversationParticipantsRepository = conversationParticipantsRepository,
            isConversationRecipientLimitExceeded = {
                isRecipientLimitExceeded
            },
            recipientPickerDelegate = recipientPickerDelegate,
            selectedRecipientsDelegate = selectedRecipientsDelegate,
            conversationResolutionDelegate = conversationResolutionDelegate,
            savedStateHandle = SavedStateHandle(),
            mainDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun createFormatter(): ContactDestinationFormatter {
        val formatter = mockk<ContactDestinationFormatter>()
        every { formatter.canonicalize(value = any()) } answers {
            firstArg<String>().trim()
        }
        return formatter
    }

    private fun createParticipantsRepository(
        participants: ImmutableList<ConversationRecipient> = persistentListOf(),
    ): ConversationParticipantsRepository {
        val repository = mockk<ConversationParticipantsRepository>()
        every {
            repository.getParticipants(conversationId = any())
        } returns flowOf(participants)
        return repository
    }

    private fun createRecipientPickerDelegate(): RecipientPickerDelegate {
        val delegate = mockk<RecipientPickerDelegate>(relaxed = true)
        every { delegate.state } returns MutableStateFlow(RecipientPickerUiState())
        every { delegate.bind(scope = any()) } just runs
        every { delegate.onExcludedDestinationsChanged(destinations = any()) } just runs
        every { delegate.clearQuery() } just runs
        return delegate
    }

    private fun createSelectedRecipientsDelegate(
        selectedRecipients: ImmutableList<SelectedRecipient> = persistentListOf(),
        toggleOutcome: RecipientToggleOutcome = RecipientToggleOutcome.Added,
    ): SelectedRecipientsDelegate {
        val delegate = mockk<SelectedRecipientsDelegate>(relaxed = true)
        every { delegate.state } returns MutableStateFlow(selectedRecipients)
        every {
            delegate.toggle(
                recipient = any(),
                canAdd = any(),
            )
        } returns toggleOutcome
        every { delegate.clear() } just runs
        every { delegate.removeWhere(predicate = any()) } just runs
        return delegate
    }

    private fun createResolutionDelegate(): ConversationResolutionDelegateMock {
        val stateFlow = MutableStateFlow<ConversationResolutionState>(
            value = ConversationResolutionState.Idle,
        )
        val outcomesSource = MutableSharedFlow<ConversationResolutionOutcome>()
        val resolvedDestinations = mutableListOf<List<String>>()
        val delegate = mockk<ConversationResolutionDelegate>(relaxed = true)

        every { delegate.state } returns stateFlow
        every { delegate.outcomes } returns outcomesSource
        every { delegate.bind(scope = any()) } just runs
        every {
            delegate.resolve(
                destinations = any(),
                recipientDestination = any(),
            )
        } answers {
            resolvedDestinations += firstArg<List<String>>()
        }
        every { delegate.cancel() } answers {
            stateFlow.value = ConversationResolutionState.Idle
        }

        return ConversationResolutionDelegateMock(
            mock = delegate,
            outcomesSource = outcomesSource,
            resolvedDestinations = resolvedDestinations,
        )
    }

    @Suppress("SameParameterValue")
    private fun participant(destination: String): ConversationRecipient {
        return ConversationRecipient(
            id = destination,
            displayName = destination,
            destination = destination,
        )
    }

    private fun selectedRecipient(destination: String): SelectedRecipient {
        return SelectedRecipient(
            destination = destination,
            label = destination.trim(),
            displayDestination = destination.trim(),
            photoUri = null,
        )
    }

    private data class ConversationResolutionDelegateMock(
        val mock: ConversationResolutionDelegate,
        val outcomesSource: MutableSharedFlow<ConversationResolutionOutcome>,
        val resolvedDestinations: MutableList<List<String>>,
    )
}
