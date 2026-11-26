package com.android.messaging.data.conversationsettings.repository

import android.content.ContentResolver
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.data.conversationsettings.model.SNOOZE_NEVER_EXPIRES
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationSettingsRepositoryImplTest {

    @Test
    fun getConversationSettings_isListedOnlyWithSortTimestamp() {
        assertEquals(true, loadSettings(metadata = createMetadata(sortTimestamp = 1L)).isListed)
        assertEquals(false, loadSettings(metadata = createMetadata(sortTimestamp = 0L)).isListed)
        assertEquals(false, loadSettings(metadata = null).isListed)
    }

    private fun loadSettings(metadata: ConversationMetadata?): ConversationSettingsData {
        val conversationsRepository = mockk<ConversationsRepository> {
            coEvery {
                getConversationMetadataSnapshot(conversationId = CONVERSATION_ID)
            } returns metadata
        }
        val notificationRepository = mockk<ConversationNotificationRepository> {
            every { isSnoozed(conversationId = CONVERSATION_ID) } returns false
            every {
                getSnoozeUntilMillis(conversationId = CONVERSATION_ID)
            } returns SNOOZE_NEVER_EXPIRES
        }
        val contentResolver = mockk<ContentResolver>(relaxed = true) {
            every { query(any(), any(), any(), any(), any()) } returns null
        }
        var settings: ConversationSettingsData? = null

        runTest {
            val repository = ConversationSettingsRepositoryImpl(
                contentResolver = contentResolver,
                conversationsRepository = conversationsRepository,
                notificationRepository = notificationRepository,
                messagingDbDispatcher = StandardTestDispatcher(testScheduler),
            )
            settings = repository.getConversationSettings(conversationId = CONVERSATION_ID).first()
        }

        return requireNotNull(settings)
    }

    private fun createMetadata(sortTimestamp: Long): ConversationMetadata {
        return ConversationMetadata(
            conversationName = "Alice",
            selfParticipantId = null,
            isGroupConversation = false,
            includeEmailAddress = false,
            participantCount = 1,
            otherParticipantDisplayDestination = null,
            otherParticipantNormalizedDestination = null,
            otherParticipantContactLookupKey = null,
            otherParticipantPhotoUri = null,
            isArchived = false,
            isBlocked = false,
            isSnoozed = false,
            composerAvailability = ConversationComposerAvailability.Editable,
            sortTimestamp = sortTimestamp,
        )
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-1")
    }
}
