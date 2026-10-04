package com.android.messaging.ui.conversation.composer.delegate.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_RESOLVED_CONVERSATION_ID as TARGET_CONVERSATION_ID
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Adding people opens a different (group) conversation, so the composer has to save what it has
 * not saved yet before the draft moves there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationDraftDelegateTransferTest : BaseConversationDraftDelegateTest() {

    @Test
    fun transferDraft_savesUnsavedEditsBeforeMovingDraft() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createBoundLoadedDelegateHarness()

            try {
                harness.delegate.onMessageTextChanged(messageText = "latest link")
                // Queued behind the transfer, so it must not save the moved edits again
                harness.delegate.persistDraft()

                harness.conversationDraftTransfers.transferDraft(
                    fromConversationId = CONVERSATION_ID,
                    toConversationId = TARGET_CONVERSATION_ID,
                )
                advanceUntilIdle()

                coVerifyOrder {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = match { draft -> draft.messageText == "latest link" },
                    )
                    harness.conversationDraftsRepository.moveDraft(
                        fromConversationId = CONVERSATION_ID,
                        toConversationId = TARGET_CONVERSATION_ID,
                    )
                }
                coVerify(exactly = 1) {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = any(),
                        draft = any(),
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun transferDraft_waitsForDraftToLoadBeforeSavingEdits() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            try {
                harness.conversationIdFlow.value = CONVERSATION_ID
                advanceUntilIdle()
                harness.delegate.seedDraft(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(messageText = "latest link"),
                )

                val transfer = launch(mainDispatcherRule.testDispatcher) {
                    harness.conversationDraftTransfers.transferDraft(
                        fromConversationId = CONVERSATION_ID,
                        toConversationId = TARGET_CONVERSATION_ID,
                    )
                }
                advanceUntilIdle()
                assertTrue(transfer.isActive)

                harness.emitDraft(
                    conversationId = CONVERSATION_ID,
                    draft = ConversationDraft(messageText = "old link"),
                )
                advanceUntilIdle()

                coVerifyOrder {
                    harness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = match { draft -> draft.messageText == "latest link" },
                    )
                    harness.conversationDraftsRepository.moveDraft(
                        fromConversationId = CONVERSATION_ID,
                        toConversationId = TARGET_CONVERSATION_ID,
                    )
                }
            } finally {
                harness.cancel()
            }
        }
    }

    @Test
    fun transferDraft_savesEditsOfEveryComposerOnTheConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val editingHarness = createBoundLoadedDelegateHarness()
            val laterHarness = createBoundLoadedDelegateHarness(
                conversationDraftTransfers = editingHarness.conversationDraftTransfers,
            )

            try {
                editingHarness.delegate.onMessageTextChanged(messageText = "latest link")

                editingHarness.conversationDraftTransfers.transferDraft(
                    fromConversationId = CONVERSATION_ID,
                    toConversationId = TARGET_CONVERSATION_ID,
                )
                advanceUntilIdle()

                coVerifyOrder {
                    editingHarness.conversationDraftsRepository.saveDraft(
                        conversationId = CONVERSATION_ID,
                        draft = match { draft -> draft.messageText == "latest link" },
                    )
                    editingHarness.conversationDraftsRepository.moveDraft(
                        fromConversationId = CONVERSATION_ID,
                        toConversationId = TARGET_CONVERSATION_ID,
                    )
                }
            } finally {
                editingHarness.cancel()
                laterHarness.cancel()
            }
        }
    }
}
