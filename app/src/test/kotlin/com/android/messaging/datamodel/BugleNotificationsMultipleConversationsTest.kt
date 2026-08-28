package com.android.messaging.datamodel

import androidx.core.app.NotificationManagerCompat
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversationsettings.repository.ConversationSnoozeQuery
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.testutil.installTestFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BugleNotificationsMultipleConversationsTest {

    private val database = mockk<DatabaseWrapper>(relaxed = true)
    private val dataModel = mockk<DataModel>(relaxed = true)

    @Before
    fun setUp() {
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            dataModel = dataModel,
        )
        every { dataModel.getDatabase() } returns database
        stubConversationLookup()
        stubSnoozeLookup()
        mockkStatic(MessageNotificationState::class)
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.processAndSend(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun createMessageNotification_withSeveralUnseenConversations_notifiesEveryConversation() {
        val newest = createNotificationConversation("18")
        val older = createNotificationConversation("19")
        val oldest = createNotificationConversation("23")
        givenUnseenMessages(newest, older, oldest)

        BugleNotifications.createMessageNotification(newest.mConversationId)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), newest) }
        verify(exactly = 1) { BugleNotifications.processAndSend(any(), older) }
        verify(exactly = 1) { BugleNotifications.processAndSend(any(), oldest) }
    }

    @Test
    fun createMessageNotification_forOlderConversation_stillNotifiesThatConversation() {
        // The MMS-download and delete-conversation paths call update() for a conversation that is
        // not the newest unseen one. That conversation is the whole point of the call.
        val newest = createNotificationConversation("18")
        val downloaded = createNotificationConversation("19")
        givenUnseenMessages(newest, downloaded)

        BugleNotifications.createMessageNotification(downloaded.mConversationId)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), downloaded) }
    }

    @Test
    fun createMessageNotification_withBlockedConversationAmongUnseen_notifiesTheRest() {
        val newest = createNotificationConversation("18")
        val blocked = createNotificationConversation(BLOCKED_CONVERSATION_ID)
        val oldest = createNotificationConversation("23")
        givenUnseenMessages(newest, blocked, oldest)

        BugleNotifications.createMessageNotification(newest.mConversationId)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), newest) }
        verify(exactly = 1) { BugleNotifications.processAndSend(any(), oldest) }
        verify(exactly = 0) { BugleNotifications.processAndSend(any(), blocked) }
        verify(exactly = 2) { BugleNotifications.processAndSend(any(), any()) }
    }

    @Test
    fun createMessageNotification_withSnoozedConversationAmongUnseen_notifiesTheRest() {
        // update() only rejects a snoozed *argument*; nothing filtered the list itself, so
        // iterating it would notify every snoozed conversation on every update (BUG-014).
        val newest = createNotificationConversation("18")
        val snoozed = createNotificationConversation(SNOOZED_CONVERSATION_ID)
        givenUnseenMessages(newest, snoozed)

        BugleNotifications.createMessageNotification(newest.mConversationId)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), newest) }
        verify(exactly = 0) { BugleNotifications.processAndSend(any(), snoozed) }
    }

    @Test
    fun createMessageNotification_withMoreUnseenConversationsThanTheCap_notifiesTheNewestOnly() {
        val conversations = (1..BugleNotifications.MAX_CONVERSATION_NOTIFICATIONS + 3)
            .map { createNotificationConversation(it.toString()) }
        givenUnseenMessages(*conversations.toTypedArray())

        BugleNotifications.createMessageNotification(conversations.first().mConversationId)

        verify(exactly = BugleNotifications.MAX_CONVERSATION_NOTIFICATIONS) {
            BugleNotifications.processAndSend(any(), any())
        }
        conversations.take(BugleNotifications.MAX_CONVERSATION_NOTIFICATIONS).forEach {
            verify(exactly = 1) { BugleNotifications.processAndSend(any(), it) }
        }
        assertTrue(
            "the conversations over the cap were dropped without a trace",
            activeNotificationTags().any {
                it.endsWith(BugleNotifications.SMS_OVERFLOW_NOTIFICATION_TAG)
            },
        )
    }

    @Test
    fun createMessageNotification_withFewerUnseenConversationsThanTheCap_postsNoOverflow() {
        givenUnseenMessages(createNotificationConversation("18"))

        BugleNotifications.createMessageNotification("18")

        assertTrue(
            "an overflow notification was posted for a single conversation",
            activeNotificationTags().none {
                it.endsWith(BugleNotifications.SMS_OVERFLOW_NOTIFICATION_TAG)
            },
        )
    }

    @Test
    fun createMessageNotification_whenUnseenConversationsFallBackUnderTheCap_cancelsTheOverflow() {
        val conversations = (1..BugleNotifications.MAX_CONVERSATION_NOTIFICATIONS + 3)
            .map { createNotificationConversation(it.toString()) }
        givenUnseenMessages(*conversations.toTypedArray())
        BugleNotifications.createMessageNotification(conversations.first().mConversationId)

        givenUnseenMessages(*conversations.take(2).toTypedArray())
        BugleNotifications.createMessageNotification(conversations.first().mConversationId)

        assertTrue(
            "the overflow notification outlived the conversations it stood for",
            activeNotificationTags().none {
                it.endsWith(BugleNotifications.SMS_OVERFLOW_NOTIFICATION_TAG)
            },
        )
    }

    private fun activeNotificationTags(): List<String> {
        return NotificationManagerCompat.from(RuntimeEnvironment.getApplication())
            .activeNotifications
            .map { it.tag }
    }

    private fun stubConversationLookup() {
        mockkStatic(ConversationListItemData::class)
        every { ConversationListItemData.getExistingConversation(database, any()) } returns null

        val blockedData = mockk<ConversationListItemData>(relaxed = true)
        every { blockedData.otherParticipantNormalizedDestination } returns BLOCKED_SENDER
        every {
            ConversationListItemData.getExistingConversation(database, BLOCKED_CONVERSATION_ID)
        } returns blockedData

        mockkStatic(BugleDatabaseOperations::class)
        every {
            BugleDatabaseOperations.isBlockedDestination(database, BLOCKED_SENDER)
        } returns true
    }

    private fun stubSnoozeLookup() {
        mockkStatic(ConversationSnoozeQuery::class)
        every { ConversationSnoozeQuery.isConversationSnoozed(any()) } returns false
        every {
            ConversationSnoozeQuery.isConversationSnoozed(SNOOZED_CONVERSATION_ID)
        } returns true
    }

    private fun givenUnseenMessages(
        vararg conversations: MessageNotificationState.Conversation,
    ) {
        val conversationsList = MessageNotificationState.ConversationsList(
            conversations.size,
            conversations.toList(),
        )
        every {
            MessageNotificationState.getNotificationState()
        } returns MessageNotificationState(conversationsList)
    }

    private companion object {
        private const val BLOCKED_CONVERSATION_ID = "193"
        private const val BLOCKED_SENDER = "+15551234567"
        private const val SNOOZED_CONVERSATION_ID = "194"
    }
}
