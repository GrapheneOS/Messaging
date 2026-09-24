package com.android.messaging.ui.conversation.messages.delegate.selection

import android.content.ClipData
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
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
internal class ConversationMessageSelectionDelegateMessageActionClickTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun copyAction_copiesTextWithoutASelection() {
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

                harness.delegate.onMessageActionClick(
                    messageId = MessageId("message-1"),
                    action = ConversationMessageAction.Copy,
                )
                advanceUntilIdle()

                assertEquals(
                    "Copied text",
                    copiedClipData.captured.getItemAt(0).text.toString(),
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun detailsAction_opensDetailsOfThatMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                    createMessageUiModel(messageId = "message-2"),
                )
                advanceUntilIdle()

                harness.delegate.navigationEvents.test {
                    harness.delegate.onMessageActionClick(
                        messageId = MessageId("message-2"),
                        action = ConversationMessageAction.Details,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenNavEvent.NavigateToMessageDetails(
                            MessageId("message-2"),
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
    fun deleteAction_confirmsAndDeletesOnlyThatMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                    createMessageUiModel(messageId = "message-2"),
                )
                advanceUntilIdle()

                harness.delegate.onMessageActionClick(
                    messageId = MessageId("message-2"),
                    action = ConversationMessageAction.Delete,
                )
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf(MessageId("message-2")),
                    harness.delegate.state.value.deleteConfirmation?.messageIds,
                )
                assertTrue(harness.delegate.state.value.selectedMessageIds.isEmpty())

                harness.delegate.confirmDeleteSelectedMessages()
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.deleteMessages(
                        messageIds = persistentSetOf(MessageId("message-2")),
                    )
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
    fun unavailableAction_isIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        canCopyMessageToClipboard = false,
                    ),
                )
                advanceUntilIdle()

                harness.delegate.onMessageActionClick(
                    messageId = MessageId("message-1"),
                    action = ConversationMessageAction.Copy,
                )
                advanceUntilIdle()

                verify(exactly = 0) {
                    harness.clipboardManager.setPrimaryClip(any())
                }
            } finally {
                harness.cancel()
            }
        }
    }
}
