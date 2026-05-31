package com.android.messaging.ui.conversation.composer.delegate.drafteditorstate

import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEdits
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationDraftEditsTest : BaseDraftEditorStateTest() {

    @Test
    fun hasChanges_whenAllFieldsNull_returnsFalse() {
        assertFalse(ConversationDraftEdits().hasChanges)
    }

    @Test
    fun hasChanges_withMessageTextSet_returnsTrue() {
        assertTrue(ConversationDraftEdits(messageText = "text").hasChanges)
    }

    @Test
    fun hasChanges_withSubjectTextSet_returnsTrue() {
        assertTrue(ConversationDraftEdits(subjectText = "subject").hasChanges)
    }

    @Test
    fun hasChanges_withSelfParticipantIdSet_returnsTrue() {
        assertTrue(ConversationDraftEdits(selfParticipantId = "sim-1").hasChanges)
    }

    @Test
    fun hasChanges_withEmptyButNonNullAttachments_returnsTrue() {
        assertTrue(ConversationDraftEdits(attachments = persistentListOf()).hasChanges)
    }

    @Test
    fun applyTo_withNullFields_returnsBaseValuesUnchanged() {
        val base = draft(
            messageText = "message",
            subjectText = "subject",
            selfParticipantId = "sim-1",
            attachments = listOf(attachment("content://attachment/1")),
        )

        assertEquals(base, ConversationDraftEdits().applyTo(base))
    }

    @Test
    fun applyTo_withSetFields_overridesBaseValues() {
        val base =
            draft(messageText = "message", subjectText = "subject", selfParticipantId = "sim-1")
        val edits = ConversationDraftEdits(
            messageText = "new-message",
            subjectText = "new-subject",
            selfParticipantId = "sim-2",
            attachments = persistentListOf(attachment("content://attachment/9")),
        )

        assertEquals(
            draft(
                messageText = "new-message",
                subjectText = "new-subject",
                selfParticipantId = "sim-2",
                attachments = listOf(attachment("content://attachment/9")),
            ),
            edits.applyTo(base),
        )
    }

    @Test
    fun applyTo_preservesBaseFlagsNotCoveredByEdits() {
        val base = draft(messageText = "message", isCheckingDraft = true, isSending = true)

        val result = ConversationDraftEdits(messageText = "new-message").applyTo(base)

        assertTrue(result.isCheckingDraft)
        assertTrue(result.isSending)
    }

    @Test
    fun normalizedAgainst_dropsFieldsEqualToBase() {
        val base =
            draft(messageText = "message", subjectText = "subject", selfParticipantId = "sim-1")
        val edits = ConversationDraftEdits(
            messageText = "message",
            subjectText = "subject",
            selfParticipantId = "sim-1",
            attachments = base.attachments,
        )

        assertFalse(edits.normalizedAgainst(base).hasChanges)
    }

    @Test
    fun normalizedAgainst_keepsEveryFieldThatDiffersFromBase() {
        val base = draft(
            messageText = "message",
            subjectText = "subject",
            selfParticipantId = "sim-1",
            attachments = listOf(attachment("content://attachment/base")),
        )
        val edits = ConversationDraftEdits(
            messageText = "different-message",
            subjectText = "different-subject",
            selfParticipantId = "sim-2",
            attachments = persistentListOf(attachment("content://attachment/edit")),
        )

        val normalized = edits.normalizedAgainst(base)

        assertEquals("different-message", normalized.messageText)
        assertEquals("different-subject", normalized.subjectText)
        assertEquals("sim-2", normalized.selfParticipantId)
        assertEquals(listOf(attachment("content://attachment/edit")), normalized.attachments)
    }

    @Test
    fun normalizedAgainst_keepsDifferingFieldWhileDroppingEqualField() {
        val base = draft(messageText = "message", subjectText = "subject")

        val normalized = ConversationDraftEdits(messageText = "different", subjectText = "subject")
            .normalizedAgainst(base)

        assertEquals("different", normalized.messageText)
        assertNull(normalized.subjectText)
    }
}
