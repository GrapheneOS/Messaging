package com.android.messaging.data.conversation.mapper

import android.net.Uri
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.assertThat
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
            CONVERSATION_ID.value,
            "self-1",
            "Hello",
        )
        messageData.mmsSubject = "Subject"
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "Caption",
                "image/jpeg",
                scratchUri("1.jpg"),
                640,
                480,
            ),
        )

        val draft = mapper.map(
            messageData = messageData,
            fallbackSelfParticipantId = ParticipantId("fallback-self"),
        )

        assertEquals("Hello", draft.messageText)
        assertEquals("Subject", draft.subjectText)
        assertThat(draft.selfParticipantId).isEqualTo(ParticipantId("self-1"))
        assertEquals(
            listOf(
                createAttachment(
                    contentType = "image/jpeg",
                    contentUri = scratchUri("1.jpg").toString(),
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
            CONVERSATION_ID.value,
            null,
            null,
        )

        val draft = mapper.map(
            messageData = messageData,
            fallbackSelfParticipantId = ParticipantId("fallback-self"),
        )

        assertThat(draft.selfParticipantId).isEqualTo(ParticipantId("fallback-self"))
    }

    @Test
    fun map_usesFallbackSelfParticipantIdWhenSourceSelfIdIsBlank() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID.value,
            "",
            "Hello",
        )

        val draft = mapper.map(
            messageData = messageData,
            fallbackSelfParticipantId = ParticipantId("fallback-self"),
        )

        assertThat(draft.selfParticipantId).isEqualTo(ParticipantId("fallback-self"))
    }

    @Test
    fun map_normalizesUnspecifiedAttachmentDimensionsToNull() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID.value,
            "self-1",
            "",
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "image/png",
                scratchUri("2.png"),
                MessagePartData.UNSPECIFIED_SIZE,
                MessagePartData.UNSPECIFIED_SIZE,
            ),
        )

        val draft = mapper.map(messageData = messageData)
        val attachment = draft.attachments.single()

        assertEquals("image/png", attachment.contentType)
        assertEquals(scratchUri("2.png").toString(), attachment.contentUri)
        assertEquals("", attachment.captionText)
        assertNull(attachment.width)
        assertNull(attachment.height)
    }

    @Test
    fun map_dropsAttachmentsWithBlankContentTypeOrUri() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID.value,
            "self-1",
            "Hello",
        )
        messageData.addPart(
            MessagePartData.createMediaMessagePart(
                "",
                scratchUri("3.jpg"),
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
                scratchUri("4.mp3"),
                0,
                0,
            ),
        )

        val draft = mapper.map(messageData = messageData)

        assertEquals(
            listOf(
                createAttachment(
                    contentType = "audio/mp3",
                    contentUri = scratchUri("4.mp3").toString(),
                    width = 0,
                    height = 0,
                ),
            ),
            draft.attachments,
        )
    }

    @Test
    fun map_dropsAttachmentsBackedByMediaStoreUris() {
        val messageData = MessageData.createDraftSmsMessage(
            CONVERSATION_ID.value,
            "self-1",
            "Hello",
        )
        listOf(
            "content://media/picker/0/com.android.providers.media.photopicker/media/1",
            "content://media/external/images/media/1",
            "content://com.android.providers.media.documents/document/image%3A1",
            // An authority that merely starts with "media" belongs to somebody else.
            "content://media.example/images/1",
            // Forwarding seeds a raw mms part uri, readable as the default SMS app.
            "content://mms/part/1",
        ).forEach { contentUri ->
            messageData.addPart(
                MessagePartData.createMediaMessagePart(
                    "image/jpeg",
                    Uri.parse(contentUri),
                    320,
                    240,
                ),
            )
        }

        val draft = mapper.map(messageData = messageData)

        assertEquals(
            listOf(
                createAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://media.example/images/1",
                    width = 320,
                    height = 240,
                ),
                createAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://mms/part/1",
                    width = 320,
                    height = 240,
                ),
            ),
            draft.attachments,
        )
    }

    private fun scratchUri(name: String): Uri {
        return MediaScratchFileProvider.getUriBuilder().appendPath(name).build()
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
