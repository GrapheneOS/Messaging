package com.android.messaging.ui.conversation.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.participant.ResolveContactAction
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveContactActionResult
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationViewModelMessageInteractionTest : BaseConversationViewModelTest() {

    @Test
    fun messageTapMethods_forwardToMessageSelectionDelegate() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageSelectionDelegate = createMessageSelectionDelegateMock()
            val viewModel = createViewModel(
                messageSelectionDelegate = messageSelectionDelegate.mock,
            )

            viewModel.onMessageClick(messageId = MessageId("message-1"))
            viewModel.onMessageLongClick(messageId = MessageId("message-2"))
            viewModel.onMessageResendClick(messageId = MessageId("message-3"))

            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageClick(messageId = MessageId("message-1"))
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageLongClick(messageId = MessageId("message-2"))
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageResendClick(
                    messageId = MessageId("message-3")
                )
            }
        }
    }

    @Test
    fun onMessageAvatarClick_whenContactIsSaved_emitsShowParticipantContactCard() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messagesDelegate = createMessagesDelegateMock()
            messagesDelegate.stateFlow.value = ConversationMessagesUiState.Present(
                messages = persistentListOf(
                    createMessageUiModel().copy(
                        messageId = MessageId("message-1"),
                        senderContactId = 42L,
                        senderContactLookupKey = "lookup-key",
                        senderNormalizedDestination = "+15551234567",
                    ),
                ),
            )
            val viewModel = createViewModel(
                messagesDelegate = messagesDelegate.mock,
                resolveContactAction = ResolveContactAction { _, _, _ ->
                    ResolveContactActionResult.ShowContactCard(
                        contactId = 42L,
                        lookupKey = "lookup-key",
                    )
                },
            )

            viewModel.effects.test {
                viewModel.onMessageAvatarClick(messageId = MessageId("message-1"))
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.ShowParticipantContactCard(
                        contactId = 42L,
                        contactLookupKey = "lookup-key",
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onMessageAvatarClick_whenContactIsNotSaved_emitsAddParticipantContact() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messagesDelegate = createMessagesDelegateMock()
            messagesDelegate.stateFlow.value = ConversationMessagesUiState.Present(
                messages = persistentListOf(
                    createMessageUiModel().copy(
                        messageId = MessageId("message-1"),
                        senderContactId = -1L,
                        senderContactLookupKey = null,
                        senderNormalizedDestination = "+15551234567",
                    ),
                ),
            )
            val viewModel = createViewModel(
                messagesDelegate = messagesDelegate.mock,
                resolveContactAction = ResolveContactAction { _, _, _ ->
                    ResolveContactActionResult.AddContact(destination = "+15551234567")
                },
            )

            viewModel.effects.test {
                viewModel.onMessageAvatarClick(messageId = MessageId("message-1"))
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.AddParticipantContact(
                        request = AddContactRequest(
                            destination = "+15551234567",
                            avatarUri = null,
                        ),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onMessageAvatarClick_whenContactActionIsUnavailable_emitsNoEffect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messagesDelegate = createMessagesDelegateMock()
            messagesDelegate.stateFlow.value = ConversationMessagesUiState.Present(
                messages = persistentListOf(
                    createMessageUiModel().copy(
                        messageId = MessageId("message-1"),
                        senderContactId = ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
                        senderNormalizedDestination = null,
                    ),
                ),
            )
            val viewModel = createViewModel(messagesDelegate = messagesDelegate.mock)

            viewModel.effects.test {
                viewModel.onMessageAvatarClick(messageId = MessageId("message-1"))
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
