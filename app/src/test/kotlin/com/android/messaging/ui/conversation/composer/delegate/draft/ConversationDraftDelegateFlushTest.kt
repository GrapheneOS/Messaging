package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.draft.ConversationDraftPendingAttachment
import com.android.messaging.testutil.PausableTestDispatcher
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.typeMessageText
import com.android.messaging.util.ContentType
import io.mockk.coVerify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationDraftDelegateFlushTest : BaseConversationDraftDelegateTest() {

    @Test
    fun flushDraft_afterScreenScopeIsCancelled_savesWorkingDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness()

            try {
                harness.delegate.typeMessageText(messageText = "Keep this text")
                harness.delegateScope.cancel()

                harness.delegate.flushDraft()
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = ConversationDraft(messageText = "Keep this text"),
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun flushDraft_whenOlderFlushRunsLast_keepsNewerDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val defaultDispatcher = PausableTestDispatcher(
                testDispatcher = mainDispatcherRule.testDispatcher,
            )
            val harness = createBoundLoadedDelegateHarness(defaultDispatcher = defaultDispatcher)

            try {
                harness.delegate.typeMessageText(messageText = "Keep this text")
                harness.delegate.addPendingAttachment(pendingAttachment = PENDING_AUDIO_ATTACHMENT)
                // The screen scope is gone before its autosave runs, as after ViewModel.clear()
                harness.delegateScope.cancel()
                runCurrent()
                defaultDispatcher.pause()

                harness.delegate.flushDraft()
                harness.delegate.resolvePendingAttachment(
                    pendingAttachmentId = PENDING_AUDIO_ATTACHMENT.pendingAttachmentId,
                    attachment = AUDIO_ATTACHMENT,
                )
                harness.delegate.flushDraft()
                assertEquals(2, defaultDispatcher.heldTaskCount)

                // Default dispatcher threads may start the newer flush first
                defaultDispatcher.release(index = 1)
                runCurrent()
                defaultDispatcher.release(index = 0)
                runCurrent()

                val savedDrafts = mutableListOf<ConversationDraft>()
                coVerify {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = capture(savedDrafts),
                    )
                }
                assertEquals(
                    ConversationDraft(
                        messageText = "Keep this text",
                        attachments = persistentListOf(AUDIO_ATTACHMENT),
                    ),
                    savedDrafts.last(),
                )
            } finally {
                defaultDispatcher.resume()
                harness.cancel()
            }
        }
    }

    private companion object {
        private val PENDING_AUDIO_ATTACHMENT = ConversationDraftPendingAttachment(
            pendingAttachmentId = "pending-audio-1",
            contentUri = "pending://audio/pending-audio-1",
            contentType = ContentType.AUDIO_3GPP,
        )
        private val AUDIO_ATTACHMENT = ConversationDraftAttachment(
            contentType = ContentType.AUDIO_3GPP,
            contentUri = "content://scratch/audio/1",
        )
    }
}
