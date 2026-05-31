package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import com.android.messaging.ui.conversation.composer.delegate.DraftSaveRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftEditorStateSaveTest : BaseDraftEditorStateTest() {

    @Test
    fun toSaveRequestOrNull_withNullConversationId_returnsNull() {
        val state = DraftEditorState(conversationId = null, isLoaded = true)

        assertNull(state.toSaveRequestOrNull())
    }

    @Test
    fun toSaveRequestOrNull_whenNotLoaded_returnsNull() {
        val state = DraftEditorState(conversationId = CONVERSATION_ID, isLoaded = false)
            .withMessageText("hi")

        assertNull(state.toSaveRequestOrNull())
    }

    @Test
    fun toSaveRequestOrNull_whenSending_returnsNull() {
        val state = loadedState(isSending = true).withMessageText("hi")

        assertNull(state.toSaveRequestOrNull())
    }

    @Test
    fun toSaveRequestOrNull_withNoRecordedChanges_returnsNull() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))

        assertNull(state.toSaveRequestOrNull())
    }

    @Test
    fun toSaveRequestOrNull_whenLoadedWithChanges_returnsRequestWithEffectiveDraft() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        assertEquals(
            DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hello")
            ),
            state.toSaveRequestOrNull(),
        )
    }

    @Test
    fun matchesSaveRequest_withDifferentConversationId_returnsFalse() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        assertFalse(
            state.matchesSaveRequest(
                DraftSaveRequest(conversationId = "other", draft = draft(messageText = "hello")),
            ),
        )
    }

    @Test
    fun matchesSaveRequest_whenNotLoaded_returnsFalse() {
        val state = DraftEditorState(conversationId = CONVERSATION_ID, isLoaded = false)
            .withMessageText("hello")

        assertFalse(
            state.matchesSaveRequest(
                DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "hello")
                ),
            ),
        )
    }

    @Test
    fun matchesSaveRequest_whenSending_returnsFalse() {
        val state = loadedState(isSending = true).withMessageText("hello")

        assertFalse(
            state.matchesSaveRequest(
                DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "hello")
                ),
            ),
        )
    }

    @Test
    fun matchesSaveRequest_withNoRecordedChanges_returnsFalse() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))

        assertFalse(
            state.matchesSaveRequest(
                DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "hi")
                ),
            ),
        )
    }

    @Test
    fun matchesSaveRequest_whenEffectiveDraftMatchesRequest_returnsTrue() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        assertTrue(
            state.matchesSaveRequest(
                DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "hello")
                ),
            ),
        )
    }

    @Test
    fun matchesSaveRequest_whenEffectiveDraftDiffersFromRequest_returnsFalse() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        assertFalse(
            state.matchesSaveRequest(
                DraftSaveRequest(
                    conversationId = CONVERSATION_ID,
                    draft = draft(messageText = "different")
                ),
            ),
        )
    }

    @Test
    fun withPersistedSaveResult_withDifferentConversationId_returnsSameState() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        assertSame(
            state,
            state.withPersistedSaveResult(
                DraftSaveRequest(conversationId = "other", draft = draft(messageText = "hello")),
            ),
        )
    }

    @Test
    fun withPersistedSaveResult_whenEffectiveDraftMatchesSavedDraft_persistsAndClearsChanges() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("hello")

        val result = state.withPersistedSaveResult(
            DraftSaveRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hello")
            ),
        )

        assertEquals(draft(messageText = "hello"), result.persistedDraft)
        assertEquals(draft(messageText = "hello"), result.effectiveDraft)
        assertNull(result.toSaveRequestOrNull())
    }

    @Test
    fun withPersistedSaveResult_whenEffectiveDraftDiverged_rebasesEditsOnSavedDraft() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))
            .withMessageText("v1")
            .withMessageText("v2")

        val result = state.withPersistedSaveResult(
            DraftSaveRequest(conversationId = CONVERSATION_ID, draft = draft(messageText = "v1")),
        )

        assertEquals(draft(messageText = "v1"), result.persistedDraft)
        assertEquals(draft(messageText = "v2"), result.effectiveDraft)
    }

    @Test
    fun withPersistedSaveResult_whenDivergedAcrossAllFields_rebasesEachFieldOntoSavedDraft() {
        val localDraft = draft(
            messageText = "local-message",
            subjectText = "local-subject",
            selfParticipantId = "sim-local",
            attachments = listOf(attachment("content://attachment/local")),
        )
        val state = loadedState(persistedDraft = draft())
            .withMessageText(localDraft.messageText)
            .withSubjectText(localDraft.subjectText)
            .withSelfParticipantId(localDraft.selfParticipantId)
            .withAttachmentsAdded(localDraft.attachments)

        val saved = DraftSaveRequest(
            conversationId = CONVERSATION_ID,
            draft = draft(
                messageText = "saved-message",
                subjectText = "saved-subject",
                selfParticipantId = "sim-saved",
                attachments = listOf(attachment("content://attachment/saved")),
            ),
        )

        val result = state.withPersistedSaveResult(saved)

        assertEquals(localDraft, result.effectiveDraft)
        assertEquals(saved.draft, result.persistedDraft)
    }
}
