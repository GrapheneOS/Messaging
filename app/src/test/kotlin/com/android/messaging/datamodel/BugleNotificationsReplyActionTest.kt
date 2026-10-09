package com.android.messaging.datamodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BugleNotificationsReplyActionTest {

    @Test
    fun aConversationWithAnAlphanumericSender_hasNoReplyAction() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            otherParticipantDestination = "AMAZON",
        )

        assertFalse(BugleNotifications.canReplyFromNotification(conversation))
    }

    @Test
    fun aConversationWithAPhoneNumber_hasAReplyAction() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            otherParticipantDestination = "+37254810027",
        )

        assertTrue(BugleNotifications.canReplyFromNotification(conversation))
    }

    @Test
    fun aGroupConversation_hasAReplyAction() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            isGroup = true,
            participantCount = 3,
        )

        assertTrue(BugleNotifications.canReplyFromNotification(conversation))
    }
}
