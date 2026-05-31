package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.DraftEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

internal class DraftEditorStateAttachmentsTest : BaseDraftEditorStateTest() {

    @Test
    fun withAttachmentsAdded_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(conversationId = null)

        assertSame(state, state.withAttachmentsAdded(listOf(attachment("content://attachment/1"))))
    }

    @Test
    fun withAttachmentsAdded_withEmptyCollection_returnsSameState() {
        val state = loadedState()

        assertSame(state, state.withAttachmentsAdded(emptyList()))
    }

    @Test
    fun withAttachmentsAdded_appendsNewAttachmentsPreservingOrder() {
        val existing = attachment("content://attachment/1")
        val added = attachment("content://attachment/2")

        val state = loadedState(persistedDraft = draft(attachments = listOf(existing)))
            .withAttachmentsAdded(listOf(added))

        assertEquals(listOf(existing, added), state.effectiveDraft.attachments)
    }

    @Test
    fun withAttachmentsAdded_skipsAttachmentsWithExistingContentUri() {
        val existing = attachment("content://attachment/1", captionText = "original")
        val duplicate = attachment("content://attachment/1", captionText = "duplicate")

        val state = loadedState(persistedDraft = draft(attachments = listOf(existing)))

        assertSame(state, state.withAttachmentsAdded(listOf(duplicate)))
    }

    @Test
    fun withAttachmentsAdded_dedupesWithinBatchByContentUriKeepingFirst() {
        val first = attachment("content://attachment/1", captionText = "first")
        val sameUri = attachment("content://attachment/1", captionText = "second")

        val state = loadedState()
            .withAttachmentsAdded(listOf(first, sameUri))

        assertEquals(listOf(first), state.effectiveDraft.attachments)
    }

    @Test
    fun withAttachmentsAdded_withMixedNewAndDuplicateBatch_appendsOnlyNewPreservingOrder() {
        val existing = attachment("content://attachment/1")
        val duplicateOfExisting = attachment("content://attachment/1", captionText = "ignored")
        val added = attachment("content://attachment/2")

        val state = loadedState(persistedDraft = draft(attachments = listOf(existing)))
            .withAttachmentsAdded(listOf(duplicateOfExisting, added))

        assertEquals(listOf(existing, added), state.effectiveDraft.attachments)
    }

    @Test
    fun withAttachmentRemoved_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(
            conversationId = null,
            persistedDraft = draft(attachments = listOf(attachment("content://attachment/1"))),
        )

        assertSame(state, state.withAttachmentRemoved("content://attachment/1"))
    }

    @Test
    fun withAttachmentRemoved_removesMatchingAttachment() {
        val keep = attachment("content://attachment/1")
        val remove = attachment("content://attachment/2")

        val state = loadedState(persistedDraft = draft(attachments = listOf(keep, remove)))
            .withAttachmentRemoved("content://attachment/2")

        assertEquals(listOf(keep), state.effectiveDraft.attachments)
    }

    @Test
    fun withAttachmentRemoved_withUnknownContentUri_returnsSameState() {
        val state = loadedState(
            persistedDraft = draft(attachments = listOf(attachment("content://attachment/1"))),
        )

        assertSame(state, state.withAttachmentRemoved("content://attachment/unknown"))
    }

    @Test
    fun withAttachmentCaption_withNullConversationId_returnsSameState() {
        val state = DraftEditorState(
            conversationId = null,
            persistedDraft = draft(attachments = listOf(attachment("content://attachment/1"))),
        )

        assertSame(state, state.withAttachmentCaption("content://attachment/1", "caption"))
    }

    @Test
    fun withAttachmentCaption_updatesCaptionForMatchingAttachment() {
        val state = loadedState(
            persistedDraft = draft(
                attachments = listOf(attachment("content://attachment/1", captionText = "old")),
            ),
        ).withAttachmentCaption("content://attachment/1", "new")

        assertEquals("new", state.effectiveDraft.attachments.single().captionText)
    }

    @Test
    fun withAttachmentCaption_withUnknownContentUri_returnsSameState() {
        val state = loadedState(
            persistedDraft = draft(attachments = listOf(attachment("content://attachment/1"))),
        )

        assertSame(state, state.withAttachmentCaption("content://attachment/unknown", "caption"))
    }

    @Test
    fun withAttachmentCaption_withUnchangedCaption_returnsSameState() {
        val state = loadedState(
            persistedDraft = draft(
                attachments = listOf(attachment("content://attachment/1", captionText = "same")),
            ),
        )

        assertSame(state, state.withAttachmentCaption("content://attachment/1", "same"))
    }

    @Test
    fun withAttachmentCaption_onMultiElementList_updatesOnlyTargetPreservingOrder() {
        val first = attachment("content://attachment/1")
        val target = attachment("content://attachment/2", captionText = "old")
        val last = attachment("content://attachment/3")

        val state = loadedState(persistedDraft = draft(attachments = listOf(first, target, last)))
            .withAttachmentCaption("content://attachment/2", "new")

        assertEquals(
            listOf(first, attachment("content://attachment/2", captionText = "new"), last),
            state.effectiveDraft.attachments,
        )
    }
}
