package com.android.messaging.data.conversation.repository.conversations

import android.database.Cursor
import android.database.MatrixCursor
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.createParticipantsCursor
import com.android.messaging.testutil.participantRow
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationsRepositoryDirectLookupTest : BaseConversationsRepositoryTest() {

    @Test
    fun getConversationSendData_returnsNullForBlankConversationIdWithoutQuerying() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val result = createRepository().getConversationSendData(
                conversationId = " ",
                requestedSelfParticipantId = "self-1",
            )

            assertNull(result)
            verify(exactly = 0) {
                contentResolver.query(any(), any(), any(), any(), any())
            }
        }
    }

    @Test
    fun getConversationSendData_returnsMetadataParticipantsAndRequestedSelfParticipant() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val metadataUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val participantsUri = MessagingContentProvider
                .buildConversationParticipantsUri(CONVERSATION_ID)
            val participantSelectionArgsSlot = slot<Array<String>>()
            every {
                contentResolver.query(
                    metadataUri,
                    ConversationListItemData.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createConversationMetadataCursor(
                conversationName = "Project",
                selfParticipantId = "metadata-self",
                participantCount = 2,
            )
            every {
                contentResolver.query(
                    participantsUri,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "requested-self",
                    subId = 7,
                    slotId = 0,
                    subscriptionName = "Carrier",
                ),
                participantRow(
                    participantId = "other",
                    subId = ParticipantData.OTHER_THAN_SELF_SUB_ID,
                    slotId = ParticipantData.INVALID_SLOT_ID,
                    subscriptionName = "",
                ),
            )
            every {
                contentResolver.query(
                    MessagingContentProvider.PARTICIPANTS_URI,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    "${ParticipantColumns._ID} = ?",
                    capture(participantSelectionArgsSlot),
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "requested-self",
                    subId = 7,
                    slotId = 0,
                    subscriptionName = "Carrier",
                ),
            )

            val result = createRepository().getConversationSendData(
                conversationId = CONVERSATION_ID,
                requestedSelfParticipantId = "requested-self",
            )

            assertEquals("Project", result?.metadata?.conversationName)
            assertEquals("metadata-self", result?.metadata?.selfParticipantId)
            assertTrue(requireNotNull(result).participants.isLoaded)
            assertEquals("requested-self", result.selfParticipant?.id)
            assertEquals(listOf("requested-self"), participantSelectionArgsSlot.captured.toList())
        }
    }

    @Test
    fun getConversationSendData_usesMetadataSelfParticipantWhenRequestIsBlank() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val metadataUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val participantsUri = MessagingContentProvider
                .buildConversationParticipantsUri(CONVERSATION_ID)
            val participantSelectionArgsSlot = slot<Array<String>>()
            every {
                contentResolver.query(
                    metadataUri,
                    ConversationListItemData.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createConversationMetadataCursor(
                conversationName = "Project",
                selfParticipantId = "metadata-self",
                participantCount = 1,
            )
            every {
                contentResolver.query(
                    participantsUri,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createParticipantsCursor()
            every {
                contentResolver.query(
                    MessagingContentProvider.PARTICIPANTS_URI,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    "${ParticipantColumns._ID} = ?",
                    capture(participantSelectionArgsSlot),
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "metadata-self",
                    subId = 5,
                    slotId = 0,
                    subscriptionName = "Carrier",
                ),
            )

            val result = createRepository().getConversationSendData(
                conversationId = CONVERSATION_ID,
                requestedSelfParticipantId = "",
            )

            assertEquals("metadata-self", result?.selfParticipant?.id)
            assertEquals(listOf("metadata-self"), participantSelectionArgsSlot.captured.toList())
        }
    }

    @Test
    fun getMessageDetailsData_returnsMessageParticipantsAndSelfParticipant() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val messagesUri = MessagingContentProvider.buildConversationMessagesUri(CONVERSATION_ID)
            val participantsUri = MessagingContentProvider
                .buildConversationParticipantsUri(CONVERSATION_ID)
            every {
                contentResolver.query(
                    messagesUri,
                    ConversationMessageData.getProjection(),
                    null,
                    null,
                    null,
                )
            } returns createConversationMessagesCursor(
                messageRow(
                    messageId = "message-1",
                    participantId = "other",
                    selfParticipantId = "self-1",
                    text = "Hello",
                ),
            )
            every {
                contentResolver.query(
                    participantsUri,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "self-1",
                    subId = 1,
                    slotId = 0,
                    subscriptionName = "Carrier",
                ),
                participantRow(
                    participantId = "other",
                    subId = ParticipantData.OTHER_THAN_SELF_SUB_ID,
                    slotId = ParticipantData.INVALID_SLOT_ID,
                    subscriptionName = "",
                ),
            )
            every {
                contentResolver.query(
                    MessagingContentProvider.PARTICIPANTS_URI,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    "${ParticipantColumns._ID} = ?",
                    arrayOf("self-1"),
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "self-1",
                    subId = 1,
                    slotId = 0,
                    subscriptionName = "Carrier",
                ),
            )

            val result = createRepository().getMessageDetailsData(
                conversationId = CONVERSATION_ID,
                messageId = "message-1",
            )

            assertEquals("message-1", result?.message?.messageId)
            assertEquals("Hello", result?.message?.text)
            assertTrue(requireNotNull(result).participants.isLoaded)
            assertEquals("self-1", result.selfParticipant?.id)
        }
    }

    @Test
    fun getMessageDetailsData_returnsNullWhenMessageCannotBeResolved() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val result = createRepository().getMessageDetailsData(
                conversationId = "",
                messageId = "message-1",
            )

            assertNull(result)
            verify(exactly = 0) {
                contentResolver.query(any(), any(), any(), any(), any())
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun createConversationMetadataCursor(
        conversationName: String,
        selfParticipantId: String,
        participantCount: Int,
    ): Cursor {
        val cursor = MatrixCursor(ConversationListItemData.PROJECTION)
        val row: Map<String, Any?> = mapOf(
            ConversationColumns._ID to CONVERSATION_ID,
            ConversationColumns.NAME to conversationName,
            ConversationColumns.ICON to "",
            ConversationColumns.SNIPPET_TEXT to "",
            ConversationColumns.SORT_TIMESTAMP to 10L,
            MessageColumns.READ to 1,
            ConversationColumns.PREVIEW_URI to "",
            ConversationColumns.PREVIEW_CONTENT_TYPE to "",
            ConversationColumns.PARTICIPANT_CONTACT_ID to -1L,
            ConversationColumns.PARTICIPANT_LOOKUP_KEY to "",
            ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to "",
            ConversationColumns.PARTICIPANT_COUNT to participantCount,
            ConversationColumns.CURRENT_SELF_ID to selfParticipantId,
            ConversationColumns.NOTIFICATION_ENABLED to 1,
            ConversationColumns.NOTIFICATION_SOUND_URI to "",
            ConversationColumns.NOTIFICATION_VIBRATION to 0,
            ConversationColumns.INCLUDE_EMAIL_ADDRESS to 0,
            MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
            ConversationColumns.SHOW_DRAFT to 0,
            ConversationColumns.DRAFT_PREVIEW_URI to "",
            ConversationColumns.DRAFT_PREVIEW_CONTENT_TYPE to "",
            ConversationColumns.DRAFT_SNIPPET_TEXT to "",
            ConversationColumns.ARCHIVE_STATUS to 0,
            MessageColumns._ID to "message-1",
            ConversationColumns.SUBJECT_TEXT to "",
            ConversationColumns.DRAFT_SUBJECT_TEXT to "",
            MessageColumns.RAW_TELEPHONY_STATUS to 0,
            "snippet_sender_first_name" to "",
            "snippet_sender_display_destination" to "",
            ConversationColumns.IS_ENTERPRISE to 0,
        )
        cursor.addRow(
            ConversationListItemData.PROJECTION.map { columnName ->
                row[columnName]
            }.toTypedArray(),
        )
        return cursor
    }

    private fun createConversationMessagesCursor(vararg rows: TestMessageRow): Cursor {
        val cursor = MatrixCursor(ConversationMessageData.getProjection())
        rows.forEach { row ->
            cursor.addRow(
                ConversationMessageData.getProjection().map { columnName ->
                    row.toColumnValues()[columnName]
                }.toTypedArray(),
            )
        }
        return cursor
    }

    @Suppress("SameParameterValue")
    private fun messageRow(
        messageId: String,
        participantId: String,
        selfParticipantId: String,
        text: String,
    ): TestMessageRow {
        return TestMessageRow(
            messageId = messageId,
            participantId = participantId,
            selfParticipantId = selfParticipantId,
            text = text,
        )
    }

    private data class TestMessageRow(
        val messageId: String,
        val participantId: String,
        val selfParticipantId: String,
        val text: String,
    ) {
        fun toColumnValues(): Map<String, Any?> {
            return mapOf(
                MessageColumns._ID to messageId,
                MessageColumns.CONVERSATION_ID to CONVERSATION_ID,
                MessageColumns.SENDER_PARTICIPANT_ID to participantId,
                ConversationColumns.ICON to "",
                "parts_ids" to "part-$messageId",
                "parts_content_types" to "text/plain",
                "parts_content_uris" to "",
                "parts_widths" to "0",
                "parts_heights" to "0",
                "parts_texts" to text,
                "parts_count" to 1,
                MessageColumns.SENT_TIMESTAMP to 1_000L,
                MessageColumns.RECEIVED_TIMESTAMP to 1_000L,
                MessageColumns.SEEN to 1,
                MessageColumns.READ to 1,
                MessageColumns.PROTOCOL to MessageData.PROTOCOL_SMS,
                MessageColumns.STATUS to MessageData.BUGLE_STATUS_INCOMING_COMPLETE,
                MessageColumns.SMS_MESSAGE_URI to "",
                MessageColumns.SMS_PRIORITY to 0,
                MessageColumns.SMS_MESSAGE_SIZE to 0,
                MessageColumns.MMS_SUBJECT to "",
                MessageColumns.MMS_EXPIRY to 0L,
                MessageColumns.RAW_TELEPHONY_STATUS to 0,
                MessageColumns.SELF_PARTICIPANT_ID to selfParticipantId,
                ParticipantColumns.FULL_NAME to "",
                ParticipantColumns.FIRST_NAME to "",
                ParticipantColumns.DISPLAY_DESTINATION to "",
                ParticipantColumns.NORMALIZED_DESTINATION to "",
                ParticipantColumns.PROFILE_PHOTO_URI to "",
                ParticipantColumns.CONTACT_ID to 0L,
                ParticipantColumns.LOOKUP_KEY to "",
            )
        }
    }
}
