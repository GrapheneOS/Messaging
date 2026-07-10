package com.android.messaging.ui.conversationsettings.screen.mapper

import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationSettingsUiStateMapperImplTest {

    private val mapper = ConversationSettingsUiStateMapperImpl(
        canPlacePhoneCall = { false },
        canShowOrAddContact = { _, _, _, _ -> false },
        isContactSavedUseCase = { _, _ -> false },
    )

    @Test
    fun map_unsavedNumberWithNullFullName_usesDestinationAsDisplayName() {
        val participantUiState = mapParticipant(
            name = null,
            unknownSender = true,
        )

        assertEquals(DESTINATION, participantUiState.displayName)
        assertNull(participantUiState.details)
    }

    @Test
    fun map_savedContact_usesFullNameAndKeepsDestinationAsDetails() {
        val participantUiState = mapParticipant(
            name = FULL_NAME,
            unknownSender = false,
        )

        assertEquals(FULL_NAME, participantUiState.displayName)
        assertEquals(DESTINATION, participantUiState.details)
    }

    private fun mapParticipant(
        name: String?,
        unknownSender: Boolean,
    ): ParticipantUiState {
        val participant = mockk<ParticipantData>(relaxed = true) {
            every { fullName } returns name
            every { sendDestination } returns DESTINATION
            every { isUnknownSender } returns unknownSender
        }

        return mapper
            .map(ConversationSettingsData(participants = persistentListOf(participant)))
            .participants
            .single()
    }

    private companion object {
        private const val DESTINATION = "+15550123"
        private const val FULL_NAME = "Ada Lovelace"
    }
}
