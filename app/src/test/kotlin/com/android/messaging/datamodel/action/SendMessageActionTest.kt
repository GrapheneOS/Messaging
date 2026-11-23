package com.android.messaging.datamodel.action

import android.app.Activity
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.sms.MmsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SendMessageActionTest : OutgoingPipelineTestBase() {

    @Test
    fun longSmsIsSentAsOneMultipartMessage() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val text = "x".repeat(FakeTelephony.SMS_SEGMENT_LENGTH + 40)

        driver.sendSmsDraft(conversationId = conversationId, text = text)

        val send = telephony.smsSends.single()
        assertEquals(text.chunked(FakeTelephony.SMS_SEGMENT_LENGTH), send.parts)
        assertEquals(listOf(1, 2), send.partIds)
        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
            world.messagesIn(conversationId = conversationId).single().status,
        )
    }

    @Test
    fun partFailingPermanentlyFailsTheWholeMultipartMessage() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        telephony.scriptSmsPartResults(Activity.RESULT_OK, SmsManager.RESULT_ERROR_GENERIC_FAILURE)

        driver.sendSmsDraft(
            conversationId = conversationId,
            text = "x".repeat(FakeTelephony.SMS_SEGMENT_LENGTH + 1),
        )

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            world.messagesIn(conversationId = conversationId).single().status,
        )
    }

    @Test
    fun temporaryFailureWaitsForTheBackoffAlarmAndResends() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        telephony.scriptSmsPartResults(SmsManager.RESULT_ERROR_NO_SERVICE)
        telephony.scriptSmsPartResults(SmsManager.RESULT_ERROR_NO_SERVICE)

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val messageId = world.messagesIn(conversationId = conversationId).single().id
        val afterFirstFailure = world.message(messageId = messageId)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY, afterFirstFailure.status)
        assertEquals(afterFirstFailure.receivedTimestamp, afterFirstFailure.retryStartTimestamp)
        assertEquals(Sms.MESSAGE_TYPE_SENT, telephony.smsStatusUpdates.last().typeOrBox)
        assertEquals(emptyList<Int>(), telephony.errorNotificationUpdates)
        assertEquals(
            RETRY_REQUEST_DELAY_FIRST_MILLIS,
            driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1),
        )
        assertEquals(1, world.pendingRetryAttempt(subId = FakeTelephony.SUB_ID_SIM_1))

        driver.fireRetryAlarm(subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING,
            telephony.smsSends.last().localStatusAtSend,
        )
        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
            statusOf(messageId = messageId),
        )
        assertEquals(
            RETRY_REQUEST_DELAY_SECOND_MILLIS,
            driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1),
        )
        assertEquals(2, world.pendingRetryAttempt(subId = FakeTelephony.SUB_ID_SIM_1))

        driver.fireRetryAlarm(subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(3, telephony.smsSends.size)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = messageId))
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
        assertEquals(0, world.pendingRetryAttempt(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun permanentFailureMarksTheMessageFailedUnseenAndHoldsTheQueueUntilTheAlarm() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        val failing = world.seedOutgoingSms(
            conversationId = conversationId,
            selfSubId = FakeTelephony.SUB_ID_SIM_1,
            text = SHORT_TEXT,
            status = MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            receivedTimestamp = now() - 2_000,
        )
        val next = world.seedOutgoingSms(
            conversationId = conversationId,
            selfSubId = FakeTelephony.SUB_ID_SIM_1,
            text = SHORT_TEXT,
            status = MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            receivedTimestamp = now() - 1_000,
        )
        telephony.scriptSmsPartResults(SmsManager.RESULT_ERROR_GENERIC_FAILURE)

        driver.processPendingMessages()

        val failed = world.message(messageId = failing)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, failed.status)
        assertFalse(failed.seen)
        assertEquals(Sms.MESSAGE_TYPE_FAILED, telephony.smsStatusUpdates.last().typeOrBox)
        assertEquals(listOf(BugleNotifications.UPDATE_ERRORS), telephony.errorNotificationUpdates)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND, statusOf(messageId = next))
        assertEquals(1, telephony.smsSends.size)
        assertEquals(
            RETRY_REQUEST_DELAY_FIRST_MILLIS,
            driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1),
        )

        driver.fireRetryAlarm(subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, statusOf(messageId = next))
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = failing))
    }

    @Test
    fun permanentFailureOfTheLastQueuedMessageSchedulesNoAlarm() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        telephony.scriptSmsPartResults(SmsManager.RESULT_ERROR_RADIO_OFF)

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            world.messagesIn(conversationId = conversationId).single().status,
        )
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
        assertEquals(0, world.pendingRetryAttempt(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun anyFailureToAnEmergencyNumberFailsAtOnceAndNotifies() {
        val conversationId = world.createConversation(
            recipients = listOf(FakeTelephony.EMERGENCY_NUMBER),
        )
        telephony.scriptSmsPartResults(SmsManager.RESULT_ERROR_NO_SERVICE)

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val message = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER, message.status)
        assertEquals(listOf(FakeTelephony.EMERGENCY_NUMBER), telephony.emergencyNotifications)
        assertEquals(listOf(BugleNotifications.UPDATE_ERRORS), telephony.errorNotificationUpdates)
        assertEquals(Sms.MESSAGE_TYPE_FAILED, telephony.smsStatusUpdates.last().typeOrBox)
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun mmsThatCannotBeWrittenToTelephonyFailsWithoutBeingSent() {
        telephony.failMmsInsert = true
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))

        driver.sendMmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val message = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, message.status)
        assertNull(message.smsMessageUri)
        assertEquals(emptyList<PlatformMmsSend>(), telephony.mmsSends)
    }

    @Test
    fun mmsRejectedBeforeReachingThePlatformFollowsTheReturnedStatus() {
        telephony.scriptMmsSendOutcome(
            MmsUtils.StatusPlusUri(
                MmsUtils.MMS_REQUEST_AUTO_RETRY,
                MessageData.RAW_TELEPHONY_STATUS_UNDEFINED,
                null,
            ),
        )
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))

        driver.sendMmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val message = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY, message.status)
        assertEquals(telephony.mmsInserts.single().uri.toString(), message.smsMessageUri)
        assertEquals(
            RETRY_REQUEST_DELAY_FIRST_MILLIS,
            driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1),
        )
    }

    @Test
    fun mmsIoErrorFromThePlatformIsRetried() {
        val messageId = sendMmsAndDeliver(resultCode = SmsManager.MMS_ERROR_IO_ERROR)

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
            statusOf(messageId = messageId),
        )
        assertEquals(
            RETRY_REQUEST_DELAY_FIRST_MILLIS,
            driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1),
        )

        driver.fireRetryAlarm(subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(1, telephony.mmsInserts.size)
        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING,
            telephony.mmsSends.last().localStatusAtSend,
        )
    }

    @Test
    fun mmsHttpNotFoundFromThePlatformFails() {
        val messageId = sendMmsAndDeliver(
            resultCode = SmsManager.MMS_ERROR_HTTP_FAILURE,
            httpStatus = HTTP_NOT_FOUND,
        )

        val message = world.message(messageId = messageId)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, message.status)
        assertFalse(message.seen)
        assertEquals(Mms.MESSAGE_BOX_FAILED, telephony.mmsStatusUpdates.last().typeOrBox)
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun mmsRejectedByTheCarrierInTheSendConfirmationFails() {
        telephony.mmsSendConfStatus = MmsUtils.MMS_REQUEST_MANUAL_RETRY

        val messageId = sendMmsAndDeliver(resultCode = Activity.RESULT_OK)

        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_FAILED, statusOf(messageId = messageId))
    }

    @Test
    fun mmsAcceptedByThePlatformWithoutASendConfirmationFails() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        driver.sendMmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        driver.deliverMmsSentResult(
            resultCode = Activity.RESULT_OK,
            withSendConfirmation = false,
        )

        assertEquals(
            MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            world.messagesIn(conversationId = conversationId).single().status,
        )
    }

    private fun sendMmsAndDeliver(resultCode: Int, httpStatus: Int = 0): String {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))
        driver.sendMmsDraft(conversationId = conversationId, text = SHORT_TEXT)
        driver.deliverMmsSentResult(resultCode = resultCode, httpStatus = httpStatus)
        return world.messagesIn(conversationId = conversationId).single().id
    }

    private companion object {
        private const val HTTP_NOT_FOUND = 404
    }
}
