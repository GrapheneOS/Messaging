package com.android.messaging.ui.conversation.messages.delegate.selection

import android.content.ClipData
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsData
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSelectionDelegateMessageActionsTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun copyAction_copiesTextAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            val copiedClipData = slot<ClipData>()
            every {
                harness.clipboardManager.setPrimaryClip(capture(copiedClipData))
            } just runs

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = "Copied text",
                        canCopyMessageToClipboard = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Copy,
                )
                advanceUntilIdle()

                assertEquals(
                    "Copied text",
                    copiedClipData.captured.getItemAt(0).text.toString(),
                )
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun downloadAction_downloadsSelectedMessageAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canDownloadMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Download,
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.downloadMessage(messageId = "message-1")
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun forwardAction_emitsForwardEffectAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            val forwardedMessage = mockk<MessageData>()
            coEvery {
                harness.createForwardedMessage.invoke(
                    conversationId = CONVERSATION_ID,
                    messageId = "message-1",
                )
            } returns forwardedMessage

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canForwardMessage = true,
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Forward,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.LaunchForwardMessage(
                            message = forwardedMessage,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun detailsAction_emitsDetailsEffectAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            val messageDetails = mockk<ConversationMessageData>()
            val participants = mockk<ConversationParticipantsData>()
            val selfParticipant = mockk<ParticipantData>()
            coEvery {
                harness.conversationsRepository.getMessageDetailsData(
                    conversationId = CONVERSATION_ID,
                    messageId = "message-1",
                )
            } returns ConversationMessageDetailsData(
                message = messageDetails,
                participants = participants,
                selfParticipant = selfParticipant,
            )

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Details,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShowMessageDetails(
                            message = messageDetails,
                            participants = participants,
                            selfParticipant = selfParticipant,
                        ),
                        awaitItem(),
                    )
                    cancelAndIgnoreRemainingEvents()
                }
                assertEquals(
                    ConversationMessageSelectionUiState(),
                    harness.delegate.state.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }
}
