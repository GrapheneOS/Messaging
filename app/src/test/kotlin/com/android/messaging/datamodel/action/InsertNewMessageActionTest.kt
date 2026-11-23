package com.android.messaging.datamodel.action

import android.app.Activity
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import com.android.messaging.datamodel.data.MessageData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsertNewMessageActionTest : OutgoingPipelineTestBase() {

    @Test
    fun singleSegmentSmsIsWrittenToTelephonyAndSentOnItsConversationSim() {
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val message = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, message.status)
        assertEquals(MessageData.PROTOCOL_SMS, message.protocol)
        assertEquals(world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_1), message.selfId)
        val insert = telephony.smsInserts.single()
        assertEquals(FakeTelephony.SUB_ID_SIM_1, insert.subId)
        assertEquals(RECIPIENT_A, insert.address)
        assertEquals(SHORT_TEXT, insert.text)
        assertEquals(Sms.STATUS_NONE, insert.status)
        assertEquals(Sms.MESSAGE_TYPE_SENT, insert.type)
        assertEquals(insert.uri.toString(), message.smsMessageUri)
        val send = telephony.smsSends.single()
        assertEquals(RECIPIENT_A, send.destination)
        assertEquals(listOf(SHORT_TEXT), send.parts)
        assertEquals(listOf(0), send.partIds)
        assertEquals(FakeTelephony.SUB_ID_SIM_1, send.subId)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, send.localStatusAtSend)
        assertEquals(
            listOf(Sms.MESSAGE_TYPE_SENT, Sms.MESSAGE_TYPE_SENT),
            telephony.smsStatusUpdates.map { update -> update.typeOrBox },
        )
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun broadcastSmsKeepsOneSentCopyAndSendsOneMessagePerRecipientInItsOwnConversation() {
        val recipients = listOf(RECIPIENT_A, RECIPIENT_B)
        val conversationId = world.createConversation(recipients = recipients)

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val broadcast = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, broadcast.status)
        val broadcastInsert = telephony.smsInserts.first()
        assertEquals(broadcastInsert.uri.toString(), broadcast.smsMessageUri)
        assertEquals(recipients.joinToString(separator = " "), broadcastInsert.address)
        assertEquals(Sms.STATUS_COMPLETE, broadcastInsert.status)
        assertEquals(telephony.threadIdFor(recipients = recipients), broadcastInsert.threadId)
        val perRecipientInserts = telephony.smsInserts.drop(1)
        assertEquals(recipients, perRecipientInserts.map { insert -> insert.address })
        assertEquals(
            perRecipientInserts.map { insert -> insert.timestamp + 1 }.distinct(),
            listOf(broadcastInsert.timestamp),
        )
        recipients.forEach { recipient ->
            val oneToOneConversationId = requireNotNull(
                world.conversationIdForThread(
                    threadId = telephony.threadIdFor(recipients = listOf(recipient)),
                ),
            )
            val sent = world.messagesIn(conversationId = oneToOneConversationId).single()
            assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, sent.status)
            assertEquals(world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_1), sent.selfId)
        }
        assertEquals(
            recipients.sorted(),
            telephony.smsSends.map { send -> send.destination }.sorted(),
        )
        telephony.smsSends.forEach { send ->
            assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, send.localStatusAtSend)
            assertEquals(listOf(SHORT_TEXT), send.parts)
        }
    }

    @Test
    fun groupMmsIsWrittenToTelephonyOnceAndCompletesWhenThePlatformReportsSuccess() {
        val recipients = listOf(RECIPIENT_A, RECIPIENT_B)
        val conversationId = world.createConversation(recipients = recipients)

        driver.sendMmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        val sending = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.PROTOCOL_MMS, sending.protocol)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, sending.status)
        assertEquals(0L, sending.receivedTimestamp % MILLIS_PER_SECOND)
        val insert = telephony.mmsInserts.single()
        assertEquals(recipients, insert.recipients)
        assertEquals(FakeTelephony.SUB_ID_SIM_1, insert.subId)
        assertEquals(SHORT_TEXT, insert.text)
        assertEquals(insert.uri.toString(), sending.smsMessageUri)
        val send = telephony.mmsSends.single()
        assertEquals(FakeTelephony.SUB_ID_SIM_1, send.subId)
        assertEquals(insert.uri, send.messageUri)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, send.localStatusAtSend)
        assertEquals(
            sending.id,
            send.sentIntentExtras.getString(SendMessageAction.EXTRA_MESSAGE_ID),
        )
        assertEquals(emptyList<TelephonyStatusUpdate>(), telephony.mmsStatusUpdates)

        driver.deliverMmsSentResult(resultCode = Activity.RESULT_OK)

        val sent = world.message(messageId = sending.id)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, sent.status)
        assertEquals(insert.uri.toString(), sent.smsMessageUri)
        assertEquals(
            listOf(Mms.MESSAGE_BOX_SENT),
            telephony.mmsStatusUpdates.map { update -> update.typeOrBox },
        )
        assertNull(driver.pendingRetryDelayMillis(subId = FakeTelephony.SUB_ID_SIM_1))
    }

    @Test
    fun recipientsWithoutSubjectAreSentAsSmsOnTheRequestedSim() {
        driver.sendToRecipients(
            subId = FakeTelephony.SUB_ID_SIM_2,
            recipients = listOf(RECIPIENT_A),
            text = SHORT_TEXT,
            subject = null,
        )

        val conversationId = requireNotNull(
            world.conversationIdForThread(
                threadId = telephony.threadIdFor(recipients = listOf(RECIPIENT_A)),
            ),
        )
        val message = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.PROTOCOL_SMS, message.protocol)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, message.status)
        assertEquals(world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_2), message.selfId)
        assertEquals(FakeTelephony.SUB_ID_SIM_2, telephony.smsSends.single().subId)
    }

    @Test
    fun recipientsWithSubjectAreSentAsMms() {
        driver.sendToRecipients(
            subId = FakeTelephony.SUB_ID_SIM_1,
            recipients = listOf(RECIPIENT_A),
            text = SHORT_TEXT,
            subject = SUBJECT,
        )

        val message = world.allMessages().single()
        assertEquals(MessageData.PROTOCOL_MMS, message.protocol)
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_SENDING, message.status)
        assertEquals(SUBJECT, telephony.mmsInserts.single().subject)
        assertEquals(FakeTelephony.SUB_ID_SIM_1, telephony.mmsSends.single().subId)
        assertEquals(emptyList<PlatformSmsSend>(), telephony.smsSends)
    }

    @Test
    fun simPickedAtSendOverridesTheConversationSim() {
        val conversationId = world.createConversation(
            recipients = listOf(RECIPIENT_A),
            selfSubId = FakeTelephony.SUB_ID_SIM_1,
        )

        driver.sendSmsDraft(
            conversationId = conversationId,
            text = SHORT_TEXT,
            requestedSubId = FakeTelephony.SUB_ID_SIM_2,
        )

        assertSentOnSim(conversationId = conversationId, subId = FakeTelephony.SUB_ID_SIM_2)
    }

    @Test
    fun draftSimIsUsedWhenNoSimIsPickedAtSend() {
        val conversationId = world.createConversation(
            recipients = listOf(RECIPIENT_A),
            selfSubId = FakeTelephony.SUB_ID_SIM_1,
        )

        driver.sendSmsDraft(
            conversationId = conversationId,
            text = SHORT_TEXT,
            draftSelfSubId = FakeTelephony.SUB_ID_SIM_2,
        )

        assertSentOnSim(conversationId = conversationId, subId = FakeTelephony.SUB_ID_SIM_2)
    }

    @Test
    fun conversationSimIsUsedWhenTheDraftHasNoSim() {
        val conversationId = world.createConversation(
            recipients = listOf(RECIPIENT_A),
            selfSubId = FakeTelephony.SUB_ID_SIM_2,
        )

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        assertSentOnSim(conversationId = conversationId, subId = FakeTelephony.SUB_ID_SIM_2)
    }

    @Test
    fun conversationWithoutSimFollowsTheSystemDefaultSmsSim() {
        telephony.defaultSmsSubscriptionId = FakeTelephony.SUB_ID_SIM_2
        val conversationId = world.createConversation(recipients = listOf(RECIPIENT_A))

        driver.sendSmsDraft(conversationId = conversationId, text = SHORT_TEXT)

        assertSentOnSim(conversationId = conversationId, subId = FakeTelephony.SUB_ID_SIM_2)
    }

    private fun assertSentOnSim(conversationId: String, subId: Int) {
        val message = world.messagesIn(conversationId = conversationId).single()
        assertEquals(MessageData.BUGLE_STATUS_OUTGOING_COMPLETE, message.status)
        assertEquals(world.selfIdFor(subId = subId), message.selfId)
        assertEquals(subId, telephony.smsInserts.single().subId)
        assertEquals(subId, telephony.smsSends.single().subId)
    }

    private companion object {
        private const val SUBJECT = "subject"
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
