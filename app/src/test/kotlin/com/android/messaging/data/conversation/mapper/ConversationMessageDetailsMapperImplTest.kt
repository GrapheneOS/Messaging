package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsData
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.mmslib.pdu.PduHeaders
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationMessageDetailsMapperImplTest {

    private val mapper = ConversationMessageDetailsMapperImpl()

    @Test
    fun map_incomingSms_mapsTypeSenderAndTimestamps() {
        val message = message(
            isSms = true,
            isIncoming = true,
            senderNormalizedDestination = "+15550100",
            sentTimeStamp = 1_000L,
            receivedTimeStamp = 2_000L,
        )

        val result = mapper.map(
            data = detailsData(message),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(ConversationMessageDetails.Type.SMS, result.type)
        assertEquals("+15550100", result.sender)
        assertEquals(1_000L, result.sentTimestamp)
        assertEquals(2_000L, result.receivedTimestamp)
        assertNull(result.subject)
        assertNull(result.priority)
        assertNull(result.sizeBytes)
    }

    @Test
    fun map_incomingSms_withInvalidSentTimestamp_omitsSentTimestamp() {
        val message = message(
            isSms = true,
            isIncoming = true,
            sentTimeStamp = 0L,
            receivedTimeStamp = 2_000L,
        )

        val result = mapper.map(
            data = detailsData(message),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertNull(result.sentTimestamp)
        assertEquals(2_000L, result.receivedTimestamp)
    }

    @Test
    fun map_outgoingCompleteSms_usesReceivedTimestampAsSent() {
        val message = message(
            isSms = true,
            isIncoming = false,
            isSendComplete = true,
            receivedTimeStamp = 3_000L,
        )

        val result = mapper.map(
            data = detailsData(message),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(3_000L, result.sentTimestamp)
        assertNull(result.receivedTimestamp)
    }

    @Test
    fun map_outgoingNotComplete_hasNoTimestamps() {
        val message = message(
            isSms = true,
            isIncoming = false,
            isSendComplete = false,
        )

        val result = mapper.map(
            data = detailsData(message),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertNull(result.sentTimestamp)
        assertNull(result.receivedTimestamp)
    }

    @Test
    fun map_mms_mapsSubjectPriorityAndSize() {
        val message = message(
            isSms = false,
            smsPriority = PduHeaders.PRIORITY_HIGH,
            smsMessageSize = 2_048,
        )

        val result = mapper.map(
            data = detailsData(message),
            activeSubscriptionCount = 1,
            cleansedSubject = "Trip photos",
            debug = null,
        )

        assertEquals(ConversationMessageDetails.Type.MMS, result.type)
        assertEquals("Trip photos", result.subject)
        assertEquals(ConversationMessageDetails.Priority.HIGH, result.priority)
        assertEquals(2_048L, result.sizeBytes)
    }

    @Test
    fun map_mms_withZeroSize_omitsSize() {
        val message = message(
            isSms = false,
            smsMessageSize = 0,
        )

        val result = mapper.map(
            data = detailsData(message),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertNull(result.sizeBytes)
    }

    @Test
    fun map_mms_mapsLowAndNormalPriority() {
        val lowMessage = message(
            isSms = false,
            smsPriority = PduHeaders.PRIORITY_LOW,
        )
        val normalMessage = message(
            isSms = false,
            smsPriority = PduHeaders.PRIORITY_NORMAL,
        )

        val lowResult = mapper.map(
            data = detailsData(lowMessage),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )
        val normalResult = mapper.map(
            data = detailsData(normalMessage),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(ConversationMessageDetails.Priority.LOW, lowResult.priority)
        assertEquals(ConversationMessageDetails.Priority.NORMAL, normalResult.priority)
    }

    @Test
    fun map_excludesSenderAndIrrelevantSelfFromRecipients() {
        val message = message(
            isSms = true,
            isIncoming = false,
            participantId = "sender",
            selfParticipantId = "self",
        )
        val participants = participantsOf(
            participant(
                id = "sender",
                normalizedDestination = "+10000000000",
            ),
            participant(
                id = "recipient",
                normalizedDestination = "+19999999999",
            ),
            participant(
                id = "self",
                isSelf = true,
                normalizedDestination = "+15555555555",
            ),
        )

        val result = mapper.map(
            data = detailsData(
                message = message,
                participants = participants,
            ),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(listOf("+19999999999"), result.recipients)
    }

    @Test
    fun map_dropsBlankRecipientDestinations() {
        val message = message(
            isSms = true,
            participantId = "sender",
        )
        val participants = participantsOf(
            participant(
                id = "recipient",
                normalizedDestination = " ",
            ),
        )

        val result = mapper.map(
            data = detailsData(
                message = message,
                participants = participants,
            ),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(emptyList<String>(), result.recipients)
    }

    @Test
    fun map_singleSubscription_hasNoSubscriptionLabel() {
        val result = mapper.map(
            data = detailsData(
                message = message(isSms = true),
                selfParticipant = participant(
                    id = "self",
                    isSelf = true,
                ),
            ),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = null,
        )

        assertNull(result.subscriptionLabel)
    }

    @Test
    fun map_multiSim_withSubscriptionName_mapsNamedLabel() {
        val self = participant(
            id = "self",
            isSelf = true,
            isActiveSubscription = true,
            isDefaultSelf = false,
            subscriptionName = "Work",
        )

        val result = mapper.map(
            data = detailsData(
                message = message(isSms = true),
                selfParticipant = self,
            ),
            activeSubscriptionCount = 2,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(ConversationSubscriptionLabel.Named(name = "Work"), result.subscriptionLabel)
    }

    @Test
    fun map_multiSim_withBlankSubscriptionName_mapsSlotLabel() {
        val self = participant(
            id = "self",
            isSelf = true,
            isActiveSubscription = true,
            isDefaultSelf = false,
            subscriptionName = null,
            displaySlotId = 2,
        )

        val result = mapper.map(
            data = detailsData(
                message = message(isSms = true),
                selfParticipant = self,
            ),
            activeSubscriptionCount = 2,
            cleansedSubject = null,
            debug = null,
        )

        assertEquals(ConversationSubscriptionLabel.Slot(slotId = 2), result.subscriptionLabel)
    }

    @Test
    fun map_multiSim_defaultSelf_hasNoSubscriptionLabel() {
        val self = participant(
            id = "self",
            isSelf = true,
            isActiveSubscription = true,
            isDefaultSelf = true,
            subscriptionName = "Work",
        )

        val result = mapper.map(
            data = detailsData(
                message = message(isSms = true),
                selfParticipant = self,
            ),
            activeSubscriptionCount = 2,
            cleansedSubject = null,
            debug = null,
        )

        assertNull(result.subscriptionLabel)
    }

    @Test
    fun map_passesThroughDebugModel() {
        val debug = ConversationMessageDetails.Debug(
            messageId = "id",
            telephonyUri = null,
            conversationId = null,
            conversationTelephonyThreadId = null,
            telephonyThreadId = null,
            contentLocationUrl = null,
            threadRecipientIds = null,
            threadRecipients = null,
            sender = null,
        )

        val result = mapper.map(
            data = detailsData(message(isSms = true)),
            activeSubscriptionCount = 1,
            cleansedSubject = null,
            debug = debug,
        )

        assertEquals(debug, result.debug)
    }

    private fun detailsData(
        message: ConversationMessageData,
        participants: ConversationParticipantsData = participantsOf(),
        selfParticipant: ParticipantData? = null,
    ): ConversationMessageDetailsData {
        return ConversationMessageDetailsData(
            message = message,
            participants = participants,
            selfParticipant = selfParticipant,
        )
    }

    private fun message(
        isSms: Boolean = true,
        isIncoming: Boolean = false,
        isSendComplete: Boolean = false,
        senderNormalizedDestination: String? = null,
        participantId: String? = null,
        selfParticipantId: String? = null,
        sentTimeStamp: Long = 0L,
        receivedTimeStamp: Long = 0L,
        smsPriority: Int = PduHeaders.PRIORITY_NORMAL,
        smsMessageSize: Int = 0,
    ): ConversationMessageData {
        return mockk(relaxed = true) {
            every { this@mockk.isSms } returns isSms
            every { this@mockk.isIncoming } returns isIncoming
            every { this@mockk.isSendComplete } returns isSendComplete
            every { this@mockk.senderNormalizedDestination } returns senderNormalizedDestination
            every { this@mockk.participantId } returns participantId
            every { this@mockk.selfParticipantId } returns selfParticipantId
            every { this@mockk.sentTimeStamp } returns sentTimeStamp
            every { this@mockk.receivedTimeStamp } returns receivedTimeStamp
            every { this@mockk.smsPriority } returns smsPriority
            every { this@mockk.smsMessageSize } returns smsMessageSize
        }
    }

    private fun participant(
        id: String,
        isSelf: Boolean = false,
        normalizedDestination: String? = null,
        isActiveSubscription: Boolean = false,
        isDefaultSelf: Boolean = false,
        subscriptionName: String? = null,
        displaySlotId: Int = 0,
    ): ParticipantData {
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.isSelf } returns isSelf
            every { this@mockk.normalizedDestination } returns normalizedDestination
            every { this@mockk.isActiveSubscription } returns isActiveSubscription
            every { this@mockk.isDefaultSelf } returns isDefaultSelf
            every { this@mockk.subscriptionName } returns subscriptionName
            every { this@mockk.displaySlotId } returns displaySlotId
        }
    }

    private fun participantsOf(
        vararg participants: ParticipantData,
    ): ConversationParticipantsData {
        return mockk {
            every { iterator() } answers { participants.toMutableList().iterator() }
        }
    }
}
