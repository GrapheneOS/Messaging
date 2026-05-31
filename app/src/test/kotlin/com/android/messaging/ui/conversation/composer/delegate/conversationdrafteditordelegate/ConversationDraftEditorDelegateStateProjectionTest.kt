package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationDraftEditorDelegateStateProjectionTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun onMessageTextChanged_reflectsTextInVisibleStateAndKeepsSmsProtocol() {
        val delegate = loadedDelegate()

        delegate.onMessageTextChanged(messageText = "hello")

        assertEquals("hello", delegate.state.value.draft.messageText)
        assertEquals(ConversationDraftSendProtocol.SMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun onSubjectTextChanged_makesDraftMmsAndResolvesVisibleProtocolToMms() {
        val delegate = loadedDelegate()

        delegate.onSubjectTextChanged(subjectText = "subject")

        assertEquals("subject", delegate.state.value.draft.subjectText)
        assertEquals(ConversationDraftSendProtocol.MMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun onSelfParticipantIdChanged_reflectsParticipantWithoutPromotingToMms() {
        val delegate = loadedDelegate()

        delegate.onSelfParticipantIdChanged(selfParticipantId = "self-2")

        assertEquals("self-2", delegate.state.value.draft.selfParticipantId)
        assertEquals(ConversationDraftSendProtocol.SMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun removingLastAttachment_emptiesDraftAndResetsProtocolToSms() {
        val delegate = loadedDelegate(
            persistedDraft = draft(attachments = listOf(attachment(contentUri = "content://a/1"))),
        )
        assertEquals(ConversationDraftSendProtocol.MMS, delegate.state.value.sendProtocol)

        delegate.removeAttachment(contentUri = "content://a/1")

        assertTrue(delegate.state.value.draft.attachments.isEmpty())
        assertEquals(ConversationDraftSendProtocol.SMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun clearingSubjectWhileTextRemains_downgradesMmsToSms() {
        val delegate = loadedDelegate(
            persistedDraft = draft(messageText = "hi", subjectText = "subject"),
        )
        assertEquals(ConversationDraftSendProtocol.MMS, delegate.state.value.sendProtocol)

        delegate.onSubjectTextChanged(subjectText = "")

        assertEquals(ConversationDraftSendProtocol.SMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun textEditAfterAppliedSendProtocol_preservesResolvedProtocolForTextDraft() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")
        delegate.applySendProtocol(sendProtocol = ConversationDraftSendProtocol.MMS)
        assertEquals(ConversationDraftSendProtocol.MMS, delegate.state.value.sendProtocol)

        delegate.onMessageTextChanged(messageText = "hi there")

        assertEquals(ConversationDraftSendProtocol.MMS, delegate.state.value.sendProtocol)
    }

    @Test
    fun reset_marksVisibleDraftAsCheckingUntilPersistedDraftArrives() {
        val delegate = createDelegate()

        delegate.reset(conversationId = "conversation-loading")
        assertTrue(delegate.state.value.draft.isCheckingDraft)

        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(
                conversationId = "conversation-loading",
            ),
        )
        assertFalse(delegate.state.value.draft.isCheckingDraft)
    }
}
