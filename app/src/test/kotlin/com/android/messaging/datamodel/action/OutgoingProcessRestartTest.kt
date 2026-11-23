package com.android.messaging.datamodel.action

import com.android.messaging.datamodel.data.MessageData
import org.junit.Assert.assertEquals
import org.junit.Test

class OutgoingProcessRestartTest : OutgoingPipelineTestBase() {

    @Test
    fun messagesInFlightWhenTheProcessDiedFailAndQueuedOnesAreSent() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val sending = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_SENDING,
            receivedTimestamp = now() - 4_000,
        )
        val resending = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_RESENDING,
            receivedTimestamp = now() - 3_000,
        )
        val queued = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            receivedTimestamp = now() - 2_000,
        )
        val retrying = seedSms(
            conversationId = conversationId,
            status = MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
            receivedTimestamp = now() - 1_000,
            selfSubId = FakeTelephony.SUB_ID_SIM_2,
        )
        val sendingMms = world.seedOutgoingMms(
            conversationId = conversationId,
            selfSubId = FakeTelephony.SUB_ID_SIM_2,
            text = SHORT_TEXT,
            status = MessageData.BUGLE_STATUS_OUTGOING_SENDING,
            receivedTimestamp = now() - 5_000,
        )

        driver.restartProcess()

        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = sending))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = resending))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = sendingMms))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = queued))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = retrying))
        assertEquals(
            listOf(queued, retrying).map { messageId ->
                requireNotNull(world.message(messageId = messageId).smsMessageUri)
            }.sorted(),
            telephony.smsSends.map { send -> send.messageUri.toString() }.sorted(),
        )
        assertEquals(emptyList<PlatformMmsSend>(), telephony.mmsSends)
    }

    private fun seedSms(
        conversationId: String,
        status: Int,
        receivedTimestamp: Long,
        selfSubId: Int = FakeTelephony.SUB_ID_SIM_1,
    ): String {
        return world.seedOutgoingSms(
            conversationId = conversationId,
            selfSubId = selfSubId,
            text = SHORT_TEXT,
            status = status,
            receivedTimestamp = receivedTimestamp,
        )
    }
}
