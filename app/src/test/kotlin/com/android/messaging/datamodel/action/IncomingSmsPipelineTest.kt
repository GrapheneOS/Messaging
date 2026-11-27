package com.android.messaging.datamodel.action

import android.provider.Telephony.Sms
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingSmsPipelineTest : IncomingPipelineTestBase() {

    @Test
    fun smsFromNewSenderCreatesAnUnreadConversationAndNotifies() {
        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        val conversationId = onlyConversationId()
        val conversation = world.conversation(conversationId = conversationId)
        val message = onlyMessage()
        assertEquals(threadIdFor(SENDER_A), conversation.threadId)
        assertEquals(listOf(SENDER_A), conversation.participantDestinations)
        assertFalse(conversation.archived)
        assertEquals(message.id, conversation.latestMessageId)
        assertEquals(TEXT, conversation.snippet)
        assertEquals(conversationId, message.conversationId)
        assertEquals(SENDER_A, message.senderDestination)
        assertEquals(MessageData.PROTOCOL_SMS, message.protocol)
        assertEquals(MessageData.BUGLE_STATUS_INCOMING_COMPLETE, message.status)
        assertEquals(TEXT, message.text)
        assertFalse(message.read)
        assertFalse(message.seen)
        assertEquals(
            listOf(messageNotificationFor(conversationId = conversationId)),
            telephony.conversationNotificationUpdates,
        )
    }

    @Test
    fun smsIsStoredInTheTelephonyInboxAsSeenBeforeTheLocalRow() {
        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        val insert = telephonySmsInboxInserts().single()
        assertEquals(SENDER_A, insert.getAsString(Sms.ADDRESS))
        assertEquals(TEXT, insert.getAsString(Sms.BODY))
        assertEquals(threadIdFor(SENDER_A), insert.getAsLong(Sms.THREAD_ID))
        assertEquals(0, insert.getAsInteger(Sms.Inbox.READ))
        assertEquals(1, insert.getAsInteger(Sms.Inbox.SEEN))
        assertTrue(
            onlyMessage().smsMessageUri.orEmpty().startsWith(Sms.Inbox.CONTENT_URI.toString()),
        )
    }

    @Test
    fun smsFromKnownSenderLandsInTheExistingConversation() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)
        driver.receiveSms(sender = SENDER_A, text = SECOND_TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(listOf(conversationId), world.conversationIds())
        val messages = world.incomingMessagesIn(conversationId = conversationId)
        assertEquals(listOf(TEXT, SECOND_TEXT), messages.map { message -> message.text })
        assertEquals(
            messages.last().id,
            world.conversation(conversationId = conversationId).latestMessageId,
        )
    }

    @Test
    fun smsFromBlockedSenderGoesToAnArchivedConversationAsSeen() {
        world.blockDestination(destination = SENDER_A)

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        assertTrue(world.conversation(conversationId = onlyConversationId()).archived)
        val message = onlyMessage()
        assertFalse(message.read)
        assertTrue(message.seen)
    }

    @Test
    fun smsFromBlockedSenderKeepsAnArchivedConversationArchived() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))
        world.archiveConversation(conversationId = conversationId)
        world.blockDestination(destination = SENDER_A)

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        assertTrue(world.conversation(conversationId = conversationId).archived)
    }

    @Test
    fun smsUnarchivesTheConversationOfAnUnblockedSender() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))
        world.archiveConversation(conversationId = conversationId)

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        assertFalse(world.conversation(conversationId = conversationId).archived)
    }

    @Test
    fun smsInTheConversationOnScreenIsReadAndSeen() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))
        world.focusConversation(conversationId = conversationId)

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        val message = onlyMessage()
        assertTrue(message.read)
        assertTrue(message.seen)
        assertEquals(1, telephonySmsInboxInserts().single().getAsInteger(Sms.Inbox.READ))
    }

    @Test
    fun smsInAnotherConversationThanTheOneOnScreenStaysUnread() {
        val focusedConversationId = world.createConversation(recipients = listOf(SENDER_B))
        world.focusConversation(conversationId = focusedConversationId)

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        val message = onlyMessage()
        assertFalse(message.read)
        assertFalse(message.seen)
    }

    @Test
    fun smsWhileTheConversationListShowsTheNewestIsSeenButUnread() {
        world.showConversationListAtNewest(scrolledToNewest = true)

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        val message = onlyMessage()
        assertFalse(message.read)
        assertTrue(message.seen)
    }

    @Test
    fun smsOnTheDefaultSimKeepsTheConversationOnTheDefaultSelf() {
        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_1), onlyMessage().selfId)
        assertEquals(
            world.selfIdFor(subId = ParticipantData.DEFAULT_SELF_SUB_ID),
            world.conversation(conversationId = onlyConversationId()).selfId,
        )
    }

    @Test
    fun smsOnTheSecondSimSwitchesTheConversationToThatSim() {
        val conversationId = world.createConversation(recipients = listOf(SENDER_A))

        driver.receiveSms(sender = SENDER_A, text = TEXT, subId = FakeTelephony.SUB_ID_SIM_2)

        val secondSimSelfId = world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_2)
        assertEquals(secondSimSelfId, onlyMessage().selfId)
        assertEquals(secondSimSelfId, world.conversation(conversationId = conversationId).selfId)
    }

    @Test
    fun smsOnAnInactiveSimLeavesTheConversationSelfAlone() {
        val conversationId = world.createConversation(
            recipients = listOf(SENDER_A),
            selfSubId = FakeTelephony.SUB_ID_SIM_1,
        )

        driver.receiveSms(
            sender = SENDER_A,
            text = TEXT,
            subId = OutgoingPipelineWorld.SUB_ID_INACTIVE_SIM,
        )

        assertEquals(
            world.selfIdFor(subId = OutgoingPipelineWorld.SUB_ID_INACTIVE_SIM),
            onlyMessage().selfId,
        )
        assertEquals(
            world.selfIdFor(subId = FakeTelephony.SUB_ID_SIM_1),
            world.conversation(conversationId = conversationId).selfId,
        )
    }

    @Test
    fun serviceCenterIsKeptOnlyWhenTheReplyPathIsPresent() {
        driver.receiveSms(
            sender = SENDER_A,
            text = TEXT,
            subId = FakeTelephony.SUB_ID_SIM_1,
            serviceCenter = SERVICE_CENTER,
        )
        val conversationId = onlyConversationId()
        assertNull(world.conversation(conversationId = conversationId).smsServiceCenter)

        driver.receiveSms(
            sender = SENDER_A,
            text = SECOND_TEXT,
            subId = FakeTelephony.SUB_ID_SIM_1,
            serviceCenter = SERVICE_CENTER,
            replyPathPresent = true,
        )

        assertEquals(
            SERVICE_CENTER,
            world.conversation(conversationId = conversationId).smsServiceCenter,
        )
    }

    @Test
    fun smsWithoutAnAddressComesFromTheUnknownSender() {
        driver.receiveSms(sender = "", text = TEXT, subId = FakeTelephony.SUB_ID_SIM_1)

        assertEquals(ParticipantData.getUnknownSenderDestination(), onlyMessage().senderDestination)
        assertEquals(
            listOf(ParticipantData.getUnknownSenderDestination()),
            world.conversation(conversationId = onlyConversationId()).participantDestinations,
        )
    }
}
