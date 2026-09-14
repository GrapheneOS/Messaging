package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.ui.conversation.composer.delegate.DraftPendingAttachmentResolution
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationDraftEditorDelegatePendingResolutionTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun resolvePendingAttachment_whenIdIsUnknown_returnsUnresolvedAndLeavesExistingPendingIntact() {
        val delegate = loadedDelegate()
        val existingPending = pendingAttachment(pendingAttachmentId = "p1")
        delegate.addPendingAttachment(pendingAttachment = existingPending)

        val resolution = delegate.resolvePendingAttachment(
            pendingAttachmentId = "missing",
            attachment = attachment(contentUri = "content://a/1"),
        )

        assertEquals(
            DraftPendingAttachmentResolution(
                didResolveAttachment = false,
                didDropAttachments = false,
            ),
            resolution,
        )
        verify(exactly = 0) {
            resolveDraftAttachmentsWithinLimit(currentAttachments = any(), attachmentsToAdd = any())
        }
        assertEquals(listOf(existingPending), delegate.state.value.pendingAttachments)
        assertTrue(delegate.state.value.draft.attachments.isEmpty())
    }

    @Test
    fun resolvePendingAttachment_whenAccepted_removesPendingAddsAttachmentAndReportsResolved() {
        val delegate = loadedDelegate()
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )
        val resolved = attachment(contentUri = "content://resolved")
        givenAttachmentLimitResult(attachmentsToAdd = listOf(resolved), didDropAttachments = false)

        val resolution = delegate.resolvePendingAttachment(
            pendingAttachmentId = "p1",
            attachment = resolved,
        )

        assertEquals(
            DraftPendingAttachmentResolution(
                didResolveAttachment = true,
                didDropAttachments = false,
            ),
            resolution,
        )
        assertTrue(delegate.state.value.pendingAttachments.isEmpty())
        assertEquals(listOf(resolved), delegate.state.value.draft.attachments.toList())
    }

    @Test
    fun resolvePendingAttachment_whenDroppedByLimit_removesPendingButDoesNotAddAttachment() {
        val delegate = loadedDelegate()
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )
        givenAttachmentLimitResult(attachmentsToAdd = emptyList(), didDropAttachments = true)

        val resolution = delegate.resolvePendingAttachment(
            pendingAttachmentId = "p1",
            attachment = attachment(contentUri = "content://candidate"),
        )

        assertEquals(
            DraftPendingAttachmentResolution(
                didResolveAttachment = false,
                didDropAttachments = true,
            ),
            resolution,
        )
        assertTrue(delegate.state.value.pendingAttachments.isEmpty())
        assertTrue(delegate.state.value.draft.attachments.isEmpty())
    }

    @Test
    fun resolvePendingAttachment_resolvesLimitAgainstCommittedAttachmentsAndCandidate() {
        val delegate = loadedDelegate(
            persistedDraft = draft(attachments = listOf(attachment(contentUri = "content://a/1"))),
        )
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )
        val resolved = attachment(contentUri = "content://resolved")
        givenAttachmentLimitResult(attachmentsToAdd = listOf(resolved), didDropAttachments = false)

        delegate.resolvePendingAttachment(pendingAttachmentId = "p1", attachment = resolved)

        verify(exactly = 1) {
            resolveDraftAttachmentsWithinLimit(
                currentAttachments = match<Collection<ConversationDraftAttachment>> { current ->
                    current.map { attachment -> attachment.contentUri } == listOf("content://a/1")
                },
                attachmentsToAdd = listOf(resolved),
            )
        }
    }

    @Test
    fun resolvePendingAttachment_resolvesLimitAgainstAttachmentsRemainingAfterIntermediateEdits() {
        val delegate = loadedDelegate(
            persistedDraft = draft(attachments = listOf(attachment(contentUri = "content://a/1"))),
        )
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )
        delegate.removeAttachment(contentUri = "content://a/1")
        val resolved = attachment(contentUri = "content://resolved")
        givenAttachmentLimitResult(attachmentsToAdd = listOf(resolved), didDropAttachments = false)

        delegate.resolvePendingAttachment(pendingAttachmentId = "p1", attachment = resolved)

        verify(exactly = 1) {
            resolveDraftAttachmentsWithinLimit(
                currentAttachments = match<Collection<ConversationDraftAttachment>> { current ->
                    current.isEmpty()
                },
                attachmentsToAdd = listOf(resolved),
            )
        }
    }
}
