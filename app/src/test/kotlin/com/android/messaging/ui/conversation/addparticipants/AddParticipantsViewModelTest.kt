package com.android.messaging.ui.conversation.addparticipants

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationDraftsRepository
import com.android.messaging.data.conversation.repository.ConversationParticipantsRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsNavEvent
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftTransfersImpl
import com.android.messaging.ui.conversation.recipientpicker.delegate.ConversationResolutionDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegate
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.util.LogUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddParticipantsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @After
    fun tearDown() {
        unmockkAll()
    }

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

            viewModel.navigationEvents.test {
                advanceUntilIdle()
                resolutionDelegate.outcomesSource.emit(
                    ConversationResolutionOutcome.Resolved(
                        conversationId = ConversationId("conversation-2"),
                    ),
                )

                assertThat(awaitItem()).isEqualTo(
                    AddParticipantsNavEvent.OpenConversation(
                        conversationId = ConversationId("conversation-2"),
                    ),
                )
                verify(exactly = 1) {
                    selectedRecipientsDelegate.clear()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun resolvedOutcome_movesDraftToResolvedConversationBeforeNavigating() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val moveDraftGate = CompletableDeferred<Unit>()
            val conversationDraftsRepository = createDraftsRepository()
            coEvery {
                conversationDraftsRepository.moveDraft(
                    fromConversationId = any(),
                    toConversationId = any(),
                )
            } coAnswers {
                moveDraftGate.await()
            }
            val resolutionDelegate = createResolutionDelegate()
            val viewModel = createViewModel(
                conversationDraftsRepository = conversationDraftsRepository,
                conversationResolutionDelegate = resolutionDelegate.mock,
            )
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)

            viewModel.navigationEvents.test {
                advanceUntilIdle()
                resolutionDelegate.outcomesSource.emit(
                    ConversationResolutionOutcome.Resolved(
                        conversationId = RESOLVED_CONVERSATION_ID,
                    ),
                )
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    conversationDraftsRepository.moveDraft(
                        fromConversationId = CONVERSATION_ID,
                        toConversationId = RESOLVED_CONVERSATION_ID,
                    )
                }
                expectNoEvents()

                moveDraftGate.complete(Unit)

                assertThat(awaitItem()).isEqualTo(
                    AddParticipantsNavEvent.OpenConversation(
                        conversationId = RESOLVED_CONVERSATION_ID,
                    ),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun resolvedOutcome_staysOnScreenWhenMovingDraftFails() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            mockkStatic(LogUtil::class)
            every { LogUtil.e(any(), any(), any()) } just runs
            val conversationDraftsRepository = createDraftsRepository()
            coEvery {
                conversationDraftsRepository.moveDraft(
                    fromConversationId = any(),
                    toConversationId = any(),
                )
            } throws IllegalStateException("Conversation conversation-2 no longer exists")
            val selectedRecipientsDelegate = createSelectedRecipientsDelegate(
                selectedRecipients = persistentListOf(
                    selectedRecipient(destination = "+1 555 0101"),
                ),
            )
            val resolutionDelegate = createResolutionDelegate()
            val viewModel = createViewModel(
                conversationDraftsRepository = conversationDraftsRepository,
                selectedRecipientsDelegate = selectedRecipientsDelegate,
                conversationResolutionDelegate = resolutionDelegate.mock,
            )
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)

            viewModel.effects.test {
                advanceUntilIdle()
                viewModel.onConfirmClick()
                resolutionDelegate.outcomesSource.emit(
                    ConversationResolutionOutcome.Resolved(
                        conversationId = RESOLVED_CONVERSATION_ID,
                    ),
                )

                assertEquals(
                    AddParticipantsEffect.ShowMessage(
                        messageResId = R.string.conversation_creation_failure,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.navigationEvents.test {
                expectNoEvents()
            }
            verify(exactly = 0) {
                selectedRecipientsDelegate.clear()
            }
            viewModel.onConfirmClick()
            assertEquals(2, resolutionDelegate.resolvedDestinations.size)
        }
    }

    @Test
    fun resolvedOutcome_ignoresClicksUntilDraftMoveFinishes() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val moveGate = CompletableDeferred<Unit>()
            val conversationDraftsRepository = createDraftsRepository()
            coEvery {
                conversationDraftsRepository.moveDraft(
                    fromConversationId = any(),
                    toConversationId = any(),
                )
            } coAnswers {
                moveGate.await()
            }
            val selectedRecipientsDelegate = createSelectedRecipientsDelegate(
                selectedRecipients = persistentListOf(
                    selectedRecipient(destination = "+1 555 0101"),
                ),
            )
            val resolutionDelegate = createResolutionDelegate()
            val viewModel = createViewModel(
                conversationDraftsRepository = conversationDraftsRepository,
                selectedRecipientsDelegate = selectedRecipientsDelegate,
                conversationResolutionDelegate = resolutionDelegate.mock,
            )
            viewModel.onConversationIdChanged(conversationId = CONVERSATION_ID)
            advanceUntilIdle()
            viewModel.onConfirmClick()
            // The resolution is already idle again while the draft is still moving
            resolutionDelegate.outcomesSource.emit(
                ConversationResolutionOutcome.Resolved(
                    conversationId = RESOLVED_CONVERSATION_ID,
                ),
            )
            advanceUntilIdle()

            viewModel.onRecipientClicked(recipient = selectedRecipient(destination = "+1 555 0102"))
            viewModel.onConfirmClick()
            moveGate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, resolutionDelegate.resolvedDestinations.size)
            verify(exactly = 0) {
                selectedRecipientsDelegate.toggle(
                    recipient = any(),
                    canAdd = any(),
                )
            }
        }
    }

    private fun createViewModel(
        contactDestinationFormatter: ContactDestinationFormatter = createFormatter(),
        conversationDraftsRepository: ConversationDraftsRepository = createDraftsRepository(),
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
            conversationDraftTransfers = ConversationDraftTransfersImpl(
                conversationDraftsRepository = conversationDraftsRepository,
            ),
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

    private fun createDraftsRepository(): ConversationDraftsRepository {
        return mockk<ConversationDraftsRepository>(relaxed = true)
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
            id = ParticipantId(destination),
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

    private companion object {
        private val RESOLVED_CONVERSATION_ID = ConversationId("conversation-2")
    }
}
