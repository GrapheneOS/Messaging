package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import com.android.messaging.ui.conversation.composer.delegate.DraftSaveRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

internal class DraftEditorStateTextEditsTest : BaseDraftEditorStateTest() {

    @Test
    fun withMessageText_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.withMessageText("hi"))
    }

    @Test
    fun withMessageText_whenTextMatchesEffectiveDraft_returnsSameState() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))

        assertSame(state, state.withMessageText("hi"))
    }

    @Test
    fun withMessageText_withNewText_updatesEffectiveDraftAndRecordsChange() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        assertEquals("hello", state.effectiveDraft.messageText)
        assertEquals(
            DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hello")
            ),
            state.toSaveRequestOrNull(),
        )
    }

    @Test
    fun withMessageText_revertingToPersistedValue_clearsRecordedChange() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")
            .withMessageText("hi")

        assertEquals("hi", state.effectiveDraft.messageText)
        assertNull(state.toSaveRequestOrNull())
    }

    @Test
    fun withSubjectText_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.withSubjectText("subject"))
    }

    @Test
    fun withSubjectText_whenTextMatchesEffectiveDraft_returnsSameState() {
        val state = loadedState(persistedDraft = draft(subjectText = "subject"))

        assertSame(state, state.withSubjectText("subject"))
    }

    @Test
    fun withSubjectText_withNewText_updatesEffectiveDraft() {
        val state = loadedState(persistedDraft = draft(subjectText = "old"))
            .withSubjectText("new")

        assertEquals("new", state.effectiveDraft.subjectText)
    }

    @Test
    fun withSelfParticipantId_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.withSelfParticipantId("sim-1"))
    }

    @Test
    fun withSelfParticipantId_withBlankId_returnsSameState() {
        val state = loadedState(persistedDraft = draft(selfParticipantId = "sim-1"))

        assertSame(state, state.withSelfParticipantId("   "))
    }

    @Test
    fun withSelfParticipantId_whenIdMatchesEffectiveDraft_returnsSameState() {
        val state = loadedState(persistedDraft = draft(selfParticipantId = "sim-1"))

        assertSame(state, state.withSelfParticipantId("sim-1"))
    }

    @Test
    fun withSelfParticipantId_withNewId_updatesEffectiveDraft() {
        val state = loadedState(persistedDraft = draft(selfParticipantId = "sim-1"))
            .withSelfParticipantId("sim-2")

        assertEquals("sim-2", state.effectiveDraft.selfParticipantId)
    }
}
