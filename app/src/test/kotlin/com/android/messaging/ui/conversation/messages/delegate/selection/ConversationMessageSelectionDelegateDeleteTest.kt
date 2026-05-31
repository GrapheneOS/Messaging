package com.android.messaging.ui.conversation.messages.delegate.selection

import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSelectionDelegateDeleteTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun deleteAction_showsAndDismissesConfirmation() {
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

                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
                advanceUntilIdle()

                assertEquals(
                    persistentSetOf("message-1", "message-2"),
                    harness.delegate.state.value.deleteConfirmation?.messageIds,
                )

                harness.delegate.dismissDeleteMessageConfirmation()
                advanceUntilIdle()

                assertNull(harness.delegate.state.value.deleteConfirmation)
                assertEquals(
                    persistentSetOf("message-1", "message-2"),
                    harness.delegate.state.value.selectedMessageIds,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun confirmDeleteSelectedMessages_deletesSelectedMessagesAndClearsSelection() {
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
                harness.delegate.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
                advanceUntilIdle()

                harness.delegate.confirmDeleteSelectedMessages()
                advanceUntilIdle()

                verify(exactly = 1) {
                    harness.conversationsRepository.deleteMessages(
                        messageIds = persistentSetOf("message-1", "message-2"),
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
}
