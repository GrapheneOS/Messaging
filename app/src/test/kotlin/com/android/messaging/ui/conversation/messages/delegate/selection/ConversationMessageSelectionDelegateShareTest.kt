package com.android.messaging.ui.conversation.messages.delegate.selection

import app.cash.turbine.test
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageSelectionDelegateShareTest :
    BaseConversationMessageSelectionDelegateTest() {

    @Test
    fun shareAction_emitsTextShareWhenSelectedMessageHasText() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = "Share me",
                        canForwardMessage = true,
                        parts = persistentListOf(
                            createAttachmentPart(),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Share,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShareMessage(
                            attachmentContentType = null,
                            attachmentContentUri = null,
                            text = "Share me",
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
    fun shareAction_emitsAttachmentShareWhenSelectedMessageHasNoText() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.messagesStateFlow.value = createMessagesUiState(
                    createMessageUiModel(
                        messageId = "message-1",
                        text = null,
                        canForwardMessage = true,
                        parts = persistentListOf(
                            createAttachmentPart(),
                        ),
                    ),
                )
                advanceUntilIdle()
                harness.delegate.onMessageLongClick(messageId = "message-1")
                advanceUntilIdle()

                harness.delegate.effects.test {
                    harness.delegate.onMessageSelectionActionClick(
                        action = ConversationMessageSelectionAction.Share,
                    )
                    advanceUntilIdle()

                    assertEquals(
                        ConversationScreenEffect.ShareMessage(
                            attachmentContentType = IMAGE_ATTACHMENT_CONTENT_TYPE,
                            attachmentContentUri = IMAGE_ATTACHMENT_CONTENT_URI,
                            text = null,
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
}
