package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.delegate.DraftSendRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationDraftEditorDelegateSendLifecycleTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun createSendRequestOrNull_whenConversationNotSet_returnsNull() {
        val delegate = createDelegate()

        assertNull(delegate.createSendRequestOrNull())
    }

    @Test
    fun createSendRequestOrNull_whenDraftHasNoContent_returnsNull() {
        val delegate = loadedDelegate()

        assertNull(delegate.createSendRequestOrNull())
    }

    @Test
    fun createSendRequestOrNull_whenPendingAttachmentsRemain_returnsNull() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")
        delegate.addPendingAttachment(
            pendingAttachment = pendingAttachment(pendingAttachmentId = "p1"),
        )

        assertNull(delegate.createSendRequestOrNull())
    }

    @Test
    fun createSendRequestOrNull_whenSendable_returnsRequestWithEffectiveDraft() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        assertEquals(
            DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
            delegate.createSendRequestOrNull(),
        )
    }

    @Test
    fun markSendingForSendRequest_forMatchingConversation_marksSendingAndReturnsTrue() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        val didMarkSending = delegate.markSendingForSendRequest(
            sendRequest = DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )

        assertTrue(didMarkSending)
        assertTrue(delegate.state.value.draft.isSending)
    }

    @Test
    fun markSendingForSendRequest_forDifferentConversation_returnsFalseWithoutMarking() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        val didMarkSending = delegate.markSendingForSendRequest(
            sendRequest = DraftSendRequest(
                conversationId = "conversation-other",
                draft = draft(messageText = "hi"),
            ),
        )

        assertFalse(didMarkSending)
        assertFalse(delegate.state.value.draft.isSending)
    }

    @Test
    fun markSendingForSendRequest_whenAlreadySending_returnsFalse() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")
        val sendRequest = DraftSendRequest(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "hi"),
        )
        assertTrue(delegate.markSendingForSendRequest(sendRequest = sendRequest))

        assertFalse(delegate.markSendingForSendRequest(sendRequest = sendRequest))
    }

    @Test
    fun markConversationDraftAsIdle_forMatchingConversation_clearsSending() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")
        delegate.markSendingForSendRequest(
            sendRequest = DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )
        assertTrue(delegate.state.value.draft.isSending)

        delegate.markConversationDraftAsIdle(conversationId = CONVERSATION_ID)

        assertFalse(delegate.state.value.draft.isSending)
    }

    @Test
    fun markConversationDraftAsIdle_forDifferentConversation_isIgnored() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")
        delegate.markSendingForSendRequest(
            sendRequest = DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )

        delegate.markConversationDraftAsIdle(conversationId = "conversation-other")

        assertTrue(delegate.state.value.draft.isSending)
    }

    @Test
    fun clearConversationDraftAfterSend_forMatchingConversation_clearsDraftContent() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        delegate.clearConversationDraftAfterSend(
            sendRequest = DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )

        assertEquals("", delegate.state.value.draft.messageText)
    }

    @Test
    fun clearConversationDraftAfterSend_forDifferentConversation_isIgnored() {
        val delegate = loadedDelegate()
        delegate.onMessageTextChanged(messageText = "hi")

        delegate.clearConversationDraftAfterSend(
            sendRequest = DraftSendRequest(
                conversationId = "conversation-other",
                draft = draft(messageText = "hi"),
            ),
        )

        assertEquals("hi", delegate.state.value.draft.messageText)
    }
}
