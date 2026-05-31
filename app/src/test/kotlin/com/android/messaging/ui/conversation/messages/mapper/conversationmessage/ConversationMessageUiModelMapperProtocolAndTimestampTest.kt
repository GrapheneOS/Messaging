package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Protocol
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageUiModelMapperProtocolAndTimestampTest :
    BaseConversationMessageUiModelMapperTest() {

    @Test
    fun map_smsMessage_mapsToSmsProtocol() {
        val uiModel = mapper.map(
            messageData(isSms = true, isMms = false, isMmsNotification = false),
        )

        assertEquals(Protocol.SMS, uiModel.protocol)
    }

    @Test
    fun map_mmsNotification_mapsToMmsPushNotificationProtocolBeforeMms() {
        val uiModel = mapper.map(
            messageData(isSms = false, isMmsNotification = true, isMms = true),
        )

        assertEquals(Protocol.MMS_PUSH_NOTIFICATION, uiModel.protocol)
    }

    @Test
    fun map_mmsMessage_mapsToMmsProtocol() {
        val uiModel = mapper.map(
            messageData(isSms = false, isMmsNotification = false, isMms = true),
        )

        assertEquals(Protocol.MMS, uiModel.protocol)
    }

    @Test
    fun map_nonTelephonyMessage_mapsToUnknownProtocol() {
        val uiModel = mapper.map(
            messageData(isSms = false, isMmsNotification = false, isMms = false),
        )

        assertEquals(Protocol.UNKNOWN, uiModel.protocol)
    }

    @Test
    fun map_incomingMessage_usesReceivedTimestampAsDisplayTimestamp() {
        val uiModel = mapper.map(
            messageData(isIncoming = true, receivedTimestamp = 500L, sentTimestamp = 100L),
        )

        assertEquals(500L, uiModel.displayTimestamp)
    }

    @Test
    fun map_incomingMessageWithoutReceivedTimestamp_fallsBackToSentTimestamp() {
        val uiModel = mapper.map(
            messageData(isIncoming = true, receivedTimestamp = 0L, sentTimestamp = 100L),
        )

        assertEquals(100L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessage_usesSentTimestampAsDisplayTimestamp() {
        val uiModel = mapper.map(
            messageData(isIncoming = false, sentTimestamp = 300L, receivedTimestamp = 900L),
        )

        assertEquals(300L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessageWithoutSentTimestamp_fallsBackToReceivedTimestamp() {
        val uiModel = mapper.map(
            messageData(isIncoming = false, sentTimestamp = 0L, receivedTimestamp = 900L),
        )

        assertEquals(900L, uiModel.displayTimestamp)
    }

    @Test
    fun map_outgoingMessageWithNegativeSentTimestamp_fallsBackToReceivedTimestamp() {
        val uiModel = mapper.map(
            messageData(isIncoming = false, sentTimestamp = -1L, receivedTimestamp = 900L),
        )

        assertEquals(900L, uiModel.displayTimestamp)
    }
}
