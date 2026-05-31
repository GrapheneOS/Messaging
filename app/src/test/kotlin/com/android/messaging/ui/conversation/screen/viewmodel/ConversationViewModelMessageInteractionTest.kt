package com.android.messaging.ui.conversation.screen.viewmodel

import app.cash.turbine.test
import com.android.messaging.datamodel.data.ParticipantData
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

            viewModel.onMessageClick(messageId = "message-1")
            viewModel.onMessageLongClick(messageId = "message-2")
            viewModel.onMessageResendClick(messageId = "message-3")

            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageClick(messageId = "message-1")
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageLongClick(messageId = "message-2")
            }
            verify(exactly = 1) {
                messageSelectionDelegate.mock.onMessageResendClick(messageId = "message-3")
            }
        }
    }

    @Test
    fun onMessageAvatarClick_whenMessageCanShowContactCard_emitsShowOrAddParticipantContact() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messagesDelegate = createMessagesDelegateMock()
            messagesDelegate.stateFlow.value = ConversationMessagesUiState.Present(
                messages = persistentListOf(
                    createMessageUiModel().copy(
                        messageId = "message-1",
                        senderContactId = 42L,
                        senderContactLookupKey = "lookup-key",
                        senderNormalizedDestination = "+15551234567",
                    ),
                ),
            )
            val viewModel = createViewModel(messagesDelegate = messagesDelegate.mock)

            viewModel.effects.test {
                viewModel.onMessageAvatarClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    ConversationScreenEffect.ShowOrAddParticipantContact(
                        contactId = 42L,
                        contactLookupKey = "lookup-key",
                        avatarUri = null,
                        normalizedDestination = "+15551234567",
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun onMessageAvatarClick_whenMessageCannotShowContactCard_emitsNoEffect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messagesDelegate = createMessagesDelegateMock()
            messagesDelegate.stateFlow.value = ConversationMessagesUiState.Present(
                messages = persistentListOf(
                    createMessageUiModel().copy(
                        messageId = "message-1",
                        senderContactId = ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
                        senderNormalizedDestination = null,
                    ),
                ),
            )
            val viewModel = createViewModel(messagesDelegate = messagesDelegate.mock)

            viewModel.effects.test {
                viewModel.onMessageAvatarClick(messageId = "message-1")
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
