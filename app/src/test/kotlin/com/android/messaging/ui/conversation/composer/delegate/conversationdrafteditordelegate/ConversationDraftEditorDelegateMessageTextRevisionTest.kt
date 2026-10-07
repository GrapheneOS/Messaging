package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.typeMessageText
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEditorDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.DraftSendRequest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationDraftEditorDelegateMessageTextRevisionTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun onMessageTextChanged_keepsTheRevision() {
        val delegate = loadedDelegate()
        val revision = delegate.messageTextRevision()

        delegate.typeMessageText(messageText = "hello")

        assertEquals("hello", delegate.state.value.draft.messageText)
        assertEquals(revision, delegate.messageTextRevision())
    }

    @Test
    fun onMessageTextChanged_typedOverReplacedText_keepsTheReplacement() {
        val delegate = loadedDelegate(persistedDraft = draft(messageText = "saved"))
        val replacedRevision = delegate.messageTextRevision()
        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "shared"),
        )

        delegate.onMessageTextChanged(
            messageText = "saved!",
            messageTextRevision = replacedRevision,
        )

        assertEquals("shared", delegate.state.value.draft.messageText)
        assertEquals(replacedRevision + 1, delegate.messageTextRevision())
    }

    @Test
    fun clearConversationDraftAfterSend_movesTheRevision() {
        val delegate = loadedDelegate()
        delegate.typeMessageText(messageText = "hi")
        val revision = delegate.messageTextRevision()

        delegate.clearConversationDraftAfterSend(
            sendRequest = DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )

        assertEquals("", delegate.state.value.draft.messageText)
        assertEquals(revision + 1, delegate.messageTextRevision())
    }

    @Test
    fun clearConversationDraftAfterSend_afterTypingDuringTheSend_keepsTheTypedTextAndRevision() {
        val delegate = loadedDelegate()
        delegate.typeMessageText(messageText = "hi")
        delegate.typeMessageText(messageText = "hi there")
        val revision = delegate.messageTextRevision()

        delegate.clearConversationDraftAfterSend(
            sendRequest = DraftSendRequest(
                conversationId = CONVERSATION_ID,
                draft = draft(messageText = "hi"),
            ),
        )

        assertEquals("hi there", delegate.state.value.draft.messageText)
        assertEquals(revision, delegate.messageTextRevision())
    }

    @Test
    fun applyPersistedDraftUpdate_loadingSavedText_movesTheRevision() {
        val delegate = createDelegate()
        delegate.reset(conversationId = CONVERSATION_ID)
        val revision = delegate.messageTextRevision()

        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(
                persistedDraft = draft(messageText = "saved"),
            ),
        )

        assertEquals("saved", delegate.state.value.draft.messageText)
        assertEquals(revision + 1, delegate.messageTextRevision())
    }

    @Test
    fun applyPersistedDraftUpdate_underTypedText_keepsTheTypedTextAndRevision() {
        val delegate = loadedDelegate()
        delegate.typeMessageText(messageText = "typed")
        val revision = delegate.messageTextRevision()

        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(
                persistedDraft = draft(messageText = "saved"),
            ),
        )

        assertEquals("typed", delegate.state.value.draft.messageText)
        assertEquals(revision, delegate.messageTextRevision())
    }

    @Test
    fun reset_toAnotherConversation_movesTheRevisionEvenWhenTheTextStaysEmpty() {
        val delegate = loadedDelegate()
        val revision = delegate.messageTextRevision()

        delegate.reset(conversationId = ConversationId("conversation-other"))

        assertEquals("", delegate.state.value.draft.messageText)
        assertEquals(revision + 1, delegate.messageTextRevision())
    }

    @Test
    fun seedDraft_replacingTheText_movesTheRevision() {
        val delegate = loadedDelegate()
        val revision = delegate.messageTextRevision()

        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "shared"),
        )

        assertEquals("shared", delegate.state.value.draft.messageText)
        assertEquals(revision + 1, delegate.messageTextRevision())
    }

    @Test
    fun removeAttachment_keepsTheRevision() {
        val delegate = loadedDelegate(
            persistedDraft = draft(
                messageText = "hi",
                attachments = listOf(attachment(contentUri = "content://a/1")),
            ),
        )
        val revision = delegate.messageTextRevision()

        delegate.removeAttachment(contentUri = "content://a/1")

        assertEquals(revision, delegate.messageTextRevision())
    }

    private fun ConversationDraftEditorDelegateImpl.messageTextRevision(): Int {
        return state.value.messageTextRevision
    }
}
