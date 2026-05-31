package com.android.messaging.ui.conversation.composer.delegate.draft

import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.draft.SendConversationDraft
import com.android.messaging.domain.conversation.usecase.draft.exception.ConversationSimNotReadyException
import com.android.messaging.domain.conversation.usecase.draft.exception.DraftDispatchFailedException
import com.android.messaging.domain.conversation.usecase.draft.exception.MessageLimitExceededException
import com.android.messaging.domain.conversation.usecase.draft.exception.TooManyVideoAttachmentsException
import com.android.messaging.domain.conversation.usecase.draft.exception.UnknownConversationRecipientException
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationDraftDelegateSendValidationTest : BaseConversationDraftDelegateTest() {

    @Test
    fun sendValidationFailure_whenRecipientIsUnknown_emitsUnknownSenderMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertSendFailureMessage(
                exception = UnknownConversationRecipientException(
                    conversationId = CONVERSATION_ID,
                ),
                expectedMessageResId = R.string.unknown_sender,
            )
        }
    }

    @Test
    fun sendValidationFailure_whenSimIsNotReady_emitsNetworkNotReadyMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertSendFailureMessage(
                exception = ConversationSimNotReadyException(
                    conversationId = CONVERSATION_ID,
                    selfSubId = 1,
                    cause = IllegalStateException("SIM unavailable"),
                ),
                expectedMessageResId = R.string.cant_send_message_without_active_subscription,
            )
        }
    }

    @Test
    fun sendValidationFailure_whenTooManyVideos_setsVideoLimitWarning() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = createSendConversationDraftMock(
                sendResult = flow {
                    throw TooManyVideoAttachmentsException(
                        conversationId = CONVERSATION_ID,
                        videoAttachmentCount = 2,
                    )
                },
            )
            val harness = createBoundLoadedDelegateHarness(
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
                assertEquals(
                    ConversationAttachmentLimitWarning.SendingVideoAttachmentLimitReached,
                    harness.delegate.attachmentLimitWarning.value,
                )
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendValidationFailure_whenMessageLimitExceeded_sendsAnywayWithLimitIgnored() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = mockk<SendConversationDraft>()
            every {
                sendConversationDraft.invoke(
                    conversationId = any(),
                    draft = any(),
                    ignoreMessageSizeLimit = false,
                )
            } returns flow {
                throw MessageLimitExceededException(
                    conversationId = CONVERSATION_ID,
                )
            }
            every {
                sendConversationDraft.invoke(
                    conversationId = any(),
                    draft = any(),
                    ignoreMessageSizeLimit = true,
                )
            } returns createSuccessfulSendFlow()
            val harness = createBoundLoadedDelegateHarness(
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
                assertEquals(
                    ConversationAttachmentLimitWarning.SendingMessageLimitReached,
                    harness.delegate.attachmentLimitWarning.value,
                )
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    sendConversationDraft.invoke(
                        conversationId = any(),
                        draft = any(),
                        ignoreMessageSizeLimit = false,
                    )
                }

                harness.delegate.sendAnywayAfterAttachmentLimitWarning()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("", harness.delegate.state.value.draft.messageText)
                assertNull(harness.delegate.attachmentLimitWarning.value)
                verify(exactly = 1) {
                    @Suppress("UnusedFlow")
                    sendConversationDraft.invoke(
                        conversationId = any(),
                        draft = match { draft -> draft.messageText == "Hello" },
                        ignoreMessageSizeLimit = true,
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendValidationFailure_whenDispatchFails_emitsGenericSendFailureMessage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertSendFailureMessage(
                exception = DraftDispatchFailedException(
                    conversationId = CONVERSATION_ID,
                    cause = IllegalStateException("boom"),
                ),
                expectedMessageResId = R.string.send_message_failure,
            )
        }
    }

    private suspend fun TestScope.assertSendFailureMessage(
        exception: Throwable,
        expectedMessageResId: Int,
    ) {
        val sendConversationDraft = createSendConversationDraftMock(
            sendResult = flow {
                throw exception
            },
        )
        val harness = createBoundLoadedDelegateHarness(
            sendConversationDraft = sendConversationDraft,
        )

        try {
            harness.delegate.onMessageTextChanged(messageText = "Hello")

            harness.delegate.effects.test {
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
                assertEquals(
                    ConversationScreenEffect.ShowMessage(
                        messageResId = expectedMessageResId,
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
