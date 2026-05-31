package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.coVerify
import io.mockk.slot
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationDraftDelegateSendTest : BaseConversationDraftDelegateTest() {

    @Test
    fun sendSuccess_allowsAutosavingNewDraftBeforeRepositoryClearsSentDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness()

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("", harness.delegate.state.value.draft.messageText)

                harness.delegate.onMessageTextChanged(messageText = "Next")
                advanceTimeBy(300.milliseconds)
                advanceUntilIdle()

                val savedDraft = slot<ConversationDraft>()
                coVerify(exactly = 1) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = capture(savedDraft),
                    )
                }
                assertEquals("Next", savedDraft.captured.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendFailure_restoresIdleStateAndKeepsDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = createSendConversationDraftMock(
                sendResult = flow {
                    throw IllegalStateException("boom")
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
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendCancellation_restoresIdleState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = createSendConversationDraftMock(
                sendResult = flow {
                    throw CancellationException("cancelled")
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
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun typingDuringSendIsPreservedWhenDispatchCompletes() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendGate = CompletableDeferred<Unit>()
            val sendConversationDraft = createSendConversationDraftMock(
                sendResult = flow {
                    sendGate.await()
                    emit(Unit)
                },
            )
            val harness = createBoundLoadedDelegateHarness(
                sendConversationDraft = sendConversationDraft,
            )

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.delegate.onSendClick()
                advanceUntilIdle()
                assertTrue(harness.delegate.state.value.draft.isSending)

                harness.delegate.onMessageTextChanged(messageText = "Next")
                sendGate.complete(Unit)
                advanceUntilIdle()

                assertFalse(harness.delegate.state.value.draft.isSending)
                assertEquals("Next", harness.delegate.state.value.draft.messageText)

                advanceTimeBy(300.milliseconds)
                advanceUntilIdle()

                val savedDraft = slot<ConversationDraft>()
                coVerify(exactly = 1) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = capture(savedDraft),
                    )
                }
                assertEquals("Next", savedDraft.captured.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun sendFlowCompletingWithoutEmissionRestoresIdleState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val sendConversationDraft = createSendConversationDraftMock(
                sendResult = emptyFlow(),
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
            } finally {
                harness.cancel()
            }
        }
    }
}
