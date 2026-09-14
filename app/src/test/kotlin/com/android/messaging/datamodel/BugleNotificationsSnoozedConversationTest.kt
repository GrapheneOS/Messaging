package com.android.messaging.datamodel

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Regression test for BUG-014.
 *
 * A snoozed conversation must post no notification, and its unseen messages must not stop another
 * conversation from posting one. The only snooze check used to sit in [BugleNotifications.update],
 * where it guards the caller's argument -- so it was bypassed entirely whenever the caller passed
 * no conversation id, as the boot and app-update paths do.
 */
@RunWith(RobolectricTestRunner::class)
class BugleNotificationsSnoozedConversationTest {

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
    fun createMessageNotification_whenSnoozedConversationIsNewest_notifiesTheOtherOne() {
        // The device repro: the snoozed conversation holds the newest unseen message, so it is the
        // one the selection lands on -- and the conversation that actually received a message
        // stays silent.
        val snoozed = createNotificationConversation(SNOOZED_CONVERSATION_ID)
        val allowed = createNotificationConversation(ALLOWED_CONVERSATION_ID)
        givenUnseenMessages(snoozed, allowed)

        BugleNotifications.createMessageNotification(ALLOWED_CONVERSATION_ID)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), allowed) }
        verify(exactly = 0) { BugleNotifications.processAndSend(any(), snoozed) }
    }

    @Test
    fun createMessageNotification_withoutConversationId_skipsSnoozedConversation() {
        // Boot and app update pass no conversation id, which bypasses the guard in update().
        val snoozed = createNotificationConversation(SNOOZED_CONVERSATION_ID)
        givenUnseenMessages(snoozed)

        BugleNotifications.createMessageNotification(null)

        verify(exactly = 0) { BugleNotifications.processAndSend(any(), any()) }
    }

    private fun stubConversationLookup() {
        mockkStatic(ConversationListItemData::class)
        every { ConversationListItemData.getExistingConversation(database, any()) } returns null
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
        private const val SNOOZED_CONVERSATION_ID = "194"
        private const val ALLOWED_CONVERSATION_ID = "195"
    }
}
