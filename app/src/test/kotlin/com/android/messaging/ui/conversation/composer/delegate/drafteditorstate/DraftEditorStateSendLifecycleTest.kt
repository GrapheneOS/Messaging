package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftEditorStateSendLifecycleTest : BaseDraftEditorStateTest() {

    @Test
    fun canSendDraft_withNullConversationId_returnsFalse() {
        val state = DraftEditorState(
            conversationId = null,
            persistedDraft = draft(messageText = "hi"),
            isLoaded = true,
        )

        assertFalse(state.canSendDraft())
    }

    @Test
    fun canSendDraft_whenNotLoaded_returnsFalse() {
        val state = DraftEditorState(
            conversationId = CONVERSATION_ID,
            persistedDraft = draft(messageText = "hi"),
            isLoaded = false,
        )

        assertFalse(state.canSendDraft())
    }

    @Test
    fun canSendDraft_whenSending_returnsFalse() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"), isSending = true)

        assertFalse(state.canSendDraft())
    }

    @Test
    fun canSendDraft_withPendingAttachments_returnsFalse() {
        val state = loadedState(
            persistedDraft = draft(messageText = "hi"),
            pendingAttachments = listOf(pendingAttachment("pending-1")),
        )

        assertFalse(state.canSendDraft())
    }

    @Test
    fun canSendDraft_withoutContent_returnsFalse() {
        val state = loadedState(persistedDraft = draft())

        assertFalse(state.canSendDraft())
    }

    @Test
    fun canSendDraft_whenLoadedWithContentAndNoBlockers_returnsTrue() {
        val state = loadedState(persistedDraft = draft(messageText = "hi"))

        assertTrue(state.canSendDraft())
    }

    @Test
    fun canSendDraft_withSubjectOnlyContent_returnsTrue() {
        val state = loadedState(persistedDraft = draft(subjectText = "subject"))

        assertTrue(state.canSendDraft())
    }

    @Test
    fun canSendDraft_withAttachmentOnlyContent_returnsTrue() {
        val state = loadedState(
            persistedDraft = draft(attachments = listOf(attachment("content://attachment/1"))),
        )

        assertTrue(state.canSendDraft())
    }

    @Test
    fun markSending_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.markSending())
    }

    @Test
    fun markSending_whenAlreadySending_returnsSameState() {
        val state = loadedState(isSending = true)

        assertSame(state, state.markSending())
    }

    @Test
    fun markSending_whenIdle_setsSending() {
        val state = loadedState(isSending = false)

        assertTrue(state.markSending().isSending)
    }

    @Test
    fun markIdle_whenNotSending_returnsSameState() {
        val state = loadedState(isSending = false)

        assertSame(state, state.markIdle())
    }

    @Test
    fun markIdle_whenSending_clearsSending() {
        val state = loadedState(isSending = true)

        assertFalse(state.markIdle().isSending)
    }

    @Test
    fun markIdle_withNullConversationId_whenSending_stillClearsSending() {
        val state = DraftEditorState(conversationId = null, isSending = true)

        assertFalse(state.markIdle().isSending)
    }

    @Test
    fun clearDraftAfterSend_whenVisibleDraftMatchesSentDraft_clearsContentKeepingSelfParticipant() {
        val sentDraft = draft(messageText = "sent", selfParticipantId = "sim-1")
        val state =
            loadedState(persistedDraft = draft(messageText = "sent", selfParticipantId = "sim-1"))

        val result = state.clearDraftAfterSend(sentDraft)

        assertEquals(draft(selfParticipantId = "sim-1"), result.effectiveDraft)
        assertEquals(sentDraft, result.pendingSentDraft)
    }

    @Test
    fun clearDraftAfterSend_whenVisibleDiverged_keepsContentAndUpdatesSelf() {
        val sentDraft = draft(messageText = "sent", selfParticipantId = "sim-2")
        val state = loadedState(persistedDraft = draft(messageText = "sent"))
            .withMessageText("sent with edit")

        val result = state.clearDraftAfterSend(sentDraft)

        assertEquals(
            draft(messageText = "sent with edit", selfParticipantId = "sim-2"),
            result.effectiveDraft,
        )
        assertEquals(sentDraft, result.pendingSentDraft)
    }

    @Test
    fun clearDraftAfterSend_resetsSendingState() {
        val state = loadedState(persistedDraft = draft(messageText = "sent"), isSending = true)

        val result = state.clearDraftAfterSend(draft(messageText = "sent"))

        assertFalse(result.isSending)
    }
}
