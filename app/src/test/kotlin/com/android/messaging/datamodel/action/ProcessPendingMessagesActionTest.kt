package com.android.messaging.datamodel.action

import com.android.messaging.datamodel.data.MessageData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcessPendingMessagesActionTest : OutgoingPipelineTestBase() {

    @Test
    fun queuedMessagesAreSentOneAtATimeInReceivedOrder() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val start = now()
        val newest = seedSms(conversationId = conversationId, receivedTimestamp = start - 1_000)
        val oldest = seedSms(conversationId = conversationId, receivedTimestamp = start - 3_000)
        val middle = seedSms(conversationId = conversationId, receivedTimestamp = start - 2_000)

        driver.processPendingMessages()

        val expectedOrder = listOf(oldest, middle, newest)
        assertEquals(
            expectedOrder.map { messageId -> world.message(messageId = messageId).smsMessageUri },
            telephony.smsSends.map { send -> send.messageUri.toString() },
        )
        telephony.smsSends.forEach { send ->
            assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, send.localStatusAtSend)
        }
        expectedOrder.forEach { messageId ->
            assertEquals(
                MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
                statusOf(messageId = messageId),
            )
        }
    }

    @Test
    fun messageAlreadySendingBlocksTheQueueOfItsSim() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val sending = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - 2_000,
            status = MessageData.BUGLE_STATUS_OUTGOING_SENDING,
        )
        val queued = seedSms(conversationId = conversationId, receivedTimestamp = now() - 1_000)

        driver.processPendingMessages()

        assertEquals(emptyList<PlatformSmsSend>(), telephony.smsSends)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, statusOf(messageId = sending))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND, statusOf(messageId = queued))
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun eachSimHasItsOwnQueue() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - 3_000,
            status = MessageData.BUGLE_STATUS_OUTGOING_SENDING,
        )
        val blockedOnSim1 = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - 2_000,
        )
        val queuedOnSim2 = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - 1_000,
            selfSubId = FakeTelephony.SUB_ID_SIM_2,
        )

        driver.processPendingMessages()

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            statusOf(messageId = blockedOnSim1),
        )
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = queuedOnSim2))
        assertEquals(FakeTelephony.SUB_ID_SIM_2, telephony.smsSends.single().subId)
    }

    @Test
    fun messageOnASimTheSystemListsButWithoutASlotFailsWithoutBeingSent() {
        telephony.activeSubscriptionIds = listOf(
            FakeTelephony.SUB_ID_SIM_1,
            FakeTelephony.SUB_ID_SIM_2,
            OutgoingPipelineWorld.SUB_ID_INACTIVE_SIM,
        )
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val queued = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - 1_000,
            selfSubId = OutgoingPipelineWorld.SUB_ID_INACTIVE_SIM,
        )

        driver.processPendingMessages()

        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = queued))
        assertEquals(emptyList<PlatformSmsSend>(), telephony.smsSends)
    }

    @Test
    fun messageOnASimMissingFromTheSystemStaysQueued() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val queued = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - 1_000,
            selfSubId = OutgoingPipelineWorld.SUB_ID_INACTIVE_SIM,
        )

        driver.processPendingMessages()

        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND, statusOf(messageId = queued))
        assertEquals(emptyList<PlatformSmsSend>(), telephony.smsSends)
        assertNull(
            driver.pendingRetryDelayMillis(subId = OutgoingPipelineWorld.SUB_ID_INACTIVE_SIM),
        )
    }

    @Test
    fun messageOutsideTheResendWindowFailsAndTheNextOneIsSent() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val expired = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - RESEND_WINDOW_MILLIS - 2_000,
            status = MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
        )
        val queued = seedSms(conversationId = conversationId, receivedTimestamp = now() - 1_000)

        driver.processPendingMessages()

        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = expired))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = queued))
        assertEquals(
            world.message(messageId = queued).smsMessageUri,
            telephony.smsSends.single().messageUri.toString(),
        )
    }

    @Test
    fun messageJustInsideTheResendWindowIsStillSent() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val retrying = seedSms(
            conversationId = conversationId,
            receivedTimestamp = now() - RESEND_WINDOW_MILLIS + 60_000,
            status = MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
        )

        driver.processPendingMessages()

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING,
            telephony.smsSends.single().localStatusAtSend,
        )
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = retrying))
    }

    @Test
    fun nothingIsSentWhileNotTheDefaultSmsAppAndARetryIsScheduled() {
        telephony.isDefaultSmsApp = false
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val queued = seedSms(conversationId = conversationId, receivedTimestamp = now() - 1_000)

        driver.processPendingMessages()

        assertEquals(emptyList<PlatformSmsSend>(), telephony.smsSends)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND, statusOf(messageId = queued))
        assertEquals(
            RETRY_REQUEST_DELAY_FIRST_MILLIS,
            driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1),
        )
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_2))
        assertEquals(1, world.pendingRetryAttempt(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    private fun seedSms(
        conversationId: String,
        receivedTimestamp: Long,
        status: Int = MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
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
