package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftEditorStateSeededDraftTest : BaseDraftEditorStateTest() {

    @Test
    fun withSeededDraft_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.withSeededDraft(draft(messageText = "seed")))
    }

    @Test
    fun withSeededDraft_replacesLocalEditsWithSeededContent() {
        val state = loadedState(persistedDraft = draft(messageText = "persisted"))
            .withSeededDraft(draft(messageText = "seed", subjectText = "subject"))

        assertEquals(draft(messageText = "seed", subjectText = "subject"), state.effectiveDraft)
    }

    @Test
    fun withSeededDraft_withAttachments_includesThemInEffectiveDraft() {
        val seededAttachment = attachment("content://attachment/1")

        val state = loadedState()
            .withSeededDraft(draft(attachments = listOf(seededAttachment)))

        assertEquals(listOf(seededAttachment), state.effectiveDraft.attachments)
    }

    @Test
    fun withSeededDraft_withBlankSelfParticipantId_fallsBackToPersistedSelfParticipantId() {
        val state = loadedState(persistedDraft = draft(selfParticipantId = "sim-persisted"))
            .withSeededDraft(draft(messageText = "seed", selfParticipantId = "   "))

        assertEquals("sim-persisted", state.effectiveDraft.selfParticipantId)
    }

    @Test
    fun withSeededDraft_withNonBlankSelfParticipantId_usesSeededSelfParticipantId() {
        val state = loadedState(persistedDraft = draft(selfParticipantId = "sim-persisted"))
            .withSeededDraft(draft(messageText = "seed", selfParticipantId = "sim-seed"))

        assertEquals("sim-seed", state.effectiveDraft.selfParticipantId)
    }

    @Test
    fun withSeededDraft_whenSeededContentEqualsPersisted_recordsNoChange() {
        val persisted = draft(messageText = "persisted", subjectText = "subject")

        val state = loadedState(persistedDraft = persisted)
            .withSeededDraft(persisted)

        assertEquals(persisted, state.effectiveDraft)
        assertNull(state.toSaveRequestOrNull())
    }

    @Test
    fun withSeededDraft_replacesPriorLocalEditsDroppingAttachmentsNotInSeed() {
        val state = loadedState()
            .withAttachmentsAdded(listOf(attachment("content://attachment/old")))
            .withSeededDraft(draft(messageText = "seed"))

        assertEquals("seed", state.effectiveDraft.messageText)
        assertTrue(state.effectiveDraft.attachments.isEmpty())
    }
}
