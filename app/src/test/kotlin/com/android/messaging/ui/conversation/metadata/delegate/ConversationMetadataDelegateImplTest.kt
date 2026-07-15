package com.android.messaging.ui.conversation.metadata.delegate

import app.cash.turbine.test
import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.domain.conversation.usecase.action.ConversationActionRequirementsResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.ui.conversation.metadata.mapper.ConversationMetadataUiStateMapper
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationMetadataDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onArchiveConversationClick_archivesViaRepositoryAndEmitsCloseConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.delegate.effects.test {
                    harness.delegate.onArchiveConversationClick()
                    advanceUntilIdle()

                    assertEquals(ConversationScreenEffect.CloseConversation, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                coVerify(exactly = 1) {
                    harness.conversationsRepository
                        .archiveConversation(conversationId = "conversation-42")
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onUnarchiveConversationClick_unarchivesViaRepositoryWithoutClosing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.delegate.effects.test {
                    harness.delegate.onUnarchiveConversationClick()
                    advanceUntilIdle()

                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }

                coVerify(exactly = 1) {
                    harness.conversationsRepository
                        .unarchiveConversation(conversationId = "conversation-42")
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onAddContactClick_emitsLaunchAddContactFlowWithDestination() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.setPresentState(
                    otherParticipantPhoneNumber = TEST_CALL_ACTION_PHONE_NUMBER,
                )
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onAddContactClick()
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.LaunchAddContactFlow(
                            destination = TEST_CALL_ACTION_PHONE_NUMBER,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onAddContactClick_doesNothingWhenDestinationMissing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                harness.setPresentState(otherParticipantPhoneNumber = null)
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onAddContactClick()
                    advanceUntilIdle()

                    expectNoEvents()
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onDeleteConversationClick_togglesConfirmationVisibility() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                advanceUntilIdle()
                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )

                harness.delegate.onDeleteConversationClick()
                advanceUntilIdle()
                assertEquals(
                    true,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )

                harness.delegate.dismissDeleteConversationConfirmation()
                advanceUntilIdle()
                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun confirmDeleteConversation_deletesViaRepositoryAndEmitsCloseConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(conversationId = "conversation-42")

            try {
                advanceUntilIdle()
                harness.delegate.onDeleteConversationClick()
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.confirmDeleteConversation()
                    advanceUntilIdle()

                    assertEquals(ConversationScreenEffect.CloseConversation, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                assertEquals(
                    false,
                    harness.delegate.isDeleteConversationConfirmationVisible.value,
                )
                verify(exactly = 1) {
                    harness.conversationsRepository
                        .deleteConversation(
                            conversationId = "conversation-42",
                            cutoffTimestamp = any(),
                        )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun commandMethods_doNothingWhenConversationIdIsBlankOrNull() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            listOf(null, "   ").forEach { blankOrNullConversationId ->
                val harness = createHarness(conversationId = blankOrNullConversationId)

                try {
                    harness.delegate.effects.test {
                        harness.delegate.onArchiveConversationClick()
                        harness.delegate.onUnarchiveConversationClick()
                        harness.delegate.onDeleteConversationClick()
                        harness.delegate.confirmDeleteConversation()
                        advanceUntilIdle()

                        expectNoEvents()
                        cancelAndIgnoreRemainingEvents()
                    }

                    assertEquals(
                        false,
                        harness.delegate.isDeleteConversationConfirmationVisible.value,
                    )
                    coVerify(exactly = 0) {
                        harness.conversationsRepository.archiveConversation(
                            conversationId = any(),
                        )
                        harness.conversationsRepository.unarchiveConversation(
                            conversationId = any(),
                        )
                        harness.conversationsRepository.deleteConversation(
                            conversationId = any(),
                            cutoffTimestamp = any(),
                        )
                    }
                } finally {
                    harness.cancel()
                }
            }
        }
    }

    private fun createHarness(conversationId: String?): DelegateHarness {
        val dispatcher = mainDispatcherRule.testDispatcher
        val scope = TestScope(dispatcher)
        val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
        val mapper = mockk<ConversationMetadataUiStateMapper>()
        val conversationIdFlow = MutableStateFlow(conversationId)
        val metadataFlow = MutableStateFlow<ConversationMetadata?>(value = null)

        every {
            conversationsRepository.getConversationMetadata(any())
        } returns metadataFlow
        every {
            mapper.map(metadata = any())
        } answers {
            val metadata = firstArg<ConversationMetadata>()
            ConversationMetadataUiState.Present(
                title = "Carol",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = metadata.otherParticipantPhotoUri,
                    normalizedDestination = metadata.otherParticipantNormalizedDestination,
                    displayName = metadata.conversationName,
                ),
                participantCount = 2,
                otherParticipantDisplayDestination = metadata.otherParticipantDisplayDestination,
                otherParticipantPhoneNumber = metadata.otherParticipantNormalizedDestination,
                otherParticipantContactLookupKey = null,
                isArchived = false,
                isBlocked = false,
                composerAvailability = ConversationComposerAvailability.Editable,
            )
        }

        val delegate = ConversationMetadataDelegateImpl(
            checkConversationActionRequirements = {
                ConversationActionRequirementsResult.Ready
            },
            conversationsRepository = conversationsRepository,
            conversationMetadataUiStateMapper = mapper,
            blockedParticipantsRepository = mockk<BlockedParticipantsRepository>(relaxed = true),
            defaultDispatcher = dispatcher,
        )
        delegate.bind(
            scope = scope,
            conversationIdFlow = conversationIdFlow,
        )

        return DelegateHarness(
            delegate = delegate,
            conversationsRepository = conversationsRepository,
            metadataFlow = metadataFlow,
            scope = scope,
        )
    }

    private fun DelegateHarness.setPresentState(
        otherParticipantPhoneNumber: String?,
    ) {
        metadataFlow.value = mockk<ConversationMetadata>(relaxed = true) {
            every { otherParticipantNormalizedDestination } returns otherParticipantPhoneNumber
        }
    }

    private data class DelegateHarness(
        val delegate: ConversationMetadataDelegateImpl,
        val conversationsRepository: ConversationsRepository,
        val metadataFlow: MutableStateFlow<ConversationMetadata?>,
        val scope: TestScope,
    ) {
        fun cancel() {
            scope.cancel()
        }
    }
}
