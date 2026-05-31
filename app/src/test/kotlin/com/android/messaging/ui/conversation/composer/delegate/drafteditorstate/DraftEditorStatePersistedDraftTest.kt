package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftEditorStatePersistedDraftTest : BaseDraftEditorStateTest() {

    @Test
    fun withPersistedDraft_withoutPendingSentDraft_setsPersistedDraftAndMarksLoaded() {
        val state = DraftEditorState(conversationId = CONVERSATION_ID, isLoaded = false)

        val result = state.withPersistedDraft(draft(messageText = "loaded"))

        assertEquals(draft(messageText = "loaded"), result.persistedDraft)
        assertEquals(draft(messageText = "loaded"), result.effectiveDraft)
        assertTrue(result.isLoaded)
    }

    @Test
    fun withPersistedDraft_normalizesLocalEditsThatMatchNewPersistedDraft() {
        val state = DraftEditorState(conversationId = CONVERSATION_ID, isLoaded = true)
            .withMessageText("loaded")

        val result = state.withPersistedDraft(draft(messageText = "loaded"))

        assertEquals(draft(messageText = "loaded"), result.effectiveDraft)
        assertNull(result.toSaveRequestOrNull())
    }

    @Test
    fun withPersistedDraft_preservesLocalEditsThatDifferFromNewPersistedDraft() {
        val state = DraftEditorState(conversationId = CONVERSATION_ID, isLoaded = true)
            .withMessageText("local")

        val result = state.withPersistedDraft(draft(messageText = "remote"))

        assertEquals(draft(messageText = "local"), result.effectiveDraft)
        assertEquals(draft(messageText = "remote"), result.persistedDraft)
    }

    @Test
    fun withPersistedDraft_whileAwaitingClear_whenPersistedMatchesSent_keepsPending() {
        val sentDraft = draft(messageText = "sent")
        val state = loadedState(persistedDraft = draft(messageText = "sent"))
            .clearDraftAfterSend(sentDraft)

        val result = state.withPersistedDraft(sentDraft)

        assertEquals(sentDraft, result.pendingSentDraft)
        assertEquals(sentDraft, result.persistedDraft)
        assertEquals(draft(), result.effectiveDraft)
    }

    @Test
    fun withPersistedDraft_whileAwaitingClear_whenRemoteChangedAndVisibleCleared_clearsPending() {
        val sentDraft = draft(messageText = "sent")
        val state = loadedState(persistedDraft = draft(messageText = "sent"))
            .clearDraftAfterSend(sentDraft)

        val result = state.withPersistedDraft(draft(messageText = "remote-update"))

        assertNull(result.pendingSentDraft)
        assertEquals(draft(messageText = "remote-update"), result.persistedDraft)
        assertEquals(draft(messageText = "remote-update"), result.effectiveDraft)
    }

    @Test
    fun withPersistedDraft_whileAwaitingClear_whenUserTyped_rebasesVisibleAndClearsPending() {
        val sentDraft = draft(messageText = "sent")
        val state = loadedState(persistedDraft = draft(messageText = "sent"))
            .clearDraftAfterSend(sentDraft)
            .withMessageText("typed-after-send")

        val result = state.withPersistedDraft(draft(messageText = "remote-update"))

        assertNull(result.pendingSentDraft)
        assertEquals(draft(messageText = "remote-update"), result.persistedDraft)
        assertEquals(draft(messageText = "typed-after-send"), result.effectiveDraft)
    }
}
