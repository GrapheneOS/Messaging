package com.android.messaging.ui.conversation.messages.delegate.selection

import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSelectionDelegateSelectionTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun onMessageLongClick_selectsSingleMessageAndExposesSupportedActions() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = "Hello",
                        canCopyMessageToClipboard = true,
                        canForwardMessage = true,
                    ),
                )
                advanceUntilIdle()

                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-1"),
                    harness.delegate.state.value.selectedMessageIds,
                )
                assertEquals(
                    persistentSetOf(
                        ConversationMessageSelectionAction.Delete,
                        ConversationMessageSelectionAction.Share,
                        ConversationMessageSelectionAction.Forward,
                        ConversationMessageSelectionAction.Copy,
                        ConversationMessageSelectionAction.Details,
                    ),
                    harness.delegate.state.value.availableActions,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun onMessageClick_doesNothingOutsideSelectionMode() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                )
                advanceUntilIdle()

                harness.delegate.onMessageClick(messageId = "message-1")
                advanceUntilIdle()

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
    fun onMessageClick_togglesSelectionWhenSelectionModeIsActive() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                    createMessageUiModel(messageId = "message-2"),
                )
                advanceUntilIdle()

                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()
                harness.delegate.onMessageClick(messageId = "message-2")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-1", "message-2"),
                    harness.delegate.state.value.selectedMessageIds,
                )
                assertEquals(
                    persistentSetOf(ConversationMessageSelectionAction.Delete),
                    harness.delegate.state.value.availableActions,
                )

                harness.delegate.onMessageClick(messageId = "message-1")
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-2"),
                    harness.delegate.state.value.selectedMessageIds,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun bind_clearsSelectionWhenConversationChanges() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(messageId = "message-1"),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()
                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
                advanceUntilIdle()

                harness.conversationIdFlow.value = "conversation-2"
                advanceUntilIdle()

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
