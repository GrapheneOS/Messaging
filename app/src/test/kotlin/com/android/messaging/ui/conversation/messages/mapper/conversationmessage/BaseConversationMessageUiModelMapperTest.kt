package com.android.messaging.ui.conversation.messages.mapper.conversationmessage

import android.net.Uri
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapper
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapperImpl
import com.android.messaging.util.OsUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before

internal abstract class BaseConversationMessageUiModelMapperTest {

    @Before
    fun mockOsUtil() {
        mockkStatic(OsUtil::class)
        every { OsUtil.isSecondaryUser() } returns false
    }

    @After
    fun unmockOsUtil() {
        unmockkStatic(OsUtil::class)
    }

    protected val vCardUiModel = ConversationVCardAttachmentUiModel(
        type = ConversationVCardAttachmentType.CONTACT,
        titleText = "Sam Rivera",
    )

    protected val conversationVCardAttachmentUiModelMapper =
        mockk<ConversationVCardAttachmentUiModelMapper> {
            every { map(metadata = null) } returns vCardUiModel
        }

    protected val mapper = ConversationMessageUiModelMapperImpl(
        conversationVCardAttachmentUiModelMapper = conversationVCardAttachmentUiModelMapper,
    )

    protected fun messageData(
        messageId: String? = "message-1",
        conversationId: String? = "conversation-1",
        text: String? = null,
        parts: List<MessagePartData>? = emptyList(),
        sentTimestamp: Long = 0L,
        receivedTimestamp: Long = 0L,
        isIncoming: Boolean = false,
        status: Int = MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
        senderDisplayName: String? = null,
        senderAvatarUri: Uri? = null,
        senderContactId: Long = 0L,
        senderContactLookupKey: String? = null,
        senderNormalizedDestination: String? = null,
        senderParticipantId: String? = null,
        selfParticipantId: String? = null,
        canClusterWithPrevious: Boolean = false,
        canClusterWithNext: Boolean = false,
        canCopyMessageToClipboard: Boolean = false,
        showDownloadMessage: Boolean = false,
        canForwardMessage: Boolean = false,
        showResendMessage: Boolean = false,
        smsMessageSize: Int = 0,
        mmsExpiry: Long = 0L,
        mmsSubject: String? = null,
        isSms: Boolean = false,
        isMmsNotification: Boolean = false,
        isMms: Boolean = false,
    ): ConversationMessageData {
        val mock = mockk<ConversationMessageData>()
        every { mock.messageId } returns messageId
        every { mock.conversationId } returns conversationId
        every { mock.text } returns text
        every { mock.parts } returns parts
        every { mock.sentTimeStamp } returns sentTimestamp
        every { mock.receivedTimeStamp } returns receivedTimestamp
        every { mock.isIncoming } returns isIncoming
        every { mock.status } returns status
        every { mock.senderDisplayName } returns senderDisplayName
        every { mock.senderProfilePhotoUri } returns senderAvatarUri
        every { mock.senderContactId } returns senderContactId
        every { mock.senderContactLookupKey } returns senderContactLookupKey
        every { mock.senderNormalizedDestination } returns senderNormalizedDestination
        every { mock.participantId } returns senderParticipantId
        every { mock.selfParticipantId } returns selfParticipantId
        every { mock.canClusterWithPreviousMessage } returns canClusterWithPrevious
        every { mock.canClusterWithNextMessage } returns canClusterWithNext
        every { mock.canCopyMessageToClipboard } returns canCopyMessageToClipboard
        every { mock.showDownloadMessage } returns showDownloadMessage
        every { mock.canForwardMessage } returns canForwardMessage
        every { mock.showResendMessage } returns showResendMessage
        every { mock.smsMessageSize } returns smsMessageSize
        every { mock.mmsExpiry } returns mmsExpiry
        every { mock.mmsSubject } returns mmsSubject
        every { mock.isSms } returns isSms
        every { mock.isMmsNotification } returns isMmsNotification
        every { mock.isMms } returns isMms

        return mock
    }

    protected fun messagePart(
        contentType: String? = "text/plain",
        text: String? = null,
        contentUri: Uri? = null,
        partId: String? = null,
        width: Int = 0,
        height: Int = 0,
    ): MessagePartData {
        val mock = mockk<MessagePartData>()
        every { mock.contentType } returns contentType
        every { mock.text } returns text
        every { mock.contentUri } returns contentUri
        every { mock.partId } returns partId
        every { mock.width } returns width
        every { mock.height } returns height
        return mock
    }
}
