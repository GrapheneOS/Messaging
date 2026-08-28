package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageUiModelMapperProtocolAndTimestampTest :
    BaseConversationMessageUiModelMapperTest() {

    @Test
    fun map_smsMessage_mapsToSmsProtocol() {
        val uiModel = mapPresent(
            messageData(isSms = true, isMms = false, isMmsNotification = false),
        )

        assertEquals(Protocol.SMS, uiModel.protocol)
    }

    @Test
    fun map_mmsNotification_mapsToMmsPushNotificationProtocolBeforeMms() {
        val uiModel = mapPresent(
            messageData(isSms = false, isMmsNotification = true, isMms = true),
        )

        assertEquals(Protocol.MMS_PUSH_NOTIFICATION, uiModel.protocol)
    }

    @Test
    fun map_mmsMessage_mapsToMmsProtocol() {
        val uiModel = mapPresent(
            messageData(isSms = false, isMmsNotification = false, isMms = true),
        )

        assertEquals(Protocol.MMS, uiModel.protocol)
    }

    @Test
    fun map_nonTelephonyMessage_mapsToUnknownProtocol() {
        val uiModel = mapPresent(
            messageData(isSms = false, isMmsNotification = false, isMms = false),
        )

        assertEquals(Protocol.UNKNOWN, uiModel.protocol)
    }

    @Test
    fun map_incomingMessage_usesReceivedTimestampAsDisplayTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = true, receivedTimestamp = 500L, sentTimestamp = 100L),
        )

        assertEquals(500L, uiModel.displayTimestamp)
    }

    @Test
    fun map_incomingMessageWithoutReceivedTimestamp_fallsBackToSentTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = true, receivedTimestamp = 0L, sentTimestamp = 100L),
        )

        assertEquals(100L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessage_usesReceivedTimestampAsDisplayTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = false, sentTimestamp = 300L, receivedTimestamp = 900L),
        )

        assertEquals(900L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessageWithoutReceivedTimestamp_fallsBackToSentTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = false, sentTimestamp = 300L, receivedTimestamp = 0L),
        )

        assertEquals(300L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessageWithNegativeReceivedTimestamp_fallsBackToSentTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = false, sentTimestamp = 300L, receivedTimestamp = -1L),
        )

        assertEquals(300L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessageWithoutSentTimestamp_stillUsesReceivedTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = false, sentTimestamp = 0L, receivedTimestamp = 900L),
        )

        assertEquals(900L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessageSentAfterItWasReceived_stillLabelsWithReceivedTimestamp() {
        val uiModel = mapPresent(
            messageData(isIncoming = false, sentTimestamp = 9_000L, receivedTimestamp = 5_000L),
        )

        assertEquals(5_000L, uiModel.displayTimestamp)
    }

    @Test
    fun map_retriedOutgoingMessage_labelsWithTheTimestampTheThreadIsSortedBy() {
        val earlier = mapPresent(
            messageData(
                isIncoming = false,
                receivedTimestamp = 1_000L,
                sentTimestamp = 1_000L,
            ),
        )
        val retried = mapPresent(
            messageData(
                isIncoming = false,
                receivedTimestamp = 5_000L,
                sentTimestamp = 900L,
            ),
        )

        assertEquals(5_000L, retried.displayTimestamp)
        assertTrue(
            "a message sorted below another must never be labelled earlier than it",
            retried.displayTimestamp > earlier.displayTimestamp,
        )
    }
}
