package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
internal class ConversationDraftDelegateObservationTest : BaseConversationDraftDelegateTest() {

    @Test
    fun bind_setsCheckingStateUntilPersistedDraftArrives() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                advanceUntilIdle()

                assertTrue(harness.delegate.state.value.draft.isCheckingDraft)

                harness.emitDraft(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(
                        messageText = "Persisted",
                    ),
                )
                advanceUntilIdle()

                assertEquals("Persisted", harness.delegate.state.value.draft.messageText)
                assertFalse(harness.delegate.state.value.draft.isCheckingDraft)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun bind_catchesObservationFailuresAndPublishesSafeEmptyDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness(
                observeFailure = IllegalStateException("boom"),
            )
            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                advanceUntilIdle()

                assertEquals(
                    ConversationDraftState(
                        draft = ConversationDraft(),
                    ),
                    harness.delegate.state.value,
                )
                assertFalse(harness.delegate.state.value.draft.isCheckingDraft)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun conversationSwitch_flushesPreviousDraftBeforeResettingState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()
            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                harness.emitDraft(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(),
                )
                advanceUntilIdle()

                harness.delegate.onMessageTextChanged(messageText = "Hello")
                harness.conversationIdFlow.value = "conversation-2"
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = any(),
                    )
                }
                assertTrue(harness.delegate.state.value.draft.isCheckingDraft)
                assertEquals("", harness.delegate.state.value.draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }
}
