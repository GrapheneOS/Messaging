package com.android.messaging.domain.conversation.usecase.forward.createforwardedmessage

import android.net.Uri
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessagePartData
import com.android.messaging.datamodel.data.PendingAttachmentData
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessageImpl
import com.android.messaging.domain.conversation.usecase.forward.ForwardedMessageSubjectFormatter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateForwardedMessageImplTest {

    private val conversationsRepository = mockk<ConversationsRepository>()
    private val subjectFormatter = mockk<ForwardedMessageSubjectFormatter>()

    @Test
    fun invoke_whenMessageDoesNotExist_returnsNull() {
        runTest {
            coEvery {
                conversationsRepository.getConversationMessage(
                    conversationId = CONVERSATION_ID,
                    messageId = MESSAGE_ID,
                )
            } returns null

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )

            assertNull(result)
            verify(exactly = 0) {
                subjectFormatter.format(subject = any())
            }
        }
    }

    @Test
    fun invoke_formatsAndCopiesSubject() {
        runTest {
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = emptyList(),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )

            assertEquals(FORWARDED_SUBJECT, result?.mmsSubject)
            verify(exactly = 1) {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            }
        }
    }

    @Test
    fun invoke_whenFormattedSubjectIsNull_keepsSubjectNull() {
        runTest {
            val sourceMessage = createSourceMessage(
                subject = null,
                parts = emptyList(),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = null)
            } returns null

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )

            assertNull(result?.mmsSubject)
            verify(exactly = 1) {
                subjectFormatter.format(subject = null)
            }
        }
    }

    @Test
    fun invoke_whenPartsAreNull_returnsMessageWithoutParts() {
        runTest {
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = null,
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )

            assertEquals(emptyList<MessagePartData>(), result?.parts?.toList())
        }
    }

    @Test
    fun invoke_whenPartsAreEmpty_returnsMessageWithoutParts() {
        runTest {
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = emptyList(),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )

            assertEquals(emptyList<MessagePartData>(), result?.parts?.toList())
        }
    }

    @Test
    fun invoke_copiesTextPartsIntoNewTextPartsInOrder() {
        runTest {
            val firstTextPart = createTextPart(text = "First")
            val secondTextPart = createTextPart(text = "Second")
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = listOf(firstTextPart, secondTextPart),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )
            val resultParts = result?.parts?.toList().orEmpty()

            assertEquals(2, resultParts.size)
            assertNotSame(firstTextPart, resultParts[0])
            assertNotSame(secondTextPart, resultParts[1])
            assertEquals("First", resultParts[0].text)
            assertEquals("Second", resultParts[1].text)
            assertTrue(resultParts[0].isText)
            assertTrue(resultParts[1].isText)
            assertNull(resultParts[0].contentUri)
            assertNull(resultParts[1].contentUri)
        }
    }

    @Test
    fun invoke_convertsAttachmentPartsIntoPendingAttachmentsInOrder() {
        runTest {
            val imageUri = Uri.parse(IMAGE_URI)
            val videoUri = Uri.parse(VIDEO_URI)
            val imagePart = createAttachmentPart(
                contentType = IMAGE_CONTENT_TYPE,
                contentUri = imageUri,
            )
            val videoPart = createAttachmentPart(
                contentType = VIDEO_CONTENT_TYPE,
                contentUri = videoUri,
            )
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = listOf(imagePart, videoPart),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )
            val resultParts = result?.parts?.toList().orEmpty()

            assertEquals(2, resultParts.size)
            assertPendingAttachment(
                part = resultParts[0],
                contentType = IMAGE_CONTENT_TYPE,
                contentUri = imageUri,
            )
            assertPendingAttachment(
                part = resultParts[1],
                contentType = VIDEO_CONTENT_TYPE,
                contentUri = videoUri,
            )
        }
    }

    @Test
    fun invoke_copiesMixedPartsInSourceOrder() {
        runTest {
            val contentUri = Uri.parse(IMAGE_URI)
            val textPart = createTextPart(text = "Before")
            val attachmentPart = createAttachmentPart(
                contentType = IMAGE_CONTENT_TYPE,
                contentUri = contentUri,
            )
            val trailingTextPart = createTextPart(text = "After")
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = listOf(textPart, attachmentPart, trailingTextPart),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            val result = createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )
            val resultParts = result?.parts?.toList().orEmpty()

            assertEquals(3, resultParts.size)
            assertEquals("Before", resultParts[0].text)
            assertPendingAttachment(
                part = resultParts[1],
                contentType = IMAGE_CONTENT_TYPE,
                contentUri = contentUri,
            )
            assertEquals("After", resultParts[2].text)
        }
    }

    @Test
    fun invoke_queriesRepositoryWithRequestedIds() {
        runTest {
            val sourceMessage = createSourceMessage(
                subject = ORIGINAL_SUBJECT,
                parts = emptyList(),
            )
            stubMessage(sourceMessage = sourceMessage)
            every {
                subjectFormatter.format(subject = ORIGINAL_SUBJECT)
            } returns FORWARDED_SUBJECT

            createUseCase().invoke(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )

            coVerify(exactly = 1) {
                conversationsRepository.getConversationMessage(
                    conversationId = CONVERSATION_ID,
                    messageId = MESSAGE_ID,
                )
            }
        }
    }

    private fun stubMessage(sourceMessage: ConversationMessageData?) {
        coEvery {
            conversationsRepository.getConversationMessage(
                conversationId = CONVERSATION_ID,
                messageId = MESSAGE_ID,
            )
        } returns sourceMessage
    }

    private fun createSourceMessage(
        subject: String?,
        parts: List<MessagePartData>?,
    ): ConversationMessageData {
        val message = mockk<ConversationMessageData>()
        every { message.mmsSubject } returns subject
        every { message.parts } returns parts
        return message
    }

    private fun createTextPart(text: String): MessagePartData {
        val part = mockk<MessagePartData>()
        every { part.isText } returns true
        every { part.text } returns text
        return part
    }

    private fun createAttachmentPart(
        contentType: String,
        contentUri: Uri,
    ): MessagePartData {
        val part = mockk<MessagePartData>()
        every { part.isText } returns false
        every { part.contentType } returns contentType
        every { part.contentUri } returns contentUri
        return part
    }

    private fun assertPendingAttachment(
        part: MessagePartData,
        contentType: String,
        contentUri: Uri,
    ) {
        assertTrue(part is PendingAttachmentData)
        assertEquals(contentType, part.contentType)
        assertEquals(contentUri, part.contentUri)
        assertEquals(MessagePartData.UNSPECIFIED_SIZE, part.width)
        assertEquals(MessagePartData.UNSPECIFIED_SIZE, part.height)
        assertNull(part.text)
        assertEquals(
            PendingAttachmentData.STATE_PENDING,
            (part as PendingAttachmentData).currentState
        )
    }

    private fun createUseCase(): CreateForwardedMessageImpl {
        return CreateForwardedMessageImpl(
            conversationsRepository = conversationsRepository,
            forwardedMessageSubjectFormatter = subjectFormatter,
        )
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-1"
        private const val FORWARDED_SUBJECT = "Fwd: Original subject"
        private const val IMAGE_CONTENT_TYPE = "image/jpeg"
        private const val IMAGE_URI = "content://media/image/1"
        private const val MESSAGE_ID = "message-1"
        private const val ORIGINAL_SUBJECT = "Original subject"
        private const val VIDEO_CONTENT_TYPE = "video/mp4"
        private const val VIDEO_URI = "content://media/video/1"
    }
}
