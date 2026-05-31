package com.android.messaging.ui.conversation.composer.delegate.conversationdrafteditordelegate

import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationDraftEditorDelegateSeedTest :
    BaseConversationDraftEditorDelegateTest() {

    @Test
    fun seedDraft_whenConversationAlreadySet_appliesSeedImmediately() {
        val delegate = createDelegate()
        delegate.reset(conversationId = CONVERSATION_ID)

        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "seeded"),
        )

        assertEquals("seeded", delegate.state.value.draft.messageText)
    }

    @Test
    fun seedDraft_beforeConversationIsSet_isDeferredUntilMatchingReset() {
        val delegate = createDelegate()

        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "seeded"),
        )
        assertEquals("", delegate.state.value.draft.messageText)

        delegate.reset(conversationId = CONVERSATION_ID)

        assertEquals("seeded", delegate.state.value.draft.messageText)
    }

    @Test
    fun seedDraft_isNotAppliedToADifferentConversation() {
        val delegate = createDelegate()
        delegate.seedDraft(
            conversationId = "conversation-seeded",
            draft = draft(messageText = "seeded"),
        )

        delegate.reset(conversationId = "conversation-other")
        assertEquals("", delegate.state.value.draft.messageText)

        delegate.reset(conversationId = "conversation-seeded")
        assertEquals("seeded", delegate.state.value.draft.messageText)
    }

    @Test
    fun seedDraft_survivesInterleavedPersistedUpdateAndIsAppliedByLaterReset() {
        val delegate = createDelegate()
        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "seeded"),
        )

        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(
                conversationId = CONVERSATION_ID,
                persistedDraft = draft(messageText = "persisted"),
            ),
        )
        assertEquals("", delegate.state.value.draft.messageText)

        delegate.reset(conversationId = CONVERSATION_ID)

        assertEquals("seeded", delegate.state.value.draft.messageText)
    }

    @Test
    fun seedDraft_isAppliedOnlyOnce() {
        val delegate = createDelegate()
        delegate.reset(conversationId = CONVERSATION_ID)
        delegate.seedDraft(
            conversationId = CONVERSATION_ID,
            draft = draft(messageText = "seeded"),
        )
        assertEquals("seeded", delegate.state.value.draft.messageText)

        delegate.reset(conversationId = CONVERSATION_ID)

        assertEquals("", delegate.state.value.draft.messageText)
    }
}
