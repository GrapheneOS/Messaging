package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftEditorStateEffectiveDraftTest : BaseDraftEditorStateTest() {

    @Test
    fun effectiveDraft_withoutLocalEdits_returnsPersistedDraft() {
        val persisted = draft(messageText = "persisted")

        val state = loadedState(persistedDraft = persisted)

        assertEquals(persisted, state.effectiveDraft)
    }

    @Test
    fun effectiveDraft_withLocalEdits_appliesEditsOverPersistedDraft() {
        val state = loadedState(
            persistedDraft = draft(messageText = "persisted", subjectText = "subject"),
        ).withMessageText("edited")

        assertEquals(draft(messageText = "edited", subjectText = "subject"), state.effectiveDraft)
    }

    @Test
    fun visibleState_withNullConversationId_returnsEmptyState() {
        val state = DraftEditorState(
            conversationId = null,
            persistedDraft = draft(messageText = "ignored"),
        )

        assertEquals(ConversationDraftState(), state.visibleState)
    }

    @Test
    fun visibleState_whenNotLoaded_marksDraftAsCheckingDraft() {
        val state = DraftEditorState(
            conversationId = CONVERSATION_ID,
            persistedDraft = draft(messageText = "hi"),
            isLoaded = false,
        )

        assertTrue(state.visibleState.draft.isCheckingDraft)
    }

    @Test
    fun visibleState_whenLoaded_marksDraftAsNotCheckingDraft() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))

        assertFalse(state.visibleState.draft.isCheckingDraft)
    }

    @Test
    fun visibleState_whenSending_marksDraftAsSending() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"), isSending = true)

        assertTrue(state.visibleState.draft.isSending)
    }

    @Test
    fun visibleState_exposesEffectiveDraftAndPendingAttachments() {
        val pending = pendingAttachment("pending-1")

        val state = loadedState(
            persistedDraft = draft(messageText = "hi"),
            pendingAttachments = listOf(pending),
        ).withMessageText("edited")

        assertEquals(
            ConversationDraftState(
                draft = draft(messageText = "edited"),
                pendingAttachments = listOf(pending),
            ),
            state.visibleState,
        )
    }
}
