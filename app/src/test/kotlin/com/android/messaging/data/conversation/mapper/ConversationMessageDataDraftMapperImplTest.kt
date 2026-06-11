package com.android.messaging.data.conversation.mapper

import android.net.Uri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationMessageDataDraftMapperImplTest {

    private val mapper = ConversationMessageDataDraftMapperImpl()

    @Test
    fun map_preservesSourceFieldsWhenSelfParticipantIdIsPresent() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID,
            "self-1",
            "Hello",
        )
        messageData.mmsSubject = "Subject"
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "Caption",
                "image/jpeg",
                Uri.parse("content://media/image/1"),
                640,
                480,
            ),
        )

        val draft = mapper.map(
            messageData = messageData,
            fallbackSelfParticipantId = "fallback-self",
        )

        assertEquals("Hello", draft.messageText)
        assertEquals("Subject", draft.subjectText)
        assertEquals("self-1", draft.selfParticipantId)
        assertEquals(
            listOf(
                createAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://media/image/1",
                    captionText = "Caption",
                    width = 640,
                    height = 480,
                ),
            ),
            draft.attachments,
        )
    }

    @Test
    fun map_usesFallbackSelfParticipantIdWhenSourceSelfIdIsNull() {
        val messageData = MessageData.createDraftMessage(
            CONVERSATION_ID,
            null,
            null,
        )

        val draft = mapper.map(
            messageData = messageData,
            fallbackSelfParticipantId = "fallback-self",
        )

        assertEquals("fallback-self", draft.selfParticipantId)
    }

    @Test
    fun map_usesFallbackSelfParticipantIdWhenSourceSelfIdIsBlank() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID,
            "",
            "Hello",
        )

        val draft = mapper.map(
            messageData = messageData,
            fallbackSelfParticipantId = "fallback-self",
        )

        assertEquals("fallback-self", draft.selfParticipantId)
    }

    @Test
    fun map_normalizesUnspecifiedAttachmentDimensionsToNull() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID,
            "self-1",
            "",
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "image/png",
                Uri.parse("content://media/image/2"),
                MessagePartData.UNSPECIFIED_SIZE,
                MessagePartData.UNSPECIFIED_SIZE,
            ),
        )

        val draft = mapper.map(messageData = messageData)
        val attachment = draft.attachments.single()

        assertEquals("image/png", attachment.contentType)
        assertEquals("content://media/image/2", attachment.contentUri)
        assertEquals("", attachment.captionText)
        assertNull(attachment.width)
        assertNull(attachment.height)
    }

    @Test
    fun map_dropsAttachmentsWithBlankContentTypeOrUri() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID,
            "self-1",
            "Hello",
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "",
                Uri.parse("content://media/image/3"),
                320,
                240,
            ),
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "image/jpeg",
                Uri.EMPTY,
                800,
                600,
            ),
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "audio/mp3",
                Uri.parse("content://media/audio/4"),
                0,
                0,
            ),
        )

        val draft = mapper.map(messageData = messageData)

        assertEquals(
            listOf(
                createAttachment(
                    contentType = "audio/mp3",
                    contentUri = "content://media/audio/4",
                    width = 0,
                    height = 0,
                ),
            ),
            draft.attachments,
        )
    }

    @Test
    fun map_dropsAttachmentsBackedByPhotoPickerUris() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID,
            "self-1",
            "Hello",
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "image/jpeg",
                Uri.parse(
                    "content://media/picker/0/" +
                        "com.android.providers.media.photopicker/media/1",
                ),
                320,
                240,
            ),
        )

        val draft = mapper.map(messageData = messageData)

        assertEquals(emptyList<ConversationDraftAttachment>(), draft.attachments)
    }

    private fun createAttachment(
        contentType: String,
        contentUri: String,
        captionText: String = "",
        width: Int? = null,
        height: Int? = null,
    ): ConversationDraftAttachment {
        return ConversationDraftAttachment(
            contentType = contentType,
            contentUri = contentUri,
            captionText = captionText,
            width = width,
            height = height,
        )
    }
}
