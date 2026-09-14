package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.domain.conversation.usecase.draft.model.DraftAttachmentLimitResult
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationDraftEditorDelegateAttachmentsTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun addAttachments_withEmptyInput_returnsEmptyResultWithoutResolvingLimit() {
        val delegate = loadedDelegate()

        val result = delegate.addAttachments(attachments = emptyList())

        assertEquals(
            DraftAttachmentLimitResult(attachmentsToAdd = emptyList(), didDropAttachments = false),
            result,
        )
        verify(exactly = 0) {
            resolveDraftAttachmentsWithinLimit(currentAttachments = any(), attachmentsToAdd = any())
        }
        assertTrue(delegate.state.value.draft.attachments.isEmpty())
    }

    @Test
    fun addAttachments_withAcceptedAttachments_addsThemAndReturnsResult() {
        val delegate = loadedDelegate()
        val attachments = listOf(
            attachment(contentUri = "content://a/1"),
            attachment(contentUri = "content://a/2"),
        )
        givenAttachmentLimitResult(attachmentsToAdd = attachments, didDropAttachments = false)

        val result = delegate.addAttachments(attachments = attachments)

        assertEquals(
            DraftAttachmentLimitResult(attachmentsToAdd = attachments, didDropAttachments = false),
            result,
        )
        assertEquals(attachments, delegate.state.value.draft.attachments.toList())
    }

    @Test
    fun addAttachments_whenEveryCandidateIsDropped_returnsResultButLeavesStateUnchanged() {
        val delegate = loadedDelegate()
        givenAttachmentLimitResult(attachmentsToAdd = emptyList(), didDropAttachments = true)

        val result = delegate.addAttachments(
            attachments = listOf(attachment(contentUri = "content://a/1")),
        )

        assertEquals(
            DraftAttachmentLimitResult(attachmentsToAdd = emptyList(), didDropAttachments = true),
            result,
        )
        assertTrue(delegate.state.value.draft.attachments.isEmpty())
    }

    @Test
    fun addAttachments_resolvesLimitAgainstCurrentEffectiveAttachments() {
        val delegate = loadedDelegate(
            persistedDraft = draft(attachments = listOf(attachment(contentUri = "content://a/1"))),
        )
        val candidate = attachment(contentUri = "content://a/2")
        givenAttachmentLimitResult(attachmentsToAdd = listOf(candidate), didDropAttachments = false)

        delegate.addAttachments(attachments = listOf(candidate))

        verify(exactly = 1) {
            resolveDraftAttachmentsWithinLimit(
                currentAttachments = match<Collection<ConversationDraftAttachment>> { current ->
                    current.map { attachment -> attachment.contentUri } == listOf("content://a/1")
                },
                attachmentsToAdd = listOf(candidate),
            )
        }
    }

    @Test
    fun tryStartAddingAttachment_whenBelowLimit_returnsTrue() {
        val delegate = loadedDelegate()
        givenAttachmentLimit(limit = 2)

        assertTrue(delegate.tryStartAddingAttachment())
    }

    @Test
    fun tryStartAddingAttachment_whenAttachmentsReachLimit_returnsFalse() {
        val delegate = loadedDelegate(
            persistedDraft = draft(
                attachments = listOf(
                    attachment(contentUri = "content://a/1"),
                    attachment(contentUri = "content://a/2"),
                ),
            ),
        )
        givenAttachmentLimit(limit = 2)

        assertFalse(delegate.tryStartAddingAttachment())
    }

    @Test
    fun tryStartAddingAttachment_countsPendingAttachmentsTowardLimit() {
        val delegate = loadedDelegate(
            persistedDraft = draft(attachments = listOf(attachment(contentUri = "content://a/1"))),
        )
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )
        givenAttachmentLimit(limit = 2)

        assertFalse(delegate.tryStartAddingAttachment())
    }

    @Test
    fun addPendingAttachment_addsToVisiblePendingAttachments() {
        val delegate = loadedDelegate()
        val pending = pendingAttachment(pendingAttachmentId = "p1")

        delegate.addPendingAttachment(pendingAttachment = pending)

        assertEquals(listOf(pending), delegate.state.value.pendingAttachments)
    }

    @Test
    fun removeAttachment_removesOnlyMatchingAttachment() {
        val delegate = loadedDelegate(
            persistedDraft = draft(
                attachments = listOf(
                    attachment(contentUri = "content://a/1"),
                    attachment(contentUri = "content://a/2"),
                ),
            ),
        )

        delegate.removeAttachment(contentUri = "content://a/1")

        assertEquals(
            listOf("content://a/2"),
            delegate.state.value.draft.attachments.map { attachment -> attachment.contentUri },
        )
    }

    @Test
    fun removePendingAttachment_removesOnlyMatchingPendingAttachment() {
        val delegate = loadedDelegate()
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p2"),
        )

        delegate.removePendingAttachment(pendingAttachmentId = "p1")

        assertEquals(
            listOf("p2"),
            delegate.state.value.pendingAttachments.map { pending -> pending.pendingAttachmentId },
        )
    }

    @Test
    fun updateAttachmentCaption_updatesCaptionForMatchingAttachment() {
        val delegate = loadedDelegate(
            persistedDraft = draft(
                attachments = listOf(attachment(contentUri = "content://a/1", captionText = "")),
            ),
        )

        delegate.updateAttachmentCaption(contentUri = "content://a/1", captionText = "a caption")

        assertEquals("a caption", delegate.state.value.draft.attachments.single().captionText)
    }
}
