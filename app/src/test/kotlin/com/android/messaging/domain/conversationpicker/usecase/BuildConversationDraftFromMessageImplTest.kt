package com.android.messaging.domain.conversationpicker.usecase

import android.net.Uri
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.MessagePartData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BuildConversationDraftFromMessageImplTest {

    private val buildConversationDraftFromMessage = BuildConversationDraftFromMessageImpl()

    @Test
    fun invoke_textSubjectAndMediaPart_mapsAllFields() {
        val message = messageData(
            text = "Forwarded body",
            subject = "Forwarded subject",
            parts = listOf(
                mediaPart(
                    contentType = "image/jpeg",
                    uri = "content://media/1",
                    caption = "Caption",
                ),
            ),
        )

        val draft = buildConversationDraftFromMessage(message)

        assertEquals("Forwarded body", draft.messageText)
        assertEquals("Forwarded subject", draft.subjectText)
        assertEquals(
            listOf(
                ConversationDraftAttachment(
                    contentType = "image/jpeg",
                    contentUri = "content://media/1",
                    captionText = "Caption",
                ),
            ),
            draft.attachments,
        )
    }

    @Test
    fun invoke_mediaPartWithoutCaption_mapsToEmptyCaption() {
        val message = messageData(
            text = "Body",
            subject = "",
            parts = listOf(
                mediaPart(
                    contentType = "image/png",
                    uri = "content://media/1",
                    caption = null,
                ),
            ),
        )

        val draft = buildConversationDraftFromMessage(message)

        assertEquals("", draft.attachments.single().captionText)
    }

    @Test
    fun invoke_nullSubject_mapsToEmptyString() {
        val message = messageData(
            text = "Body",
            subject = null,
            parts = emptyList(),
        )

        val draft = buildConversationDraftFromMessage(message)

        assertEquals("", draft.subjectText)
    }

    @Test
    fun invoke_nonMediaParts_areExcludedFromAttachments() {
        val message = messageData(
            text = "Body",
            subject = "",
            parts = listOf(
                mediaPart(
                    contentType = "text/plain",
                    uri = "content://text/1",
                    caption = null,
                ),
                mediaPart(
                    contentType = "image/png",
                    uri = "content://media/2",
                    caption = null,
                ),
            ),
        )

        val draft = buildConversationDraftFromMessage(message)

        assertEquals(
            listOf(
                ConversationDraftAttachment(
                    contentType = "image/png",
                    contentUri = "content://media/2",
                ),
            ),
            draft.attachments,
        )
    }

    @Test
    fun invoke_mediaPartWithoutUri_isExcludedFromAttachments() {
        val partWithoutUri = mockk<MessagePartData> {
            every { contentType } returns "image/jpeg"
            every { contentUri } returns null
        }

        val message = messageData(
            text = "Body",
            subject = "",
            parts = listOf(partWithoutUri),
        )

        val draft = buildConversationDraftFromMessage(message)

        assertEquals(emptyList<ConversationDraftAttachment>(), draft.attachments)
    }

    @Test
    fun invoke_noParts_producesEmptyAttachments() {
        val message = messageData(
            text = "",
            subject = "",
            parts = emptyList(),
        )

        val draft = buildConversationDraftFromMessage(message)

        assertEquals(emptyList<ConversationDraftAttachment>(), draft.attachments)
    }

    private fun messageData(
        text: String,
        subject: String?,
        parts: List<MessagePartData>,
    ): MessageData {
        return mockk {
            every { messageText } returns text
            every { mmsSubject } returns subject
            every { this@mockk.parts } returns parts
        }
    }

    private fun mediaPart(
        contentType: String,
        uri: String,
        caption: String?,
    ): MessagePartData {
        return mockk {
            every { this@mockk.contentType } returns contentType
            every { contentUri } returns Uri.parse(uri)
            every { text } returns caption
        }
    }
}
