package com.android.messaging.ui.blockedparticipants.screen.mapper

import com.android.messaging.data.blockedparticipants.model.BlockedDirectChat
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class BlockedParticipantsUiStateMapperImplTest {

    private val mapper = BlockedParticipantsUiStateMapperImpl(
        canPlacePhoneCall = { false },
        canShowOrAddContact = { _, _, _, _ -> false },
        isContactSavedUseCase = { _, _ -> false },
    )

    @Test
    fun map_unsavedNumberWithNullFullName_usesFormattedDestinationAsDisplayName() {
        val participantUiState = mapParticipant(
            name = null,
            unknownSender = true,
        )

        assertEquals(DISPLAY_DESTINATION, participantUiState.displayName)
        assertNull(participantUiState.details)
    }

    @Test
    fun map_savedContact_usesFullNameAndKeepsFormattedDestinationAsDetails() {
        val participantUiState = mapParticipant(
            name = FULL_NAME,
            unknownSender = false,
        )

        assertEquals(FULL_NAME, participantUiState.displayName)
        assertEquals(DISPLAY_DESTINATION, participantUiState.details)
    }

    private fun mapParticipant(
        name: String?,
        unknownSender: Boolean,
    ): BlockedParticipantUiState {
        val participant = mockk<ParticipantData>(relaxed = true) {
            every { fullName } returns name
            every { sendDestination } returns SEND_DESTINATION
            every { displayDestination } returns DISPLAY_DESTINATION
            every { isUnknownSender } returns unknownSender
        }

        return mapper
            .map(
                persistentListOf(
                    BlockedDirectChat(
                        participant = participant,
                        conversationId = CONVERSATION_ID,
                    ),
                ),
            )
            .single()
    }

    private companion object {
        private val CONVERSATION_ID = ConversationId("conversation-1")
        private const val SEND_DESTINATION = "+15550123"
        private const val DISPLAY_DESTINATION = "+1 555-0123"
        private const val FULL_NAME = "Ada Lovelace"
    }
}
