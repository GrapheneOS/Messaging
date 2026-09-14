package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationDraftDelegateAutosaveTest : BaseConversationDraftDelegateTest() {

    @Test
    fun onMessageTextChanged_autosavesAfterDebounce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness()

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")

                advanceTimeBy(299.milliseconds)
                coVerify(exactly = 0) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = any(),
                        draft = any(),
                    )
                }

                advanceTimeBy(1.milliseconds)
                advanceUntilIdle()

                val savedDraft = slot<ConversationDraft>()
                coVerify(exactly = 1) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = capture(savedDraft),
                    )
                }
                assertEquals("Hello", savedDraft.captured.messageText)
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun persistDraft_catchesSaveFailuresAndLeavesStateUsable() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness()

            try {
                harness.delegate.onMessageTextChanged(messageText = "Hello")
                coEvery {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = any(),
                        draft = any(),
                    )
                } throws IllegalStateException("boom")

                harness.delegate.persistDraft()
                advanceUntilIdle()

                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
                coVerify {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = match { draft -> draft.messageText == "Hello" },
                    )
                }

                coEvery {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = any(),
                        draft = any(),
                    )
                } just runs
                harness.delegate.persistDraft()
                advanceUntilIdle()

                assertEquals("Hello", harness.delegate.state.value.draft.messageText)
            } finally {
                harness.cancel()
            }
        }
    }
}
