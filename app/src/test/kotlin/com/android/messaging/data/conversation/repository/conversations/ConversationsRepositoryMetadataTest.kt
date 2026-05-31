package com.android.messaging.data.conversation.repository.conversations

import android.database.ContentObserver
import android.database.Cursor
import android.database.MatrixCursor
import app.cash.turbine.test
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.createParticipantsCursor
import com.android.messaging.testutil.participantRow
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationsRepositoryMetadataTest : BaseConversationsRepositoryTest() {

    @Test
    fun getConversationMetadata_registersAndUnregistersObserverForCollection() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObservers = mutableListOf<ContentObserver>()
            val capturedProjections = mutableListOf<Array<String>?>()
            val repository = createRepository()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

            stubObserverRegistration(
                registeredObservers = registeredObservers,
                expectedUri = expectedUri,
            )
            stubQuery(
                expectedUri = expectedUri,
                capturedProjections = capturedProjections,
                result = createConversationMetadataCursor(
                    row = conversationMetadataRow(
                        conversationName = "Weekend plan",
                        selfParticipantId = "self-1",
                        participantCount = 3,
                    ),
                ),
            )

            repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
                assertEquals("Weekend plan", awaitItem()?.conversationName)
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                contentResolver.registerContentObserver(
                    expectedUri,
                    true,
                    registeredObservers.single(),
                )
            }
            verify(exactly = 1) {
                contentResolver.unregisterContentObserver(registeredObservers.single())
            }
            assertEquals(
                ConversationListItemData.PROJECTION.toList(),
                capturedProjections.single()?.toList(),
            )
        }
    }

    @Test
    fun getConversationMetadata_emitsMappedMetadata() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObservers = mutableListOf<ContentObserver>()
            val capturedProjections = mutableListOf<Array<String>?>()
            val repository = createRepository()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

            stubObserverRegistration(
                registeredObservers = registeredObservers,
                expectedUri = expectedUri,
            )
            stubQuery(
                expectedUri = expectedUri,
                capturedProjections = capturedProjections,
                result = createConversationMetadataCursor(
                    row = conversationMetadataRow(
                        conversationName = "Carol, Dave, Erin",
                        selfParticipantId = "self-2",
                        participantCount = 3,
                    ),
                ),
            )

            repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
                val metadata = awaitItem()

                assertEquals("Carol, Dave, Erin", metadata?.conversationName)
                assertEquals("self-2", metadata?.selfParticipantId)
                assertEquals(true, metadata?.isGroupConversation)
                assertEquals(3, metadata?.participantCount)
                assertEquals(false, metadata?.isArchived)
                assertEquals(null, metadata?.otherParticipantContactLookupKey)

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                ConversationListItemData.PROJECTION.toList(),
                capturedProjections.single()?.toList(),
            )
        }
    }

    @Test
    fun getConversationMetadata_exposesArchiveStatusAndLookupKey() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObservers = mutableListOf<ContentObserver>()
            val capturedProjections = mutableListOf<Array<String>?>()
            val repository = createRepository()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

            stubObserverRegistration(
                registeredObservers = registeredObservers,
                expectedUri = expectedUri,
            )
            stubQuery(
                expectedUri = expectedUri,
                capturedProjections = capturedProjections,
                result = createConversationMetadataCursor(
                    row = conversationMetadataRow(
                        conversationName = "Carol",
                        selfParticipantId = "self-2",
                        participantCount = 2,
                        isArchived = true,
                        otherParticipantLookupKey = "lookup-key-carol",
                    ),
                ),
            )

            repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
                val metadata = awaitItem()

                assertEquals(true, metadata?.isArchived)
                assertEquals("lookup-key-carol", metadata?.otherParticipantContactLookupKey)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun getConversationMetadata_oneOnOneConversation_usesParticipantDetailsForSubtitleAndAvatar() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val registeredObservers = mutableListOf<ContentObserver>()
            val metadataProjections = mutableListOf<Array<String>?>()
            val participantsProjections = mutableListOf<Array<String>?>()
            val repository = createRepository()
            val metadataUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)
            val participantsUri = MessagingContentProvider
                .buildConversationParticipantsUri(CONVERSATION_ID)

            stubObserverRegistration(
                registeredObservers = registeredObservers,
                expectedUri = metadataUri,
            )
            stubQuery(
                expectedUri = metadataUri,
                capturedProjections = metadataProjections,
                result = createConversationMetadataCursor(
                    row = conversationMetadataRow(
                        conversationName = "Carol",
                        selfParticipantId = "self-2",
                        participantCount = 1,
                        otherParticipantLookupKey = "legacy-lookup-key",
                        otherParticipantNormalizedDestination = TEST_CALL_ACTION_PHONE_NUMBER,
                    ),
                ),
            )
            stubQuery(
                expectedUri = participantsUri,
                capturedProjections = participantsProjections,
                result = createParticipantsCursor(
                    participantRow(
                        participantId = "self-2",
                        subId = 1,
                        displayDestination = "",
                        normalizedDestination = "",
                        profilePhotoUri = "",
                        lookupKey = "",
                        contactId = 1L,
                        fullName = "Name self-2",
                        firstName = "Name",
                    ),
                    participantRow(
                        participantId = "participant-1",
                        subId = ParticipantData.OTHER_THAN_SELF_SUB_ID,
                        displayDestination = "(555) 123-4567",
                        normalizedDestination = TEST_CALL_ACTION_PHONE_NUMBER,
                        profilePhotoUri = "content://contacts/people/1/photo",
                        lookupKey = "lookup-key-carol",
                        contactId = 1L,
                        fullName = "Name participant-1",
                        firstName = "Name",
                    ),
                ),
            )

            repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
                val metadata = awaitItem()

                assertEquals("(555) 123-4567", metadata?.otherParticipantDisplayDestination)
                assertEquals(
                    TEST_CALL_ACTION_PHONE_NUMBER,
                    metadata?.otherParticipantNormalizedDestination,
                )
                assertEquals("lookup-key-carol", metadata?.otherParticipantContactLookupKey)
                assertEquals(
                    "content://contacts/people/1/photo",
                    metadata?.otherParticipantPhotoUri,
                )

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                ParticipantData.ParticipantsQuery.PROJECTION.toList(),
                participantsProjections.single()?.toList(),
            )
        }
    }

    @Test
    fun getConversationMetadata_returnsNullWhenCursorIsEmpty() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObservers = mutableListOf<ContentObserver>()
            val capturedProjections = mutableListOf<Array<String>?>()
            val repository = createRepository()
            val expectedUri = MessagingContentProvider.buildConversationMetadataUri(CONVERSATION_ID)

            stubObserverRegistration(
                registeredObservers = registeredObservers,
                expectedUri = expectedUri,
            )
            stubQuery(
                expectedUri = expectedUri,
                capturedProjections = capturedProjections,
                result = createConversationMetadataCursor(row = null),
            )

            repository.getConversationMetadata(conversationId = CONVERSATION_ID).test {
                assertEquals(null, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                ConversationListItemData.PROJECTION.toList(),
                capturedProjections.single()?.toList(),
            )
        }
    }

    private fun createConversationMetadataCursor(row: TestConversationMetadataRow?): Cursor {
        val cursor = MatrixCursor(ConversationListItemData.PROJECTION)

        if (row != null) {
            cursor.addRow(
                ConversationListItemData.PROJECTION.map { columnName ->
                    row.toColumnValues()[columnName]
                }.toTypedArray(),
            )
        }

        return cursor
    }

    private fun conversationMetadataRow(
        conversationName: String,
        selfParticipantId: String,
        participantCount: Int,
        isArchived: Boolean = false,
        otherParticipantLookupKey: String = "",
        otherParticipantNormalizedDestination: String = "",
    ): TestConversationMetadataRow {
        return TestConversationMetadataRow(
            conversationName = conversationName,
            selfParticipantId = selfParticipantId,
            participantCount = participantCount,
            isArchived = isArchived,
            otherParticipantLookupKey = otherParticipantLookupKey,
            otherParticipantNormalizedDestination = otherParticipantNormalizedDestination,
        )
    }

    private data class TestConversationMetadataRow(
        val conversationName: String,
        val selfParticipantId: String,
        val participantCount: Int,
        val isArchived: Boolean = false,
        val otherParticipantLookupKey: String = "",
        val otherParticipantNormalizedDestination: String = "",
    ) {
        fun toColumnValues(): Map<String, Any?> {
            return mapOf(
                ConversationColumns._ID to CONVERSATION_ID,
                ConversationColumns.NAME to conversationName,
                ConversationColumns.ICON to "",
                ConversationColumns.SNIPPET_TEXT to "",
                ConversationColumns.SORT_TIMESTAMP to 0L,
                MessageColumns.READ to 1,
                ConversationColumns.PREVIEW_URI to "",
                ConversationColumns.PREVIEW_CONTENT_TYPE to "",
                ConversationColumns.PARTICIPANT_CONTACT_ID to -1L,
                ConversationColumns.PARTICIPANT_LOOKUP_KEY to otherParticipantLookupKey,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to
                    otherParticipantNormalizedDestination,
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
                ConversationColumns.ARCHIVE_STATUS to if (isArchived) 1 else 0,
                MessageColumns._ID to "message-1",
                ConversationColumns.SUBJECT_TEXT to "",
                ConversationColumns.DRAFT_SUBJECT_TEXT to "",
                MessageColumns.RAW_TELEPHONY_STATUS to 0,
                "snippet_sender_first_name" to "",
                "snippet_sender_display_destination" to "",
                ConversationColumns.IS_ENTERPRISE to 0,
            )
        }
    }
}
