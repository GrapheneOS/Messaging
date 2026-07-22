package com.android.messaging.ui.conversation.messages.ui.message

import android.net.Uri
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationMessageContentBuilderTest {

    @Test
    fun attachmentOnlyAudioMessage_doesNotUseMimeTypeAsBodyText() {
        val message = createMessage(
            text = null,
            parts = persistentListOf(
                createAudioPart(),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertNull(content.bodyText)
        assertTrue(content.isAttachmentOnly)
        assertEquals(1, content.attachmentSections.trailingItems.size)

        val attachment = content.attachmentSections.trailingItems.single()
            as ConversationAttachmentItem.Inline

        val inlineAttachment = attachment.attachment as ConversationInlineAttachment.Audio
        assertEquals(AUDIO_CONTENT_URI, inlineAttachment.contentUri)
    }

    @Test
    fun attachmentOnlyVCardMessage_buildsInlineVCardAttachment_withoutBodyText() {
        val vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            titleText = "Sam Rivera",
            subtitleText = "sam@example.com",
        )
        val message = createMessage(
            text = null,
            parts = persistentListOf(
                createVCardPart(
                    vCardUiModel = vCardUiModel,
                ),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertNull(content.bodyText)
        assertTrue(content.isAttachmentOnly)

        val trailingAttachment = content.attachmentSections.trailingItems.single()

        val inlineAttachment = (trailingAttachment as ConversationAttachmentItem.Inline)
            .attachment as ConversationInlineAttachment.VCard
        assertEquals(V_CARD_CONTENT_URI, inlineAttachment.contentUri)
        assertEquals(ConversationVCardAttachmentType.CONTACT, inlineAttachment.type)
        assertEquals("Sam Rivera", inlineAttachment.titleText)
        assertEquals("sam@example.com", inlineAttachment.subtitleText)
    }

    @Test
    fun messageText_isUsedAsBodyText() {
        val message = createMessage(
            text = "  See you soon  ",
            parts = persistentListOf(),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertEquals("See you soon", content.bodyText)
        assertFalse(content.isAttachmentOnly)
    }

    @Test
    fun messageText_takesPrecedenceOverAttachmentCaption() {
        val message = createMessage(
            text = "Message body",
            parts = persistentListOf(
                createAudioPart(
                    text = "Attachment caption",
                ),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertEquals("Message body", content.bodyText)
        assertFalse(content.isAttachmentOnly)
    }

    @Test
    fun attachmentCaption_isUsedAsBodyText() {
        val message = createMessage(
            text = null,
            parts = persistentListOf(
                createAudioPart(
                    text = "Ambient room tone",
                ),
            ),
        )

        val content = buildConversationMessageContent(
            message = message,
            subjectText = null,
        )

        assertEquals("Ambient room tone", content.bodyText)
        assertFalse(content.isAttachmentOnly)
    }

    private fun createMessage(
        text: String?,
        parts: ImmutableList<ConversationMessagePartUiModel>,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = "message-1",
            conversationId = CONVERSATION_ID,
            text = text,
            parts = parts,
            sentTimestamp = 1L,
            receivedTimestamp = 1L,
            displayTimestamp = 1L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = 0L,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = true,
            canDownloadMessage = false,
            canForwardMessage = true,
            canResendMessage = false,
            canSaveAttachments = false,
            isYouTubePreviewEnabled = false,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.MMS,
        )
    }

    private fun createAudioPart(
        text: String? = null,
    ): ConversationMessagePartUiModel.Attachment.Audio {
        return ConversationMessagePartUiModel.Attachment.Audio(
            text = text,
            contentType = "audio/x-wav",
            contentUri = Uri.parse(AUDIO_CONTENT_URI),
            width = 0,
            height = 0,
        )
    }

    private fun createVCardPart(
        vCardUiModel: ConversationVCardAttachmentUiModel,
    ): ConversationMessagePartUiModel.Attachment.VCard {
        return ConversationMessagePartUiModel.Attachment.VCard(
            text = null,
            contentType = "text/x-vCard",
            contentUri = Uri.parse(V_CARD_CONTENT_URI),
            width = 0,
            height = 0,
            vCardUiModel = vCardUiModel,
        )
    }

    private companion object {
        private const val AUDIO_CONTENT_URI = "content://mms/part/audio-1"
        private const val V_CARD_CONTENT_URI = "content://mms/part/vcard-1"
    }
}
