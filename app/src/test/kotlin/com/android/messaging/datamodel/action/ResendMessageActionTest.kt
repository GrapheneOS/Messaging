package com.android.messaging.datamodel.action

import com.android.messaging.datamodel.data.MessageData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResendMessageActionTest : OutgoingPipelineTestBase() {

    @Test
    fun failedSmsOlderThanTheResendWindowIsResentInANewWindow() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val originalTimestamp = now() - RESEND_WINDOW_MILLIS - 5 * 60_000
        val messageId = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            receivedTimestamp = originalTimestamp,
        )

        driver.resend(messageId = messageId)

        val send = telephony.smsSends.single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, send.localStatusAtSend)
        val resent = world.message(messageId = messageId)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, resent.status)
        assertTrue(resent.retryStartTimestamp > originalTimestamp)
        assertTrue(resent.receivedTimestamp >= resent.retryStartTimestamp)
        assertEquals(world.message(messageId = messageId).smsMessageUri, send.messageUri.toString())
    }

    @Test
    fun resentMessageGoesAheadOfNewerQueuedMessages() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val failed = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            receivedTimestamp = now() - 60_000,
        )
        val queued = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            receivedTimestamp = now() - 1_000,
        )
        val failedUri = world.message(messageId = failed).smsMessageUri
        val queuedUri = world.message(messageId = queued).smsMessageUri

        driver.resend(messageId = failed)

        assertEquals(
            listOf(failedUri, queuedUri),
            telephony.smsSends.map { send -> send.messageUri.toString() },
        )
    }

    @Test
    fun onlyFailedMessagesCanBeResent() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val statuses = listOf(
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER,
            MessageData.BUGLE_STATUS_OUTGOING_SENDING,
        )
        val messageIds = statuses.mapIndexed { index, status ->
            seedSms(
                conversationId = conversationId,
                status = status,
                receivedTimestamp = now() - 10_000 + index,
            )
        }

        messageIds.forEach { messageId -> driver.resend(messageId = messageId) }

        assertEquals(statuses, messageIds.map { messageId -> statusOf(messageId = messageId) })
        assertEquals(emptyList<PlatformSmsSend>(), telephony.smsSends)
    }

    @Test
    fun failedMmsIsResentOnItsSim() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val messageId = world.seedOutgoingMms(
            conversationId = conversationId,
            selfSubId = FakeTelephony.SUB_ID_SIM_2,
            text = SHORT_TEXT,
            status = MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            receivedTimestamp = now() - 60_000,
        )

        driver.resend(messageId = messageId)

        val send = telephony.mmsSends.single()
        assertEquals(FakeTelephony.SUB_ID_SIM_2, send.subId)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, send.localStatusAtSend)
        assertEquals(0L, world.message(messageId = messageId).retryStartTimestamp % 1_000L)
    }

    private fun seedSms(conversationId: String, status: Int, receivedTimestamp: Long): String {
        return world.seedOutgoingSms(
            conversationId = conversationId,
            selfSubId = FakeTelephony.SUB_ID_SIM_1,
            text = SHORT_TEXT,
            status = status,
            receivedTimestamp = receivedTimestamp,
        )
    }
}
