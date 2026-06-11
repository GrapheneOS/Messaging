package com.android.messaging.ui.conversation.metadata.mapper

import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.testutil.TEST_CALL_ACTION_PHONE_NUMBER
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationMetadataUiStateMapperImplTest {

    private val mapper = ConversationMetadataUiStateMapperImpl()

    @Test
    fun map_oneOnOneConversation_usesSingleAvatarAndDisplayDestination() {
        val result = mapper.map(
            metadata = ConversationMetadata(
                conversationName = "Carol",
                selfParticipantId = "self-1",
                isGroupConversation = false,
                includeEmailAddress = false,
                participantCount = 1,
                otherParticipantDisplayDestination = "(555) 123-4567",
                otherParticipantNormalizedDestination = TEST_CALL_ACTION_PHONE_NUMBER,
                otherParticipantContactLookupKey = "lookup-key",
                otherParticipantPhotoUri = "content://contacts/people/1/photo",
                isArchived = false,
                isBlocked = false,
                composerAvailability = ConversationComposerAvailability.Editable,
                sortTimestamp = 0L,
            ),
        )

        assertEquals(
            ConversationMetadataUiState.Present(
                title = "Carol",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = "content://contacts/people/1/photo",
                    normalizedDestination = TEST_CALL_ACTION_PHONE_NUMBER,
                    displayName = "Carol",
                ),
                participantCount = 1,
                otherParticipantDisplayDestination = "(555) 123-4567",
                otherParticipantPhoneNumber = TEST_CALL_ACTION_PHONE_NUMBER,
                otherParticipantContactLookupKey = "lookup-key",
                isArchived = false,
                isBlocked = false,
                composerAvailability = ConversationComposerAvailability.Editable,
            ),
            result,
        )
    }

    @Test
    fun map_groupConversation_usesGroupAvatarAndNoPhoneNumber() {
        val result = mapper.map(
            metadata = ConversationMetadata(
                conversationName = "Weekend plan",
                selfParticipantId = "self-1",
                isGroupConversation = true,
                includeEmailAddress = false,
                participantCount = 3,
                otherParticipantDisplayDestination = null,
                otherParticipantNormalizedDestination = "not-a-phone-number",
                otherParticipantContactLookupKey = null,
                otherParticipantPhotoUri = "content://contacts/people/1/photo",
                isArchived = true,
                isBlocked = false,
                composerAvailability = ConversationComposerAvailability.Editable,
                sortTimestamp = 0L,
            ),
        )

        val presentState = result as ConversationMetadataUiState.Present
        assertEquals(ConversationMetadataUiState.Avatar.Group, presentState.avatar)
        assertNull(presentState.otherParticipantPhoneNumber)
        assertEquals(true, presentState.isArchived)
    }
}
