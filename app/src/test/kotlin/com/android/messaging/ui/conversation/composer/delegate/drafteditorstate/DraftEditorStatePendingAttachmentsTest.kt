package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftEditorStatePendingAttachmentsTest : BaseDraftEditorStateTest() {

    @Test
    fun withPendingAttachmentAdded_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.withPendingAttachmentAdded(pendingAttachment("pending-1")))
    }

    @Test
    fun withPendingAttachmentAdded_appendsPendingAttachment() {
        val existing = pendingAttachment("pending-1")
        val added = pendingAttachment("pending-2")

        val state = loadedState(pendingAttachments = listOf(existing))
            .withPendingAttachmentAdded(added)

        assertEquals(listOf(existing, added), state.pendingAttachments)
    }

    @Test
    fun withPendingAttachmentRemoved_removesMatchingPendingAttachment() {
        val keep = pendingAttachment("pending-1")
        val remove = pendingAttachment("pending-2")

        val state = loadedState(pendingAttachments = listOf(keep, remove))
            .withPendingAttachmentRemoved("pending-2")

        assertEquals(listOf(keep), state.pendingAttachments)
    }

    @Test
    fun withPendingAttachmentRemoved_withUnknownId_returnsSameState() {
        val state = loadedState(pendingAttachments = listOf(pendingAttachment("pending-1")))

        assertSame(state, state.withPendingAttachmentRemoved("unknown"))
    }

    @Test
    fun withPendingAttachmentRemoved_withNullConversationId_stillRemoves() {
        val state = DraftEditorState(
            conversationId = null,
            pendingAttachments = listOf(pendingAttachment("pending-1")),
        )

        assertTrue(state.withPendingAttachmentRemoved("pending-1").pendingAttachments.isEmpty())
    }
}
