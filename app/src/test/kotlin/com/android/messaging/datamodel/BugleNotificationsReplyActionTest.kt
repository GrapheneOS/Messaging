package com.android.messaging.datamodel

import com.android.messaging.sms.MmsConfig
import com.android.messaging.testutil.stubMmsConfig
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BugleNotificationsReplyActionTest {

    @Before
    fun setUp() {
        mockkStatic(MmsConfig::class)
        stubMmsConfig(subId = NO_ALIAS_SUB_ID, aliasEnabled = false)
        stubMmsConfig(subId = ALIAS_SUB_ID, aliasEnabled = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(MmsConfig::class)
    }

    @Test
    fun aConversationWithAnAlphanumericSender_hasNoReplyAction() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            subId = NO_ALIAS_SUB_ID,
            otherParticipantDestination = "AMAZON",
        )

        assertFalse(BugleNotifications.canReplyFromNotification(conversation))
    }

    @Test
    fun aConversationWithAPhoneNumber_hasAReplyAction() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            subId = NO_ALIAS_SUB_ID,
            otherParticipantDestination = "+37254810027",
        )

        assertTrue(BugleNotifications.canReplyFromNotification(conversation))
    }

    @Test
    fun aConversationWithACarrierAlias_hasAReplyActionOnASimThatSupportsAliases() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            subId = ALIAS_SUB_ID,
            otherParticipantDestination = "john.doe",
        )

        assertTrue(BugleNotifications.canReplyFromNotification(conversation))
    }

    @Test
    fun aConversationWithACarrierAlias_hasNoReplyActionOnASimWithoutAliases() {
        val conversation = createNotificationConversation(
            conversationId = "conversation-1",
            subId = NO_ALIAS_SUB_ID,
            otherParticipantDestination = "john.doe",
        )

        assertFalse(BugleNotifications.canReplyFromNotification(conversation))
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

    private companion object {
        const val NO_ALIAS_SUB_ID = 1
        const val ALIAS_SUB_ID = 2
    }
}
