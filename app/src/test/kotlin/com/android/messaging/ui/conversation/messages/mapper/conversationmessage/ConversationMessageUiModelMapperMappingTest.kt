package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import android.net.Uri
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Protocol
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageUiModelMapperMappingTest :
    BaseConversationMessageUiModelMapperTest() {

    @Test
    fun map_mapsEveryScalarFieldOntoUiModel() {
        val avatarUri = Uri.parse("content://avatar/7")

        val uiModel = mapper.map(
            messageData(
                messageId = "message-7",
                conversationId = "conversation-3",
                text = "Hello there",
                parts = emptyList(),
                sentTimestamp = 1_000L,
                receivedTimestamp = 2_000L,
                isIncoming = false,
                status = MessageData.BUGLE_STATUS_OUTGOING_DELIVERED,
                senderDisplayName = "Ada Lovelace",
                senderAvatarUri = avatarUri,
                senderContactId = 42L,
                senderContactLookupKey = "lookup-7",
                senderNormalizedDestination = "+15550100",
                senderParticipantId = "participant-7",
                selfParticipantId = "self-7",
                canClusterWithPrevious = true,
                canClusterWithNext = false,
                canCopyMessageToClipboard = true,
                showDownloadMessage = false,
                canForwardMessage = true,
                showResendMessage = false,
                mmsSubject = "Subject line",
                isSms = true,
            ),
        )

        assertEquals(
            ConversationMessageUiModel(
                messageId = "message-7",
                conversationId = "conversation-3",
                text = "Hello there",
                parts = persistentListOf(),
                sentTimestamp = 1_000L,
                receivedTimestamp = 2_000L,
                displayTimestamp = 1_000L,
                status = Status.Outgoing.Delivered,
                isIncoming = false,
                senderDisplayName = "Ada Lovelace",
                senderAvatarUri = avatarUri,
                senderContactId = 42L,
                senderContactLookupKey = "lookup-7",
                senderNormalizedDestination = "+15550100",
                senderParticipantId = "participant-7",
                selfParticipantId = "self-7",
                canClusterWithPrevious = true,
                canClusterWithNext = false,
                canCopyMessageToClipboard = true,
                canDownloadMessage = false,
                canForwardMessage = true,
                canResendMessage = false,
                canSaveAttachments = false,
                mmsDownload = null,
                mmsSubject = "Subject line",
                protocol = Protocol.SMS,
            ),
            uiModel,
        )
    }

    @Test
    fun map_withNullMessageIdAndConversationId_substitutesEmptyStrings() {
        val uiModel = mapper.map(
            messageData(messageId = null, conversationId = null),
        )

        assertEquals("", uiModel.messageId)
        assertEquals("", uiModel.conversationId)
    }

    @Test
    fun map_withNullParts_returnsEmptyPartsList() {
        val uiModel = mapper.map(messageData(parts = null))

        assertEquals(persistentListOf<ConversationMessagePartUiModel>(), uiModel.parts)
    }

    @Test
    fun map_withBlankSenderIdentifiers_mapsThemToNull() {
        val uiModel = mapper.map(
            messageData(
                senderNormalizedDestination = "   ",
                senderParticipantId = "",
                selfParticipantId = " ",
            ),
        )

        assertNull(uiModel.senderNormalizedDestination)
        assertNull(uiModel.senderParticipantId)
        assertNull(uiModel.selfParticipantId)
    }
}
