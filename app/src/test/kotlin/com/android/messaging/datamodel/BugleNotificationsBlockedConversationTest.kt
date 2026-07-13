package com.android.messaging.datamodel

import android.media.AudioManager
import android.net.Uri
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.RingtoneUtil
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

@RunWith(RobolectricTestRunner::class)
class BugleNotificationsBlockedConversationTest {

    private val database = mockk<DatabaseWrapper>(relaxed = true)
    private val dataModel = mockk<DataModel>(relaxed = true)

    @Before
    fun setUp() {
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            dataModel = dataModel,
        )
        every { dataModel.getDatabase() } returns database
        silenceRinger()
        stubConversationLookup()
        stubNotificationDelivery()
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun createMessageNotification_withBlockedConversation_postsNoUnrelatedNotification() {
        givenUnseenMessage(ALLOWED_CONVERSATION_ID)

        BugleNotifications.createMessageNotification(BLOCKED_CONVERSATION_ID)

        verify(exactly = 0) { BugleNotifications.processAndSend(any(), any()) }
    }

    @Test
    fun createMessageNotification_withAllowedConversation_postsNotification() {
        val conversation = givenUnseenMessage(ALLOWED_CONVERSATION_ID)

        BugleNotifications.createMessageNotification(ALLOWED_CONVERSATION_ID)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), conversation) }
    }

    @Test
    fun createMessageNotification_withoutConversationId_skipsBlockedConversationInState() {
        val blocked = createNotificationConversation(BLOCKED_CONVERSATION_ID)
        val allowed = createNotificationConversation(ALLOWED_CONVERSATION_ID)
        givenUnseenMessages(blocked, allowed)

        BugleNotifications.createMessageNotification(null)

        verify(exactly = 1) { BugleNotifications.processAndSend(any(), allowed) }
    }

    @Test
    fun createMessageNotification_withBlockedObservableConversation_playsNoSound() {
        givenNoUnseenMessages()
        givenConversationObservable(BLOCKED_CONVERSATION_ID)

        BugleNotifications.createMessageNotification(BLOCKED_CONVERSATION_ID)

        verify(exactly = 0) { RingtoneUtil.getNotificationRingtoneUri(any(), any()) }
    }

    @Test
    fun createMessageNotification_withAllowedObservableConversation_playsSound() {
        givenNoUnseenMessages()
        givenConversationObservable(ALLOWED_CONVERSATION_ID)

        BugleNotifications.createMessageNotification(ALLOWED_CONVERSATION_ID)

        verify(exactly = 1) {
            RingtoneUtil.getNotificationRingtoneUri(ALLOWED_CONVERSATION_ID, null)
        }
    }

    private fun stubConversationLookup() {
        mockkStatic(ConversationListItemData::class)
        stubConversation(BLOCKED_CONVERSATION_ID, BLOCKED_SENDER)
        stubConversation(ALLOWED_CONVERSATION_ID, ALLOWED_SENDER)
        mockkStatic(BugleDatabaseOperations::class)
        every {
            BugleDatabaseOperations.isBlockedDestination(database, BLOCKED_SENDER)
        } returns true
        every {
            BugleDatabaseOperations.isBlockedDestination(database, ALLOWED_SENDER)
        } returns false
    }

    private fun stubConversation(
        conversationId: String,
        sender: String,
    ) {
        val convData = mockk<ConversationListItemData>(relaxed = true)
        every { convData.otherParticipantNormalizedDestination } returns sender
        every { convData.notificationSoundUri } returns null
        every {
            ConversationListItemData.getExistingConversation(database, conversationId)
        } returns convData
    }

    private fun stubNotificationDelivery() {
        mockkStatic(MessageNotificationState::class)
        mockkStatic(RingtoneUtil::class)
        every { RingtoneUtil.getNotificationRingtoneUri(any(), any()) } returns RINGTONE_URI
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.processAndSend(any(), any()) } just runs
    }

    private fun silenceRinger() {
        RuntimeEnvironment.getApplication()
            .getSystemService(AudioManager::class.java)
            .ringerMode = AudioManager.RINGER_MODE_SILENT
    }

    private fun givenUnseenMessage(
        conversationId: String,
    ): MessageNotificationState.Conversation {
        return createNotificationConversation(conversationId).also { conversation ->
            givenUnseenMessages(conversation)
        }
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

    private fun givenNoUnseenMessages() {
        every { MessageNotificationState.getNotificationState() } returns null
    }

    private fun givenConversationObservable(conversationId: String) {
        every { dataModel.isNewMessageObservable(conversationId) } returns true
    }

    private companion object {
        private const val BLOCKED_CONVERSATION_ID = "193"
        private const val BLOCKED_SENDER = "+15551234567"
        private const val ALLOWED_CONVERSATION_ID = "194"
        private const val ALLOWED_SENDER = "+15557654321"
        private val RINGTONE_URI: Uri = Uri.parse("content://settings/system/notification_sound")
    }
}
