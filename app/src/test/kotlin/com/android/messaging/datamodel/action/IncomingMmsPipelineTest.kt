package com.android.messaging.datamodel.action

import android.telephony.SmsManager
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.mmslib.pdu.PduHeaders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingMmsPipelineTest : IncomingPipelineTestBase() {

    @Test
    fun notificationFromNewSenderStartsAnAutomaticDownloadWithoutNotifying() {
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        val conversationId = onlyConversationId()
        val message = onlyMessage()
        assertEquals(
            listOf(SENDER_A),
            world.conversation(conversationId = conversationId).participantDestinations,
        )
        assertEquals(MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION, message.protocol)
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING, message.status)
        val download = incomingMms.downloads.single()
        assertEquals(FakeTelephony.SUB_ID_SIM_1, download.subId)
        assertEquals(
            MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
            download.localStatusAtDownload,
        )
        assertEquals(message.id, download.extras.getString(DownloadMmsAction.EXTRA_MESSAGE_ID))
        assertEquals(
            conversationId,
            download.extras.getString(DownloadMmsAction.EXTRA_CONVERSATION_ID),
        )
        assertTrue(download.extras.getBoolean(DownloadMmsAction.EXTRA_AUTO_DOWNLOAD))
        assertEquals(
            emptyList<ConversationNotificationUpdate>(),
            telephony.conversationNotificationUpdates,
        )
        assertEquals(emptyList<MmsNotifyResponse>(), incomingMms.notifyResponses)
    }

    @Test
    fun notificationWithAutoRetrieveOffWaitsForTheUserAndDefersTheMmsc() {
        disableMmsAutoRetrieve(subId = FakeTelephony.SUB_ID_SIM_1)

        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        val conversationId = onlyConversationId()
        val message = onlyMessage()
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD, message.status)
        assertEquals(
            message.id,
            world.conversation(conversationId = conversationId).latestMessageId,
        )
        assertEquals(emptyList<PlatformMmsDownload>(), incomingMms.downloads)
        assertEquals(
            listOf(messageNotificationFor(conversationId = conversationId)),
            telephony.conversationNotificationUpdates,
        )
        val response = incomingMms.notifyResponses.single()
        assertEquals(FakeTelephony.SUB_ID_SIM_1, response.subId)
        assertEquals(PduHeaders.STATUS_DEFERRED, response.status)
    }

    @Test
    fun notificationFromBlockedSenderIsNeverDownloadedAutomatically() {
        world.blockDestination(destination = SENDER_A)

        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        val message = onlyMessage()
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD, message.status)
        assertTrue(message.seen)
        assertTrue(world.conversation(conversationId = onlyConversationId()).archived)
        assertEquals(emptyList<PlatformMmsDownload>(), incomingMms.downloads)
    }

    @Test
    fun notificationInTheConversationOnScreenIsRead() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))
        world.focusConversation(conversationId = conversationId)
        disableMmsAutoRetrieve(subId = FakeTelephony.SUB_ID_SIM_1)

        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        val message = onlyMessage()
        assertEquals(conversationId, message.conversationId)
        assertTrue(message.read)
        assertTrue(message.seen)
    }

    @Test
    fun completedDownloadReplacesTheNotificationRowAndNotifies() {
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)
        val notificationId = onlyMessage().id

        driver.completeMmsDownload(content = DownloadedMms(sender = SENDER_A, text = TEXT))

        val conversationId = onlyConversationId()
        val message = onlyMessage()
        val insert = incomingMms.downloadedInserts.single()
        assertEquals(notificationId, message.id)
        assertEquals(MessageData.PROTOCOL_MMS, message.protocol)
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.status)
        assertEquals(TEXT, message.text)
        assertEquals(SENDER_A, message.senderDestination)
        assertEquals(insert.uri.toString(), message.smsMessageUri)
        assertTrue(insert.autoDownload)
        assertFalse(message.read)
        assertFalse(message.seen)
        val conversation = world.conversation(conversationId = conversationId)
        assertEquals(message.id, conversation.latestMessageId)
        assertEquals(TEXT, conversation.snippet)
        assertEquals(
            listOf(messageNotificationFor(conversationId = conversationId)),
            telephony.conversationNotificationUpdates,
        )
    }

    @Test
    fun completedDownloadInTheConversationOnScreenIsReadAndSeen() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)
        world.focusConversation(conversationId = conversationId)

        driver.completeMmsDownload(content = DownloadedMms(sender = SENDER_A, text = TEXT))

        val message = onlyMessage()
        assertTrue(message.read)
        assertTrue(message.seen)
    }

    @Test
    fun completedGroupDownloadMovesTheMessageAndDropsTheEmptyOneToOneConversation() {
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)
        val oneToOneConversationId = onlyConversationId()

        driver.completeMmsDownload(
            content = DownloadedMms(
                sender = SENDER_A,
                text = TEXT,
                otherRecipients = listOf(SENDER_B, SENDER_C),
            ),
        )

        val groupConversationId = onlyConversationId()
        val group = world.conversation(conversationId = groupConversationId)
        assertTrue(groupConversationId != oneToOneConversationId)
        assertEquals(threadIdFor(SENDER_A, SENDER_B, SENDER_C), group.threadId)
        assertEquals(listOf(SENDER_A, SENDER_B, SENDER_C), group.participantDestinations)
        assertEquals(groupConversationId, onlyMessage().conversationId)
        assertEquals(SENDER_A, onlyMessage().senderDestination)
    }

    @Test
    fun completedGroupDownloadKeepsTheOneToOneConversationThatHasOtherMessages() {
        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)
        val oneToOneConversationId = onlyConversationId()
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        driver.completeMmsDownload(
            content = DownloadedMms(
                sender = SENDER_A,
                text = SECOND_TEXT,
                otherRecipients = listOf(SENDER_B),
            ),
        )

        assertEquals(2, world.conversationIds().size)
        assertEquals(
            listOf(TEXT),
            world.incomingMessagesIn(conversationId = oneToOneConversationId).map { message ->
                message.text
            },
        )
    }

    @Test
    fun manualDownloadRunsOnlyWhenTheUserAsks() {
        disableMmsAutoRetrieve(subId = FakeTelephony.SUB_ID_SIM_1)
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)
        val messageId = onlyMessage().id

        driver.redownloadMms(messageId = messageId)

        val download = incomingMms.downloads.single()
        assertEquals(
            MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING,
            download.localStatusAtDownload,
        )
        assertFalse(download.extras.getBoolean(DownloadMmsAction.EXTRA_AUTO_DOWNLOAD))

        driver.completeMmsDownload(content = DownloadedMms(sender = SENDER_A, text = TEXT))

        val message = world.incomingMessage(messageId = messageId)
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.status)
        assertFalse(incomingMms.downloadedInserts.single().autoDownload)
    }

    @Test
    fun automaticDownloadLosingTheConnectionWaitsToRetryAutomatically() {
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        driver.failMmsDownload(resultCode = SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS)

        assertEquals(MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD, onlyMessage().status)
        assertEquals(1, incomingMms.downloads.size)
        assertEquals(emptyList<MmsNotifyResponse>(), incomingMms.notifyResponses)
    }

    @Test
    fun automaticDownloadThatNeedsTheUserFailsAndDefersTheMmsc() {
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)
        val conversationId = onlyConversationId()

        driver.failMmsDownload(resultCode = SmsManager.MMS_ERROR_UNSPECIFIED)

        assertEquals(MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED, onlyMessage().status)
        assertEquals(PduHeaders.STATUS_DEFERRED, incomingMms.notifyResponses.single().status)
        assertEquals(
            onlyMessage().id,
            world.conversation(conversationId = conversationId).latestMessageId,
        )
    }

    @Test
    fun downloadOfAnExpiredMessageIsNotRetried() {
        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_1)

        driver.failMmsDownload(
            resultCode = SmsManager.MMS_ERROR_HTTP_FAILURE,
            httpStatus = HTTP_NOT_FOUND,
        )

        assertEquals(
            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE,
            onlyMessage().status,
        )
    }

    @Test
    fun notificationOnTheSecondSimIsDownloadedOnThatSimAndSwitchesTheConversation() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))

        driver.receiveMmsNotification(sender = SENDER_A, subId = FakeTelephony.SUB_ID_SIM_2)
        driver.completeMmsDownload(content = DownloadedMms(sender = SENDER_A, text = TEXT))

        val secondSimSelfId = world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_2)
        assertEquals(FakeTelephony.SUB_ID_SIM_2, incomingMms.downloads.single().subId)
        assertEquals(FakeTelephony.SUB_ID_SIM_2, incomingMms.downloadedInserts.single().subId)
        assertEquals(secondSimSelfId, onlyMessage().selfId)
        assertEquals(secondSimSelfId, world.conversation(conversationId = conversationId).selfId)
    }

    private companion object {
        private const val HTTP_NOT_FOUND = 404
    }
}
