package com.android.messaging.ui.conversationlist.common

import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationListItemContentDescriptionTest {

    @Test
    fun contentDescriptionSpec_oneOnOneIncomingSuccessful_usesSenderDescription() {
        val item = previewConversationListItem(
            conversationId = "one_on_one_incoming",
            title = "Bob",
            snippetText = "Hello there",
            senderName = "Bob",
        )

        val spec = conversationListItemContentDescriptionSpec(
            item = item,
            message = item.snippet.text.orEmpty(),
            time = "33 minutes",
        )

        assertEquals(R.string.one_on_one_incoming_successful_message_prefix, spec.primaryResId)
        assertEquals("Bob", spec.senderOrConversationName)
        assertEquals("Hello there", spec.message)
        assertEquals("33 minutes", spec.time)
        assertEquals("Bob", spec.conversationName)
        assertFalse(spec.appendFailedDraftDescription)
    }

    @Test
    fun contentDescriptionSpec_groupIncomingSuccessful_usesSenderAndGroupDescription() {
        val item = previewConversationListItem(
            conversationId = "group_incoming",
            title = "Family",
            snippetText = "Hello there",
            senderName = "Alice",
            isGroup = true,
        )

        val spec = conversationListItemContentDescriptionSpec(
            item = item,
            message = item.snippet.text.orEmpty(),
            time = "33 minutes",
        )

        assertEquals(R.string.group_incoming_successful_message_prefix, spec.primaryResId)
        assertEquals("Alice", spec.senderOrConversationName)
        assertEquals("Hello there", spec.message)
        assertEquals("Family", spec.conversationName)
    }

    @Test
    fun contentDescriptionSpec_outgoingSending_usesSendingDescription() {
        val item = previewConversationListItem(
            conversationId = "sending",
            title = "Bob",
            snippetText = "On my way",
            status = ConversationListMessageStatus.Sending,
            isOutgoing = true,
        )

        val spec = conversationListItemContentDescriptionSpec(
            item = item,
            message = item.snippet.text.orEmpty(),
            time = "33 minutes",
        )

        assertEquals(R.string.one_on_one_outgoing_sending_message_prefix, spec.primaryResId)
        assertEquals("Bob", spec.senderOrConversationName)
    }

    @Test
    fun contentDescriptionSpec_draft_usesOutgoingDraftDescription() {
        val item = previewConversationListItem(
            conversationId = "draft",
            title = "Bob",
            snippetText = "Draft body",
            status = ConversationListMessageStatus.Draft,
            isDraft = true,
        )

        val spec = conversationListItemContentDescriptionSpec(
            item = item,
            message = item.snippet.text.orEmpty(),
            time = "33 minutes",
        )

        assertEquals(R.string.one_on_one_outgoing_draft_message_prefix, spec.primaryResId)
        assertEquals("Bob", spec.senderOrConversationName)
    }

    @Test
    fun contentDescriptionSpec_failedDraft_appendsFailedDraftDescription() {
        val item = previewConversationListItem(
            conversationId = "failed_draft",
            title = "Bob",
            snippetText = "Draft body",
            status = ConversationListMessageStatus.Failed(rawTelephonyStatus = 0),
            isOutgoing = true,
            isDraft = true,
        )

        val spec = conversationListItemContentDescriptionSpec(
            item = item,
            message = item.snippet.text.orEmpty(),
            time = "33 minutes",
        )

        assertEquals(R.string.one_on_one_outgoing_draft_message_prefix, spec.primaryResId)
        assertTrue(spec.appendFailedDraftDescription)
    }

    @Test
    fun badgeContentDescriptionResIds_visibleBadges_returnsStatusLabels() {
        val item = previewConversationListItem(
            conversationId = "badges",
            title = "Bob",
            snippetText = "Hello",
            isUnread = true,
            isEnterprise = true,
            isSnoozed = true,
            isPinned = true,
        )

        val resIds = conversationListItemBadgeContentDescriptionResIds(item)

        assertEquals(
            listOf(
                R.string.conversation_list_status_unread,
                R.string.conversation_list_status_work_profile,
                R.string.conversation_list_status_snoozed,
                R.string.conversation_list_status_pinned,
            ),
            resIds,
        )
    }

    @Test
    fun badgeContentDescriptionResIds_mutedAndSnoozed_prefersMutedStatus() {
        val item = previewConversationListItem(
            conversationId = "muted_snoozed",
            title = "Bob",
            snippetText = "Hello",
            isMuted = true,
            isSnoozed = true,
        )

        val resIds = conversationListItemBadgeContentDescriptionResIds(item)

        assertEquals(
            listOf(R.string.conversation_list_status_notifications_off),
            resIds,
        )
    }
}
