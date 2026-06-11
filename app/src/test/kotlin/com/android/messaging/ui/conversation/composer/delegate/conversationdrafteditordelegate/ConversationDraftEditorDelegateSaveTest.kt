package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import app.cash.turbine.test
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.delegate.DraftSaveRequest
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationDraftEditorDelegateSaveTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun currentSaveRequest_whenLoadedDraftBecomesDirty_reflectsEffectiveDraft() {
        val delegate = loadedDelegate()
        assertNull(delegate.currentSaveRequest)

        delegate.onMessageTextChanged(messageText = "draft text")

        assertEquals(
            DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "draft text"),
            ),
            delegate.currentSaveRequest,
        )
    }

    @Test
    fun saveRequests_emitsNullThenSaveRequestAsDraftBecomesDirty() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = loadedDelegate()

            delegate.saveRequests.test {
                assertNull(awaitItem())

                delegate.onMessageTextChanged(messageText = "hello")
                runCurrent()

                assertEquals(
                    DraftSaveRequest(
                        conversationId = CONVERSATION_ID,
                        draft = draft(messageText = "hello"),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun reset_returnsPendingSaveRequestAndClearsDraft() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "unsaved")

        val saveRequest = delegate.reset(conversationId = null)

        assertEquals(
            DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "unsaved"),
            ),
            saveRequest,
        )
        assertEquals(ConversationDraftState(), delegate.state.value)
        assertNull(delegate.currentSaveRequest)
    }

    @Test
    fun reset_whenThereIsNothingToSave_returnsNull() {
        val delegate = loadedDelegate()

        assertNull(delegate.reset(conversationId = null))
    }

    @Test
    fun applyPersistedDraftUpdate_forDifferentConversation_isIgnored() {
        val delegate = loadedDelegate(persistedDraft = draft(messageText = "persisted"))

        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(
                conversationId = "conversation-other",
                persistedDraft = draft(messageText = "other text"),
            ),
        )

        assertEquals("persisted", delegate.state.value.draft.messageText)
    }

    @Test
    fun applyPersistedDraftUpdate_forMatchingConversation_updatesPersistedDraftAndMarksLoaded() {
        val delegate = createDelegate()
        delegate.reset(conversationId = CONVERSATION_ID)
        assertTrue(delegate.state.value.draft.isCheckingDraft)

        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(
                conversationId = CONVERSATION_ID,
                persistedDraft = draft(messageText = "from server"),
            ),
        )

        assertEquals("from server", delegate.state.value.draft.messageText)
        assertFalse(delegate.state.value.draft.isCheckingDraft)
    }

    @Test
    fun matchesSaveRequest_reflectsWhetherRequestEqualsCurrentEffectiveDraft() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        assertTrue(
            delegate.matchesSaveRequest(
                saveRequest = DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "hi"),
                ),
            ),
        )
        assertFalse(
            delegate.matchesSaveRequest(
                saveRequest = DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "different"),
                ),
            ),
        )
    }

    @Test
    fun applyPersistedSaveResult_clearsPendingEditsWhileKeepingVisibleDraft() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")
        assertEquals(
            DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
            delegate.currentSaveRequest,
        )

        delegate.applyPersistedSaveResult(
            saveRequest = DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )

        assertNull(delegate.currentSaveRequest)
        assertEquals("hi", delegate.state.value.draft.messageText)
    }
}
