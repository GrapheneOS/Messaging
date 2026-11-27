package com.android.messaging.datamodel.action

import com.android.messaging.datamodel.data.ParticipantData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationLookupTest : IncomingPipelineTestBase() {

    @Test
    fun newRecipientGetsAConversationOnTheDefaultSelf() {
        val conversationId = driver.conversationForRecipient(
            threadId = threadIdFor(SENDER_A),
            recipient = SENDER_A,
            senderBlocked = false,
            subId = FakeTelephony.SUB_ID_SIM_2,
        )

        val conversation = world.conversation(conversationId = conversationId)
        assertEquals(threadIdFor(SENDER_A), conversation.threadId)
        assertEquals(listOf(SENDER_A), conversation.participantDestinations)
        assertEquals(
            world.selfIdFor(subId = ParticipantData.DEFAULT_SELF_SUB_ID),
            conversation.selfId,
        )
        assertFalse(conversation.archived)
    }

    @Test
    fun blockedRecipientGetsAnArchivedConversation() {
        val conversationId = driver.conversationForRecipient(
            threadId = threadIdFor(SENDER_A),
            recipient = SENDER_A,
            senderBlocked = true,
            subId = FakeTelephony.SUB_ID_SIM_1,
        )

        assertTrue(world.conversation(conversationId = conversationId).archived)
    }

    @Test
    fun existingThreadIsReusedWithoutChangingItsArchiveState() {
        val existingId = world.createConversation(recipients = listOf(SENDER_A))

        val conversationId = driver.conversationForRecipient(
            threadId = threadIdFor(SENDER_A),
            recipient = SENDER_A,
            senderBlocked = true,
            subId = FakeTelephony.SUB_ID_SIM_1,
        )

        assertEquals(existingId, conversationId)
        assertEquals(listOf(existingId), world.conversationIds())
        assertFalse(world.conversation(conversationId = conversationId).archived)
    }

    @Test
    fun conversationsAreKeyedByThreadNotByParticipant() {
        val existingId = world.createConversation(recipients = listOf(SENDER_A))

        val conversationId = driver.conversationForRecipient(
            threadId = threadIdFor(SENDER_A, SENDER_B),
            recipient = SENDER_A,
            senderBlocked = false,
            subId = FakeTelephony.SUB_ID_SIM_1,
        )

        assertTrue(conversationId != existingId)
        assertEquals(
            listOf(SENDER_A),
            world.conversation(conversationId = conversationId).participantDestinations,
        )
    }

    @Test
    fun threadLookupTakesTheParticipantsFromTheTelephonyThread() {
        val threadId = threadIdFor(SENDER_C, SENDER_A, SENDER_B)

        val conversationId = driver.conversationForThread(
            threadId = threadId,
            senderBlocked = false,
            subId = FakeTelephony.SUB_ID_SIM_1,
        )

        val conversation = world.conversation(conversationId = conversationId)
        assertEquals(threadId, conversation.threadId)
        assertEquals(listOf(SENDER_A, SENDER_B, SENDER_C), conversation.participantDestinations)
        assertEquals(
            conversationId,
            driver.conversationForThread(
                threadId = threadId,
                senderBlocked = false,
                subId = FakeTelephony.SUB_ID_SIM_1,
            ),
        )
    }

    @Test
    fun threadLookupArchivesANewConversationFromABlockedSender() {
        val conversationId = driver.conversationForThread(
            threadId = threadIdFor(SENDER_A),
            senderBlocked = true,
            subId = FakeTelephony.SUB_ID_SIM_1,
        )

        assertTrue(world.conversation(conversationId = conversationId).archived)
    }
}
