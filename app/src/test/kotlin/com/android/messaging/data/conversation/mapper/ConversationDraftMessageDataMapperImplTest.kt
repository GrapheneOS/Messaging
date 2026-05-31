package com.android.messaging.data.conversation.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationDraftMessageDataMapperImplTest {

    private val mapper = ConversationDraftMessageDataMapperImpl()

    @Test
    fun map_createsDraftSmsMessageForPlainTextDraft() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "self-1",
            ),
        )
        val parts = message.parts.toList()

        assertEquals(CONVERSATION_ID, message.conversationId)
        assertEquals("self-1", message.selfId)
        assertEquals("self-1", message.participantId)
        assertEquals(MessageData.PROTOCOL_SMS, message.protocol)
        assertEquals("Hello", message.messageText)
        assertEquals(1, parts.size)
        assertTrue(parts.single().isText)
    }

    @Test
    fun map_createsDraftMmsMessageForSubjectOnlyDraft() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                subjectText = "Subject",
                selfParticipantId = "self-1",
            ),
        )
        val parts = message.parts.toList()

        assertEquals(MessageData.PROTOCOL_MMS, message.protocol)
        assertEquals("Subject", message.mmsSubject)
        assertEquals("", message.messageText)
        assertTrue(parts.isEmpty())
    }

    @Test
    fun map_keepsSelfAndParticipantUnsetWhenSelfParticipantIdIsBlank() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "",
            ),
        )

        assertNull(message.selfId)
        assertNull(message.participantId)
    }

    @Test
    fun map_createsDraftMmsMessageWhenForced() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                messageText = "Hello",
                selfParticipantId = "self-1",
            ),
            forceMms = true,
        )

        assertEquals(MessageData.PROTOCOL_MMS, message.protocol)
        assertEquals("Hello", message.messageText)
        assertTrue(message.parts.toList().single().isText)
    }

    @Test
    fun map_createsMediaPartForValidAttachment() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                attachments = persistentListOf(
                    ConversationDraftAttachment(
                        contentType = "image/jpeg",
                        contentUri = IMAGE_CONTENT_URI,
                        width = 640,
                        height = 480,
                    ),
                ),
            ),
        )

        val attachmentPart = message.parts.toList().single()

        assertEquals(MessageData.PROTOCOL_MMS, message.protocol)
        assertTrue(attachmentPart.isImage)
        assertEquals("image/jpeg", attachmentPart.contentType)
        assertEquals(IMAGE_CONTENT_URI, attachmentPart.contentUri.toString())
        assertEquals(640, attachmentPart.width)
        assertEquals(480, attachmentPart.height)
        assertNull(attachmentPart.text)
    }

    @Test
    fun map_createsCaptionedMediaPartWithUnspecifiedDimensions() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                attachments = persistentListOf(
                    ConversationDraftAttachment(
                        contentType = "image/jpeg",
                        contentUri = IMAGE_CONTENT_URI,
                        captionText = "Caption",
                    ),
                ),
            ),
        )

        val attachmentPart = message.parts.toList().single()

        assertEquals("Caption", attachmentPart.text)
        assertEquals(MessagePartData.UNSPECIFIED_SIZE, attachmentPart.width)
        assertEquals(MessagePartData.UNSPECIFIED_SIZE, attachmentPart.height)
    }

    @Test
    fun map_dropsAttachmentsWithBlankRequiredFields() {
        val message = mapper.map(
            conversationId = CONVERSATION_ID,
            draft = ConversationDraft(
                messageText = "Hello",
                attachments = persistentListOf(
                    ConversationDraftAttachment(
                        contentType = "",
                        contentUri = IMAGE_CONTENT_URI,
                    ),
                    ConversationDraftAttachment(
                        contentType = "image/jpeg",
                        contentUri = "",
                    ),
                ),
            ),
        )

        val parts = message.parts.toList()

        assertEquals(MessageData.PROTOCOL_SMS, message.protocol)
        assertEquals(1, parts.size)
        assertTrue(parts.single().isText)
    }

    private companion object {
        private const val IMAGE_CONTENT_URI = "content://mms/part/image-1"
    }
}
